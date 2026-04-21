import com.hrms.db.facade.HRMSDatabaseFacade;
import com.hrms.db.factory.RepositoryFactory;
import com.hrms.db.repositories.payroll.IPayrollRepository;
import java.util.List;

public class TestConnection {
    public static void main(String[] args) {
        try {
            System.out.println(">>> Starting Aether-DBMS Integration Test <<<");

            // 1. Get the Singleton Facade and Boot Hibernate
            HRMSDatabaseFacade dbFacade = HRMSDatabaseFacade.getInstance();
            dbFacade.initialize();

            // 2. Get the Factory from the Facade
            RepositoryFactory factory = dbFacade.getRepositories();

            // 3. Request the Payroll Repository
            IPayrollRepository payrollRepo = factory.getPayrollRepository();

            // 4. Test Connectivity
            System.out.println("Attempting to reach HRMDB SQLite file...");
            List<String> activeIds = payrollRepo.getAllActiveEmployeeIDs();

            if (activeIds != null && !activeIds.isEmpty()) {
                System.out.println("[SUCCESS] Database is alive. Found " + activeIds.size() + " employees.");
                
                // 5. Test Data Package Retrieval
                String testEmp = activeIds.get(0);
                var data = payrollRepo.fetchEmployeeData(testEmp, "2026-04");
                
                if (data != null && data.employee != null) {
                    // Corrected 'department' name based on your DTO file
                    System.out.println("Fetched: " + data.employee.name + " [" + data.employee.department + "]");
                }
            } else {
                System.out.println("[WARNING] Connection successful, but the hrms.db file appears empty.");
            }

            // 6. Graceful Shutdown
            dbFacade.shutdown();

        } catch (Exception e) {
            System.err.println("[CRITICAL] Integration Failed.");
            e.printStackTrace();
        }
    } // Closes main method
} // Closes class