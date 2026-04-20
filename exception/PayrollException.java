package exception;

/**
 * =============================================================================
 * CLASS: PayrollException  (Root of all custom exceptions)
 * =============================================================================
 */
public class PayrollException extends RuntimeException {

    public enum Category { MAJOR, WARNING, MINOR }

    private final String   exceptionName;
    private final Category category;
    private final String   empID; 

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

    // Exception 1 — PAYSLIP_GENERATION_FAILED
    public static class PayslipGenerationFailed extends PayrollException {
        public PayslipGenerationFailed(String empID) {
            super("PAYSLIP_GENERATION_FAILED", Category.WARNING, 
                "Unable to generate PDF payslip for Employee ID: " + empID, empID);
        }
    }

    // Exception 2 — MISSING_TAX_REGIME
    public static class MissingTaxRegime extends PayrollException {
        public MissingTaxRegime(String empID) {
            super("MISSING_TAX_REGIME", Category.WARNING, 
                "Tax regime declaration missing for Employee ID: " + empID + ". Defaulting to OLD regime.", empID);
        }
    }

    // Exception 3 — MISSING_WORK_STATE
    public static class MissingWorkState extends PayrollException {
        public MissingWorkState(String empID) {
            super("MISSING_WORK_STATE", Category.MAJOR, 
                "Critical Error: No work state assigned to Employee ID: " + empID + ". PT calculation impossible.", empID);
        }
    }

    // Exception 4 — DUPLICATE_CLAIM_ID
    public static class DuplicateClaimId extends PayrollException {
        private final String claimID;
        public DuplicateClaimId(String empID, String claimID) {
            super("DUPLICATE_CLAIM_ID", Category.MINOR, 
                "Reimbursement claim " + claimID + " has already been processed for Employee ID: " + empID, empID);
            this.claimID = claimID;
        }
        public String getClaimID() { return claimID; }
    }

    // Exception 5 — MISSING_PERFORMANCE_RATING
    public static class MissingPerformanceRating extends PayrollException {
        public MissingPerformanceRating(String empID) {
            super("MISSING_PERFORMANCE_RATING", Category.WARNING, 
                "Performance rating not found for Employee ID: " + empID + ". Bonus queued as arrears.", empID);
        }
    }

    // Exception 6 — INVALID_PAY_PERIOD
    public static class InvalidPayPeriod extends PayrollException {
        public InvalidPayPeriod(String payPeriod) {
            super("INVALID_PAY_PERIOD", Category.MAJOR, 
                "The requested pay period '" + payPeriod + "' is closed or invalid. Batch termination triggered.");
        }
    }

    // Exception 7 — REPORT_FORMAT_ERROR
    public static class ReportFormatError extends PayrollException {
        public ReportFormatError() {
            super("REPORT_FORMAT_ERROR", Category.WARNING, 
                "Compliance report generation encountered a formatting error. Saving raw data to temp table.");
        }
    }

    // Exception 8 — MISSING_BASE_SALARY
    public static class MissingBaseSalary extends PayrollException {
        public MissingBaseSalary(String empID) {
            super("MISSING_BASE_SALARY", Category.MAJOR, 
                "Mandatory basic pay field is missing or zero for Employee ID: " + empID, empID);
        }
    }

    // Exception 9 — ATTENDANCE_LOG_TIMEOUT
    public static class AttendanceLogTimeout extends PayrollException {
        private final int retryCount;
        public AttendanceLogTimeout(int retryCount) {
            super("ATTENDANCE_LOG_TIMEOUT", Category.MAJOR, 
                "Attendance service timed out after " + retryCount + " retries. Manual CSV upload required.");
            this.retryCount = retryCount;
        }
        public int getRetryCount() { return retryCount; }
    }

    // Exception 10 — NEGATIVE_NET_PAY
    public static class NegativeNetPay extends PayrollException {
        private final double computedNetPay; 
        public NegativeNetPay(String empID, double computedNetPay) {
            super("NEGATIVE_NET_PAY", Category.MAJOR, 
                "Calculation error: Net pay for Employee ID " + empID + " is negative: " + computedNetPay, empID);
            this.computedNetPay = computedNetPay;
        }
        public double getComputedNetPay() { return computedNetPay; }
    }

    // Exception 11 — EXCEEDS_CLAIM_LIMIT
    public static class ExceedsClaimLimit extends PayrollException {
        private final double claimAmount;
        private final double maxLimit;
        public ExceedsClaimLimit(String empID, double claimAmount, double maxLimit) {
            super("EXCEEDS_CLAIM_LIMIT", Category.WARNING, 
                "Claim " + claimAmount + " for Employee " + empID + " exceeds the grade limit of " + maxLimit, empID);
            this.claimAmount = claimAmount;
            this.maxLimit    = maxLimit;
        }
        public double getClaimAmount() { return claimAmount; }
        public double getMaxLimit()    { return maxLimit; }
    }
}