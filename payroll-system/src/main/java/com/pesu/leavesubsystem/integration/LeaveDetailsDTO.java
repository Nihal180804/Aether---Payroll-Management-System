package com.pesu.leavesubsystem.integration;

public class LeaveDetailsDTO {
    private String employeeId;
    private String payPeriod;
    private int leaveWithPay;
    private int leaveWithoutPay;
    private int workingDaysInMonth;
    private double overtimeHours;

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getPayPeriod() {
        return payPeriod;
    }

    public void setPayPeriod(String payPeriod) {
        this.payPeriod = payPeriod;
    }

    public int getLeaveWithPay() {
        return leaveWithPay;
    }

    public void setLeaveWithPay(int leaveWithPay) {
        this.leaveWithPay = leaveWithPay;
    }

    public int getLeaveWithoutPay() {
        return leaveWithoutPay;
    }

    public void setLeaveWithoutPay(int leaveWithoutPay) {
        this.leaveWithoutPay = leaveWithoutPay;
    }

    public int getWorkingDaysInMonth() {
        return workingDaysInMonth;
    }

    public void setWorkingDaysInMonth(int workingDaysInMonth) {
        this.workingDaysInMonth = workingDaysInMonth;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }

    public void setOvertimeHours(double overtimeHours) {
        this.overtimeHours = overtimeHours;
    }
}
