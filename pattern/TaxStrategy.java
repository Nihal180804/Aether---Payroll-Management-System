package pattern;

import model.Employee;

/**
 * =============================================================================
 * BEHAVIOURAL PATTERN — Strategy
 * =============================================================================
 * Interface: TaxStrategy
 *
 * WHY THIS PATTERN?
 *   Tax calculation logic is totally different per country:
 *     India  → Old/New slab regime, 80C deductions, PT slabs by state
 *     US     → Federal brackets, filing status (SINGLE/MARRIED)
 *     SG     → Singapore progressive rates, no filing status
 *
 *   Without Strategy, IncomeTaxTDS would have a massive if-else chain.
 *   Every new country would require editing IncomeTaxTDS directly → violates
 *   SOLID Open/Closed Principle.
 *
 * HOW IT WORKS:
 *   1. IncomeTaxTDS holds a reference to TaxStrategy (the "context").
 *   2. At runtime, TaxStrategyFactory selects which concrete class to use.
 *   3. IncomeTaxTDS calls strategy.calculateMonthlyTax(...) — it doesn't
 *      know or care which concrete class it's talking to.
 *
 * GRASP — Polymorphism:
 *   Instead of: if (country == "IN") { ... } else if (country == "US") { ... }
 *   We use:     strategy.calculateMonthlyTax(emp, annualGross)
 * =============================================================================
 */
public interface TaxStrategy {

    /**
     * Calculates the monthly TDS / withholding tax.
     *
     * @param emp              The employee (used for declared investments, filing status, etc.)
     * @param annualGrossIncome Gross pay × 12 + projected bonus
     * @return Monthly tax deduction amount in the employee's home currency.
     */
    double calculateMonthlyTax(Employee emp, double annualGrossIncome);

    // ═══════════════════════════════════════════════════════════════════════════
    //  Concrete Strategy 1 — India Old Regime
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── IndiaOldRegime.calculateMonthlyTax()
     * ─────────────────────────────────────────────────────────────────────────
     * Old Regime allows deductions (80C, standard deduction).
     *
     * STEP 1: Compute taxable income:
     *   taxableIncome = annualGrossIncome - 50,000 (standard deduction)
     *                                     - emp.getDeclaredInvestments()
     *   Clamp to 0 if negative: taxableIncome = Math.max(0, taxableIncome)
     *
     * STEP 2: Apply Old Regime slabs:
     *   ≤ 2,50,000  → 0%
     *   2.5L – 5L   → 5%  on amount above 2.5L
     *   5L – 10L    → 20% on amount above 5L  (+ 12,500 from previous slab)
     *   > 10L       → 30% on amount above 10L (+ 1,12,500 from previous slabs)
     *
     * STEP 3: Add 4% Health & Education Cess:
     *   annualTax = annualTax * 1.04
     *
     * STEP 4: Return monthly amount:
     *   return annualTax / 12.0
     * ─────────────────────────────────────────────────────────────────────────
     */
    class IndiaOldRegime implements TaxStrategy {

        @Override
        public double calculateMonthlyTax(Employee emp, double annualGrossIncome) {
            // TODO Step 1: Compute taxableIncome (subtract standard deduction + declaredInvestments)

            // TODO Step 2: Apply slab rates using if-else
            // HINT:
            // if (taxableIncome <= 250_000) return 0;
            // if (taxableIncome <= 500_000) return ((taxableIncome - 250_000) * 0.05) * 1.04 / 12;
            // ... continue for higher slabs

            return 0.0; // REMOVE once implemented
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Concrete Strategy 2 — India New Regime
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── IndiaNewRegime.calculateMonthlyTax()
     * ─────────────────────────────────────────────────────────────────────────
     * New Regime: higher standard deduction (₹75,000), NO 80C/investment deductions.
     *
     * STEP 1: taxableIncome = annualGrossIncome - 75,000
     *         NOTE: Do NOT subtract declaredInvestments (not allowed in new regime)
     *
     * STEP 2: Apply New Regime slabs (post-Budget 2024):
     *   ≤ 3L        → 0%
     *   3L – 7L     → 5%
     *   7L – 10L    → 10%
     *   10L – 12L   → 15%
     *   12L – 15L   → 20%
     *   > 15L       → 30%
     *
     * STEP 3 & 4: Same as Old Regime — add 4% Cess, divide by 12.
     * ─────────────────────────────────────────────────────────────────────────
     */
    class IndiaNewRegime implements TaxStrategy {

        @Override
        public double calculateMonthlyTax(Employee emp, double annualGrossIncome) {
            // TODO Step 1: taxableIncome = annualGrossIncome - 75,000 (no investment deduction!)

            // TODO Step 2: Apply new regime slab rates

            // TODO Step 3 & 4: Add 4% cess, divide by 12

            return 0.0; // REMOVE once implemented
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Concrete Strategy 3 — US Federal
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── USFederal.calculateMonthlyTax()
     * ─────────────────────────────────────────────────────────────────────────
     * US federal withholding. Uses emp.getFilingStatus() to determine brackets.
     *
     * STEP 1: Standard deduction:
     *   SINGLE  → $14,600
     *   MARRIED → $29,200
     *   taxableIncome = Math.max(0, annualGrossIncome - standardDeduction)
     *
     * STEP 2: Apply brackets (2024 Single):
     *   ≤ $11,600   → 10%
     *   up to $47,150   → 12%
     *   up to $100,525  → 22%
     *   up to $191,950  → 24%
     *   (continue if needed)
     *
     * STEP 3: Return annualTax / 12.0 (no cess for US)
     *
     * HINT: Check filing status with: "MARRIED".equalsIgnoreCase(emp.getFilingStatus())
     * ─────────────────────────────────────────────────────────────────────────
     */
    class USFederal implements TaxStrategy {

        @Override
        public double calculateMonthlyTax(Employee emp, double annualGrossIncome) {
            // TODO Step 1: Determine filing status and standard deduction

            // TODO Step 2: Apply correct bracket method based on filing status

            // TODO Step 3: Return annualTax / 12.0

            return 0.0; // REMOVE once implemented
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  Concrete Strategy 4 — Singapore
    // ═══════════════════════════════════════════════════════════════════════════
    /**
     * TODO ── Singapore.calculateMonthlyTax()
     * ─────────────────────────────────────────────────────────────────────────
     * Singapore progressive tax, no standard deduction.
     *
     * Key brackets (YA 2024):
     *   ≤ S$20,000   → 0%
     *   up to S$30K  → 2%
     *   up to S$40K  → 3.5%
     *   up to S$80K  → 7%
     *   up to S$120K → 11.5%
     *   > S$320K     → 22%
     *
     * HINT: No filing status needed. Just apply slabs to annualGrossIncome directly.
     * ─────────────────────────────────────────────────────────────────────────
     */
    class Singapore implements TaxStrategy {

        @Override
        public double calculateMonthlyTax(Employee emp, double annualGrossIncome) {
            // TODO: Apply Singapore progressive slab rates to annualGrossIncome

            return 0.0; // REMOVE once implemented
        }
    }
}
