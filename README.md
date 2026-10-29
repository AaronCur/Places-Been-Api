# Places-Been

A REST API for keeping track of the places you've travelled to. Record the cities you've visited (with coordinates), and
the API groups them by country and gives you summary stats — ready to back a map or travel-log frontend.

## Contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [API reference](#api-reference)
- [Error handling](#error-handling)
- [Testing](#testing)
- [Docker](#docker)

## Features

- **City CRUD** – add, list, fetch, update and delete visited cities with latitude/longitude.
- **Automatic countries** – countries are created on the fly when you add a city, and removed when their last city is
  deleted. Country matching is case-insensitive.
- **Duplicate protection** – the same city can't be added twice to the same country.
- **Stats** – total countries and cities visited.
- **Versioned API** – endpoints are versioned via the `Api-Version` request header (defaults to `1`).
- **Consistent errors** – every error returns the same JSON shape.
- **OpenAPI docs** – interactive Swagger UI out of the box.

## Tech stack

| Concern     | Technology                                              |
|-------------|---------------------------------------------------------|
| Language    | Java 25                                                 |
| Framework   | Spring Boot 4 (Web MVC, Data JPA, Validation, Actuator) |
| Database    | PostgreSQL 16 (H2 in-memory for tests)                  |
| API docs    | springdoc-openapi (Swagger UI)                          |
| Build       | Maven (wrapper included)                                |
| Boilerplate | Lombok                                                  |
| Containers  | Docker, Docker Compose                                  |

## Project structure

```
src/main/java/travel/placesbeen
├── PlacesBeenApplication.java      # Entry point
├── city/                           # City entity, controller, service, repository, DTOs
├── country/                        # Country entity, controller, service, repository, DTOs
├── stats/                          # Summary statistics endpoint
├── common/exception/               # GlobalExceptionHandler + ErrorResponse
└── config/CorsConfig.java          # CORS for the frontend (disabled in the test profile)
```

Each feature package follows the same layering: **Controller → Service → Repository**, with request/response records (
`CityRequest`, `CityResponse`, …) so JPA entities are never exposed directly.

### Data model

```
Country (id, name [unique], flag)
   └── 1 ── * City (id, name, latitude, longitude, country_id)
```

## Getting started

### Prerequisites

- JDK 25
- Docker (for the PostgreSQL database)

### 1. Start PostgreSQL

`docker-compose.yml` runs Postgres 16 and exposes it on **port 5433**:

```bash
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
docker compose up -d
```

### 2. Run the API

```bash
export DB_URL=jdbc:postgresql://localhost:5433/places_been_db
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export CORS_ALLOWED_ORIGINS=http://localhost:4200

./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

The API starts on <http://localhost:8080>. The schema is created/updated automatically by Hibernate (
`ddl-auto: update`).

### 3. Explore

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI spec: <http://localhost:8080/v3/api-docs>
- Health check: <http://localhost:8080/actuator/health>

## Configuration

All environment-specific settings come from environment variables:

| Variable               | Description                                   | Example                                           |
|------------------------|-----------------------------------------------|---------------------------------------------------|
| `DB_URL`               | JDBC URL of the PostgreSQL database           | `jdbc:postgresql://localhost:5433/places_been_db` |
| `DB_USERNAME`          | Database user                                 | `postgres`                                        |
| `DB_PASSWORD`          | Database password                             | `postgres`                                        |
| `CORS_ALLOWED_ORIGINS` | Origin allowed to call the API (frontend URL) | `http://localhost:4200`                           |

## API reference

All endpoints accept an optional `Api-Version` header. Only version `1` exists today, and it is the default.

### Cities — `/api/cities`

| Method   | Path               | Description                                            | Success                   |
|----------|--------------------|--------------------------------------------------------|---------------------------|
| `GET`    | `/api/cities`      | List all cities (by id)                                | `200`                     |
| `GET`    | `/api/cities/{id}` | Get a city                                             | `200`                     |
| `POST`   | `/api/cities`      | Add a city (creates country if needed)                 | `201` + `Location` header |
| `PUT`    | `/api/cities/{id}` | Update a city's name/coordinates                       | `200`                     |
| `DELETE` | `/api/cities/{id}` | Delete a city (and its country if it was the last one) | `204`                     |

**Request body** (`POST` / `PUT`) — all fields required:

```json
{
  "name": "Berlin",
  "country": "Germany",
  "latitude": 52.52,
  "longitude": 13.405
}
```

**Response:**

```json
{
  "id": 1,
  "name": "Berlin",
  "latitude": 52.52,
  "longitude": 13.405,
  "country": "Germany",
  "countryFlag": "🇩🇪"
}
```

Example:

```bash
curl -X POST http://localhost:8080/api/cities \
  -H "Content-Type: application/json" \
  -H "Api-Version: 1" \
  -d '{"name":"Berlin","country":"Germany","latitude":52.52,"longitude":13.405}'
```

### Countries — `/api/countries` (read-only)

| Method | Path                                | Description                       |
|--------|-------------------------------------|-----------------------------------|
| `GET`  | `/api/countries`                    | List countries                    |
| `GET`  | `/api/countries?includeCities=true` | List countries with nested cities |
| `GET`  | `/api/countries/{id}`               | Get a country                     |

```json
[
  {
    "id": 1,
    "name": "Germany",
    "flag": "🇩🇪",
    "cities": [
      {
        "id": 1,
        "name": "Berlin",
        "latitude": 52.52,
        "longitude": 13.405
      }
    ]
  }
]
```

Null fields (e.g. `flag`, or `cities` when not requested) are omitted from responses.

### Stats — `/api/stats`

| Method | Path                 | Description                        |
|--------|----------------------|------------------------------------|
| `GET`  | `/api/stats/summary` | Total countries and cities visited |

```json
{
  "totalCountries": 2,
  "totalCities": 5
}
```

## Error handling

Errors are handled centrally in `GlobalExceptionHandler` and always look like this:

```json
{
  "timestamp": "2026-10-15T14:19:21.72",
  "status": 404,
  "error": "Not Found",
  "message": "City not found with id: 99",
  "path": "/api/cities/99"
}
```

| Status | When                                                                   |
|--------|------------------------------------------------------------------------|
| `400`  | Validation failure, malformed JSON, missing or wrongly-typed parameter |
| `404`  | City or country doesn't exist                                          |
| `405`  | HTTP method not supported on that endpoint                             |
| `409`  | Duplicate city, or a database constraint violation                     |
| `500`  | Anything unexpected (internal details are never leaked)                |

## Testing

```bash
./mvnw test
```

Tests run against an in-memory H2 database (`src/test/resources/application.yaml`), so no Postgres is needed.

| Test class                   | Type                             | Covers                                                  |
|------------------------------|----------------------------------|---------------------------------------------------------|
| `CityServiceTest`            | Unit (Mockito)                   | Add/update/delete logic, country auto-create & cleanup  |
| `CountryServiceTest`         | Unit (Mockito)                   | Country listing with/without nested cities              |
| `StatsServiceTest`           | Unit (Mockito)                   | Summary counts                                          |
| `CityControllerTest`         | Web slice (`@WebMvcTest`)        | Endpoints, status codes, validation and error responses |
| `CountryControllerTest`      | Web slice (`@WebMvcTest`)        | Endpoints, query params, error responses                |
| `StatsControllerTest`        | Web slice (`@WebMvcTest`)        | Summary endpoint                                        |
| `CityRepositoryTest`         | JPA slice (`@DataJpaTest`)       | Derived queries against a real (H2) database            |
| `PlacesBeenIntegrationTest`  | Full context (`@SpringBootTest`) | End-to-end flows through HTTP and the database          |
| `PlacesBeenApplicationTests` | Smoke                            | Application context loads                               |

## Docker

Build and run the API as a container (multi-stage build: JDK to compile, JRE to run):

```bash
docker build -t places-been .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5433/places_been_db \
  -e DB_USERNAME=postgres \
  -e DB_PASSWORD=postgres \
  -e CORS_ALLOWED_ORIGINS=http://localhost:4200 \
  places-been
```

> `docker-compose.yml` currently only starts the database; the API is run separately as shown above.
