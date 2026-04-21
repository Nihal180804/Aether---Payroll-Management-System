import com.hrms.db.facade.HRMSDatabaseFacade;
import com.hrms.db.config.DatabaseConnection;
import com.hrms.db.entities.Employee;
import com.hrms.db.entities.LeaveRecord;
import com.hrms.db.entities.Financial; // Corrected to Financial
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.time.LocalDate;
import java.util.Random;
import java.util.UUID;

public class AetherSeeder {
    public static void main(String[] args) {
        HRMSDatabaseFacade.getInstance().initialize();
        Random rand = new Random();
        String currentPayPeriod = "2026-04";

        try (Session session = DatabaseConnection.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            System.out.println(">>> Seeding Employees + Financial Records...");

            for (int i = 1; i <= 15; i++) {
                String id = "AETHER_EMP_" + String.format("%03d", i);
                
                // 1. Create Employee
                Employee emp = new Employee();
                emp.setEmpId(id);
                emp.setName("Aether_User_" + i);
                emp.setDepartment(i % 2 == 0 ? "Engineering" : "Finance");
                emp.setDesignation(i % 2 == 0 ? "Lead Developer" : "Accountant");
                emp.setRole("USER");
                emp.setDateOfJoining(LocalDate.now().minusYears(rand.nextInt(5) + 1));
                session.persist(emp); 

                // 2. Create Financial Record (Mapped to your Financial.java)
                Financial fin = new Financial();
                fin.setRecordId(UUID.randomUUID().toString()); // Required: length 36
                fin.setEmployee(emp);
                fin.setPayPeriod(currentPayPeriod); // Required
                
                // Seeding randomized values for your trackers
                fin.setPendingClaims(500.0 + (rand.nextDouble() * 2000.0));
                fin.setApprovedReimbursement(200.0 + (rand.nextDouble() * 1000.0));
                fin.setInsurancePremium(1500.0);
                fin.setDeclaredInvestments(50000.0 + (rand.nextInt(100000)));
                
                session.persist(fin);

                // 3. Create Leave Record (For LOP Testing)
                if (i % 5 == 0) {
                    LeaveRecord leave = new LeaveRecord();
                    leave.setLeaveId("LVE_" + id);
                    leave.setEmployee(emp);
                    leave.setStartDate(LocalDate.now().minusDays(5));
                    leave.setEndDate(LocalDate.now().minusDays(3));
                    leave.setLeaveType("UNPAID");
                    leave.setStatus("APPROVED");
                    session.persist(leave); 
                }
                
                System.out.println("  [+] Inserted " + id + " with Financial Record for " + currentPayPeriod);
            }

            tx.commit();
            System.out.println(">>> SEEDING COMPLETE: 15 Employees with deep data profiles.");

        } catch (Exception e) {
            System.err.println("[ERROR] Seeding failed. Check if the 'Financial' entity is properly mapped.");
            e.printStackTrace();
        } finally {
            HRMSDatabaseFacade.getInstance().shutdown();
        }
    }
}