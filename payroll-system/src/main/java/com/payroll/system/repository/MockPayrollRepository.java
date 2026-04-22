package com.payroll.system.repository;

import java.util.*;
import com.hrms.db.repositories.payroll.IPayrollRepository;
import com.hrms.db.repositories.payroll.PayrollDataPackage;
import com.hrms.db.repositories.payroll.PayrollResultDTO;
import com.hrms.db.repositories.payroll.EmployeeDTO;
import com.hrms.db.repositories.payroll.AttendanceDTO;
import com.hrms.db.repositories.payroll.FinancialsDTO;
import com.hrms.db.repositories.payroll.TaxContextDTO;

/**
 * =============================================================================
 * CLASS: MockPayrollRepository
 * =============================================================================
 * Fully in-memory implementation of IPayrollRepository.
 * Used while the DB team's real implementation is not yet ready.
 *
 * TO SWAP TO REAL DB:
 * In PayrollPresenterImpl, change:
 * IPayrollRepository repo = new MockPayrollRepository();
 * to:
 * IPayrollRepository repo = new RealPayrollRepository(dbConnection);
 * Nothing else changes. This is Dependency Inversion Principle working.
 *
 * 10 EMPLOYEES (each exercises a specific exception path):
 * EMP001 — Happy path, India, New Regime, Karnataka
 * EMP002 — MISSING_TAX_REGIME (WARNING)
 * EMP003 — MISSING_WORK_STATE (MAJOR — skipped)
 * EMP004 — EXCEEDS_CLAIM_LIMIT (WARNING)
 * EMP005 — Singapore (multi-country)
 * EMP006 — US Federal, married
 * EMP007 — NEGATIVE_NET_PAY (MAJOR)
 * EMP008 — MISSING_PERFORMANCE_RATING (WARNING)
 * EMP009 — Gratuity eligible (9 years)
 * EMP010 — Happy path, junior, Old Regime
 * =============================================================================
 */
public class MockPayrollRepository implements IPayrollRepository {

    private final Map<String, PayrollDataPackage> employeeStore = new HashMap<>();
    private final List<String> activeEmpIDs = new ArrayList<>();
    private final List<String> savedResults = new ArrayList<>();
    private final List<String> errorLog = new ArrayList<>();

    public MockPayrollRepository() {
        seed();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // IPayrollRepository — Interface Implementation
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public PayrollDataPackage fetchEmployeeData(String empID, String payPeriod) {
        PayrollDataPackage pkg = employeeStore.get(empID);
        if (pkg == null) {
            System.err.printf("[MOCK DB] WARNING: No data found for empID=%s%n", empID);
            return null;
        }
        pkg.payPeriod = payPeriod;
        System.out.printf("[MOCK DB] Fetched %s (%s) for period %s%n",
                empID, pkg.employee.name, payPeriod);
        return pkg;
    }

    @Override
    public List<String> getAllActiveEmployeeIDs() {
        System.out.printf("[MOCK DB] Returning %d active employee IDs.%n", activeEmpIDs.size());
        return Collections.unmodifiableList(activeEmpIDs);
    }

    @Override
    public boolean savePayrollResult(String batchID, PayrollResultDTO result) {
        String entry = String.format(
                "SAVED | batch=%s | emp=%s | record=%s | gross=%.2f | net=%.2f | tax=%.2f | pf=%.2f",
                batchID, result.empID, result.recordID,
                result.finalGrossPay, result.finalNetPay, result.taxDeducted, result.pfAmount);
        savedResults.add(entry);
        System.out.println("[MOCK DB] " + entry);
        return true;
    }

    @Override
    public void logProcessingError(String batchID, String empID, String errorMsg) {
        String entry = String.format("ERROR | batch=%s | emp=%s | msg=%s", batchID, empID, errorMsg);
        errorLog.add(entry);
        System.err.println("[MOCK DB ERROR LOG] " + entry);
    }

    /** All saved payroll results (mock-only helper for tests). */
    public List<String> getSavedResults() {
        return Collections.unmodifiableList(savedResults);
    }

    /** All logged errors (mock-only helper for tests). */
    public List<String> getErrorLog() {
        return Collections.unmodifiableList(errorLog);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Data Seeding
    // ═══════════════════════════════════════════════════════════════════════════

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

    private void add(PayrollDataPackage pkg) {
        employeeStore.put(pkg.employee.empID, pkg);
        activeEmpIDs.add(pkg.employee.empID);
    }

    // ── Seed helpers ─────────────────────────────────────────────────────────

    private PayrollDataPackage emp001_happyPath() {
        PayrollDataPackage p = new PayrollDataPackage();
        p.employee = emp("EMP001", "Arjun Sharma", "Engineering", "L3", 80_000.0, 6);
        p.attendance = att(22, 1, 0, 176.0, 4.0);
        p.financials = fin(8_000.0, 8_000.0, 1_200.0, 150_000.0);
        p.tax = tax("IN", "INR", "NEW", "KARNATAKA", "SINGLE", "", "ABCDE1234F");
        p.payPeriod = "2025-06";
        return p;
    }

    private PayrollDataPackage emp002_missingTaxRegime() {
        PayrollDataPackage p = new PayrollDataPackage();
        p.employee = emp("EMP002", "Priya Nair", "Finance", "L2", 55_000.0, 3);
        p.attendance = att(22, 0, 2, 160.0, 0.0);
        p.financials = fin(6_000.0, 6_000.0, 800.0, 50_000.0);
        p.tax = tax("IN", "INR", "", "MAHARASHTRA", "SINGLE", "", "PQRST5678G"); // BLANK regime
        p.payPeriod = "2025-06";
        return p;
    }

    private PayrollDataPackage emp003_missingWorkState() {
        PayrollDataPackage p = new PayrollDataPackage();
        p.employee = emp("EMP003", "Ravi Kumar", "Human Resources", "L1", 35_000.0, 1);
        p.attendance = att(22, 0, 1, 168.0, 0.0);
        p.financials = fin(3_000.0, 3_000.0, 500.0, 0.0);
        p.tax = tax("IN", "INR", "OLD", "", "SINGLE", "", "UVWXY9012H"); // BLANK state
        p.payPeriod = "2025-06";
        return p;
    }

    private PayrollDataPackage emp004_exceedsClaimLimit() {
        PayrollDataPackage p = new PayrollDataPackage();
        p.employee = emp("EMP004", "Meera Iyer", "Operations", "L2", 52_000.0, 4);
        p.attendance = att(22, 2, 0, 176.0, 3.0);
        p.financials = fin(18_500.0, 10_000.0, 900.0, 80_000.0); // claims exceed L2 limit
        p.tax = tax("IN", "INR", "OLD", "TAMIL_NADU", "SINGLE", "", "LMNOP3456I");
        p.payPeriod = "2025-06";
        return p;
    }

    private PayrollDataPackage emp005_singapore() {
        PayrollDataPackage p = new PayrollDataPackage();
        p.employee = emp("EMP005", "Li Wei", "Product", "L4", 8_500.0, 7);
        p.attendance = att(22, 1, 0, 176.0, 2.5);
        p.financials = fin(1_200.0, 1_200.0, 180.0, 0.0);
        p.tax = tax("SG", "SGD", "SG_STANDARD", "SINGAPORE", "SINGLE", "", "S1234567A");
        p.payPeriod = "2025-06";
        return p;
    }

    private PayrollDataPackage emp006_usMarried() {
        PayrollDataPackage p = new PayrollDataPackage();
        p.employee = emp("EMP006", "John Mitchell", "Sales", "L3", 7_500.0, 5);
        p.attendance = att(21, 0, 0, 168.0, 0.0);
        p.financials = fin(500.0, 500.0, 350.0, 0.0);
        p.tax = tax("US", "USD", "FEDERAL", "CALIFORNIA", "MARRIED", "W4-2024", "XXX-XX-1234");
        p.payPeriod = "2025-06";
        return p;
    }

    private PayrollDataPackage emp007_negativeNetPay() {
        PayrollDataPackage p = new PayrollDataPackage();
        p.employee = emp("EMP007", "Suresh Babu", "Logistics", "L1", 22_000.0, 2);
        p.attendance = att(22, 0, 15, 56.0, 0.0); // 15 LOP days
        p.financials = fin(0.0, 0.0, 1_500.0, 0.0); // high insurance
        p.tax = tax("IN", "INR", "OLD", "WEST_BENGAL", "SINGLE", "", "FGHIJ6789K");
        p.payPeriod = "2025-06";
        return p;
    }

    private PayrollDataPackage emp008_missingPerformanceRating() {
        PayrollDataPackage p = new PayrollDataPackage();
        p.employee = emp("EMP008", "Ananya Krishnan", "Design", "CONTRACT", 45_000.0, 2);
        p.attendance = att(22, 1, 0, 176.0, 1.5);
        p.financials = fin(4_000.0, 4_000.0, 700.0, 30_000.0);
        p.tax = tax("IN", "INR", "NEW", "KARNATAKA", "SINGLE", "", "KLMNO2345L");
        p.payPeriod = "2025-06";
        return p;
    }

    private PayrollDataPackage emp009_gratuityEligible() {
        PayrollDataPackage p = new PayrollDataPackage();
        p.employee = emp("EMP009", "Vikram Malhotra", "Engineering", "L5", 150_000.0, 9);
        p.attendance = att(22, 2, 0, 176.0, 0.0);
        p.financials = fin(45_000.0, 45_000.0, 2_500.0, 150_000.0);
        p.tax = tax("IN", "INR", "OLD", "MAHARASHTRA", "MARRIED", "", "RSTUV7890M");
        p.payPeriod = "2025-06";
        return p;
    }

    private PayrollDataPackage emp010_juniorOldRegime() {
        PayrollDataPackage p = new PayrollDataPackage();
        p.employee = emp("EMP010", "Divya Reddy", "Customer Support", "L1", 28_000.0, 1);
        p.attendance = att(22, 0, 0, 176.0, 2.0);
        p.financials = fin(2_000.0, 2_000.0, 400.0, 20_000.0);
        p.tax = tax("IN", "INR", "OLD", "ANDHRA_PRADESH", "SINGLE", "", "NOPQR1234N");
        p.payPeriod = "2025-06";
        return p;
    }

    // ── Builder helpers ───────────────────────────────────────────────────────

    private EmployeeDTO emp(String id, String name, String dept,
            String grade, double pay, int years) {
        EmployeeDTO e = new EmployeeDTO();
        e.empID = id;
        e.name = name;
        e.department = dept;
        e.gradeLevel = grade;
        e.basicPay = pay;
        e.yearsOfService = years;
        return e;
    }

    private AttendanceDTO att(int days, int lwp, int lop,
            double hours, double ot) {
        AttendanceDTO a = new AttendanceDTO();
        a.workingDaysInMonth = days;
        a.leaveWithPay = lwp;
        a.leaveWithoutPay = lop;
        a.hoursWorked = hours;
        a.overtimeHours = ot;
        return a;
    }

    private FinancialsDTO fin(double claims, double approved,
            double insurance, double investments) {
        FinancialsDTO f = new FinancialsDTO();
        f.pendingClaims = claims;
        f.approvedReimbursement = approved;
        f.insurancePremium = insurance;
        f.declaredInvestments = investments;
        return f;
    }

    private TaxContextDTO tax(String country, String currency,
            String regime, String state, String filing, String code, String natID) {
        TaxContextDTO t = new TaxContextDTO();
        t.countryCode = country;
        t.currencyCode = currency;
        t.taxRegime = regime;
        t.stateName = state;
        t.filingStatus = filing;
        t.taxCode = code;
        t.nationalIDNumber = natID;
        return t;
    }
}

