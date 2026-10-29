package travel.placesbeen.city;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CityRequest (
       @NotBlank String  name,
       @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode,
       @NotNull Double latitude,
       @NotNull Double longitude
){}

