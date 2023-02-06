#!/bin/sh

export REPORT_CONFIG=$(dirname $(readlink -f $0))/test-report.cfg

if [ -z $1 ]
then
    $(dirname "$(readlink -f $0)")/market-report-test
else
    $(dirname "$(readlink -f $0)")/market-report-test $1
fi
