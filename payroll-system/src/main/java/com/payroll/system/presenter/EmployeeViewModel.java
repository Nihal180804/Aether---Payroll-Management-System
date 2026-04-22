package com.payroll.system.presenter;

/** View model for one employee row in the UI's employee directory. */
public class EmployeeViewModel {
    public final String empID;
    public final String name;
    public final String department;
    public final String grade;
    public final String basicPay;
    public final String country;
    public final String taxRegime;
    public final String state;
    public final String yearsService;

    public EmployeeViewModel(String empID, String name, String department,
            String grade, String basicPay, String country,
            String taxRegime, String state, String yearsService) {
        this.empID       = empID;
        this.name        = name;
        this.department  = department;
        this.grade       = grade;
        this.basicPay    = basicPay;
        this.country     = country;
        this.taxRegime   = taxRegime;
        this.state       = state;
        this.yearsService = yearsService;
    }
}

