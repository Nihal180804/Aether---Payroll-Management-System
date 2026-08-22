package com.pesu.expensesubsystem.integration;

import java.math.BigDecimal;

/**
 * Integration DTO exposing a single approved expense claim to the payroll subsystem.
 *
 * <p>This is a local adapter type that stands in for the Expense subsystem's published
 * integration contract, letting payroll consume approved reimbursement claims without
 * depending on the expense subsystem's internal entities.
 */
public class ApprovedClaimDTO {

    private final String claimId;
    private final String employeeId;
    private final BigDecimal amount;

    public ApprovedClaimDTO(String claimId, String employeeId, BigDecimal amount) {
        this.claimId = claimId;
        this.employeeId = employeeId;
        this.amount = amount;
    }

    public String getClaimId() {
        return claimId;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}
