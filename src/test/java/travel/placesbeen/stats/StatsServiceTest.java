package travel.placesbeen.stats;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import travel.placesbeen.city.CityRepository;
import travel.placesbeen.country.CountryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private CityRepository cityRepository;

    @InjectMocks
    private StatsService statsService;

    @Test
    void getStatsSummary_returnsRepositoryCounts() {
        when(countryRepository.countByCitiesIsNotEmpty()).thenReturn(3L);
        when(cityRepository.count()).thenReturn(12L);

        StatsSummaryResponse summary = statsService.getStatsSummary();

        assertThat(summary.totalCountries()).isEqualTo(3L);
        assertThat(summary.totalCities()).isEqualTo(12L);
    }

    @Test
    void getStatsSummary_returnsZerosWhenEmpty() {
        when(countryRepository.countByCitiesIsNotEmpty()).thenReturn(0L);
        when(cityRepository.count()).thenReturn(0L);

        assertThat(statsService.getStatsSummary()).isEqualTo(new StatsSummaryResponse(0, 0));
    }
}
