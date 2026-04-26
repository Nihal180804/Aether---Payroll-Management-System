package com.payroll.system.model;

/** Immutable employee snapshot used during payroll calculations. */
public class Employee {

    private final String empID;
    private final String name;
    private final String department;
    private final Integer roleId;
    private final String gradeLevel;
    private final double basicPay;
    private final int yearsOfService;
    private final int workingDaysInMonth;
    private final int leaveWithoutPay;
    private final double overtimeHours;
    private final double pendingClaims;
    private final double approvedReimbursement;
    private final double insurancePremium;
    private final double declaredInvestments;
    private final String taxRegime;
    private final String countryCode;
    private final String currencyCode;
    private final String stateName;
    private final String filingStatus;
    private final SalaryGradeStructure salaryGrade;

    Employee(Builder b) {
        this.empID = b.empID;
        this.name = b.name;
        this.department = b.department;
        this.roleId = b.roleId;
        this.gradeLevel = b.gradeLevel;
        this.basicPay = b.basicPay;
        this.yearsOfService = b.yearsOfService;
        this.workingDaysInMonth = b.workingDaysInMonth;
        this.leaveWithoutPay = b.leaveWithoutPay;
        this.overtimeHours = b.overtimeHours;
        this.pendingClaims = b.pendingClaims;
        this.approvedReimbursement = b.approvedReimbursement;
        this.insurancePremium = b.insurancePremium;
        this.declaredInvestments = b.declaredInvestments;
        this.taxRegime = b.taxRegime;
        this.countryCode = b.countryCode;
        this.currencyCode = b.currencyCode;
        this.stateName = b.stateName;
        this.filingStatus = b.filingStatus;
        this.salaryGrade = new SalaryGradeStructure(b.gradeLevel, b.basicPay);
    }

    public String getEmpID() { return empID; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
    public Integer getRoleId() { return roleId; }
    public String getGradeLevel() { return gradeLevel; }
    public double getBasicPay() { return basicPay; }
    public int getYearsOfService() { return yearsOfService; }
    public int getWorkingDaysInMonth() { return workingDaysInMonth; }
    public int getLeaveWithoutPay() { return leaveWithoutPay; }
    public double getOvertimeHours() { return overtimeHours; }
    public double getPendingClaims() { return pendingClaims; }
    public double getApprovedReimbursement() { return approvedReimbursement; }
    public double getInsurancePremium() { return insurancePremium; }
    public double getDeclaredInvestments() { return declaredInvestments; }
    public String getTaxRegime() { return taxRegime; }
    public String getCountryCode() { return countryCode; }
    public String getCurrencyCode() { return currencyCode; }
    public String getStateName() { return stateName; }
    public String getFilingStatus() { return filingStatus; }
    public SalaryGradeStructure getSalaryGrade() { return salaryGrade; }

    /** Prints a short summary for debugging and demo output. */
    public void getDetails() {
        System.out.printf(
                "[Employee] %s | %s | %s | Grade: %s | Basic: %.2f%n",
                empID, name, department, gradeLevel, basicPay);
    }

    /** Builder keeps employee construction readable despite the large field set. */
    public static class Builder {
        private final String empID;
        private final String name;

        private String department = "UNKNOWN";
        private Integer roleId = null;
        private String gradeLevel = "L1";
        private double basicPay = 0.0;
        private int yearsOfService = 0;
        private int workingDaysInMonth = 22;
        private int leaveWithoutPay = 0;
        private double overtimeHours = 0.0;
        private double pendingClaims = 0.0;
        private double approvedReimbursement = 0.0;
        private double insurancePremium = 0.0;
        private double declaredInvestments = 0.0;
        private String taxRegime = "OLD";
        private String countryCode = "IN";
        private String currencyCode = "INR";
        private String stateName = "";
        private String filingStatus = "SINGLE";

        public Builder(String empID, String name) {
            this.empID = empID;
            this.name = name;
        }

        public Builder department(String v) { this.department = v; return this; }
        public Builder roleId(Integer v) { this.roleId = v; return this; }
        public Builder gradeLevel(String v) { this.gradeLevel = v; return this; }
        public Builder basicPay(double v) { this.basicPay = v; return this; }
        public Builder yearsOfService(int v) { this.yearsOfService = v; return this; }
        public Builder workingDaysInMonth(int v) { this.workingDaysInMonth = v; return this; }
        public Builder leaveWithoutPay(int v) { this.leaveWithoutPay = v; return this; }
        public Builder overtimeHours(double v) { this.overtimeHours = v; return this; }
        public Builder pendingClaims(double v) { this.pendingClaims = v; return this; }
        public Builder approvedReimbursement(double v) { this.approvedReimbursement = v; return this; }
        public Builder insurancePremium(double v) { this.insurancePremium = v; return this; }
        public Builder declaredInvestments(double v) { this.declaredInvestments = v; return this; }
        public Builder taxRegime(String v) { this.taxRegime = v; return this; }
        public Builder countryCode(String v) { this.countryCode = v; return this; }
        public Builder currencyCode(String v) { this.currencyCode = v; return this; }
        public Builder stateName(String v) { this.stateName = v; return this; }
        public Builder filingStatus(String v) { this.filingStatus = v; return this; }

        /** Rejects incomplete employees before they enter payroll processing. */
        public Employee build() {
            if (basicPay <= 0) {
                throw new IllegalStateException("MISSING_BASE_SALARY: " + empID + " must have a positive salary.");
            }
            return new Employee(this);
        }
    }
}
