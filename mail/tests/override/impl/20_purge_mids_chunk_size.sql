CREATE OR REPLACE FUNCTION impl.purge_mids_chunk_size(
) RETURNS bigint AS $$
SELECT 3::bigint;
$$ LANGUAGE SQL IMMUTABLE;