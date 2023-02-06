#!/usr/bin/env bash

_scenario() {
    GLB beta-start:                                                                 res=
    GLB beta-wait-callisto:             beta-start                                  res=
    GLB beta-finish:                    beta-wait-callisto                          res=
}

#!+INCLUDES
source beta_common.sh
#!-INCLUDES
