package com.pesu.leavesubsystem.integration;

import com.payroll.system.util.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

public class LeaveDataProviderImpl {

    private final String dbUrl;

    public LeaveDataProviderImpl() {
        this.dbUrl = DatabaseConfig.getJdbcUrl();
    }

    public LeaveDetailsDTO getLeaveDetailsForPayroll(String employeeId, String payPeriod, int fallbackWorkingDays) {
        LeaveDetailsDTO dto = new LeaveDetailsDTO();
        dto.setEmployeeId(employeeId);
        dto.setPayPeriod(payPeriod);
        dto.setWorkingDaysInMonth(fallbackWorkingDays > 0 ? fallbackWorkingDays : inferWorkingDays(payPeriod));

        hydrateFromLeaveRecords(dto);
        hydrateOvertime(dto, payPeriod);
        return dto;
    }

    private void hydrateFromLeaveRecords(LeaveDetailsDTO dto) {
        String sql = """
                SELECT start_date, end_date, leave_type
                FROM leave_records
                WHERE emp_id = ?
                  AND UPPER(COALESCE(status, '')) = 'APPROVED'
                """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dto.getEmployeeId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    LocalDate start = parseDate(rs.getString("start_date"));
                    LocalDate end = parseDate(rs.getString("end_date"));
                    int overlapDays = countWorkdayOverlap(start, end, dto.getPayPeriod());
                    if (overlapDays <= 0) {
                        continue;
                    }
                    String type = rs.getString("leave_type");
                    if (isUnpaidLeave(type)) {
                        dto.setLeaveWithoutPay(dto.getLeaveWithoutPay() + overlapDays);
                    } else {
                        dto.setLeaveWithPay(dto.getLeaveWithPay() + overlapDays);
                    }
                }
            }
        } catch (SQLException ignored) {
        }
    }

    private void hydrateOvertime(LeaveDetailsDTO dto, String payPeriod) {
        String sql = """
                SELECT COALESCE(SUM(overtime_hours), 0)
                FROM overtime_records
                WHERE emp_id = ?
                  AND UPPER(COALESCE(approval_status, '')) = 'APPROVED'
                """;

        try (Connection conn = DriverManager.getConnection(dbUrl);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dto.getEmployeeId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    dto.setOvertimeHours(rs.getDouble(1));
                }
            }
        } catch (SQLException ignored) {
        }
    }

    private boolean isUnpaidLeave(String leaveType) {
        if (leaveType == null) {
            return false;
        }
        String normalized = leaveType.trim().toLowerCase();
        return normalized.contains("without")
                || normalized.contains("unpaid")
                || normalized.contains("lop")
                || normalized.contains("loss");
    }

    private int inferWorkingDays(String payPeriod) {
        YearMonth month = YearMonth.parse(payPeriod);
        int workdays = 0;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            DayOfWeek dow = month.atDay(day).getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                workdays++;
            }
        }
        return workdays;
    }

    private int countWorkdayOverlap(LocalDate start, LocalDate end, String payPeriod) {
        if (start == null || end == null) {
            return 0;
        }
        YearMonth month = YearMonth.parse(payPeriod);
        LocalDate rangeStart = start.isAfter(month.atDay(1)) ? start : month.atDay(1);
        LocalDate rangeEnd = end.isBefore(month.atEndOfMonth()) ? end : month.atEndOfMonth();
        if (rangeEnd.isBefore(rangeStart)) {
            return 0;
        }
        int days = 0;
        for (LocalDate d = rangeStart; !d.isAfter(rangeEnd); d = d.plusDays(1)) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                days++;
            }
        }
        return days;
    }

    private LocalDate parseDate(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value);
    }
}
