package travel.placesbeen.country;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import travel.placesbeen.city.City;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Country {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true, length = 2)
    private String isoCode;

    @OneToMany(mappedBy = "country")
    private List<City> cities = new ArrayList<>();

    // Helper constructor for the Service logic
    public Country(String isoCode, String name) {
        this.name = name;
        this.isoCode = isoCode;
    }


}
