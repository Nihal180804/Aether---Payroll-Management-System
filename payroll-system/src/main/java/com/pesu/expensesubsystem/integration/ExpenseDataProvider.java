package com.pesu.expensesubsystem.integration;

import java.util.List;

/**
 * Integration contract the Expense subsystem exposes to payroll.
 *
 * <p>Payroll depends only on this interface so the concrete provider (real DB-backed or a
 * test double) can be swapped without touching the reimbursement logic.
 */
public interface ExpenseDataProvider {

    /** Returns all expense claims that have been approved and are ready for reimbursement. */
    List<ApprovedClaimDTO> getApprovedClaimsForPayroll();
}
