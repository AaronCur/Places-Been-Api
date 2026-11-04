package travel.placesbeen.trip;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

// TODO: class-level @ValidDateRange (end on or after start), shared with TripUpdateRequest
public record TripRequest(
        // TODO: max length, like CityRequest
        @NotBlank String cityName,
        // TODO: optional; same rules as CityRequest.state (max length, not blank when given)
        String state,
        // TODO: ISO 3166-1 alpha-2 pattern, like CityRequest
        @NotBlank String countryCode,
        // TODO: range check (-90..90)
        @NotNull Double latitude,
        // TODO: range check (-180..180)
        @NotNull Double longitude,
        // TODO: not in the future
        @NotNull LocalDate startDate,
        // Optional: null for a day trip
        LocalDate endDate,
        // TODO: optional; max length to match the column
        String notes
) {
}
