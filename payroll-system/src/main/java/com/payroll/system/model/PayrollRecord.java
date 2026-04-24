package com.payroll.system.model;

/** Holds the computed results for one employee's payroll run. */
public class PayrollRecord {

    private final String recordID;
    private final String empID;
    private final String batchID;
    private final String payPeriod;

    private double finalGrossPay;
    private double finalNetPay;
    private double penaltyAmount;
    private double overtimePay;
    private double payoutAmount;
    private double reimbursementPayout;
    private double gratuityAmount;
    private double pfAmount;
    private double ptAmount;
    private double monthlyTdsAmount;
    private double arrearsDeduction;
    private double bonusArrears;

    private boolean flaggedForHrReview;
    private String hrReviewReason;
    private String payslipPdfPath;

    public PayrollRecord(String recordID, String empID, String batchID, String payPeriod) {
        this.recordID = recordID;
        this.empID = empID;
        this.batchID = batchID;
        this.payPeriod = payPeriod;
    }

    public String getRecordID() { return recordID; }
    public String getEmpID() { return empID; }
    public String getBatchID() { return batchID; }
    public String getPayPeriod() { return payPeriod; }
    public double getFinalGrossPay() { return finalGrossPay; }
    public double getFinalNetPay() { return finalNetPay; }
    public double getPenaltyAmount() { return penaltyAmount; }
    public double getOvertimePay() { return overtimePay; }
    public double getPayoutAmount() { return payoutAmount; }
    public double getReimbursementPayout() { return reimbursementPayout; }
    public double getGratuityAmount() { return gratuityAmount; }
    public double getPfAmount() { return pfAmount; }
    public double getPtAmount() { return ptAmount; }
    public double getMonthlyTdsAmount() { return monthlyTdsAmount; }
    public double getArrearsDeduction() { return arrearsDeduction; }
    public double getBonusArrears() { return bonusArrears; }
    public boolean isFlaggedForHrReview() { return flaggedForHrReview; }
    public String getHrReviewReason() { return hrReviewReason; }
    public String getPayslipPdfPath() { return payslipPdfPath; }

    public void setFinalGrossPay(double v) { this.finalGrossPay = v; }
    public void setFinalNetPay(double v) { this.finalNetPay = v; }
    public void setPenaltyAmount(double v) { this.penaltyAmount = v; }
    public void setOvertimePay(double v) { this.overtimePay = v; }
    public void setPayoutAmount(double v) { this.payoutAmount = v; }
    public void setReimbursementPayout(double v) { this.reimbursementPayout = v; }
    public void setGratuityAmount(double v) { this.gratuityAmount = v; }
    public void setPfAmount(double v) { this.pfAmount = v; }
    public void setPtAmount(double v) { this.ptAmount = v; }
    public void setMonthlyTdsAmount(double v) { this.monthlyTdsAmount = v; }
    public void setArrearsDeduction(double v) { this.arrearsDeduction = v; }
    public void setBonusArrears(double v) { this.bonusArrears = v; }
    public void setPayslipPdfPath(String v) { this.payslipPdfPath = v; }

    /** Marks the record for manual follow-up without failing the whole batch. */
    public void flagForHrReview(String reason) {
        this.flaggedForHrReview = true;
        this.hrReviewReason = reason;
    }

    /** Combines the component amounts into gross and net pay. */
    public void calculateTotals(Employee emp) {
        finalGrossPay = emp.getBasicPay() - penaltyAmount + overtimePay;
        double totalDeductions = pfAmount + ptAmount + monthlyTdsAmount + emp.getInsurancePremium();
        double netPay = finalGrossPay + payoutAmount + reimbursementPayout - totalDeductions;

        if (netPay < 0) {
            this.arrearsDeduction = Math.abs(netPay);
            this.finalNetPay = 0.0;
            flagForHrReview("NEGATIVE_NET_PAY: arrears = " + this.arrearsDeduction);
        } else {
            this.finalNetPay = netPay;
        }
    }

    @Override
    public String toString() {
        return String.format(
                "PayrollRecord[%s | Emp=%s | Gross=%.2f | Net=%.2f | TDS=%.2f | PF=%.2f | PT=%.2f]",
                recordID, empID, finalGrossPay, finalNetPay, monthlyTdsAmount, pfAmount, ptAmount
        );
    }
}
