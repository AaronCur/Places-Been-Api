package travel.placesbeen.config;

import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The app's single source of "now". Services take a {@link Clock} instead of calling {@code Instant.now()},
 * so tests can pass {@code Clock.fixed(...)} and get the same answer on any day (e.g. "trips this year").
 */
@Configuration
public class ClockConfig {

    // UTC so stored timestamps don't depend on the server's time zone
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    // Bean Validation has its own idea of "now" for @PastOrPresent/@Future etc., which defaults to the system clock.
    // Point it at the Clock bean so validation and the services agree on what "today" is (and tests can fix both)
    @Bean
    public ValidationConfigurationCustomizer validationClock(Clock clock) {
        return configuration -> configuration.clockProvider(() -> clock);
    }
}
