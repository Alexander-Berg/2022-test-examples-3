CONFIG=etc/collectors/dev.yml

function run_tests {
    echo running main tests
    $BIN $CONFIG &
    local pid=$!
    echo service collectors started ${pid}

    wait_for_ping "http://localhost:3048"
    wait_for_acquire_shards "http://localhost:8080" $(get_shards_count)

    echo running tests
    ../../../ya make -j$J -tA --test-tag ya:manual src/main
    local result=$?
    stop $pid

    return ${result}
}
