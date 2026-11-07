package com.vsms;

import java.util.List;

/** Simple in-memory copy of a JDBC result set. */
public record Result(List<String> columns, List<Object[]> rows) {
    public boolean isEmpty() { return rows.isEmpty(); }
    public Object first() { return rows.isEmpty() ? null : rows.get(0)[0]; }
}
