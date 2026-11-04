package travel.placesbeen.trip;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDate;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TripResponse(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        String notes,
        CitySummary city,
        CountrySummary country
) {
    // Shapes from the plan: just what the timeline, the country list and the globe need
    public record CitySummary(Long id, String name, String state, Double latitude, Double longitude) {
    }

    public record CountrySummary(String isoCode, String name, String flag) {
    }
}
