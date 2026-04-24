package com.payroll.system.presenter;

import com.hrms.db.repositories.payroll.IPayrollRepository;
import com.hrms.db.repositories.payroll.PayrollDataPackage;
import com.hrms.db.repositories.payroll.PayrollResultDTO;
import com.payroll.system.exception.PayrollException;
import com.payroll.system.model.Employee;
import com.payroll.system.model.PayrollRecord;
import com.payroll.system.repository.MockPayrollRepository;
import com.payroll.system.repository.PayrollRepositoryImpl;
import com.payroll.system.service.PayRunController;
import com.payroll.system.service.PayrollFacade;
import com.payroll.system.service.PayrollSystemFactory;
import com.payroll.system.service.PayslipExportService;
import com.payroll.system.util.AuditLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Bridges the UI-facing presenter contract to the payroll workflow. */
public class PayrollPresenterImpl implements PayrollPresenter {

    private final IPayrollRepository repo;
    private final AuditLogger auditLogger;
    private final PayslipExportService payslipExportService;
    private PayRunController lastController;
    private String lastPayPeriod;

    /** Creates the presenter with the database-backed repository. */
    public PayrollPresenterImpl() {
        this.repo = new PayrollRepositoryImpl();
        this.auditLogger = new AuditLogger();
        this.payslipExportService = new PayslipExportService(repo, auditLogger);
        auditLogger.logWarning("SYSTEM", "PayrollPresenterImpl initialized - REAL DB Active");
    }

    /** Creates the presenter with an injected repository, mainly for tests. */
    public PayrollPresenterImpl(IPayrollRepository repo, AuditLogger auditLogger) {
        this.repo = repo;
        this.auditLogger = auditLogger;
        this.payslipExportService = new PayslipExportService(repo, auditLogger);
    }

    @Override
    public List<EmployeeViewModel> loadAllEmployees(String payPeriod) {
        this.lastPayPeriod = payPeriod;
        List<String> ids = repo.getAllActiveEmployeeIDs();
        List<EmployeeViewModel> result = new ArrayList<>();
        for (String id : ids) {
            PayrollDataPackage pkg = repo.fetchEmployeeData(id, payPeriod);
            if (pkg != null) {
                result.add(toEmployeeViewModel(pkg));
            }
        }
        result.sort(Comparator
                .comparing((EmployeeViewModel vm) -> normalized(vm.department))
                .thenComparing(vm -> normalized(vm.name))
                .thenComparing(vm -> normalized(vm.empID)));
        return result;
    }

    @Override
    public int getEmployeeCount() {
        return repo.getAllActiveEmployeeIDs().size();
    }

    @Override
    public BatchResult runBatch(String batchId, String payPeriod) {
        this.lastPayPeriod = payPeriod;

        List<String> allIds = repo.getAllActiveEmployeeIDs();
        Map<String, PayrollDataPackage> pkgMap = new LinkedHashMap<>();
        List<Employee> empList = new ArrayList<>();

        for (String id : allIds) {
            PayrollDataPackage pkg = repo.fetchEmployeeData(id, payPeriod);
            if (pkg == null) {
                continue;
            }
            pkgMap.put(id, pkg);
            Employee emp = toEmployee(pkg);
            if (emp != null) {
                empList.add(emp);
            }
        }

        PayrollFacade facade = PayrollSystemFactory.createFacade(auditLogger);
        PayRunController controller = new PayRunController(batchId, payPeriod, facade, auditLogger);
        this.lastController = controller;

        try {
            List<PayrollRecord> completed = controller.executeBatchPayroll(empList);
            List<String> auditLog = auditLogger.getAllEntries();
            Set<String> processedIds = new HashSet<>();
            List<PayrollResultViewModel> results = new ArrayList<>();

            for (PayrollRecord rec : completed) {
                processedIds.add(rec.getEmpID());
                PayrollDataPackage pkg = pkgMap.get(rec.getEmpID());
                String name = pkg != null ? pkg.employee.name : rec.getEmpID();
                String dept = pkg != null ? pkg.employee.department : "-";
                String bpay = pkg != null ? fmt(pkg.employee.basicPay, pkg.tax.currencyCode) : "-";
                results.add(toPayrollResultViewModel(batchId, rec, name, dept, bpay, auditLog));

                PayrollResultDTO dto = toResultDTO(rec);
                repo.savePayrollResult(batchId, dto);
            }

            for (String id : allIds) {
                if (!processedIds.contains(id)) {
                    PayrollDataPackage pkg = pkgMap.get(id);
                    String name = pkg != null ? pkg.employee.name : id;
                    String dept = pkg != null ? pkg.employee.department : "-";
                    String bpay = pkg != null ? fmt(pkg.employee.basicPay, pkg.tax.currencyCode) : "-";
                    results.add(new PayrollResultViewModel(
                            batchId + "-" + id, id, name, dept,
                            bpay, "-", "-", "-", "0.00",
                            "SKIPPED - MISSING_WORK_STATE"));
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
        if (lastController == null) {
            return false;
        }
        return lastController.verifyCalculations();
    }

    @Override
    public String getDbStatus() {
        return (repo instanceof MockPayrollRepository) ? "MockDB Active" : "Real DB Connected";
    }

    @Override
    public String getEmployeePayslip(String employeeId, String payPeriod) {
        return payslipExportService.getLatestPayslipText(employeeId, payPeriod);
    }

    @Override
    public String getAllEmployeePayslips(String payPeriod) {
        return payslipExportService.getAllLatestPayslipsAsText(payPeriod);
    }

    @Override
    public String getAllEmployeePayslipsCsv(String payPeriod) {
        return payslipExportService.getAllLatestPayslipsAsCsv(payPeriod);
    }

    /** Converts repository data to a validated payroll employee. */
    private Employee toEmployee(PayrollDataPackage pkg) {
        try {
            return new Employee.Builder(pkg.employee.empID, pkg.employee.name)
                    .department(pkg.employee.department)
                    .roleId(pkg.employee.roleId)
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
            auditLogger.logMajorError(pkg.employee.empID, "MISSING_BASE_SALARY: " + e.getMessage());
            return null;
        }
    }

    /** Converts repository data to the employee row shown in the UI. */
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
                pkg.employee.yearsOfService + " yr" + (pkg.employee.yearsOfService == 1 ? "" : "s"),
                String.valueOf(pkg.attendance.leaveWithoutPay),
                fmt(pkg.attendance.overtimeHours),
                fmt(pkg.financials.pendingClaims, pkg.tax.currencyCode),
                fmt(pkg.financials.approvedReimbursement, pkg.tax.currencyCode));
    }

    /** Builds the result row and derives a user-facing status label. */
    private PayrollResultViewModel toPayrollResultViewModel(
            String batchId, PayrollRecord rec, String name, String dept, String basicPay, List<String> allAuditEntries) {

        String status;
        if (rec.isFlaggedForHrReview()) {
            String reason = rec.getHrReviewReason();
            if (reason != null && reason.contains("NEGATIVE_NET_PAY")) {
                status = "NEGATIVE_NET_PAY -> arrears set";
            } else {
                status = reason != null ? reason : "HR Review Required";
            }
        } else if (rec.getBonusArrears() > 0) {
            status = "MISSING_PERFORMANCE_RATING -> bonus queued";
        } else {
            String warnLine = allAuditEntries.stream()
                    .filter(e -> e.contains("[WARN]") && e.contains("EmpID=" + rec.getEmpID()))
                    .findFirst()
                    .orElse(null);
            if (warnLine != null) {
                if (warnLine.contains("MISSING_TAX_REGIME")) {
                    status = "MISSING_TAX_REGIME -> defaulted OLD";
                } else if (warnLine.contains("EXCEEDS_CLAIM_LIMIT")) {
                    status = "EXCEEDS_CLAIM_LIMIT -> claim capped";
                } else {
                    status = "Warning (see Audit Log)";
                }
            } else {
                status = "OK";
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

    /** Maps a computed record to its persistence DTO. */
    private PayrollResultDTO toResultDTO(PayrollRecord rec) {
        PayrollResultDTO dto = new PayrollResultDTO();
        dto.empID = rec.getEmpID();
        dto.recordID = rec.getRecordID();
        dto.payPeriod = rec.getPayPeriod();
        dto.finalGrossPay = rec.getFinalGrossPay();
        dto.finalNetPay = rec.getFinalNetPay();
        dto.penaltyAmount = rec.getPenaltyAmount();
        dto.pfAmount = rec.getPfAmount();
        dto.taxDeducted = rec.getMonthlyTdsAmount();
        dto.payoutAmount = rec.getPayoutAmount();
        dto.overtimePay = rec.getOvertimePay();
        dto.ptAmount = rec.getPtAmount();
        dto.reimbursementPayout = rec.getReimbursementPayout();
        dto.gratuityAmount = rec.getGratuityAmount();
        return dto;
    }

    private String fmt(double value) {
        return String.format("%.2f", value);
    }

    /** Formats a value with the appropriate currency prefix when known. */
    private String fmt(double value, String currencyCode) {
        if (currencyCode == null) {
            return String.format("%,.2f", value);
        }

        String symbol = switch (currencyCode) {
            case "INR" -> "Rs";
            case "USD" -> "$";
            case "SGD" -> "S$";
            default -> currencyCode + " ";
        };
        return symbol + String.format("%,.2f", value);
    }

    private String normalized(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
