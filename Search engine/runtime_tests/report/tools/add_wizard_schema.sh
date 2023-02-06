#!/bin/bash

SELF=$(readlink $0 || echo $0)
BIN=$(dirname $SELF)
ROOT=$BIN/../../

$BIN/add_wizard_schema.py \
    --schema_dir      $ROOT/report/schema_data \
    --test_file_dir   $ROOT/report/integration/web_manual \
    --testpalm        "$1" \
    --testpalm_type   "$2" \
    --counter_prefix  "$3" \
    --location        "$4"
