#!/bin/bash

LOG_DIR=tests/hermione/report/logs

CLEMENT_CMD=clement
TEST_CMD=hermione

if [ "$1" = "--gui" ]; then
    TEST_CMD=hermione-gui
fi

mkdir -p $LOG_DIR

RESULT=1

RED='\033[0;31m'
GREEN='\033[0;32m'
NO_COLOR='\033[0m'

echo "Start build static";
echo "You can find Build log in $LOG_DIR/static-build.log";
CONNECT_APP__ASSETS="/portal/static" CONNECT_APP__FREEZE_PATH="/portal/_" LOCAL_STATIC=true npm run prod > $LOG_DIR/static-build.log 2>&1;
echo "Static built";

if [ "$1" = "--write" ]; then
    CLEMENT_CMD=clement-write
    TEST_CMD=hermione-write
elif [ "$1" = "--create" ]; then
    CLEMENT_CMD=clement-create
    TEST_CMD=hermione-create
elif [ "$1" = "--gui-write" ]; then
    CLEMENT_CMD=clement-write
    TEST_CMD=hermione-gui-write
elif [ "$1" = "--gui-create" ]; then
    CLEMENT_CMD=clement-create
    TEST_CMD=hermione-gui-create
fi

npm run $CLEMENT_CMD > $LOG_DIR/clement.log 2>&1 &
CLEMENT_PID=$!;
echo "Start Clement server. PID=$CLEMENT_PID";

npm run ui-test-server > $LOG_DIR/server.log 2>&1 &
SERVER_PID=$!;
echo "Start Connect server. PID=$SERVER_PID";


npm run $TEST_CMD
RESULT=$?
echo "npm run $TEST_CMD";
echo "Start Hermione";


npx tree-kill $SERVER_PID >> $LOG_DIR/server.log 2>&1;
echo "Killing Connect server with PID=$SERVER_PID";

npx tree-kill $CLEMENT_PID >> $LOG_DIR/clement.log 2>&1;
echo "Killing Clement server with PID=$CLEMENT_PID";

if [ $RESULT = 0 ]; then
    echo -e "${GREEN}SUCCESS${NO_COLOR}";
else
    echo -e "${RED}FAILED${NO_COLOR}";
fi
