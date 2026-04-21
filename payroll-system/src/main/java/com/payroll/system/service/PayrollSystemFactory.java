package com.payroll.system.service;


import com.payroll.system.util.AuditLogger;

/**
 * Factory that assembles and returns a fully wired PayrollFacade.
 *
 * This is the ONLY class outside the service package needs to call —
 * it gives access to all package-private service wiring without exposing
 * internal construction details.
 */
public class PayrollSystemFactory {

    private PayrollSystemFactory() {}

    /**
     * Creates a PayrollFacade with all services wired up and ready to use.
     * To swap the expense provider (e.g. to real DB team's class),
     * modify only this factory method.
     */
    public static PayrollFacade createFacade(AuditLogger auditLogger) {
        LossOfPayTracker      lopTracker          = new LossOfPayTracker();
        BonusDistributor      bonusDistributor    = new BonusDistributor(auditLogger);
        ReimbursementTracker  reimbursementTracker = new ReimbursementTracker(auditLogger,
                                                        new MockExpenseProvider());
        SeverancePay          severancePay        = new SeverancePay();
        StatuaryDeduction     statuaryDeduction   = new StatuaryDeduction();
        IncomeTaxTDS          incomeTaxTDS        = new IncomeTaxTDS(auditLogger);
        DigitalPayslipGenerator payslipGenerator  = new DigitalPayslipGenerator("/output/payslips");

        return new PayrollFacade(
                lopTracker, bonusDistributor, reimbursementTracker,
                severancePay, statuaryDeduction, incomeTaxTDS,
                payslipGenerator, auditLogger);
    }
}



