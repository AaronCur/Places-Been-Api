package travel.placesbeen.stats;

import org.springframework.stereotype.Service;
import travel.placesbeen.city.CityRepository;
import travel.placesbeen.country.CountryRepository;

@Service
public class StatsService {

    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;

    public StatsService(CountryRepository countryRepository, CityRepository cityRepository) {
        this.countryRepository = countryRepository;
        this.cityRepository = cityRepository;
    }

    public StatsSummaryResponse getStatsSummary() {
        long totalCountries = countryRepository.count();
        long totalCities = cityRepository.count();

        return new StatsSummaryResponse(totalCountries, totalCities);
    }
}
