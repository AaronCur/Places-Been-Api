package travel.placesbeen.country;

import com.fasterxml.jackson.annotation.JsonInclude;
import travel.placesbeen.city.CityResponse;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL) // Hidden if null!
public record CountryResponse(
        Long id,
        String name,
        String isoCode,
        String flag,
        List<CityResponse> cities) {
}
