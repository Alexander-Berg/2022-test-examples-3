#!/bin/bash

if [[ ! -v BUILD_ROOT_DIR ]]; then
    BUILD_ROOT_DIR=".."
    echo warning: expecting build root at ${BUILD_ROOT_DIR}
fi;

MOBILE_API_PORT=4080
INTERNAL_PORT=8080
MASTER_MOBILE_API_PORT=4081
MASTER_INTERNAL_PORT=8082
CHECK_LOCK_URL=localhost:8081/check_lock?resource=xeno_tests
BIN=./${BUILD_ROOT_DIR}/xeno
CONFIG=etc/xeno/dev.yml
SHARDER_TESTS_CONFIG=etc/xeno/sharder-tests.yml
SHARDER_TESTS_MASTER_CONFIG=etc/xeno/sharder-tests-master.yml
PROXY_AUTH_TESTS_CONFIG=etc/xeno/proxy_auth_by_password_tests.yml
RATE_LIMIT_TESTS_CONFIG=etc/xeno/rate_limit_auth_by_password_tests.yml
LOCKER=locker/locker
LOCKER_CONFIG=etc/xeno/locker.yml
SHARPEI_HOST=sharpei-testing.mail.yandex.net
export PGPASSFILE=etc/xeno/.pgpass

function is_alive {
    pid=$@
    if ! kill -0 $pid &> /dev/null; then
        return 1
    fi
    return 0
}

function is_auth_lock_acquired {
    host=$1
    if (curl $host/auth_master | grep $(hostname) -q) &>/dev/null; then
        return 0
    fi
    return 1
}

function is_shards_acquired {
    host=$1
    acquired_shards_count=$(curl -s $host/acquired_buckets_info | grep -o '"id"' | wc -l)
    shards_count=$(curl -s $SHARPEI_HOST/v3/stat | grep -o '"id"' | wc -l)
    [[ $acquired_shards_count -eq $shards_count ]] && return 0 || return 1
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
            echo failed to stop xeno
            return 1
        fi
    fi
    return 0
}

function check_dependencies {
    DEPS=$(cat requirements.txt)
    for package in $DEPS; do
        package=${package%==*}
        if ! (python2 -m pip list | grep "$package" -q) &>/dev/null; then
            if [[ "$package" == "psycopg2" ]]; then
                if ! (dpkg -l | grep libpq-dev -q) &>/dev/null; then
                    echo missing dependecy: libpq-dev
                    echo 'use "apt-get install libpq-dev" to install it'
                fi
            fi

            echo missing pip dependecy: "$package"
            echo 'use "python2 -m pip install -r requirements.txt" to install all required dependencies'
            exit 1
        fi
    done
}

function check_process_not_running {
    if (pgrep -x "locker") &>/dev/null; then
        echo 'locker is already running'
        exit 1
    fi
    if (pgrep -x "xeno") &>/dev/null; then
        echo 'xeno is already running'
        exit 1
    fi
}

function copy_configs {
    echo copy configs
    CONFDIR=etc/xeno
    mkdir -p $CONFDIR && rm -rf $CONFDIR/*
    cp -a ${BUILD_ROOT_DIR}/$CONFDIR/* $CONFDIR/
    ln -sf ${BUILD_ROOT_DIR}/../../../macs_pg/etc/query.conf $CONFDIR/query.conf
    ln -sf ${BUILD_ROOT_DIR}/../../../ymod_cachedb/etc/cachedb.conf $CONFDIR/cachedb.conf
}

function get_secrets {
    get_secrets/get_secrets
    chmod 0600 .pgpass
    mv .xiva_api_token.yml .encryption.yml tvm_secret .pgpass etc/xeno/
}

function check_lock {
    (curl --fail $CHECK_LOCK_URL) &>/dev/null
}

function acquire_lock {
    echo acquire lock

    ${LOCKER} ${LOCKER_CONFIG} &
    locker_pid=$!

    echo locker started ${locker_pid}

    echo -ne 'waiting for lock'
    for i in {0..20};
    do
        if check_lock; then
            break;
        fi
        echo -ne '.'
        sleep 0.2
    done
    echo

    if ! is_alive $locker_pid; then
        echo locker failed to start 1>&2
        exit 1
    fi

    if ! check_lock; then
        echo -ne "lock not acquired "
        curl -w "\n" $CHECK_LOCK_URL
        exit 1
    fi
}

function wait_for_ping {
    echo -ne 'wait for ping'
    host=$1
    for i in {0..20};
    do
        if (curl $host/ping | grep pong -q) &>/dev/null; then
            break;
        fi
        echo -ne '.'
        sleep 0.2
    done
    echo
}

function wait_for_acquire_auth_lock {
    echo -ne 'wait for auth lock'
    host=$1
    for i in {0..50};
    do
        if is_auth_lock_acquired $host; then
            echo
            return 0
        fi
        echo -ne '.'
        sleep 0.2
    done
    echo
    echo auth lock not acquired
    exit 1
}

function wait_for_acquire_shards {
    echo -ne 'wait for acquire shards'
    host=$1
    for i in {0..50};
    do
        if is_shards_acquired $host; then
            echo
            return 0
        fi
        echo -ne '.'
        sleep 0.2
    done
    echo
    echo shards not acquired
    exit 1
}

function run_tests {
    echo run common tests

    echo running xeno
    $BIN $CONFIG &
    pid=$!

    echo xeno started ${pid}

    wait_for_ping "localhost:"$MOBILE_API_PORT

    if ! is_alive $pid; then
        echo xeno failed to start 1>&2
        exit 1
    fi

    echo 'running tests'
    nosetests --stop --verbose --with-profile --profile-restrict='tests.py' --nocapture tests.py
    result=$?

    if ! is_alive $pid; then
        echo xeno are down after tests 1>&2
        result=1
    fi

    stop $pid

    return ${result}
}

function run_sharder_tests {
    echo run sharder tests

    echo running master xeno
    $BIN $SHARDER_TESTS_MASTER_CONFIG &
    master_pid=$!
    echo service xeno started ${master_pid}

    wait_for_ping "localhost:"$MASTER_MOBILE_API_PORT

    if ! is_alive $master_pid; then
        echo xeno failed to start 1>&2
        exit 1
    fi

    wait_for_acquire_shards "localhost:"$MASTER_INTERNAL_PORT

    echo running xeno
    $BIN $SHARDER_TESTS_CONFIG &
    pid=$!

    echo xeno started ${pid}

    wait_for_ping "localhost:"$MOBILE_API_PORT

    if ! is_alive $pid; then
        echo xeno failed to start 1>&2
        exit 1
    fi

    echo 'running sharder tests'
    nosetests --stop --verbose --with-profile --profile-restrict='sharder-tests.py' --nocapture sharder-tests.py
    result=$?

    if ! is_alive $pid; then
        echo xeno are down after tests 1>&2
        result=1
    fi

    stop $pid
    stop $master_pid

    return ${result}
}

function run_proxy_auth_by_password_tests {
    echo run proxy auth by password tests

    echo running master xeno
    $BIN $PROXY_AUTH_TESTS_CONFIG &
    master_pid=$!
    echo service xeno started ${master_pid}

    wait_for_ping "localhost:"$MASTER_MOBILE_API_PORT

    if ! is_alive $master_pid; then
        echo xeno failed to start 1>&2
        exit 1
    fi

    wait_for_acquire_auth_lock "localhost:"$MASTER_INTERNAL_PORT

    echo running xeno
    $BIN $CONFIG &
    proxies_pid=$!
    echo xeno started ${proxies_pid}

    wait_for_ping "localhost:"$MOBILE_API_PORT

    if ! is_alive $proxies_pid; then
        echo xeno failed to start 1>&2
        exit 1
    fi

    wait_for_acquire_auth_lock "localhost:"$INTERNAL_PORT

    echo 'running proxy to master auth by password tests'
    nosetests --stop -a type='proxy' --verbose --with-profile --profile-restrict='proxy-auth-by-pass-tests.py' --nocapture proxy-auth-by-pass-tests.py
    result=$?

    if [ "$result" -eq "1" ]; then
        return ${result}
    fi

    stop $master_pid

    wait_for_acquire_auth_lock "localhost:"$INTERNAL_PORT

    nosetests --stop -a type='acqured_auth_lock' --verbose --with-profile --profile-restrict='proxy-auth-by-pass-tests.py' --nocapture proxy-auth-by-pass-tests.py
    result=$?

    stop $proxies_pid

    return ${result}
}

function run_rate_limit_auth_by_password_tests {
    echo run rate limit auth by password tests

    echo running xeno
    $BIN $RATE_LIMIT_TESTS_CONFIG &
    pid=$!
    echo service xeno started ${pid}

    wait_for_ping "localhost:"$MOBILE_API_PORT

    echo 'running rate limit auth by password tests'
    nosetests --stop --verbose --with-profile --profile-restrict='rate-limit-auth-by-pass-tests.py' --nocapture rate-limit-auth-by-pass-tests.py
    result=$?

    if ! is_alive $pid; then
        echo xeno failed to start 1>&2
        exit 1
    fi

    stop $pid

    return ${result}
}

trap 'kill $(jobs -p)' EXIT # kill jobs (e.g. xeno, locker) on process exit
check_dependencies
check_process_not_running
copy_configs
acquire_lock
get_secrets
run_tests && run_sharder_tests && run_proxy_auth_by_password_tests && run_rate_limit_auth_by_password_tests
