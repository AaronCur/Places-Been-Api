package travel.placesbeen.country;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import travel.placesbeen.city.CityResponse;

import java.util.List;

@Service
public class CountryService {

    private final CountryRepository countryRepository;

    public CountryService(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    public List<CountryResponse> getAllCountries(boolean includeCities) {
        return countryRepository.findAll().stream().map(country -> mapToResponse(country, includeCities)).toList();
    }

    public CountryResponse getCountryById(Long id) {
        return mapToResponse(countryRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Country not found with id: " + id)
        ), false);
    }

    // Private helper to keep code DRY (Don't Repeat Yourself)
    private CountryResponse mapToResponse(Country country, boolean includeCities) {
        List<CityResponse> nestedCities = null;

        if (includeCities && country.getCities() != null) {
            nestedCities = country.getCities().stream()
                    .map(city -> new CityResponse(
                            city.getId(),
                            city.getName(),
                            city.getLatitude(),
                            city.getLongitude().doubleValue(),
                            null,
                            null
                    ))
                    .toList();
        }

        return new CountryResponse(
                country.getId(),
                country.getName(),
                country.getFlag(),
                nestedCities // This will cleanly pass either the list or null
        );
    }

}
