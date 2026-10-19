package battleship.part3;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
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
import java.util.ArrayList;
import java.util.List;

public final class Client extends JFrame {
    private static final Color BG = new Color(7, 17, 31);
    private static final Color PANEL = new Color(13, 29, 47);
    private static final Color PANEL_LIGHT = new Color(18, 39, 61);
    private static final Color LINE = new Color(35, 61, 82);
    private static final Color TEXT = new Color(230, 241, 249);
    private static final Color MUTED = new Color(143, 165, 183);
    private static final Color TEAL = new Color(37, 211, 190);
    private static final Color RED = new Color(255, 104, 105);
    private static final Color WATER = new Color(17, 41, 62);
    private static final Color SHIP = new Color(35, 105, 139);
    private static final Color MISS = new Color(37, 59, 76);

    private final JButton[][] own = new JButton[6][6];
    private final JButton[][] enemy = new JButton[6][6];
    private final JLabel status = new JLabel("Đang kết nối...");
    private final JLabel statusDot = new JLabel("●");
    private final JLabel fleetProgress = new JLabel("0 / 4 tàu");
    private final JTextArea log = new JTextArea();
    private final Board placement = new Board();
    private final List<String> placements = new ArrayList<>();
    private JButton directionButton;
    private JButton readyButton;

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
        super("BATTLESHIP  /  Naval Arena");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 760));
        setSize(1160, 820);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(22, 26, 20, 26));
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBoards(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        connect();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(18, 0));
        header.setOpaque(false);

        JPanel brand = new JPanel(new BorderLayout(0, 4));
        brand.setOpaque(false);
        JLabel title = label("BATTLESHIP", 25, TEAL, Font.BOLD);
        JLabel subtitle = label("NAVAL ARENA   /   TRẬN ĐẤU 6 × 6", 11, MUTED, Font.BOLD);
        brand.add(title, BorderLayout.NORTH);
        brand.add(subtitle, BorderLayout.SOUTH);
        header.add(brand, BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout(12, 0));
        right.setOpaque(false);
        JPanel statusCard = new RoundedPanel(PANEL, 16);
        statusCard.setLayout(new BorderLayout(9, 0));
        statusCard.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        statusDot.setForeground(TEAL);
        statusDot.setFont(new Font("SansSerif", Font.BOLD, 12));
        status.setForeground(TEXT);
        status.setFont(new Font("SansSerif", Font.BOLD, 13));
        statusCard.add(statusDot, BorderLayout.WEST);
        statusCard.add(status, BorderLayout.CENTER);
        right.add(statusCard, BorderLayout.CENTER);

        JPanel controls = new JPanel(new GridLayout(1, 2, 8, 0));
        controls.setOpaque(false);
        directionButton = styledButton("↔  NGANG", PANEL_LIGHT, TEXT);
        directionButton.addActionListener(e -> {
            horizontal = !horizontal;
            directionButton.setText(horizontal ? "↔  NGANG" : "↕  DỌC");
        });
        readyButton = styledButton("SẴN SÀNG", TEAL, BG);
        readyButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        readyButton.setEnabled(false);
        readyButton.addActionListener(e -> ready());
        controls.add(directionButton);
        controls.add(readyButton);
        right.add(controls, BorderLayout.EAST);
        header.add(right, BorderLayout.CENTER);
        return header;
    }

    private JPanel buildBoards() {
        JPanel boards = new JPanel(new GridLayout(1, 2, 18, 0));
        boards.setOpaque(false);
        boards.add(makeBoardCard("01", "HẠM ĐỘI CỦA BẠN",
                "Đặt tàu theo thứ tự 2, 3, 4, 5 ô", own, true));
        boards.add(makeBoardCard("02", "VÙNG BIỂN ĐỐI THỦ",
                "Chọn tọa độ để khai hỏa khi đến lượt", enemy, false));
        return boards;
    }

    private JPanel makeBoardCard(String number, String title, String description,
                                 JButton[][] cells, boolean mine) {
        RoundedPanel card = new RoundedPanel(PANEL, 20);
        card.setLayout(new BorderLayout(0, 14));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 1, true),
                BorderFactory.createEmptyBorder(18, 18, 15, 18)));

        JPanel heading = new JPanel(new BorderLayout(12, 0));
        heading.setOpaque(false);
        JLabel badge = new JLabel(number, SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setBackground(PANEL_LIGHT);
        badge.setForeground(TEAL);
        badge.setFont(new Font("SansSerif", Font.BOLD, 12));
        badge.setPreferredSize(new Dimension(38, 38));
        heading.add(badge, BorderLayout.WEST);

        JPanel names = new JPanel(new BorderLayout(0, 5));
        names.setOpaque(false);
        names.add(label(title, 14, TEXT, Font.BOLD), BorderLayout.NORTH);
        names.add(label(description, 11, MUTED, Font.PLAIN), BorderLayout.SOUTH);
        heading.add(names, BorderLayout.CENTER);
        if (mine) {
            fleetProgress.setForeground(TEAL);
            fleetProgress.setFont(new Font("SansSerif", Font.BOLD, 12));
            heading.add(fleetProgress, BorderLayout.EAST);
        }
        card.add(heading, BorderLayout.NORTH);

        JPanel ocean = new JPanel(new GridLayout(7, 7, 5, 5));
        ocean.setOpaque(false);
        ocean.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        ocean.add(new JLabel(""));
        for (int c = 1; c <= 6; c++) ocean.add(axisLabel(String.valueOf(c)));
        for (int r = 0; r < 6; r++) {
            ocean.add(axisLabel(String.valueOf((char) ('A' + r))));
            for (int c = 0; c < 6; c++) {
                final int row = r, col = c;
                JButton cell = new JButton("");
                cell.setPreferredSize(new Dimension(60, 56));
                cell.setFont(new Font("SansSerif", Font.BOLD, 19));
                cell.setForeground(TEXT);
                cell.setBackground(WATER);
                cell.setOpaque(true);
                cell.setFocusPainted(false);
                cell.setBorder(BorderFactory.createLineBorder(LINE, 1, true));
                cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                cell.addActionListener(e -> {
                    if (mine) place(row, col); else shoot(row, col);
                });
                cells[r][c] = cell;
                ocean.add(cell);
            }
        }
        card.add(ocean, BorderLayout.CENTER);

        JPanel legend = new JPanel(new GridLayout(1, 3, 6, 0));
        legend.setOpaque(false);
        legend.add(legendItem("■", "Tàu", SHIP));
        legend.add(legendItem("×", "Trúng", RED));
        legend.add(legendItem("·", "Trượt", MUTED));
        card.add(legend, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(16, 0));
        footer.setOpaque(false);

        JPanel help = new RoundedPanel(PANEL, 16);
        help.setLayout(new BorderLayout(0, 6));
        help.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        help.add(label("MỤC TIÊU", 10, TEAL, Font.BOLD), BorderLayout.NORTH);
        help.add(label("<html>Đánh chìm toàn bộ tàu đối thủ.<br>Trúng thì bắn tiếp; trượt thì đổi lượt.</html>",
                12, TEXT, Font.PLAIN), BorderLayout.CENTER);
        help.setPreferredSize(new Dimension(270, 78));
        footer.add(help, BorderLayout.WEST);

        RoundedPanel logCard = new RoundedPanel(PANEL, 16);
        logCard.setLayout(new BorderLayout(0, 6));
        logCard.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        logCard.add(label("NHẬT KÝ TRẬN ĐẤU", 10, MUTED, Font.BOLD), BorderLayout.NORTH);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setRows(3);
        log.setForeground(new Color(190, 210, 224));
        log.setBackground(PANEL);
        log.setCaretColor(TEAL);
        log.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(PANEL);
        logCard.add(scroll, BorderLayout.CENTER);
        footer.add(logCard, BorderLayout.CENTER);
        return footer;
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
                    SwingUtilities.invokeLater(() -> {
                        status.setText("Mất kết nối với Server");
                        statusDot.setForeground(RED);
                    });
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
        int action = JOptionPane.showOptionDialog(this, "Chọn cách vào trò chơi",
                "Tài khoản BattleShip", JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, choices, choices[0]);
        if (action < 0 || action == 2) {
            dispose();
            return;
        }

        String username = JOptionPane.showInputDialog(this,
                "Tên tài khoản (3–50 ký tự, chữ/số/_):", "abc1");
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
            fleetProgress.setText(placement.placed() + " / 4 tàu");
            log.append("Đã đặt tàu dài " + length + " ô.\n");
            if (placement.ready()) {
                readyButton.setEnabled(true);
                status.setText("Đã đặt đủ tàu. Nhấn SẴN SÀNG để bắt đầu.");
            }
        } catch (Exception e) {
            showMessage(e.getMessage(), "Vị trí tàu không hợp lệ");
        }
    }

    private void ready() {
        if (!placement.ready() || ready) return;
        ready = true;
        readyButton.setEnabled(false);
        directionButton.setEnabled(false);
        send("READY");
        status.setText("Đã sẵn sàng, đang chờ đối thủ...");
        log.append("Bạn đã sẵn sàng.\n");
        render();
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
            statusDot.setForeground(RED);
        }
    }

    private void handle(String message) {
        String[] x = message.split("\\|", -1);
        switch (x[0]) {
            case "WELCOME" -> player = Integer.parseInt(x[1]);
            case "AUTH_OK" -> {
                player = Integer.parseInt(x[1]);
                status.setText("Đã đăng nhập: " + x[2]);
                log.append("Đã đăng nhập với tài khoản " + x[2] + ".\n");
            }
            case "AUTH_FAIL" -> {
                showMessage(x[1], "Không đăng nhập được");
                requestAuth();
            }
            case "WAIT" -> status.setText(x[1]);
            case "INFO" -> {
                opponentConnected = true;
                statusDot.setForeground(TEAL);
                alert(x[1]);
            }
            case "START", "TIME" -> alert(x[1]);
            case "PLACE_OK" -> log.append("Server xác nhận đặt tàu "
                    + x[1] + "/4.\n");
            case "TURN" -> {
                active = Integer.parseInt(x[1]);
                status.setText((active == player ? "ĐẾN LƯỢT BẠN" : "LƯỢT CỦA " + x[2])
                        + "   ·   " + x[3] + " giây");
                statusDot.setForeground(active == player ? TEAL : MUTED);
            }
            case "ATTACK_RESULT" -> { /* Kết quả thể hiện trên bàn cờ, không hiện thông báo trúng/trượt. */ }
            case "SUNK" -> alert(x[1] + " đã đánh chìm một tàu của " + x[2] + "!");
            case "WIN" -> alert("Người chiến thắng: " + x[1] + ". "
                    + x[2] + " đã hết tàu.");
            case "ERROR" -> showMessage(x[1], "Thông báo");
            case "STATE" -> {
                mode = x[1];
                active = Integer.parseInt(x[2]);
                ownState = x[4];
                enemyState = x[5];
                render();
                status.setText(mode.equals("PLAYING")
                        ? (active == player ? "ĐẾN LƯỢT BẠN" : "ĐỐI THỦ ĐANG ĐI")
                                + "   ·   " + x[3] + " giây"
                        : ready ? "Đã sẵn sàng · chờ đối thủ"
                                : "Đang đặt tàu · tàu tiếp theo dài "
                                        + placement.nextLength() + " ô");
                statusDot.setForeground(mode.equals("PLAYING") && active == player ? TEAL : MUTED);
            }
            default -> { }
        }
    }

    private void render() {
        for (int r = 0; r < 6; r++) {
            for (int c = 0; c < 6; c++) {
                int i = r * 6 + c;
                String shownOwn = ready ? ownState : placement.encode();
                styleCell(own[r][c], shownOwn.charAt(i), true);
                styleCell(enemy[r][c], enemyState.charAt(i), false);
            }
        }
    }

    private void styleCell(JButton cell, char value, boolean ownBoard) {
        cell.setText(switch (value) {
            case '1' -> "■";
            case '2' -> "·";
            case '3' -> "×";
            default -> "";
        });
        cell.setForeground(value == '3' ? RED : value == '2' ? MUTED : TEXT);
        cell.setBackground(switch (value) {
            case '1' -> SHIP;
            case '2' -> MISS;
            case '3' -> new Color(75, 39, 51);
            default -> WATER;
        });
        cell.setBorder(BorderFactory.createLineBorder(
                value == '3' ? RED : value == '1' ? new Color(53, 146, 174) : LINE, 1, true));
        cell.setToolTipText("Ô " + (char) ('A' + cellRow(cell)) + (cellColumn(cell) + 1));
    }

    private int cellRow(JButton target) {
        for (int r = 0; r < 6; r++) for (int c = 0; c < 6; c++)
            if (own[r][c] == target || enemy[r][c] == target) return r;
        return 0;
    }

    private int cellColumn(JButton target) {
        for (int r = 0; r < 6; r++) for (int c = 0; c < 6; c++)
            if (own[r][c] == target || enemy[r][c] == target) return c;
        return 0;
    }

    private JLabel axisLabel(String text) {
        JLabel label = label(text, 11, MUTED, Font.BOLD);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private JPanel legendItem(String symbol, String text, Color color) {
        JPanel item = new JPanel(new BorderLayout(7, 0));
        item.setOpaque(false);
        JLabel mark = label(symbol, 15, color, Font.BOLD);
        item.add(mark, BorderLayout.WEST);
        item.add(label(text, 11, MUTED, Font.PLAIN), BorderLayout.CENTER);
        return item;
    }

    private JButton styledButton(String text, Color background, Color foreground) {
        JButton button = new JButton(text);
        button.setOpaque(true);
        button.setBackground(background);
        button.setForeground(foreground);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(11, 15, 11, 15));
        button.setFont(new Font("SansSerif", Font.BOLD, 11));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JLabel label(String text, int size, Color color, int style) {
        JLabel label = new JLabel(text);
        label.setForeground(color);
        label.setFont(new Font("SansSerif", style, size));
        return label;
    }

    private void alert(String message) {
        log.append(message + "\n");
        showMessage(message, "BattleShip");
    }

    private void showMessage(String message, String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setUndecorated(true);
        RoundedPanel body = new RoundedPanel(PANEL_LIGHT, 18);
        body.setLayout(new BorderLayout(0, 16));
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINE, 1, true),
                BorderFactory.createEmptyBorder(20, 24, 16, 24)));
        JLabel text = label("<html><div style='text-align:center;width:300px'>"
                + message + "</div></html>", 14, TEXT, Font.BOLD);
        text.setHorizontalAlignment(SwingConstants.CENTER);
        body.add(text, BorderLayout.CENTER);
        JButton ok = styledButton("ĐỒNG Ý", TEAL, BG);
        ok.addActionListener(e -> dialog.dispose());
        body.add(ok, BorderLayout.SOUTH);
        dialog.setContentPane(body);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        Timer timer = new Timer(3000, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
        dialog.setVisible(true);
    }

    private static final class RoundedPanel extends JPanel {
        private final Color color;
        private final int radius;
        RoundedPanel(Color color, int radius) {
            this.color = color;
            this.radius = radius;
            setOpaque(false);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(color);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g.dispose();
            super.paintComponent(graphics);
        }
    }
}
