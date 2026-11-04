-- The dataset's state/region name, so two cities with one name in one country (Portland OR / ME) can both exist.
-- Null where the dataset has no state (e.g. Gibraltar).
ALTER TABLE city
    ADD COLUMN state VARCHAR(255);

-- Replaces unique(name, country_id) from V2
ALTER TABLE city
    DROP CONSTRAINT uktot9ajsfwuxb6345nkdfk3qxj;

-- NULLS NOT DISTINCT (Postgres 15+): without it two "Gibraltar" rows with a null state would both be allowed.
-- "Portland" with no state alongside "Portland, Oregon" is still allowed, because null and 'Oregon' differ.
ALTER TABLE city
    ADD CONSTRAINT uk_city_name_state_country UNIQUE NULLS NOT DISTINCT (name, state, country_id);
