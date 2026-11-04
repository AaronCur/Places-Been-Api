package travel.placesbeen.trip;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import travel.placesbeen.city.City;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "trip")
@Getter
@Setter
@NoArgsConstructor
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;

    @Column(nullable = false)
    private LocalDate startDate;

    // Null for a day trip
    private LocalDate endDate;

    @Column(length = 2000)
    private String notes;

    // TODO: set by the service from an injected Clock, so tests can pin it
    @Column(nullable = false)
    private Instant createdAt;

    public Trip(City city, LocalDate startDate, LocalDate endDate, String notes, Instant createdAt) {
        this.city = city;
        this.startDate = startDate;
        this.endDate = endDate;
        this.notes = notes;
        this.createdAt = createdAt;
    }
}
