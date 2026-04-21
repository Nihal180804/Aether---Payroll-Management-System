package com.payroll.system.util;

import com.payroll.system.model.PayrollRecord;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * =============================================================================
 * CLASS: AuditLog (from class diagram)
 * =============================================================================
 * From class diagram:
 * Attributes : actionType (String), TimeStamp (Date), performedBy (String)
 * Method : logTransaction() : void
 * Relationships:
 * - PayRunController "Writes to" AuditLog
 * - ComplianceAndReporting "References" AuditLog
 * - AuditLog "Includes" IncomeTaxTDS
 *
 * GRASP — Pure Fabrication:
 * AuditLogger doesn't represent a real-world domain object.
 * It exists purely to centralise logging so services don't each
 * implement their own logging logic (eliminates duplication).
 *
 * Shared utility — used by all three team members.
 * =============================================================================
 */
public class AuditLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // In-memory store — in production, rows would be inserted into the AuditLog
    // table
    private final List<String> logEntries = new ArrayList<>();

    /**
     * TODO ── log() (maps to logTransaction() in the class diagram)
     * ─────────────────────────────────────────────────────────────────────────
     * Logs a successful payroll action.
     *
     * HINT:
     * 1. Build a formatted string:
     * "[AUDIT] 2025-06-01 10:30:00 | RecordID=... | EmpID=... | Action=... |
     * By=..."
     * 2. Add it to logEntries list
     * 3. Print it: System.out.println(entry)
     *
     * Use: LocalDateTime.now().format(FMT) for the timestamp.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public void log(PayrollRecord record, String actionType, String performedBy) {
        String timestamp = LocalDateTime.now().format(FMT);
        String entry = String.format(
                "[AUDIT] %s | RecordID=%s | EmpID=%s | Action=%s | By=%s",
                timestamp, record.getRecordID(), record.getEmpID(), actionType, performedBy);
        logEntries.add(entry);
        System.out.println(entry);
    }

    /**
     * TODO ── logWarning()
     * ─────────────────────────────────────────────────────────────────────────
     * Logs a WARNING or MINOR exception (non-halting).
     *
     * HINT: Format:
     * "[WARN] 2025-06-01 10:30:00 | EmpID=E001 | message"
     * ─────────────────────────────────────────────────────────────────────────
     */
    public void logWarning(String empID, String message) {
        String timestamp = LocalDateTime.now().format(FMT);
        String entry = String.format(
                "[WARN] %s | EmpID=%s | %s",
                timestamp, empID, message);
        logEntries.add(entry);
        System.out.println(entry);
    }

    /**
     * TODO ── logMajorError()
     * ─────────────────────────────────────────────────────────────────────────
     * Logs a MAJOR error (employee was skipped due to this error).
     *
     * HINT: Format:
     * "[ERROR] 2025-06-01 10:30:00 | EmpID=E001 | MAJOR → reason"
     * Print to System.err (not System.out) so it stands out.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public void logMajorError(String empID, String reason) {
        String timestamp = LocalDateTime.now().format(FMT);
        String entry = String.format(
                "[ERROR] %s | EmpID=%s | MAJOR → %s",
                timestamp, empID, reason);
        logEntries.add(entry);
        System.err.println(entry);
    }

    /**
     * Returns all log entries collected so far.
     * Used by ComplianceAndReporting to include the audit trail in reports.
     */
    public List<String> getAllEntries() {
        return Collections.unmodifiableList(logEntries);
    }

    /**
     * Clears all log entries.
     */
    public void clearLog() {
        logEntries.clear();
    }
}

