package com.hrms.db.repositories.payroll;
public interface IPayrollRepository {
    boolean savePayrollResult(String batchID, PayrollResultDTO result);
    void logProcessingError(String batchID, String empID, String errorMsg);
    java.util.List<String> getAllActiveEmployeeIDs();
    PayrollDataPackage fetchEmployeeData(String empID, String payPeriod);
}
