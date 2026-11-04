package travel.placesbeen.city;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import travel.placesbeen.country.Country;

@Entity
@Table(name = "city")
@Getter
@Setter
@NoArgsConstructor
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Optional: the dataset's state/region, null where there is none. Part of the city's uniqueness (see V4)
    private String state;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    // No cascade: countries are seeded reference data, so a city may only point at an existing one
    @ManyToOne(fetch = FetchType.LAZY) // Optimized for performance
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    // Standard constructor for easy testing/service use
    public City(String name, String state, Double latitude, Double longitude, Country country) {
        this.name = name;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
        this.country = country;
    }

}
