package com.payroll.system;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hrms.service.IPayrollService;
import com.payroll.system.presenter.EmployeeViewModel;
import com.payroll.system.presenter.PayrollPresenter;
import com.payroll.system.presenter.PayrollPresenterImpl;
import com.payroll.system.service.PayrollServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class AppTest {

    @Test
    public void presenterLoadsEmployeesFromIntegratedRepository() {
        PayrollPresenter presenter = new PayrollPresenterImpl();

        List<EmployeeViewModel> employees = presenter.loadAllEmployees("2025-06");

        assertFalse(employees.isEmpty(), "Expected employees from the integrated repository");
        assertTrue(presenter.getEmployeeCount() >= employees.size());
    }

    @Test
    public void presenterGeneratesIndividualAndBulkPayslips() {
        PayrollPresenter presenter = new PayrollPresenterImpl();
        List<EmployeeViewModel> employees = presenter.loadAllEmployees("2025-06");
        assertFalse(employees.isEmpty());

        String employeePayslip = presenter.getEmployeePayslip(employees.get(0).empID, "2025-06");
        String allPayslips = presenter.getAllEmployeePayslips("2025-06");
        String csv = presenter.getAllEmployeePayslipsCsv("2025-06");

        assertTrue(employeePayslip.contains("AETHER PAYROLL SOLUTIONS"));
        assertTrue(allPayslips.contains("AETHER PAYROLL SOLUTIONS"));
        assertTrue(csv.startsWith("employee_id,name,department,pay_period,gross_pay,tds,pf,pt,net_pay"));
    }

    @Test
    public void attritionServiceProvidesPayrollBreakdown() {
        PayrollPresenter presenter = new PayrollPresenterImpl();
        List<EmployeeViewModel> employees = presenter.loadAllEmployees("2025-06");
        assertFalse(employees.isEmpty());

        IPayrollService payrollService = new PayrollServiceImpl();
        Map<String, Double> breakdown = payrollService.getSalaryBreakdown(
                employees.get(0).empID, LocalDate.of(2025, 6, 1));

        assertNotNull(breakdown);
        assertTrue(breakdown.containsKey("net_pay"));
    }
}
