package com.vsms;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Base64;

/** Application login. Users live in the APP_USER table; passwords are stored as salted PBKDF2 hashes. */
final class Auth {
    private Auth() {}

    record User(long id, String username, String fullName, String role) {}

    private static User current;

    static User current() { return current; }
    static void logout() { current = null; }

    /** Creates the APP_USER table (if missing) and the default admin account on first use. */
    static void ensureSchema() throws SQLException {
        Db.update("CREATE TABLE IF NOT EXISTS APP_USER ("
                + "user_id INT AUTO_INCREMENT PRIMARY KEY, "
                + "username VARCHAR(50) NOT NULL UNIQUE, "
                + "full_name VARCHAR(100) NOT NULL, "
                + "role VARCHAR(20) NOT NULL DEFAULT 'Staff', "
                + "salt VARCHAR(64) NOT NULL, "
                + "password_hash VARCHAR(128) NOT NULL) ENGINE=InnoDB");
        long n = ((Number) Db.query("SELECT COUNT(*) FROM APP_USER").first()).longValue();
        if (n == 0) createUser("admin", "Administrator", "Admin", "admin123");
    }

    static void createUser(String username, String fullName, String role, String password) throws SQLException {
        String salt = newSalt();
        Db.update("INSERT INTO APP_USER (username, full_name, role, salt, password_hash) VALUES (?,?,?,?,?)",
                username, fullName, role, salt, hash(password, salt));
    }

    /** Returns true and stores the session user when the credentials are correct. */
    static boolean login(String username, String password) throws SQLException {
        Result r = Db.query("SELECT user_id, username, full_name, role, salt, password_hash FROM APP_USER WHERE username = ?", username);
        if (r.isEmpty()) { hash(password, newSalt()); return false; }   // same work whether or not the user exists
        Object[] row = r.rows().get(0);
        if (!matches(password, (String) row[4], (String) row[5])) return false;
        current = new User(((Number) row[0]).longValue(), (String) row[1], (String) row[2], (String) row[3]);
        return true;
    }

    static void changePassword(String oldPassword, String newPassword) throws SQLException {
        Result r = Db.query("SELECT salt, password_hash FROM APP_USER WHERE user_id = ?", current.id());
        Object[] row = r.rows().get(0);
        if (!matches(oldPassword, (String) row[0], (String) row[1]))
            throw new IllegalArgumentException("The current password is incorrect.");
        if (newPassword.length() < 6) throw new IllegalArgumentException("The new password must have at least 6 characters.");
        String salt = newSalt();
        Db.update("UPDATE APP_USER SET salt = ?, password_hash = ? WHERE user_id = ?", salt, hash(newPassword, salt), current.id());
    }

    private static boolean matches(String password, String salt, String stored) {
        return MessageDigest.isEqual(hash(password, salt).getBytes(StandardCharsets.UTF_8), stored.getBytes(StandardCharsets.UTF_8));
    }

    private static String newSalt() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }

    private static String hash(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), 65536, 256);
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(h);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
