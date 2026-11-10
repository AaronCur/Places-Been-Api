package travel.placesbeen.config;

import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.convention.TestBean;
import travel.placesbeen.trip.TripUpdateRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves @PastOrPresent reads the app's Clock bean rather than the system clock: with the clock pinned to
 * 1 June 2020, a trip starting the next day is "in the future" even though that date is long past in real life.
 */
@SpringBootTest
@ActiveProfiles("test")
class ValidationClockTest {

    @TestBean // replaces the Clock bean in this test's context, using the static factory method below
    private Clock clock;

    static Clock clock() {
        return Clock.fixed(Instant.parse("2020-06-01T12:00:00Z"), ZoneOffset.UTC);
    }

    @Autowired
    private Validator validator;

    @Test
    void startDateAfterTheClocksTodayIsRejected() {
        var trip = new TripUpdateRequest(LocalDate.of(2020, 6, 2), null, null);

        assertThat(validator.validate(trip))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("startDate");
    }

    @Test
    void startDateOnTheClocksTodayIsAccepted() {
        var trip = new TripUpdateRequest(LocalDate.of(2020, 6, 1), null, null);

        assertThat(validator.validate(trip)).isEmpty();
    }
}
