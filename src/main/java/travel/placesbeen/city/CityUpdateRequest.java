package travel.placesbeen.city;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// The country isn't editable: to move a city, delete it and add it again
public record CityUpdateRequest (
       @NotBlank String name,
       @NotNull Double latitude,
       @NotNull Double longitude
){}
