CREATE OR REPLACE FUNCTION cast_code_mail_attaches_to_store(
    i_attaches mail.attach[]
) RETURNS code.store_attach[] AS $$
SELECT ARRAY(
    SELECT (a.hid,
            a.type,
            a.filename,
            a.size)::code.store_attach
      FROM unnest($1) AS a);
$$ LANGUAGE SQL IMMUTABLE RETURNS NULL ON NULL INPUT;
