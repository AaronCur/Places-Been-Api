package travel.placesbeen.city;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import travel.placesbeen.country.Country;
import travel.placesbeen.country.CountryRepository;
import travel.placesbeen.country.Flags;
import travel.placesbeen.country.UnknownCountryCodeException;

import java.util.List;

@Service
public class CityService {

    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;

    public CityService(CityRepository cityRepository, CountryRepository countryRepository) {
        this.cityRepository = cityRepository;
        this.countryRepository = countryRepository;
    }

    @Transactional
    public City findOrCreate(CityRequest cityRequest) {
        Country country = findCountry(cityRequest.countryCode());

        String state = normalizeState(cityRequest.state());

        // A second trip to a city reuses the stored one (its name casing and coordinates win)
        return cityRepository.findByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode(cityRequest.name(), state, country.getIsoCode())
                .orElseGet(() -> cityRepository.save(
                        new City(cityRequest.name(), state, cityRequest.latitude(), cityRequest.longitude(), country)));

    }

    @Transactional
    public CityResponse addCity(CityRequest cityRequest) {

        Country country = findCountry(cityRequest.countryCode());

        String state = normalizeState(cityRequest.state());

        boolean alreadyExists = cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode(cityRequest.name(), state, cityRequest.countryCode());

        if (alreadyExists) {
            throw new IllegalStateException(alreadyExistsMessage(cityRequest.name(), state, country));
        }

        City city = new City(cityRequest.name(), state, cityRequest.latitude(), cityRequest.longitude(), country);

        City savedCity = cityRepository.save(city);
        return mapToResponse(savedCity);
    }


    public List<CityResponse> getAllCities() {
        return cityRepository.findAllByOrderByIdAsc().stream().map(this::mapToResponse).toList();

    }

    public CityResponse getCityById(Long id) {
        return cityRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + id));
    }

    @Transactional
    public CityResponse updateCityById(CityUpdateRequest cityUpdateRequest, Long id) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("City not found with id: " + id));

        // A case-only rename ("berlin" -> "Berlin") matches the city itself, so only check when the name really changes
        boolean renamedToExisting = !city.getName().equalsIgnoreCase(cityUpdateRequest.name())
                && cityRepository.existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode(cityUpdateRequest.name(), city.getState(), city.getCountry().getIsoCode());

        if (renamedToExisting) {
            throw new IllegalStateException(alreadyExistsMessage(cityUpdateRequest.name(), city.getState(), city.getCountry()));
        }

        city.setName(cityUpdateRequest.name());
        city.setLatitude(cityUpdateRequest.latitude());
        city.setLongitude(cityUpdateRequest.longitude());

        City updatedCity = cityRepository.save(city);
        return mapToResponse(updatedCity);
    }

    @Transactional
    public void deleteCityById(Long id) {

        cityRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("City not found with id: " + id));

        // Countries are seeded reference data, so they stay; a country without cities is simply no longer visited
        cityRepository.deleteById(id);
    }

    // Names the state too, or "Portland already exists in United States" is ambiguous
    private static String alreadyExistsMessage(String name, String state, Country country) {
        String place = state == null ? name : name + ", " + state;
        return "City already exists with name: " + place + " in country: " + country.getName();
    }

    private Country findCountry(String isoCode) {

        return countryRepository.findByIsoCode(isoCode)
                .orElseThrow(() -> new UnknownCountryCodeException(isoCode));

    }

    private String normalizeState(String state) {
        return state == null ? null : state.strip();
    }



    // Private helper to keep code DRY (Don't Repeat Yourself)
    private CityResponse mapToResponse(City city) {
        return new CityResponse(
                city.getId(),
                city.getName(),
                city.getState(),
                city.getLatitude(),
                city.getLongitude(),
                city.getCountry().getName(),
                city.getCountry().getIsoCode(),
                Flags.fromIsoCode(city.getCountry().getIsoCode())
        );
    }

}
