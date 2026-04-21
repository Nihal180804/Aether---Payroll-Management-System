package com.payroll.system.presenter;

import com.payroll.system.repository.*;
import com.hrms.db.repositories.payroll.*;
import com.payroll.system.exception.PayrollException;
import com.payroll.system.model.Employee;
import com.payroll.system.model.PayrollRecord;
import com.payroll.system.service.PayrollFacade;
import com.payroll.system.service.PayRunController;
import com.payroll.system.service.PayrollSystemFactory;
import com.payroll.system.util.AuditLogger;

import java.text.NumberFormat;
import java.util.*;

/**
 * =============================================================================
 * CLASS: PayrollPresenterImpl
 * =============================================================================
 * Implements PayrollPresenter by wiring:
 * IPayrollRepository → Employee.Builder → PayRunController → PayrollFacade
 *
 * The UI only holds a PayrollPresenter reference — it has zero knowledge of
 * model, service, pattern, or exception classes.
 * =============================================================================
 */
public class PayrollPresenterImpl implements PayrollPresenter {

    private final IPayrollRepository repo;
    private final AuditLogger auditLogger;
    private PayRunController lastController;
    private String lastPayPeriod;

    // ── Default constructor — uses real Database ──────────────────────
    public PayrollPresenterImpl() {
        this.repo = new PayrollRepositoryImpl();
        this.auditLogger = new AuditLogger();
        auditLogger.logWarning("SYSTEM", "PayrollPresenterImpl initialized — REAL DB Active");
    }

    // ── DI constructor — inject any IPayrollRepository (real DB, test stub…) ─
    public PayrollPresenterImpl(IPayrollRepository repo, AuditLogger auditLogger) {
        this.repo = repo;
        this.auditLogger = auditLogger;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PayrollPresenter — Interface Implementation
    // ═══════════════════════════════════════════════════════════════════════════

    @Override
    public List<EmployeeViewModel> loadAllEmployees(String payPeriod) {
        this.lastPayPeriod = payPeriod;
        List<String> ids = repo.getAllActiveEmployeeIDs();
        List<EmployeeViewModel> result = new ArrayList<>();
        for (String id : ids) {
            PayrollDataPackage pkg = repo.fetchEmployeeData(id, payPeriod);
            if (pkg != null)
                result.add(toEmployeeViewModel(pkg));
        }
        return result;
    }

    @Override
    public int getEmployeeCount() {
        return repo.getAllActiveEmployeeIDs().size();
    }

    @Override
    public BatchResult runBatch(String batchId, String payPeriod) {
        this.lastPayPeriod = payPeriod;

        // 1. Fetch all employee packages
        List<String> allIds = repo.getAllActiveEmployeeIDs();
        Map<String, PayrollDataPackage> pkgMap = new LinkedHashMap<>();
        List<Employee> empList = new ArrayList<>();

        for (String id : allIds) {
            PayrollDataPackage pkg = repo.fetchEmployeeData(id, payPeriod);
            if (pkg == null)
                continue;
            pkgMap.put(id, pkg);
            Employee emp = toEmployee(pkg);
            if (emp != null)
                empList.add(emp);
        }

        // 2. Wire full backend stack
        PayrollFacade facade = PayrollSystemFactory.createFacade(auditLogger);
        PayRunController controller = new PayRunController(batchId, payPeriod, facade, auditLogger);
        this.lastController = controller;

        try {
            // 3. Execute batch
            List<PayrollRecord> completed = controller.executeBatchPayroll(empList);
            List<String> auditLog = auditLogger.getAllEntries();

            // 4. Map results → ViewModels
            Set<String> processedIds = new HashSet<>();
            List<PayrollResultViewModel> results = new ArrayList<>();

            for (PayrollRecord rec : completed) {
                processedIds.add(rec.getEmpID());
                PayrollDataPackage pkg = pkgMap.get(rec.getEmpID());
                String name = pkg != null ? pkg.employee.name : rec.getEmpID();
                String dept = pkg != null ? pkg.employee.department : "—";
                String bpay = pkg != null ? fmt(pkg.employee.basicPay, pkg.tax.currencyCode) : "—";
                results.add(toPayrollResultViewModel(batchId, rec, name, dept, bpay, auditLog));

                // 5. Persist result
                PayrollResultDTO dto = toResultDTO(rec);
                repo.savePayrollResult(batchId, dto);
            }

            // 6. Add skipped employees as explicit rows
            for (String id : allIds) {
                if (!processedIds.contains(id)) {
                    PayrollDataPackage pkg = pkgMap.get(id);
                    String name = pkg != null ? pkg.employee.name : id;
                    String dept = pkg != null ? pkg.employee.department : "—";
                    String bpay = pkg != null ? fmt(pkg.employee.basicPay, pkg.tax.currencyCode) : "—";
                    results.add(new PayrollResultViewModel(
                            batchId + "-" + id, id, name, dept,
                            bpay, "—", "—", "—", "0.00",
                            "✗ SKIPPED — MISSING_WORK_STATE"));
                    repo.logProcessingError(batchId, id, "MISSING_WORK_STATE: employee skipped from batch");
                }
            }

            int skipped = allIds.size() - processedIds.size();
            return new BatchResult(results, completed.size(), skipped, null);

        } catch (PayrollException.InvalidPayPeriod e) {
            auditLogger.logMajorError("SYSTEM", "INVALID_PAY_PERIOD: " + e.getMessage());
            return new BatchResult(Collections.emptyList(), 0, 0,
                    "INVALID_PAY_PERIOD: " + e.getMessage());
        }
    }

    @Override
    public List<String> getAuditLog() {
        return auditLogger.getAllEntries();
    }

    @Override
    public void clearAuditLog() {
        auditLogger.clearLog();
    }

    @Override
    public boolean verifyLastBatch() {
        if (lastController == null)
            return false;
        return lastController.verifyCalculations();
    }

    @Override
    public String getDbStatus() {
        return (repo instanceof MockPayrollRepository)
                ? "MockDB Active — 10 employees"
                : "Real DB Connected";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Private Helpers
    // ═══════════════════════════════════════════════════════════════════════════

    /** PayrollDataPackage → Employee using Builder (validates basicPay > 0). */
    private Employee toEmployee(PayrollDataPackage pkg) {
        try {
            return new Employee.Builder(pkg.employee.empID, pkg.employee.name)
                    .department(pkg.employee.department)
                    .gradeLevel(pkg.employee.gradeLevel)
                    .basicPay(pkg.employee.basicPay)
                    .yearsOfService(pkg.employee.yearsOfService)
                    .workingDaysInMonth(pkg.attendance.workingDaysInMonth)
                    .leaveWithoutPay(pkg.attendance.leaveWithoutPay)
                    .overtimeHours(pkg.attendance.overtimeHours)
                    .pendingClaims(pkg.financials.pendingClaims)
                    .approvedReimbursement(pkg.financials.approvedReimbursement)
                    .insurancePremium(pkg.financials.insurancePremium)
                    .declaredInvestments(pkg.financials.declaredInvestments)
                    .taxRegime(Objects.requireNonNullElse(pkg.tax.taxRegime, "OLD"))
                    .countryCode(Objects.requireNonNullElse(pkg.tax.countryCode, "IN"))
                    .currencyCode(Objects.requireNonNullElse(pkg.tax.currencyCode, "INR"))
                    .stateName(Objects.requireNonNullElse(pkg.tax.stateName, ""))
                    .filingStatus(Objects.requireNonNullElse(pkg.tax.filingStatus, "SINGLE"))
                    .build();
        } catch (IllegalStateException e) {
            auditLogger.logMajorError(pkg.employee.empID,
                    "MISSING_BASE_SALARY: " + e.getMessage());
            return null;
        }
    }

    /** PayrollDataPackage → EmployeeViewModel. */
    private EmployeeViewModel toEmployeeViewModel(PayrollDataPackage pkg) {
        String regime = Objects.requireNonNullElse(pkg.tax.taxRegime, "");
        String state = Objects.requireNonNullElse(pkg.tax.stateName, "");
        
        return new EmployeeViewModel(
                pkg.employee.empID,
                pkg.employee.name,
                pkg.employee.department,
                pkg.employee.gradeLevel,
                fmt(pkg.employee.basicPay, pkg.tax.currencyCode),
                pkg.tax.countryCode,
                regime.isBlank() ? "NOT SET" : regime,
                state.isBlank() ? "NOT SET" : state,
                pkg.employee.yearsOfService + " yr" + (pkg.employee.yearsOfService == 1 ? "" : "s"));
    }

    /**
     * PayrollRecord → PayrollResultViewModel. Scans audit log for per-employee
     * warnings.
     */
    private PayrollResultViewModel toPayrollResultViewModel(
            String batchId, PayrollRecord rec,
            String name, String dept, String basicPay,
            List<String> allAuditEntries) {

        String status;
        if (rec.isFlaggedForHrReview()) {
            String reason = rec.getHrReviewReason();
            if (reason != null && reason.contains("NEGATIVE_NET_PAY"))
                status = "⚠ NEGATIVE_NET_PAY → arrears set";
            else
                status = "⚠ " + (reason != null ? reason : "HR Review Required");
        } else if (rec.getBonusArrears() > 0) {
            status = "⚠ MISSING_PERFORMANCE_RATING → bonus queued";
        } else {
            // Check audit log for non-fatal warnings for this specific employee
            String warnLine = allAuditEntries.stream()
                    .filter(e -> e.contains("[WARN]") && e.contains("EmpID=" + rec.getEmpID()))
                    .findFirst().orElse(null);
            if (warnLine != null) {
                if (warnLine.contains("MISSING_TAX_REGIME"))
                    status = "⚠ MISSING_TAX_REGIME → defaulted OLD";
                else if (warnLine.contains("EXCEEDS_CLAIM_LIMIT"))
                    status = "⚠ EXCEEDS_CLAIM_LIMIT → claim capped";
                else
                    status = "⚠ Warning (see Audit Log)";
            } else {
                status = "✔ OK";
            }
        }

        return new PayrollResultViewModel(
                batchId + "-" + rec.getEmpID(),
                rec.getEmpID(),
                name,
                dept,
                basicPay,
                fmt(rec.getPfAmount()),
                fmt(rec.getMonthlyTdsAmount()),
                fmt(rec.getPtAmount()),
                fmt(rec.getFinalNetPay()),
                status);
    }

    /** PayrollRecord → PayrollResultDTO for persistence. */
    private PayrollResultDTO toResultDTO(PayrollRecord rec) {
        PayrollResultDTO dto = new PayrollResultDTO();
        dto.empID = rec.getEmpID();
        dto.recordID = rec.getRecordID();
        dto.finalGrossPay = rec.getFinalGrossPay();
        dto.finalNetPay = rec.getFinalNetPay();
        dto.penaltyAmount = rec.getPenaltyAmount();
        dto.pfAmount = rec.getPfAmount();
        dto.taxDeducted = rec.getMonthlyTdsAmount();
        dto.payoutAmount = rec.getPayoutAmount();
        return dto;
    }

    /** Format a double as a locale number string. */
    private String fmt(double value) {
        return String.format("%.2f", value);
    }

    /** Format with currency symbol. */
    private String fmt(double value, String currencyCode) {
        if (currencyCode == null) return String.format("%,.2f", value);
        
        String symbol = switch (currencyCode) {
            case "INR" -> "₹";
            case "USD" -> "$";
            case "SGD" -> "S$";
            default -> currencyCode + " ";
        };
        return symbol + String.format("%,.2f", value);
    }
}


