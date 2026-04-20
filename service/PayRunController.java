package service;

import exception.PayrollException;
import model.Employee;
import model.PayrollRecord;
import pattern.PayrollFacade;
import util.AuditLogger;

import java.util.*;

/**
 * =============================================================================
 * CLASS: PayRunController
 * =============================================================================
 * From the class diagram:
 *   Attributes : BatchID (String), PayPeriod (String)
 *   Methods    : executeBatchPayroll() : void, verifyCalculations() : boolean
 *   Relationship: "generates" PayrollRecord, "Invokes" DigitalPayslipGenerator
 *                 "Fetches Data From" Employee
 *
 * This is the top-level batch controller. It:
 *   1. Validates the pay period (INVALID_PAY_PERIOD)
 *   2. Fetches attendance (ATTENDANCE_LOG_TIMEOUT — retry 3×)
 *   3. Iterates all employees, calls PayrollFacade.processEmployee()
 *   4. Catches MAJOR per-employee exceptions and skips those employees
 *   5. Locks the pay period after a successful run
 *
 * GRASP — Controller:
 *   Receives "Run Payroll" command from the UI layer and delegates all
 *   business logic to PayrollFacade and underlying services.
 *
 * GRASP — Creator:
 *   Creates PayrollRecord objects (it "records" them and has the batchID/payPeriod).
 * =============================================================================
 */
public class PayRunController {

    private final String        batchID;
    private final String        payPeriod;
    private final PayrollFacade facade;
    private final AuditLogger   auditLogger;

    private static final int MAX_ATTENDANCE_RETRIES = 3; // Per ATTENDANCE_LOG_TIMEOUT plan

    // Tracks processed pay periods to prevent duplicate runs
    private final Set<String> processedPayPeriods = new HashSet<>();

    public PayRunController(String batchID, String payPeriod,
                            PayrollFacade facade, AuditLogger auditLogger) {
        this.batchID     = batchID;
        this.payPeriod   = payPeriod;
        this.facade      = facade;
        this.auditLogger = auditLogger;
    }

    /**
     * TODO ── executeBatchPayroll()
     * ─────────────────────────────────────────────────────────────────────────
     * From class diagram: +executeBatchPayroll() : void
     *
     * OUTLINE:
     *   1. Call verifyPayPeriod(payPeriod)  → throws InvalidPayPeriod if duplicate
     *
     *   2. Call fetchAttendanceWithRetry()  → retries 3× on timeout
     *
     *   3. For each Employee in the list:
     *        a. Create a new PayrollRecord:
     *             String recordID = batchID + "-" + emp.getEmpID();
     *             PayrollRecord record = new PayrollRecord(recordID, emp.getEmpID(), batchID, payPeriod);
     *
     *        b. Try: facade.processEmployee(emp, record)
     *             → add record to completedList
     *
     *        c. Catch MissingWorkState (MAJOR):
     *             → record.flagForHrReview(...)
     *             → auditLogger.logMajorError(...)
     *             → add empID to skippedList
     *             → continue (do NOT break the loop)
     *
     *        d. Catch MissingBaseSalary (MAJOR):
     *             → auditLogger.logMajorError(...)
     *             → add to skippedList, continue
     *
     *   4. Lock the pay period: processedPayPeriods.add(payPeriod)
     *
     *   5. Print batch summary
     *
     * HINT: The for-each loop should NOT stop when one employee fails.
     *       Only InvalidPayPeriod (before the loop) stops the whole batch.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public List<PayrollRecord> executeBatchPayroll(List<Employee> employees)
            throws PayrollException.InvalidPayPeriod {

        // Step 1: verifyPayPeriod(payPeriod)
        verifyPayPeriod(payPeriod);

        // Step 2: fetchAttendanceWithRetry()
        fetchAttendanceWithRetry();

        List<PayrollRecord> completedRecords = new ArrayList<>();
        List<String>        skippedEmployees = new ArrayList<>();

        // Step 3: Loop through employees
        for (Employee emp : employees) {
            // 3a: Create PayrollRecord
            String recordID = batchID + "-" + emp.getEmpID();
            PayrollRecord record = new PayrollRecord(recordID, emp.getEmpID(), batchID, payPeriod);

            // 3b & 3c & 3d: try { facade.processEmployee(...) } catch (...)
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

        // Step 4: Lock pay period
        processedPayPeriods.add(payPeriod);

        // Step 5: Print summary
        System.out.printf("[BATCH] Summary: %d processed, %d skipped.%n", completedRecords.size(), skippedEmployees.size());

        return completedRecords;
    }

    /**
     * TODO ── verifyCalculations()
     * ─────────────────────────────────────────────────────────────────────────
     * From class diagram: -verifyCalculations() : boolean
     *
     * Should verify that the batch completed with no critical inconsistencies.
     * For now: print a confirmation message and return true.
     *
     * HINT: In production this would checksum totals. For the demo, just:
     *   System.out.println("[VERIFY] Calculations verified.");
     *   return true;
     * ─────────────────────────────────────────────────────────────────────────
     */
    public boolean verifyCalculations() {
        System.out.println("[VERIFY] Calculations verified.");
        return true;
    }

    /**
     * Validates that this pay period hasn't already been processed.
     * Per INVALID_PAY_PERIOD plan: throw immediately, lock the Run button.
     */
    private void verifyPayPeriod(String payPeriod) throws PayrollException.InvalidPayPeriod {
        if (payPeriod == null || payPeriod.isBlank()) {
            throw new PayrollException.InvalidPayPeriod("Pay period cannot be null or blank.");
        }
        if (processedPayPeriods.contains(payPeriod)) {
            throw new PayrollException.InvalidPayPeriod("Payroll has already been run for period: " + payPeriod);
        }
    }

    /**
     * Fetches attendance data with up to MAX_ATTENDANCE_RETRIES attempts.
     * Per ATTENDANCE_LOG_TIMEOUT plan: retry 3×, then alert admin.
     *
     * TODO ── fetchAttendanceWithRetry()
     * ─────────────────────────────────────────────────────────────────────────
     * HINT — structure:
     *   int attempts = 0;
     *   while (attempts < MAX_ATTENDANCE_RETRIES) {
     *       try {
     *           // simulate fetch — print "[ATTENDANCE] Fetching... attempt X"
     *           // In production: attendanceDB.fetch(payPeriod);
     *           return; // success — exit loop
     *       } catch (Exception e) {
     *           attempts++;
     *           auditLogger.logWarning(batchID, "ATTENDANCE_LOG_TIMEOUT retry " + attempts);
     *           if (attempts >= MAX_ATTENDANCE_RETRIES) {
     *               System.err.println("[ALERT] Admin: Upload attendance CSV manually.");
     *           }
     *       }
     *   }
     * ─────────────────────────────────────────────────────────────────────────
     */
    private void fetchAttendanceWithRetry() {
        int attempts = 0;
        while (attempts < MAX_ATTENDANCE_RETRIES) {
            try {
                System.out.printf("[ATTENDANCE] Fetching... attempt %d%n", attempts + 1);
                // In a real application, this would be a call to an external service.
                // To simulate failure, we could add a random exception throw.
                // For this implementation, we assume it succeeds on the first try.
                return; // Success
            } catch (Exception e) { // This would be a specific timeout exception
                attempts++;
                auditLogger.logWarning(batchID, "ATTENDANCE_LOG_TIMEOUT retry " + attempts);
                if (attempts >= MAX_ATTENDANCE_RETRIES) {
                    System.err.println("[ALERT] Admin: Upload attendance CSV manually.");
                }
            }
        }
    }
}
