package com.pesu.leavesubsystem.integration;

import com.payroll.system.util.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DB-backed provider that contributes Leave-subsystem data to a payroll run.
 *
 * <p>It surfaces <em>approved</em> overtime from the {@code overtime_records} table, which is
 * purely additive to pay and therefore safe to override. Leave-day accounting (paid/unpaid
 * days) is deliberately left to the attendance table of record: {@code leave_records} stores
 * only date ranges and leave types with no paid/unpaid classification, and misclassifying an
 * unpaid day would incorrectly dock salary. Returning {@code null} (or zero day counts) tells
 * payroll to keep the attendance figures.
 */
public class LeaveDataProviderImpl {

    private static final String APPROVED_OVERTIME_SQL = """
            SELECT COALESCE(SUM(overtime_hours), 0) AS total_overtime
            FROM overtime_records
            WHERE emp_id = ?
              AND UPPER(COALESCE(approval_status, '')) = 'APPROVED'
            """;

    /**
     * Returns leave/overtime details to layer over attendance, or {@code null} when the Leave
     * subsystem has nothing authoritative to contribute for this employee and period.
     */
    public LeaveDetailsDTO getLeaveDetailsForPayroll(String empID, String payPeriod, int workingDaysInMonth) {
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getJdbcUrl());
             PreparedStatement ps = conn.prepareStatement(APPROVED_OVERTIME_SQL)) {

            ps.setString(1, empID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double overtime = rs.getDouble("total_overtime");
                    if (overtime > 0) {
                        // Zero day counts => payroll keeps the attendance-of-record leave figures.
                        return new LeaveDetailsDTO(0, 0, 0, overtime);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.printf("LeaveDataProviderImpl.getLeaveDetailsForPayroll failed for empID=%s: %s%n",
                    empID, e.getMessage());
        }
        return null;
    }
}
