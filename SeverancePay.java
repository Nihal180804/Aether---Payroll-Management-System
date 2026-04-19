package service;

import model.Employee;

public class SeverancePay {
    public double calculateSeverance(Employee emp) {
        // Example: 1 month basic pay per year of service
        return emp.getBasicPay() * emp.getYearsOfService();
    }
}
