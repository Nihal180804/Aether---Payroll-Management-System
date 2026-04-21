/**
 * LEAVE INTEGRATION CONTRACT
 * Hand this .java file to the Leave Management Team.
 */
interface LeaveDataProvider {
    /**
     * Fetches all leave records for a specific month.
     * @param payPeriod e.g., "April-2026"
     */
    List<LeaveDetailsDTO> getLeaveDataForPeriod(String payPeriod);
}

/**
 * DATA TRANSFER OBJECT (DTO)
 * No setters included to ensure data integrity during payroll processing.
 */
class LeaveDetailsDTO {
    private final String empID;
    private final double lopDays; // Leave Without Pay (Loss of Pay) days

    // The Leave team uses this constructor to pack the data
    public LeaveDetailsDTO(String empID, double lopDays) {
        this.empID = empID;
        this.lopDays = lopDays;
    }

    // Your trackers use these getters to read the data
    public String getEmpID() { return empID; }
    public double getLopDays() { return lopDays; }
}