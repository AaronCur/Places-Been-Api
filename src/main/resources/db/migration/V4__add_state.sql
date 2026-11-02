ALTER TABLE City
    ADD COLUMN state VARCHAR(255);
ALTER TABLE City
    DROP CONSTRAINT uktot9ajsfwuxb6345nkdfk3qxj;
ALTER TABLE City
    ADD CONSTRAINT uk_city_name_state_country UNIQUE NULLS NOT DISTINCT (name, state, country_id);
