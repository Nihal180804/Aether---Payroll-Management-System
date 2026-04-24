package com.payroll.system.util;

import com.payroll.system.model.PayrollRecord;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Centralized in-memory audit log used by the payroll workflow. */
public class AuditLogger {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Stored in memory for the current run. */
    private final List<String> logEntries = new ArrayList<>();

    /** Records a successful payroll action. */
    public void log(PayrollRecord record, String actionType, String performedBy) {
        String timestamp = LocalDateTime.now().format(FMT);
        String entry = String.format(
                "[AUDIT] %s | RecordID=%s | EmpID=%s | Action=%s | By=%s",
                timestamp, record.getRecordID(), record.getEmpID(), actionType, performedBy);
        logEntries.add(entry);
        System.out.println(entry);
    }

    /** Records a non-blocking warning. */
    public void logWarning(String empID, String message) {
        String timestamp = LocalDateTime.now().format(FMT);
        String entry = String.format(
                "[WARN] %s | EmpID=%s | %s",
                timestamp, empID, message);
        logEntries.add(entry);
        System.out.println(entry);
    }

    /** Records a blocking error that skipped an employee or batch. */
    public void logMajorError(String empID, String reason) {
        String timestamp = LocalDateTime.now().format(FMT);
        String entry = String.format(
                "[ERROR] %s | EmpID=%s | MAJOR -> %s",
                timestamp, empID, reason);
        logEntries.add(entry);
        System.err.println(entry);
    }

    /** Returns all log entries collected so far. */
    public List<String> getAllEntries() {
        return Collections.unmodifiableList(logEntries);
    }

    /** Clears all log entries. */
    public void clearLog() {
        logEntries.clear();
    }
}
