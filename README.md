# Vehicle Service Management System (Java Swing + MySQL)

Desktop UI for `vehicle_service_db.sql` (ADBMS project). Java 17+, Maven, MySQL 8.0.16+.

## Run in IntelliJ IDEA
1. **File > Open** and select this folder's `pom.xml` ("Open as Project"). Let Maven download `mysql-connector-j`.
2. **File > Project Structure > Project SDK** = JDK 17 or newer.
3. Open `src/main/java/com/vsms/Db.java` and set your MySQL details at the top (`HOST`, `PORT`, `DATABASE`, `USER`, `PASSWORD`).
   Every screen gets its connection from `Db.getConnection()`.
4. Run `com.vsms.Main`. If the database does not exist yet, the app offers to create it from the bundled script.
5. Sign in with **admin / admin123** and change the password with the "Change Password" button.

Login accounts are kept in an `APP_USER` table that the app creates automatically (passwords stored as salted PBKDF2 hashes).
Note: re-running the SQL script drops the database, which also removes `APP_USER`; it is re-created with the default admin on the next start.

Optional runnable jar: `mvn package`, then `java -jar target/vsms-swing-1.0.jar`.

## Screens and what they call
| Screen | Database features used |
|---|---|
| Dashboard | KPI counts, `Vehicle_Service_Summary` view |
| Customers | INSERT / UPDATE / DELETE, IS NULL filter, COUNT + LEFT JOIN |
| Vehicles | CRUD, status filter, **`GetVehicleServiceCount()`** column, customer + vehicle registered in one transaction |
| Mechanics | CRUD, specialization filter, workload aggregates |
| Service Records | **`AddServiceRecord`** (check-in), **`AddServicePart`** (stock check + reduce), Complete / Cancel (transactions), date + status filters |
| Spare Parts | CRUD, restock, low-stock filter |
| Payments | **`RecordPayment`**, edit / delete, SUM total, IS NULL filter |
| Reports | view, joins, COUNT / AVG / SUM, both subqueries, EXPLAIN, index list, all integrity checks, function tester, CSV export |

## Notes
* Complete / Cancel are done in Java transactions (the script has no procedure for them). Completing sets the
  vehicle back to `Available`, so the same vehicle can be checked in again.
* `AddServiceRecord` inserts `odometer_km = 0`; the real reading is entered when the service is completed.
* Data issue in the sample script: Engine Oil (part 1) is used 8 times but only 6 units are deducted. Fix with
  `UPDATE PART SET stock_qty = stock_qty - 2 WHERE part_id = 1;`


## New innovation modules (database unchanged)
The updated Swing application adds a **Smart Innovations** section without creating or altering any database table/column:

1. **Smart Mechanic Match** — ranks mechanics using specialization match and current workload.
2. **Service Cost Estimator** — estimates labour + selected spare-part cost using existing PART/SERVICE data; read-only.
3. **Vehicle Health Center** — calculates a simple service-health score and shows recent service history and lifetime spend.
4. **Smart Stock Predictor** — uses the last 90 days of SERVICE_PART usage to suggest reorder quantities and priority.

### UI improvements
- Consistent toolbar/search alignment on all management screens.
- More uniform button sizing and table row height.
- Cleaner filter-row spacing and responsive search area.
- No database schema changes are required for these innovations.
