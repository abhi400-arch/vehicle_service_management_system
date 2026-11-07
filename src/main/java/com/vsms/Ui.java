package com.vsms;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Theme, reusable widgets and small validation helpers. */
public final class Ui {
    private Ui() {}

    public static final Color NAVY = new Color(0x0F2A43), NAVY2 = new Color(0x1E3A5F),
            ACCENT = new Color(0x2563EB), BG = new Color(0xF3F5F9), TEXT = new Color(0x1F2937),
            MUTED = new Color(0x6B7280), GREEN = new Color(0x16A34A), AMBER = new Color(0xD97706),
            RED = new Color(0xDC2626), SLATE = new Color(0x475569), LINE = new Color(0xE5E7EB);

    private static final Map<String, Color> STATUS = Map.ofEntries(
            Map.entry("Completed", GREEN), Map.entry("Paid", GREEN), Map.entry("OK", GREEN),
            Map.entry("PASS", GREEN), Map.entry("Available", GREEN),
            Map.entry("In Service", AMBER), Map.entry("In Progress", AMBER), Map.entry("Pending", AMBER),
            Map.entry("Partial", AMBER), Map.entry("Open", ACCENT),
            Map.entry("Cancelled", RED), Map.entry("Failed", RED), Map.entry("LOW", RED), Map.entry("FAIL", RED),
            Map.entry("Inactive", MUTED), Map.entry("Refunded", MUTED));

    public static Font font(int style, int size) { return new Font("Segoe UI", style, size); }

    public static void installLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo i : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(i.getName())) { UIManager.setLookAndFeel(i.getClassName()); break; }
            }
        } catch (Exception ignored) { }
        UIManager.put("defaultFont", font(Font.PLAIN, 13));
        UIManager.put("nimbusSelection", ACCENT);
        UIManager.put("OptionPane.messageFont", font(Font.PLAIN, 13));
        UIManager.put("OptionPane.buttonFont", font(Font.BOLD, 12));
    }

    // ------------------------------------------------------------------ widgets

    public static JLabel label(String text, int size, boolean bold, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(font(bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(c);
        return l;
    }

    public static JTextField field(int cols) {
        JTextField f = new JTextField(cols);
        f.setFont(font(Font.PLAIN, 13));
        f.setBorder(new CompoundBorder(new LineBorder(new Color(0xD1D5DB)), new EmptyBorder(5, 8, 5, 8)));
        return f;
    }

    public static JButton button(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = !isEnabled() ? new Color(0xC7CDD6)
                        : getModel().isPressed() ? bg.darker()
                        : getModel().isRollover() ? bg.brighter() : bg;
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(false);
        b.setForeground(Color.WHITE);
        b.setFont(font(Font.BOLD, 12));
        b.setBorder(new EmptyBorder(8, 16, 8, 16));
        b.setMinimumSize(new Dimension(90, 36));
        b.setPreferredSize(new Dimension(Math.max(96, b.getPreferredSize().width + 16), 36));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static JPanel card(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(new LineBorder(LINE), new EmptyBorder(14, 16, 14, 16)));
        return p;
    }

    public static void onChange(JTextField f, Runnable r) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    // ------------------------------------------------------------------ tables

    public static JTable table() {
        JTable t = new JTable();
        t.setRowHeight(32);
        t.setFont(font(Font.PLAIN, 13));
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setSelectionBackground(new Color(0xDBEAFE));
        t.setSelectionForeground(TEXT);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        t.setDefaultRenderer(Object.class, new CellRenderer());
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setPreferredSize(new Dimension(100, 34));
        h.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object value, boolean selected,
                                                            boolean focused, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        tbl, value, selected, focused, row, column);
                l.setOpaque(true);
                l.setBackground(NAVY2);
                l.setForeground(Color.WHITE);
                l.setFont(font(Font.BOLD, 12));
                // Keep every header directly aligned with the values underneath it:
                // text/status columns = left, numeric columns = right.
                l.setHorizontalAlignment(columnAlignment(tbl, column));
                l.setBorder(new EmptyBorder(0, 10, 0, 10));
                return l;
            }
        });
        return t;
    }

    public static DefaultTableModel toModel(Result r) {
        DefaultTableModel m = new DefaultTableModel(r.columns().toArray(), 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        for (Object[] row : r.rows()) m.addRow(row);
        return m;
    }

    private static int columnAlignment(JTable t, int viewColumn) {
        if (t.getColumnCount() == 0 || viewColumn < 0 || viewColumn >= t.getColumnCount()) {
            return SwingConstants.LEFT;
        }

        int modelColumn = t.convertColumnIndexToModel(viewColumn);
        TableModel model = t.getModel();

        // Prefer the declared column class when a custom model provides one.
        Class<?> type = model.getColumnClass(modelColumn);
        if (type != null && Number.class.isAssignableFrom(type)) {
            return SwingConstants.RIGHT;
        }

        // DefaultTableModel reports Object.class, so inspect the actual values.
        for (int r = 0; r < Math.min(model.getRowCount(), 80); r++) {
            Object value = model.getValueAt(r, modelColumn);
            if (value == null) continue;
            if (value instanceof Number || value instanceof BigDecimal) {
                return SwingConstants.RIGHT;
            }
            return SwingConstants.LEFT;
        }
        return SwingConstants.LEFT;
    }

    public static void fit(JTable t) {
        if (t.getColumnCount() == 0) return;
        TableColumnModel cm = t.getColumnModel();
        FontMetrics fm = t.getFontMetrics(t.getFont());
        t.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        for (int c = 0; c < cm.getColumnCount(); c++) {
            int w = fm.stringWidth(String.valueOf(t.getColumnName(c))) + 36;
            for (int r = 0; r < Math.min(t.getRowCount(), 80); r++) {
                Object v = t.getValueAt(r, c);
                String text = v == null ? "NULL" : String.valueOf(v);
                w = Math.max(w, fm.stringWidth(text) + 28);
            }
            // Keep columns readable without allowing one long value to consume
            // the entire table. AUTO_RESIZE_SUBSEQUENT_COLUMNS then uses any
            // remaining screen width to keep the table visually balanced.
            int min = c == 0 ? 72 : 90;
            int max = 260;
            cm.getColumn(c).setMinWidth(min);
            cm.getColumn(c).setPreferredWidth(Math.min(Math.max(w, min), max));
        }
    }

    /** Modal dialog that simply shows a result set. */
    public static void showTable(Component owner, String title, Result r) {
        JTable t = table();
        t.setModel(toModel(r));
        fit(t);
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(new LineBorder(LINE));
        sp.setPreferredSize(new Dimension(760, 300));
        JOptionPane.showMessageDialog(owner, sp, title, JOptionPane.PLAIN_MESSAGE);
    }

    private static final class CellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            l.setBorder(new EmptyBorder(0, 10, 0, 10));
            l.setFont(font(Font.PLAIN, 13));
            l.setForeground(TEXT);
            l.setHorizontalAlignment(SwingConstants.LEFT);
            if (!sel) l.setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
            if (v == null) {
                l.setText("NULL");
                l.setForeground(new Color(0x9CA3AF));
                l.setFont(font(Font.ITALIC, 12));
            } else if (v instanceof BigDecimal bd) {
                l.setText(String.format("%,.2f", bd));
                l.setHorizontalAlignment(SwingConstants.RIGHT);
            } else if (v instanceof Number) {
                l.setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                Color sc = STATUS.get(v.toString());
                if (sc != null) { l.setForeground(sc); l.setFont(font(Font.BOLD, 12)); }
            }
            return l;
        }
    }

    // ------------------------------------------------------------------ dialogs / errors

    public interface Action { void run() throws Exception; }

    public static void info(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Information", JOptionPane.INFORMATION_MESSAGE); }
    public static void warn(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Please check", JOptionPane.WARNING_MESSAGE); }
    public static void error(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Error", JOptionPane.ERROR_MESSAGE); }
    public static boolean confirm(Component p, String m) {
        return JOptionPane.showConfirmDialog(p, m, "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** Runs an action, turning every failure into a readable dialog. Returns true on success. */
    public static boolean run(Component parent, Action a) {
        try {
            a.run();
            return true;
        } catch (IllegalArgumentException e) {
            warn(parent, e.getMessage());
        } catch (Exception e) {
            error(parent, friendly(e));
        }
        return false;
    }

    static String friendly(Throwable e) {
        String m = e.getMessage() == null ? e.toString() : e.getMessage();
        String low = m.toLowerCase();
        if (low.contains("foreign key constraint fails"))
            return "This record is referenced by other records, so it cannot be deleted or changed.";
        if (low.contains("duplicate entry"))
            return "Duplicate value: a record with the same unique value (registration number or e-mail) already exists.";
        if (low.contains("check constraint"))
            return "The database rejected the value (for example a negative amount or an invalid status).";
        if (e instanceof SQLException se && se.getSQLState() != null && se.getSQLState().startsWith("08"))
            return "Cannot reach the database server. Is MySQL running?\n" + m;
        return m;
    }

    // ------------------------------------------------------------------ helpers

    public record Item(long id, String label) {
        @Override public String toString() { return label; }
    }

    public static List<Item> items(String sql, Object... p) throws SQLException {
        List<Item> l = new ArrayList<>();
        for (Object[] r : Db.query(sql, p).rows()) l.add(new Item(((Number) r[0]).longValue(), s(r[1])));
        return l;
    }

    public static Item find(List<Item> l, Object id) {
        if (id == null) return null;
        long v = ((Number) id).longValue();
        for (Item i : l) if (i.id() == v) return i;
        return null;
    }

    public static String s(Object o) { return o == null ? "" : o.toString(); }
    public static String nn(String s) { return s == null || s.isBlank() ? null : s.trim(); }

    public static String money(Object o) {
        BigDecimal b = o instanceof BigDecimal bd ? bd : new BigDecimal(s(o).isEmpty() ? "0" : o.toString());
        return "\u20B9" + String.format("%,.2f", b);
    }

    public static int parseInt(String label, String s, int min, int max) {
        try {
            int v = Integer.parseInt(s.trim());
            if (v < min || v > max) throw new IllegalArgumentException(label + " must be between " + min + " and " + max + ".");
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number.");
        }
    }

    public static BigDecimal parseMoney(String label, String s) {
        try {
            BigDecimal v = new BigDecimal(s.trim().replace(",", ""));
            if (v.signum() < 0) throw new IllegalArgumentException(label + " cannot be negative.");
            return v.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a valid amount.");
        }
    }

    public static java.sql.Date parseDate(String label, String s) {
        try {
            return java.sql.Date.valueOf(java.time.LocalDate.parse(s.trim()));
        } catch (Exception e) {
            throw new IllegalArgumentException(label + " must be a date in yyyy-MM-dd format.");
        }
    }

    public static void checkPhone(String phone) {
        if (phone != null && !Pattern.matches("[0-9+\\- ]{7,15}", phone))
            throw new IllegalArgumentException("Phone must contain 7-15 digits.");
    }

    public static void checkEmail(String email) {
        if (email != null && !Pattern.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+", email))
            throw new IllegalArgumentException("Enter a valid e-mail address.");
    }
}
