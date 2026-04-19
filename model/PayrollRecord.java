package model;

/**
 * =============================================================================
 * CLASS: PayrollRecord
 * =============================================================================
 * From the class diagram:
 *   Attributes : recordOd (String), finalGrossPay (double), finalNetPay (double)
 *   Method     : calculateTotals() : void
 *   Relationships:
 *     - PayRunController "generates" PayrollRecord
 *     - DigitalPayslipGenerator "reads data from" PayrollRecord
 *     - PayrollRecord "includes" → LossOfPayTracker, BonusDistributor,
 *                                  ReimbursementTracker, SeverancePay,
 *                                  SalaryGradeStructure, StatuaryDeduction,
 *                                  IncomeTaxTDS
 *
 * This is the central OUTPUT object of one employee's pay run.
 * All services WRITE into this object; the payslip generator READS from it.
 *
 * GRASP — Low Coupling:
 *   PayrollRecord is a pure data container. It holds results but does NOT
 *   call any service — services call it.
 * =============================================================================
 */
public class PayrollRecord {

    // ── Identity (from class diagram) ─────────────────────────────────────────
    private final String recordID;   // Unique ID: batchID + empID
    private final String empID;
    private final String batchID;
    private final String payPeriod;

    // ── Core outputs (from class diagram) ─────────────────────────────────────
    private double finalGrossPay;  // Basic + overtime - LOP penalty
    private double finalNetPay;    // Gross - all deductions (must be ≥ 0)

    // ── Component outputs (written by individual services) ────────────────────
    private double penaltyAmount;       // From LossOfPayTracker
    private double overtimePay;         // From LossOfPayTracker
    private double payoutAmount;        // From BonusDistributor
    private double reimbursementPayout; // From ReimbursementTracker
    private double gratuityAmount;      // From SeverancePay
    private double pfAmount;            // From StatuaryDeduction
    private double ptAmount;            // From StatuaryDeduction
    private double monthlyTdsAmount;    // From IncomeTaxTDS

    // ── Arrears (carried to next cycle) ───────────────────────────────────────
    private double arrearsDeduction; // Set when NEGATIVE_NET_PAY is triggered
    private double bonusArrears;     // Set when MISSING_PERFORMANCE_RATING is triggered

    // ── HR Review flag ────────────────────────────────────────────────────────
    private boolean flaggedForHrReview;
    private String  hrReviewReason;

    // ── File paths (written after generation) ─────────────────────────────────
    private String payslipPdfPath;

    public PayrollRecord(String recordID, String empID, String batchID, String payPeriod) {
        this.recordID  = recordID;
        this.empID     = empID;
        this.batchID   = batchID;
        this.payPeriod = payPeriod;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String  getRecordID()            { return recordID; }
    public String  getEmpID()               { return empID; }
    public String  getBatchID()             { return batchID; }
    public String  getPayPeriod()           { return payPeriod; }
    public double  getFinalGrossPay()       { return finalGrossPay; }
    public double  getFinalNetPay()         { return finalNetPay; }
    public double  getPenaltyAmount()       { return penaltyAmount; }
    public double  getOvertimePay()         { return overtimePay; }
    public double  getPayoutAmount()        { return payoutAmount; }
    public double  getReimbursementPayout() { return reimbursementPayout; }
    public double  getGratuityAmount()      { return gratuityAmount; }
    public double  getPfAmount()            { return pfAmount; }
    public double  getPtAmount()            { return ptAmount; }
    public double  getMonthlyTdsAmount()    { return monthlyTdsAmount; }
    public double  getArrearsDeduction()    { return arrearsDeduction; }
    public double  getBonusArrears()        { return bonusArrears; }
    public boolean isFlaggedForHrReview()  { return flaggedForHrReview; }
    public String  getHrReviewReason()      { return hrReviewReason; }
    public String  getPayslipPdfPath()      { return payslipPdfPath; }

    // ── Setters (only for computed/output fields) ─────────────────────────────
    public void setFinalGrossPay(double v)       { this.finalGrossPay = v; }
    public void setFinalNetPay(double v)         { this.finalNetPay = v; }
    public void setPenaltyAmount(double v)       { this.penaltyAmount = v; }
    public void setOvertimePay(double v)         { this.overtimePay = v; }
    public void setPayoutAmount(double v)        { this.payoutAmount = v; }
    public void setReimbursementPayout(double v) { this.reimbursementPayout = v; }
    public void setGratuityAmount(double v)      { this.gratuityAmount = v; }
    public void setPfAmount(double v)            { this.pfAmount = v; }
    public void setPtAmount(double v)            { this.ptAmount = v; }
    public void setMonthlyTdsAmount(double v)    { this.monthlyTdsAmount = v; }
    public void setArrearsDeduction(double v)    { this.arrearsDeduction = v; }
    public void setBonusArrears(double v)        { this.bonusArrears = v; }
    public void setPayslipPdfPath(String v)      { this.payslipPdfPath = v; }

    /** Call this when MISSING_WORK_STATE or NEGATIVE_NET_PAY is triggered. */
    public void flagForHrReview(String reason) {
        this.flaggedForHrReview = true;
        this.hrReviewReason     = reason;
    }

    /**
     * TODO ── calculateTotals()
     * ─────────────────────────────────────────────────────────────────────────
     * From the class diagram: +calculateTotals() : void
     *
     * This method computes finalGrossPay and finalNetPay using the component
     * values that have already been set by each service.
     *
     * STEP 1 — Gross Pay:
     *   finalGrossPay = basicPay - penaltyAmount + overtimePay
     *   (basicPay comes from the Employee object passed in as a parameter)
     *
     * STEP 2 — Total Deductions:
     *   totalDeductions = pfAmount + ptAmount + monthlyTdsAmount + insurancePremium
     *
     * STEP 3 — Net Pay:
     *   netPay = finalGrossPay + payoutAmount + reimbursementPayout - totalDeductions
     *
     * STEP 4 — NEGATIVE_NET_PAY guard:
     *   If netPay < 0:
     *     → set arrearsDeduction = Math.abs(netPay)
     *     → set finalNetPay = 0.0
     *     → call flagForHrReview("NEGATIVE_NET_PAY: arrears = " + arrearsDeduction)
     *   Else:
     *     → set finalNetPay = netPay
     *
     * HINT: All the values you need (pfAmount, ptAmount, etc.) are already
     *       stored as fields in this class — just use their getters or fields directly.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public void calculateTotals(Employee emp) {
        // TODO Step 1: Calculate finalGrossPay
        // finalGrossPay = emp.getBasicPay() - penaltyAmount + overtimePay;

        // TODO Step 2: Sum up all deductions
        // double totalDeductions = pfAmount + ptAmount + monthlyTdsAmount + emp.getInsurancePremium();

        // TODO Step 3: Calculate netPay
        // double netPay = finalGrossPay + payoutAmount + reimbursementPayout - totalDeductions;

        // TODO Step 4: Guard against negative net pay
        // if (netPay < 0) { ... } else { finalNetPay = netPay; }
    }

    @Override
    public String toString() {
        return String.format(
            "PayrollRecord[%s | Emp=%s | Gross=%.2f | Net=%.2f | TDS=%.2f | PF=%.2f | PT=%.2f]",
            recordID, empID, finalGrossPay, finalNetPay, monthlyTdsAmount, pfAmount, ptAmount
        );
    }
}
