#!/bin/bash

if [[ ! -v BUILD_ROOT_DIR ]]; then
    BUILD_ROOT_DIR=".."
    echo warning: expecting build root at ${BUILD_ROOT_DIR}
fi;

BIN=${BUILD_ROOT_DIR}/collectors
export PGPASSFILE=etc/collectors/.pgpass
J=40

# Hosts
SHARPEI_HOST="sharpei-testing.mail.yandex.net:80"
COLLECTORS_HOST="localhost:3048"
LOCKER_HOST="localhost:8081"

function copy_configs {
    echo copy configs
    CONFDIR=etc/collectors
    mkdir -p $CONFDIR && rm -rf $CONFDIR/*
    cp -a ${BUILD_ROOT_DIR}/$CONFDIR/* $CONFDIR/
    ln -sf ${BUILD_ROOT_DIR}/../../../macs_pg/etc/query.conf $CONFDIR/query.conf
}

function get_secrets {
    get_secrets/get_secrets
    chmod 0600 .pgpass
    mkdir -p etc/collectors/
    mv .pgpass tvm_secret tvm_secret_ext .oauth_client.yml .rpop_secret.yml etc/collectors/
}

function is_alive {
    local pid=$@
    if ! kill -0 $pid &> /dev/null; then
        return 1
    fi
    return 0
}

function check_process_not_running {
    local name=$1
    if (pgrep -x name) &>/dev/null; then
        echo "$name is already running"
        exit 1
    fi
}

function stop {
    local pid=$@
    if [ "$pid" != "" ]; then
        kill -TERM $pid
        echo -ne 'stopping' $pid
        for i in {0..30}; do
            if ! is_alive $pid; then
                echo
                return 0
            fi
              echo -ne '.'
            sleep 0.1
        done
        echo
        if is_alive $pid; then
            echo failed to stop collectors
            return 1
        fi
    fi
    return 0
}

function wait_for_ping {
    echo -ne 'wait for ping'
    local host=$1
    for i in {0..20};
    do
        if (curl $host/ping | grep pong -q) &>/dev/null; then
            echo
            return 0
        fi
        echo -ne '.'
        sleep 0.2
    done
    echo
    echo "ping not successful"
    exit 1
}

function get_shard_ids {
    echo $(curl -s "${SHARPEI_HOST}/v3/stat" | jq -r ".[].id")
}

function get_shards_count {
    echo $(curl -s "${SHARPEI_HOST}/v3/stat" | jq ".[].id" | wc -l)
}

function is_shards_acquired {
    local host=$1
    local number=$2
    local acquired_shards_count=$(curl -s ${host}/acquired_shards | jq ".[].id" | wc -l)
    [[ ${acquired_shards_count} -eq ${number} ]] && return 0 || return 1
}

function wait_for_acquire_shards {
    echo -ne 'wait for acquire shards'
    local host=$1
    local number=$2
    for i in {0..60};
    do
        if is_shards_acquired ${host} ${number}; then
            echo
            return 0
        fi
        echo -ne '.'
        sleep 0.2
    done
    echo
    echo "shards not acquired"
    exit 1
}

trap 'kill $(jobs -p)' EXIT # kill jobs on process exit
check_process_not_running collectors
copy_configs
get_secrets
for test in $(find src -name test.sh); do
    source "$test"
    run_tests || exit 1
done
