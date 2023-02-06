#!/usr/bin/env bash

_scenario() {
    GLB workflow-acceptance-start:           res=
    GLB workflow-acceptance-queue-pop:       workflow-acceptance-start res=
    GLB workflow-acceptance-lock:            workflow-acceptance-queue-pop res=
    GLB workflow-acceptance-beta:            workflow-acceptance-lock res=
    GLB workflow-acceptance-run:             workflow-acceptance-beta res=
    GLB workflow-acceptance-unlock:          workflow-acceptance-run res=
    GLB workflow-acceptance-finish:          workflow-acceptance-unlock res= jumpto=workflow-acceptance-start:count<inf
}

#!+INCLUDES
source workflow-acceptance-functions.sh
#!-INCLUDES
