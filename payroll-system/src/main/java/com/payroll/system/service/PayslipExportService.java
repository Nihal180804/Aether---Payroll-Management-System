package com.payroll.system.service;

import com.hrms.db.repositories.payroll.IPayrollRepository;
import com.hrms.db.repositories.payroll.PayrollDataPackage;
import com.payroll.system.exception.PayrollException;
import com.payroll.system.model.Employee;
import com.payroll.system.model.PayrollRecord;
import com.payroll.system.repository.PayrollRepositoryImpl;
import com.payroll.system.util.AuditLogger;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PayslipExportService {

    private final IPayrollRepository repo;
    private final AuditLogger auditLogger;
    private final DigitalPayslipGenerator payslipGenerator;

    public PayslipExportService() {
        this(new PayrollRepositoryImpl(), new AuditLogger());
    }

    public PayslipExportService(IPayrollRepository repo, AuditLogger auditLogger) {
        this.repo = repo;
        this.auditLogger = auditLogger;
        this.payslipGenerator = new DigitalPayslipGenerator("payslips");
    }

    public String getLatestPayslipText(String employeeId, String payPeriod) {
        PayrollDataPackage pkg = repo.fetchEmployeeData(employeeId, payPeriod);
        if (pkg == null) {
            return "No payslip found for employee: " + employeeId;
        }

        try {
            Employee employee = toEmployee(pkg);
            PayrollRecord record = calculateRecord(employee, payPeriod);
            return payslipGenerator.generatePayslipText(employee, record);
        } catch (RuntimeException e) {
            return "Unable to generate payslip for employee " + employeeId + ": " + e.getMessage();
        }
    }

    public Map<String, String> getLatestPayslipTextByEmployee(String payPeriod) {
        Map<String, String> payslips = new LinkedHashMap<>();
        for (String employeeId : repo.getAllActiveEmployeeIDs()) {
            payslips.put(employeeId, getLatestPayslipText(employeeId, payPeriod));
        }
        return payslips;
    }

    public String getAllLatestPayslipsAsText(String payPeriod) {
        StringBuilder export = new StringBuilder();
        for (String payslip : getLatestPayslipTextByEmployee(payPeriod).values()) {
            if (!export.isEmpty()) {
                export.append(System.lineSeparator()).append(System.lineSeparator());
            }
            export.append(payslip);
        }
        return export.toString();
    }

    public String getAllLatestPayslipsAsCsv(String payPeriod) {
        StringBuilder csv = new StringBuilder();
        csv.append("employee_id,name,department,pay_period,gross_pay,tds,pf,pt,net_pay,bonus,reimbursement,gratuity,status")
                .append(System.lineSeparator());

        for (String employeeId : repo.getAllActiveEmployeeIDs()) {
            PayrollDataPackage pkg = repo.fetchEmployeeData(employeeId, payPeriod);
            if (pkg == null) {
                csv.append(csv(employeeId)).append(",,,,,,,,,,,,").append(System.lineSeparator());
                continue;
            }

            try {
                Employee employee = toEmployee(pkg);
                PayrollRecord record = calculateRecord(employee, payPeriod);
                csv.append(csv(employee.getEmpID())).append(',')
                        .append(csv(employee.getName())).append(',')
                        .append(csv(employee.getDepartment())).append(',')
                        .append(csv(record.getPayPeriod())).append(',')
                        .append(record.getFinalGrossPay()).append(',')
                        .append(record.getMonthlyTdsAmount()).append(',')
                        .append(record.getPfAmount()).append(',')
                        .append(record.getPtAmount()).append(',')
                        .append(record.getFinalNetPay()).append(',')
                        .append(record.getPayoutAmount()).append(',')
                        .append(record.getReimbursementPayout()).append(',')
                        .append(record.getGratuityAmount()).append(',')
                        .append(csv(record.isFlaggedForHrReview() ? "HR_REVIEW" : "READY"))
                        .append(System.lineSeparator());
            } catch (RuntimeException e) {
                csv.append(csv(employeeId)).append(',')
                        .append(csv(pkg.employee.name)).append(',')
                        .append(csv(pkg.employee.department)).append(',')
                        .append(csv(payPeriod)).append(",,,,,,,,,")
                        .append(csv("ERROR: " + e.getClass().getSimpleName()))
                        .append(System.lineSeparator());
            }
        }
        return csv.toString();
    }

    public PayrollRecord getLatestPayrollRecord(String employeeId, String payPeriod) {
        PayrollDataPackage pkg = repo.fetchEmployeeData(employeeId, payPeriod);
        if (pkg == null) {
            return null;
        }
        return calculateRecord(toEmployee(pkg), payPeriod);
    }

    public List<String> getAllEmployeeIds() {
        return repo.getAllActiveEmployeeIDs();
    }

    private PayrollRecord calculateRecord(Employee employee, String payPeriod) {
        PayrollRecord record = new PayrollRecord("PAYSLIP-" + employee.getEmpID() + "-" + payPeriod,
                employee.getEmpID(), "PAYSLIP-EXPORT", payPeriod);
        try {
            PayrollFacade facade = PayrollSystemFactory.createFacade(auditLogger);
            facade.processEmployee(employee, record);
        } catch (PayrollException.MissingWorkState e) {
            record.flagForHrReview(e.getExceptionName() + ": " + e.getMessage());
            auditLogger.logMajorError(employee.getEmpID(), e.getMessage());
        }
        return record;
    }

    private Employee toEmployee(PayrollDataPackage pkg) {
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
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public static String toPayPeriod(LocalDate date) {
        return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
    }
}
