-- trimmed "pg_dump -s ..." output

-- for executions

DROP TABLE cached_variant_checks IF EXISTS;
DROP TABLE variant_availability_checks IF EXISTS;

CREATE TABLE cached_variant_checks (
    partner_id character varying(256) NOT NULL,
    variant_id character varying(256) NOT NULL,
    expires_at timestamp without time zone,
    check_id uuid
);

CREATE TABLE public.variant_availability_checks (
    id uuid NOT NULL,
    data other,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    state character varying(128)
);

ALTER TABLE cached_variant_checks
    ADD CONSTRAINT pk_cached_variant_checks PRIMARY KEY (partner_id, variant_id);

ALTER TABLE variant_availability_checks
    ADD CONSTRAINT pk_variant_availability_checks PRIMARY KEY (id);


ALTER TABLE cached_variant_checks
    ADD CONSTRAINT cached_variant_checks_check_fk FOREIGN KEY (check_id) REFERENCES variant_availability_checks(id);
