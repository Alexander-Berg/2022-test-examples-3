#!/bin/bash

set -euo pipefail
trap 'pkill -P $$' SIGINT SIGTERM
export LOG_LEVEL=INFO
export YT_LOG_LEVEL=INFO
export LISTEN_THREADS=4
export REQUEST_QUEUE_SIZE=50000
export ECHO_HOST='http://localhost'
export ECHO_PORT=2509
export ECHO_CONCURRENCY=4
export ASYNC_IO_CONCURRENCY=4
DEFAULT_SERVER_PORT=2510
ASYNC_SERVER_PORT=2511
RAW_YT_SERVER=2512
USERVER_PORT=8089 # shoul be reflected in userver config
ARC_ROOT="$( cd "$( dirname "$0" )/../../../../../../" && pwd )"


function stopServer {
    echo "stopping $1"
    pids=$(pgrep $1 || true)
    if [[ -z "$pids" ]]; then
        echo "server not started"
    else
        for pid in $pids; do
            printf "found pid: $pids"
            kill $pid 2> /dev/null || true
            wait $pid 2> /dev/null || true
            echo " - stopped"
        done
        echo "server stopped"
    fi
    echo
}

function waitPort {
    echo "wait for server on port $1"
    while [[ "$(curl -s -o /dev/null -w ''%{http_code}'' localhost:$1/ping)" != "200" ]]; do
        sleep 1;
    done
    echo "got response from server on port $1"
    echo
}

function debugServer {
    echo "debug $1 on port $2"
    ya tool gdb --args ./$1/$1 -p $2 --log-path=/tmp/$1.log
    echo
    exit 0
}

function startServer {
    stopServer $1
    echo "starting $1 on port $2"
    ./$1/$1 -p $2 --log-path=/tmp/$1.log & 
    echo "started with pid=$!"
    echo
    waitPort $2
}

function startUServer {
    stopServer $1
    echo "starting $1 on port $2"
    ./$1/$1 --config $ARC_ROOT/market/library/shiny/server/sample/async_full/userver/static_config.yaml & 
    echo "started with pid=$!"
    echo
    waitPort $2
}

function explodeServerByClientsCount {
    echo "Trying to explode $1 Clients=$3 RequestCount=$4sec"
    read -p "Prees enter to start"
    bin=$ARC_ROOT/market/ammo_api/tools/shiny_tank/shiny_tank
    $bin --input ./ammo.txt  --target http://localhost:$2 --stream-count $3 --request-count $4 --stats-port 8080
    echo
    echo
}

function explodeServerByRps {
    echo "Trying to explode $1 RPS=$3 Duration=$4sec"
    read -p "Prees enter to start"
    bin=$ARC_ROOT/market/ammo_api/tools/shiny_tank/shiny_tank
    $bin --input ./ammo.txt  --target http://localhost:$2 --rps $3 --duration $4 --stats-port 8080 
    echo
    echo
}

function explodeServer {
    explodeServerByRps $1 $2 25 $3
    explodeServerByRps $1 $2 50 $3
    explodeServerByRps $1 $2 100 $3
    explodeServerByRps $1 $2 150 $3
    explodeServerByRps $1 $2 200 $3

    explodeServerByClientsCount $1 $2 2 15000
    explodeServerByClientsCount $1 $2 4 15000
    explodeServerByClientsCount $1 $2 8 15000
    explodeServerByClientsCount $1 $2 16 15000
    explodeServerByClientsCount $1 $2 32 15000

    # bombardier -c $3 -l -d $3 -p intro,result http://localhost:$2/test > ./test_results/$1-bombardier-concurrency-$3.txt 
    # fasthttploader -c $3 -d $3 -r ./test_results/$1-report-concurrency-$3.html http://localhost:$2/test
}

function testServer {
    startServer $1 $2
    explodeServer $1 $2 $3
    stopServer $1
}

function testUServer {
    startUServer $1 $2
    explodeServer $1 $2 $3
    stopServer $1
}

cd $ARC_ROOT/market/library/shiny/server/sample/async_full
mkdir -p ./test_results
rm ./test_results/* || true
ya make -r -C $ARC_ROOT/market/ammo_api/tools/shiny_tank
ya make -r --force-build-depends

startServer echo_server $ECHO_PORT
testUServer userver $USERVER_PORT 100
testServer raw_yt_server $ASYNC_SERVER_PORT 100
testServer default_server $DEFAULT_SERVER_PORT 100
testServer async_server $ASYNC_SERVER_PORT 100
stopServer echo_server
