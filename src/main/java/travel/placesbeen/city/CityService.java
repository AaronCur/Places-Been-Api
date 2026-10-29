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
    public CityResponse addCity(CityRequest cityrequest) {

        Country country = countryRepository.findByIsoCode(cityrequest.countryCode())
                .orElseThrow(() -> new UnknownCountryCodeException(cityrequest.countryCode()));

        boolean alreadyExists = cityRepository.existsByNameIgnoreCaseAndCountry_IsoCode(cityrequest.name(), cityrequest.countryCode());

        if (alreadyExists) {
            throw new IllegalStateException("City already exists with name: " + cityrequest.name() + " in country: " + country.getName());
        }

        City city = new City(cityrequest.name(), cityrequest.latitude(), cityrequest.longitude(), country);

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
                && cityRepository.existsByNameIgnoreCaseAndCountry_IsoCode(cityUpdateRequest.name(), city.getCountry().getIsoCode());

        if (renamedToExisting) {
            throw new IllegalStateException("City already exists with name: " + cityUpdateRequest.name() + " in country: " + city.getCountry().getName());
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

    // Private helper to keep code DRY (Don't Repeat Yourself)
    private CityResponse mapToResponse(City city) {
        return new CityResponse(
                city.getId(),
                city.getName(),
                city.getLatitude(),
                city.getLongitude(),
                city.getCountry().getName(),
                city.getCountry().getIsoCode(),
                Flags.fromIsoCode(city.getCountry().getIsoCode())
        );
    }

}
