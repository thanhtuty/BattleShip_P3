package battleship.part3;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JPasswordField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.JDialog;
import javax.swing.Timer;

public final class Client extends JFrame {
    private final JButton[][] own = new JButton[6][6];
    private final JButton[][] enemy = new JButton[6][6];
    private final JLabel status = new JLabel("Đang kết nối...");
    private final JTextArea log = new JTextArea();
    private final Board placement = new Board();
    private final List<String> placements = new ArrayList<>();

    private boolean horizontal = true;
    private boolean ready;
    private boolean opponentConnected;
    private DataInputStream in;
    private DataOutputStream out;
    private int player = -1;
    private String mode = "SETUP";
    private int active = -1;
    private String ownState = "0".repeat(36);
    private String enemyState = "0".repeat(36);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client().setVisible(true));
    }

    Client() {
        super("BattleShip Online - Phần 3");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        JPanel top = new JPanel(new BorderLayout());
        status.setFont(new Font("SansSerif", Font.BOLD, 16));
        top.add(status, BorderLayout.CENTER);
        JButton rotate = new JButton("Đổi hướng: ngang");
        rotate.addActionListener(e -> {
            horizontal = !horizontal;
            rotate.setText("Đổi hướng: " + (horizontal ? "ngang" : "dọc"));
        });
        top.add(rotate, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);

        JPanel boards = new JPanel(new GridLayout(1, 2, 12, 0));
        boards.add(makeBoard("Bàn của tôi", own, true));
        boards.add(makeBoard("Bàn đối thủ - nhấn để bắn", enemy, false));
        add(boards, BorderLayout.CENTER);

        log.setEditable(false);
        log.setRows(5);
        add(new JScrollPane(log), BorderLayout.SOUTH);
        connect();
    }

    private JPanel makeBoard(String title, JButton[][] cells, boolean mine) {
        JPanel panel = new JPanel(new GridLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                final int row = r, col = c;
                JButton button = new JButton(" ");
                button.setPreferredSize(new Dimension(55, 50));
                button.addActionListener(e -> {
                    if (mine) place(row, col); else shoot(row, col);
                });
                cells[r][c] = button;
                panel.add(button);
            }
        }
        return panel;
    }

    private void connect() {
        String host = JOptionPane.showInputDialog(this, "IP máy chạy Server", "127.0.0.1");
        if (host == null || host.isBlank()) {
            dispose();
            return;
        }
        try {
            Socket socket = new Socket(host.trim(),
                    Integer.parseInt(System.getProperty("port", "5002")));
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            Thread reader = new Thread(() -> {
                try {
                    while (true) {
                        String message = in.readUTF();
                        SwingUtilities.invokeLater(() -> handle(message));
                    }
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> status.setText("Mất kết nối với Server"));
                }
            }, "server-reader");
            reader.setDaemon(true);
            reader.start();
            requestAuth();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Không kết nối được Server: " + e.getMessage());
            dispose();
        }
    }

    private void requestAuth() {
        Object[] choices = {"Đăng nhập", "Tạo tài khoản", "Hủy"};
        int action = JOptionPane.showOptionDialog(this,
                "Chọn cách vào trò chơi", "Tài khoản BattleShip",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, choices, choices[0]);
        if (action < 0 || action == 2) {
            dispose();
            return;
        }

        String username = JOptionPane.showInputDialog(this,
                "Tên tài khoản (3–50 ký tự, chữ/số/_):", "player1");
        if (username == null) {
            requestAuth();
            return;
        }
        JPasswordField passwordField = new JPasswordField();
        int result = JOptionPane.showConfirmDialog(this, passwordField,
                "Nhập mật khẩu (ít nhất 8 ký tự)", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            requestAuth();
            return;
        }
        String password = new String(passwordField.getPassword());
        if (username.contains("|") || password.contains("|")) {
            JOptionPane.showMessageDialog(this, "Tên và mật khẩu không được chứa ký tự |.");
            requestAuth();
            return;
        }
        send((action == 0 ? "LOGIN|" : "REGISTER|")
                + username.trim() + "|" + password);
        status.setText(action == 0 ? "Đang kiểm tra tài khoản..." : "Đang tạo tài khoản...");
    }

    private void place(int row, int col) {
        if (ready || placement.ready()) return;
        if (!opponentConnected) {
            status.setText("Đang chờ người chơi thứ hai đăng nhập...");
            return;
        }
        try {
            int length = placement.nextLength();
            placement.place(row, col, horizontal);
            placements.add(row + "," + col + "," + (horizontal ? "H" : "V"));
            send("PLACE|" + row + "|" + col + "|" + (horizontal ? "H" : "V"));
            render();
            log.append("Đã đặt tàu dài " + length + ".\n");
            if (placement.ready()
                    && JOptionPane.showConfirmDialog(this, "Đã đặt đủ 4 tàu. Báo Ready?",
                            "Sẵn sàng", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                ready = true;
                send("READY");
                status.setText("Đã sẵn sàng, chờ đối thủ...");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }

    private void shoot(int row, int col) {
        if (!mode.equals("PLAYING") || active != player) return;
        send("SHOT|" + row + "|" + col);
    }

    private void send(String message) {
        try {
            out.writeUTF(message);
            out.flush();
        } catch (Exception e) {
            status.setText("Không gửi được dữ liệu tới Server");
        }
    }

    private void handle(String message) {
        String[] x = message.split("\\|", -1);
        switch (x[0]) {
            case "WELCOME" -> player = Integer.parseInt(x[1]);
            case "AUTH_OK" -> {
                player = Integer.parseInt(x[1]);
                status.setText("Đăng nhập thành công: " + x[2]);
                log.append("Đã đăng nhập với tài khoản " + x[2] + ".\n");
            }
            case "AUTH_FAIL" -> {
                JOptionPane.showMessageDialog(this, x[1], "Không đăng nhập được",
                        JOptionPane.ERROR_MESSAGE);
                requestAuth();
            }
            case "WAIT" -> status.setText(x[1]);
            case "INFO" -> {
                opponentConnected = true;
                alert(x[1]);
            }
            case "START", "TIME" -> alert(x[1]);
            case "PLACE_OK" -> log.append("Server xác nhận đặt tàu "
                    + x[1] + "/4.\n");
            case "TURN" -> {
                active = Integer.parseInt(x[1]);
                status.setText((active == player ? "Đến lượt bạn" : "Lượt của " + x[2])
                        + " · còn " + x[3] + " giây");
            }
            case "ATTACK_RESULT" -> {
                if (x[2].equals("MISS"))
                    log.append(x[1] + " bắn trượt; lượt chuyển cho " + x[5] + ".\n");
            }
            case "SUNK" -> alert(x[1] + " đã đánh chìm một tàu của " + x[2] + "!");
            case "WIN" -> alert("Người chiến thắng: " + x[1] + ". "
                    + x[2] + " đã hết tàu.");
            case "ERROR" -> alert(x[1]);
            case "STATE" -> {
                mode = x[1];
                active = Integer.parseInt(x[2]);
                ownState = x[4];
                enemyState = x[5];
                render();
                status.setText(mode.equals("PLAYING")
                        ? (active == player ? "Đến lượt bạn" : "Đối thủ đang đi")
                                + " · còn " + x[3] + " giây"
                        : ready ? "Chờ cả hai Ready"
                                : "Đang đặt tàu · tàu tiếp theo dài " + placement.nextLength());
            }
            default -> { }
        }
    }

    private void alert(String message) {
        log.append(message + "\n");
        JDialog dialog = new JDialog(this, "Thông báo", true);
        JLabel label = new JLabel(message, SwingConstants.CENTER);
        label.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> dialog.dispose());
        dialog.add(label, BorderLayout.CENTER);
        dialog.add(ok, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        Timer timer = new Timer(3000, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
        dialog.setVisible(true);
    }

    private void render() {
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                int i = r * 6 + c;
                String shownOwn = ready ? ownState : placement.encode();
                own[r][c].setText(switch (shownOwn.charAt(i)) {
                    case '1' -> "■";
                    case '2' -> "·";
                    case '3' -> "×";
                    default -> " ";
                });
                enemy[r][c].setText(switch (enemyState.charAt(i)) {
                    case '2' -> "·";
                    case '3' -> "×";
                    default -> " ";
                });
            }
        }
    }
}
