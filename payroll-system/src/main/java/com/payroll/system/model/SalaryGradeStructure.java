package com.payroll.system.model;

/**
 * =============================================================================
 * CLASS: SalaryGradeStructure
 * =============================================================================
 * From the class diagram:
 *   Attributes : GradeLevel (String), basicPay (Double)
 *   Method     : getgradeDetails() : Double
 *   Relationship: Employee "Possesses" one SalaryGradeStructure
 *                 PayrollRecord includes one SalaryGradeStructure
 *
 * This class represents the salary band / grade configuration for an employee.
 * It is used by StatuaryDeduction (PF is calculated on basicPay from here)
 * and by BonusDistributor (bonus % depends on grade level).
 * =============================================================================
 */
public class SalaryGradeStructure {

    private final String gradeLevel; // e.g. "L1", "L2", "L3", "L4", "L5"
    private final double basicPay;   // Monthly basic salary for this grade

    public SalaryGradeStructure(String gradeLevel, double basicPay) {
        this.gradeLevel = gradeLevel;
        this.basicPay   = basicPay;
    }

    public String getGradeLevel() { return gradeLevel; }
    public double getBasicPay()   { return basicPay; }

    /**
     * TODO ── getgradeDetails()
     * ─────────────────────────────────────────────────────────────────────────
     * From the class diagram: +getgradeDetails() : Double
     *
     * Should return the basicPay for this grade (the "Double" in the diagram).
     * Also print a summary line so it's visible in logs.
     *
     * HINT: Print something like:
     *   [Grade] L3 → basicPay = 80000.00
     * Then return basicPay.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public double getgradeDetails() {
        // TODO: Print grade summary and return basicPay
        return 0.0; // REMOVE this line once implemented
    }
}

