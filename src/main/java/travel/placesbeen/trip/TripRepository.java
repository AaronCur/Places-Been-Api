package travel.placesbeen.trip;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    // Newest first; id breaks ties so trips on the same day keep a stable order.
    // Fetch each trip's city and its country in the same query, since mapping reads them (avoids N+1)
    @EntityGraph(attributePaths = {"city", "city.country"})
    List<Trip> findAllByOrderByStartDateDescIdDesc();

    //Check if a city has any trips so we can decide to delete a city
    boolean existsByCity_Id(Long cityId);
}
