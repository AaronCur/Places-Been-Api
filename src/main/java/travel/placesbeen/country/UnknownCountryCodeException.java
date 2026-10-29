package travel.placesbeen.country;

/**
 * A request named a country code that isn't in the seeded country table. It's the client's mistake, so it maps to 400.
 */
public class UnknownCountryCodeException extends RuntimeException {

    public UnknownCountryCodeException(String isoCode) {
        super("Unknown country code: " + isoCode);
    }
}
