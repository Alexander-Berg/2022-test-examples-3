#!/usr/bin/env bash

_scenario() {
    GLB acceptance-start:                                     res=
    GLB acceptance-init:                                      acceptance-start res=

    GLB acceptance-create-ticket:                             acceptance-init res=
    GLB acceptance-performance-launcher:                      acceptance-create-ticket res=
    GLB acceptance-performance-meta:                          acceptance-performance-launcher res=
    GLB acceptance-performance-base:                          acceptance-performance-launcher res=
    GLB acceptance-performance-released-base:                 acceptance-performance-launcher res=
    GLB acceptance-stats:                                     acceptance-performance-base acceptance-performance-released-base acceptance-performance-meta  res=
    GLB acceptance-mlm-ru-leakage-launch-controller:          acceptance-create-ticket res=
    GLB acceptance-mlm-ru-leakage-await-controller:           acceptance-mlm-ru-leakage-launch-controller res=
    GLB acceptance-mlm-test-launch-controller:                acceptance-create-ticket res=
    GLB acceptance-mlm-test-await-controller:                 acceptance-mlm-test-launch-controller res=
    GLB acceptance-cbir-controller-scraper:                   acceptance-create-ticket res=

    GLB acceptance-finish-mandatory:                          acceptance-stats acceptance-cbir-controller-scraper acceptance-mlm-test-await-controller res=
    GLB acceptance-finish-optional:                           acceptance-mlm-ru-leakage-await-controller res=
    GLB acceptance-finish:                                    acceptance-finish-mandatory acceptance-finish-optional res=
}

#!+INCLUDES
source acceptance_common.sh
source acceptance_targets.sh
#!-INCLUDES
