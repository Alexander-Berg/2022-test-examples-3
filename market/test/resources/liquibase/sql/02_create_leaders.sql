-- table for logging and monitorings
-- on takeover leader inserts a row
create table leaders (
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    hostname TEXT NOT NULL
);

CREATE INDEX leaders_created_at_idx ON leaders (created_at);
