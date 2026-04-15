package service;

import exception.PayrollException;
import model.Employee;
import model.PayrollRecord;
import util.AuditLogger;
import java.util.*;

// =============================================================================
// SERVICE: LossOfPayTracker
// Owner: Nihal RG (PES1UG23AM187)
// From class diagram: +int lwpDays, +double penaltyAmount
//                     +calculateLopDeduction(base) : double
// =============================================================================
/**
 * Computes LOP (Loss of Pay) deductions and overtime pay from attendance data.
 *
 * GRASP — Information Expert:
 *   This class owns attendance-based calculations because it has all the
 *   attendance fields: leaveWithoutPay, hoursWorked, overtimeHours.
 */
class LossOfPayTracker {

    private static final double OVERTIME_MULTIPLIER = 1.5; // Time-and-a-half
    private static final double STD_HOURS_PER_DAY   = 8.0; // Assumed daily working hours

    /**
     * TODO ── calculateLopDeduction()
     * ─────────────────────────────────────────────────────────────────────────
     * Formula: (basicPay ÷ workingDaysInMonth) × leaveWithoutPay
     *
     * HINTS:
     *   1. Guard: if workingDaysInMonth <= 0, return 0.0 (avoids division by zero)
     *   2. Guard: if leaveWithoutPay <= 0, return 0.0 (short-circuit, no LOP)
     *   3. double dailyRate = emp.getBasicPay() / emp.getWorkingDaysInMonth();
     *   4. return dailyRate * emp.getLeaveWithoutPay();
     * ─────────────────────────────────────────────────────────────────────────
     */
    public double calculateLopDeduction(Employee emp) {
        // TODO: Guard checks, then compute dailyRate × lopDays
        return 0.0; // REMOVE once implemented
    }

    /**
     * TODO ── calculateOvertimePay()
     * ─────────────────────────────────────────────────────────────────────────
     * Formula: hourlyRate × OVERTIME_MULTIPLIER × overtimeHours
     * where:   hourlyRate = basicPay ÷ (workingDaysInMonth × STD_HOURS_PER_DAY)
     *
     * HINTS:
     *   1. Guard: if overtimeHours <= 0, return 0.0
     *   2. double hourlyRate = emp.getBasicPay() / (emp.getWorkingDaysInMonth() * STD_HOURS_PER_DAY);
     *   3. return hourlyRate * OVERTIME_MULTIPLIER * emp.getOvertimeHours();
     * ─────────────────────────────────────────────────────────────────────────
     */
    public double calculateOvertimePay(Employee emp) {
        // TODO: Guard, compute hourlyRate, return overtime pay
        return 0.0; // REMOVE once implemented
    }
}


// =============================================================================
// SERVICE: BonusDistributor
// Owner: Nehan Ahmad (PES1UG23AM184)
// From class diagram: +String bonusType, +double payoutAmount
//                     +calculateVariablePay() : double
// =============================================================================
/**
 * Computes the variable pay / bonus for each employee.
 * Exception: MISSING_PERFORMANCE_RATING (WARNING) → bonus = 0, queue arrears.
 */
class BonusDistributor {

    private final AuditLogger auditLogger;

    // Bonus rate per grade (% of basicPay)
    // e.g. L3 employee gets 12% of basicPay as bonus
    private static final Map<String, Double> BONUS_RATE = new HashMap<>();
    static {
        BONUS_RATE.put("L1", 0.05);
        BONUS_RATE.put("L2", 0.08);
        BONUS_RATE.put("L3", 0.12);
        BONUS_RATE.put("L4", 0.15);
        BONUS_RATE.put("L5", 0.20);
    }

    BonusDistributor(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    /**
     * TODO ── calculateBonus() / calculateVariablePay()
     * ─────────────────────────────────────────────────────────────────────────
     * HINT — logic flow:
     *   1. Look up the bonus rate: Double rate = BONUS_RATE.get(emp.getGradeLevel())
     *   2. If rate is null (grade not found):
     *        → MISSING_PERFORMANCE_RATING (WARNING) — do NOT throw upward
     *        → auditLogger.logWarning(emp.getEmpID(), ...)
     *        → record.setBonusArrears(emp.getBasicPay() * 0.05) // conservative estimate
     *        → return 0.0
     *   3. If rate found:
     *        → return emp.getBasicPay() * rate
     * ─────────────────────────────────────────────────────────────────────────
     */
    public double calculateBonus(Employee emp, PayrollRecord record) {
        // TODO: Look up grade, handle missing rating, return bonus amount
        return 0.0; // REMOVE once implemented
    }
}


// =============================================================================
// SERVICE: ReimbursementTracker
// Owner: Nehan Ahmad (PES1UG23AM184)
// From class diagram: +double pendingClaims, +double arrears
//                     +getApprovedReimbursements() : double
// =============================================================================
/**
 * Validates and computes the reimbursement payout for an employee.
 *
 * Exceptions:
 *   DUPLICATE_CLAIM_ID  (MINOR)   → reject, notify, return 0.0
 *   EXCEEDS_CLAIM_LIMIT (WARNING) → cap at grade limit, notify, return maxLimit
 */
class ReimbursementTracker {

    private final AuditLogger auditLogger;
    private final Set<String> processedClaimIDs = new HashSet<>(); // Tracks processed claims

    // Max reimbursement ceiling per grade
    private static final Map<String, Double> GRADE_LIMITS = new HashMap<>();
    static {
        GRADE_LIMITS.put("L1", 5_000.0);
        GRADE_LIMITS.put("L2", 10_000.0);
        GRADE_LIMITS.put("L3", 20_000.0);
        GRADE_LIMITS.put("L4", 35_000.0);
        GRADE_LIMITS.put("L5", 50_000.0);
    }

    ReimbursementTracker(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    /**
     * TODO ── getApprovedReimbursement()
     * ─────────────────────────────────────────────────────────────────────────
     * HINT — logic flow:
     *   1. Build claimID: String claimID = emp.getEmpID() + "-" + record.getPayPeriod();
     *
     *   2. Check for duplicate:
     *        if (processedClaimIDs.contains(claimID)):
     *          → DUPLICATE_CLAIM_ID (MINOR) — log warning, notify employee, return 0.0
     *
     *   3. Get grade limit:
     *        double maxLimit = GRADE_LIMITS.getOrDefault(emp.getGradeLevel(), 5000.0);
     *
     *   4. Check if claim exceeds limit:
     *        if (emp.getPendingClaims() > maxLimit):
     *          → EXCEEDS_CLAIM_LIMIT (WARNING) — log warning, notify, set approved = maxLimit
     *        else:
     *          → approved = Math.min(emp.getPendingClaims(), emp.getApprovedReimbursement())
     *
     *   5. Add claimID to processedClaimIDs (mark as done)
     *   6. Return approved
     * ─────────────────────────────────────────────────────────────────────────
     */
    public double getApprovedReimbursement(Employee emp, PayrollRecord record) {
        // TODO: Duplicate check → limit check → mark processed → return approved
        return 0.0; // REMOVE once implemented
    }
}


// =============================================================================
// SERVICE: SeverancePay
// Owner: Nihal RG (PES1UG23AM187)
// From class diagram: +int yearsOfService, +double noticePeroidPay, +double gratuityAmount
//                     +calculateFinalSettlement() : double
// =============================================================================
/**
 * Computes gratuity and final settlement for employees.
 * Gratuity eligibility: minimum 5 years of continuous service (India).
 */
class SeverancePay {

    private static final int    MIN_YEARS      = 5;          // Minimum service for gratuity
    private static final double DIVISOR        = 26.0;       // Working days assumed/month (legal)
    private static final double DAYS_PER_YEAR  = 15.0;       // Days per completed year (legal)
    private static final double MAX_GRATUITY   = 2_000_000.0; // ₹20 lakh cap

    /**
     * TODO ── calculateGratuity()
     * ─────────────────────────────────────────────────────────────────────────
     * Formula (Payment of Gratuity Act, 1972):
     *   totalGratuity = (basicPay / 26) × 15 × yearsOfService
     *   Cap at ₹20,00,000
     *   Monthly provision = totalGratuity / (yearsOfService × 12)
     *
     * HINTS:
     *   1. if (emp.getYearsOfService() < MIN_YEARS) return 0.0;  (not eligible)
     *   2. double total = (emp.getBasicPay() / DIVISOR) * DAYS_PER_YEAR * emp.getYearsOfService();
     *   3. total = Math.min(total, MAX_GRATUITY);
     *   4. return total / (emp.getYearsOfService() * 12);  // monthly provision
     * ─────────────────────────────────────────────────────────────────────────
     */
    public double calculateGratuity(Employee emp) {
        // TODO: Eligibility check → compute total gratuity → cap → return monthly provision
        return 0.0; // REMOVE once implemented
    }

    /**
     * TODO ── calculateFinalSettlement()
     * ─────────────────────────────────────────────────────────────────────────
     * Called only when an employee is exiting. Full lump sum payout.
     *
     * Formula:
     *   totalSettlement = noticePeriodPay + full gratuity + approvedReimbursement
     *
     * HINT: Re-use the gratuity formula (but return the FULL total, not monthly).
     *       Full gratuity = (basicPay / 26) × 15 × yearsOfService, capped at 20L.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public double calculateFinalSettlement(Employee emp, double noticePeriodPay) {
        // TODO: Compute full gratuity + noticePeriodPay + approvedReimbursement
        return 0.0; // REMOVE once implemented
    }
}


// =============================================================================
// SERVICE: StatuaryDeduction
// Owner: Nehan Ahmad (PES1UG23AM184)
// From class diagram: +double pfAmount, +double ptAmount, +double insurancePremium
//                     +calculatePF() : double, +calculatePT() : double
// =============================================================================
/**
 * Computes PF (Provident Fund) and PT (Professional Tax) deductions.
 * Exception: MISSING_WORK_STATE (MAJOR) — thrown from calculatePT if stateName is blank.
 */
class StatuaryDeduction {

    private static final double PF_RATE = 0.12; // Employee's PF contribution = 12% of basic

    // Professional Tax by Indian state (monthly, INR)
    private static final Map<String, Double> PT_BY_STATE = new HashMap<>();
    static {
        PT_BY_STATE.put("KARNATAKA",      200.0);
        PT_BY_STATE.put("MAHARASHTRA",    200.0);
        PT_BY_STATE.put("WEST_BENGAL",    150.0);
        PT_BY_STATE.put("TAMIL_NADU",     100.0);
        PT_BY_STATE.put("ANDHRA_PRADESH", 150.0);
        PT_BY_STATE.put("TELANGANA",      150.0);
        // States not in this map do not levy PT (e.g. Delhi) → return 0
    }

    /**
     * TODO ── calculatePF()
     * ─────────────────────────────────────────────────────────────────────────
     * Formula: emp.getBasicPay() × PF_RATE (12%)
     * PF is always on BASIC pay, not gross pay.
     *
     * HINT: This one is straightforward — one line.
     *       return emp.getBasicPay() * PF_RATE;
     * ─────────────────────────────────────────────────────────────────────────
     */
    public double calculatePF(Employee emp) {
        // TODO: return basicPay × 0.12
        return 0.0; // REMOVE once implemented
    }

    /**
     * TODO ── calculatePT()
     * ─────────────────────────────────────────────────────────────────────────
     * Looks up PT slab for the employee's work state.
     *
     * HINTS:
     *   1. if (emp.getStateName() == null || emp.getStateName().isBlank()):
     *        → throw new PayrollException.MissingWorkState(emp.getEmpID());
     *          (MAJOR — PayrollFacade lets this propagate to PayRunController)
     *
     *   2. Double pt = PT_BY_STATE.get(emp.getStateName().toUpperCase());
     *
     *   3. if (pt == null) return 0.0;  // State doesn't levy PT (e.g. Delhi)
     *
     *   4. return pt;
     * ─────────────────────────────────────────────────────────────────────────
     */
    public double calculatePT(Employee emp) throws PayrollException.MissingWorkState {
        // TODO: Null/blank guard → lookup state → return PT or 0.0
        return 0.0; // REMOVE once implemented
    }
}


// =============================================================================
// SERVICE: IncomeTaxTDS
// Owner: Nihal J (PES1UG23AM186)
// From class diagram: +double declaredInvestments, +double monthlyTdsAmount
//                     +calculateTDS(annualIncome) : double
// =============================================================================
/**
 * Computes monthly TDS using the Strategy pattern for multi-country support.
 * Delegates to TaxStrategy (selected by TaxStrategyFactory at runtime).
 *
 * Exception: MISSING_TAX_REGIME (WARNING) → default to OLD regime, email employee.
 */
class IncomeTaxTDS {

    private final AuditLogger auditLogger;

    IncomeTaxTDS(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    /**
     * TODO ── calculateTDS()
     * ─────────────────────────────────────────────────────────────────────────
     * HINTS — logic flow:
     *   1. Read tax regime: String regime = emp.getTaxRegime();
     *
     *   2. Check if regime is null or blank:
     *        → MISSING_TAX_REGIME (WARNING) — do NOT throw upward
     *        → auditLogger.logWarning(emp.getEmpID(), "...")
     *        → triggerTaxRegimeReminderEmail(emp)
     *        → regime = "OLD"   ← safe default per exception plan
     *
     *   3. Get strategy from factory:
     *        TaxStrategy strategy = TaxStrategyFactory.get(emp.getCountryCode(), regime);
     *
     *   4. Delegate to strategy:
     *        double tds = strategy.calculateMonthlyTax(emp, annualGrossIncome);
     *
     *   5. Clamp to 0 (TDS can never be negative):
     *        return Math.max(0.0, tds);
     * ─────────────────────────────────────────────────────────────────────────
     */
    public double calculateTDS(Employee emp, double annualGrossIncome) {
        // TODO Step 1: Read taxRegime from emp

        // TODO Step 2: Handle missing regime (WARNING — log and default to "OLD")

        // TODO Step 3: Get the right strategy via TaxStrategyFactory

        // TODO Step 4: Delegate calculation to strategy

        // TODO Step 5: Return Math.max(0.0, tds)
        return 0.0; // REMOVE once implemented
    }

    /**
     * Sends an automated reminder email to the employee to declare their regime.
     * In production: call an email service or push to a notification queue.
     * Kept separate so it can be replaced/mocked without changing calculateTDS().
     */
    private void triggerTaxRegimeReminderEmail(Employee emp) {
        // In production: emailService.send(emp.getEmail(), "DECLARE_TAX_REGIME");
        System.out.printf("[EMAIL] Reminder → Employee %s: Please declare your tax regime.%n",
                          emp.getEmpID());
    }
}


// =============================================================================
// SERVICE: DigitalPayslipGenerator
// Owner: Nehan Ahmad (PES1UG23AM184)
// From class diagram: +generatePDF(PayrollRecord) : File
//                     +distributedViaEmail(empId) : void
// =============================================================================
/**
 * Generates the PDF payslip and distributes it to the employee via email.
 * Exception: PAYSLIP_GENERATION_FAILED (WARNING) — queued for retry, batch continues.
 */
class DigitalPayslipGenerator {

    private final String outputDirectory; // Base path for payslip files

    DigitalPayslipGenerator(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    /**
     * TODO ── generatePDF()
     * ─────────────────────────────────────────────────────────────────────────
     * HINTS — logic flow:
     *   1. Build the file path:
     *        String fileName = "payslip_" + emp.getEmpID() + "_" + record.getPayPeriod() + ".pdf";
     *        String fullPath = outputDirectory + "/" + fileName;
     *
     *   2. Wrap in try-catch:
     *        try {
     *            simulatePdfWrite(fullPath, emp, record); // prints payslip summary
     *            distributedViaEmail(emp.getEmpID());     // prints "email sent" message
     *            return fullPath;
     *        } catch (Exception e) {
     *            throw new PayrollException.PayslipGenerationFailed(emp.getEmpID());
     *        }
     *
     * NOTE: The caller (PayrollFacade) catches PayslipGenerationFailed and
     *       logs a retry — it does NOT re-throw or halt the batch.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public String generatePDF(Employee emp, PayrollRecord record)
            throws PayrollException.PayslipGenerationFailed {
        // TODO: Build file path, simulate PDF write, call distributedViaEmail, return path
        return null; // REMOVE once implemented
    }

    /**
     * TODO ── distributedViaEmail()
     * ─────────────────────────────────────────────────────────────────────────
     * From class diagram: +distributedViaEmail(empId) : void
     *
     * In production: call an email service with the payslip PDF path as attachment.
     * For now: print a message like:
     *   [EMAIL] Payslip sent to EMP001 → /output/payslips/payslip_EMP001_2025-06.pdf
     *
     * HINT: Use System.out.printf(...) — this is the simulation of email dispatch.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public void distributedViaEmail(String empId) {
        // TODO: Print email dispatch confirmation message
    }

    /** Simulates writing the PDF to disk. Replace with real PDF library in production. */
    private void simulatePdfWrite(String path, Employee emp, PayrollRecord record) {
        System.out.printf(
            "[PDF] Writing payslip → %s | Gross: %.2f | Net: %.2f | TDS: %.2f | PF: %.2f%n",
            path, record.getFinalGrossPay(), record.getFinalNetPay(),
            record.getMonthlyTdsAmount(), record.getPfAmount()
        );
    }
}
