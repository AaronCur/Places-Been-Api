package travel.placesbeen.trip;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DateRangeValidator implements ConstraintValidator<ValidDateRange, DateRange> {

    @Override
    public boolean isValid(DateRange range, ConstraintValidatorContext context) {
        // A missing startDate is @NotNull's job, and a null endDate is a day trip; don't report either twice
        if (range == null || range.startDate() == null || range.endDate() == null) {
            return true;
        }

        if (!range.endDate().isBefore(range.startDate())) {
            return true;
        }

        // Report it on endDate rather than on the whole object: GlobalExceptionHandler only reads field errors,
        // and the UI can show it under the end date field
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("endDate")
                .addConstraintViolation();
        return false;
    }
}
