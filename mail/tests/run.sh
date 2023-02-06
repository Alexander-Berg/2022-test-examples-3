#!/bin/bash

# Postgres can take a while to initialize,
# set this option if you want to leave the container
# running after the test session. Convenient for debugging
# tests or adding new tests.
LEAVE_DB_RUNNING=$1

DB_NAME=resharddb
DB_USER=xiva_user
DB_PASSWORD=whocares
[[ $DB_PORT ]] || DB_PORT=12000

function conninfo_from_port {
    DB_PORT=$1
    echo "host=localhost port=$DB_PORT user=$DB_USER dbname=$DB_NAME password=$DB_PASSWORD"
}

function start_db {
    DB_PORT=$1

docker build --pull -t 'resharddb_tests' --network=host ../ > /dev/null
docker run --detach --rm \
    --env DB_PORT=$DB_PORT \
    --env DB_NAME=$DB_NAME \
    --env DB_USER=$DB_USER \
    --env DB_PASSWORD=$DB_PASSWORD \
    --name 'xiva_resharddb_tests' --network=host 'resharddb_tests'
}

function stop_db {
    CONTAINER_ID=$1

    echo 'stopping db...'
    docker stop $1 > /dev/null # container will be removed automatically
}

pg_isready -h localhost -p $DB_PORT \
    || CONTAINER_ID=`start_db $DB_PORT`

export RESHARDDB_CONNINFO=`conninfo_from_port $DB_PORT`

while [[ true ]]; do
    pg_isready -h localhost -p $DB_PORT \
        && psql "$RESHARDDB_CONNINFO" -c 'select from resharding.migrations;' \
        && break

    echo 'waiting for db to start (might take a minute or two)...'
    sleep 3
done

nosetests -s resharding.py

[[ ! $LEAVE_DB_RUNNING && $CONTAINER_ID ]] && stop_db $CONTAINER_ID