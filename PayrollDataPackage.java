/**
 * Master Container for all data required to start calculations.
 */
public class PayrollDataPackage {
    public EmployeeDTO employee;
    public AttendanceDTO attendance;
    public FinancialsDTO financials;
    public TaxContextDTO tax;
    public String payPeriod;
}

// Group 1: Basic Info & Salary Grade
class EmployeeDTO {
    public String empID;
    public String name;
    public String department;
    public String gradeLevel;
    public double basicPay;
    public int yearsOfService;
}

// Group 2: Attendance & Leave (Used by LossOfPayTracker)
class AttendanceDTO {
    public int workingDaysInMonth;
    public int leaveWithPay;
    public int leaveWithoutPay; // "lopDays"
    public double hoursWorked;
    public double overtimeHours;
}

// Group 3: Claims & Investments (Used by Reimbursement/Bonus)
class FinancialsDTO {
    public double pendingClaims;
    public double approvedReimbursement;
    public double insurancePremium;
    public double declaredInvestments;
}

// Group 4: Region & Tax (Used by IncomeTaxTDS)
class TaxContextDTO {
    public String countryCode;
    public String currencyCode;
    public String taxRegime;
    public String stateName;
    public String filingStatus;
    public String taxCode;
    public String nationalIDNumber;
}