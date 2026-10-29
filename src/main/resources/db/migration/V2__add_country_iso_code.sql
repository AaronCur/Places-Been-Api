DELETE
FROM city;
DELETE
FROM country;
ALTER TABLE country
    ADD COLUMN iso_code VARCHAR(2) NOT NULL;
ALTER TABLE country
    ADD CONSTRAINT uk_country_iso_code UNIQUE (iso_code);
ALTER TABLE country
    ADD CONSTRAINT ck_country_iso_code CHECK (LENGTH(iso_code) = 2 AND iso_code = UPPER(iso_code));
ALTER TABLE country
    DROP COLUMN flag;
ALTER TABLE city
    DROP CONSTRAINT ukqsstlki7ni5ovaariyy9u8y79;
