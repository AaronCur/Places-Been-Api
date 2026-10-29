# Places Been API

A personal travel tracker: the cities and countries I've visited (trips coming next).
Spring Boot REST API backed by Postgres. Single user for now, but design so multiple users can be added later.
The Angular 21 frontend lives in a separate repo, `Places-Been-Ui`. Keep API changes backwards compatible within a version, and call out any change the frontend will need.

> This is a **learning project**. I'm a senior Java/Angular developer using it to practise modern
> Spring Boot and Angular features and AI-assisted development. Read "Working with me" before writing any code.

## Stack

- Java 25, Spring Boot 4.0.x (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa`), Maven wrapper
- PostgreSQL 16 via `docker-compose.yml` (host port 5433) locally, Supabase when deployed; schema managed by Flyway
- Lombok on entities only; springdoc-openapi for Swagger UI
- Tests: JUnit 5, `@WebMvcTest` with `MockMvc` and `@MockitoBean`, `@DataJpaTest` and `@SpringBootTest` on in-memory
  H2 (`src/test/resources/application.yaml`, PostgreSQL mode); Flyway migrations run there too

Spring Boot 4 / Spring Framework 7 / Jackson 3 APIs differ from older versions. If you're unsure whether an API
exists or has moved in these versions, say so and check before using it.

## Commands

```bash
docker compose up -d        # start Postgres on localhost:5433 (needs DB_USERNAME / DB_PASSWORD)
./mvnw spring-boot:run      # API on :8080, Swagger UI at /swagger-ui.html
./mvnw test                 # tests, on in-memory H2 (no database needed)
```

The app reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `CORS_ALLOWED_ORIGINS` from the environment. Locally,
point `DB_URL` at `jdbc:postgresql://localhost:5433/places_been_db`; never run experiments against Supabase.

## Structure

Package by feature under `travel.placesbeen`:

```
city/      City entity, CityRepository, CityService, CityController, CityRequest, CityResponse
country/   Country entity, repository, service, controller, CountryResponse (read-only API)
stats/     summary counts
config/    CorsConfig
common/exception/   GlobalExceptionHandler, ErrorResponse
```

## Reference implementation

`city/` was written by hand and is **the pattern to follow** for new features: controller → service → repository,
request/response records, mapping in a private `mapToResponse` in the service, and `CityControllerTest` for tests.
If the pattern seems wrong for a new case, say so; don't silently diverge or "fix" `city/` unless asked.

## Conventions

- Constructor injection only.
- Request and response DTOs are Java `record`s. Entities never leave the service layer.
- Entities use Lombok `@Getter`/`@Setter`/`@NoArgsConstructor`; no Lombok on records.
- Validation with Bean Validation annotations on request records and `@Valid` in controllers.
- Not found → `EntityNotFoundException` (404). Duplicates → 409. All errors go through `GlobalExceptionHandler`.
- Every endpoint is versioned with Spring Framework 7 API versioning: `@GetMapping(version = "1")` etc.
  The version is read from a request header (see `spring.mvc.apiversion` in `application.yaml`).
- REST: plural nouns (`/api/cities`), `201` + `Location` on create, `204` on delete.
- Tag each controller with `@Tag` for Swagger.
- Watch for N+1 queries when returning related data; use a fetch join or `@EntityGraph`.
- Schema changes go in a new Flyway migration, `src/main/resources/db/migration/V<n>__<description>.sql`.
  Hibernate only validates (`ddl-auto: validate`). Never edit a migration that has already run anywhere;
  add a new one. Existing databases (Supabase) were baselined at V1.

## Testing

- Every controller gets a `@WebMvcTest` test in the style of `CityControllerTest`, covering happy paths AND
  validation errors, 404s and 409s.
- Service logic gets plain unit tests.
- Tests are part of every change, not a follow-up.

## Working with me

1. **Plan first.** For any feature, propose the entities, endpoints, DTOs and test list, then stop and wait for my OK.
2. **Small changes.** One feature or one layer per change, so I can review the diff like a pull request.
3. **Explain decisions.** After implementing, list the 3–5 non-obvious choices you made and the alternatives you rejected.
4. **Hand-code mode.** If I mark a task `[hand-code]`, do NOT write the implementation. Instead review my approach,
   ask questions, point me to the relevant Spring concept, and review my code when I share it.
5. **Quiz me.** When I say "quiz me", ask me questions about the code just written instead of explaining it.
6. Don't add dependencies, change the build or change these conventions without asking.
7. Run the tests before saying something is done, and report the results.
8. Commit only when I ask.

## Planned changes (not done yet; don't assume they exist)

- Migrate `ErrorResponse` to RFC 9457 `ProblemDetail`.
- Testcontainers for repository and `@SpringBootTest` tests.
- Add a `Trip` feature (being hand-coded).
- Decide whether to seed countries from ISO 3166 instead of creating them when a city is added.
