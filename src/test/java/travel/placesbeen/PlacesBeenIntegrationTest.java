package travel.placesbeen;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import travel.placesbeen.city.CityRepository;
import travel.placesbeen.country.CountryRepository;
import travel.placesbeen.trip.TripRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests through the full Spring context against the in-memory H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlacesBeenIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoSpyBean // real repository; two tests make its duplicate check miss, as a race would
    private CityRepository cityRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private TripRepository tripRepository;

    @BeforeEach
    void cleanDatabase() {
        tripRepository.deleteAll(); // first: trips point at cities, and trip.city_id has no ON DELETE
        cityRepository.deleteAll(); // countries are seeded reference data, so they stay
    }

    // Returns the trip's Location; the response body carries the city id
    private MvcResult createTrip(String cityName, String state, String countryCode, String startDate) throws Exception {
        String body = """
                {"cityName": "%s", "state": "%s", "countryCode": "%s", "latitude": 38.722, "longitude": -9.139,
                 "startDate": "%s"}
                """.formatted(cityName, state, countryCode, startDate);

        return mockMvc.perform(post("/api/trips").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private static long cityIdOf(MvcResult tripResult) throws Exception {
        return ((Number) JsonPath.read(tripResult.getResponse().getContentAsString(), "$.city.id")).longValue();
    }

    private String createCity(String name, String state, String countryCode, double lat, double lon) throws Exception {
        String body = """
                {"name": "%s", "state": "%s", "countryCode": "%s", "latitude": %s, "longitude": %s}
                """.formatted(name, state, countryCode, lat, lon);

        MvcResult result = mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getHeader("Location");
    }

    @Test
    void createdCityIsRetrievableAtLocationHeader() throws Exception {
        String location = createCity("Berlin", "Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(get(location).header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Berlin"))
                .andExpect(jsonPath("$.state").value("Berlin"))
                .andExpect(jsonPath("$.country").value("Germany"))
                .andExpect(jsonPath("$.countryCode").value("DE"))
                .andExpect(jsonPath("$.countryFlag").value("🇩🇪"))
                .andExpect(jsonPath("$.latitude").value(52.52));
    }

    @Test
    void cityWithoutStateIsCreatedAndReturnedWithoutOne() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Gibraltar", "countryCode": "GI", "latitude": 36.14, "longitude": -5.35}
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(get(result.getResponse().getHeader("Location")).header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gibraltar"))
                .andExpect(jsonPath("$.state").doesNotExist());
    }

    @Test
    void addingCitiesCountsEachVisitedCountryOnce() throws Exception {
        createCity("Berlin", "Berlin", "DE", 52.52, 13.405);
        createCity("Munich", "Bavaria", "DE", 48.137, 11.575); // same country, counted once
        createCity("Lyon", "Métropole de Lyon", "FR", 45.764, 4.835);

        mockMvc.perform(get("/api/stats/summary").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCountries").value(2))
                .andExpect(jsonPath("$.totalCities").value(3));
    }

    @Test
    void sameCityNameInTwoStatesOfOneCountryIsAllowed() throws Exception {
        createCity("Portland", "Oregon", "US", 45.523, -122.676);
        createCity("Portland", "Maine", "US", 43.661, -70.255);

        mockMvc.perform(get("/api/stats/summary").header("Api-Version", "1"))
                .andExpect(jsonPath("$.totalCountries").value(1))
                .andExpect(jsonPath("$.totalCities").value(2));
    }

    @Test
    void cityWithNoStateIsAllowedAlongsideSameNameWithState() throws Exception {
        createCity("Portland", "Oregon", "US", 45.523, -122.676);

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Portland", "countryCode": "US", "latitude": 45.523, "longitude": -122.676}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").doesNotExist());

        mockMvc.perform(get("/api/stats/summary").header("Api-Version", "1"))
                .andExpect(jsonPath("$.totalCities").value(2));
    }

    @Test
    void sameCityNameAndStateIsRejectedWithConflict() throws Exception {
        createCity("Portland", "Oregon", "US", 45.523, -122.676);

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "portland", "state": "Oregon", "countryCode": "US", "latitude": 45.523, "longitude": -122.676}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void sameCityWithNoStateIsRejectedWithConflict() throws Exception {
        String body = """
                {"name": "Gibraltar", "countryCode": "GI", "latitude": 36.14, "longitude": -5.35}
                """;

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void stateDifferingOnlyInCaseOrSpacesIsRejectedWithConflict() throws Exception {
        createCity("Portland", "Maine", "US", 43.661, -70.255);

        for (String state : new String[]{"maine", " Maine "}) {
            mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name": "Portland", "state": "%s", "countryCode": "US", "latitude": 43.661, "longitude": -70.255}
                                    """.formatted(state)))
                    .andExpect(status().isConflict());
        }
    }

    @Test
    void duplicateThatGetsPastTheCheckIsRejectedByTheDatabaseWithConflict() throws Exception {
        createCity("Portland", "Maine", "US", 43.661, -70.255);
        doReturn(false).when(cityRepository)
                .existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Portland", "Maine", "US");

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Portland", "state": "Maine", "countryCode": "US", "latitude": 43.661, "longitude": -70.255}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Database conflict: The record may already exist or violates a database constraint."));
    }

    @Test
    void renameThatGetsPastTheCheckIsRejectedByTheDatabaseWithConflict() throws Exception {
        createCity("Munich", "Bavaria", "DE", 48.137, 11.575);
        String nuremberg = createCity("Nuremberg", "Bavaria", "DE", 49.452, 11.077);
        doReturn(false).when(cityRepository)
                .existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("Munich", "Bavaria", "DE");

        // The UPDATE only runs at commit, so this also proves that failure still reaches the exception handler
        mockMvc.perform(put(nuremberg).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Munich", "latitude": 49.452, "longitude": 11.077}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Database conflict: The record may already exist or violates a database constraint."));
    }

    @Test
    void sameCityNameInTwoCountriesIsAllowed() throws Exception {
        createCity("Valencia", "Valencia", "ES", 39.47, -0.376);
        createCity("Valencia", "Carabobo", "VE", 10.162, -68.008);

        mockMvc.perform(get("/api/stats/summary").header("Api-Version", "1"))
                .andExpect(jsonPath("$.totalCountries").value(2))
                .andExpect(jsonPath("$.totalCities").value(2));
    }

    @Test
    void countriesEndpointReturnsIsoCodeAndDerivedFlag() throws Exception {
        createCity("Berlin", "Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(get("/api/countries").header("Api-Version", "1"))
                .andExpect(jsonPath("$[0].isoCode").value("DE"))
                .andExpect(jsonPath("$[0].flag").value("🇩🇪"));
    }

    @Test
    void countriesEndpointNestsCitiesWhenRequested() throws Exception {
        createCity("Berlin", "Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(get("/api/countries").param("includeCities", "true").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Germany"))
                .andExpect(jsonPath("$[0].cities[0].name").value("Berlin"))
                .andExpect(jsonPath("$[0].cities[0].state").value("Berlin"));
    }

    @Test
    void duplicateCityIsRejectedWithConflict() throws Exception {
        createCity("Berlin", "Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "berlin", "state": "Berlin", "countryCode": "DE", "latitude": 1.0, "longitude": 1.0}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void updatingCityChangesItsFields() throws Exception {
        String location = createCity("Berlin", "Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(put(location).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Berlin Mitte", "latitude": 52.5, "longitude": 13.4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Berlin Mitte"));

        mockMvc.perform(get(location).header("Api-Version", "1"))
                .andExpect(jsonPath("$.name").value("Berlin Mitte"))
                .andExpect(jsonPath("$.state").value("Berlin")) // not part of an update
                .andExpect(jsonPath("$.longitude").value(13.4));
    }

    @Test
    void renamingCityToExistingNameInSameStateIsRejectedWithConflict() throws Exception {
        createCity("Munich", "Bavaria", "DE", 48.137, 11.575);
        String nuremberg = createCity("Nuremberg", "Bavaria", "DE", 49.452, 11.077);

        mockMvc.perform(put(nuremberg).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "MUNICH", "latitude": 49.452, "longitude": 11.077}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void renamingCityToNameUsedInAnotherStateIsAllowed() throws Exception {
        createCity("Berlin", "Berlin", "DE", 52.52, 13.405);
        String munich = createCity("Munich", "Bavaria", "DE", 48.137, 11.575);

        mockMvc.perform(put(munich).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Berlin", "latitude": 48.137, "longitude": 11.575}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("Bavaria"));
    }

    @Test
    void updateIgnoresCountryCodeSoCityStaysInItsCountry() throws Exception {
        String location = createCity("Berlin", "Berlin", "DE", 52.52, 13.405);

        mockMvc.perform(put(location).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Berlin", "countryCode": "FR", "latitude": 52.52, "longitude": 13.405}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countryCode").value("DE")); // not part of CityUpdateRequest, so ignored
    }

    @Test
    void updateIgnoresStateSoCityKeepsItsState() throws Exception {
        String location = createCity("Munich", "Bavaria", "DE", 48.137, 11.575);

        mockMvc.perform(put(location).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Munich", "state": "Berlin", "latitude": 48.137, "longitude": 11.575}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("Bavaria")); // not part of CityUpdateRequest, so ignored
    }

    @Test
    void renamingCityWithNoStateToExistingNameWithNoStateIsRejectedWithConflict() throws Exception {
        String body = """
                {"name": "%s", "countryCode": "GI", "latitude": 36.14, "longitude": -5.35}
                """;
        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("Gibraltar")))
                .andExpect(status().isCreated());
        String catalanBay = mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.formatted("Catalan Bay")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        // The service check must query "state is null"; "state = null" would match nothing and reach the database
        mockMvc.perform(put(catalanBay).header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "gibraltar", "latitude": 36.14, "longitude": -5.35}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("gibraltar in country: Gibraltar")));
    }

    @Test
    void deletingLastCityInCountryStopsItBeingVisited() throws Exception {
        String berlin = createCity("Berlin", "Berlin", "DE", 52.52, 13.405);
        String munich = createCity("Munich", "Bavaria", "DE", 48.137, 11.575);

        mockMvc.perform(delete(berlin).header("Api-Version", "1")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/countries").header("Api-Version", "1"))
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete(munich).header("Api-Version", "1")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/countries").header("Api-Version", "1"))
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get(munich).header("Api-Version", "1"))
                .andExpect(status().isNotFound());
        assertThat(countryRepository.findByIsoCode("DE")).isPresent(); // seeded country is kept
    }

    @Test
    void caseOnlyDuplicateThatGetsPastTheCheckIsRejectedByTheDatabase() throws Exception {
        createCity("Portland", "Maine", "US", 43.661, -70.255);
        doReturn(false).when(cityRepository)
                .existsByNameIgnoreCaseAndStateIgnoreCaseAndCountry_IsoCode("PORTLAND", "maine", "US");

        // Before V6 the unique constraint compared exact text, so this insert succeeded and left two Portlands
        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "PORTLAND", "state": "maine", "countryCode": "US", "latitude": 43.661, "longitude": -70.255}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Database conflict: The record may already exist or violates a database constraint."));
    }

    @Test
    void secondTripToCityReusesItAndDeletingTheLastTripDeletesTheCity() throws Exception {
        MvcResult first = createTrip("Lisbon", "Lisbon", "PT", "2024-05-01");
        MvcResult second = createTrip(" lisbon ", "Lisbon", "PT", "2025-08-20"); // same city despite case and spaces
        long cityId = cityIdOf(first);
        assertThat(cityIdOf(second)).isEqualTo(cityId);

        mockMvc.perform(delete(first.getResponse().getHeader("Location")).header("Api-Version", "1"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/cities/" + cityId).header("Api-Version", "1"))
                .andExpect(status().isOk()); // one trip left, so the city stays

        mockMvc.perform(delete(second.getResponse().getHeader("Location")).header("Api-Version", "1"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/cities/" + cityId).header("Api-Version", "1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/countries").header("Api-Version", "1"))
                .andExpect(jsonPath("$").isEmpty()); // Portugal is no longer visited
        assertThat(countryRepository.findByIsoCode("PT")).isPresent(); // but the seeded country is kept
    }

    @Test
    void deletingCityThatStillHasTripsIsRejectedWithConflict() throws Exception {
        long cityId = cityIdOf(createTrip("Lisbon", "Lisbon", "PT", "2025-08-20"));

        mockMvc.perform(delete("/api/cities/" + cityId).header("Api-Version", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("City Lisbon still has trips: delete them first"));

        mockMvc.perform(get("/api/cities/" + cityId).header("Api-Version", "1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/trips").header("Api-Version", "1"))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void unknownCountryCodeIsRejectedWith400() throws Exception {
        mockMvc.perform(post("/api/cities").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Nowhere", "countryCode": "QQ", "latitude": 0.0, "longitude": 0.0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown country code: QQ"));
    }

    @Test
    void unknownIdsReturn404() throws Exception {
        mockMvc.perform(get("/api/cities/999999").header("Api-Version", "1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/countries/999999").header("Api-Version", "1"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/cities/999999").header("Api-Version", "1"))
                .andExpect(status().isNotFound());
    }
}
