CREATE TABLE step_events (
    event_step_id character varying(128) NOT NULL PRIMARY KEY,
    cluster character varying(128) NOT NULL,
    event_name character varying(128) NOT NULL,
    path character varying(2048) NOT NULL,
    partition bigint,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    step_created_at timestamp without time zone NOT NULL,
    loaded boolean DEFAULT false,
    loaded_at timestamp without time zone,
    data_rejected boolean DEFAULT false,
    data_rejected_at timestamp without time zone,
    manually_rejected boolean DEFAULT false NOT NULL,
    destination character varying(128) DEFAULT 'vertica'::character varying NOT NULL,
    retry_count integer DEFAULT 0 NOT NULL,
    last_error text,
    last_status character varying(128) DEFAULT 'UNKNOWN'::character varying NOT NULL,
    rejected_reason text,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    priority character varying(128),
    rows bigint,
    size bigint,
    raw_event jsonb
);

CREATE INDEX step_created_at_idx ON step_events (step_created_at);

