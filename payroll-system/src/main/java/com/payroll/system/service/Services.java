package com.payroll.system.service;

import com.hrms.succession.dto.SuccessionBonusDTO;
import com.hrms.succession.facade.SuccessionBonusFacade;
import com.pesu.expensesubsystem.integration.ApprovedClaimDTO;
import com.pesu.expensesubsystem.integration.ExpenseDataProvider;
import com.pesu.leavesubsystem.integration.LeaveDataProviderImpl;
import com.pesu.leavesubsystem.integration.LeaveDetailsDTO;
import com.payroll.system.exception.PayrollException;
import com.payroll.system.model.Employee;
import com.payroll.system.model.PayrollRecord;
import com.payroll.system.pattern.TaxStrategy;
import com.payroll.system.pattern.TaxStrategyFactory;
import com.payroll.system.util.AuditLogger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Services {
}

/** Computes loss-of-pay deductions and overtime pay from attendance data. */
class LossOfPayTracker {

    private static final double OVERTIME_MULTIPLIER = 1.5;
    private static final double STD_HOURS_PER_DAY = 8.0;
    private final LeaveDataProviderImpl leaveDataProvider;

    LossOfPayTracker() {
        this(new LeaveDataProviderImpl());
    }

    LossOfPayTracker(LeaveDataProviderImpl leaveDataProvider) {
        this.leaveDataProvider = leaveDataProvider;
    }

    public double calculateLopDeduction(Employee emp) {
        return calculateLopDeduction(emp, null);
    }

    public double calculateLopDeduction(Employee emp, PayrollRecord record) {
        LeaveDetailsDTO leaveDetails = resolveLeaveDetails(emp, record);
        int workingDaysInMonth = leaveDetails != null && leaveDetails.getWorkingDaysInMonth() > 0
                ? leaveDetails.getWorkingDaysInMonth()
                : emp.getWorkingDaysInMonth();
        int leaveWithoutPay = leaveDetails != null && leaveDetails.getLeaveWithoutPay() > 0
                ? leaveDetails.getLeaveWithoutPay()
                : emp.getLeaveWithoutPay();

        if (workingDaysInMonth <= 0 || leaveWithoutPay <= 0) {
            return 0.0;
        }
        double dailyRate = emp.getBasicPay() / workingDaysInMonth;
        return dailyRate * leaveWithoutPay;
    }

    public double calculateOvertimePay(Employee emp) {
        return calculateOvertimePay(emp, null);
    }

    public double calculateOvertimePay(Employee emp, PayrollRecord record) {
        LeaveDetailsDTO leaveDetails = resolveLeaveDetails(emp, record);
        int workingDaysInMonth = leaveDetails != null && leaveDetails.getWorkingDaysInMonth() > 0
                ? leaveDetails.getWorkingDaysInMonth()
                : emp.getWorkingDaysInMonth();
        double overtimeHours = leaveDetails != null && leaveDetails.getOvertimeHours() > 0
                ? leaveDetails.getOvertimeHours()
                : emp.getOvertimeHours();

        if (overtimeHours <= 0 || workingDaysInMonth <= 0) {
            return 0.0;
        }
        double hourlyRate = emp.getBasicPay() / (workingDaysInMonth * STD_HOURS_PER_DAY);
        return hourlyRate * OVERTIME_MULTIPLIER * overtimeHours;
    }

    private LeaveDetailsDTO resolveLeaveDetails(Employee emp, PayrollRecord record) {
        if (record == null || record.getPayPeriod() == null || record.getPayPeriod().isBlank()) {
            return null;
        }
        return leaveDataProvider.getLeaveDetailsForPayroll(
                emp.getEmpID(),
                record.getPayPeriod(),
                emp.getWorkingDaysInMonth());
    }
}

/** Computes bonus payouts and succession-related adjustments. */
class BonusDistributor {

    private static final int TEMP_ROLE_ID = 101;
    private static final Map<String, Double> BONUS_RATE = new HashMap<>();

    static {
        BONUS_RATE.put("L1", 0.05);
        BONUS_RATE.put("L2", 0.08);
        BONUS_RATE.put("L3", 0.12);
        BONUS_RATE.put("L4", 0.15);
        BONUS_RATE.put("L5", 0.20);
    }

    private final AuditLogger auditLogger;
    private final SuccessionBonusFacade successionFacade;

    BonusDistributor(AuditLogger auditLogger, SuccessionBonusFacade successionFacade) {
        this.auditLogger = auditLogger;
        this.successionFacade = successionFacade;
    }

    public double calculateBonus(Employee emp, PayrollRecord record) {
        Double rate = BONUS_RATE.get(emp.getGradeLevel());
        if (rate == null) {
            PayrollException.MissingPerformanceRating warning =
                    new PayrollException.MissingPerformanceRating(emp.getEmpID());
            auditLogger.logWarning(emp.getEmpID(), warning.getMessage());
            record.setBonusArrears(emp.getBasicPay() * 0.05);
            return 0.0;
        }

        double baseBonus = emp.getBasicPay() * rate;
        double criticalityBonus = 0.0;
        double retentionRiskBonus = 0.0;
        double potentialReward = 0.0;
        double futureLeaderIncentive = 0.0;

        Integer roleId = emp.getRoleId() != null ? emp.getRoleId() : TEMP_ROLE_ID;
        SuccessionBonusDTO metrics = successionFacade.getBonusInputs(emp.getEmpID(), roleId);
        if (metrics != null) {
            if (isCriticalRole(metrics.getCriticality())) {
                criticalityBonus = baseBonus * 0.10;
            }
            if (metrics.getRiskLevel() != null && metrics.getRiskLevel().equalsIgnoreCase("High")) {
                retentionRiskBonus = baseBonus * 0.15;
            }
            if (metrics.getAppraisalScore() > 90 && metrics.getReadinessScore() > 80) {
                potentialReward = 2500.0;
            }
            if (metrics.getSuccessorRank() == 1) {
                futureLeaderIncentive = 5000.0;
            }
        }

        double finalBonus = baseBonus + criticalityBonus + retentionRiskBonus + potentialReward + futureLeaderIncentive;
        auditLogger.log(
                record,
                String.format(
                        "BONUS_BREAKDOWN base=%.2f strategic=%.2f total=%.2f [criticality=%.2f, risk=%.2f, potential=%.2f, successor=%.2f]",
                        baseBonus,
                        criticalityBonus + retentionRiskBonus + potentialReward + futureLeaderIncentive,
                        finalBonus,
                        criticalityBonus,
                        retentionRiskBonus,
                        potentialReward,
                        futureLeaderIncentive),
                "BonusDistributor");
        return finalBonus;
    }

    private boolean isCriticalRole(String criticality) {
        return criticality != null
                && (criticality.equalsIgnoreCase("Critical") || criticality.equalsIgnoreCase("High"));
    }
}

/** Validates and computes the reimbursement payout for an employee. */
class ReimbursementTracker {

    private static final Map<String, Double> GRADE_LIMITS = new HashMap<>();

    static {
        GRADE_LIMITS.put("L1", 5_000.0);
        GRADE_LIMITS.put("L2", 10_000.0);
        GRADE_LIMITS.put("L3", 20_000.0);
        GRADE_LIMITS.put("L4", 35_000.0);
        GRADE_LIMITS.put("L5", 50_000.0);
    }

    private final AuditLogger auditLogger;
    private final Set<String> processedClaimIDs = new HashSet<>();
    private final ExpenseDataProvider expenseProvider;

    ReimbursementTracker(AuditLogger auditLogger, ExpenseDataProvider provider) {
        this.auditLogger = auditLogger;
        this.expenseProvider = provider;
    }

    public double getApprovedReimbursement(Employee emp, PayrollRecord record) {
        String claimID = emp.getEmpID() + "-" + record.getPayPeriod();
        if (processedClaimIDs.contains(claimID)) {
            PayrollException.DuplicateClaimId ex =
                    new PayrollException.DuplicateClaimId(emp.getEmpID(), claimID);
            auditLogger.logWarning(emp.getEmpID(), ex.getMessage());
            return 0.0;
        }

        double amountFound = 0.0;
        List<ApprovedClaimDTO> claims = expenseProvider.getApprovedClaimsForPayroll();
        for (ApprovedClaimDTO dto : claims) {
            if (emp.getEmpID().equals(dto.getEmployeeId())) {
                amountFound = dto.getAmount() == null ? 0.0 : dto.getAmount().doubleValue();
                break;
            }
        }

        double maxLimit = GRADE_LIMITS.getOrDefault(emp.getGradeLevel(), 5000.0);
        if (amountFound > maxLimit) {
            PayrollException.ExceedsClaimLimit warning =
                    new PayrollException.ExceedsClaimLimit(emp.getEmpID(), amountFound, maxLimit);
            auditLogger.logWarning(emp.getEmpID(), warning.getMessage());
            amountFound = maxLimit;
        }

        processedClaimIDs.add(claimID);
        return amountFound;
    }
}

/** Computes gratuity and final-settlement amounts. */
class SeverancePay {

    private static final int MIN_YEARS = 5;
    private static final double DIVISOR = 26.0;
    private static final double DAYS_PER_YEAR = 15.0;
    private static final double MAX_GRATUITY = 2_000_000.0;

    public double calculateGratuity(Employee emp) {
        if (emp.getYearsOfService() < MIN_YEARS) {
            return 0.0;
        }
        double total = (emp.getBasicPay() / DIVISOR) * DAYS_PER_YEAR * emp.getYearsOfService();
        total = Math.min(total, MAX_GRATUITY);
        return total / (emp.getYearsOfService() * 12);
    }

    public double calculateFinalSettlement(Employee emp, double noticePeriodPay) {
        double fullGratuity = 0.0;
        if (emp.getYearsOfService() >= MIN_YEARS) {
            fullGratuity = (emp.getBasicPay() / DIVISOR) * DAYS_PER_YEAR * emp.getYearsOfService();
            fullGratuity = Math.min(fullGratuity, MAX_GRATUITY);
        }
        return noticePeriodPay + fullGratuity + emp.getApprovedReimbursement();
    }
}

/** Computes PF and PT deductions. */
class StatuaryDeduction {

    private static final double PF_RATE = 0.12;
    private static final Map<String, Double> PT_BY_STATE = new HashMap<>();

    static {
        PT_BY_STATE.put("KARNATAKA", 200.0);
        PT_BY_STATE.put("MAHARASHTRA", 200.0);
        PT_BY_STATE.put("WEST_BENGAL", 150.0);
        PT_BY_STATE.put("TAMIL_NADU", 100.0);
        PT_BY_STATE.put("ANDHRA_PRADESH", 150.0);
        PT_BY_STATE.put("TELANGANA", 150.0);
    }

    public double calculatePF(Employee emp) {
        double pf = emp.getBasicPay() * PF_RATE;
        System.out.printf("[PF] EmpID=%s | Rs%.2f x 12%% = Rs%.2f%n",
                emp.getEmpID(), emp.getBasicPay(), pf);
        return pf;
    }

    public double calculatePT(Employee emp) throws PayrollException.MissingWorkState {
        if (emp.getStateName() == null || emp.getStateName().isBlank()) {
            throw new PayrollException.MissingWorkState(emp.getEmpID());
        }

        Double pt = PT_BY_STATE.get(emp.getStateName().toUpperCase(Locale.ROOT));
        return pt == null ? 0.0 : pt;
    }
}

/** Computes monthly TDS using the configured tax strategy. */
class IncomeTaxTDS {

    private final AuditLogger auditLogger;

    IncomeTaxTDS(AuditLogger auditLogger) {
        this.auditLogger = auditLogger;
    }

    public double calculateTDS(Employee emp, double annualGrossIncome) {
        String regime = emp.getTaxRegime();
        if (regime == null || regime.isBlank()) {
            PayrollException.MissingTaxRegime warning =
                    new PayrollException.MissingTaxRegime(emp.getEmpID());
            auditLogger.logWarning(emp.getEmpID(), warning.getMessage());
            triggerTaxRegimeReminderEmail(emp);
            regime = "OLD";
        }

        TaxStrategy strategy = TaxStrategyFactory.get(emp.getCountryCode(), regime);
        double tds = strategy.calculateMonthlyTax(emp, annualGrossIncome);
        double result = Math.max(0.0, tds);
        System.out.printf("[TDS] EmpID=%s | Country=%s | Regime=%s | Annual=%.2f -> Monthly TDS=Rs%.2f%n",
                emp.getEmpID(), emp.getCountryCode(), regime, annualGrossIncome, result);
        return result;
    }

    /** Keeps reminder delivery separate from the tax calculation flow. */
    private void triggerTaxRegimeReminderEmail(Employee emp) {
        System.out.printf("[EMAIL] Reminder -> Employee %s: Please declare your tax regime.%n",
                emp.getEmpID());
    }
}

/** Generates the payslip file and simulates email delivery. */
class DigitalPayslipGenerator {

    private final String outputDirectory;

    DigitalPayslipGenerator(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String generatePDF(Employee emp, PayrollRecord record)
            throws PayrollException.PayslipGenerationFailed {
        String fileName = "payslip_" + sanitize(emp.getEmpID()) + "_" + sanitize(record.getPayPeriod()) + ".txt";
        Path outputDir = Path.of(outputDirectory);
        Path filePath = outputDir.resolve(fileName);

        try {
            Files.createDirectories(outputDir);
            writePayslipFile(filePath, emp, record);
            distributedViaEmail(emp.getEmpID(), filePath.toAbsolutePath().toString());
            return filePath.toAbsolutePath().toString();
        } catch (IOException e) {
            throw new PayrollException.PayslipGenerationFailed(emp.getEmpID());
        }
    }

    public void distributedViaEmail(String empId, String filePath) {
        System.out.printf("[EMAIL] Payslip sent to %s -> %s%n", empId, filePath);
    }

    private void writePayslipFile(Path filePath, Employee emp, PayrollRecord record) throws IOException {
        Files.writeString(filePath, generatePayslipText(emp, record), StandardCharsets.UTF_8);
        System.out.printf(
                "[PDF] Writing payslip -> %s | Gross: %.2f | Net: %.2f | TDS: %.2f | PF: %.2f%n",
                filePath.toAbsolutePath(),
                record.getFinalGrossPay(),
                record.getFinalNetPay(),
                record.getMonthlyTdsAmount(),
                record.getPfAmount());
    }

    public String generatePayslipText(Employee emp, PayrollRecord record) {
        return """
                ==================================================
                         AETHER PAYROLL SOLUTIONS
                ==================================================
                Employee ID      : %s
                Employee Name    : %s
                Department       : %s
                Pay Period       : %s
                Record ID        : %s
                Batch ID         : %s
                --------------------------------------------------
                Basic Pay        : INR %s
                Overtime Pay     : INR %s
                Bonus Payout     : INR %s
                Reimbursement    : INR %s
                Gratuity         : INR %s
                --------------------------------------------------
                PF Deduction     : INR %s
                PT Deduction     : INR %s
                TDS Deduction    : INR %s
                LOP Penalty      : INR %s
                --------------------------------------------------
                Gross Pay        : INR %s
                Net Pay          : INR %s
                Bonus Arrears    : INR %s
                Payslip Status   : %s
                ==================================================
                """.formatted(
                emp.getEmpID(),
                emp.getName(),
                emp.getDepartment(),
                record.getPayPeriod(),
                record.getRecordID(),
                record.getBatchID(),
                money(emp.getBasicPay()),
                money(record.getOvertimePay()),
                money(record.getPayoutAmount()),
                money(record.getReimbursementPayout()),
                money(record.getGratuityAmount()),
                money(record.getPfAmount()),
                money(record.getPtAmount()),
                money(record.getMonthlyTdsAmount()),
                money(record.getPenaltyAmount()),
                money(record.getFinalGrossPay()),
                money(record.getFinalNetPay()),
                money(record.getBonusArrears()),
                record.isFlaggedForHrReview() ? "HR REVIEW REQUIRED" : "READY");
    }

    private String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private String money(double value) {
        return String.format(Locale.ROOT, "%,.2f", value);
    }
}
