import exception.PayrollException;
import model.Employee;
import model.PayrollRecord;
import pattern.PayrollFacade;
import service.*;
import util.AuditLogger;

import java.util.Arrays;
import java.util.List;

/**
 * =============================================================================
 * CLASS: PayrollMain  (Entry point / Demo driver)
 * =============================================================================
 * This file wires ALL components together and runs a sample batch.
 * It demonstrates all three design patterns and all major exceptions.
 *
 * RUN ORDER:
 *   1. Construct services
 *   2. Compose PayrollFacade with those services (Facade pattern)
 *   3. Build Employee objects using Builder pattern
 *   4. Run batch via PayRunController
 *   5. Observe console output — each step is logged
 *
 * DESIGN PATTERNS DEMONSTRATED:
 *   ┌──────────────┬─────────────────────────────────────────────────┐
 *   │ Creational   │ Builder        → Employee.Builder               │
 *   │              │ Factory        → TaxStrategyFactory             │
 *   │ Structural   │ Facade         → PayrollFacade                  │
 *   │ Behavioural  │ Strategy       → TaxStrategy + implementations  │
 *   └──────────────┴─────────────────────────────────────────────────┘
 *
 * EXCEPTIONS TRIGGERED IN THIS DEMO:
 *   emp2 → MISSING_TAX_REGIME    (WARNING — defaults to OLD, continues)
 *   emp2 → EXCEEDS_CLAIM_LIMIT   (WARNING — capped at grade limit)
 *   emp3 → MISSING_WORK_STATE    (MAJOR   — emp3 is skipped, batch continues)
 *   emp4 → Singapore strategy    (multi-country demo)
 *   2nd run → INVALID_PAY_PERIOD (MAJOR   — entire batch rejected)
 * =============================================================================
 */
public class PayrollMain {

    public static void main(String[] args) {

        // ── 1. Shared utilities ────────────────────────────────────────────────
        AuditLogger auditLogger = new AuditLogger();

        // ── 2. Construct all services ──────────────────────────────────────────
        // Each service is constructed with its dependencies passed in (DI)
        LossOfPayTracker      lopTracker           = new LossOfPayTracker();
        BonusDistributor      bonusDistributor      = new BonusDistributor(auditLogger);
        ReimbursementTracker  reimbursementTracker  = new ReimbursementTracker(auditLogger);
        SeverancePay          severancePay          = new SeverancePay();
        StatuaryDeduction     statuaryDeduction     = new StatuaryDeduction();
        IncomeTaxTDS          incomeTaxTDS          = new IncomeTaxTDS(auditLogger);
        DigitalPayslipGenerator payslipGenerator    = new DigitalPayslipGenerator("/output/payslips");

        // ── 3. Compose the Facade (Structural Pattern) ────────────────────────
        // PayRunController only talks to this — never to individual services.
        PayrollFacade facade = new PayrollFacade(
            lopTracker, bonusDistributor, reimbursementTracker,
            severancePay, statuaryDeduction, incomeTaxTDS,
            payslipGenerator, auditLogger
        );

        // ── 4. Build employees using Builder pattern ───────────────────────────

        // emp1: Happy path — India, New Regime, Karnataka, 6 years service
        Employee emp1 = new Employee.Builder("EMP001", "Arjun Sharma")
            .department("Engineering").gradeLevel("L3")
            .basicPay(80_000.0).yearsOfService(6)
            .workingDaysInMonth(22).leaveWithoutPay(0).overtimeHours(4.0)
            .pendingClaims(8_000.0).approvedReimbursement(8_000.0)
            .insurancePremium(1_200.0).declaredInvestments(150_000.0)
            .taxRegime("NEW").countryCode("IN").currencyCode("INR")
            .stateName("KARNATAKA")
            .build();

        // emp2: Missing tax regime (WARNING) + claim exceeds limit (WARNING)
        Employee emp2 = new Employee.Builder("EMP002", "Priya Nair")
            .department("Finance").gradeLevel("L2")
            .basicPay(55_000.0).yearsOfService(3)
            .workingDaysInMonth(22).leaveWithoutPay(2)
            .pendingClaims(15_000.0)      // L2 limit is 10,000 → EXCEEDS_CLAIM_LIMIT
            .approvedReimbursement(10_000.0)
            .insurancePremium(800.0).declaredInvestments(0.0)
            .taxRegime("")                // blank → MISSING_TAX_REGIME
            .countryCode("IN").currencyCode("INR")
            .stateName("MAHARASHTRA")
            .build();

        // emp3: Missing work state → MISSING_WORK_STATE (MAJOR — will be skipped)
        Employee emp3 = new Employee.Builder("EMP003", "Ravi Kumar")
            .department("HR").gradeLevel("L1")
            .basicPay(35_000.0).yearsOfService(1)
            .workingDaysInMonth(22).leaveWithoutPay(1)
            .pendingClaims(3_000.0).approvedReimbursement(3_000.0)
            .insurancePremium(500.0).taxRegime("OLD")
            .countryCode("IN").currencyCode("INR")
            .stateName("")               // blank → MISSING_WORK_STATE (MAJOR)
            .build();

        // emp4: Singapore employee — uses Singapore TaxStrategy
        Employee emp4 = new Employee.Builder("EMP004", "Li Wei")
            .department("Operations").gradeLevel("L4")
            .basicPay(8_000.0).yearsOfService(7)  // SGD
            .workingDaysInMonth(22).leaveWithoutPay(0).overtimeHours(2.0)
            .pendingClaims(1_500.0).approvedReimbursement(1_500.0)
            .insurancePremium(150.0).declaredInvestments(0.0)
            .taxRegime("SG_STANDARD")
            .countryCode("SG").currencyCode("SGD")
            .stateName("SINGAPORE")
            .build();

        List<Employee> employees = Arrays.asList(emp1, emp2, emp3, emp4);

        // ── 5. Run the batch ───────────────────────────────────────────────────
        PayRunController controller = new PayRunController(
            "BATCH-2025-06", "2025-06", facade, auditLogger
        );

        try {
            List<PayrollRecord> records = controller.executeBatchPayroll(employees);

            System.out.println("\n── Final Payroll Records ──");
            records.forEach(r -> System.out.println("  " + r));

            controller.verifyCalculations();

            // ── 6. Demonstrate INVALID_PAY_PERIOD ─────────────────────────────
            // Running for the same period again should be rejected immediately.
            System.out.println("\n── Attempting duplicate run for same period ──");
            controller.executeBatchPayroll(employees); // Should throw InvalidPayPeriod

        } catch (PayrollException.InvalidPayPeriod e) {
            System.err.println("\n[BATCH HALTED] " + e.getExceptionName() + ": " + e.getMessage());
            System.err.println("[ACTION] Lock the 'Run Payroll' button for period 2025-06.");
        }

        // ── 7. Print full audit trail ──────────────────────────────────────────
        System.out.println("\n── Full Audit Trail ──");
        auditLogger.getAllEntries().forEach(System.out::println);
    }
}
