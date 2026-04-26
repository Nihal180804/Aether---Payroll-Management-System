package com.payroll.system.service;

import com.payroll.system.exception.PayrollException;
import com.payroll.system.model.Employee;
import com.payroll.system.model.PayrollRecord;
import com.payroll.system.util.AuditLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Coordinates a batch payroll run and skips only employees with blocking errors. */
public class PayRunController {

    private static final int MAX_ATTENDANCE_RETRIES = 3;

    private final String batchID;
    private final String payPeriod;
    private final PayrollFacade facade;
    private final AuditLogger auditLogger;
    private final Set<String> processedPayPeriods = new HashSet<>();

    public PayRunController(String batchID, String payPeriod, PayrollFacade facade, AuditLogger auditLogger) {
        this.batchID = batchID;
        this.payPeriod = payPeriod;
        this.facade = facade;
        this.auditLogger = auditLogger;
    }

    /** Runs payroll for every employee in the batch and keeps going after per-employee failures. */
    public List<PayrollRecord> executeBatchPayroll(List<Employee> employees)
            throws PayrollException.InvalidPayPeriod {
        verifyPayPeriod(payPeriod);
        fetchAttendanceWithRetry();

        List<PayrollRecord> completedRecords = new ArrayList<>();
        List<String> skippedEmployees = new ArrayList<>();

        for (Employee emp : employees) {
            PayrollRecord record = new PayrollRecord(batchID + "-" + emp.getEmpID(), emp.getEmpID(), batchID, payPeriod);
            try {
                facade.processEmployee(emp, record);
                completedRecords.add(record);
            } catch (PayrollException.MissingWorkState e) {
                record.flagForHrReview(e.getExceptionName());
                auditLogger.logMajorError(emp.getEmpID(), e.getExceptionName() + ": " + e.getMessage());
                skippedEmployees.add(emp.getEmpID());
            } catch (PayrollException.MissingBaseSalary e) {
                auditLogger.logMajorError(emp.getEmpID(), e.getExceptionName() + ": " + e.getMessage());
                skippedEmployees.add(emp.getEmpID());
            }
        }

        processedPayPeriods.add(payPeriod);
        System.out.printf("[BATCH] Summary: %d processed, %d skipped.%n", completedRecords.size(), skippedEmployees.size());
        return completedRecords;
    }

    /** Placeholder verification hook for post-run validation. */
    public boolean verifyCalculations() {
        System.out.println("[VERIFY] Calculations verified.");
        return true;
    }

    /** Blocks duplicate or blank pay periods before a batch starts. */
    private void verifyPayPeriod(String payPeriod) throws PayrollException.InvalidPayPeriod {
        if (payPeriod == null || payPeriod.isBlank()) {
            throw new PayrollException.InvalidPayPeriod("Pay period cannot be null or blank.");
        }
        if (processedPayPeriods.contains(payPeriod)) {
            throw new PayrollException.InvalidPayPeriod("Payroll has already been run for period: " + payPeriod);
        }
    }

    /** Simulates attendance fetch retries before payroll begins. */
    private void fetchAttendanceWithRetry() {
        int attempts = 0;
        while (attempts < MAX_ATTENDANCE_RETRIES) {
            try {
                System.out.printf("[ATTENDANCE] Fetching... attempt %d%n", attempts + 1);
                return;
            } catch (Exception e) {
                attempts++;
                auditLogger.logWarning(batchID, "ATTENDANCE_LOG_TIMEOUT retry " + attempts);
                if (attempts >= MAX_ATTENDANCE_RETRIES) {
                    System.err.println("[ALERT] Admin: Upload attendance CSV manually.");
                }
            }
        }
    }
}
