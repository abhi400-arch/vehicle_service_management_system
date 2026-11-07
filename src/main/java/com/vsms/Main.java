package com.vsms;

import javax.swing.*;
import java.sql.SQLException;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Ui.installLookAndFeel();
            if (prepareDatabase()) new LoginFrame().setVisible(true);
            else System.exit(1);
        });
    }

    /** Connects using Db.getConnection(); on the very first run offers to create the database. */
    private static boolean prepareDatabase() {
        try {
            try {
                Db.testConnection();
            } catch (SQLException e) {
                boolean missing = e.getErrorCode() == 1049 || e.getErrorCode() == 1146;
                if (!missing) throw e;
                int ok = JOptionPane.showConfirmDialog(null,
                        "The database '" + Db.databaseName() + "' was not found.\n"
                        + "Create it now from the bundled SQL script (with sample data)?",
                        "First run", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (ok != JOptionPane.YES_OPTION) return false;
                Db.runBundledScript();
            }
            Auth.ensureSchema();
            return true;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Cannot connect to the database.\n\n" + Ui.friendly(e)
                    + "\n\nCheck that MySQL is running and that the settings at the top of Db.java are correct.",
                    "Database error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
