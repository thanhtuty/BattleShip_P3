package battleship.part3;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Tài khoản dùng PBKDF2, mật khẩu không được lưu dạng chữ thường. */
public final class AccountDAO {
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();
    private final DbConfig db;

    public AccountDAO(DbConfig db) { this.db = db; }

    public void ensureDemoAccounts() throws SQLException {
        createIfMissing("abc1", "abc12345");
        createIfMissing("abc2", "abc12345");
    }

    private void createIfMissing(String username, String password) throws SQLException {
        if (exists(username)) return;
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = hash(password.toCharArray(), salt, ITERATIONS);
        try (Connection c = db.connect();
             PreparedStatement p = c.prepareStatement(
                 "IF NOT EXISTS (SELECT 1 FROM dbo.UserAccounts WHERE Username=?) " +
                 "INSERT dbo.UserAccounts(Username,PasswordSalt,PasswordHash,Iterations) VALUES(?,?,?,?)")) {
            p.setString(1, username);
            p.setString(2, username);
            p.setBytes(3, salt);
            p.setBytes(4, hash);
            p.setInt(5, ITERATIONS);
            p.executeUpdate();
        }
    }

    public boolean register(String username, String password) throws SQLException {
        String name = normalize(username);
        validate(name, password);
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = hash(password.toCharArray(), salt, ITERATIONS);
        try (Connection c = db.connect();
             PreparedStatement p = c.prepareStatement(
                 "INSERT dbo.UserAccounts(Username,PasswordSalt,PasswordHash,Iterations) VALUES(?,?,?,?)")) {
            p.setString(1, name);
            p.setBytes(2, salt);
            p.setBytes(3, hash);
            p.setInt(4, ITERATIONS);
            p.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getErrorCode() == 2627 || e.getErrorCode() == 2601) return false;
            throw e;
        }
    }

    public boolean authenticate(String username, String password) throws SQLException {
        String name = normalize(username);
        if (name.isBlank() || password == null) return false;
        try (Connection c = db.connect();
             PreparedStatement p = c.prepareStatement(
                 "SELECT PasswordSalt,PasswordHash,Iterations FROM dbo.UserAccounts WHERE Username=?")) {
            p.setString(1, name);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) return false;
                byte[] salt = r.getBytes(1);
                byte[] expected = r.getBytes(2);
                int iterations = r.getInt(3);
                byte[] actual = hash(password.toCharArray(), salt, iterations);
                boolean matches = MessageDigest.isEqual(expected, actual);
                Arrays.fill(actual, (byte) 0);
                return matches;
            }
        }
    }

    private boolean exists(String username) throws SQLException {
        try (Connection c = db.connect();
             PreparedStatement p = c.prepareStatement(
                 "SELECT 1 FROM dbo.UserAccounts WHERE Username=?")) {
            p.setString(1, username);
            try (ResultSet r = p.executeQuery()) { return r.next(); }
        }
    }

    private static String normalize(String username) {
        return username == null ? "" : username.trim();
    }

    private static void validate(String username, String password) {
        if (!username.matches("[A-Za-z0-9_]{3,50}"))
            throw new IllegalArgumentException("Tên tài khoản phải dài 3–50 ký tự, chỉ gồm chữ, số hoặc dấu gạch dưới.");
        if (password == null || password.length() < 8)
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 8 ký tự.");
    }

    private static byte[] hash(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, HASH_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("JDK không hỗ trợ PBKDF2WithHmacSHA256", e);
        } finally {
            spec.clearPassword();
            Arrays.fill(password, '\0');
        }
    }
}
