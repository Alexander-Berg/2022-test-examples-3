CREATE TABLE IF NOT EXISTS entities(
  id BIGSERIAL PRIMARY KEY,
  name TEXT NOT NULL,
  age SMALLINT,
  json_data JSON DEFAULT '{}',
  json_binary_data JSONB DEFAULT '{}',
  generic_json_binary_data JSONB DEFAULT '{}',
  nested_str TEXT,
  nested_num BIGINT
);

CREATE TABLE keys(
    long_key BIGINT NOT NULL,
    uuid_key uuid NOT NULL
);

CREATE TABLE composite_key_table (
    id BIGSERIAL NOT NULL,
    type TEXT NOT NULL,
    data TEXT NOT NULL,
    PRIMARY KEY (id, type)
);
