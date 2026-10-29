package travel.placesbeen.country;

/**
 * Turns an ISO 3166-1 alpha-2 code into its flag emoji. A flag emoji is the code's two letters written as
 * Unicode regional indicator symbols (U+1F1E6 'A' to U+1F1FF 'Z'), so "DE" becomes 🇩🇪.
 */
public final class Flags {

    private static final int REGIONAL_INDICATOR_A = 0x1F1E6;

    private Flags() {
    }

    public static String fromIsoCode(String isoCode) {
        if (isoCode == null || !isoCode.matches("[A-Z]{2}")) {
            throw new IllegalArgumentException("Not an ISO 3166-1 alpha-2 code: " + isoCode);
        }
        return isoCode.codePoints()
                .map(letter -> REGIONAL_INDICATOR_A + (letter - 'A'))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
