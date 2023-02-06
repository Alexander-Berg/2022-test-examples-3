#!/bin/bash -x

SELF=$(readlink $0 || echo $0)
BIN=$(dirname $SELF)
ROOT=$BIN/..

HOST=${HOST:-localhost:80}

set -x

# https://github.com/box/flaky

# all tests are flaky by default:
python $ROOT/run_test.py $ROOT/report/*/{web,people,inverted} --host=$HOST -v -s --force-flaky --max-runs=3 --min-passes=1 --no-success-flaky-report $*

# only explicitly marked tests are flaky
#python $ROOT/run_test.py $ROOT/report/*/{web,people} --host=$HOST -v -s --no-success-flaky-report $*

# completely disable test repetition
#python $ROOT/run_test.py $ROOT/report/*/{web,people} --host=$HOST -v -s -p no:flaky $*
