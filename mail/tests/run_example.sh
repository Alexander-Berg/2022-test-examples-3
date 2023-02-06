#!/bin/bash

TESTS_DIR=`pwd`
YPLATFORM_PATH=../../..

#if [[ ! -e $PROJ_DIR ]]; then
#    echo "You must edit the PROJ_DIR variable in this script!"
#    exit 1;
#fi

KEEP_XTERM_OPEN="echo \"press enter to close\"; read";

rm -rf /tmp/acceptor_*;
echo "Starting the acceptors"
xterm -geometry 80x24+10+10 -e "cd ${YPLATFORM_PATH}; ./sbin/yplatform ${TESTS_DIR}/arbiter1.conf; $KEEP_XTERM_OPEN" &
xterm -geometry 80x24+600+10 -e "cd ${YPLATFORM_PATH}; ./sbin/yplatform ${TESTS_DIR}/arbiter2.conf; $KEEP_XTERM_OPEN" &

xterm -geometry 80x24+10+400 -e "cd ${YPLATFORM_PATH}; ./sbin/yplatform ${TESTS_DIR}/node1.conf; $KEEP_XTERM_OPEN" &
xterm -geometry 80x24+600+400 -e "cd ${YPLATFORM_PATH}; ./sbin/yplatform ${TESTS_DIR}/node2.conf; $KEEP_XTERM_OPEN" &

#xterm -geometry 80x24+600+300 -e "cd $PROJ_DIR; ./example_learner; $KEEP_XTERM_OPEN" &
#xterm -geometry 80x24+800+300 -e "cd $PROJ_DIR; ./tp_monitor; $KEEP_XTERM_OPEN" &
#sleep 3;

#xterm -geometry 80x24+10+300 -e "cd $PROJ_DIR; ./example_proposer 0; $KEEP_XTERM_OPEN" &
#sleep 2;

#xterm -geometry 80x8+10+600 -e "cd $PROJ_DIR; ./benchmark_client -s 10; $KEEP_XTERM_OPEN" &

echo "Press enter to send the kill signal"
read
(cd ${YPLATFORM_PATH} ; killall -INT xterm)
