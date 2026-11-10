package travel.placesbeen.trip;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

@ValidDateRange
public record TripRequest(

        @NotBlank @Size(max = 255) String cityName,
        @Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "State must not be blank") String state,
        @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @NotNull @PastOrPresent LocalDate startDate,
        // Optional: null for a day trip
        LocalDate endDate,
        @Size(max = 2000) String notes
) implements DateRange {
}
