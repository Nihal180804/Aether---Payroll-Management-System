package com.payroll.system.presenter;

import java.util.List;

/** Result of one payroll batch run. */
public class BatchResult {
    public final List<PayrollResultViewModel> results;
    public final int    processedCount;
    public final int    skippedCount;
    public final String error;  // null if batch completed without a fatal error

    public BatchResult(List<PayrollResultViewModel> results,
                       int processedCount, int skippedCount, String error) {
        this.results        = results;
        this.processedCount = processedCount;
        this.skippedCount   = skippedCount;
        this.error          = error;
    }
}

