#!/bin/bash

function kill_pathfinders {
    ps -o "%p %a" ax | grep '[0-9] ./targetpathfinder_old 8095' | sed 's/\([0-9]\) .*$/\1/' | xargs kill -9
    ps -o "%p %a" ax | grep '[0-9] ./targetpathfinder 8096' | sed 's/\([0-9]\) .*$/\1/' | xargs kill -9
    rm targetpathfinder_old
    rm targetpathfinder
    rm *.log
    rm log.txt
}
trap kill_pathfinders EXIT

EXE_PATH="/home/teamcity/pathfinder_tests/targetpathfinder"
DB_PATH="/home/teamcity/pathfinder_tests/data/rasp"

cd "`dirname "$0"`"

(
    set -e
    cd ../webpathfinder/targetpathfinder
    ./remake.sh
)
kill_pathfinders

cp $EXE_PATH ./targetpathfinder_old
cp ../webpathfinder/targetpathfinder/targetpathfinder ./

set -e

./targetpathfinder_old 8095 $DB_PATH targetpathfinder_old.log targetpathfinder_old.pid 1
./targetpathfinder 8096 $DB_PATH targetpathfinder.log targetpathfinder.pid 1

ps -o "%r %p %c %a" ax | grep targetpathfind

python test_pathfinder.py
