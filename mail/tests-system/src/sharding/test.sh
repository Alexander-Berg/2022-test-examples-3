CONFIG=etc/collectors/sharding_test.yml
MASTER_CONFIG=etc/collectors/sharding_test_master.yml

function run_tests {
    echo running sharding tests

    $BIN $MASTER_CONFIG &
    master_pid=$!
    wait_for_ping "http://localhost:4048"
    echo master service collectors started $master_pid

    wait_for_acquire_shards "http://localhost:9080" $(get_shards_count)

    $BIN $CONFIG &
    pid=$!
    wait_for_ping "http://localhost:3048"
    echo service collectors started $pid

    ../../../ya make -j$J -tA --test-tag ya:manual src/sharding
    result=$?

    stop $pid
    stop $master_pid

    return ${result}
}
