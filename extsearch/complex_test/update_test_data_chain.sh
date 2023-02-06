#! /usr/bin/env bash

set -x

DATA_PATH=$1
shift

INDEX_ROOT="extsearch/video/robot/index"
DATA_INDEX_ROOT="${DATA_PATH}/extsearch/video/robot/index"
TEST_INTERMEDIATE_PATH="tests/test-results/tests/testing_out_stuff/results"

run-test() {
	local tested_bin=$1
	local test_name=$2
	shift
	shift
	ya make -r $* -C ${tested_bin} -tPZ -F ${test_name} || exit
}

cp ${DATA_INDEX_ROOT}/complex_test/shardhosts ${DATA_INDEX_ROOT}/indexfactors/prepare/shardhosts
cp ${DATA_INDEX_ROOT}/complex_test/hostfactors ${DATA_INDEX_ROOT}/indexfactors/prepare/hostfactors
cp ${DATA_INDEX_ROOT}/complex_test/urlfactors ${DATA_INDEX_ROOT}/indexfactors/prepare/urlfactors
cp ${DATA_INDEX_ROOT}/complex_test/authorfactors ${DATA_INDEX_ROOT}/indexfactors/prepare/authorfactors
cp ${DATA_INDEX_ROOT}/complex_test/url2author ${DATA_INDEX_ROOT}/indexfactors/prepare/url2author
cp ${DATA_INDEX_ROOT}/complex_test/related_items ${DATA_INDEX_ROOT}/vlinkindex/related_items
cp ${DATA_INDEX_ROOT}/complex_test/timestamp ${DATA_INDEX_ROOT}/indexfactors/indexfactors/timestamp

cp ${DATA_INDEX_ROOT}/complex_test/media ${DATA_PATH}/yweb/video/mediadb/mediaexport/indexexport/media
cp ${DATA_INDEX_ROOT}/complex_test/media.thumb.data ${DATA_PATH}/yweb/video/mediadb/mediaexport/indexexport/media.thumb.data
cp ${DATA_INDEX_ROOT}/complex_test/mediaplan ${DATA_PATH}/yweb/video/mediadb/mediaexport/indexexport/plan

cp ${DATA_INDEX_ROOT}/complex_test/url2doc ${DATA_INDEX_ROOT}/indexfactors/prepare/url2doc

run-test ${INDEX_ROOT}/indexfactors test_preparefactors.py $*
cp ${INDEX_ROOT}/indexfactors/${TEST_INTERMEDIATE_PATH}/test_preparefactors.test/outurlfactors ${DATA_INDEX_ROOT}/indexfactors/indexfactors/urlfactors
cp ${INDEX_ROOT}/indexfactors/${TEST_INTERMEDIATE_PATH}/test_preparefactors.test/outauthorfactors ${DATA_INDEX_ROOT}/indexfactors/indexfactors/authorfactors

run-test ${INDEX_ROOT}/complex_test gen_indexann_items.py $*
cp ${INDEX_ROOT}/complex_test/${TEST_INTERMEDIATE_PATH}/gen_indexann_items.test/indexann_items ${DATA_INDEX_ROOT}/indexfactors/indexfactors/indexann_items

run-test yweb/video/mediadb/mediaexport test_indexexport.py $*

run-test yweb/video/mediadb/mediaexport test_nothumbgen.py $*
cp yweb/video/mediadb/mediaexport/${TEST_INTERMEDIATE_PATH}/test_nothumbgen.test/index_items ${DATA_INDEX_ROOT}/vlinkindex/index_items
cp yweb/video/mediadb/mediaexport/${TEST_INTERMEDIATE_PATH}/test_nothumbgen.test/thumb_items ${DATA_INDEX_ROOT}/vlinkindex/thumbs_items

run-test ${INDEX_ROOT}/vlinkindex test_diff.py $*

run-test ${INDEX_ROOT}/vlinkindex test_fusion_for_factors.py $*
cp ${INDEX_ROOT}/vlinkindex/${TEST_INTERMEDIATE_PATH}/test_fusion_for_factors.test/fusion ${DATA_INDEX_ROOT}/indexfactors/indexfactors/fusion

run-test ${INDEX_ROOT}/indexfactors test_indexhostfactors.py $*
cp ${INDEX_ROOT}/indexfactors/${TEST_INTERMEDIATE_PATH}/test_indexhostfactors.test/factorswshard_lenval ${DATA_INDEX_ROOT}/indexfactors/prepare/factorswshard
cp ${INDEX_ROOT}/indexfactors/${TEST_INTERMEDIATE_PATH}/test_indexhostfactors.test/host_factors ${DATA_INDEX_ROOT}/indexbuilder/host_factors

run-test ${INDEX_ROOT}/indexfactors test_spreadhostfactors.py $*
cp ${INDEX_ROOT}/indexfactors/${TEST_INTERMEDIATE_PATH}/test_spreadhostfactors.test/hostfactors ${DATA_INDEX_ROOT}/indexfactors/indexfactors/hostfactors

run-test ${INDEX_ROOT}/indexfactors test_indexfactors.py $*
cp ${INDEX_ROOT}/indexfactors/${TEST_INTERMEDIATE_PATH}/test_indexfactors.test/factors_index ${DATA_INDEX_ROOT}/indexbuilder/build/input/fusion

run-test ${INDEX_ROOT}/indexfactors test_pruning.py $*
cp ${INDEX_ROOT}/indexfactors/${TEST_INTERMEDIATE_PATH}/test_pruning.test/factors_index ${DATA_INDEX_ROOT}/indexbuilder/renumber/input/fusion

run-test ${INDEX_ROOT}/indexfactors test_spok.py $*

run-test ${INDEX_ROOT}/indexbuilder build.py $*
run-test ${INDEX_ROOT}/indexbuilder renumber.py $*
