#!/bin/bash

function parse_msgs {
    SPM=../bin/parse_message

    rm -r $1/nmeta 2>/dev/null
    mkdir $1/nmeta

    find $1/messages -type f | xargs -I {} basename {} .msg \
        | xargs -n1 -P8 -i% bash -c "${SPM} ${1}/messages/%.msg > ${1}/nmeta/%.xml"
}

MDIR='.'
if [ $# -ne 0 ]
then
    MDIR=$1
    LAST_CH=${MDIR:${#MDIR}-1:1}
    if [ $LAST_CH == '/' ]
    then
        MDIR=${MDIR:0:${#MDIR}-1}
    fi
fi

#echo "It may take some time, do not worry"

echo Parsing $MDIR"/messages/*" ...
parse_msgs $MDIR

echo "Comparing to etalon..."
diff $MDIR/meta $MDIR/nmeta > diff.log --brief

echo "Check diff.log for difference in etalon & current parsings"
