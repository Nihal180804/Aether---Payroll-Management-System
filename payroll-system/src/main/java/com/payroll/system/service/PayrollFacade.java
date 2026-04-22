package com.payroll.system.service;

import com.payroll.system.exception.PayrollException;
import com.payroll.system.model.Employee;
import com.payroll.system.model.PayrollRecord;
import com.payroll.system.service.*;
import com.payroll.system.util.AuditLogger;

public class PayrollFacade {

    private final LossOfPayTracker lopTracker;
    private final BonusDistributor bonusDistributor;
    private final ReimbursementTracker reimbursementTracker;
    private final SeverancePay severancePay;
    private final StatuaryDeduction statuaryDeduction;
    private final IncomeTaxTDS incomeTaxTDS;
    private final DigitalPayslipGenerator payslipGenerator;
    private final AuditLogger auditLogger;

    public PayrollFacade(
            LossOfPayTracker lopTracker,
            BonusDistributor bonusDistributor,
            ReimbursementTracker reimbursementTracker,
            SeverancePay severancePay,
            StatuaryDeduction statuaryDeduction,
            IncomeTaxTDS incomeTaxTDS,
            DigitalPayslipGenerator payslipGenerator,
            AuditLogger auditLogger) {
        this.lopTracker = lopTracker;
        this.bonusDistributor = bonusDistributor;
        this.reimbursementTracker = reimbursementTracker;
        this.severancePay = severancePay;
        this.statuaryDeduction = statuaryDeduction;
        this.incomeTaxTDS = incomeTaxTDS;
        this.payslipGenerator = payslipGenerator;
        this.auditLogger = auditLogger;
    }

    public void processEmployee(Employee emp, PayrollRecord record) {

        // LOP Penalty
        double penalty = lopTracker.calculateLopDeduction(emp);
        record.setPenaltyAmount(penalty);

        // Overtime Pay
        double ot = lopTracker.calculateOvertimePay(emp);
        record.setOvertimePay(ot);

        // Statutory Deductions
        double pf = statuaryDeduction.calculatePF(emp);
        double pt = statuaryDeduction.calculatePT(emp);
        record.setPfAmount(pf);
        record.setPtAmount(pt);

        // TDS as in incomeTaxTDS
        double tds = incomeTaxTDS.calculateTDS(emp, emp.getBasicPay() * 12);
        record.setMonthlyTdsAmount(tds);

        // Bonus
        double bonus = bonusDistributor.calculateBonus(emp, record);
        record.setPayoutAmount(bonus);

        // Reimbursements
        double reimb = reimbursementTracker.getApprovedReimbursement(emp, record);
        record.setReimbursementPayout(reimb);

        // Gratuity
        double gratuity = severancePay.calculateGratuity(emp);
        record.setGratuityAmount(gratuity);

        // For calculating total i.e. finalGrossPay and finalNetPay
        record.calculateTotals(emp);

        // Payslip Generation
        try {
            String path = payslipGenerator.generatePDF(emp, record);
            record.setPayslipPdfPath(path);
        } catch (PayrollException.PayslipGenerationFailed e) {
            auditLogger.logWarning(emp.getEmpID(), "Payslip queued for retry");// logs warninig but dosen't halt the
        }
    }
}


