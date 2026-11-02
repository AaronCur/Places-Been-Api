package travel.placesbeen.city;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CityRequest (
       @NotBlank String  name,
       @Size(max = 255) @Pattern(regexp = ".*\\S.*", message = "State must not be blank") String state,
       @NotBlank @Pattern(regexp = "[A-Z]{2}") String countryCode,
       @NotNull Double latitude,
       @NotNull Double longitude
){}

