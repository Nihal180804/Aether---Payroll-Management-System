package com.payroll.system.model;

/**
 * =============================================================================
 * CLASS: Employee
 * =============================================================================
 * Represents a single employee in the payroll system.
 * This class stores identity and configuration data fetched from the DB.
 *
 * From the class diagram:
 *   - Attributes : empID, name, department
 *   - Method     : getDetails()
 *   - Relationship: Employee "Possesses" a SalaryGradeStructure (1-to-1)
 *
 * GRASP — Information Expert:
 *   Any class that needs employee identity data asks THIS class, not the DB.
 *
 * HOW TO USE:
 *   Use Employee.Builder to construct — do NOT call the constructor directly.
 *   Example:
 *     Employee e = new Employee.Builder("E001", "Arjun").basicPay(50000).build();
 * =============================================================================
 */
public class Employee {

    // ── Core identity fields (from class diagram) ─────────────────────────────
    private final String empID;
    private final String name;
    private final String department;

    // ── Salary & service fields ───────────────────────────────────────────────
    private final String gradeLevel;
    private final double basicPay;
    private final int    yearsOfService;

    // ── Attendance fields ─────────────────────────────────────────────────────
    private final int    workingDaysInMonth;
    private final int    leaveWithoutPay;      // "LOP days" — used by LossOfPayTracker
    private final double overtimeHours;

    // ── Financial fields ──────────────────────────────────────────────────────
    private final double pendingClaims;
    private final double approvedReimbursement;
    private final double insurancePremium;
    private final double declaredInvestments;  // 80C/80D declarations — used by IncomeTaxTDS

    // ── Tax & region fields ───────────────────────────────────────────────────
    private final String taxRegime;    // "OLD" or "NEW" for India; varies by country
    private final String countryCode;  // ISO code e.g. "IN", "US", "SG"
    private final String currencyCode;
    private final String stateName;    // Needed for PT slab lookup (India)
    private final String filingStatus; // "SINGLE" or "MARRIED" — used for US tax

    // ── Link to SalaryGradeStructure (diagram shows Employee "Possesses" this) ─
    private final SalaryGradeStructure salaryGrade;

    // Package-private constructor — callers must use Builder
    Employee(Builder b) {
        this.empID               = b.empID;
        this.name                = b.name;
        this.department          = b.department;
        this.gradeLevel          = b.gradeLevel;
        this.basicPay            = b.basicPay;
        this.yearsOfService      = b.yearsOfService;
        this.workingDaysInMonth  = b.workingDaysInMonth;
        this.leaveWithoutPay     = b.leaveWithoutPay;
        this.overtimeHours       = b.overtimeHours;
        this.pendingClaims       = b.pendingClaims;
        this.approvedReimbursement = b.approvedReimbursement;
        this.insurancePremium    = b.insurancePremium;
        this.declaredInvestments = b.declaredInvestments;
        this.taxRegime           = b.taxRegime;
        this.countryCode         = b.countryCode;
        this.currencyCode        = b.currencyCode;
        this.stateName           = b.stateName;
        this.filingStatus        = b.filingStatus;
        this.salaryGrade         = new SalaryGradeStructure(b.gradeLevel, b.basicPay);
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getEmpID()               { return empID; }
    public String getName()                { return name; }
    public String getDepartment()          { return department; }
    public String getGradeLevel()          { return gradeLevel; }
    public double getBasicPay()            { return basicPay; }
    public int    getYearsOfService()      { return yearsOfService; }
    public int    getWorkingDaysInMonth()  { return workingDaysInMonth; }
    public int    getLeaveWithoutPay()     { return leaveWithoutPay; }
    public double getOvertimeHours()       { return overtimeHours; }
    public double getPendingClaims()       { return pendingClaims; }
    public double getApprovedReimbursement(){ return approvedReimbursement; }
    public double getInsurancePremium()    { return insurancePremium; }
    public double getDeclaredInvestments() { return declaredInvestments; }
    public String getTaxRegime()           { return taxRegime; }
    public String getCountryCode()         { return countryCode; }
    public String getCurrencyCode()        { return currencyCode; }
    public String getStateName()           { return stateName; }
    public String getFilingStatus()        { return filingStatus; }
    public SalaryGradeStructure getSalaryGrade() { return salaryGrade; }

    /**
     * TODO ── getDetails()
     * ─────────────────────────────────────────────────────────────────────────
     * This method appears in the class diagram as:  - getDetails() : void
     *
     * It should print (or return) a formatted summary of this employee's
     * key fields: empID, name, department, gradeLevel, basicPay.
     *
     * HINT: Use System.out.printf() with a clear format string.
     *       Example output:
     *         [Employee] E001 | Arjun Sharma | Engineering | Grade: L3 | Basic: 80000.00
     * ─────────────────────────────────────────────────────────────────────────
     */
    public void getDetails() {
        // TODO: Print employee summary using System.out.printf(...)
    }

    // ═════════════════════════════════════════════════════════════════════════
    // INNER CLASS: Builder  (Creational Pattern — Builder)
    // ═════════════════════════════════════════════════════════════════════════
    /**
     * Why Builder? Employee has 18+ fields. A constructor with 18 parameters
     * is impossible to read or call correctly. Builder gives us named fields
     * and validation before the object is committed.
     *
     * SOLID — Open/Closed: New optional fields can be added to Builder
     * without changing any existing code that constructs Employee objects.
     */
    public static class Builder {
        // Required
        private final String empID;
        private final String name;

        // Optional with defaults
        private String department          = "UNKNOWN";
        private String gradeLevel          = "L1";
        private double basicPay            = 0.0;
        private int    yearsOfService      = 0;
        private int    workingDaysInMonth  = 22;
        private int    leaveWithoutPay     = 0;
        private double overtimeHours       = 0.0;
        private double pendingClaims       = 0.0;
        private double approvedReimbursement = 0.0;
        private double insurancePremium    = 0.0;
        private double declaredInvestments = 0.0;
        private String taxRegime           = "OLD";
        private String countryCode         = "IN";
        private String currencyCode        = "INR";
        private String stateName           = "";
        private String filingStatus        = "SINGLE";

        public Builder(String empID, String name) {
            this.empID = empID;
            this.name  = name;
        }

        // Fluent setters — each returns 'this' so calls can be chained
        public Builder department(String v)           { this.department = v; return this; }
        public Builder gradeLevel(String v)           { this.gradeLevel = v; return this; }
        public Builder basicPay(double v)             { this.basicPay = v; return this; }
        public Builder yearsOfService(int v)          { this.yearsOfService = v; return this; }
        public Builder workingDaysInMonth(int v)      { this.workingDaysInMonth = v; return this; }
        public Builder leaveWithoutPay(int v)         { this.leaveWithoutPay = v; return this; }
        public Builder overtimeHours(double v)        { this.overtimeHours = v; return this; }
        public Builder pendingClaims(double v)        { this.pendingClaims = v; return this; }
        public Builder approvedReimbursement(double v){ this.approvedReimbursement = v; return this; }
        public Builder insurancePremium(double v)     { this.insurancePremium = v; return this; }
        public Builder declaredInvestments(double v)  { this.declaredInvestments = v; return this; }
        public Builder taxRegime(String v)            { this.taxRegime = v; return this; }
        public Builder countryCode(String v)          { this.countryCode = v; return this; }
        public Builder currencyCode(String v)         { this.currencyCode = v; return this; }
        public Builder stateName(String v)            { this.stateName = v; return this; }
        public Builder filingStatus(String v)         { this.filingStatus = v; return this; }

        /**
         * TODO ── build()
         * ─────────────────────────────────────────────────────────────────────
         * Before constructing the Employee, validate that basicPay > 0.
         * If it is 0 or negative, throw:
         *   new IllegalStateException("MISSING_BASE_SALARY: ...")
         *
         * HINT: if (basicPay <= 0) throw new IllegalStateException("...");
         *       return new Employee(this);
         * ─────────────────────────────────────────────────────────────────────
         */
        public Employee build() {
            if (basicPay <= 0) {
                throw new IllegalStateException("MISSING_BASE_SALARY: " + empID + " must have a positive salary.");
            }
            return new Employee(this);
        }
    }
}

