#!/bin/sh

TABLE=$1
BASEDIR=$(dirname "$0")
SCHEMA_FILE=$BASEDIR/../resources/yt/schemas/${TABLE:2}
SCHEMA_DIR=${SCHEMA_FILE%/*}
mkdir -p "$SCHEMA_DIR"
ya tool yt --proxy=hahn get $TABLE/@schema > $SCHEMA_FILE.schema
arc add $SCHEMA_FILE.schema
