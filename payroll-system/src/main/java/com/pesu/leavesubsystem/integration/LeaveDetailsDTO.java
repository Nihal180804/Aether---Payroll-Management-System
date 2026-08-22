package com.pesu.leavesubsystem.integration;

/**
 * Integration DTO carrying leave and overtime figures the Leave subsystem contributes to a
 * single employee's pay period.
 *
 * <p>Payroll treats any positive value here as an authoritative override of the attendance
 * table; zeroes mean "no override — use attendance of record".
 */
public class LeaveDetailsDTO {

    private final int workingDaysInMonth;
    private final int leaveWithPay;
    private final int leaveWithoutPay;
    private final double overtimeHours;

    public LeaveDetailsDTO(int workingDaysInMonth, int leaveWithPay, int leaveWithoutPay, double overtimeHours) {
        this.workingDaysInMonth = workingDaysInMonth;
        this.leaveWithPay = leaveWithPay;
        this.leaveWithoutPay = leaveWithoutPay;
        this.overtimeHours = overtimeHours;
    }

    public int getWorkingDaysInMonth() {
        return workingDaysInMonth;
    }

    public int getLeaveWithPay() {
        return leaveWithPay;
    }

    public int getLeaveWithoutPay() {
        return leaveWithoutPay;
    }

    public double getOvertimeHours() {
        return overtimeHours;
    }
}
