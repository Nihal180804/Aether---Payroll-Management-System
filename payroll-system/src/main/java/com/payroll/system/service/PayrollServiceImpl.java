package com.payroll.system.service;

import com.hrms.service.IPayrollService;
import com.payroll.system.model.PayrollRecord;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PayrollServiceImpl implements IPayrollService {

    private final PayslipExportService payslipExportService;

    public PayrollServiceImpl() {
        this(new PayslipExportService());
    }

    public PayrollServiceImpl(PayslipExportService payslipExportService) {
        this.payslipExportService = payslipExportService;
    }

    @Override
    public double getNetSalary(String employeeId) {
        PayrollRecord record = payslipExportService.getLatestPayrollRecord(employeeId, "2025-06");
        return record == null ? 0.0 : record.getFinalNetPay();
    }

    @Override
    public Map<String, Double> getSalaryBreakdown(String employeeId, LocalDate payPeriod) {
        PayrollRecord record = payslipExportService.getLatestPayrollRecord(employeeId,
                PayslipExportService.toPayPeriod(payPeriod));
        Map<String, Double> breakdown = new LinkedHashMap<>();
        if (record == null) {
            return breakdown;
        }

        breakdown.put("gross_pay", record.getFinalGrossPay());
        breakdown.put("net_pay", record.getFinalNetPay());
        breakdown.put("tds", record.getMonthlyTdsAmount());
        breakdown.put("pf", record.getPfAmount());
        breakdown.put("pt", record.getPtAmount());
        breakdown.put("bonus", record.getPayoutAmount());
        breakdown.put("reimbursement", record.getReimbursementPayout());
        breakdown.put("gratuity", record.getGratuityAmount());
        breakdown.put("lop_penalty", record.getPenaltyAmount());
        breakdown.put("overtime", record.getOvertimePay());
        return breakdown;
    }

    @Override
    public double getAverageSalaryByDepartment(String department) {
        double total = 0.0;
        int count = 0;

        String csv = payslipExportService.getAllLatestPayslipsAsCsv("2025-06");
        for (String line : csv.split("\\R")) {
            if (line.startsWith("employee_id") || line.isBlank()) {
                continue;
            }
            String[] columns = parseCsvLine(line);
            if (columns.length > 8 && department.equalsIgnoreCase(columns[2])) {
                total += Double.parseDouble(columns[8].isBlank() ? "0" : columns[8]);
                count++;
            }
        }
        return count == 0 ? 0.0 : total / count;
    }

    @Override
    public List<PayslipSummary> getPayslipHistory(String employeeId, LocalDate from, LocalDate to) {
        List<PayslipSummary> history = new ArrayList<>();
        LocalDate cursor = LocalDate.of(from.getYear(), from.getMonthValue(), 1);
        LocalDate end = LocalDate.of(to.getYear(), to.getMonthValue(), 1);

        while (!cursor.isAfter(end)) {
            PayrollRecord record = payslipExportService.getLatestPayrollRecord(employeeId,
                    PayslipExportService.toPayPeriod(cursor));
            if (record != null) {
                LocalDate period = cursor;
                double netPaid = record.getFinalNetPay();
                double bonus = record.getPayoutAmount();
                history.add(new PayslipSummary() {
                    @Override
                    public LocalDate getPayPeriodStart() {
                        return period;
                    }

                    @Override
                    public double getNetPaid() {
                        return netPaid;
                    }

                    @Override
                    public double getTotalBonus() {
                        return bonus;
                    }
                });
            }
            cursor = cursor.plusMonths(1);
        }
        return history;
    }

    private static String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString());
        return values.toArray(String[]::new);
    }
}
