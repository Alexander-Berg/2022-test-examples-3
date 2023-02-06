#!/bin/bash

set -ex

export PYTHONPATH="$PYTHONPATH:./tests-system/certificates"

mkdir -p ./var/xiva-server/xconf/env2
python ./tests-system/xconf_dump_gen.py ./tests-system/test_tokens.json > ./var/xiva-server/xconf/cached_config

./tests-system/tvm_gen.sh $TVM_TICKETS_DIR

/usr/bin/xiva-server ./etc/xiva-server/local-auto-test.yml
