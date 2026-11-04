package travel.placesbeen.trip;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TripController.class)
public class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    // --- Happy paths ---

    @Test
    @Disabled("TODO")
    void shouldReturnAllTripsNewestFirst() throws Exception {
        // given the service returns two trips, newest first
        // when GET /api/trips
        // then 200 and the trips in that order, with nested city and country
    }

    @Test
    @Disabled("TODO")
    void shouldReturnEmptyListWhenNoTrips() throws Exception {
        // given the service returns no trips
        // when GET /api/trips
        // then 200 and []
    }

    @Test
    @Disabled("TODO")
    void shouldAddTrip() throws Exception {
        // given a valid body with start and end dates
        // when POST /api/trips
        // then 201, a Location header ending in the new id, and the trip in the body
    }

    @Test
    @Disabled("TODO")
    void shouldAddDayTripWithoutEndDate() throws Exception {
        // given a valid body with no endDate
        // when POST /api/trips
        // then 201 and the service receives a request with endDate null
    }

    @Test
    @Disabled("TODO")
    void shouldUpdateTrip() throws Exception {
        // given a valid update body
        // when PUT /api/trips/1
        // then 200 and the updated trip
    }

    @Test
    @Disabled("TODO")
    void shouldDeleteTrip() throws Exception {
        // given an existing trip
        // when DELETE /api/trips/1
        // then 204 and the service was asked to delete id 1
    }

    // --- Validation on POST (400, service never called) ---

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenCityNameMissing() throws Exception {
        // given a body without cityName
        // when POST /api/trips
        // then 400 naming cityName
    }

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenCountryCodeMissing() throws Exception {
        // given a body without countryCode
        // when POST /api/trips
        // then 400 naming countryCode
    }

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenCountryCodeIsNotIsoAlpha2() throws Exception {
        // given countryCode "deu"
        // when POST /api/trips
        // then 400 naming countryCode
    }

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenCoordinatesMissing() throws Exception {
        // given a body without latitude and longitude
        // when POST /api/trips
        // then 400 naming both
    }

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenCoordinatesOutOfRange() throws Exception {
        // given latitude 91 and longitude 181
        // when POST /api/trips
        // then 400 naming both
    }

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenStartDateMissing() throws Exception {
        // given a body without startDate
        // when POST /api/trips
        // then 400 naming startDate
    }

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenStartDateInFuture() throws Exception {
        // given startDate tomorrow
        // when POST /api/trips
        // then 400 naming startDate
    }

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenEndDateBeforeStartDate() throws Exception {
        // given endDate the day before startDate
        // when POST /api/trips
        // then 400 from the class-level date range check
    }

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenNotesTooLong() throws Exception {
        // given notes one character over the limit
        // when POST /api/trips
        // then 400 naming notes
    }

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenDateIsMalformed() throws Exception {
        // given startDate "12/10/2026"
        // when POST /api/trips
        // then 400 (malformed JSON / invalid data types)
    }

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenCountryCodeUnknown() throws Exception {
        // given the service throws UnknownCountryCodeException for "QQ"
        // when POST /api/trips
        // then 400 naming QQ
    }

    // --- Validation on PUT ---

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenUpdatedStartDateMissing() throws Exception {
        // given an update body without startDate
        // when PUT /api/trips/1
        // then 400 naming startDate
    }

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenUpdatedEndDateBeforeStartDate() throws Exception {
        // given an update body with endDate before startDate
        // when PUT /api/trips/1
        // then 400 from the class-level date range check
    }

    @Test
    @Disabled("TODO")
    void shouldReturn400WhenIdIsNotANumber() throws Exception {
        // given id "abc"
        // when PUT and DELETE /api/trips/abc
        // then 400 for both
    }

    // --- Not found ---

    @Test
    @Disabled("TODO")
    void shouldReturn404WhenUpdatingMissingTrip() throws Exception {
        // given the service throws EntityNotFoundException for id 99
        // when PUT /api/trips/99
        // then 404
    }

    @Test
    @Disabled("TODO")
    void shouldReturn404WhenDeletingMissingTrip() throws Exception {
        // given the service throws EntityNotFoundException for id 99
        // when DELETE /api/trips/99
        // then 404
    }
}
