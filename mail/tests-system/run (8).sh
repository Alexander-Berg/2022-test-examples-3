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
export SHARD_1="host=sas-ommek17z8j1rx2a4.db.yandex.net,vla-xxgmbzz6dya5kuun.db.yandex.net port=6432 dbname=xtable_dev_01 sslmode=verify-full user=xiva_user target_session_attrs=read-write"
export SHARD_2="host=sas-ommek17z8j1rx2a4.db.yandex.net,vla-xxgmbzz6dya5kuun.db.yandex.net port=6432 dbname=xtable_dev_02 sslmode=verify-full user=xiva_user target_session_attrs=read-write"
python $working_dir/tests-system/prepare_db.py

# run app
pids=$(run_app $working_dir $bin_dir/xivamesh etc/xivamesh/local-autotest.yml)

# run tests pack
pushd $working_dir/tests-system
nosetests -x *.py

is_alive $pids || exit 1
