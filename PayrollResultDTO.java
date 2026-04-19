/** 
 * DTO: PayrollResultDTO
 * Contains the final values calculated by the Payroll Subsystem.
 * Maps to "Computed Payroll Outputs" in the project spreadsheet.
 */
public class PayrollResultDTO {
    public String empID;
    public String recordID;      // Unique ID for this specific pay record
    public double finalGrossPay; // Total before deductions
    public double finalNetPay;   // Take-home pay
    public double penaltyAmount; // LOP Deductions
    public double pfAmount;      // Provident Fund
    public double taxDeducted;   // Income Tax / TDS
    public double payoutAmount;  // Final amount disbursed
}