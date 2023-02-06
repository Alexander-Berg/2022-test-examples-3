#!/usr/bin/env bash

_scenario()                          {
    GLB workflow-start:                     res=
    GLB workflow-drop-old-data:             workflow-start res=
    GLB workflow-copy-prod-data:            workflow-drop-old-data res=
    GLB workflow-copy-selectionrank-bin:    workflow-copy-prod-data res=
    GLB workflow-build-index-binaries:      workflow-start res=
    GLB workflow-update-index-config:       workflow-build-index-binaries res=
    GLB workflow-update-index-binaries:     workflow-build-index-binaries res=
    GLB workflow-rebuild-selectionrank:     workflow-copy-selectionrank-bin res=
    GLB workflow-copy-factordb:             workflow-copy-prod-data res=
    GLB workflow-copy-heavy-bases:          workflow-rebuild-selectionrank res=
    GLB workflow-index:                     workflow-copy-heavy-bases workflow-copy-factordb workflow-update-index-binaries workflow-update-index-config res=
    GLB workflow-statistics:                workflow-index res=
    GLB workflow-finish:                    workflow-statistics mailto=images-index-monitoring res=
}

#!+INCLUDES
source ../mrindex/acceptance_common.sh
source ../mrindex/acceptance_ticket.rc
source ../common/zookeeper.sh
source ../common/startrek.sh
source workflow-branch-acceptance-functions.sh
source common.sh
#!-INCLUDES

workflow-start()                     {
    true
}
