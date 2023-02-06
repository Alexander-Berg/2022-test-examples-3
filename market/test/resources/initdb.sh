#!/bin/bash

# Embedded PG fix for MacOS Monterey and higher: https://github.com/opentable/otj-pg-embedded/issues/163
# Change your EMBEDDED_PG_FOLDER path

EMBEDDED_PG_FOLDER=/var/folders/p9/zgyr3nf53lngflkz67_621_rs7xzv0/T/embedded-pg/PG-310a8143d9abbddd9372c02bbaeb4a59/bin
POSTGRES=$(which postgres)
INIT_DB=$(which initdb)
PG_CTL=$(which pg_ctl)

cp "${POSTGRES}" ${EMBEDDED_PG_FOLDER}/postgres
cp "${INIT_DB}" ${EMBEDDED_PG_FOLDER}/initdb
cp "${PG_CTL}" ${EMBEDDED_PG_FOLDER}/pg_ctl
