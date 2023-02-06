#!/bin/bash

#-------------------------------------------------------------------------------
# system tests base

[[ "$TRACE" ]] && set -x
set -eEo pipefail

bin_dir="$1"
working_dir="$2"
[[ -z "$bin_dir" ]] && bin_dir=".";
[[ -z "$working_dir" ]] && working_dir="..";
echo "bin_dir=${bin_dir}"
echo "working_dir=${working_dir}"

export PATH="$PATH:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
export PYTHONPATH=$PYTHONPATH:"${working_dir}/../core"

source "$working_dir/../core/pycommon/exec_tools.sh"

# system tests base
#-------------------------------------------------------------------------------

# prepare test data
mkdir -p $working_dir/var/reaper/xconf/
./xconf_dump_gen.py ./test_tokens.json > $working_dir/var/reaper/xconf/cached_config

# run app
pids=$(run_app $working_dir $bin_dir/reaper etc/reaper/local-auto-test.yml)

# run tests pack
pushd $working_dir/tests-system
nosetests -x general/*.py
popd

is_alive $pids || exit 1
stop_app $pids
is_alive $pids && exit 1

# run app
pids=$(run_app $working_dir $bin_dir/reaper etc/reaper/local-auto-test-fallback.yml)

# run tests pack
pushd $working_dir/tests-system
nosetests -x general/*.py

is_alive $pids || exit 1