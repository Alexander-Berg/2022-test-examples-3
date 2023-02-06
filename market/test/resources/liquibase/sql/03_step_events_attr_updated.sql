ALTER TABLE step_events ALTER COLUMN  destination SET DEFAULT 'clickhouse'::character varying;

ALTER TABLE step_events ADD COLUMN size_updated_at timestamp without time zone;
