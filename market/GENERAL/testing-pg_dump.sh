#!/bin/sh
# run pg_dump on testing PGaaS
# Example: PGAAS_SCHEMA="${USER}_operator_window" ./testing-pgaas-pg_dump.sh --table='checkouter_orders' > ~/tmp/testing_dump.sql
set -e
cd "$(dirname "$0")"

export PG_CMD='pg_dump'

if [ -z "$PGAAS_SCHEMA" ]; then
    PGAAS_SCHEMA='testing_operator_window'
fi

./testing-pgaas.sh --schema="$PGAAS_SCHEMA" "$@"
