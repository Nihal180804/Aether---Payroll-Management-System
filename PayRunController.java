import model.Employee;
import model.PayrollRecord;
import pattern.PayrollFacade;
import util.AuditLogger;
import exception.PayrollException;

import java.util.*;

public class PayRunController {
    private final String batchId;
    private final String payPeriod;
    private final PayrollFacade facade;
    private final AuditLogger auditLogger;
    private boolean batchExecuted;
    private List<PayrollRecord> lastRecords;

    public PayRunController(String batchId, String payPeriod, PayrollFacade facade, AuditLogger auditLogger) {
        this.batchId = batchId;
        this.payPeriod = payPeriod;
        this.facade = facade;
        this.auditLogger = auditLogger;
        this.batchExecuted = false;
        this.lastRecords = new ArrayList<>();
    }

    public List<PayrollRecord> executeBatchPayroll(List<Employee> employees) throws PayrollException.InvalidPayPeriod {
        if (batchExecuted) {
            throw new PayrollException.InvalidPayPeriod("Payroll already run for period: " + payPeriod);
        }
        auditLogger.log("[BATCH START] Batch: " + batchId + ", Period: " + payPeriod);
        List<PayrollRecord> records = new ArrayList<>();
        for (Employee emp : employees) {
            try {
                PayrollRecord rec = facade.processEmployee(emp, payPeriod);
                records.add(rec);
                auditLogger.log("[SUCCESS] Payroll processed for: " + emp.getEmployeeId());
            } catch (PayrollException.Major e) {
                auditLogger.log("[SKIP] " + emp.getEmployeeId() + ": " + e.getExceptionName() + " - " + e.getMessage());
            } catch (PayrollException.Minor e) {
                auditLogger.log("[WARN] " + emp.getEmployeeId() + ": " + e.getExceptionName() + " - " + e.getMessage());
                PayrollRecord rec = facade.processEmployeeWithWarning(emp, payPeriod, e);
                records.add(rec);
            }
        }
        batchExecuted = true;
        lastRecords = records;
        auditLogger.log("[BATCH END] Batch: " + batchId);
        return records;
    }

    public void verifyCalculations() {
        auditLogger.log("[VERIFY] Verifying payroll calculations for batch: " + batchId);
        // Placeholder for actual verification logic
    }
}
