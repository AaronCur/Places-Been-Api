package travel.placesbeen.city;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    // Fetch each city's country in the same query, since mapping reads it (avoids N+1)
    @EntityGraph(attributePaths = "country")
    List<City> findAllByOrderByIdAsc();

    boolean existsByNameIgnoreCaseAndCountry_IsoCode(String cityName, String isoCode);

}
