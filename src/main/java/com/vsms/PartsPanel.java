package com.vsms;

import javax.swing.*;
import java.math.BigDecimal;

final class PartsPanel extends BasePanel {
    private final JCheckBox low = new JCheckBox("Low stock only (stock < 10)");

    PartsPanel() {
        super("Spare Parts & Stock", "Maintain unit prices and stock; stock is reduced automatically when parts are used in a service");
        action("Add Part", Ui.ACCENT, this::add);
        action("Edit", Ui.SLATE, this::edit);
        action("Restock", Ui.GREEN, this::restock);
        action("Delete", Ui.RED, this::delete);
        low.setOpaque(false);
        low.addActionListener(e -> refresh());
        filters.add(low);
    }

    @Override protected Result fetch() throws Exception {
        return Db.query("SELECT p.part_id AS `ID`, p.part_name AS `Part`, p.unit_price AS `Unit Price`, "
                + "p.stock_qty AS `Stock`, COALESCE(SUM(sp.quantity), 0) AS `Units Used`, "
                + "CASE WHEN p.stock_qty < 10 THEN 'LOW' ELSE 'OK' END AS `Level` "
                + "FROM PART p LEFT JOIN SERVICE_PART sp ON sp.part_id = p.part_id"
                + (low.isSelected() ? " WHERE p.stock_qty < 10" : "")
                + " GROUP BY p.part_id, p.part_name, p.unit_price, p.stock_qty ORDER BY p.part_id");
    }

    private void add() {
        Form f = new Form(this, "Add Part", "Save");
        f.text("Part name", ""); f.text("Unit price", ""); f.text("Stock quantity", "0");
        if (f.run(() -> save(f, null))) refresh();
    }

    private void edit() {
        if (!need()) return;
        long id = id();
        Form f = new Form(this, "Edit Part #" + id, "Update");
        f.text("Part name", Ui.s(cell("Part")));
        f.text("Unit price", ((BigDecimal) cell("Unit Price")).toPlainString());
        f.text("Stock quantity", Ui.s(cell("Stock")));
        if (f.run(() -> save(f, id))) refresh();
    }

    private void save(Form f, Long id) throws Exception {
        String name = f.str("Part name");
        if (name.isEmpty()) throw new IllegalArgumentException("Part name is required.");
        BigDecimal price = Ui.parseMoney("Unit price", f.str("Unit price"));
        int stock = Ui.parseInt("Stock quantity", f.str("Stock quantity"), 0, 1_000_000);
        if (id == null) {
            Db.syncAutoIncrement("PART", "part_id");
            Db.update("INSERT INTO PART (part_name, unit_price, stock_qty) VALUES (?,?,?)", name, price, stock);
        } else {
            Db.update("UPDATE PART SET part_name=?, unit_price=?, stock_qty=? WHERE part_id=?", name, price, stock, id);
        }
    }

    private void restock() {
        if (!need()) return;
        long id = id();
        Form f = new Form(this, "Restock: " + Ui.s(cell("Part")), "Add Stock");
        f.text("Quantity to add", "10");
        if (f.run(() -> {
            int q = Ui.parseInt("Quantity", f.str("Quantity to add"), 1, 1_000_000);
            Db.update("UPDATE PART SET stock_qty = stock_qty + ? WHERE part_id=?", q, id);
        })) refresh();
    }

    private void delete() {
        if (!need()) return;
        long id = id();
        if (!Ui.confirm(this, "Delete part #" + id + "?\n(Only possible when it was never used in a service.)")) return;
        if (Ui.run(this, () -> {
            Db.update("DELETE FROM PART WHERE part_id=?", id);
            Db.syncAutoIncrement("PART", "part_id");
        })) refresh();
    }
}
