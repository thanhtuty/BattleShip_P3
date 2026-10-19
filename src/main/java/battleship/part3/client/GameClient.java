package battleship.part3.client;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Kết nối TCP phía Client, tương thích giao thức readUTF/writeUTF của Server. */
public final class GameClient implements Closeable {
    private final Socket socket;
    private final DataInputStream input;
    private final DataOutputStream output;

    public GameClient(String host, int port) throws IOException {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Địa chỉ Server không được để trống.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Cổng phải nằm trong khoảng 1–65535.");
        }
        socket = new Socket();
        socket.connect(new InetSocketAddress(host.trim(), port), 5000);
        input = new DataInputStream(socket.getInputStream());
        output = new DataOutputStream(socket.getOutputStream());
    }

    public synchronized void send(String message) throws IOException {
        if (message == null) {
            throw new IllegalArgumentException("Thông điệp không được null.");
        }
        output.writeUTF(message);
        output.flush();
    }

    public String receive() throws IOException {
        return input.readUTF();
    }

    public boolean isConnected() {
        return socket.isConnected() && !socket.isClosed();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
