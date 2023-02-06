#!/bin/bash

if [[ ! -v BUILD_ROOT_DIR ]]; then
    BUILD_ROOT_DIR=".."
    echo warning: expecting build root at ${BUILD_ROOT_DIR}
fi;

BIN=${BUILD_ROOT_DIR}/imap
CONFIG=etc/imap/dev.yml
J=40

function copy_configs {
    echo copy configs
    CONFDIR=etc/imap
    mkdir -p $CONFDIR && rm -rf $CONFDIR/*
    cp -a ${BUILD_ROOT_DIR}/$CONFDIR/* $CONFDIR/
    ln -sf ${BUILD_ROOT_DIR}/../../../macs_pg/etc/query.conf $CONFDIR/query.conf
}

function get_secrets {
    get_secrets/get_secrets
    chmod 0600 .pgpass
    mkdir -p etc/imap/
    mv .xiva_api_token.yml .pgpass cert.pem tvm_secret etc/imap/
}

function is_alive {
    pid=$@
    if ! kill -0 $pid &> /dev/null; then
        return 1
    fi
    return 0
}

function check_process_not_running {
    name=$1
    if (pgrep -x name) &>/dev/null; then
        echo "$name is already running"
        exit 1
    fi
}

function stop {
    pid=$@
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
            echo failed to stop imap
            return 1
        fi
    fi
    return 0
}

function wait_imap {
    echo -ne 'wait for imap'
    for i in {0..20};
    do
        if (echo -en ". logout\r\n" | nc -w 5 127.0.0.1 31143 | grep -qw ". OK LOGOUT") &>/dev/null; then
            break;
        fi
        echo -ne '.'
        sleep 0.2
    done
    echo
}

function run_tests {
    # enable client logs
    mkdir -p var/log/imap/uids/

    echo running imap
    PGPASSFILE=etc/imap/.pgpass $BIN $CONFIG &
    pid=$!
    echo service imap started ${pid}

    wait_imap

    echo running tests
    ../../../ya make -j$J -tA --test-tag ya:manual
    result=$?
    stop $pid

    return ${result}
}

check_process_not_running imap
copy_configs
get_secrets
run_tests
