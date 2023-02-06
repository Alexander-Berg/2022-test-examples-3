#!/usr/bin/env bash

test_dirs='
../cbir/it
../indexbuilder/it
../inputdoc/it2
../metadoc/it
../planner/it
../remapindex/it
../selectionrank/it
'

ya make -trAP --test-stderr --keep-temps --test-param=run_with_yt $test_dirs "$@" 

