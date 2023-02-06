CREATE OR REPLACE FUNCTION impl.get_chain_size(
) RETURNS smallint AS $$
SELECT 3::smallint;
$$ LANGUAGE SQL IMMUTABLE;
