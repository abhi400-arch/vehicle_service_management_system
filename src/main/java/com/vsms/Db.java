package com.vsms;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;

/** All JDBC access. Every call opens a short-lived connection, so no state can leak between screens. */
public final class Db {
    private Db() {}

    // ======================================================================
    //  DATABASE SETTINGS - change these values to match your MySQL server
    // ======================================================================
    private static final String HOST = "localhost";
    private static final String PORT = "3306";
    private static final String DATABASE = "vehicle_service_db";
    private static final String USER = "root";
    private static final String PASSWORD = "Abhishekt@2006";   // <-- put your MySQL password here

    private static String url(boolean withDb) {
        return "jdbc:mysql://" + HOST + ":" + PORT + "/" + (withDb ? DATABASE : "")
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
                + "&yearIsDateType=false&connectTimeout=5000";
    }

    /** The one place where a database connection is created. */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url(true), USER, PASSWORD);
    }

    /** Connection to the server without selecting a database (used only to create the database). */
    private static Connection getServerConnection() throws SQLException {
        return DriverManager.getConnection(url(false), USER, PASSWORD);
    }

    public static String databaseName() { return DATABASE; }

    private static void bind(PreparedStatement ps, Object[] p) throws SQLException {
        for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
    }

    private static Result read(ResultSet rs) throws SQLException {
        ResultSetMetaData md = rs.getMetaData();
        int n = md.getColumnCount();
        List<String> cols = new ArrayList<>();
        for (int i = 1; i <= n; i++) cols.add(md.getColumnLabel(i));
        List<Object[]> rows = new ArrayList<>();
        while (rs.next()) {
            Object[] r = new Object[n];
            for (int i = 0; i < n; i++) r[i] = rs.getObject(i + 1);
            rows.add(r);
        }
        return new Result(cols, rows);
    }

    // ---------- plain statements (own connection) ----------

    public static Result query(String sql, Object... p) throws SQLException {
        try (Connection c = getConnection()) { return query(c, sql, p); }
    }

    public static int update(String sql, Object... p) throws SQLException {
        try (Connection c = getConnection()) { return exec(c, sql, p); }
    }

    /** Calls a stored procedure and returns the first result set it produces (its status row). */
    public static Result call(String sql, Object... p) throws SQLException {
        try (Connection c = getConnection(); CallableStatement cs = c.prepareCall(sql)) {
            bind(cs, p);
            boolean has = cs.execute();
            while (true) {
                if (has) {
                    try (ResultSet rs = cs.getResultSet()) { return read(rs); }
                } else if (cs.getUpdateCount() == -1) {
                    break;
                }
                has = cs.getMoreResults();
            }
            return new Result(List.of(), List.of());
        }
    }

    // ---------- statements inside an existing connection / transaction ----------

    public static Result query(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            try (ResultSet rs = ps.executeQuery()) { return read(rs); }
        }
    }

    public static int exec(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            bind(ps, p);
            return ps.executeUpdate();
        }
    }

    public static long insert(Connection c, String sql, Object... p) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getLong(1); }
        }
    }

    public interface Work { void run(Connection c) throws SQLException; }

    /** Runs the work as ONE transaction: COMMIT on success, ROLLBACK on any error. */
    public static void transaction(Work w) throws SQLException {
        try (Connection c = getConnection()) {
            c.setAutoCommit(false);
            try {
                w.run(c);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** Resets the AUTO_INCREMENT counter of a table to MAX(idCol) + 1 so deleted trailing IDs are reused. */
    public static void syncAutoIncrement(String table, String idCol) {
        try {
            Result r = query("SELECT COALESCE(MAX(" + idCol + "), 0) + 1 FROM " + table);
            if (!r.isEmpty() && r.first() instanceof Number n) {
                update("ALTER TABLE " + table + " AUTO_INCREMENT = " + n.longValue());
            }
        } catch (Exception ignored) {}
    }

    // ---------- SQL script runner (understands DELIMITER) ----------

    public static int runScript(String script) throws SQLException {
        try (Connection c = getServerConnection(); Statement st = c.createStatement()) {
            String delim = ";";
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (String raw : script.split("\\R")) {
                String line = raw.trim();
                if (sb.length() == 0 && (line.isEmpty() || line.startsWith("--"))) continue;
                if (sb.length() == 0 && line.toUpperCase().startsWith("DELIMITER ")) {
                    delim = line.substring(10).trim();
                    continue;
                }
                sb.append(raw).append('\n');
                if (line.endsWith(delim)) {
                    String stmt = sb.toString().trim();
                    stmt = stmt.substring(0, stmt.length() - delim.length()).trim();
                    sb.setLength(0);
                    if (!stmt.isEmpty()) { st.execute(stmt); count++; }
                }
            }
            return count;
        }
    }

    /** Creates the database from the bundled script (used automatically on the very first start). */
    public static int runBundledScript() throws SQLException, IOException {
        try (InputStream in = Db.class.getResourceAsStream("/vehicle_service_db.sql")) {
            if (in == null) throw new IOException("Bundled vehicle_service_db.sql not found.");
            return runScript(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public static void testConnection() throws SQLException {
        query("SELECT COUNT(*) FROM CUSTOMER");
    }
}
