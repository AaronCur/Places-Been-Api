package travel.placesbeen.city;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CityResponse(
        Long id,
        String name,
        Double latitude,
        Double longitude,
        String country,
        String countryCode,
        String countryFlag
) { }
