import com.pesu.expensesubsystem.integration.*; // Import the team's package
import java.util.List;

public class TestExpenseIntegration {
    public static void main(String[] args) {
        System.out.println("=== Aether System: Expense Integration Test ===");
        
        try {
            // 1. Initialize the implementation they provided
            // Note: This calls their constructor which links to their Mock repository.
            ExpenseDataProvider provider = new ExpenseDataProviderImpl();

            // 2. Call their method to fetch the DTOs
            System.out.println("[TEST] Calling getApprovedClaimsForPayroll()...");
            List<ApprovedClaimDTO> claims = provider.getApprovedClaimsForPayroll();

            // 3. Verify and Print the data
            if (claims == null) {
                System.err.println("[FAIL] The provider returned a null list.");
            } else if (claims.isEmpty()) {
                System.out.println("[INFO] The provider returned an empty list. (Mock might be empty)");
            } else {
                System.out.println("[SUCCESS] Found " + claims.size() + " approved claims:");
                for (ApprovedClaimDTO claim : claims) {
                    // Using the specific getter methods found in their .class file
                    System.out.printf(" >> Employee: %-10s | Amount: %8.2f | ClaimID: %s | Date: %s%n",
                        claim.getEmployeeId(),
                        claim.getAmount().doubleValue(),
                        claim.getClaimId(),
                        claim.getApprovalDate());
                }
            }
            
        } catch (NoClassDefFoundError e) {
            // This happens if you are missing their internal dependencies (like ClaimRepository)
            System.err.println("[CRITICAL] Missing .class files! Ensure the full directory structure is present.");
            System.err.println("Technical detail: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("[ERROR] An unexpected error occurred during integration:");
            e.printStackTrace();
        }
        
        System.out.println("===============================================");
    }
}