package travel.placesbeen.trip;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// The place isn't editable: to move a trip, delete it and add it again
@ValidDateRange
public record TripUpdateRequest(
        @NotNull @PastOrPresent LocalDate startDate,
        // Optional: null for a day trip
        LocalDate endDate,
        @Size(max = 2000) String notes
) implements DateRange {
}
