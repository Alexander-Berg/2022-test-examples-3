#!/usr/bin/env bash
set -uxe
export CM_TARGET="$1"
export INDEX_CM_LOGS_DIR=/cm/worker.var/logs
export ZK_BINARIES_PATH='/media-services/images/robot/acceptance/main-binaries-task'
export ZK_FACTORS_BINARIES_PATH='/media-services/images/robot/acceptance/factors-binaries-task'
export ZK_ACCEPTANCE_START_TS_PATH=/media-services/images/robot/acceptance/main-start-ts
export INDEX_BUNDLES_TASK=IMAGES_BUILD_MAIN_INDEX_NIGHTLY_BUNDLES

#!+INCLUDES

_scenario () {
    source common/common_mr.scenario
}

MR_PROD_PREFIX=images
MR_TEST_PREFIX=images/main_test

MR_PREFIX=${MR_TEST_PREFIX}

export BERKANAVT=${BERKANAVT:-/home/robot-imgtestbase/berkanavt}
export IMAGES=${IMAGES:-/home/robot-imgtestbase/berkanavt/images/main_test}
export PYTHONPATH=${PYTHONPATH:-}:$IMAGES/scripts/garden/sandbox:$IMAGES/scripts/garden/py_common/contrib

KIWI_EXPORT_PATH=${MR_PREFIX}/kiwi_export

SOLOMON_CLUSTER=robot_test

source common/arnold.rc

source common/common_mr.sh

source svn/robot_imgtestbase.rc
source svn/svn.sh

source common/functions.sh

source common/download_resources.sh
source common/release_resources.sh
source common/common.sh
source common/requests.sh

source mrindex/test.rc
source mrindex/common.sh

source workflow/workflow-test.sh
source workflow/workflow-acceptance-test.sh

source mrindex/test_beta.sh
source mrindex/test_acceptance.sh

source mrindex/test_index.sh
source mrindex/statisticslight.sh

source statistics/binaries.sh
source statistics/statistics.sh

export MR_TMP="${YT_PREFIX}${MR_PREFIX}/tmp"

#!-INCLUDES

echo "Args:" "$@"
echo "Date:" `date`
echo "Host:" `hostname`
echo " Sys:" `uname -a`
echo "======================================================================"
echo "=== Running $1 on `hostname`"
echo "======================================================================"

# It just have to be here.
"$@"
