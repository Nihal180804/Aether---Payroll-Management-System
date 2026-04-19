package exception;

/**
 * =============================================================================
 * CLASS: PayrollException  (Root of all custom exceptions)
 * =============================================================================
 * All 11 exceptions from the OCBC Exception Table are defined here as
 * static inner classes. This keeps the exception hierarchy in one file and
 * makes imports clean (just import exception.PayrollException.*).
 *
 * SOLID — Open/Closed:
 *   New exceptions can be added as new inner classes without modifying
 *   existing catch blocks.
 *
 * Category meanings (from the exception table):
 *   MAJOR   → Halt processing for THIS employee, continue batch for others
 *   WARNING → Log it, apply a safe default, keep going — do NOT throw upward
 *   MINOR   → Reject the specific item, continue everything else
 * =============================================================================
 */
public class PayrollException extends RuntimeException {

    public enum Category { MAJOR, WARNING, MINOR }

    private final String   exceptionName;
    private final Category category;
    private final String   empID; // null for batch-level errors

    public PayrollException(String exceptionName, Category category, String message, String empID) {
        super(message);
        this.exceptionName = exceptionName;
        this.category      = category;
        this.empID         = empID;
    }

    public PayrollException(String exceptionName, Category category, String message) {
        this(exceptionName, category, message, null);
    }

    public String   getExceptionName() { return exceptionName; }
    public Category getCategory()      { return category; }
    public String   getEmpID()         { return empID; }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Exception 1 — PAYSLIP_GENERATION_FAILED
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── PayslipGenerationFailed
     * ─────────────────────────────────────────────────────────────────────────
     * Category : WARNING
     * Thrown by: DigitalPayslipGenerator
     * Plan     : Log error, notify IT, queue employee for retry after batch.
     *
     * HINT: Call super() with:
     *   exceptionName = "PAYSLIP_GENERATION_FAILED"
     *   category      = Category.WARNING
     *   message       = "Unable to generate PDF payslip for Employee ID: " + empID
     * ─────────────────────────────────────────────────────────────────────────
     */
    public static class PayslipGenerationFailed extends PayrollException {
        public PayslipGenerationFailed(String empID) {
            // TODO: call super(...) with the correct name, category, and message
            super("PAYSLIP_GENERATION_FAILED", Category.WARNING, "TODO: fill in message", empID);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Exception 2 — MISSING_TAX_REGIME
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── MissingTaxRegime
     * ─────────────────────────────────────────────────────────────────────────
     * Category : WARNING
     * Thrown by: IncomeTaxTDS
     * Plan     : Default to Old Tax Regime, continue payroll, email employee.
     *
     * HINT: Message should say which employee ID is missing their declaration.
     *       After catching this in IncomeTaxTDS, set taxRegime = "OLD" and continue.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public static class MissingTaxRegime extends PayrollException {
        public MissingTaxRegime(String empID) {
            // TODO: fill in correct message from the exception table
            super("MISSING_TAX_REGIME", Category.WARNING, "TODO: fill in message", empID);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Exception 3 — MISSING_WORK_STATE
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── MissingWorkState
     * ─────────────────────────────────────────────────────────────────────────
     * Category : MAJOR
     * Thrown by: StatuaryDeduction (calculatePT)
     * Plan     : Suspend employee's payroll. Flag record as "Requires HR Input".
     *
     * HINT: This IS thrown upward (MAJOR). PayRunController must catch it,
     *       skip this employee, and call record.flagForHrReview(...).
     * ─────────────────────────────────────────────────────────────────────────
     */
    public static class MissingWorkState extends PayrollException {
        public MissingWorkState(String empID) {
            // TODO: fill in correct message from the exception table
            super("MISSING_WORK_STATE", Category.MAJOR, "TODO: fill in message", empID);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Exception 4 — DUPLICATE_CLAIM_ID
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── DuplicateClaimId
     * ─────────────────────────────────────────────────────────────────────────
     * Category : MINOR
     * Thrown by: ReimbursementTracker
     * Plan     : Reject the duplicate, proceed with rest of payroll, notify employee.
     *
     * HINT: This should NOT stop the batch or even the employee — just return 0
     *       for the reimbursement and log a warning.
     *       Store the claimID as a field so the catch block can log which claim.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public static class DuplicateClaimId extends PayrollException {
        private final String claimID;
        public DuplicateClaimId(String empID, String claimID) {
            // TODO: fill in correct message from the exception table
            super("DUPLICATE_CLAIM_ID", Category.MINOR, "TODO: fill in message", empID);
            this.claimID = claimID;
        }
        public String getClaimID() { return claimID; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Exception 5 — MISSING_PERFORMANCE_RATING
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── MissingPerformanceRating
     * ─────────────────────────────────────────────────────────────────────────
     * Category : WARNING
     * Thrown by: BonusDistributor
     * Plan     : Set bonus = 0. Add to Arrears Queue for retroactive payout.
     *
     * HINT: After catching, call record.setBonusArrears(estimatedBonus)
     *       and return 0.0 as the bonus for this cycle.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public static class MissingPerformanceRating extends PayrollException {
        public MissingPerformanceRating(String empID) {
            // TODO: fill in correct message from the exception table
            super("MISSING_PERFORMANCE_RATING", Category.WARNING, "TODO: fill in message", empID);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Exception 6 — INVALID_PAY_PERIOD
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── InvalidPayPeriod
     * ─────────────────────────────────────────────────────────────────────────
     * Category : MAJOR
     * Thrown by: PayRunController.verifyPayPeriod()
     * Plan     : Terminate the batch immediately. Lock the "Run Payroll" button.
     *
     * HINT: This is thrown BEFORE the employee loop starts, so it stops
     *       the entire batch — not just one employee.
     *       No empID needed here (batch-level error).
     * ─────────────────────────────────────────────────────────────────────────
     */
    public static class InvalidPayPeriod extends PayrollException {
        public InvalidPayPeriod(String payPeriod) {
            // TODO: fill in correct message, include the payPeriod string
            super("INVALID_PAY_PERIOD", Category.MAJOR, "TODO: fill in message");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Exception 7 — REPORT_FORMAT_ERROR
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── ReportFormatError
     * ─────────────────────────────────────────────────────────────────────────
     * Category : WARNING
     * Thrown by: ComplianceAndReporting
     * Plan     : Save raw data to temp table, skip auto-export, prompt Finance admin.
     *
     * HINT: No empID needed — this is a report-level error, not employee-level.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public static class ReportFormatError extends PayrollException {
        public ReportFormatError() {
            // TODO: fill in correct message from the exception table
            super("REPORT_FORMAT_ERROR", Category.WARNING, "TODO: fill in message");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Exception 8 — MISSING_BASE_SALARY
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── MissingBaseSalary
     * ─────────────────────────────────────────────────────────────────────────
     * Category : MAJOR
     * Thrown by: Employee.Builder.build() when basicPay <= 0
     * Plan     : Halt payroll for this employee, skip to next, log alert for HR.
     *
     * HINT: Thrown inside the Builder, so PayRunController's try-catch will
     *       catch it and skip that employee.
     * ─────────────────────────────────────────────────════════════════════════
     */
    public static class MissingBaseSalary extends PayrollException {
        public MissingBaseSalary(String empID) {
            // TODO: fill in correct message from the exception table
            super("MISSING_BASE_SALARY", Category.MAJOR, "TODO: fill in message", empID);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Exception 9 — ATTENDANCE_LOG_TIMEOUT
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── AttendanceLogTimeout
     * ─────────────────────────────────────────────────────────────────────────
     * Category : MAJOR
     * Thrown by: PayRunController.fetchAttendanceWithRetry()
     * Plan     : Retry 3 times. If all fail, alert admin to upload CSV manually.
     *
     * HINT: Store retryCount as a field. In PayRunController, use a while loop:
     *   int attempts = 0;
     *   while (attempts < MAX_RETRIES) { try { fetch(); break; } catch(...) { attempts++; } }
     * ─────────────────────────────────────────────────────────────────────────
     */
    public static class AttendanceLogTimeout extends PayrollException {
        private final int retryCount;
        public AttendanceLogTimeout(int retryCount) {
            // TODO: fill in correct message from the exception table
            super("ATTENDANCE_LOG_TIMEOUT", Category.MAJOR, "TODO: fill in message");
            this.retryCount = retryCount;
        }
        public int getRetryCount() { return retryCount; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Exception 10 — NEGATIVE_NET_PAY
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── NegativeNetPay
     * ─────────────────────────────────────────────────────────────────────────
     * Category : MAJOR
     * Thrown by: PayrollRecord.calculateTotals()
     * Plan     : Set net pay to 0. Carry over remaining as arrears. Flag for HR.
     *
     * HINT: Store the actual negative value as computedNetPay so the catch
     *       block knows how much to carry over as arrears.
     *       Math.abs(computedNetPay) gives the arrears amount.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public static class NegativeNetPay extends PayrollException {
        private final double computedNetPay; // The raw negative figure before clamping
        public NegativeNetPay(String empID, double computedNetPay) {
            // TODO: fill in correct message from the exception table
            super("NEGATIVE_NET_PAY", Category.MAJOR, "TODO: fill in message", empID);
            this.computedNetPay = computedNetPay;
        }
        public double getComputedNetPay() { return computedNetPay; }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Exception 11 — EXCEEDS_CLAIM_LIMIT
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── ExceedsClaimLimit
     * ─────────────────────────────────────────────────────────────────────────
     * Category : WARNING
     * Thrown by: ReimbursementTracker
     * Plan     : Process only up to the max limit. Discard excess. Notify employee.
     *
     * HINT: Store both claimAmount and maxLimit as fields so the catch block
     *       can cap the value: return maxLimit (not claimAmount).
     * ─────────────────────────────────────────────────────────────────────────
     */
    public static class ExceedsClaimLimit extends PayrollException {
        private final double claimAmount;
        private final double maxLimit;
        public ExceedsClaimLimit(String empID, double claimAmount, double maxLimit) {
            // TODO: fill in correct message from the exception table
            super("EXCEEDS_CLAIM_LIMIT", Category.WARNING, "TODO: fill in message", empID);
            this.claimAmount = claimAmount;
            this.maxLimit    = maxLimit;
        }
        public double getClaimAmount() { return claimAmount; }
        public double getMaxLimit()    { return maxLimit; }
    }
}
