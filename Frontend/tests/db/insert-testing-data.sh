#!/usr/bin/env bash

set -e

clickhouse-client --port $CH_PORT --query="drop database if exists riverbank_testing"
clickhouse-client --port $CH_PORT --query="create database riverbank_testing"
# tables
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/tables/rum_csp.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/tables/rum_errors.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/tables/gogol.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/tables/strm.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/tables/perf.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/tables/ottsessions.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/tables/drm.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/tables/ott_acs.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/tables/ott_app.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/tables/gen_music.sql
# session1 bfa681933f66cc15153e5bdd1633bcc4f1c6b368048exWEBx9252x1655469547
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session1/rum_csp.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session1/gogol.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session1/strm.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session1/perf.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session1/ottsessions.sql
# session2 242a58c6c3f552ba0da3557d1a35504f3e26ea2d1d10xWEBx9311x1657632680
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session2/rum_errors.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session2/ottsessions.sql
# session3 2c12e356e8c967dd45d99196f721596abb3cb4dc9ee9xWEBx9344x1657141895
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session3/ottsessions.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session3/drm.sql
# session4 4cdd69876fb0262e87f15b613195c84e081af6cb134bxWEBx9392x1658146586
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session4/ottsessions.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session4/ott_acs.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session4/ott_app.sql
# session5 ril1y2ztrf3p3a5l9fqa4ox5kz1h77dz64qk53x2mqp3xMIOx0582x1658992146
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session5/ottsessions.sql
clickhouse-client --port $CH_PORT --queries-file=./src/tests/db/sql/session5/gen_music.sql

psql -p 5432 -d postgres -c "drop database if exists riverbank_testing"
psql -p 5432 -d postgres -c "create database riverbank_testing"
psql -p 5432 -d riverbank_testing -f ./src/tests/db/sql/tables/cached_sessions_postgres.sql
