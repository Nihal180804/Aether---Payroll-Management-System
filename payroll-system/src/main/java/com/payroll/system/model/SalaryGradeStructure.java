package com.payroll.system.model;

/** Stores the salary band attached to an employee. */
public class SalaryGradeStructure {

    private final String gradeLevel;
    private final double basicPay;

    public SalaryGradeStructure(String gradeLevel, double basicPay) {
        this.gradeLevel = gradeLevel;
        this.basicPay = basicPay;
    }

    public String getGradeLevel() { return gradeLevel; }
    public double getBasicPay() { return basicPay; }

    /** Logs the grade summary and returns its base pay. */
    public double getgradeDetails() {
        System.out.printf("[Grade] %s -> basicPay = %.2f%n", gradeLevel, basicPay);
        return basicPay;
    }
}
