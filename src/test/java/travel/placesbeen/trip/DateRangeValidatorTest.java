package travel.placesbeen.trip;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

// Plain Bean Validation, no Spring: @ValidDateRange finds its validator through @Constraint
class DateRangeValidatorTest {

    private static final LocalDate START = LocalDate.of(2025, 6, 10);

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void endDateBeforeStartDateIsReportedOnEndDate() {
        Set<ConstraintViolation<TripUpdateRequest>> violations =
                validator.validate(new TripUpdateRequest(START, START.minusDays(1), null));

        assertThat(violations).singleElement().satisfies(violation -> {
            // On the field, not the object, so GlobalExceptionHandler's field-error message includes it
            assertThat(violation.getPropertyPath()).hasToString("endDate");
            assertThat(violation.getMessage()).isEqualTo("End date must be on or after the start date");
        });
    }

    @Test
    void endDateOnStartDateIsValid() {
        assertThat(validator.validate(new TripUpdateRequest(START, START, null))).isEmpty();
    }

    @Test
    void endDateAfterStartDateIsValid() {
        assertThat(validator.validate(new TripUpdateRequest(START, START.plusDays(6), null))).isEmpty();
    }

    @Test
    void dayTripWithNoEndDateIsValid() {
        assertThat(validator.validate(new TripUpdateRequest(START, null, null))).isEmpty();
    }

    @Test
    void missingStartDateIsOnlyReportedByNotNull() {
        Set<ConstraintViolation<TripUpdateRequest>> violations =
                validator.validate(new TripUpdateRequest(null, START, null));

        assertThat(violations).singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath()).hasToString("startDate"));
    }

    @Test
    void appliesToTripRequestToo() {
        TripRequest request = new TripRequest("Lisbon", "Lisbon", "PT", 38.722, -9.139,
                START, START.minusDays(1), null);

        assertThat(validator.validate(request)).singleElement()
                .satisfies(violation -> assertThat(violation.getPropertyPath()).hasToString("endDate"));
    }
}
