package travel.placesbeen.trip;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// The place isn't editable: to move a trip, delete it and add it again
// TODO: class-level @ValidDateRange (end on or after start), shared with TripRequest
public record TripUpdateRequest(
        // TODO: not in the future
        @NotNull LocalDate startDate,
        // Optional: null for a day trip
        LocalDate endDate,
        // TODO: optional; max length to match the column
        String notes
) {
}
