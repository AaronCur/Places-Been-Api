package travel.placesbeen.config;

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
}
