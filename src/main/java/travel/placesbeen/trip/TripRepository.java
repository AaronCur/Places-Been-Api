package travel.placesbeen.trip;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    // Newest first; id breaks ties so trips on the same day keep a stable order.
    // Fetch each trip's city and its country in the same query, since mapping reads them (avoids N+1)
    @EntityGraph(attributePaths = {"city", "city.country"})
    List<Trip> findAllByOrderByStartDateDescIdDesc();

    // One trip with its city and country in a single query, for building a full response.
    // findById stays for callers that only need the trip (e.g. delete reads just the city's id)
    @EntityGraph(attributePaths = {"city", "city.country"})
    Optional<Trip> findWithCityById(Long id);

    //Check if a city has any trips so we can decide to delete a city
    boolean existsByCity_Id(Long cityId);
}
