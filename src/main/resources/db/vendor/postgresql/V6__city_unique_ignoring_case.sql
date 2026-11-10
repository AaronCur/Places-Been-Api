-- The service matches cities ignoring case ("paris" finds "Paris"), but uk_city_name_state_country compared exact text,
-- so "paris" and "Paris" could both be stored (e.g. two requests at once), and the next case-insensitive lookup then
-- found two rows and failed with a 500. Make the database compare the same way the lookups do.
-- upper() rather than lower(): Spring Data's IgnoreCase queries compare upper(...) = upper(...), so this matches them exactly.
-- Fails if the table already holds two cities differing only in case: merge those first.
ALTER TABLE city
    DROP CONSTRAINT uk_city_name_state_country;

-- An expression index, not a constraint: UNIQUE constraints only take plain columns.
-- NULLS NOT DISTINCT as in V4, so two "Gibraltar" rows with no state still clash.
CREATE UNIQUE INDEX uk_city_name_state_country ON city (upper(name), upper(state), country_id) NULLS NOT DISTINCT;
