package com.payroll.system.presenter;

import java.util.List;

/**
 * INTERFACE: PayrollPresenter
 * =============================================================================
 * The ONLY contract the UI layer knows about. The UI never imports any class
 * from model, service, pattern, exception, or util packages.
 *
 * To replace the UI entirely, create a new class (CLI, REST controller, etc.)
 * that holds a reference to a PayrollPresenter implementation.
 *
 * To replace the backend (e.g., swap mock DB for real DB), swap the
 * PayrollPresenterImpl's IPayrollRepository — the UI code is untouched.
 * =============================================================================
 */
public interface PayrollPresenter {

    /**
     * Loads all active employees for the given pay period.
     * @param payPeriod e.g. "2025-06"
     * @return list of view models, one per employee
     */
    List<EmployeeViewModel> loadAllEmployees(String payPeriod);

    /**
     * Returns the count of active employees.
     */
    int getEmployeeCount();

    /**
     * Executes a full payroll batch.
     * @param batchId  unique ID for this run e.g. "BATCH-2025-06"
     * @param payPeriod e.g. "2025-06"
     * @return BatchResult containing result rows and error info if any
     */
    BatchResult runBatch(String batchId, String payPeriod);

    /**
     * Returns current audit log entries as formatted strings.
     */
    List<String> getAuditLog();

    /**
     * Clears all audit log entries.
     */
    void clearAuditLog();

    /**
     * Verifies the last completed batch's calculations.
     * @return true if verification passed
     */
    boolean verifyLastBatch();

    /**
     * Status string describing the active data source.
     * e.g. "MockDB Active" or "PostgreSQL Connected"
     */
    String getDbStatus();

    /**
     * Retrieves the latest payslip for a specific employee and pay period.
     */
    String getEmployeePayslip(String employeeId, String payPeriod);

    /**
     * Retrieves all latest payslips for a pay period as formatted text.
     */
    String getAllEmployeePayslips(String payPeriod);

    /**
     * Retrieves all latest payslips for a pay period as CSV.
     */
    String getAllEmployeePayslipsCsv(String payPeriod);
}
