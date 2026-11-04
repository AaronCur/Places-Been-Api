package travel.placesbeen.trip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {
    // TODO: queries the plan needs:
    //  - all trips newest first (by startDate, then id so same-day trips keep a stable order), fetching each
    //    trip's city and its country in the same query (avoids N+1 when mapping)
    //  - count the trips of one city, for the delete cascade
}
