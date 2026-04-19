package pattern;

/**
 * =============================================================================
 * CREATIONAL PATTERN — Factory Method
 * CLASS: TaxStrategyFactory
 * =============================================================================
 * WHY THIS PATTERN?
 *   IncomeTaxTDS needs a TaxStrategy, but the right one depends on runtime
 *   data (employee's countryCode + taxRegime). Hard-coding strategy selection
 *   inside IncomeTaxTDS would violate SOLID — Dependency Inversion.
 *
 *   TaxStrategyFactory centralises all selection logic in ONE place.
 *   IncomeTaxTDS calls TaxStrategyFactory.get(...) and never imports a
 *   concrete strategy class directly.
 *
 * SOLID:
 *   - O: New country → add one case here, zero changes elsewhere.
 *   - D: IncomeTaxTDS depends on TaxStrategy (interface), not concrete classes.
 * =============================================================================
 */
public class TaxStrategyFactory {

    // Private constructor — this is a utility class, never instantiated
    private TaxStrategyFactory() {}

    /**
     * TODO ── get()
     * ─────────────────────────────────────────────────────────────────────────
     * Returns the correct TaxStrategy based on countryCode and taxRegime.
     *
     * HINT — use a switch on countryCode.toUpperCase():
     *
     *   case "IN":
     *     if ("NEW".equalsIgnoreCase(taxRegime)) return new TaxStrategy.IndiaNewRegime();
     *     return new TaxStrategy.IndiaOldRegime();   // default for "OLD" or null
     *
     *   case "US":
     *     return new TaxStrategy.USFederal();
     *
     *   case "SG":
     *     return new TaxStrategy.Singapore();
     *
     *   default:
     *     throw new IllegalArgumentException("Unsupported country: " + countryCode);
     *
     * Also handle null countryCode — default to "IN" if null or blank.
     * ─────────────────────────────────────────────────────────────────────────
     */
    public static TaxStrategy get(String countryCode, String taxRegime) {
        // TODO: Null/blank guard — default countryCode to "IN"

        // TODO: Switch on countryCode.toUpperCase() and return correct strategy

        return null; // REMOVE once implemented
    }
}
