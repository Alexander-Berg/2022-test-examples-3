CREATE OR REPLACE FUNCTION impl.get_blacklist_limit(
) RETURNS integer AS $$
SELECT 3;
$$ LANGUAGE SQL IMMUTABLE;