# VSMS – Innovation Update

## What was added
The existing database schema is intentionally unchanged. The Java Swing application now includes a **Smart Innovations** menu with four read-only/analysis modules:

1. **Smart Mechanic Match**
   - Select an available vehicle and service type.
   - Ranks mechanics using specialization match, active workload and total jobs.
   - Does not write to the database.

2. **Service Cost Estimator**
   - Uses historical average labour cost for the selected service type.
   - Lets the user select existing spare parts and quantities.
   - Calculates an estimated labour + parts total without creating a service record.

3. **Vehicle Health Center**
   - Shows a simple 0–100 service-health score.
   - Displays last service, service count, latest odometer and lifetime service cost.
   - Shows recent service history for the selected vehicle.

4. **Smart Stock Predictor**
   - Uses existing SERVICE_PART usage from the previous 90 days.
   - Estimates average monthly usage and a suggested two-month reorder quantity.
   - Flags parts as URGENT / PLAN / OK.

## UI fixes
- Standardized action-button sizing.
- Improved management-screen toolbar/search alignment.
- Improved filter-row spacing.
- Standardized table font and row height.

## Run
1. Open the project in IntelliJ IDEA as a Maven project.
2. In `src/main/java/com/vsms/Db.java`, set `PASSWORD` to your local MySQL root password.
3. Keep `DATABASE = "vehicle_service_db"` unchanged.
4. Make sure MySQL is running.
5. Run `com.vsms.Main`.
6. If the database already exists, the application uses the existing data. **Do not run the bundled SQL script if you want to preserve your current records**, because that script recreates the sample database.
7. Login with your existing VSMS account. On a fresh database the bundled app creates `admin / admin123`.

No new SQL is required for the innovation modules.
