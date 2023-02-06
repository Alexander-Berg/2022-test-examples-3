--
-- PostgreSQL database dump
--

-- Dumped from database version 10.21 (Ubuntu 10.21-201)
-- Dumped by pg_dump version 10.21 (Ubuntu 10.21-0ubuntu0.18.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', 'public', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pg_repack; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS pg_repack WITH SCHEMA public;


--
-- Name: EXTENSION pg_repack; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION pg_repack IS 'Reorganize tables in PostgreSQL databases with minimal locks';


--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS plpgsql WITH SCHEMA pg_catalog;


--
-- Name: EXTENSION plpgsql; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION plpgsql IS 'PL/pgSQL procedural language';


--
-- Name: citext; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS citext WITH SCHEMA public;


--
-- Name: EXTENSION citext; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION citext IS 'data type for case-insensitive character strings';


--
-- Name: postgres_fdw; Type: EXTENSION; Schema: -; Owner:
--

CREATE EXTENSION IF NOT EXISTS postgres_fdw WITH SCHEMA public;


--
-- Name: EXTENSION postgres_fdw; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION postgres_fdw IS 'foreign-data wrapper for remote PostgreSQL servers';


--
-- Name: footgun(text, text); Type: FUNCTION; Schema: public; Owner: crypta
--

CREATE FUNCTION public.footgun(_schema text, _parttionbase text) RETURNS void
    LANGUAGE plpgsql
    AS $$
DECLARE
    row     record;
BEGIN
    FOR row IN
        SELECT
            table_schema,
            table_name
        FROM
            information_schema.tables
        WHERE
            table_type = 'BASE TABLE'
        AND
            table_schema = _schema
        AND
            table_name ILIKE (_parttionbase || '%')
    LOOP
        EXECUTE 'DROP TABLE ' || quote_ident(row.table_schema) || '.' || quote_ident(row.table_name) || ' CASCADE';
        RAISE INFO 'Dropped table: %', quote_ident(row.table_schema) || '.' || quote_ident(row.table_name);
    END LOOP;
END;
$$;


ALTER FUNCTION public.footgun(_schema text, _parttionbase text) OWNER TO crypta;

--
-- Name: get_heap_bloat_info(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.get_heap_bloat_info() RETURNS TABLE(current_database name, schemaname name, tblname name, real_size text, extra_size text, extra_ratio double precision, fillfactor integer, bloat_size text, bloat_size_bytes bigint, bloat_ratio double precision, is_na boolean)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN
RETURN QUERY WITH s AS (
SELECT
        tbl.oid AS tblid,
	ns.nspname AS schemaname,
	tbl.relname AS tblname,
	tbl.reltuples,
        tbl.relpages AS heappages,
	COALESCE(toast.relpages, 0) AS toastpages,
        COALESCE(toast.reltuples, 0) AS toasttuples,
        COALESCE(substring(array_to_string(tbl.reloptions, ' ')
          FROM '%fillfactor=#"__#"%' FOR '#')::smallint, 100) AS fillfactor,
        current_setting('block_size')::numeric AS bs,
        CASE WHEN version()~'mingw32' OR version()~'64-bit|x86_64|ppc64|ia64|amd64'
	THEN 8
	ELSE 4
	END AS ma,
        24 AS page_hdr,
        23 + CASE WHEN MAX(coalesce(null_frac,0)) > 0
	THEN ( 7 + count(*) ) / 8
	ELSE 0::int
	END
          + CASE WHEN tbl.relhasoids
	THEN 4
	ELSE 0
	END AS tpl_hdr_size,
        sum( (1-coalesce(s.null_frac, 0)) * coalesce(s.avg_width, 1024) ) AS tpl_data_size,
        bool_or(att.atttypid = 'pg_catalog.name'::regtype) AS is_na
FROM pg_attribute AS att
        JOIN pg_class AS tbl ON att.attrelid = tbl.oid
        JOIN pg_namespace AS ns ON ns.oid = tbl.relnamespace
        JOIN pg_stats AS s ON s.schemaname=ns.nspname
          AND s.tablename = tbl.relname AND s.inherited=false AND s.attname=att.attname
        LEFT JOIN pg_class AS toast ON tbl.reltoastrelid = toast.oid
WHERE att.attnum > 0 AND NOT att.attisdropped
        AND tbl.relkind = 'r'
GROUP BY
	1,2,3,4,5,6,7,8,9,10, tbl.relhasoids
ORDER BY
	5 DESC
), s2 AS (
SELECT
      	( 4 + tpl_hdr_size + tpl_data_size + (2*ma)
        - CASE WHEN tpl_hdr_size%ma = 0
	THEN ma
	ELSE tpl_hdr_size%ma
	END
        - CASE WHEN ceil(tpl_data_size)::int%ma = 0
	THEN ma
	ELSE ceil(tpl_data_size)::int%ma
	END
      	) AS tpl_size,
	bs - page_hdr AS size_per_block,
	(heappages + toastpages) AS tblpages,
	heappages,
      	toastpages,
	reltuples,
	toasttuples,
	bs,
	page_hdr,
	tblid,
	s.schemaname,
	s.tblname,
	s.fillfactor,
	s.is_na
FROM s
), s3 AS (
SELECT
	ceil( reltuples / ( (bs-page_hdr)/tpl_size ) ) + ceil( toasttuples / 4 ) AS est_tblpages,
    	ceil( reltuples / ( (bs-page_hdr)*s2.fillfactor/(tpl_size*100) ) ) + ceil( toasttuples / 4 ) AS est_tblpages_ff,
    	s2.tblpages,
    	s2.fillfactor,
    	s2.bs,
    	s2.tblid,
    	s2.schemaname,
    	s2.tblname,
    	s2.heappages,
    	s2.toastpages,
    	s2.is_na
FROM s2
) SELECT
	current_database(),
	s3.schemaname,
	s3.tblname,
	pg_size_pretty(bs*s3.tblpages) AS real_size,
	pg_size_pretty(((s3.tblpages-est_tblpages)*bs)::bigint) AS extra_size,
  	CASE WHEN tblpages - est_tblpages > 0
    	THEN 100 * (s3.tblpages - est_tblpages)/s3.tblpages::float
    	ELSE 0
  	END AS extra_ratio,
	s3.fillfactor,
	pg_size_pretty(((s3.tblpages-est_tblpages_ff)*bs)::bigint) AS bloat_size,
  	((tblpages-est_tblpages_ff)*bs)::bigint bytes_bloat_size,
  	CASE WHEN s3.tblpages - est_tblpages_ff > 0
    	THEN 100 * (s3.tblpages - est_tblpages_ff)/s3.tblpages::float
    	ELSE 0
  END AS bloat_ratio, s3.is_na
  FROM s3;

END;
$$;


ALTER FUNCTION public.get_heap_bloat_info() OWNER TO postgres;

--
-- Name: get_index_bloat_info(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.get_index_bloat_info() RETURNS TABLE(dbname name, schema_name name, table_name name, index_name name, bloat_pct numeric, bloat_bytes numeric, bloat_size text, total_bytes numeric, index_size text, table_bytes bigint, table_size text, index_scans bigint)
    LANGUAGE plpgsql SECURITY DEFINER
    AS $$
BEGIN

RETURN QUERY WITH btree_index_atts AS (
    SELECT
	nspname,
	relname,
	reltuples,
	relpages,
	indrelid,
	relam,
        regexp_split_to_table(indkey::text, ' ')::smallint AS attnum,
        indexrelid as index_oid
    FROM pg_index
    JOIN pg_class ON pg_class.oid=pg_index.indexrelid
    JOIN pg_namespace ON pg_namespace.oid = pg_class.relnamespace
    JOIN pg_am ON pg_class.relam = pg_am.oid
    WHERE pg_am.amname = 'btree'
    ),
index_item_sizes AS (
    SELECT
    	i.nspname,
    	i.relname,
	i.reltuples,
	i.relpages,
	i.relam,
    	s.starelid,
	a.attrelid AS table_oid,
	index_oid,
    	current_setting('block_size')::numeric AS bs,
    	/* MAXALIGN: 4 on 32bits, 8 on 64bits (and mingw32 ?) */
    	CASE
        WHEN version() ~ 'mingw32' OR version() ~ '64-bit'
	THEN 8
        ELSE 4
    	END AS maxalign,
    	24 AS pagehdr,
    	/* per tuple header: add index_attribute_bm if some cols are null-able */
    	CASE WHEN max(coalesce(s.stanullfrac,0)) = 0
	THEN 2
	ELSE 6
    	END AS index_tuple_hdr,
    	/* data len: we remove null values save space using it fractionnal part from stats */
    	SUM( (1-coalesce(s.stanullfrac, 0)) * coalesce(s.stawidth, 2048) ) AS nulldatawidth
    	FROM pg_attribute AS a
    	JOIN pg_statistic AS s ON s.starelid=a.attrelid AND s.staattnum = a.attnum
    	JOIN btree_index_atts AS i ON i.indrelid = a.attrelid AND a.attnum = i.attnum
    	WHERE
		a.attnum > 0
    	GROUP BY
		1, 2, 3, 4, 5, 6, 7, 8, 9
),
index_aligned AS (
    SELECT
	maxalign,
	bs,
	nspname,
	relname AS index_name,
	reltuples,
        relpages,
	relam,
	table_oid,
	index_oid,
      	( 2 + maxalign -
	CASE /* Add padding to the index tuple header to align on MAXALIGN */
            WHEN index_tuple_hdr%maxalign = 0
	    THEN maxalign
            ELSE index_tuple_hdr%maxalign
          END
        + nulldatawidth + maxalign -
	CASE /* Add padding to the data to align on MAXALIGN */
            WHEN nulldatawidth::integer%maxalign = 0 THEN maxalign
            ELSE nulldatawidth::integer%maxalign
          END
      	)::numeric AS nulldatahdrwidth,
	pagehdr
    FROM index_item_sizes AS s1
),
otta_calc AS (
  SELECT
	s2.bs,
	s2.nspname,
	s2.table_oid,
	s2.index_oid,
	s2.index_name,
	s2.relpages,
	COALESCE(CEIL((reltuples*(4+nulldatahdrwidth))/(bs-pagehdr::float)) +
      	CASE WHEN am.amname IN ('hash','btree')
	THEN 1
	ELSE 0
	END ,
	0 -- btree and hash have a metadata reserved block
    ) AS otta
  FROM index_aligned AS s2
    LEFT JOIN pg_am am ON s2.relam = am.oid
),
raw_bloat AS (
    SELECT
	current_database() AS dbname,
	nspname,
	c.relname AS table_name,
	sub.index_name,
        bs*(sub.relpages)::bigint AS totalbytes,
        CASE
            WHEN sub.relpages <= otta THEN 0
            ELSE bs*(sub.relpages-otta)::bigint END
            AS wastedbytes,
        CASE
            WHEN sub.relpages <= otta
            THEN 0
		ELSE bs*(sub.relpages-otta)::bigint * 100 / (bs*(sub.relpages)::bigint)
		END
        AS realbloat,
        pg_relation_size(sub.table_oid) AS table_bytes,
        stat.idx_scan AS index_scans
    FROM otta_calc AS sub
    JOIN pg_class AS c ON c.oid=sub.table_oid
    JOIN pg_stat_user_indexes AS stat ON sub.index_oid = stat.indexrelid
)
SELECT
	r.dbname AS database_name,
	nspname AS schema_name,
	r.table_name,
	r.index_name,
        round(realbloat, 1) AS bloat_pct,
        wastedbytes AS bloat_bytes,
	pg_size_pretty(wastedbytes::bigint) AS bloat_size,
        totalbytes AS index_bytes,
	pg_size_pretty(totalbytes::bigint) AS index_size,
        r.table_bytes,
	pg_size_pretty(r.table_bytes) AS table_size,
        r.index_scans
FROM raw_bloat r;
END;
$$;


ALTER FUNCTION public.get_index_bloat_info() OWNER TO postgres;

--
-- Name: sentry_increment_project_counter(bigint, integer); Type: FUNCTION; Schema: public; Owner: crypta
--

CREATE FUNCTION public.sentry_increment_project_counter(project bigint, delta integer) RETURNS integer
    LANGUAGE plpgsql
    AS $$
        declare
          new_val int;
        begin
          loop
            update sentry_projectcounter set value = value + delta
             where project_id = project
               returning value into new_val;
            if found then
              return new_val;
            end if;
            begin
              insert into sentry_projectcounter(project_id, value)
                   values (project, delta)
                returning value into new_val;
              return new_val;
            exception when unique_violation then
            end;
          end loop;
        end
        $$;


ALTER FUNCTION public.sentry_increment_project_counter(project bigint, delta integer) OWNER TO crypta;

--
-- Name: postgresdb; Type: SERVER; Schema: -; Owner: postgres
--

CREATE SERVER postgresdb FOREIGN DATA WRAPPER postgres_fdw OPTIONS (
    dbname 'postgres',
    host 'localhost',
    port '6432',
    updatable 'false'
);


ALTER SERVER postgresdb OWNER TO postgres;

--
-- Name: USER MAPPING public SERVER postgresdb; Type: USER MAPPING; Schema: -; Owner: postgres
--

CREATE USER MAPPING FOR public SERVER postgresdb;


SET default_tablespace = '';

SET default_with_oids = false;

--
-- Name: TolokaResult; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public."TolokaResult" (
    taskid character varying(40),
    iscorrect boolean,
    browser_name character varying(64),
    browser_family character varying(32),
    browser_mobile boolean,
    code character varying(64),
    useragent text,
    yuid character varying(20),
    dt timestamp without time zone,
    id integer NOT NULL,
    yuid_generated boolean DEFAULT false
);


ALTER TABLE public."TolokaResult" OWNER TO crypta;

--
-- Name: api_audiences; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_audiences (
    id character varying(80) NOT NULL,
    author character varying(100000) NOT NULL,
    name character varying(100000) NOT NULL,
    purpose character varying(100000) NOT NULL,
    login character varying(100000) NOT NULL,
    external_id bigint DEFAULT 0,
    source_path character varying(100000) NOT NULL,
    source_field character varying(100000) NOT NULL,
    state character varying(50),
    created bigint NOT NULL,
    modified bigint NOT NULL
);


ALTER TABLE public.api_audiences OWNER TO crypta;

--
-- Name: api_constructor_rules; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_constructor_rules (
    id character varying(80) NOT NULL,
    author character varying(80) NOT NULL,
    name character varying(256) NOT NULL,
    days bigint NOT NULL,
    min_days bigint NOT NULL,
    created bigint NOT NULL,
    modified bigint NOT NULL,
    issue_id character varying(64) DEFAULT ''::character varying,
    issue_key character varying(64) DEFAULT ''::character varying
);


ALTER TABLE public.api_constructor_rules OWNER TO crypta;

--
-- Name: api_constructor_rules_conditions; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_constructor_rules_conditions (
    rule_id character varying(80) NOT NULL,
    source character varying(80) NOT NULL,
    value text NOT NULL,
    revision bigint NOT NULL,
    created bigint NOT NULL,
    modified bigint NOT NULL,
    state character varying(80) NOT NULL,
    has_errors boolean DEFAULT false
);


ALTER TABLE public.api_constructor_rules_conditions OWNER TO crypta;

--
-- Name: api_constructor_rules_conditions_revision_seq; Type: SEQUENCE; Schema: public; Owner: crypta
--

CREATE SEQUENCE public.api_constructor_rules_conditions_revision_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.api_constructor_rules_conditions_revision_seq OWNER TO crypta;

--
-- Name: api_constructor_rules_conditions_revision_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: crypta
--

ALTER SEQUENCE public.api_constructor_rules_conditions_revision_seq OWNED BY public.api_constructor_rules_conditions.revision;


--
-- Name: api_evaluations; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_evaluations (
    yandexuid character varying(24),
    segment_id character varying(16),
    value character varying(16),
    good boolean,
    "timestamp" bigint
);


ALTER TABLE public.api_evaluations OWNER TO crypta;

--
-- Name: api_geo_evaluations; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_geo_evaluations (
    id character varying(80) NOT NULL,
    yandexuid character varying(100) NOT NULL,
    lat double precision NOT NULL,
    lon double precision NOT NULL,
    state character varying(1000) NOT NULL,
    "timestamp" bigint NOT NULL
);


ALTER TABLE public.api_geo_evaluations OWNER TO crypta;

--
-- Name: api_idm_login_roles; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_idm_login_roles (
    login character varying(50) NOT NULL,
    role character varying(50) NOT NULL
);


ALTER TABLE public.api_idm_login_roles OWNER TO crypta;

--
-- Name: api_idm_logins; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_idm_logins (
    login character varying(50) NOT NULL
);


ALTER TABLE public.api_idm_logins OWNER TO crypta;

--
-- Name: api_idm_roles; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_idm_roles (
    name character varying(50) NOT NULL,
    ru_name character varying(1000) NOT NULL,
    en_name character varying(1000) NOT NULL,
    ru_desc character varying(1000) NOT NULL,
    en_desc character varying(1000) NOT NULL
);


ALTER TABLE public.api_idm_roles OWNER TO crypta;

--
-- Name: api_keywords; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_keywords (
    id character varying(256) NOT NULL,
    segments character varying(65536) NOT NULL
);


ALTER TABLE public.api_keywords OWNER TO crypta;

--
-- Name: api_lab_sample_views; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_lab_sample_views (
    sample_id character varying(80) NOT NULL,
    id character varying(80) NOT NULL,
    path character varying(1000) NOT NULL,
    state character varying(80) NOT NULL,
    options bytea,
    options_ character varying(10000),
    type character varying(32) DEFAULT ''::character varying,
    error character varying(80) DEFAULT ''::character varying
);


ALTER TABLE public.api_lab_sample_views OWNER TO crypta;

--
-- Name: api_lab_samples; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_lab_samples (
    id character varying(80) NOT NULL,
    name character varying(256),
    modified bigint NOT NULL,
    created bigint NOT NULL,
    author character varying(80) NOT NULL,
    id_type character varying(80) DEFAULT 'yandexuid'::character varying NOT NULL,
    id_key character varying(80) DEFAULT 'yandexuid'::character varying NOT NULL,
    ttl bigint DEFAULT 604800,
    grouping_key character varying(80),
    access_level character varying(80) DEFAULT 'PRIVATE'::character varying NOT NULL,
    type character varying(50) NOT NULL,
    cdp_id character varying(40),
    max_groups_count integer DEFAULT 10,
    date_key character varying(80) DEFAULT ''::character varying,
    state character varying(80) DEFAULT ''::character varying
);


ALTER TABLE public.api_lab_samples OWNER TO crypta;

--
-- Name: api_lab_samples_backup; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_lab_samples_backup (
    id character varying(80),
    path character varying(1000),
    describe_task_id character varying(255),
    name character varying(256),
    modified bigint,
    created bigint,
    author character varying(80),
    id_type character varying(80),
    id_key character varying(80),
    source character varying(1000),
    ttl bigint,
    grouping_key character varying(80)
);


ALTER TABLE public.api_lab_samples_backup OWNER TO crypta;

--
-- Name: api_model_segment_relations; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_model_segment_relations (
    model_id character varying(80) NOT NULL,
    segment_id character varying(80) NOT NULL,
    low_threshold double precision,
    high_threshold double precision,
    created bigint DEFAULT 0 NOT NULL,
    modified bigint DEFAULT 0 NOT NULL
);


ALTER TABLE public.api_model_segment_relations OWNER TO crypta;

--
-- Name: api_models; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_models (
    id character varying(80) NOT NULL,
    description character varying(10000) NOT NULL,
    uri character varying(1000) NOT NULL,
    state character varying(50) NOT NULL,
    created bigint DEFAULT 0 NOT NULL,
    modified bigint DEFAULT 0 NOT NULL,
    s3_uri character varying(300),
    tag character varying(50) DEFAULT ''::character varying
);


ALTER TABLE public.api_models OWNER TO crypta;

--
-- Name: api_responsibles; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_responsibles (
    id character varying(80) NOT NULL,
    segment_id character varying NOT NULL,
    created bigint DEFAULT 0 NOT NULL,
    modified bigint DEFAULT 0 NOT NULL
);


ALTER TABLE public.api_responsibles OWNER TO crypta;

--
-- Name: api_sample_user_set_ids; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_sample_user_set_ids (
    sample_id character varying(32) NOT NULL,
    user_set_id character varying(32),
    grouping_key_value character varying(32)
);


ALTER TABLE public.api_sample_user_set_ids OWNER TO crypta;

--
-- Name: api_segment_export_tags; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_segment_export_tags (
    tag character varying(256) NOT NULL,
    segment_export_id character varying(80) NOT NULL
);


ALTER TABLE public.api_segment_export_tags OWNER TO crypta;

--
-- Name: api_segment_exports; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_segment_exports (
    segment_id character varying(80) NOT NULL,
    type character varying(80) NOT NULL,
    export_keyword_id bigint NOT NULL,
    export_segment_id bigint NOT NULL,
    coverage_profiles_value bigint,
    coverage_profiles_timestamp bigint,
    coverage_bigb_value bigint,
    coverage_bigb_timestamp bigint,
    state character varying(50) DEFAULT 'CREATED'::character varying NOT NULL,
    id character varying(80) NOT NULL,
    rule_id character varying(80),
    expression text,
    export_to_bb boolean DEFAULT true NOT NULL,
    rule_has_errors boolean DEFAULT false,
    lal text,
    activity_check_timestamp bigint DEFAULT 0
);


ALTER TABLE public.api_segment_exports OWNER TO crypta;

--
-- Name: api_segment_groups; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_segment_groups (
    name_en character varying(256),
    name_ru character varying(256),
    group_id character varying(80) NOT NULL,
    parent_group_id character varying(80) NOT NULL
);


ALTER TABLE public.api_segment_groups OWNER TO crypta;

--
-- Name: api_segment_id_to_user_set_id; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_segment_id_to_user_set_id (
    segment_lab_id character varying(64) NOT NULL,
    user_set_id character varying(32) NOT NULL
);


ALTER TABLE public.api_segment_id_to_user_set_id OWNER TO crypta;


CREATE COLLATION russian (provider=icu, locale = 'ru_RU');

--
-- Name: api_segments; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_segments (
    id character varying(80) NOT NULL,
    ticket character varying(50) NOT NULL,
    state character varying(50) DEFAULT 'CREATED'::character varying NOT NULL,
    scope character varying(50) NOT NULL,
    description_ru character varying(100000) DEFAULT ''::character varying NOT NULL,
    description_en character varying(100000) DEFAULT ''::character varying NOT NULL,
    name_en character varying(1000) DEFAULT ''::character varying NOT NULL,
    name_ru character varying(1000) COLLATE russian DEFAULT ''::character varying NOT NULL,
    modified bigint DEFAULT 0 NOT NULL,
    created bigint DEFAULT 0 NOT NULL,
    type character varying(50) NOT NULL,
    parent_id character varying(60),
    name character varying(1000) DEFAULT ''::character varying NOT NULL,
    description character varying(1000) DEFAULT ''::character varying NOT NULL,
    priority bigint DEFAULT 0,
    author character varying(80) NOT NULL,
    tanker_key character varying(256),
    tanker_name_key character varying(256),
    tanker_description_key character varying(256)
);


ALTER TABLE public.api_segments OWNER TO crypta;

--
-- Name: api_socdem_thresholds; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_socdem_thresholds (
    socdem character varying(256) NOT NULL,
    segment character varying(200) NOT NULL,
    threshold double precision NOT NULL
);


ALTER TABLE public.api_socdem_thresholds OWNER TO crypta;

--
-- Name: api_stakeholders; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.api_stakeholders (
    id character varying(80) NOT NULL,
    segment_id character varying NOT NULL,
    created bigint DEFAULT 0 NOT NULL,
    modified bigint DEFAULT 0 NOT NULL
);


ALTER TABLE public.api_stakeholders OWNER TO crypta;

--
-- Name: audience_grab; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.audience_grab (
    grab_id character varying(256) NOT NULL,
    "timestamp" bigint NOT NULL,
    login character varying(80) NOT NULL,
    auth boolean DEFAULT false
);


ALTER TABLE public.audience_grab OWNER TO crypta;

--
-- Name: lab_training_sample_metrics; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.lab_training_sample_metrics (
    sample_id character varying(32) NOT NULL,
    roc_auc double precision,
    accuracy double precision,
    positive_class_ratio double precision,
    train_sample_size integer,
    matched_ids_ratio double precision,
    top_features text,
    segments_metric double precision,
    positive_class_metric double precision,
    negative_class_metric double precision
);


ALTER TABLE public.lab_training_sample_metrics OWNER TO crypta;

--
-- Name: lab_training_sample_user_set_ids; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.lab_training_sample_user_set_ids (
    sample_id character varying(32) NOT NULL,
    user_set_id character varying(32) NOT NULL,
    target_type character varying(32) NOT NULL,
    origin_type character varying(32) NOT NULL,
    segment_size integer
);


ALTER TABLE public.lab_training_sample_user_set_ids OWNER TO crypta;

--
-- Name: lab_training_samples; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.lab_training_samples (
    id character varying(32) NOT NULL,
    name character varying(256),
    author character varying(32) NOT NULL,
    access_level character varying(32) NOT NULL,
    ttl bigint,
    created bigint NOT NULL,
    modified bigint NOT NULL,
    status character varying(50) DEFAULT 'UNKNOWN'::character varying,
    training_error character varying(256) DEFAULT NULL::character varying,
    model_name character varying(32),
    partner character varying(32)
);


ALTER TABLE public.lab_training_samples OWNER TO crypta;

--
-- Name: portal_grab; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.portal_grab (
    grab_id character varying(1000) NOT NULL,
    login character varying(80) NOT NULL,
    useragent character varying(100000) NOT NULL,
    yandexuid character varying(20) NOT NULL,
    "timestamp" bigint NOT NULL
);


ALTER TABLE public.portal_grab OWNER TO crypta;

--
-- Name: qrtz_blob_triggers; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.qrtz_blob_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    blob_data bytea
);


ALTER TABLE public.qrtz_blob_triggers OWNER TO crypta;

--
-- Name: qrtz_calendars; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.qrtz_calendars (
    sched_name character varying(120) NOT NULL,
    calendar_name character varying(200) NOT NULL,
    calendar bytea NOT NULL
);


ALTER TABLE public.qrtz_calendars OWNER TO crypta;

--
-- Name: qrtz_cron_triggers; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.qrtz_cron_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    cron_expression character varying(250) NOT NULL,
    time_zone_id character varying(80)
);


ALTER TABLE public.qrtz_cron_triggers OWNER TO crypta;

--
-- Name: qrtz_fired_triggers; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.qrtz_fired_triggers (
    sched_name character varying(120) NOT NULL,
    entry_id character varying(140) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    instance_name character varying(200) NOT NULL,
    fired_time bigint NOT NULL,
    sched_time bigint NOT NULL,
    priority integer NOT NULL,
    state character varying(16) NOT NULL,
    job_name character varying(200),
    job_group character varying(200),
    is_nonconcurrent boolean NOT NULL,
    requests_recovery boolean
);


ALTER TABLE public.qrtz_fired_triggers OWNER TO crypta;

--
-- Name: qrtz_job_details; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.qrtz_job_details (
    sched_name character varying(120) NOT NULL,
    job_name character varying(200) NOT NULL,
    job_group character varying(200) NOT NULL,
    description character varying(250),
    job_class_name character varying(250) NOT NULL,
    is_durable boolean NOT NULL,
    is_nonconcurrent boolean NOT NULL,
    is_update_data boolean NOT NULL,
    requests_recovery boolean NOT NULL,
    job_data bytea
);


ALTER TABLE public.qrtz_job_details OWNER TO crypta;

--
-- Name: qrtz_locks; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.qrtz_locks (
    sched_name character varying(120) NOT NULL,
    lock_name character varying(40) NOT NULL
);


ALTER TABLE public.qrtz_locks OWNER TO crypta;

--
-- Name: qrtz_paused_trigger_grps; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.qrtz_paused_trigger_grps (
    sched_name character varying(120) NOT NULL,
    trigger_group character varying(150) NOT NULL
);


ALTER TABLE public.qrtz_paused_trigger_grps OWNER TO crypta;

--
-- Name: qrtz_scheduler_state; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.qrtz_scheduler_state (
    sched_name character varying(120) NOT NULL,
    instance_name character varying(200) NOT NULL,
    last_checkin_time bigint NOT NULL,
    checkin_interval bigint NOT NULL
);


ALTER TABLE public.qrtz_scheduler_state OWNER TO crypta;

--
-- Name: qrtz_simple_triggers; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.qrtz_simple_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    repeat_count bigint NOT NULL,
    repeat_interval bigint NOT NULL,
    times_triggered bigint NOT NULL
);


ALTER TABLE public.qrtz_simple_triggers OWNER TO crypta;

--
-- Name: qrtz_simprop_triggers; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.qrtz_simprop_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    str_prop_1 character varying(512),
    str_prop_2 character varying(512),
    str_prop_3 character varying(512),
    int_prop_1 integer,
    int_prop_2 integer,
    long_prop_1 bigint,
    long_prop_2 bigint,
    dec_prop_1 numeric,
    dec_prop_2 numeric,
    bool_prop_1 boolean,
    bool_prop_2 boolean,
    time_zone_id character varying(80)
);


ALTER TABLE public.qrtz_simprop_triggers OWNER TO crypta;

--
-- Name: qrtz_triggers; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.qrtz_triggers (
    sched_name character varying(120) NOT NULL,
    trigger_name character varying(150) NOT NULL,
    trigger_group character varying(150) NOT NULL,
    job_name character varying(200) NOT NULL,
    job_group character varying(200) NOT NULL,
    description character varying(250),
    next_fire_time bigint,
    prev_fire_time bigint,
    priority integer,
    trigger_state character varying(16) NOT NULL,
    trigger_type character varying(8) NOT NULL,
    start_time bigint NOT NULL,
    end_time bigint,
    calendar_name character varying(200),
    misfire_instr smallint,
    job_data bytea
);


ALTER TABLE public.qrtz_triggers OWNER TO crypta;

--
-- Name: repl_mon; Type: FOREIGN TABLE; Schema: public; Owner: postgres
--

CREATE FOREIGN TABLE public.repl_mon (
    ts timestamp with time zone,
    location text,
    replics integer,
    master text
)
SERVER postgresdb
OPTIONS (
    schema_name 'public',
    table_name 'repl_mon'
);


ALTER FOREIGN TABLE public.repl_mon OWNER TO postgres;

--
-- Name: training_samples_industries; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.training_samples_industries (
    name character varying(256),
    model_name character varying(128) NOT NULL,
    author character varying(32) NOT NULL,
    objective character varying(256),
    positive_conversions character varying(256),
    negative_conversions character varying(256)
);


ALTER TABLE public.training_samples_industries OWNER TO crypta;

--
-- Name: url_page_id; Type: TABLE; Schema: public; Owner: crypta
--

CREATE TABLE public.url_page_id (
    url character varying(120),
    ids character varying(16)[]
);


ALTER TABLE public.url_page_id OWNER TO crypta;

--
-- Name: zheglov_id_seq; Type: SEQUENCE; Schema: public; Owner: crypta
--

CREATE SEQUENCE public.zheglov_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.zheglov_id_seq OWNER TO crypta;

--
-- Name: zheglov_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: crypta
--

ALTER SEQUENCE public.zheglov_id_seq OWNED BY public."TolokaResult".id;


--
-- Name: TolokaResult id; Type: DEFAULT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public."TolokaResult" ALTER COLUMN id SET DEFAULT nextval('zheglov_id_seq'::regclass);


--
-- Name: api_constructor_rules_conditions revision; Type: DEFAULT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_constructor_rules_conditions ALTER COLUMN revision SET DEFAULT nextval('api_constructor_rules_conditions_revision_seq'::regclass);


--
-- Name: api_audiences PK_AUDIENCES; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_audiences
    ADD CONSTRAINT "PK_AUDIENCES" PRIMARY KEY (id);


--
-- Name: api_geo_evaluations PK_GEO_EVALUATION; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_geo_evaluations
    ADD CONSTRAINT "PK_GEO_EVALUATION" PRIMARY KEY (id);


--
-- Name: portal_grab PK_GRAB_ID; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.portal_grab
    ADD CONSTRAINT "PK_GRAB_ID" PRIMARY KEY (grab_id);


--
-- Name: api_segment_groups PK_ID; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_segment_groups
    ADD CONSTRAINT "PK_ID" PRIMARY KEY (group_id);


--
-- Name: api_idm_logins PK_IDM_LOGIN; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_idm_logins
    ADD CONSTRAINT "PK_IDM_LOGIN" PRIMARY KEY (login);


--
-- Name: api_idm_roles PK_IDM_ROLE; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_idm_roles
    ADD CONSTRAINT "PK_IDM_ROLE" PRIMARY KEY (name);


--
-- Name: api_lab_samples PK_LAB_SAMPLES; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_lab_samples
    ADD CONSTRAINT "PK_LAB_SAMPLES" PRIMARY KEY (id);


--
-- Name: lab_training_samples PK_LAB_TRAINING_SAMPLES; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.lab_training_samples
    ADD CONSTRAINT "PK_LAB_TRAINING_SAMPLES" PRIMARY KEY (id);


--
-- Name: training_samples_industries PK_LAB_TRAINING_SAMPLES_INDUSTRIES; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.training_samples_industries
    ADD CONSTRAINT "PK_LAB_TRAINING_SAMPLES_INDUSTRIES" PRIMARY KEY (model_name);


--
-- Name: lab_training_sample_metrics PK_LAB_TRAINING_SAMPLE_METRICS; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.lab_training_sample_metrics
    ADD CONSTRAINT "PK_LAB_TRAINING_SAMPLE_METRICS" PRIMARY KEY (sample_id);


--
-- Name: lab_training_sample_user_set_ids PK_LAB_TRAINING_SAMPLE_USER_SET_IDS; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.lab_training_sample_user_set_ids
    ADD CONSTRAINT "PK_LAB_TRAINING_SAMPLE_USER_SET_IDS" PRIMARY KEY (sample_id, user_set_id);


--
-- Name: api_models PK_MODEL; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_models
    ADD CONSTRAINT "PK_MODEL" PRIMARY KEY (id);


--
-- Name: api_model_segment_relations PK_MODEL_SEGMENT_RELATIONS; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_model_segment_relations
    ADD CONSTRAINT "PK_MODEL_SEGMENT_RELATIONS" PRIMARY KEY (model_id, segment_id);


--
-- Name: api_responsibles PK_RESPONSIBLES; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_responsibles
    ADD CONSTRAINT "PK_RESPONSIBLES" PRIMARY KEY (id, segment_id);


--
-- Name: api_segments PK_SEGMENT; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_segments
    ADD CONSTRAINT "PK_SEGMENT" PRIMARY KEY (id);


--
-- Name: api_stakeholders PK_STAKEHOLDERS; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_stakeholders
    ADD CONSTRAINT "PK_STAKEHOLDERS" PRIMARY KEY (id, segment_id);


--
-- Name: api_constructor_rules_conditions api_constructor_rules_conditions_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_constructor_rules_conditions
    ADD CONSTRAINT api_constructor_rules_conditions_pkey PRIMARY KEY (rule_id, source, state);


--
-- Name: api_constructor_rules_conditions api_constructor_rules_conditions_revision_unique; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_constructor_rules_conditions
    ADD CONSTRAINT api_constructor_rules_conditions_revision_unique UNIQUE (revision);


--
-- Name: api_constructor_rules api_constructor_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_constructor_rules
    ADD CONSTRAINT api_constructor_rules_pkey PRIMARY KEY (id);


--
-- Name: api_lab_sample_views api_lab_sample_views_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_lab_sample_views
    ADD CONSTRAINT api_lab_sample_views_pkey PRIMARY KEY (sample_id, id);


--
-- Name: api_segment_exports api_segment_exports_export_ids_unique; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_segment_exports
    ADD CONSTRAINT api_segment_exports_export_ids_unique UNIQUE (export_keyword_id, export_segment_id);


--
-- Name: api_segment_exports api_segment_exports_ids_unique; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_segment_exports
    ADD CONSTRAINT api_segment_exports_ids_unique UNIQUE (id, segment_id);


--
-- Name: api_segment_exports api_segment_exports_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_segment_exports
    ADD CONSTRAINT api_segment_exports_pkey PRIMARY KEY (id);


--
-- Name: api_segment_exports api_segment_exports_rule_id_unique; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_segment_exports
    ADD CONSTRAINT api_segment_exports_rule_id_unique UNIQUE (rule_id);


--
-- Name: audience_grab audience_grab_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.audience_grab
    ADD CONSTRAINT audience_grab_pkey PRIMARY KEY (grab_id);


--
-- Name: api_keywords pk_keywords; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_keywords
    ADD CONSTRAINT pk_keywords PRIMARY KEY (id);


--
-- Name: api_socdem_thresholds pk_socdem_thresholds; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_socdem_thresholds
    ADD CONSTRAINT pk_socdem_thresholds PRIMARY KEY (socdem, segment);


--
-- Name: qrtz_blob_triggers qrtz_blob_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_calendars qrtz_calendars_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_calendars
    ADD CONSTRAINT qrtz_calendars_pkey PRIMARY KEY (sched_name, calendar_name);


--
-- Name: qrtz_cron_triggers qrtz_cron_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_fired_triggers qrtz_fired_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_fired_triggers
    ADD CONSTRAINT qrtz_fired_triggers_pkey PRIMARY KEY (sched_name, entry_id);


--
-- Name: qrtz_job_details qrtz_job_details_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_job_details
    ADD CONSTRAINT qrtz_job_details_pkey PRIMARY KEY (sched_name, job_name, job_group);


--
-- Name: qrtz_locks qrtz_locks_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_locks
    ADD CONSTRAINT qrtz_locks_pkey PRIMARY KEY (sched_name, lock_name);


--
-- Name: qrtz_paused_trigger_grps qrtz_paused_trigger_grps_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_paused_trigger_grps
    ADD CONSTRAINT qrtz_paused_trigger_grps_pkey PRIMARY KEY (sched_name, trigger_group);


--
-- Name: qrtz_scheduler_state qrtz_scheduler_state_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_scheduler_state
    ADD CONSTRAINT qrtz_scheduler_state_pkey PRIMARY KEY (sched_name, instance_name);


--
-- Name: qrtz_simple_triggers qrtz_simple_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_simprop_triggers qrtz_simprop_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_simprop_triggers
    ADD CONSTRAINT qrtz_simprop_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: qrtz_triggers qrtz_triggers_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_pkey PRIMARY KEY (sched_name, trigger_name, trigger_group);


--
-- Name: api_segment_export_tags unique_tag; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_segment_export_tags
    ADD CONSTRAINT unique_tag UNIQUE (tag, segment_export_id);


--
-- Name: TolokaResult zheglov_pkey; Type: CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public."TolokaResult"
    ADD CONSTRAINT zheglov_pkey PRIMARY KEY (id);


--
-- Name: idx_qrtz_ft_job_group; Type: INDEX; Schema: public; Owner: crypta
--

CREATE INDEX idx_qrtz_ft_job_group ON public.qrtz_fired_triggers USING btree (job_group);


--
-- Name: idx_qrtz_ft_job_name; Type: INDEX; Schema: public; Owner: crypta
--

CREATE INDEX idx_qrtz_ft_job_name ON public.qrtz_fired_triggers USING btree (job_name);


--
-- Name: idx_qrtz_ft_job_req_recovery; Type: INDEX; Schema: public; Owner: crypta
--

CREATE INDEX idx_qrtz_ft_job_req_recovery ON public.qrtz_fired_triggers USING btree (requests_recovery);


--
-- Name: idx_qrtz_ft_trig_group; Type: INDEX; Schema: public; Owner: crypta
--

CREATE INDEX idx_qrtz_ft_trig_group ON public.qrtz_fired_triggers USING btree (trigger_group);


--
-- Name: idx_qrtz_ft_trig_inst_name; Type: INDEX; Schema: public; Owner: crypta
--

CREATE INDEX idx_qrtz_ft_trig_inst_name ON public.qrtz_fired_triggers USING btree (instance_name);


--
-- Name: idx_qrtz_ft_trig_name; Type: INDEX; Schema: public; Owner: crypta
--

CREATE INDEX idx_qrtz_ft_trig_name ON public.qrtz_fired_triggers USING btree (trigger_name);


--
-- Name: idx_qrtz_ft_trig_nm_gp; Type: INDEX; Schema: public; Owner: crypta
--

CREATE INDEX idx_qrtz_ft_trig_nm_gp ON public.qrtz_fired_triggers USING btree (sched_name, trigger_name, trigger_group);


--
-- Name: idx_qrtz_j_req_recovery; Type: INDEX; Schema: public; Owner: crypta
--

CREATE INDEX idx_qrtz_j_req_recovery ON public.qrtz_job_details USING btree (requests_recovery);


--
-- Name: idx_qrtz_t_next_fire_time; Type: INDEX; Schema: public; Owner: crypta
--

CREATE INDEX idx_qrtz_t_next_fire_time ON public.qrtz_triggers USING btree (next_fire_time);


--
-- Name: idx_qrtz_t_nft_st; Type: INDEX; Schema: public; Owner: crypta
--

CREATE INDEX idx_qrtz_t_nft_st ON public.qrtz_triggers USING btree (next_fire_time, trigger_state);


--
-- Name: idx_qrtz_t_state; Type: INDEX; Schema: public; Owner: crypta
--

CREATE INDEX idx_qrtz_t_state ON public.qrtz_triggers USING btree (trigger_state);


--
-- Name: taskid_index; Type: INDEX; Schema: public; Owner: crypta
--

CREATE INDEX taskid_index ON public."TolokaResult" USING btree (taskid);


--
-- Name: api_idm_login_roles FK_IDM_LOGIN; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_idm_login_roles
    ADD CONSTRAINT "FK_IDM_LOGIN" FOREIGN KEY (login) REFERENCES api_idm_logins(login);


--
-- Name: api_model_segment_relations FK_MODELS; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_model_segment_relations
    ADD CONSTRAINT "FK_MODELS" FOREIGN KEY (model_id) REFERENCES api_models(id);


--
-- Name: api_segments FK_PARENT_ID; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_segments
    ADD CONSTRAINT "FK_PARENT_ID" FOREIGN KEY (parent_id) REFERENCES api_segments(id);


--
-- Name: api_responsibles FK_RESPONSIBLES_SEGMENTS; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_responsibles
    ADD CONSTRAINT "FK_RESPONSIBLES_SEGMENTS" FOREIGN KEY (segment_id) REFERENCES api_segments(id);


--
-- Name: api_model_segment_relations FK_SEGMENTS; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_model_segment_relations
    ADD CONSTRAINT "FK_SEGMENTS" FOREIGN KEY (segment_id) REFERENCES api_segments(id);


--
-- Name: api_stakeholders FK_STAKEHOLDERS_SEGMENTS; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_stakeholders
    ADD CONSTRAINT "FK_STAKEHOLDERS_SEGMENTS" FOREIGN KEY (segment_id) REFERENCES api_segments(id);


--
-- Name: api_constructor_rules_conditions api_constructor_rules_conditions_fkey; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_constructor_rules_conditions
    ADD CONSTRAINT api_constructor_rules_conditions_fkey FOREIGN KEY (rule_id) REFERENCES api_constructor_rules(id);


--
-- Name: api_sample_user_set_ids api_lab_sample_id_user_set_id_fk; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_sample_user_set_ids
    ADD CONSTRAINT api_lab_sample_id_user_set_id_fk FOREIGN KEY (sample_id) REFERENCES api_lab_samples(id) ON DELETE CASCADE;


--
-- Name: api_lab_sample_views api_lab_sample_views_sample_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_lab_sample_views
    ADD CONSTRAINT api_lab_sample_views_sample_id_fkey FOREIGN KEY (sample_id) REFERENCES api_lab_samples(id);


--
-- Name: api_segment_exports api_segment_exports_rule_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_segment_exports
    ADD CONSTRAINT api_segment_exports_rule_id_fkey FOREIGN KEY (rule_id) REFERENCES api_constructor_rules(id);


--
-- Name: api_segment_exports api_segment_exports_segment_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_segment_exports
    ADD CONSTRAINT api_segment_exports_segment_id_fkey FOREIGN KEY (segment_id) REFERENCES api_segments(id);


--
-- Name: lab_training_sample_metrics lab_training_sample_metrics_fk; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.lab_training_sample_metrics
    ADD CONSTRAINT lab_training_sample_metrics_fk FOREIGN KEY (sample_id) REFERENCES lab_training_samples(id) ON DELETE CASCADE;


--
-- Name: lab_training_sample_user_set_ids lab_training_sample_user_set_ids_fk; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.lab_training_sample_user_set_ids
    ADD CONSTRAINT lab_training_sample_user_set_ids_fk FOREIGN KEY (sample_id) REFERENCES lab_training_samples(id) ON DELETE CASCADE;


--
-- Name: qrtz_blob_triggers qrtz_blob_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_blob_triggers
    ADD CONSTRAINT qrtz_blob_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES qrtz_triggers(sched_name, trigger_name, trigger_group) ON DELETE CASCADE;


--
-- Name: qrtz_cron_triggers qrtz_cron_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_cron_triggers
    ADD CONSTRAINT qrtz_cron_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES qrtz_triggers(sched_name, trigger_name, trigger_group) ON DELETE CASCADE;


--
-- Name: qrtz_simple_triggers qrtz_simple_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_simple_triggers
    ADD CONSTRAINT qrtz_simple_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES qrtz_triggers(sched_name, trigger_name, trigger_group) ON DELETE CASCADE;


--
-- Name: qrtz_simprop_triggers qrtz_simprop_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_simprop_triggers
    ADD CONSTRAINT qrtz_simprop_triggers_sched_name_fkey FOREIGN KEY (sched_name, trigger_name, trigger_group) REFERENCES qrtz_triggers(sched_name, trigger_name, trigger_group) ON DELETE CASCADE;


--
-- Name: qrtz_triggers qrtz_triggers_sched_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.qrtz_triggers
    ADD CONSTRAINT qrtz_triggers_sched_name_fkey FOREIGN KEY (sched_name, job_name, job_group) REFERENCES qrtz_job_details(sched_name, job_name, job_group);


--
-- Name: api_segment_export_tags segment_export_id; Type: FK CONSTRAINT; Schema: public; Owner: crypta
--

ALTER TABLE ONLY public.api_segment_export_tags
    ADD CONSTRAINT segment_export_id FOREIGN KEY (segment_export_id) REFERENCES api_segment_exports(id);


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

GRANT USAGE ON SCHEMA public TO monitor;


--
-- Name: FUNCTION pg_stat_reset(); Type: ACL; Schema: pg_catalog; Owner: postgres
--

GRANT ALL ON FUNCTION pg_catalog.pg_stat_reset() TO mdb_admin;


--
-- Name: FOREIGN DATA WRAPPER postgres_fdw; Type: ACL; Schema: -; Owner: postgres
--

GRANT ALL ON FOREIGN DATA WRAPPER postgres_fdw TO mdb_admin;


--
-- Name: TABLE repl_mon; Type: ACL; Schema: public; Owner: postgres
--

GRANT SELECT ON TABLE public.repl_mon TO PUBLIC;


--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: public; Owner: postgres
--

ALTER DEFAULT PRIVILEGES FOR ROLE postgres IN SCHEMA public GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO crypta WITH GRANT OPTION;


--
-- PostgreSQL database dump complete
--
