-- H2 copy of the Postgres V6 for the tests (db/vendor/postgresql). H2 has no expression indexes, so it compares
-- generated upper-case copies of the columns instead. Same rule: name and state are unique per country ignoring case.
-- Keep the two files in step.
ALTER TABLE city
    DROP CONSTRAINT uk_city_name_state_country;

ALTER TABLE city
    ADD COLUMN name_key VARCHAR(255) GENERATED ALWAYS AS (upper(name));

ALTER TABLE city
    ADD COLUMN state_key VARCHAR(255) GENERATED ALWAYS AS (upper(state));

ALTER TABLE city
    ADD CONSTRAINT uk_city_name_state_country UNIQUE NULLS NOT DISTINCT (name_key, state_key, country_id);
