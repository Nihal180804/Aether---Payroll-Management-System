package model;

public class PayrollRecord {
    private final String employeeId;
    private final String employeeName;
    private final String payPeriod;
    private final double grossPay;
    private final double deductions;
    private final double netPay;
    private final String currencyCode;

    public PayrollRecord(String employeeId, String employeeName, String payPeriod, double grossPay, double deductions, double netPay, String currencyCode) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.payPeriod = payPeriod;
        this.grossPay = grossPay;
        this.deductions = deductions;
        this.netPay = netPay;
        this.currencyCode = currencyCode;
    }

    public String getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public String getPayPeriod() { return payPeriod; }
    public double getGrossPay() { return grossPay; }
    public double getDeductions() { return deductions; }
    public double getNetPay() { return netPay; }
    public String getCurrencyCode() { return currencyCode; }

    @Override
    public String toString() {
        return String.format("%s (%s) | Period: %s | Gross: %.2f | Deductions: %.2f | Net: %.2f %s",
                employeeName, employeeId, payPeriod, grossPay, deductions, netPay, currencyCode);
    }
}
