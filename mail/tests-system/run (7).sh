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
export ARCADIA_SOURCE_ROOT=$(ya dump root)
export PYTHONPATH=$PYTHONPATH:"${working_dir}/../core"

source "$working_dir/../core/pycommon/exec_tools.sh"

# system tests base
#-------------------------------------------------------------------------------

trap '{ docker-compose down --rmi local -v; }' INT ERR EXIT
docker-compose up -d --force-recreate
wait_pg_is_ready 'localhost' '6432' 'xivadb' 'xiva_user' 'xiva_password'
wait_pg_is_ready 'localhost' '6433' 'xivadb' 'xiva_user' 'xiva_password'
wait_pg_is_ready 'localhost' '7432' 'xivadb' 'xiva_user' 'xiva_password'
wait_pg_is_ready 'localhost' '7433' 'xivadb' 'xiva_user' 'xiva_password'
wait_pg_is_ready 'localhost' '8432' 'xivadb' 'xiva_user' 'xiva_password'

# prepare test data
export XTEST_HOST=localhost
export XTEST_PORT=14080
export XTEST_PORT2=15080

echo 'running with resharding disabled'
# run app
pids=$(run_app $working_dir $bin_dir/xivahub etc/xivahub/local1_tests.yml)
pids="$pids $(run_app $working_dir $bin_dir/xivahub etc/xivahub/local2_tests.yml)"

# wait for catchup
for i in {0..20}; do
  curl -s localhost:$XTEST_PORT/state | grep -q '"convey_enabled" : true' &>/dev/null && break
  echo -ne "."; sleep 0.2
done

curl -s "localhost:$XTEST_PORT/enable/t"
curl -s "localhost:$XTEST_PORT2/enable/t"

sleep 6

# run tests pack
pushd $working_dir/tests-system
nosetests -x notify/*.py subscribe/*.py utility_apis/*.py xtasks/*.py uidset/*.py
popd

is_alive $pids || exit 1
stop_app $pids
is_alive $pids && exit 2

echo 'running with resharding enabled'
# run app
pids=$(run_app $working_dir $bin_dir/xivahub etc/xivahub/local1_resharding_tests.yml)
pids="$pids $(run_app $working_dir $bin_dir/xivahub etc/xivahub/local2_resharding_tests.yml)"

# wait for catchup
for i in {0..20}; do
  curl -s localhost:$XTEST_PORT/state | grep -q '"master_available" : true' &>/dev/null && break
  echo -ne "."; sleep 0.2
done

# run tests pack
pushd $working_dir/tests-system
nosetests -x resharding/*.py notify/*.py subscribe/*.py utility_apis/*.py xtasks/*.py uidset/*.py
popd

echo "pids are $pids"
is_alive $pids || exit 3
stop_app $pids
is_alive $pids && exit 4
# necessary because last is_alive returns 1
exit 0