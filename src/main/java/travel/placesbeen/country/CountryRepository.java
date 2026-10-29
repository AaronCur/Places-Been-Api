package travel.placesbeen.country;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {
    Optional<Country> findByIsoCode(String isoCode);

    // Visited = has at least one city. Cities are fetched in the same query to avoid N+1 when they're mapped.
    @EntityGraph(attributePaths = "cities")
    List<Country> findByCitiesIsNotEmptyOrderByNameAsc();

    long countByCitiesIsNotEmpty();
}
