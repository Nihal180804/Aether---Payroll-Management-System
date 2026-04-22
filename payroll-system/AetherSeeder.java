import java.sql.*;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * AETHER SEEDER v2.0
 * Clears and populates hrms.db with high-volume, diverse payroll data.
 */
public class AetherSeeder {

    private static final String DB_URL = "jdbc:sqlite:hrms.db";
    private static final String[] PERIODS = { "2025-06", "2026-04" };
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            System.out.println(">>> STARTING AETHER SEEDER...");

            initSchema(conn);
            clearData(conn);

            for (String period : PERIODS) {
                System.out.println(">>> Seeding period: " + period);
                seedBatch(conn, 50, period);
            }

            System.out.println(">>> SEEDING COMPLETE: Multi-period data inserted.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void initSchema(Connection conn) throws SQLException {
        System.out.println("[0] Ensuring tax_context table exists...");
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS tax_context (" +
                    "record_id VARCHAR PRIMARY KEY, " +
                    "emp_id VARCHAR, " +
                    "pay_period VARCHAR, " +
                    "country_code VARCHAR, " +
                    "currency_code VARCHAR, " +
                    "tax_regime VARCHAR, " +
                    "state_name VARCHAR, " +
                    "filing_status VARCHAR, " +
                    "tax_code VARCHAR, " +
                    "national_id_number VARCHAR)");
        }
    }

    private static void clearData(Connection conn) throws SQLException {
        String[] tables = {
                "payroll_results", "payroll_audit_log", "financials",
                "attendance", "tax_context", "employees"
        };
        System.out.println("[1] Clearing existing data...");
        try (Statement stmt = conn.createStatement()) {
            for (String table : tables) {
                stmt.execute("DELETE FROM " + table);
            }
        }
    }

    private static void seedBatch(Connection conn, int count, String period) throws SQLException {
        // First pass: Employees (only if not already seeded)
        String empSql = "INSERT OR IGNORE INTO employees (emp_id, name, department, grade_level, basic_pay, years_of_service, country_code, currency_code, state_name, tax_regime, filing_status, tax_code, national_id_number, employment_status, employment_type, designation, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        // Second pass: Period-specific data
        String attSql = "INSERT INTO attendance (record_id, emp_id, pay_period, working_days_in_month, leave_with_pay, leave_without_pay, overtime_hours) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String finSql = "INSERT INTO financials (record_id, emp_id, pay_period, pending_claims, approved_reimbursement, insurance_premium, declared_investments) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String taxSql = "INSERT INTO tax_context (record_id, emp_id, pay_period, country_code, currency_code, tax_regime, state_name, filing_status, tax_code, national_id_number) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement empStmt = conn.prepareStatement(empSql);
                PreparedStatement attStmt = conn.prepareStatement(attSql);
                PreparedStatement finStmt = conn.prepareStatement(finSql);
                PreparedStatement taxStmt = conn.prepareStatement(taxSql)) {

            for (int i = 1; i <= count; i++) {
                String id = String.format("AETHER_%03d", i);

                // Random deterministic data based on ID (for consistency across periods)
                Random idRand = new Random(id.hashCode());
                String name = generateName(idRand);
                String dept = DEPARTMENTS[idRand.nextInt(DEPARTMENTS.length)];
                String grade = GRADES[idRand.nextInt(GRADES.length)];
                double pay = 25000 + idRand.nextDouble() * 150000;
                int years = idRand.nextInt(15);

                // Diversity Logic
                String country = "IN";
                String currency = "INR";
                String regime = i % 3 == 0 ? "OLD" : "NEW";
                String state = "KARNATAKA";

                if (i % 10 == 0) {
                    country = "SG";
                    currency = "SGD";
                    state = "SINGAPORE";
                    regime = "SG_STANDARD";
                } else if (i % 12 == 0) {
                    country = "US";
                    currency = "USD";
                    state = "CALIFORNIA";
                    regime = "FEDERAL";
                }

                // Edge cases
                if (i == 3)
                    state = ""; // Missing state
                if (i == 7)
                    pay = 5000; // Low pay
                if (i == 13)
                    regime = ""; // Missing regime

                Timestamp now = new Timestamp(System.currentTimeMillis());

                // 1. Employee (INSERT OR IGNORE)
                empStmt.setString(1, id);
                empStmt.setString(2, name);
                empStmt.setString(3, dept);
                empStmt.setString(4, grade);
                empStmt.setDouble(5, pay);
                empStmt.setInt(6, years);
                empStmt.setString(7, country);
                empStmt.setString(8, currency);
                empStmt.setString(9, state);
                empStmt.setString(10, regime);
                empStmt.setString(11, "SINGLE");
                empStmt.setString(12, "");
                empStmt.setString(13, "NID-" + (100000 + idRand.nextInt(900000)));
                empStmt.setString(14, "ACTIVE");
                empStmt.setString(15, "FULL_TIME");
                empStmt.setString(16, "Associate " + dept);
                empStmt.setTimestamp(17, now);
                empStmt.addBatch();

                // 2. Attendance
                String recId = "REC_" + id + "_" + period;
                attStmt.setString(1, recId);
                attStmt.setString(2, id);
                attStmt.setString(3, period);
                attStmt.setInt(4, 22);
                attStmt.setInt(5, idRand.nextInt(3));
                attStmt.setInt(6, (i == 7) ? 15 : idRand.nextInt(2));
                attStmt.setDouble(7, idRand.nextInt(10));
                attStmt.addBatch();

                // 3. Financials
                finStmt.setString(1, recId);
                finStmt.setString(2, id);
                finStmt.setString(3, period);
                finStmt.setDouble(4, idRand.nextDouble() * 20000);
                finStmt.setDouble(5, idRand.nextDouble() * 10000);
                finStmt.setDouble(6, 500 + idRand.nextDouble() * 2000);
                finStmt.setDouble(7, idRand.nextDouble() * 150000);
                finStmt.addBatch();

                // 4. Tax Context
                taxStmt.setString(1, recId);
                taxStmt.setString(2, id);
                taxStmt.setString(3, period);
                taxStmt.setString(4, country);
                taxStmt.setString(5, currency);
                taxStmt.setString(6, regime);
                taxStmt.setString(7, state);
                taxStmt.setString(8, "SINGLE");
                taxStmt.setString(9, "");
                taxStmt.setString(10, "NID-" + (100000 + idRand.nextInt(900000)));
                taxStmt.addBatch();
            }

            empStmt.executeBatch();
            attStmt.executeBatch();
            finStmt.executeBatch();
            taxStmt.executeBatch();
        }
    }

    private static String generateName(Random rand) {
        String[] first = { "Arjun", "Priya", "Ravi", "Meera", "Suresh", "Ananya", "Vikram", "Divya", "Rahul", "Ayesha",
                "John", "Li", "Sarah", "Amit", "Neha" };
        String[] last = { "Sharma", "Nair", "Kumar", "Iyer", "Babu", "Krishnan", "Malhotra", "Reddy", "Verma", "Khan",
                "Mitchell", "Wei", "Smith", "Gupta", "Patel" };
        return first[rand.nextInt(first.length)] + " " + last[rand.nextInt(last.length)];
    }

    private static final String[] DEPARTMENTS = { "Engineering", "Finance", "Human Resources", "Operations", "Product",
            "Sales", "Design", "Logistics", "Marketing", "Legal" };
    private static final String[] GRADES = { "L1", "L2", "L3", "L4", "L5", "CONTRACT" };
}
