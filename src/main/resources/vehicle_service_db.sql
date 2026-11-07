-- ============================================================
-- VEHICLE SERVICE MANAGEMENT SYSTEM (VSMS)
-- Advanced Database Management System Practical
-- Database: vehicle_service_db
-- DBMS: MySQL 8.0+
-- No User Interface
-- ============================================================

DROP DATABASE IF EXISTS vehicle_service_db;
CREATE DATABASE vehicle_service_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE vehicle_service_db;

-- ============================================================
-- PHASE 1 & 2 : DATABASE CREATION
-- Seven tables exactly as specified in the proposal
-- ============================================================

CREATE TABLE CUSTOMER (
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_name VARCHAR(60) NOT NULL,
    phone VARCHAR(15),
    email VARCHAR(80) UNIQUE
) ENGINE=InnoDB;

CREATE TABLE VEHICLE (
    vehicle_id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    registration_no VARCHAR(20) UNIQUE NOT NULL,
    model VARCHAR(40) NOT NULL,
    vehicle_year YEAR,
    vehicle_status VARCHAR(20) NOT NULL DEFAULT 'Available',
    CONSTRAINT fk_vehicle_customer
        FOREIGN KEY (customer_id) REFERENCES CUSTOMER(customer_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_vehicle_status
        CHECK (vehicle_status IN ('Available','In Service','Completed','Inactive')),
    CONSTRAINT chk_vehicle_year
        CHECK (vehicle_year IS NULL OR vehicle_year >= 1980)
) ENGINE=InnoDB;

CREATE TABLE MECHANIC (
    mechanic_id INT PRIMARY KEY AUTO_INCREMENT,
    mechanic_name VARCHAR(60) NOT NULL,
    phone VARCHAR(15),
    specialization VARCHAR(50)
) ENGINE=InnoDB;

CREATE TABLE SERVICE (
    service_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id INT NOT NULL,
    mechanic_id INT NOT NULL,
    service_date DATE NOT NULL,
    service_type VARCHAR(50) NOT NULL,
    odometer_km INT NOT NULL,
    labor_cost DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    service_status VARCHAR(20) NOT NULL DEFAULT 'Open',
    CONSTRAINT fk_service_vehicle
        FOREIGN KEY (vehicle_id) REFERENCES VEHICLE(vehicle_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_service_mechanic
        FOREIGN KEY (mechanic_id) REFERENCES MECHANIC(mechanic_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_service_odometer
        CHECK (odometer_km >= 0),
    CONSTRAINT chk_service_labor
        CHECK (labor_cost >= 0),
    CONSTRAINT chk_service_status
        CHECK (service_status IN ('Open','In Progress','Completed','Cancelled'))
) ENGINE=InnoDB;

CREATE TABLE PART (
    part_id INT PRIMARY KEY AUTO_INCREMENT,
    part_name VARCHAR(60) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    stock_qty INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_part_price
        CHECK (unit_price >= 0),
    CONSTRAINT chk_part_stock
        CHECK (stock_qty >= 0)
) ENGINE=InnoDB;

CREATE TABLE SERVICE_PART (
    service_part_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT NOT NULL,
    part_id INT NOT NULL,
    quantity INT NOT NULL,
    part_cost DECIMAL(10,2) NOT NULL,
    CONSTRAINT fk_service_part_service
        FOREIGN KEY (service_id) REFERENCES SERVICE(service_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_service_part_part
        FOREIGN KEY (part_id) REFERENCES PART(part_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_service_part_quantity
        CHECK (quantity > 0),
    CONSTRAINT chk_service_part_cost
        CHECK (part_cost >= 0)
) ENGINE=InnoDB;

CREATE TABLE PAYMENT (
    payment_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    service_id BIGINT NOT NULL,
    payment_date DATE NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    payment_method VARCHAR(20),
    payment_status VARCHAR(20) NOT NULL DEFAULT 'Pending',
    CONSTRAINT fk_payment_service
        FOREIGN KEY (service_id) REFERENCES SERVICE(service_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT chk_payment_amount
        CHECK (amount >= 0),
    CONSTRAINT chk_payment_method
        CHECK (payment_method IS NULL OR
               payment_method IN ('Cash','UPI','Card','Bank Transfer')),
    CONSTRAINT chk_payment_status
        CHECK (payment_status IN ('Pending','Paid','Partial','Refunded','Failed'))
) ENGINE=InnoDB;

-- ============================================================
-- PHASE 2 : SAMPLE DATA / DATA PREPARATION
-- ============================================================

INSERT INTO CUSTOMER (customer_name, phone, email) VALUES
('Arjun Nair',      '9876501001', 'arjun.nair@example.com'),
('Rahul Menon',     '9876501002', 'rahul.menon@example.com'),
('Anjali Thomas',   '9876501003', 'anjali.thomas@example.com'),
('Vivek Kumar',     '9876501004', 'vivek.kumar@example.com'),
('Neha Joseph',     '9876501005', 'neha.joseph@example.com'),
('Akhil Raj',       '9876501006', 'akhil.raj@example.com'),
('Meera Das',       '9876501007', 'meera.das@example.com'),
('Sanjay Pillai',   '9876501008', 'sanjay.pillai@example.com');

INSERT INTO VEHICLE
(customer_id, registration_no, model, vehicle_year, vehicle_status) VALUES
(1, 'KL01AB1234', 'Maruti Swift',     2021, 'Available'),
(1, 'KL01CD5678', 'Hyundai i20',      2020, 'Available'),
(2, 'KL07EF1122', 'Honda City',       2019, 'Completed'),
(3, 'KL08GH3344', 'Toyota Innova',    2022, 'Available'),
(4, 'KL10IJ5566', 'Kia Seltos',      2021, 'Completed'),
(5, 'KL11KL7788', 'Tata Nexon',       2020, 'Available'),
(6, 'KL12MN9900', 'Mahindra XUV700',  2023, 'Available'),
(7, 'KL13OP2468', 'Hyundai Creta',    2022, 'Completed'),
(8, 'KL14QR1357', 'Honda Amaze',      2018, 'Available'),
(8, 'KL14ST9753', 'Tata Altroz',      2021, 'Inactive');

INSERT INTO MECHANIC (mechanic_name, phone, specialization) VALUES
('Suresh Kumar',  '9000001001', 'Engine'),
('Manoj Joseph',  '9000001002', 'Electrical'),
('Ravi Shankar',  '9000001003', 'Brakes'),
('Nithin Das',    '9000001004', 'Transmission'),
('Faisal Ahmed',  '9000001005', 'General Service'),
('Vishnu Raj',    '9000001006', 'AC and Cooling');

INSERT INTO PART (part_name, unit_price, stock_qty) VALUES
('Engine Oil 5W30',       2800.00, 50),
('Oil Filter',             450.00, 40),
('Air Filter',              650.00, 35),
('Brake Pad Set',          3200.00, 20),
('Spark Plug Set',         1800.00, 25),
('Battery',                6500.00, 15),
('Coolant',                 800.00, 30),
('Clutch Plate',           7200.00, 10),
('AC Filter',               550.00, 30),
('Brake Fluid',             600.00, 25);

-- Existing service records
INSERT INTO SERVICE
(vehicle_id, mechanic_id, service_date, service_type, odometer_km, labor_cost, service_status) VALUES
(1, 5, '2026-01-10', 'General Service', 25000, 1200.00, 'Completed'),
(1, 1, '2026-06-15', 'Engine Service', 30000, 2500.00, 'Completed'),
(2, 2, '2026-02-18', 'Electrical Check', 28000, 1000.00, 'Completed'),
(2, 5, '2026-07-22', 'General Service', 34000, 1300.00, 'Completed'),
(3, 3, '2026-03-05', 'Brake Service', 45000, 1800.00, 'Completed'),
(3, 4, '2026-08-12', 'Transmission Check', 50000, 2200.00, 'Completed'),
(4, 1, '2026-04-20', 'Engine Service', 18000, 3000.00, 'Completed'),
(5, 3, '2026-05-11', 'Brake Service', 38000, 1900.00, 'Completed'),
(5, 5, '2026-08-25', 'General Service', 42000, 1400.00, 'Completed'),
(6, 6, '2026-06-03', 'AC Service', 27000, 1600.00, 'Completed'),
(7, 1, '2026-06-28', 'Engine Service', 15000, 2800.00, 'Completed'),
(8, 5, '2026-07-05', 'General Service', 36000, 1250.00, 'Completed'),
(9, 3, '2026-08-02', 'Brake Service', 60000, 1700.00, 'Completed');

-- Existing service-part usage.
-- part_cost stores the cost for the quantity used in that service.
INSERT INTO SERVICE_PART (service_id, part_id, quantity, part_cost) VALUES
(1, 1, 1, 2800.00),
(1, 2, 1,  450.00),
(2, 1, 1, 2800.00),
(2, 5, 1, 1800.00),
(3, 6, 1, 6500.00),
(4, 1, 1, 2800.00),
(4, 3, 1,  650.00),
(5, 4, 1, 3200.00),
(6, 8, 1, 7200.00),
(7, 1, 2, 5600.00),
(7, 2, 1,  450.00),
(8, 4, 1, 3200.00),
(9, 1, 1, 2800.00),
(10, 7, 2, 1600.00),
(11, 1, 1, 2800.00),
(12, 1, 1, 2800.00),
(12, 9, 1,  550.00),
(13, 4, 1, 3200.00),
(13, 10, 1,  600.00);

-- Payments
INSERT INTO PAYMENT
(service_id, payment_date, amount, payment_method, payment_status) VALUES
(1,  '2026-01-10', 4450.00, 'UPI',          'Paid'),
(2,  '2026-06-15', 7100.00, 'Card',         'Paid'),
(3,  '2026-02-18', 7500.00, 'Cash',         'Paid'),
(4,  '2026-07-22', 4750.00, 'UPI',          'Paid'),
(5,  '2026-03-05', 5000.00, 'Card',         'Paid'),
(6,  '2026-08-12', 9400.00, 'Bank Transfer','Paid'),
(7,  '2026-04-20', 9050.00, 'UPI',          'Paid'),
(8,  '2026-05-11', 5100.00, 'Cash',         'Paid'),
(9,  '2026-08-25', 4200.00, 'UPI',          'Paid'),
(10, '2026-06-03', 3200.00, 'Card',         'Paid'),
(11, '2026-06-28', 5600.00, 'UPI',          'Paid'),
(12, '2026-07-05', 4600.00, 'Cash',         'Paid'),
(13, '2026-08-02', 5500.00, 'Card',         'Paid');

-- Reduce stock to reflect the existing service-part usage.
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 1;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 2;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 5;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 6;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 3;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 4;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 8;
UPDATE PART SET stock_qty = stock_qty - 2 WHERE part_id = 1;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 2;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 4;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 1;
UPDATE PART SET stock_qty = stock_qty - 2 WHERE part_id = 7;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 1;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 1;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 9;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 4;
UPDATE PART SET stock_qty = stock_qty - 1 WHERE part_id = 10;

-- ============================================================
-- PHASE 3 : BASIC QUERIES
-- ============================================================

-- 1. Available vehicles
SELECT *
FROM VEHICLE
WHERE vehicle_status = 'Available'
ORDER BY registration_no;

-- 2. Completed services
SELECT *
FROM SERVICE
WHERE service_status = 'Completed'
ORDER BY service_date DESC;

-- 3. Services by date
SELECT *
FROM SERVICE
WHERE service_date BETWEEN '2026-01-01' AND '2026-12-31'
ORDER BY service_date;

-- 4. Mechanics by specialization
SELECT *
FROM MECHANIC
WHERE specialization = 'Engine'
ORDER BY mechanic_name;

-- 5. Low-stock parts
SELECT *
FROM PART
WHERE stock_qty < 10
ORDER BY stock_qty ASC;

-- 6. Payment records
SELECT *
FROM PAYMENT
ORDER BY payment_date DESC;

-- ============================================================
-- PHASE 2 : AGGREGATE FUNCTIONS
-- ============================================================

-- Services per vehicle
SELECT
    v.vehicle_id,
    v.registration_no,
    COUNT(s.service_id) AS service_count
FROM VEHICLE v
LEFT JOIN SERVICE s ON v.vehicle_id = s.vehicle_id
GROUP BY v.vehicle_id, v.registration_no
ORDER BY service_count DESC;

-- Average service labor cost
SELECT ROUND(AVG(labor_cost), 2) AS average_labor_cost
FROM SERVICE;

-- Total maintenance/service expenditure
SELECT
    ROUND(
        COALESCE(SUM(s.labor_cost), 0) +
        COALESCE((SELECT SUM(sp.part_cost) FROM SERVICE_PART sp), 0),
        2
    ) AS total_service_expenditure
FROM SERVICE s;

-- Total payments
SELECT ROUND(SUM(amount), 2) AS total_payments
FROM PAYMENT
WHERE payment_status IN ('Paid','Partial');

-- ============================================================
-- PHASE 3 : JOINS
-- ============================================================

-- CUSTOMER + VEHICLE ownership details
SELECT
    c.customer_id,
    c.customer_name,
    c.phone,
    c.email,
    v.vehicle_id,
    v.registration_no,
    v.model,
    v.vehicle_status
FROM CUSTOMER c
JOIN VEHICLE v ON c.customer_id = v.customer_id
ORDER BY c.customer_name, v.registration_no;

-- VEHICLE + SERVICE + MECHANIC service report
SELECT
    v.registration_no,
    v.model,
    s.service_id,
    s.service_date,
    s.service_type,
    m.mechanic_name,
    m.specialization,
    s.labor_cost,
    s.service_status
FROM VEHICLE v
JOIN SERVICE s ON v.vehicle_id = s.vehicle_id
JOIN MECHANIC m ON s.mechanic_id = m.mechanic_id
ORDER BY s.service_date DESC;

-- SERVICE + SERVICE_PART + PART parts-used details
SELECT
    s.service_id,
    v.registration_no,
    s.service_date,
    p.part_name,
    sp.quantity,
    sp.part_cost,
    ROUND(sp.part_cost / sp.quantity, 2) AS unit_cost
FROM SERVICE s
JOIN VEHICLE v ON s.vehicle_id = v.vehicle_id
JOIN SERVICE_PART sp ON s.service_id = sp.service_id
JOIN PART p ON sp.part_id = p.part_id
ORDER BY s.service_id, p.part_name;

-- ============================================================
-- PHASE 3 : NESTED QUERIES / SUBQUERIES
-- ============================================================

-- Vehicles whose service count is above the average service count
SELECT
    v.vehicle_id,
    v.registration_no,
    v.model,
    COUNT(s.service_id) AS service_count
FROM VEHICLE v
JOIN SERVICE s ON v.vehicle_id = s.vehicle_id
GROUP BY v.vehicle_id, v.registration_no, v.model
HAVING COUNT(s.service_id) >
       (
           SELECT AVG(service_count)
           FROM (
               SELECT vehicle_id, COUNT(*) AS service_count
               FROM SERVICE
               GROUP BY vehicle_id
           ) AS vehicle_service_counts
       )
ORDER BY service_count DESC;

-- Services whose total cost is above the overall average service cost
SELECT
    s.service_id,
    v.registration_no,
    s.service_date,
    s.service_type,
    ROUND(
        s.labor_cost +
        COALESCE((SELECT SUM(sp.part_cost)
                  FROM SERVICE_PART sp
                  WHERE sp.service_id = s.service_id), 0),
        2
    ) AS total_service_cost
FROM SERVICE s
JOIN VEHICLE v ON s.vehicle_id = v.vehicle_id
WHERE
    s.labor_cost +
    COALESCE((SELECT SUM(sp.part_cost)
              FROM SERVICE_PART sp
              WHERE sp.service_id = s.service_id), 0)
    >
    (
        SELECT AVG(service_total)
        FROM (
            SELECT
                s2.service_id,
                s2.labor_cost +
                COALESCE((SELECT SUM(sp2.part_cost)
                          FROM SERVICE_PART sp2
                          WHERE sp2.service_id = s2.service_id), 0)
                AS service_total
            FROM SERVICE s2
        ) AS service_totals
    )
ORDER BY total_service_cost DESC;

-- ============================================================
-- PHASE 3 : VIEW
-- Vehicle_Service_Summary
-- ============================================================

DROP VIEW IF EXISTS Vehicle_Service_Summary;

CREATE VIEW Vehicle_Service_Summary AS
SELECT
    v.registration_no,
    c.customer_name AS customer,
    s.service_type,
    m.mechanic_name AS mechanic,
    s.service_date,
    s.service_status AS status,
    ROUND(
        s.labor_cost +
        COALESCE((
            SELECT SUM(sp.part_cost)
            FROM SERVICE_PART sp
            WHERE sp.service_id = s.service_id
        ), 0),
        2
    ) AS total_service_cost
FROM CUSTOMER c
JOIN VEHICLE v ON c.customer_id = v.customer_id
JOIN SERVICE s ON v.vehicle_id = s.vehicle_id
JOIN MECHANIC m ON s.mechanic_id = m.mechanic_id;

SELECT *
FROM Vehicle_Service_Summary
ORDER BY service_date DESC;

-- ============================================================
-- PHASE 3 : QUERY OPTIMIZATION / INDEXES
-- ============================================================

CREATE INDEX idx_service_vehicle
    ON SERVICE(vehicle_id);

CREATE INDEX idx_service_mechanic
    ON SERVICE(mechanic_id);

CREATE INDEX idx_service_part_service
    ON SERVICE_PART(service_id);

CREATE INDEX idx_service_part_part
    ON SERVICE_PART(part_id);

-- EXPLAIN before/after index-oriented query
EXPLAIN
SELECT
    v.registration_no,
    s.service_date,
    s.service_type,
    m.mechanic_name
FROM VEHICLE v
JOIN SERVICE s ON v.vehicle_id = s.vehicle_id
JOIN MECHANIC m ON s.mechanic_id = m.mechanic_id
WHERE v.vehicle_id = 1;

-- ============================================================
-- PHASE 3 : FUNCTION
-- GetVehicleServiceCount(vehicle_id)
-- ============================================================

DROP FUNCTION IF EXISTS GetVehicleServiceCount;

DELIMITER $$

CREATE FUNCTION GetVehicleServiceCount(p_vehicle_id INT)
RETURNS INT
DETERMINISTIC
READS SQL DATA
BEGIN
    DECLARE v_count INT DEFAULT 0;

    SELECT COUNT(*)
    INTO v_count
    FROM SERVICE
    WHERE vehicle_id = p_vehicle_id;

    RETURN v_count;
END$$

DELIMITER ;

-- Function test
SELECT
    vehicle_id,
    registration_no,
    GetVehicleServiceCount(vehicle_id) AS total_services
FROM VEHICLE
ORDER BY vehicle_id;

-- ============================================================
-- PHASE 3 & 4 : MAIN PROCEDURE
-- AddServiceRecord(vehicle_id, mechanic_id, service_type, labor_cost)
--
-- It:
-- 1. Locks and checks the vehicle.
-- 2. Confirms that the vehicle is Available.
-- 3. Inserts SERVICE.
-- 4. Creates a SAVEPOINT before the vehicle-status update.
-- 5. Updates VEHICLE to In Service.
-- 6. Commits both changes as one transaction.
-- 7. Rolls back everything if any error occurs.
-- ============================================================

DROP PROCEDURE IF EXISTS AddServiceRecord;

DELIMITER $$

CREATE PROCEDURE AddServiceRecord(
    IN p_vehicle_id INT,
    IN p_mechanic_id INT,
    IN p_service_type VARCHAR(50),
    IN p_labor_cost DECIMAL(10,2)
)
BEGIN
    DECLARE v_vehicle_status VARCHAR(20);
    DECLARE v_service_id BIGINT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    START TRANSACTION;

    -- Lock the selected vehicle to handle concurrent service allocation.
    SELECT vehicle_status
    INTO v_vehicle_status
    FROM VEHICLE
    WHERE vehicle_id = p_vehicle_id
    FOR UPDATE;

    IF v_vehicle_status IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Vehicle does not exist';
    END IF;

    IF v_vehicle_status <> 'Available' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Vehicle is not Available for a new service';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM MECHANIC
        WHERE mechanic_id = p_mechanic_id
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Mechanic does not exist';
    END IF;

    IF p_service_type IS NULL OR TRIM(p_service_type) = '' THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Service type cannot be empty';
    END IF;

    IF p_labor_cost IS NULL OR p_labor_cost < 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Labor cost cannot be negative';
    END IF;

    INSERT INTO SERVICE
        (vehicle_id, mechanic_id, service_date, service_type,
         odometer_km, labor_cost, service_status)
    VALUES
        (p_vehicle_id, p_mechanic_id, CURDATE(), p_service_type,
         0, p_labor_cost, 'In Progress');

    SET v_service_id = LAST_INSERT_ID();

    -- Required SAVEPOINT before final vehicle-status update.
    SAVEPOINT before_vehicle_status_update;

    UPDATE VEHICLE
    SET vehicle_status = 'In Service'
    WHERE vehicle_id = p_vehicle_id;

    IF ROW_COUNT() <> 1 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Vehicle status update failed';
    END IF;

    COMMIT;

    SELECT
        v_service_id AS service_id,
        p_vehicle_id AS vehicle_id,
        p_mechanic_id AS mechanic_id,
        'In Progress' AS service_status,
        'Service record created and vehicle marked In Service' AS message;
END$$

DELIMITER ;

-- ============================================================
-- ADDITIONAL PROCEDURE FOR THE FUNCTIONAL MODULE:
-- Store parts used and update stock atomically.
-- No new table/schema is introduced.
-- ============================================================

DROP PROCEDURE IF EXISTS AddServicePart;

DELIMITER $$

CREATE PROCEDURE AddServicePart(
    IN p_service_id BIGINT,
    IN p_part_id INT,
    IN p_quantity INT
)
BEGIN
    DECLARE v_stock INT;
    DECLARE v_unit_price DECIMAL(10,2);
    DECLARE v_service_exists INT DEFAULT 0;
    DECLARE v_part_exists INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    IF p_quantity IS NULL OR p_quantity <= 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Part quantity must be greater than zero';
    END IF;

    START TRANSACTION;

    SELECT COUNT(*)
    INTO v_service_exists
    FROM SERVICE
    WHERE service_id = p_service_id;

    IF v_service_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Service does not exist';
    END IF;

    -- Lock part row to prevent concurrent stock updates.
    SELECT stock_qty, unit_price
    INTO v_stock, v_unit_price
    FROM PART
    WHERE part_id = p_part_id
    FOR UPDATE;

    IF v_unit_price IS NULL THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Part does not exist';
    END IF;

    IF v_stock < p_quantity THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Insufficient part stock';
    END IF;

    INSERT INTO SERVICE_PART
        (service_id, part_id, quantity, part_cost)
    VALUES
        (p_service_id, p_part_id, p_quantity,
         v_unit_price * p_quantity);

    UPDATE PART
    SET stock_qty = stock_qty - p_quantity
    WHERE part_id = p_part_id;

    COMMIT;

    SELECT
        p_service_id AS service_id,
        p_part_id AS part_id,
        p_quantity AS quantity,
        v_unit_price * p_quantity AS part_cost,
        'Part usage recorded and stock updated' AS message;
END$$

DELIMITER ;

-- ============================================================
-- ADDITIONAL PROCEDURE FOR PAYMENT FUNCTIONAL MODULE
-- ============================================================

DROP PROCEDURE IF EXISTS RecordPayment;

DELIMITER $$

CREATE PROCEDURE RecordPayment(
    IN p_service_id BIGINT,
    IN p_payment_date DATE,
    IN p_amount DECIMAL(10,2),
    IN p_payment_method VARCHAR(20),
    IN p_payment_status VARCHAR(20)
)
BEGIN
    DECLARE v_service_exists INT DEFAULT 0;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    SELECT COUNT(*)
    INTO v_service_exists
    FROM SERVICE
    WHERE service_id = p_service_id;

    IF v_service_exists = 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Service does not exist';
    END IF;

    IF p_amount IS NULL OR p_amount < 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Payment amount cannot be negative';
    END IF;

    START TRANSACTION;

    INSERT INTO PAYMENT
        (service_id, payment_date, amount, payment_method, payment_status)
    VALUES
        (p_service_id, COALESCE(p_payment_date, CURDATE()), p_amount,
         p_payment_method, COALESCE(p_payment_status, 'Pending'));

    COMMIT;

    SELECT
        LAST_INSERT_ID() AS payment_id,
        p_service_id AS service_id,
        'Payment recorded successfully' AS message;
END$$

DELIMITER ;

-- ============================================================
-- PHASE 4 : TRANSACTION / FUNCTIONAL TESTS
-- These test calls use currently available sample data.
-- Uncomment/run individually when testing.
-- ============================================================

-- Test main service procedure:
-- Vehicle 1 is Available in the initial dataset.
-- CALL AddServiceRecord(1, 5, 'Routine Inspection', 900.00);

-- Test part procedure after creating a service:
-- CALL AddServicePart(14, 1, 1);

-- Test payment procedure:
-- CALL RecordPayment(14, CURDATE(), 900.00, 'UPI', 'Paid');

-- Test function directly:
-- SELECT GetVehicleServiceCount(1);

-- Verify vehicle/service relationship after AddServiceRecord:
-- SELECT vehicle_id, registration_no, vehicle_status
-- FROM VEHICLE
-- WHERE vehicle_id = 1;
--
-- SELECT *
-- FROM SERVICE
-- WHERE vehicle_id = 1
-- ORDER BY service_id DESC
-- LIMIT 1;

-- ============================================================
-- PHASE 4 : VALIDATION QUERIES
-- ============================================================

-- 1. Missing vehicle references
SELECT s.service_id, s.vehicle_id
FROM SERVICE s
LEFT JOIN VEHICLE v ON s.vehicle_id = v.vehicle_id
WHERE v.vehicle_id IS NULL;

-- 2. Missing mechanic references
SELECT s.service_id, s.mechanic_id
FROM SERVICE s
LEFT JOIN MECHANIC m ON s.mechanic_id = m.mechanic_id
WHERE m.mechanic_id IS NULL;

-- 3. Missing service-part references
SELECT sp.service_part_id
FROM SERVICE_PART sp
LEFT JOIN SERVICE s ON sp.service_id = s.service_id
LEFT JOIN PART p ON sp.part_id = p.part_id
WHERE s.service_id IS NULL OR p.part_id IS NULL;

-- 4. Missing payment-service references
SELECT p.payment_id
FROM PAYMENT p
LEFT JOIN SERVICE s ON p.service_id = s.service_id
WHERE s.service_id IS NULL;

-- 5. Negative stock check
SELECT *
FROM PART
WHERE stock_qty < 0;

-- 6. Active service records vs vehicle status
SELECT
    s.service_id,
    v.registration_no,
    s.service_status,
    v.vehicle_status
FROM SERVICE s
JOIN VEHICLE v ON s.vehicle_id = v.vehicle_id
WHERE s.service_status IN ('Open','In Progress')
  AND v.vehicle_status <> 'In Service';

-- 7. Service summary with payment totals
SELECT
    s.service_id,
    v.registration_no,
    s.service_date,
    ROUND(
        s.labor_cost +
        COALESCE((SELECT SUM(sp.part_cost)
                  FROM SERVICE_PART sp
                  WHERE sp.service_id = s.service_id), 0),
        2
    ) AS total_service_cost,
    COALESCE((
        SELECT SUM(p.amount)
        FROM PAYMENT p
        WHERE p.service_id = s.service_id
          AND p.payment_status IN ('Paid','Partial')
    ), 0) AS total_paid
FROM SERVICE s
JOIN VEHICLE v ON s.vehicle_id = v.vehicle_id
ORDER BY s.service_id;

-- ============================================================
-- END OF VEHICLE SERVICE MANAGEMENT SYSTEM
-- ============================================================
