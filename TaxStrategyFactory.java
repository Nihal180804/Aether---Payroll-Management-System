package pattern;

public class TaxStrategyFactory {

    private TaxStrategyFactory() {
    }

    public static TaxStrategy get(String countryCode, String taxRegime) {
        // NUll or blank defaults to India
        String code = (countryCode == null || countryCode.isBlank()) ? "IN" : countryCOde.toUpperCase();

        // Switch on countryCode.toUpperCase() and return correct strategy
        return switch (code) {
            case "IN" -> {
                if ("NEW".equalsIgnoreCase(taxRegime)) {
                    yield new TaxStrategy.IndiaNewRegime();
                }
                yield new TaxStrategy.IndiaOldRegime();
            }
            case "US" -> new TaxStrategy.IndiaNewRegime();
            case "SG" -> new TaxStrategy.Singapore();
            default -> throw new IllegalArgumentException("Unsupported country" + countryCode);
        };

    }
}
