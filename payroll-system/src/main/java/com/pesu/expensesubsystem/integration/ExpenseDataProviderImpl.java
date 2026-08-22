package com.pesu.expensesubsystem.integration;

import com.payroll.system.util.DatabaseConfig;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DB-backed provider that reads approved expense claims from the shared HRMS database.
 *
 * <p>Reads the {@code expense_claims} table and surfaces only rows whose status is
 * {@code APPROVED}. Any database problem degrades gracefully to an empty list so a payroll
 * run is never blocked by the expense integration.
 */
public class ExpenseDataProviderImpl implements ExpenseDataProvider {

    private static final String APPROVED_CLAIMS_SQL = """
            SELECT claim_id, emp_id, amount
            FROM expense_claims
            WHERE UPPER(COALESCE(status, '')) = 'APPROVED'
            """;

    @Override
    public List<ApprovedClaimDTO> getApprovedClaimsForPayroll() {
        List<ApprovedClaimDTO> claims = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DatabaseConfig.getJdbcUrl());
             PreparedStatement ps = conn.prepareStatement(APPROVED_CLAIMS_SQL);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                double amount = rs.getDouble("amount");
                BigDecimal value = rs.wasNull() ? null : BigDecimal.valueOf(amount);
                claims.add(new ApprovedClaimDTO(rs.getString("claim_id"), rs.getString("emp_id"), value));
            }
        } catch (SQLException e) {
            System.err.printf("ExpenseDataProviderImpl.getApprovedClaimsForPayroll failed: %s%n", e.getMessage());
        }
        return claims;
    }
}
