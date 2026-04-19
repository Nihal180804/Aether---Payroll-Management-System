package service;

import model.Employee;

public class LossOfPayTracker {
    public double calculateLossOfPay(Employee emp) {
        // Simple calculation: (leaveWithoutPay / workingDaysInMonth) * basicPay
        if (emp.getWorkingDaysInMonth() == 0) return 0.0;
        return (emp.getLeaveWithoutPay() / (double) emp.getWorkingDaysInMonth()) * emp.getBasicPay();
    }
}
