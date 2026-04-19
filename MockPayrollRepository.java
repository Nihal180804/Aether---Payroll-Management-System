import java.util.*;

/**
 * =============================================================================
 * CLASS: MockPayrollRepository
 * =============================================================================
 * A fully in-memory implementation of IPayrollRepository.
 * Used by Team OCBC while the DB team's real implementation is not yet ready.
 *
 * WHEN THE DB TEAM IS READY:
 *   Simply swap this out. In PayrollMain, change:
 *     IPayrollRepository repo = new MockPayrollRepository();
 *   to:
 *     IPayrollRepository repo = new RealPayrollRepository(dbConnection);
 *   Nothing else changes — this is the Dependency Inversion Principle working.
 *
 * GRASP — Polymorphism:
 *   Both MockPayrollRepository and the real DB class implement IPayrollRepository.
 *   PayRunController never knows which one it's talking to.
 *
 * EMPLOYEES IN THIS MOCK (10 total):
 *   EMP001 — Happy path, India, New Regime, Karnataka, senior engineer
 *   EMP002 — Missing tax regime → triggers MISSING_TAX_REGIME (WARNING)
 *   EMP003 — Missing work state → triggers MISSING_WORK_STATE (MAJOR, skipped)
 *   EMP004 — Claim exceeds grade limit → triggers EXCEEDS_CLAIM_LIMIT (WARNING)
 *   EMP005 — Singapore employee, multi-country demo
 *   EMP006 — US employee, married filing status
 *   EMP007 — High deductions → triggers NEGATIVE_NET_PAY (MAJOR)
 *   EMP008 — Missing performance rating → triggers MISSING_PERFORMANCE_RATING (WARNING)
 *   EMP009 — Long service employee, gratuity eligible (7 years)
 *   EMP010 — Happy path, India, Old Regime, junior employee
 *
 * Team OCBC — Payroll Management Subsystem
 * =============================================================================
 */
public class MockPayrollRepository implements IPayrollRepository {

    // ── In-memory stores ───────────────────────────────────────────────────────
    private final Map<String, PayrollDataPackage> employeeStore  = new HashMap<>();
    private final List<String>                    activeEmpIDs   = new ArrayList<>();
    private final List<String>                    savedResults   = new ArrayList<>(); // Simulates DB writes
    private final List<String>                    errorLog       = new ArrayList<>(); // Simulates error table

    /**
     * Constructor — populates the mock data store with 10 employees.
     */
    public MockPayrollRepository() {
        seed();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  IPayrollRepository — Interface Implementation
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Fetches all payroll data for a given employee and pay period.
     * In the real implementation, this would run a SELECT across multiple tables.
     * Here we just return the pre-seeded in-memory package.
     */
    @Override
    public PayrollDataPackage fetchEmployeeData(String empID, String payPeriod) {
        PayrollDataPackage pkg = employeeStore.get(empID);
        if (pkg == null) {
            System.err.printf("[MOCK DB] WARNING: No data found for empID=%s%n", empID);
            return null;
        }
        // Update the payPeriod field to match the requested period
        pkg.payPeriod = payPeriod;
        System.out.printf("[MOCK DB] Fetched data for %s (%s) — period: %s%n",
                          empID, pkg.employee.name, payPeriod);
        return pkg;
    }

    /**
     * Returns the list of all active employee IDs.
     * In the real DB, this would be: SELECT empID FROM employees WHERE status = 'ACTIVE'
     */
    @Override
    public List<String> getAllActiveEmployeeIDs() {
        System.out.printf("[MOCK DB] Returning %d active employee IDs.%n", activeEmpIDs.size());
        return Collections.unmodifiableList(activeEmpIDs);
    }

    /**
     * Simulates persisting a PayrollResultDTO back to the database.
     * In the real DB: INSERT INTO payroll_results (...) VALUES (...)
     * Here we just print and store a confirmation string.
     */
    @Override
    public boolean savePayrollResult(String batchID, PayrollResultDTO result) {
        String entry = String.format(
            "SAVED | batch=%s | emp=%s | record=%s | gross=%.2f | net=%.2f | tax=%.2f | pf=%.2f",
            batchID, result.empID, result.recordID,
            result.finalGrossPay, result.finalNetPay,
            result.taxDeducted, result.pfAmount
        );
        savedResults.add(entry);
        System.out.println("[MOCK DB] " + entry);
        return true; // Always succeeds in mock
    }

    /**
     * Simulates writing an error entry to the processing_errors table.
     * In the real DB: INSERT INTO processing_errors (batchID, empID, errorMsg, timestamp)
     */
    @Override
    public void logProcessingError(String batchID, String empID, String errorMsg) {
        String entry = String.format(
            "ERROR | batch=%s | emp=%s | msg=%s", batchID, empID, errorMsg
        );
        errorLog.add(entry);
        System.err.println("[MOCK DB ERROR LOG] " + entry);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Mock-only helpers (not part of the interface)
    // ═════════════════════════════════════════════════════════════════════════

    /** Returns all saved payroll results — useful for verifying output in tests. */
    public List<String> getSavedResults() {
        return Collections.unmodifiableList(savedResults);
    }

    /** Returns all logged errors — useful for post-batch audit. */
    public List<String> getErrorLog() {
        return Collections.unmodifiableList(errorLog);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  Data Seeding
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Populates the in-memory store with 10 realistic dummy employees.
     * Each employee is designed to exercise a specific code path or exception.
     */
    private void seed() {
        add(emp001_happyPath());
        add(emp002_missingTaxRegime());
        add(emp003_missingWorkState());
        add(emp004_exceedsClaimLimit());
        add(emp005_singapore());
        add(emp006_usMarried());
        add(emp007_negativeNetPay());
        add(emp008_missingPerformanceRating());
        add(emp009_gratuityEligible());
        add(emp010_juniorOldRegime());
    }

    /** Registers a package in the store and adds empID to the active list. */
    private void add(PayrollDataPackage pkg) {
        employeeStore.put(pkg.employee.empID, pkg);
        activeEmpIDs.add(pkg.employee.empID);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Employee seed methods — one per employee
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * EMP001 — Happy path
     * Senior engineer, India, New Tax Regime, Karnataka.
     * All fields populated, no exceptions expected.
     */
    private PayrollDataPackage emp001_happyPath() {
        PayrollDataPackage p = new PayrollDataPackage();

        p.employee             = new EmployeeDTO();
        p.employee.empID       = "EMP001";
        p.employee.name        = "Arjun Sharma";
        p.employee.department  = "Engineering";
        p.employee.gradeLevel  = "L3";
        p.employee.basicPay    = 80_000.0;
        p.employee.yearsOfService = 6;

        p.attendance                   = new AttendanceDTO();
        p.attendance.workingDaysInMonth = 22;
        p.attendance.leaveWithPay       = 1;
        p.attendance.leaveWithoutPay    = 0;
        p.attendance.hoursWorked        = 176.0;
        p.attendance.overtimeHours      = 4.0;

        p.financials                        = new FinancialsDTO();
        p.financials.pendingClaims          = 8_000.0;
        p.financials.approvedReimbursement  = 8_000.0;
        p.financials.insurancePremium       = 1_200.0;
        p.financials.declaredInvestments    = 150_000.0; // 80C investments

        p.tax                    = new TaxContextDTO();
        p.tax.countryCode        = "IN";
        p.tax.currencyCode       = "INR";
        p.tax.taxRegime          = "NEW";
        p.tax.stateName          = "KARNATAKA";
        p.tax.filingStatus       = "SINGLE";
        p.tax.taxCode            = "";
        p.tax.nationalIDNumber   = "ABCDE1234F"; // PAN

        p.payPeriod = "2025-06";
        return p;
    }

    /**
     * EMP002 — MISSING_TAX_REGIME (WARNING)
     * Finance analyst, India. taxRegime left blank.
     * Expected behaviour: defaults to OLD regime, email reminder sent, payroll continues.
     */
    private PayrollDataPackage emp002_missingTaxRegime() {
        PayrollDataPackage p = new PayrollDataPackage();

        p.employee             = new EmployeeDTO();
        p.employee.empID       = "EMP002";
        p.employee.name        = "Priya Nair";
        p.employee.department  = "Finance";
        p.employee.gradeLevel  = "L2";
        p.employee.basicPay    = 55_000.0;
        p.employee.yearsOfService = 3;

        p.attendance                   = new AttendanceDTO();
        p.attendance.workingDaysInMonth = 22;
        p.attendance.leaveWithPay       = 0;
        p.attendance.leaveWithoutPay    = 2; // 2 LOP days
        p.attendance.hoursWorked        = 160.0;
        p.attendance.overtimeHours      = 0.0;

        p.financials                       = new FinancialsDTO();
        p.financials.pendingClaims         = 6_000.0;
        p.financials.approvedReimbursement = 6_000.0;
        p.financials.insurancePremium      = 800.0;
        p.financials.declaredInvestments   = 50_000.0;

        p.tax                  = new TaxContextDTO();
        p.tax.countryCode      = "IN";
        p.tax.currencyCode     = "INR";
        p.tax.taxRegime        = "";           // ← BLANK — triggers MISSING_TAX_REGIME
        p.tax.stateName        = "MAHARASHTRA";
        p.tax.filingStatus     = "SINGLE";
        p.tax.nationalIDNumber = "PQRST5678G";

        p.payPeriod = "2025-06";
        return p;
    }

    /**
     * EMP003 — MISSING_WORK_STATE (MAJOR)
     * HR executive, India. stateName left blank.
     * Expected behaviour: payroll SKIPPED for this employee, flagged for HR review.
     */
    private PayrollDataPackage emp003_missingWorkState() {
        PayrollDataPackage p = new PayrollDataPackage();

        p.employee             = new EmployeeDTO();
        p.employee.empID       = "EMP003";
        p.employee.name        = "Ravi Kumar";
        p.employee.department  = "Human Resources";
        p.employee.gradeLevel  = "L1";
        p.employee.basicPay    = 35_000.0;
        p.employee.yearsOfService = 1;

        p.attendance                   = new AttendanceDTO();
        p.attendance.workingDaysInMonth = 22;
        p.attendance.leaveWithPay       = 0;
        p.attendance.leaveWithoutPay    = 1;
        p.attendance.hoursWorked        = 168.0;
        p.attendance.overtimeHours      = 0.0;

        p.financials                       = new FinancialsDTO();
        p.financials.pendingClaims         = 3_000.0;
        p.financials.approvedReimbursement = 3_000.0;
        p.financials.insurancePremium      = 500.0;
        p.financials.declaredInvestments   = 0.0;

        p.tax                  = new TaxContextDTO();
        p.tax.countryCode      = "IN";
        p.tax.currencyCode     = "INR";
        p.tax.taxRegime        = "OLD";
        p.tax.stateName        = "";           // ← BLANK — triggers MISSING_WORK_STATE
        p.tax.filingStatus     = "SINGLE";
        p.tax.nationalIDNumber = "UVWXY9012H";

        p.payPeriod = "2025-06";
        return p;
    }

    /**
     * EMP004 — EXCEEDS_CLAIM_LIMIT (WARNING)
     * Operations manager, India. pendingClaims exceeds L2 grade limit of ₹10,000.
     * Expected behaviour: reimbursement capped at ₹10,000, excess discarded, employee notified.
     */
    private PayrollDataPackage emp004_exceedsClaimLimit() {
        PayrollDataPackage p = new PayrollDataPackage();

        p.employee             = new EmployeeDTO();
        p.employee.empID       = "EMP004";
        p.employee.name        = "Meera Iyer";
        p.employee.department  = "Operations";
        p.employee.gradeLevel  = "L2";
        p.employee.basicPay    = 52_000.0;
        p.employee.yearsOfService = 4;

        p.attendance                   = new AttendanceDTO();
        p.attendance.workingDaysInMonth = 22;
        p.attendance.leaveWithPay       = 2;
        p.attendance.leaveWithoutPay    = 0;
        p.attendance.hoursWorked        = 176.0;
        p.attendance.overtimeHours      = 3.0;

        p.financials                       = new FinancialsDTO();
        p.financials.pendingClaims         = 18_500.0; // ← Exceeds L2 limit of 10,000
        p.financials.approvedReimbursement = 10_000.0;
        p.financials.insurancePremium      = 900.0;
        p.financials.declaredInvestments   = 80_000.0;

        p.tax                  = new TaxContextDTO();
        p.tax.countryCode      = "IN";
        p.tax.currencyCode     = "INR";
        p.tax.taxRegime        = "OLD";
        p.tax.stateName        = "TAMIL_NADU";
        p.tax.filingStatus     = "SINGLE";
        p.tax.nationalIDNumber = "LMNOP3456I";

        p.payPeriod = "2025-06";
        return p;
    }

    /**
     * EMP005 — Singapore employee (multi-country)
     * Uses Singapore TaxStrategy. No PT (not applicable outside India).
     * Expected behaviour: Singapore slab rates applied, SGD currency.
     */
    private PayrollDataPackage emp005_singapore() {
        PayrollDataPackage p = new PayrollDataPackage();

        p.employee             = new EmployeeDTO();
        p.employee.empID       = "EMP005";
        p.employee.name        = "Li Wei";
        p.employee.department  = "Product";
        p.employee.gradeLevel  = "L4";
        p.employee.basicPay    = 8_500.0;       // SGD
        p.employee.yearsOfService = 7;

        p.attendance                   = new AttendanceDTO();
        p.attendance.workingDaysInMonth = 22;
        p.attendance.leaveWithPay       = 1;
        p.attendance.leaveWithoutPay    = 0;
        p.attendance.hoursWorked        = 176.0;
        p.attendance.overtimeHours      = 2.5;

        p.financials                       = new FinancialsDTO();
        p.financials.pendingClaims         = 1_200.0;
        p.financials.approvedReimbursement = 1_200.0;
        p.financials.insurancePremium      = 180.0;
        p.financials.declaredInvestments   = 0.0;

        p.tax                  = new TaxContextDTO();
        p.tax.countryCode      = "SG";          // ← Singapore strategy selected
        p.tax.currencyCode     = "SGD";
        p.tax.taxRegime        = "SG_STANDARD";
        p.tax.stateName        = "SINGAPORE";
        p.tax.filingStatus     = "SINGLE";
        p.tax.nationalIDNumber = "S1234567A";   // NRIC

        p.payPeriod = "2025-06";
        return p;
    }

    /**
     * EMP006 — US employee, married filing status
     * Uses USFederal TaxStrategy with married brackets.
     * Expected behaviour: higher standard deduction ($29,200) applied.
     */
    private PayrollDataPackage emp006_usMarried() {
        PayrollDataPackage p = new PayrollDataPackage();

        p.employee             = new EmployeeDTO();
        p.employee.empID       = "EMP006";
        p.employee.name        = "John Mitchell";
        p.employee.department  = "Sales";
        p.employee.gradeLevel  = "L3";
        p.employee.basicPay    = 7_500.0;       // USD/month
        p.employee.yearsOfService = 5;

        p.attendance                   = new AttendanceDTO();
        p.attendance.workingDaysInMonth = 21;
        p.attendance.leaveWithPay       = 0;
        p.attendance.leaveWithoutPay    = 0;
        p.attendance.hoursWorked        = 168.0;
        p.attendance.overtimeHours      = 0.0;

        p.financials                       = new FinancialsDTO();
        p.financials.pendingClaims         = 500.0;
        p.financials.approvedReimbursement = 500.0;
        p.financials.insurancePremium      = 350.0;
        p.financials.declaredInvestments   = 0.0;

        p.tax                  = new TaxContextDTO();
        p.tax.countryCode      = "US";          // ← US Federal strategy selected
        p.tax.currencyCode     = "USD";
        p.tax.taxRegime        = "FEDERAL";
        p.tax.stateName        = "CALIFORNIA";
        p.tax.filingStatus     = "MARRIED";     // ← Married brackets applied
        p.tax.taxCode          = "W4-2024";
        p.tax.nationalIDNumber = "XXX-XX-1234"; // SSN (masked)

        p.payPeriod = "2025-06";
        return p;
    }

    /**
     * EMP007 — NEGATIVE_NET_PAY (MAJOR)
     * Employee with very high LOP + heavy deductions relative to basic pay.
     * Expected behaviour: net pay clamped to 0, excess carried as arrears, HR flagged.
     */
    private PayrollDataPackage emp007_negativeNetPay() {
        PayrollDataPackage p = new PayrollDataPackage();

        p.employee             = new EmployeeDTO();
        p.employee.empID       = "EMP007";
        p.employee.name        = "Suresh Babu";
        p.employee.department  = "Logistics";
        p.employee.gradeLevel  = "L1";
        p.employee.basicPay    = 22_000.0;      // Low basic pay
        p.employee.yearsOfService = 2;

        p.attendance                   = new AttendanceDTO();
        p.attendance.workingDaysInMonth = 22;
        p.attendance.leaveWithPay       = 0;
        p.attendance.leaveWithoutPay    = 15;   // ← Very high LOP (15 days out of 22)
        p.attendance.hoursWorked        = 56.0;
        p.attendance.overtimeHours      = 0.0;

        p.financials                       = new FinancialsDTO();
        p.financials.pendingClaims         = 0.0;
        p.financials.approvedReimbursement = 0.0;
        p.financials.insurancePremium      = 1_500.0; // ← High insurance relative to pay
        p.financials.declaredInvestments   = 0.0;

        p.tax                  = new TaxContextDTO();
        p.tax.countryCode      = "IN";
        p.tax.currencyCode     = "INR";
        p.tax.taxRegime        = "OLD";
        p.tax.stateName        = "WEST_BENGAL";
        p.tax.filingStatus     = "SINGLE";
        p.tax.nationalIDNumber = "FGHIJ6789K";

        p.payPeriod = "2025-06";
        return p;
    }

    /**
     * EMP008 — MISSING_PERFORMANCE_RATING (WARNING)
     * Employee whose grade level is not in the bonus rate table (simulates missing rating).
     * Expected behaviour: bonus = 0, arrears queued, payroll continues.
     */
    private PayrollDataPackage emp008_missingPerformanceRating() {
        PayrollDataPackage p = new PayrollDataPackage();

        p.employee             = new EmployeeDTO();
        p.employee.empID       = "EMP008";
        p.employee.name        = "Ananya Krishnan";
        p.employee.department  = "Design";
        p.employee.gradeLevel  = "CONTRACT"; // ← Not in bonus rate table → MISSING_PERFORMANCE_RATING
        p.employee.basicPay    = 45_000.0;
        p.employee.yearsOfService = 2;

        p.attendance                   = new AttendanceDTO();
        p.attendance.workingDaysInMonth = 22;
        p.attendance.leaveWithPay       = 1;
        p.attendance.leaveWithoutPay    = 0;
        p.attendance.hoursWorked        = 176.0;
        p.attendance.overtimeHours      = 1.5;

        p.financials                       = new FinancialsDTO();
        p.financials.pendingClaims         = 4_000.0;
        p.financials.approvedReimbursement = 4_000.0;
        p.financials.insurancePremium      = 700.0;
        p.financials.declaredInvestments   = 30_000.0;

        p.tax                  = new TaxContextDTO();
        p.tax.countryCode      = "IN";
        p.tax.currencyCode     = "INR";
        p.tax.taxRegime        = "NEW";
        p.tax.stateName        = "KARNATAKA";
        p.tax.filingStatus     = "SINGLE";
        p.tax.nationalIDNumber = "KLMNO2345L";

        p.payPeriod = "2025-06";
        return p;
    }

    /**
     * EMP009 — Gratuity eligible (long service)
     * 9 years of service → well above the 5-year minimum.
     * Expected behaviour: gratuity provision calculated and included in record.
     */
    private PayrollDataPackage emp009_gratuityEligible() {
        PayrollDataPackage p = new PayrollDataPackage();

        p.employee             = new EmployeeDTO();
        p.employee.empID       = "EMP009";
        p.employee.name        = "Vikram Malhotra";
        p.employee.department  = "Engineering";
        p.employee.gradeLevel  = "L5";
        p.employee.basicPay    = 1_50_000.0;    // Senior architect
        p.employee.yearsOfService = 9;          // ← Gratuity eligible (≥ 5 years)

        p.attendance                   = new AttendanceDTO();
        p.attendance.workingDaysInMonth = 22;
        p.attendance.leaveWithPay       = 2;
        p.attendance.leaveWithoutPay    = 0;
        p.attendance.hoursWorked        = 176.0;
        p.attendance.overtimeHours      = 0.0;

        p.financials                       = new FinancialsDTO();
        p.financials.pendingClaims         = 45_000.0;
        p.financials.approvedReimbursement = 45_000.0;
        p.financials.insurancePremium      = 2_500.0;
        p.financials.declaredInvestments   = 150_000.0;

        p.tax                  = new TaxContextDTO();
        p.tax.countryCode      = "IN";
        p.tax.currencyCode     = "INR";
        p.tax.taxRegime        = "OLD";
        p.tax.stateName        = "MAHARASHTRA";
        p.tax.filingStatus     = "MARRIED";
        p.tax.nationalIDNumber = "RSTUV7890M";

        p.payPeriod = "2025-06";
        return p;
    }

    /**
     * EMP010 — Happy path, junior employee, Old Regime
     * Entry-level, Andhra Pradesh, straightforward payroll with no exceptions.
     */
    private PayrollDataPackage emp010_juniorOldRegime() {
        PayrollDataPackage p = new PayrollDataPackage();

        p.employee             = new EmployeeDTO();
        p.employee.empID       = "EMP010";
        p.employee.name        = "Divya Reddy";
        p.employee.department  = "Customer Support";
        p.employee.gradeLevel  = "L1";
        p.employee.basicPay    = 28_000.0;
        p.employee.yearsOfService = 1;

        p.attendance                   = new AttendanceDTO();
        p.attendance.workingDaysInMonth = 22;
        p.attendance.leaveWithPay       = 0;
        p.attendance.leaveWithoutPay    = 0;
        p.attendance.hoursWorked        = 176.0;
        p.attendance.overtimeHours      = 2.0;

        p.financials                       = new FinancialsDTO();
        p.financials.pendingClaims         = 2_000.0;
        p.financials.approvedReimbursement = 2_000.0;
        p.financials.insurancePremium      = 400.0;
        p.financials.declaredInvestments   = 20_000.0;

        p.tax                  = new TaxContextDTO();
        p.tax.countryCode      = "IN";
        p.tax.currencyCode     = "INR";
        p.tax.taxRegime        = "OLD";
        p.tax.stateName        = "ANDHRA_PRADESH";
        p.tax.filingStatus     = "SINGLE";
        p.tax.nationalIDNumber = "NOPQR1234N";

        p.payPeriod = "2025-06";
        return p;
    }
}
