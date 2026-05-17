package travel.placesbeen.country;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping( "/api/countries")
@Tag( name = "Country API", description = "API for managing countries")
public class CountryController {

    private final CountryService countryService;
    public CountryController(CountryService countryService) {
        this.countryService = countryService;
    }

    @GetMapping(version = "1")
    public ResponseEntity<List<CountryResponse>> getAllCountries(@RequestParam(value = "includeCities", required = false, defaultValue = "false") boolean includeCities) {
        return ResponseEntity.ok(countryService.getAllCountries(includeCities));
    }

    @GetMapping(value = "/{id}", version = "1")
    public ResponseEntity<CountryResponse> getCountryById(@PathVariable Long id) {
        return ResponseEntity.ok(countryService.getCountryById(id));
    }
}

