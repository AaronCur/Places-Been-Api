package travel.placesbeen.city;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import travel.placesbeen.country.Country;

@Entity
@Table(name = "city", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "country_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE}) // Optimized for performance
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    // Standard constructor for easy testing/service use
    public City(String name, Double latitude, Double longitude, Country country) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.country = country;
    }

}
