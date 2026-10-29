---
name: scaffold-feature
description: Scaffold empty outlines (entity, repository, service, controller, request/response records, tests) for a new feature package in Places Been API, following the city/ reference pattern. Outlines only, no business logic, so the user can hand-code the implementation.
argument-hint: <feature> [field:Type ...]   e.g. trip startDate:LocalDate endDate:LocalDate? notes:String? city:City
disable-model-invocation: true
---

# Scaffold a feature (outlines only)

The user is hand-coding this feature to learn Spring Boot. Your job is to remove the typing, **not** the thinking:
create compiling skeletons that follow the `city/` reference pattern, then stop. Never fill in business logic,
queries, mapping code or test assertions, even if it seems quicker. If the user later asks for help, review and
guide, per "Hand-code mode" in `CLAUDE.md`.

## 1. Inputs

`$ARGUMENTS` is `<feature> [field:Type ...]`.

- `<feature>`: singular, lower case (`trip`). It gives the package `travel.placesbeen.<feature>`, the class prefix
  `<Feature>` (`Trip`) and the path `/api/<plural>` (`/api/trips`).
- Fields: `name:Type`. A trailing `?` means optional (nullable column, no `@NotNull`/`@NotBlank` on the request).
  A type that is an existing entity (`City`, `Country`) means a relationship.
- No fields given: read the feature's section in the current plan or ask the user for the field list. Don't guess.
- If `src/main/java/travel/placesbeen/<feature>/` already exists, stop and ask; never overwrite.

## 2. Read the reference first

Read these and mirror their style exactly (naming, annotations, imports, comment density):
`city/City.java`, `CityRepository.java`, `CityService.java`, `CityController.java`, `CityRequest.java`,
`CityResponse.java`, `src/test/java/travel/placesbeen/city/CityControllerTest.java`.
Conventions are in `CLAUDE.md`. Spring Boot 4 / Jackson 3 import paths differ from older versions; copy them
from the existing files rather than from memory.

## 3. Files to create

All under `src/main/java/travel/placesbeen/<feature>/` (tests under `src/test/java/...`):

| File | Contents |
|---|---|
| `<Feature>.java` | `@Entity`, `@Table(name = "<feature>")`, Lombok `@Getter @Setter @NoArgsConstructor`, `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id`. One field per spec with `@Column(nullable = ...)`. **Relationships:** declare the field with a `// TODO:` comment above it asking the user to choose the mapping (`@ManyToOne`? fetch type? cascade? join column?), with no mapping annotations. |
| `<Feature>Repository.java` | `@Repository interface <Feature>Repository extends JpaRepository<<Feature>, Long>` with no query methods. Add a `// TODO:` listing the queries the plan needs (e.g. "newest first with a fetch join", "count by city"), as comments only. |
| `<Feature>Request.java` | `record` with the request fields and Bean Validation where the type makes it obvious (`@NotBlank` String, `@NotNull` required non-String). Anything domain-specific (ranges, patterns, cross-field rules) is a `// TODO:` comment. |
| `<Feature>Response.java` | `record` with `id` plus the response fields, `@JsonInclude(JsonInclude.Include.NON_NULL)` like `CityResponse`. Nested objects are `// TODO:` comments if the shape is undecided. |
| `<Feature>Service.java` | `@Service`, constructor injection of the repository (plus other repositories the plan names). One public method per endpoint with the right signature and `@Transactional` where `CityService` uses it. Body: `throw new UnsupportedOperationException("TODO: <one line of what it must do>");`. Include an empty `private <Feature>Response mapToResponse(<Feature> entity)` that throws the same way. |
| `<Feature>Controller.java` | `@RestController`, `@RequestMapping("/api/<plural>")`, `@Tag(name = "<Feature> API", description = "API for managing <plural>")`, constructor injection. Full handler signatures for GET all, GET by id, POST, PUT, DELETE (only those the plan lists) with `version = "1"`, `@Valid @RequestBody`, `@PathVariable`. Body: `throw new UnsupportedOperationException("TODO: ...");` with the expected status in the message (`201 + Location`, `204`, ...). |
| `<Feature>ControllerTest.java` | `@WebMvcTest(<Feature>Controller.class)`, `MockMvc`, `@MockitoBean` service, like `CityControllerTest`. One **empty** test method per case, each `@Disabled("TODO")` so the build stays green: happy path per endpoint, validation 400 per required field, 400 for a non-numeric id, 404 on get/put/delete of a missing id, 409 if duplicates are possible. Body: a `// given / when / then` comment describing the expectation in one line each. |
| `<Feature>ServiceTest.java` | Plain JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`). Empty `@Disabled("TODO")` methods for each rule in the plan (e.g. "delete removes city when last trip"). |

Do **not** edit existing files (`GlobalExceptionHandler`, other features, `application.yaml`, `pom.xml`). If the
feature will need changes there, list them in the summary instead.

## 4. Check it compiles

Run `./mvnw -q compile test-compile` (no database needed). Fix only scaffolding mistakes (imports, typos).
Don't run the full test suite: `contextLoads` needs Postgres.

## 5. Report and stop

Reply with:
1. The files created (clickable paths).
2. A **TODO checklist in a sensible implementation order** (entity mapping → repository queries → service →
   controller → tests), each item pointing to the relevant Spring concept to read up on (e.g. "`@ManyToOne(fetch =
   LAZY)` and why LAZY", "derived query vs `@Query` + `JOIN FETCH`", "custom class-level constraint").
3. Changes needed in files you didn't touch (exception handler, other features).
4. A reminder that the user writes the code and can ask for a review (the `springboot-reviewer` agent) when ready.

Don't commit.
