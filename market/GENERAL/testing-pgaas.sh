#!/bin/sh
# see options in ./pgaas.sh
set -e
SRC_DIR="$(dirname "$0")"

if [ -z "$PGAAS_HOST" ]; then
    PGAAS_HOST='vla-nhomj6chbdihowj6.db.yandex.net,lilucrmdb-test01i.db.yandex.net,lilucrmdb-test01h.db.yandex.net'
fi
export PGAAS_HOST

export PGAAS_DB='lilucrmdb_test'

"$SRC_DIR/pgaas.sh" "$@"
