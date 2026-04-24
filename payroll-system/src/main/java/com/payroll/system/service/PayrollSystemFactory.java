package com.payroll.system.service;

import com.hrms.db.repositories.succession.DevelopmentPlanRepositoryImpl;
import com.hrms.db.repositories.succession.PlanTaskRepositoryImpl;
import com.hrms.db.repositories.succession.ReadinessScoreRepositoryImpl;
import com.hrms.db.repositories.succession.RiskLogRepositoryImpl;
import com.hrms.db.repositories.succession.RoleRepositoryImpl;
import com.hrms.db.repositories.succession.SuccessorAssignmentRepositoryImpl;
import com.hrms.succession.facade.SuccessionBonusFacade;
import com.hrms.succession.service.DevelopmentPlanService;
import com.hrms.succession.service.ReadinessCalculatorService;
import com.hrms.succession.service.RiskAnalyzerService;
import com.hrms.succession.service.RoleService;
import com.hrms.succession.service.SuccessorMatcherService;
import com.pesu.expensesubsystem.integration.ExpenseDataProviderImpl;
import com.payroll.system.util.AuditLogger;

import java.lang.reflect.Field;

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
        BonusDistributor      bonusDistributor    = new BonusDistributor(auditLogger, createSuccessionBonusFacade());
        ReimbursementTracker  reimbursementTracker = new ReimbursementTracker(auditLogger,
                                                        new ExpenseDataProviderImpl());
        SeverancePay          severancePay        = new SeverancePay();
        StatuaryDeduction     statuaryDeduction   = new StatuaryDeduction();
        IncomeTaxTDS          incomeTaxTDS        = new IncomeTaxTDS(auditLogger);
        DigitalPayslipGenerator payslipGenerator  = new DigitalPayslipGenerator("output/payslips");

        return new PayrollFacade(
                lopTracker, bonusDistributor, reimbursementTracker,
                severancePay, statuaryDeduction, incomeTaxTDS,
                payslipGenerator, auditLogger);
    }

    private static SuccessionBonusFacade createSuccessionBonusFacade() {
        try {
            RoleRepositoryImpl roleRepo = new RoleRepositoryImpl();
            ReadinessScoreRepositoryImpl readinessRepo = new ReadinessScoreRepositoryImpl();
            SuccessorAssignmentRepositoryImpl assignmentRepo = new SuccessorAssignmentRepositoryImpl();
            RiskLogRepositoryImpl riskRepo = new RiskLogRepositoryImpl();
            DevelopmentPlanRepositoryImpl planRepo = new DevelopmentPlanRepositoryImpl();
            PlanTaskRepositoryImpl taskRepo = new PlanTaskRepositoryImpl();

            ReadinessCalculatorService readinessService = new ReadinessCalculatorService();
            setField(readinessService, "readinessRepo", readinessRepo);

            RoleService roleService = new RoleService();
            setField(roleService, "roleRepo", roleRepo);

            SuccessorMatcherService matcherService = new SuccessorMatcherService();
            setField(matcherService, "assignRepo", assignmentRepo);

            RiskAnalyzerService riskService = new RiskAnalyzerService();
            setField(riskService, "riskRepo", riskRepo);

            DevelopmentPlanService planService = new DevelopmentPlanService();
            setField(planService, "planRepo", planRepo);
            setField(planService, "taskRepo", taskRepo);

            SuccessionBonusFacade facade = new SuccessionBonusFacade();
            setField(facade, "readinessService", readinessService);
            setField(facade, "roleService", roleService);
            setField(facade, "matcherService", matcherService);
            setField(facade, "riskService", riskService);
            setField(facade, "planService", planService);
            return facade;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to wire SuccessionBonusFacade", e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}



