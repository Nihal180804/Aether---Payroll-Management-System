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

public class PayrollDataProviderImpl implements ExpenseDataProvider {

    private final String dbUrl;

    public PayrollDataProviderImpl() {
        this.dbUrl = DatabaseConfig.getJdbcUrl();
    }

    @Override
    public List<ApprovedClaimDTO> getApprovedClaimsForPayroll() {
        List<ApprovedClaimDTO> claims = loadApprovedExpenseClaims();
        if (!claims.isEmpty()) {
            return claims;
        }
        return loadApprovedClaimsFromFinancialFallback();
    }

    private List<ApprovedClaimDTO> loadApprovedExpenseClaims() {
        String sql = """
                SELECT
                    ec.claim_id,
                    ec.emp_id,
                    ec.amount,
                    COALESCE(ca.approval_id, ec.created_at) AS approval_marker,
                    ec.expense_date
                FROM expense_claims ec
                LEFT JOIN claim_approvals ca
                    ON ca.claim_id = ec.claim_id
                WHERE UPPER(COALESCE(ca.status, ec.status, '')) = 'APPROVED'
                ORDER BY ec.emp_id, ec.expense_date DESC, ec.claim_id
                """;

        List<ApprovedClaimDTO> claims = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ApprovedClaimDTO dto = new ApprovedClaimDTO();
                dto.setClaimId(rs.getString("claim_id"));
                dto.setEmployeeId(rs.getString("emp_id"));
                dto.setAmount(BigDecimal.valueOf(rs.getDouble("amount")));
                dto.setApprovalDate(rs.getString("expense_date"));
                claims.add(dto);
            }
        } catch (SQLException e) {
            return List.of();
        }
        return claims;
    }

    private List<ApprovedClaimDTO> loadApprovedClaimsFromFinancialFallback() {
        String sql = """
                SELECT f.emp_id, f.pay_period, f.approved_reimbursement
                FROM financials f
                JOIN (
                    SELECT emp_id, MAX(pay_period) AS latest_period
                    FROM financials
                    GROUP BY emp_id
                ) latest
                  ON latest.emp_id = f.emp_id
                 AND latest.latest_period = f.pay_period
                WHERE COALESCE(f.approved_reimbursement, 0) > 0
                ORDER BY f.emp_id
                """;

        List<ApprovedClaimDTO> claims = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ApprovedClaimDTO dto = new ApprovedClaimDTO();
                String employeeId = rs.getString("emp_id");
                String payPeriod = rs.getString("pay_period");
                dto.setClaimId("FIN-" + employeeId + "-" + payPeriod);
                dto.setEmployeeId(employeeId);
                dto.setAmount(BigDecimal.valueOf(rs.getDouble("approved_reimbursement")));
                dto.setApprovalDate(payPeriod + "-01");
                claims.add(dto);
            }
        } catch (SQLException e) {
            return List.of();
        }
        return claims;
    }
}
