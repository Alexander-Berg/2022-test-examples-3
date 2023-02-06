CONFIG=etc/collectors/collecting_test.yml
LOCKER_CONFIG=etc/collectors/locker.yml

LOCKER=./locker/locker

SRC_USER="collectors-src-user"

function get_user_uid {
    local login=$1
    local host="http://pass-test.yandex.ru"
    local args="blackbox?userip=127.0.0.1&format=json&method=userinfo&dbfields=subscription.suid.2&login=${login}"
    echo $(curl -s "${host}/${args}" | jq -r ".[] | .[] | .uid | .value")
}

function get_user_shard {
    local uid=$1
    local args="conninfo?mode=all&uid=${uid}"
    echo $(curl -s "${SHARPEI_HOST}/${args}" | jq -r ".id")
}

function lock_shard {
    local resource=$1
    curl -s "${LOCKER_HOST}/lock?resource=${resource}" > /dev/null
}

function lock_src_user_shard {
    local src_user_id=$(get_user_uid ${SRC_USER})
    local src_user_shard=$(get_user_shard ${src_user_id})
    lock_shard ${src_user_shard}
    echo "${src_user_shard}"
}

function is_resource_locked {
    local resource=$1
    local result=$(curl -s "${LOCKER_HOST}/check_lock?resource=${resource}")
    [[ ! -z ${result} ]] && return 0 || return 1
}

function wait_for_lock_shard {
    local resource=$1
    for i in {0..60};
    do
        if is_resource_locked ${resource}; then
            echo
            return 0
        fi
        echo -ne '.'
        sleep 0.2
    done
    echo
    echo "shard not locked"
    exit 1
}

function run_tests {
    # setup locker
    ${LOCKER} ${LOCKER_CONFIG} &
    local locker_pid=$!
    echo "locker started ${locker_pid}"
    wait_for_ping ${LOCKER_HOST}
    local locked_shard=$(lock_src_user_shard)
    wait_for_lock_shard ${locked_shard}
    echo "locking shard ${locked_shard}"

    # setup collectors
    echo "running collectors"
    ${BIN} ${CONFIG} &
    local pid=$!
    wait_for_ping ${COLLECTORS_HOST}
    wait_for_acquire_shards "localhost:8080" $(( $(get_shards_count) - 1))
    echo "service collectors started ${pid}"

    # run tests
    echo "running tests"
    ../../../ya make -j$J -tA --test-tag ya:manual src/collecting
    local result=$?

    # stop processes
    stop ${pid}
    stop ${locker_pid}

    return ${result}
}
