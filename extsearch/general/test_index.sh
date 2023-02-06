#!/usr/bin/env bash

#!+INCLUDES
source test.rc
source common.sh
source big_index.scenario
source build_index.sh
source build_garbage_tier.sh
source imtub_test.sh
#!-INCLUDES

index-prepare-config() {
    true
}

index-prepare-binaries() {
    true
}

index-garbage-select() {
    true
}

index-launch-garbage-tier() {
    true
}

index-garbage-remap-metadoc() {
    true
}

index-finish() {
    true
}
