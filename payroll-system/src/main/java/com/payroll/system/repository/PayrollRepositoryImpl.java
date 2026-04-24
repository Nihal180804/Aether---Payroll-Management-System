package com.payroll.system.repository;

import com.hrms.db.repositories.payroll.AttendanceDTO;
import com.hrms.db.repositories.payroll.EmployeeDTO;
import com.hrms.db.repositories.payroll.FinancialsDTO;
import com.hrms.db.repositories.payroll.IPayrollRepository;
import com.hrms.db.repositories.payroll.PayrollDataPackage;
import com.hrms.db.repositories.payroll.PayrollResultDTO;
import com.hrms.db.repositories.payroll.TaxContextDTO;
import com.pesu.expensesubsystem.integration.ApprovedClaimDTO;
import com.pesu.expensesubsystem.integration.ExpenseDataProvider;
import com.pesu.expensesubsystem.integration.ExpenseDataProviderImpl;
import com.pesu.leavesubsystem.integration.LeaveDataProviderImpl;
import com.pesu.leavesubsystem.integration.LeaveDetailsDTO;
import com.payroll.system.util.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class PayrollRepositoryImpl implements IPayrollRepository {

    private static final String DB_URL = DatabaseConfig.getJdbcUrl();
    private final LeaveDataProviderImpl leaveDataProvider = new LeaveDataProviderImpl();
    private final ExpenseDataProvider expenseDataProvider = new ExpenseDataProviderImpl();

    @Override
    public boolean savePayrollResult(String batchID, PayrollResultDTO result) {
        try (Connection conn = openConnection()) {
            ensurePayrollResultsTable(conn);
            Set<String> columns = getTableColumns(conn, "payroll_results");
            Map<String, Object> values = new LinkedHashMap<>();

            putIfPresent(values, columns, result.empID, "emp_id");
            putIfPresent(values, columns, batchID, "batch_id");
            putIfPresent(values, columns, result.recordID, "record_id");
            putIfPresent(values, columns, result.payPeriod, "pay_period");
            putIfPresent(values, columns, result.finalGrossPay, "final_gross_pay", "gross_pay");
            putIfPresent(values, columns, result.finalNetPay, "final_net_pay", "net_pay");
            putIfPresent(values, columns, result.overtimePay, "overtime_pay");
            putIfPresent(values, columns, result.penaltyAmount, "penalty_amount");
            putIfPresent(values, columns, result.pfAmount, "pf_amount");
            putIfPresent(values, columns, result.ptAmount, "pt_amount");
            putIfPresent(values, columns, result.taxDeducted, "tax_deducted", "monthly_tds_amount", "tds_amount",
                    "tax_amount");
            putIfPresent(values, columns, result.payoutAmount, "payout_amount");
            putIfPresent(values, columns, result.reimbursementPayout, "reimbursement_payout");
            putIfPresent(values, columns, result.gratuityAmount, "gratuity_amount");
            putIfPresent(values, columns, Timestamp.from(Instant.now()), "created_at", "processed_at");

            String sql = buildInsertSql("payroll_results", values);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int index = 1;
                for (Object value : values.values()) {
                    ps.setObject(index++, value);
                }
                return ps.executeUpdate() == 1;
            }
        } catch (SQLException e) {
            System.err.printf("PayrollRepositoryImpl.savePayrollResult failed for empID=%s: %s%n",
                    result.empID, e.getMessage());
            return false;
        }
    }

    @Override
    public void logProcessingError(String batchID, String empID, String errorMsg) {
        String sql = """
                INSERT INTO payroll_audit_log (batch_id, emp_id, log_level, message, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection conn = openConnection()) {
            ensurePayrollAuditTable(conn);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, batchID);
                ps.setString(2, empID);
                ps.setString(3, "WARN");
                ps.setString(4, errorMsg);
                ps.setTimestamp(5, Timestamp.from(Instant.now()));
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.printf("[WARN ] [PayrollRepositoryImpl.logProcessingError] Payroll error for empID=%s: %s%n",
                    empID, errorMsg);
        }
    }

    @Override
    public List<String> getAllActiveEmployeeIDs() {
        String sql = """
                SELECT emp_id
                FROM employees
                WHERE UPPER(COALESCE(employment_status, 'ACTIVE')) = 'ACTIVE'
                ORDER BY emp_id
                """;

        List<String> ids = new ArrayList<>();
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ids.add(rs.getString("emp_id"));
            }
        } catch (SQLException e) {
            System.err.printf("PayrollRepositoryImpl.getAllActiveEmployeeIDs failed: %s%n", e.getMessage());
        }
        return ids;
    }

    @Override
    public PayrollDataPackage fetchEmployeeData(String empID, String payPeriod) {
        String sql = """
                SELECT
                    e.emp_id,
                    e.name,
                    e.department,
                    COALESCE(
                        (SELECT cr.role_id
                         FROM critical_roles cr
                         WHERE LOWER(TRIM(COALESCE(cr.role_name, ''))) = LOWER(TRIM(COALESCE(e.role, '')))
                         LIMIT 1),
                        (SELECT cr.role_id
                         FROM critical_roles cr
                         WHERE LOWER(TRIM(COALESCE(cr.role_name, ''))) = LOWER(TRIM(COALESCE(e.designation, '')))
                         LIMIT 1),
                        (SELECT cr.role_id
                         FROM critical_roles cr
                         WHERE LOWER(TRIM(COALESCE(cr.department, ''))) = LOWER(TRIM(COALESCE(e.department, '')))
                         ORDER BY cr.role_id
                         LIMIT 1)
                    ) AS resolved_role_id,
                    e.grade_level,
                    e.basic_pay,
                    e.years_of_service,
                    e.country_code AS emp_country_code,
                    e.currency_code AS emp_currency_code,
                    e.state_name AS emp_state_name,
                    e.tax_regime AS emp_tax_regime,
                    e.filing_status AS emp_filing_status,
                    e.tax_code AS emp_tax_code,
                    e.national_id_number AS emp_national_id_number,
                    a.working_days_in_month,
                    a.leave_with_pay,
                    a.leave_without_pay,
                    a.overtime_hours,
                    f.pending_claims,
                    f.approved_reimbursement,
                    f.insurance_premium,
                    f.declared_investments,
                    t.country_code AS tax_country_code,
                    t.currency_code AS tax_currency_code,
                    t.tax_regime AS tax_tax_regime,
                    t.state_name AS tax_state_name,
                    t.filing_status AS tax_filing_status,
                    t.tax_code AS tax_tax_code,
                    t.national_id_number AS tax_national_id_number
                FROM employees e
                LEFT JOIN attendance a
                    ON a.emp_id = e.emp_id AND a.pay_period = ?
                LEFT JOIN financials f
                    ON f.emp_id = e.emp_id AND f.pay_period = ?
                LEFT JOIN tax_context t
                    ON t.emp_id = e.emp_id AND t.pay_period = ?
                WHERE e.emp_id = ?
                  AND UPPER(COALESCE(e.employment_status, 'ACTIVE')) = 'ACTIVE'
                """;

        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, payPeriod);
            ps.setString(2, payPeriod);
            ps.setString(3, payPeriod);
            ps.setString(4, empID);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                PayrollDataPackage pkg = new PayrollDataPackage();
                pkg.payPeriod = payPeriod;
                pkg.employee = mapEmployee(rs);
                pkg.attendance = mapAttendance(rs, empID, payPeriod);
                pkg.financials = mapFinancials(rs, empID);
                pkg.tax = mapTaxContext(rs);
                return pkg;
            }
        } catch (SQLException e) {
            System.err.printf("PayrollRepositoryImpl.fetchEmployeeData failed for empID=%s, period=%s: %s%n",
                    empID, payPeriod, e.getMessage());
            return null;
        }
    }

    private Connection openConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private EmployeeDTO mapEmployee(ResultSet rs) throws SQLException {
        EmployeeDTO dto = new EmployeeDTO();
        dto.empID = rs.getString("emp_id");
        dto.name = rs.getString("name");
        dto.department = rs.getString("department");
        int roleId = rs.getInt("resolved_role_id");
        dto.roleId = rs.wasNull() ? null : roleId;
        dto.gradeLevel = rs.getString("grade_level");
        dto.basicPay = rs.getDouble("basic_pay");
        dto.yearsOfService = rs.getInt("years_of_service");
        return dto;
    }

    private AttendanceDTO mapAttendance(ResultSet rs, String empID, String payPeriod) throws SQLException {
        AttendanceDTO dto = new AttendanceDTO();
        dto.workingDaysInMonth = rs.getInt("working_days_in_month");
        dto.leaveWithPay = rs.getInt("leave_with_pay");
        dto.leaveWithoutPay = rs.getInt("leave_without_pay");
        dto.overtimeHours = rs.getDouble("overtime_hours");
        dto.hoursWorked = dto.workingDaysInMonth * 8.0;

        LeaveDetailsDTO leaveDetails = leaveDataProvider.getLeaveDetailsForPayroll(empID, payPeriod, dto.workingDaysInMonth);
        if (leaveDetails != null) {
            dto.workingDaysInMonth = leaveDetails.getWorkingDaysInMonth() > 0
                    ? leaveDetails.getWorkingDaysInMonth()
                    : dto.workingDaysInMonth;
            if (leaveDetails.getLeaveWithPay() > 0) {
                dto.leaveWithPay = leaveDetails.getLeaveWithPay();
            }
            if (leaveDetails.getLeaveWithoutPay() > 0) {
                dto.leaveWithoutPay = leaveDetails.getLeaveWithoutPay();
            }
            if (leaveDetails.getOvertimeHours() > 0) {
                dto.overtimeHours = leaveDetails.getOvertimeHours();
            }
            dto.hoursWorked = dto.workingDaysInMonth * 8.0;
        }
        return dto;
    }

    private FinancialsDTO mapFinancials(ResultSet rs, String empID) throws SQLException {
        FinancialsDTO dto = new FinancialsDTO();
        dto.pendingClaims = rs.getDouble("pending_claims");
        dto.approvedReimbursement = rs.getDouble("approved_reimbursement");
        dto.insurancePremium = rs.getDouble("insurance_premium");
        dto.declaredInvestments = rs.getDouble("declared_investments");
        double subsystemApproved = expenseDataProvider.getApprovedClaimsForPayroll().stream()
                .filter(claim -> empID.equals(claim.getEmployeeId()))
                .map(ApprovedClaimDTO::getAmount)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(java.math.BigDecimal::doubleValue)
                .sum();
        if (subsystemApproved > 0) {
            dto.approvedReimbursement = subsystemApproved;
        }
        return dto;
    }

    private TaxContextDTO mapTaxContext(ResultSet rs) throws SQLException {
        TaxContextDTO dto = new TaxContextDTO();
        dto.countryCode = coalesce(rs.getString("tax_country_code"), rs.getString("emp_country_code"), "IN");
        dto.currencyCode = coalesce(rs.getString("tax_currency_code"), rs.getString("emp_currency_code"), "INR");
        dto.taxRegime = coalesce(rs.getString("tax_tax_regime"), rs.getString("emp_tax_regime"), "OLD");
        dto.stateName = coalesce(rs.getString("tax_state_name"), rs.getString("emp_state_name"), "");
        dto.filingStatus = coalesce(rs.getString("tax_filing_status"), rs.getString("emp_filing_status"), "SINGLE");
        dto.taxCode = coalesce(rs.getString("tax_tax_code"), rs.getString("emp_tax_code"), "");
        dto.nationalIDNumber = coalesce(
                rs.getString("tax_national_id_number"),
                rs.getString("emp_national_id_number"),
                "");
        return dto;
    }

    private String coalesce(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return values.length == 0 ? "" : values[values.length - 1];
    }

    private void ensurePayrollResultsTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS payroll_results (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        batch_id VARCHAR,
                        emp_id VARCHAR,
                        record_id VARCHAR,
                        final_gross_pay DOUBLE,
                        final_net_pay DOUBLE,
                        penalty_amount DOUBLE,
                        pf_amount DOUBLE,
                        tax_deducted DOUBLE,
                        payout_amount DOUBLE,
                        created_at TIMESTAMP
                    )
                    """);
        }
    }

    private void ensurePayrollAuditTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS payroll_audit_log (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        batch_id VARCHAR,
                        emp_id VARCHAR,
                        log_level VARCHAR,
                        message VARCHAR,
                        created_at TIMESTAMP
                    )
                    """);
        }
    }

    private Set<String> getTableColumns(Connection conn, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                columns.add(rs.getString("name").toLowerCase(Locale.ROOT));
            }
        }
        return columns;
    }

    private void putIfPresent(Map<String, Object> values, Set<String> columns, Object value, String... candidates) {
        for (String candidate : candidates) {
            if (columns.contains(candidate.toLowerCase(Locale.ROOT))) {
                values.put(candidate, value);
                return;
            }
        }
    }

    private String buildInsertSql(String tableName, Map<String, Object> values) {
        String joinedColumns = String.join(", ", values.keySet());
        String placeholders = String.join(", ", java.util.Collections.nCopies(values.size(), "?"));
        return "INSERT OR REPLACE INTO " + tableName + " (" + joinedColumns + ") VALUES (" + placeholders + ")";
    }
}
