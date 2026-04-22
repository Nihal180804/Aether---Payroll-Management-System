package com.payroll.system.pattern;

import com.payroll.system.model.Employee;

/**
 * Factory class to instantiate the correct tax strategy based on employee's country and regime.
 */
public class TaxStrategyFactory {
    public static TaxStrategy get(String countryCode, String regime) {
        if ("US".equalsIgnoreCase(countryCode)) {
            return new TaxStrategy.USFederal();
        } else if ("SG".equalsIgnoreCase(countryCode)) {
            return new TaxStrategy.Singapore();
        } else {
            // Default to India
            if ("NEW".equalsIgnoreCase(regime)) {
                return new TaxStrategy.IndiaNewRegime();
            } else {
                return new TaxStrategy.IndiaOldRegime();
            }
        }
    }
}
