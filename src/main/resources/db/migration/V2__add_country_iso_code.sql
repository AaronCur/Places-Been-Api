-- Adds the ISO 3166-1 alpha-2 code to country (replacing the free-text flag) and lets the same city name exist in
-- two countries.
--
-- DESTRUCTIVE ON PURPOSE: this deletes every city and country, including on Supabase (baselined at V1, so V2 runs
-- there too). The existing rows were test data without ISO codes, and an empty table is what lets iso_code be added
-- as NOT NULL without a default or backfill. V3 then seeds every country.
DELETE
FROM city;
DELETE
FROM country;
ALTER TABLE country
    ADD COLUMN iso_code VARCHAR(2) NOT NULL;
ALTER TABLE country
    ADD CONSTRAINT uk_country_iso_code UNIQUE (iso_code);
-- Length and upper case only: letters-only ([A-Z]{2}) is enforced by @Pattern in the API, because a regex CHECK
-- isn't written the same way in H2 and Postgres
ALTER TABLE country
    ADD CONSTRAINT ck_country_iso_code CHECK (LENGTH(iso_code) = 2 AND iso_code = UPPER(iso_code));
ALTER TABLE country
    DROP COLUMN flag;
-- Hibernate's generated name for the old unique(name) on city (see V1); confirm it on Supabase before deploying
ALTER TABLE city
    DROP CONSTRAINT ukqsstlki7ni5ovaariyy9u8y79;
