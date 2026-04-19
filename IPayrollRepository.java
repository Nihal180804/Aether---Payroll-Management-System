import java.util.List;

/**
 * INTERFACE: IPayrollRepository
 * The formal contract between the Payroll Subsystem and the Database Team.
 * * GRASP: Polymorphism - Allows switching between Mock and Real DB implementations.
 * SOLID: Dependency Inversion - Payroll logic depends on this abstraction.
 */
public interface IPayrollRepository {

    /**
     * READ: Fetches the 40+ fields for a specific employee.
     */
    PayrollDataPackage fetchEmployeeData(String empID, String payPeriod);

    /**
     * READ: Gets all active employees to be processed in a batch run.
     */
    List<String> getAllActiveEmployeeIDs();

    /**
     * WRITE: Persists the calculated payroll results back to the database.
     */
    boolean savePayrollResult(String batchID, PayrollResultDTO result);

    /**
     * EXCEPTION HANDLING: Logs errors encountered during calculation.
     * Requirement #4: Handles exceptions sent out by teams.
     */
    void logProcessingError(String batchID, String empID, String errorMsg);
}