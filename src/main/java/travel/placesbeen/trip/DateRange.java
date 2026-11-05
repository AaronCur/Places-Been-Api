package travel.placesbeen.trip;

import java.time.LocalDate;

/**
 * Anything with a start and an optional end date. Lets one {@link DateRangeValidator} check both
 * {@link TripRequest} and {@link TripUpdateRequest}; their record components already provide these methods.
 */
public interface DateRange {
    LocalDate startDate();

    LocalDate endDate();
}
