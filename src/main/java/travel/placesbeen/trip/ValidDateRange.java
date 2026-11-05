package travel.placesbeen.trip;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class-level check that a {@link DateRange} doesn't end before it starts. It sits on the type because a
 * field-level constraint only sees its own field. The error is reported on {@code endDate}.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {

    String message() default "End date must be on or after the start date";

    // Required by the Bean Validation spec, even though we don't use them
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
