package com.payroll.system.pattern;

import com.payroll.system.model.Employee;



/**
 * Strategy Pattern — one interface, four concrete tax strategies.
 * TaxStrategyFactory selects the right one at runtime.
 */
public interface TaxStrategy {

    double calculateMonthlyTax(Employee emp, double annualGrossIncome);

    // ═══════════════════════════════════════════════════════════
    // Strategy 1 — India Old Regime
    // ═══════════════════════════════════════════════════════════
    class IndiaOldRegime implements TaxStrategy {
        @Override
        public double calculateMonthlyTax(Employee emp, double annualGrossIncome) {
            // Standard deduction 50k + 80C investments
            double taxableIncome = annualGrossIncome - 50_000 - emp.getDeclaredInvestments(); // FIXED
            taxableIncome = Math.max(0, taxableIncome);

            double annualTax = 0;
            if (taxableIncome > 1_000_000)
                annualTax += (taxableIncome - 1_000_000) * 0.30 + 112_500;
            else if (taxableIncome > 500_000)
                annualTax += (taxableIncome - 500_000) * 0.20 + 12_500;
            else if (taxableIncome > 250_000)
                annualTax += (taxableIncome - 250_000) * 0.05;

            return (annualTax * 1.04) / 12.0; // 4% cess, monthly
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Strategy 2 — India New Regime
    // ═══════════════════════════════════════════════════════════
    class IndiaNewRegime implements TaxStrategy {
        @Override
        public double calculateMonthlyTax(Employee emp, double annualGrossIncome) {
            double taxableIncome = Math.max(0, annualGrossIncome - 75_000); // higher std deduction
            double annualTax = 0;
            if (taxableIncome > 1_500_000)
                annualTax += (taxableIncome - 1_500_000) * 0.30 + 150_000;
            else if (taxableIncome > 1_200_000)
                annualTax += (taxableIncome - 1_200_000) * 0.20 + 90_000;
            else if (taxableIncome > 1_000_000)
                annualTax += (taxableIncome - 1_000_000) * 0.15 + 60_000;
            else if (taxableIncome > 700_000)
                annualTax += (taxableIncome - 700_000) * 0.10 + 20_000;
            else if (taxableIncome > 300_000)
                annualTax += (taxableIncome - 300_000) * 0.05;

            return (annualTax * 1.04) / 12.0;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Strategy 3 — US Federal
    // ═══════════════════════════════════════════════════════════
    class USFederal implements TaxStrategy {
        @Override
        public double calculateMonthlyTax(Employee emp, double annualGrossIncome) {
            boolean isMarried = "MARRIED".equalsIgnoreCase(emp.getFilingStatus());
            double standardDeduction = isMarried ? 29_200 : 14_600;
            double taxableIncome = Math.max(0, annualGrossIncome - standardDeduction);

            double annualTax = 0;
            if (taxableIncome > 191_950)
                annualTax += (taxableIncome - 191_950) * 0.24 + 38_307;
            else if (taxableIncome > 100_525)
                annualTax += (taxableIncome - 100_525) * 0.22 + 18_193;
            else if (taxableIncome > 47_150)
                annualTax += (taxableIncome - 47_150) * 0.12 + 5_466;
            else if (taxableIncome > 11_600)
                annualTax += (taxableIncome - 11_600) * 0.10;

            return annualTax / 12.0;
        }
    }

    // ═══════════════════════════════════════════════════════════
    // Strategy 4 — Singapore
    // ═══════════════════════════════════════════════════════════
    class Singapore implements TaxStrategy {
        @Override
        public double calculateMonthlyTax(Employee emp, double annualGrossIncome) {
            double annualTax = 0;
            if (annualGrossIncome > 320_000)
                annualTax += (annualGrossIncome - 320_000) * 0.22 + 44_550;
            else if (annualGrossIncome > 120_000)
                annualTax += (annualGrossIncome - 120_000) * 0.115 + 7_950;
            else if (annualGrossIncome > 80_000)
                annualTax += (annualGrossIncome - 80_000) * 0.07 + 3_350;
            else if (annualGrossIncome > 40_000)
                annualTax += (annualGrossIncome - 40_000) * 0.035 + 550;
            else if (annualGrossIncome > 30_000)
                annualTax += (annualGrossIncome - 30_000) * 0.02 + 200;
            else if (annualGrossIncome > 20_000)
                annualTax += (annualGrossIncome - 20_000) * 0.02;

            return annualTax / 12.0;
        }
    }
}



