package com.hrms.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Integration contract consumed by the Attrition Analysis subsystem.
 */
public interface IPayrollService {

    double getNetSalary(String employeeId);

    Map<String, Double> getSalaryBreakdown(String employeeId, LocalDate payPeriod);

    double getAverageSalaryByDepartment(String department);

    List<PayslipSummary> getPayslipHistory(String employeeId, LocalDate from, LocalDate to);

    interface PayslipSummary {
        LocalDate getPayPeriodStart();

        double getNetPaid();

        double getTotalBonus();
    }
}
