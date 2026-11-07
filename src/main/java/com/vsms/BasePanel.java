package com.vsms;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.regex.Pattern;

/** Common layout: title, action buttons, filters, search box, table and a status line. */
abstract class BasePanel extends JPanel implements Refreshable {
    protected final JTable table = Ui.table();
    protected final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    protected final JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    protected final JTextField search = Ui.field(18);
    protected final JLabel subtitle;
    protected final JLabel summary = Ui.label("", 13, true, Ui.TEXT);
    private final JLabel count = Ui.label("", 12, false, Ui.MUTED);
    private TableRowSorter<TableModel> sorter;

    BasePanel(String title, String sub) {
        super(new BorderLayout(0, 14));
        setBackground(Ui.BG);
        setBorder(new EmptyBorder(22, 28, 18, 28));

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        JLabel t = Ui.label(title, 24, true, Ui.TEXT);
        subtitle = Ui.label(sub, 13, false, Ui.MUTED);
        t.setAlignmentX(LEFT_ALIGNMENT);
        subtitle.setAlignmentX(LEFT_ALIGNMENT);
        north.add(t);
        north.add(subtitle);
        north.add(Box.createVerticalStrut(14));

        actions.setOpaque(false);
        filters.setOpaque(false);
        JPanel bar = new JPanel(new BorderLayout(18, 0));
        bar.setOpaque(false);
        bar.setAlignmentX(LEFT_ALIGNMENT);
        actions.setBorder(new EmptyBorder(0, 0, 0, 0));
        bar.add(actions, BorderLayout.CENTER);

        JPanel sp = new JPanel(new BorderLayout(8, 0));
        sp.setOpaque(false);
        sp.setPreferredSize(new Dimension(300, 38));
        sp.add(Ui.label("Search", 12, false, Ui.MUTED), BorderLayout.WEST);
        sp.add(search, BorderLayout.CENTER);
        bar.add(sp, BorderLayout.EAST);
        north.add(bar);
        north.add(Box.createVerticalStrut(10));

        JPanel filterBar = new JPanel(new BorderLayout());
        filterBar.setOpaque(false);
        filterBar.setAlignmentX(LEFT_ALIGNMENT);
        filters.setBorder(new EmptyBorder(0, 2, 0, 0));
        filterBar.add(filters, BorderLayout.WEST);
        north.add(filterBar);
        add(north, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new LineBorder(Ui.LINE));
        scroll.getViewport().setBackground(Color.WHITE);
        add(scroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(summary, BorderLayout.WEST);
        south.add(count, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        Ui.onChange(search, this::applySearch);
    }

    protected abstract Result fetch() throws Exception;

    @Override public void refresh() { Ui.run(this, () -> show(fetch())); }

    protected JButton action(String text, Color color, Runnable r) {
        JButton b = Ui.button(text, color);
        b.addActionListener(e -> r.run());
        actions.add(b);
        return b;
    }

    protected void show(Result r) {
        DefaultTableModel m = Ui.toModel(r);
        table.setModel(m);
        sorter = new TableRowSorter<>(m);
        table.setRowSorter(sorter);
        applySearch();
        Ui.fit(table);
    }

    private void applySearch() {
        if (sorter == null) return;
        String q = search.getText().trim();
        sorter.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
        count.setText(table.getRowCount() + " record(s)");
    }

    /** Value of a column in the selected row, or null. */
    protected Object cell(String column) {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        int idx = table.getModel().getColumnCount() == 0 ? -1 : ((DefaultTableModel) table.getModel()).findColumn(column);
        return idx < 0 ? null : table.getModel().getValueAt(table.convertRowIndexToModel(row), idx);
    }

    /** First column of the selected row (the primary key in every screen). */
    protected long id() {
        int row = table.getSelectedRow();
        return ((Number) table.getModel().getValueAt(table.convertRowIndexToModel(row), 0)).longValue();
    }

    protected boolean need() {
        if (table.getSelectedRow() < 0) { Ui.info(this, "Please select a row in the table first."); return false; }
        return true;
    }
}
