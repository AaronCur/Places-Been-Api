package travel.placesbeen.city;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// The country isn't editable: to move a city, delete it and add it again
public record CityUpdateRequest (
       @NotBlank @Size(max = 255) String name,
       @NotNull Double latitude,
       @NotNull Double longitude
){}
