package com.payroll.system.presenter;

/** View model for one row in the payroll results table. */
public class PayrollResultViewModel {
    public final String recordID;
    public final String empID;
    public final String name;
    public final String department;
    public final String basicPay;
    public final String pf;
    public final String tds;
    public final String pt;
    public final String netPay;
    public final String status;

    public PayrollResultViewModel(String recordID, String empID, String name,
            String department, String basicPay, String pf, String tds,
            String pt, String netPay, String status) {
        this.recordID   = recordID;
        this.empID      = empID;
        this.name       = name;
        this.department = department;
        this.basicPay   = basicPay;
        this.pf         = pf;
        this.tds        = tds;
        this.pt         = pt;
        this.netPay     = netPay;
        this.status     = status;
    }
}

