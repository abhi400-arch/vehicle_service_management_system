package com.vsms;

/** SQL fragments shared by several screens (all refer to alias "s" = SERVICE). */
final class Sql {
    private Sql() {}

    /** Total cost of a service = labour + all parts used. */
    static final String TOTAL =
        "(s.labor_cost + COALESCE((SELECT SUM(sp.part_cost) FROM SERVICE_PART sp " +
        "WHERE sp.service_id = s.service_id), 0))";

    /** Amount already received for a service. */
    static final String PAID =
        "COALESCE((SELECT SUM(p.amount) FROM PAYMENT p WHERE p.service_id = s.service_id " +
        "AND p.payment_status IN ('Paid','Partial')), 0)";
}
