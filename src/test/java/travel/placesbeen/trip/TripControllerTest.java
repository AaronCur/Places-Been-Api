package travel.placesbeen.trip;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import travel.placesbeen.country.UnknownCountryCodeException;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TripController.class)
public class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TripService tripService;

    private static final TripResponse.CitySummary LISBON =
            new TripResponse.CitySummary(5L, "Lisbon", "Lisbon", 38.722, -9.139);
    private static final TripResponse.CountrySummary PORTUGAL =
            new TripResponse.CountrySummary("PT", "Portugal", "🇵🇹");

    private static final TripResponse LISBON_TRIP = new TripResponse(1L,
            LocalDate.of(2025, 8, 20), LocalDate.of(2025, 8, 27), "Pastéis de nata", LISBON, PORTUGAL);

    // --- Request bodies ---
    // A valid POST body as raw JSON values, so each test changes or removes just the field it's about

    private static Map<String, String> validTripFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("cityName", "\"Lisbon\"");
        fields.put("state", "\"Lisbon\"");
        fields.put("countryCode", "\"PT\"");
        fields.put("latitude", "38.722");
        fields.put("longitude", "-9.139");
        fields.put("startDate", "\"2025-08-20\"");
        fields.put("endDate", "\"2025-08-27\"");
        fields.put("notes", "\"Pastéis de nata\"");
        return fields;
    }

    private static String json(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(field -> "\"" + field.getKey() + "\": " + field.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private static String validTripBody() {
        return json(validTripFields());
    }

    private static String tripBodyWith(String field, String jsonValue) {
        Map<String, String> fields = validTripFields();
        fields.put(field, jsonValue);
        return json(fields);
    }

    private static String tripBodyWithout(String... fieldsToRemove) {
        Map<String, String> fields = validTripFields();
        for (String field : fieldsToRemove) {
            fields.remove(field);
        }
        return json(fields);
    }

    private static final String VALID_UPDATE_BODY = """
            {
                "startDate": "2025-08-21",
                "endDate": "2025-08-28",
                "notes": "Went back for more"
            }
            """;

    // --- Happy paths ---

    @Test
    void shouldReturnAllTripsNewestFirst() throws Exception {
        TripResponse newer = new TripResponse(2L, LocalDate.of(2026, 3, 10), null, null,
                new TripResponse.CitySummary(7L, "Valencia", "Valencia", 39.47, -0.376),
                new TripResponse.CountrySummary("ES", "Spain", "🇪🇸"));
        when(tripService.getAllTrips()).thenReturn(List.of(newer, LISBON_TRIP));

        mockMvc.perform(get("/api/trips").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].startDate").value("2026-03-10"))
                // @JsonInclude(NON_NULL): a day trip has no endDate key at all, not "endDate": null
                .andExpect(jsonPath("$[0].endDate").doesNotExist())
                .andExpect(jsonPath("$[0].city.name").value("Valencia"))
                .andExpect(jsonPath("$[0].country.isoCode").value("ES"))
                .andExpect(jsonPath("$[1].id").value(1))
                .andExpect(jsonPath("$[1].endDate").value("2025-08-27"))
                .andExpect(jsonPath("$[1].notes").value("Pastéis de nata"))
                .andExpect(jsonPath("$[1].city.id").value(5))
                .andExpect(jsonPath("$[1].city.latitude").value(38.722))
                .andExpect(jsonPath("$[1].city.longitude").value(-9.139))
                .andExpect(jsonPath("$[1].country.name").value("Portugal"))
                .andExpect(jsonPath("$[1].country.flag").value("🇵🇹"));
    }

    @Test
    void shouldReturnEmptyListWhenNoTrips() throws Exception {
        when(tripService.getAllTrips()).thenReturn(List.of());

        mockMvc.perform(get("/api/trips").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void shouldReturnTripById() throws Exception {
        when(tripService.getTripById(1L)).thenReturn(LISBON_TRIP);

        mockMvc.perform(get("/api/trips/1").header("Api-Version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.startDate").value("2025-08-20"))
                .andExpect(jsonPath("$.city.name").value("Lisbon"))
                .andExpect(jsonPath("$.country.isoCode").value("PT"));
    }

    @Test
    void shouldAddTrip() throws Exception {
        when(tripService.addTrip(any(TripRequest.class))).thenReturn(LISBON_TRIP);

        mockMvc.perform(post("/api/trips").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTripBody()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/trips/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.city.name").value("Lisbon"));

        // The JSON reached the service intact, dates parsed from ISO strings
        ArgumentCaptor<TripRequest> captor = ArgumentCaptor.forClass(TripRequest.class);
        verify(tripService).addTrip(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new TripRequest("Lisbon", "Lisbon", "PT", 38.722, -9.139,
                LocalDate.of(2025, 8, 20), LocalDate.of(2025, 8, 27), "Pastéis de nata"));
    }

    @Test
    void shouldAddDayTripWithoutEndDate() throws Exception {
        when(tripService.addTrip(any(TripRequest.class))).thenReturn(LISBON_TRIP);

        mockMvc.perform(post("/api/trips").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripBodyWithout("endDate")))
                .andExpect(status().isCreated());

        ArgumentCaptor<TripRequest> captor = ArgumentCaptor.forClass(TripRequest.class);
        verify(tripService).addTrip(captor.capture());
        assertThat(captor.getValue().endDate()).isNull();
    }

    @Test
    void shouldAddTripWithoutStateOrNotes() throws Exception {
        when(tripService.addTrip(any(TripRequest.class))).thenReturn(LISBON_TRIP);

        mockMvc.perform(post("/api/trips").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripBodyWithout("state", "notes")))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldAcceptNotesOfExactly2000Characters() throws Exception {
        when(tripService.addTrip(any(TripRequest.class))).thenReturn(LISBON_TRIP);

        mockMvc.perform(post("/api/trips").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripBodyWith("notes", "\"" + "a".repeat(2000) + "\"")))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldUpdateTrip() throws Exception {
        TripResponse updated = new TripResponse(1L, LocalDate.of(2025, 8, 21), LocalDate.of(2025, 8, 28),
                "Went back for more", LISBON, PORTUGAL);
        when(tripService.updateTripById(any(TripUpdateRequest.class), eq(1L))).thenReturn(updated);

        mockMvc.perform(put("/api/trips/1").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2025-08-21"))
                .andExpect(jsonPath("$.endDate").value("2025-08-28"))
                .andExpect(jsonPath("$.notes").value("Went back for more"));

        verify(tripService).updateTripById(
                new TripUpdateRequest(LocalDate.of(2025, 8, 21), LocalDate.of(2025, 8, 28), "Went back for more"), 1L);
    }

    @Test
    void shouldDeleteTrip() throws Exception {
        mockMvc.perform(delete("/api/trips/1").header("Api-Version", "1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(tripService).deleteTripById(1L);
    }

    // --- Validation on POST (400, service never called) ---

    @Test
    void shouldReturn400WhenCityNameMissing() throws Exception {
        expectPostRejected(tripBodyWithout("cityName"), "cityName");
    }

    @Test
    void shouldReturn400WhenCityNameIsBlank() throws Exception {
        expectPostRejected(tripBodyWith("cityName", "\"   \""), "cityName");
    }

    @Test
    void shouldReturn400WhenStateIsBlank() throws Exception {
        expectPostRejected(tripBodyWith("state", "\"  \""), "state");
    }

    @Test
    void shouldReturn400WhenCountryCodeMissing() throws Exception {
        expectPostRejected(tripBodyWithout("countryCode"), "countryCode");
    }

    @Test
    void shouldReturn400WhenCountryCodeIsNotIsoAlpha2() throws Exception {
        expectPostRejected(tripBodyWith("countryCode", "\"deu\""), "countryCode");
    }

    @Test
    void shouldReturn400WhenCoordinatesMissing() throws Exception {
        expectPostRejected(tripBodyWithout("latitude", "longitude"), "latitude", "longitude");
    }

    @Test
    void shouldReturn400WhenCoordinatesOutOfRange() throws Exception {
        Map<String, String> fields = validTripFields();
        fields.put("latitude", "91");
        fields.put("longitude", "181");

        expectPostRejected(json(fields), "latitude", "longitude");
    }

    @Test
    void shouldReturn400WhenCoordinatesBelowRange() throws Exception {
        Map<String, String> fields = validTripFields();
        fields.put("latitude", "-91");
        fields.put("longitude", "-181");

        expectPostRejected(json(fields), "latitude", "longitude");
    }

    @Test
    void shouldReturn400WhenStartDateMissing() throws Exception {
        expectPostRejected(tripBodyWithout("startDate", "endDate"), "startDate");
    }

    @Test
    void shouldReturn400WhenStartDateInFuture() throws Exception {
        // Far enough ahead that the validator's clock (the real one) can't disagree
        LocalDate nextYear = LocalDate.now().plusYears(1);
        Map<String, String> fields = validTripFields();
        fields.put("startDate", "\"" + nextYear + "\"");
        fields.put("endDate", "\"" + nextYear.plusDays(3) + "\"");

        expectPostRejected(json(fields), "startDate");
    }

    @Test
    void shouldReturn400WhenEndDateBeforeStartDate() throws Exception {
        // The class-level @ValidDateRange reports on endDate, so the message names it
        expectPostRejected(tripBodyWith("endDate", "\"2025-08-19\""),
                "endDate: End date must be on or after the start date");
    }

    @Test
    void shouldReturn400WhenNotesTooLong() throws Exception {
        expectPostRejected(tripBodyWith("notes", "\"" + "a".repeat(2001) + "\""), "notes");
    }

    @Test
    void shouldReturn400WhenDateIsMalformed() throws Exception {
        mockMvc.perform(post("/api/trips").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripBodyWith("startDate", "\"12/10/2026\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON request body or invalid data types."));

        verifyNoInteractions(tripService);
    }

    @Test
    void shouldReturn400WhenCountryCodeUnknown() throws Exception {
        // Passes validation (two capital letters) but isn't a seeded country
        when(tripService.addTrip(any(TripRequest.class))).thenThrow(new UnknownCountryCodeException("QQ"));

        mockMvc.perform(post("/api/trips").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripBodyWith("countryCode", "\"QQ\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown country code: QQ"));
    }

    // --- Validation on PUT ---

    @Test
    void shouldReturn400WhenUpdatedStartDateMissing() throws Exception {
        expectPutRejected("""
                { "endDate": "2025-08-28", "notes": "No start" }
                """, "startDate");
    }

    @Test
    void shouldReturn400WhenUpdatedEndDateBeforeStartDate() throws Exception {
        expectPutRejected("""
                { "startDate": "2025-08-21", "endDate": "2025-08-20" }
                """, "endDate: End date must be on or after the start date");
    }

    @Test
    void shouldReturn400WhenUpdatedNotesTooLong() throws Exception {
        expectPutRejected("{ \"startDate\": \"2025-08-21\", \"notes\": \"" + "a".repeat(2001) + "\" }", "notes");
    }

    @Test
    void shouldReturn400WhenIdIsNotANumber() throws Exception {
        mockMvc.perform(get("/api/trips/abc").header("Api-Version", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));

        mockMvc.perform(put("/api/trips/abc").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));

        mockMvc.perform(delete("/api/trips/abc").header("Api-Version", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));

        verifyNoInteractions(tripService);
    }

    // --- Not found ---

    @Test
    void shouldReturn404WhenTripNotFound() throws Exception {
        when(tripService.getTripById(99L)).thenThrow(new EntityNotFoundException("Trip not found with id: 99"));

        mockMvc.perform(get("/api/trips/99").header("Api-Version", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Trip not found with id: 99"));
    }

    @Test
    void shouldReturn404WhenUpdatingMissingTrip() throws Exception {
        when(tripService.updateTripById(any(TripUpdateRequest.class), eq(99L)))
                .thenThrow(new EntityNotFoundException("Trip not found with id: 99"));

        mockMvc.perform(put("/api/trips/99").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Trip not found with id: 99"));
    }

    @Test
    void shouldReturn404WhenDeletingMissingTrip() throws Exception {
        doThrow(new EntityNotFoundException("Trip not found with id: 99")).when(tripService).deleteTripById(99L);

        mockMvc.perform(delete("/api/trips/99").header("Api-Version", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Trip not found with id: 99"));
    }

    // --- Helpers ---

    private void expectPostRejected(String body, String... messageParts) throws Exception {
        var result = mockMvc.perform(post("/api/trips").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        for (String part : messageParts) {
            result.andExpect(jsonPath("$.message", containsString(part)));
        }
        verifyNoInteractions(tripService);
    }

    private void expectPutRejected(String body, String... messageParts) throws Exception {
        var result = mockMvc.perform(put("/api/trips/1").header("Api-Version", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        for (String part : messageParts) {
            result.andExpect(jsonPath("$.message", containsString(part)));
        }
        verifyNoInteractions(tripService);
    }
}
