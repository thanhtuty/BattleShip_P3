package battleship.part3;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.Random;

/** Server có thẩm quyền xác nhận lượt bắn, kết quả và ghi dữ liệu xuống SQL Server. */
public final class Server {
    private final GameDAO dao;
    private final AccountDAO accounts;
    private final Player[] players = new Player[2];
    private final Board[] boards = new Board[2];
    private TurnManager turn;
    private long matchId;
    private boolean playing;
    private boolean finished;

    private Server(GameDAO dao, AccountDAO accounts) { this.dao = dao; this.accounts = accounts; }

    public static void main(String[] args) throws Exception {
        DbConfig config = DbConfig.load();
        try (var connection = config.connect()) {
            System.out.println("Đã kết nối SQL Server: " + connection.getMetaData().getDatabaseProductName());
        }
        int port = Integer.parseInt(System.getProperty("port", "5002"));
        AccountDAO accounts = new AccountDAO(config);
        accounts.ensureDemoAccounts();
        System.out.println("Tài khoản mô phỏng: abc1 / abc12345 và abc2 / abc12345");
        new Server(new GameDAO(config), accounts).listen(port);
    }

    private void listen(int port) throws IOException {
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("BattleShip Phần 3 đang nghe cổng " + port + ". Mở hai Client để chơi.");
            for (int i = 0; i < 2; i++) {
                Socket socket = server.accept();
                Player player = new Player(i, socket);
                players[i] = player;
                boards[i] = new Board();
                player.send("WELCOME|" + i);
                new Thread(player, "client-" + i).start();
            }
            while (true) {
                try {
                    Thread.sleep(1000);
                    synchronized (this) {
                        if (playing && !finished && turn.expired()) {
                            int previous = turn.current();
                            turn.miss();
                            try {
                                dao.changeTurn(matchId, players[turn.current()].name);
                                dao.event(matchId, players[previous].name, "TIMEOUT",
                                        "Hết 15 giây; lượt chuyển sang " + players[turn.current()].name);
                            } catch (SQLException e) {
                                System.err.println("Không lưu được timeout: " + e.getMessage());
                            }
                            broadcast("TIME|Hết 15 giây. Lượt chuyển sang " + players[turn.current()].name);
                        }
                        if (turn != null) broadcastTurn();
                        if (playing && !finished) state();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private synchronized void message(Player player, String line) throws Exception {
        String[] fields = line.split("\\|", -1);
        switch (fields[0]) {
            case "LOGIN", "REGISTER" -> {
                if (player.authenticated) throw new IllegalStateException("Client này đã đăng nhập.");
                if (fields.length < 3) {
                    player.send("AUTH_FAIL|Thiếu tên tài khoản hoặc mật khẩu.");
                    return;
                }
                String username = fields[1].trim();
                boolean accepted;
                if (fields[0].equals("REGISTER")) {
                    try {
                        accepted = accounts.register(username, fields[2]);
                    } catch (IllegalArgumentException e) {
                        player.send("AUTH_FAIL|" + e.getMessage());
                        return;
                    }
                    if (!accepted) {
                        player.send("AUTH_FAIL|Tên tài khoản đã tồn tại.");
                        return;
                    }
                } else {
                    accepted = accounts.authenticate(username, fields[2]);
                    if (!accepted) {
                        player.send("AUTH_FAIL|Sai tài khoản hoặc mật khẩu.");
                        return;
                    }
                }
                player.name = username;
                player.authenticated = true;
                player.send("AUTH_OK|" + player.id + "|" + player.name);
                if (players[0] != null && players[1] != null
                        && players[0].authenticated && players[1].authenticated && matchId == 0) {
                    matchId = dao.create(players[0].name, players[1].name);
                    dao.event(matchId, players[0].name, "CONNECT", "Vào phòng");
                    dao.event(matchId, players[1].name, "CONNECT", "Vào phòng");
                    broadcast("INFO|Hai người chơi đã đăng nhập. Hãy đặt tàu.");
                    state();
                } else {
                    player.send("WAIT|Đăng nhập thành công. Đang chờ người chơi còn lại.");
                }
            }
            case "PLACE" -> {
                if (matchId == 0) throw new IllegalStateException("Đang chờ đối thủ kết nối");
                if (player.ready) throw new IllegalStateException("Bạn đã Ready rồi");
                int row = Integer.parseInt(fields[1]);
                int col = Integer.parseInt(fields[2]);
                boolean horizontal = fields[3].equals("H");
                if (!horizontal && !fields[3].equals("V")) throw new IllegalArgumentException("Hướng đặt tàu không hợp lệ");
                Board board = boards[player.id];
                int length = board.nextLength();
                board.place(row, col, horizontal);
                dao.event(matchId, player.name, "PLACE_SHIP", "Tàu dài " + length + " tại " + (row + 1) + "," + (col + 1) + " hướng " + fields[3]);
                player.send("PLACE_OK|" + board.placed() + "|" + board.nextLength());
                state();
            }
            case "READY" -> {
                if (matchId == 0) throw new IllegalStateException("Đang chờ đối thủ kết nối");
                if (player.ready) throw new IllegalStateException("Bạn đã Ready rồi");
                Board board = boards[player.id];
                if (board == null || !board.ready()) throw new IllegalStateException("Cần đặt đủ 4 tàu trước khi Ready");
                dao.ready(matchId, player.name, board.encode());
                dao.event(matchId, player.name, "READY", "Đã sẵn sàng");
                player.ready = true;
                if (players[0].ready && players[1].ready) {
                    playing = true;
                    turn = new TurnManager(new Random().nextInt(2));
                    dao.start(matchId, players[turn.current()].name);
                    dao.event(matchId, players[turn.current()].name, "START", "Trận bắt đầu");
                    broadcast("START|Trận đấu bắt đầu. " + players[turn.current()].name + " đi trước.");
                    broadcastTurn();
                }
                state();
            }
            case "SHOT" -> {
                if (!playing || finished) throw new IllegalStateException("Trận chưa bắt đầu hoặc đã kết thúc");
                if (player.id != turn.current()) throw new IllegalStateException("Chưa đến lượt bạn");
                int row = Integer.parseInt(fields[1]);
                int col = Integer.parseInt(fields[2]);
                int targetId = 1 - player.id;
                String result = boards[targetId].shoot(row, col);
                if (result.equals("MISS")) turn.miss(); else turn.hit();
                String nextPlayer = players[turn.current()].name;
                dao.shot(matchId, player.name, players[targetId].name, result, row, col,
                        nextPlayer, boards[targetId].encode());
                broadcast("ATTACK_RESULT|" + player.name + "|" + result + "|" + row + "|" + col + "|" + nextPlayer);
                if (boards[targetId].allSunk()) {
                    finished = true;
                    dao.finish(matchId, player.name);
                    dao.event(matchId, player.name, "WIN", "Toàn bộ tàu đối thủ đã bị đánh chìm");
                    broadcast("WIN|" + player.name + "|" + players[targetId].name);
                } else if (result.equals("SUNK")) {
                    broadcast("SUNK|" + player.name + "|" + players[targetId].name);
                } else if (result.equals("MISS")) {
                    broadcast("MISS|" + player.name + "|" + nextPlayer);
                }
                broadcastTurn();
                state();
            }
            default -> throw new IllegalArgumentException("Lệnh không hợp lệ");
        }
    }

    private void state() {
        if (players[0] == null || players[1] == null) return;
        for (Player player : players) {
            int opponent = 1 - player.id;
            String own = boards[player.id] == null ? "0".repeat(36) : boards[player.id].encode(true);
            String enemy = boards[opponent] == null ? "0".repeat(36) : boards[opponent].encode(false);
            player.send("STATE|" + (playing ? "PLAYING" : "SETUP") + "|"
                    + (turn == null ? -1 : turn.current()) + "|"
                    + (turn == null ? 15 : turn.secondsLeft()) + "|" + own + "|" + enemy
                    + "|" + (player.ready ? "1" : "0"));
        }
    }

    private void broadcast(String message) {
        for (Player player : players) if (player != null) player.send(message);
    }

    private void broadcastTurn() {
        if (turn == null) return;
        broadcast("TURN|" + turn.current() + "|" + players[turn.current()].name + "|" + turn.secondsLeft());
    }

    private synchronized void disconnected(Player player) {
        if (matchId == 0 || finished) return;
        try {
            dao.event(matchId, player.name, "DISCONNECT", "Người chơi mất kết nối");
            if (playing) {
                String winner = players[1 - player.id].name;
                finished = true;
                dao.finish(matchId, winner);
                dao.event(matchId, winner, "WIN", "Đối thủ mất kết nối");
                broadcast("WIN|" + winner + "|" + player.name);
            } else {
                broadcast("INFO|" + player.name + " đã ngắt kết nối trước khi bắt đầu trận.");
            }
        } catch (SQLException e) {
            System.err.println("Không lưu được trạng thái mất kết nối: " + e.getMessage());
        }
    }

    private final class Player implements Runnable {
        private final int id;
        private final Socket socket;
        private final DataInputStream input;
        private final DataOutputStream output;
        private String name = "Player";
        private boolean ready;
        private boolean authenticated;

        private Player(int id, Socket socket) throws IOException {
            this.id = id;
            this.socket = socket;
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
        }

        private synchronized void send(String message) {
            try { output.writeUTF(message); output.flush(); }
            catch (IOException ignored) { }
        }

        @Override public void run() {
            try {
                while (true) {
                    String line = input.readUTF();
                    try { message(this, line); }
                    catch (Exception e) { send("ERROR|" + e.getMessage()); }
                }
            } catch (IOException e) {
                send("ERROR|Kết nối đã đóng");
                disconnected(this);
                try { socket.close(); } catch (IOException ignored) { }
            }
        }
    }
}
