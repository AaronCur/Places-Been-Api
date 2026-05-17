package travel.placesbeen.city;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;


@RestController
@RequestMapping("/api/cities")
@Tag( name = "City API", description = "API for managing cities")
public class CityController {
    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping(version = "1")
    public ResponseEntity<List<CityResponse>> getAllCities() {
        return ResponseEntity.ok(cityService.getAllCities());
    }

    @GetMapping(value = "/{id}", version = "1")
    public ResponseEntity<CityResponse> getCityById(@PathVariable Long id) {
        return ResponseEntity.ok(cityService.getCityById(id));
    }

    @PostMapping(version = "1")
    public ResponseEntity<CityResponse> addCity(@Valid @RequestBody CityRequest cityRequest) {
        CityResponse newCity = cityService.addCity(cityRequest);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()    // Takes "http://localhost:8080/api/v1/cities"
                .path("/{id}")           // Appends "/{id}"
                .buildAndExpand(newCity.id())
                .toUri();
        return ResponseEntity.created(location).body(newCity);
    }

    @PutMapping(value = "/{id}", version = "1")
    public ResponseEntity<CityResponse> updateCityById(@PathVariable Long id, @Valid @RequestBody CityRequest cityRequest) {
        return ResponseEntity.ok(cityService.updateCityById(cityRequest, id));
    }

    @DeleteMapping(value = "/{id}", version = "1")
    public ResponseEntity<Void> deleteCityById(@PathVariable Long id) {
        cityService.deleteCityById(id);
        return ResponseEntity.noContent().build();
    }
}
