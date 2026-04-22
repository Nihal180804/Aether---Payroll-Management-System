package com.pesu.expensesubsystem.integration;

import java.util.List;

public interface ExpenseDataProvider {
    List<ApprovedClaimDTO> getApprovedClaimsForPayroll();
}
