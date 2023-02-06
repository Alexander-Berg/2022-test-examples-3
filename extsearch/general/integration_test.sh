#!/usr/bin/env bash

_scenario()                                 {
    GLB integration-test-start:                    res=
    GLB integration-test-indexbuilder:             integration-test-start res=
    GLB integration-test-selectionrank:            integration-test-start res=
    GLB integration-test-planner:                  integration-test-start res=
    GLB integration-test-remapindex:               integration-test-start res=
    GLB integration-test-cbir:                     integration-test-start res=
    GLB integration-test-cbir-indexer:             integration-test-start res=
    GLB integration-test-inputdoc:                 integration-test-start res=
    GLB integration-test-metadoc:                  integration-test-start res=
    GLB integration-test-metadoc-external-content: integration-test-start res=
    GLB integration-test-end-to-end:               integration-test-start res=
    GLB integration-test-imagedb:                  integration-test-start res=
    GLB integration-test-linkdb:                   integration-test-start res=
    GLB integration-test-urldb:                    integration-test-start res=
    GLB integration-test-hostdb:                   integration-test-start res=
    GLB integration-test-fastmatcher-mr:           integration-test-start res=
    GLB integration-test-fastanndata:              integration-test-start res=
    GLB integration-test-streams:                  integration-test-start res=
    GLB integration-test-userfactors2:             integration-test-start res=
    GLB integration-test-anndata:                  integration-test-start res=
    GLB integration-test-rim:                      integration-test-start res=
    GLB integration-test-shardpreparer:            integration-test-start res=
    GLB integration-test-click-sim:                integration-test-start res=
    GLB integration-test-rtcvdup:                  integration-test-start res=
    GLB integration-test-traceroute:               integration-test-start res=
    GLB integration-test-id2url:                   integration-test-start res=
    GLB integration-test-mkcrcdb:                  integration-test-start res=
    GLB integration-test-ytcloud:                  integration-test-start res=
    GLB integration-test-finish:                   integration-test-traceroute integration-test-id2url integration-test-mkcrcdb integration-test-indexbuilder integration-test-selectionrank integration-test-planner integration-test-remapindex integration-test-cbir integration-test-cbir-indexer integration-test-inputdoc integration-test-metadoc integration-test-metadoc-external-content integration-test-end-to-end integration-test-imagedb integration-test-linkdb integration-test-urldb integration-test-hostdb integration-test-fastmatcher-mr integration-test-fastanndata integration-test-streams integration-test-userfactors2 integration-test-anndata integration-test-rim integration-test-shardpreparer integration-test-click-sim integration-test-rtcvdup integration-test-ytcloud res=
}

#!+INCLUDES
source download_resources.sh
source release_resources.sh
source sandbox.sh
#!-INCLUDES

integration-test-env()                      {
    true
}

integration-test-run-test-case()            {
    integration-test-env

    run-sandbox-task \
        -t ${SANDBOX_OAUTH_FILE} \
        -u IMAGES-BASE-DEPLOY \
        -d "Run tests for index binaries from trunk" \
        -w 300 \
        --priority-class "SERVICE" \
        --priority "HIGH" \
        YA_MAKE \
        --task-bool-param test true \
        --disk-space $((150 * 1024 * 1024 * 1024)) \
        --task-param targets "$1"
}

integration-test-start()                    {
    true
}

integration-test-indexbuilder()             {
    integration-test-run-test-case extsearch/images/robot/index/indexbuilder/it
}

integration-test-selectionrank()            {
    integration-test-run-test-case extsearch/images/robot/index/selectionrank/it
}

integration-test-planner()                  {
    integration-test-run-test-case extsearch/images/robot/index/planner/it
}

integration-test-remapindex()               {
    integration-test-run-test-case extsearch/images/robot/index/remapindex/it
}

integration-test-cbir()                     {
    integration-test-run-test-case extsearch/images/robot/index/cbir/it
}

integration-test-cbir-indexer()             {
    integration-test-run-test-case extsearch/images/robot/index/cbir_indexer/it
}

integration-test-inputdoc()                 {
    integration-test-run-test-case extsearch/images/robot/index/inputdoc/it
}

integration-test-metadoc()                  {
    integration-test-run-test-case extsearch/images/robot/index/metadoc/it
}

integration-test-metadoc-external-content() {
    integration-test-run-test-case extsearch/images/robot/index/metadoc/lib/create/ut
}

integration-test-end-to-end()               {
    integration-test-run-test-case extsearch/images/robot/index/it
}

integration-test-imagedb()                  {
    integration-test-run-test-case extsearch/images/robot/mrdb/mkimagedb/it
}

integration-test-linkdb()                   {
    integration-test-run-test-case extsearch/images/robot/mrdb/linkdb/it
}

integration-test-urldb()                    {
    integration-test-run-test-case extsearch/images/robot/mrdb/mkurldb/it
}

integration-test-hostdb()                   {
    integration-test-run-test-case extsearch/images/robot/mrdb/hostdb/mkhostdb/it
}

integration-test-fastmatcher-mr()           {
    integration-test-run-test-case cv/semidups/fastmatcher/fastmatchermr/it
}

integration-test-fastanndata()              {
    integration-test-run-test-case extsearch/images/robot/userdata/fastanndata/it
}

integration-test-streams()                  {
    integration-test-run-test-case extsearch/images/robot/mrdb/streams/it
}

integration-test-userfactors2()             {
    integration-test-run-test-case extsearch/images/robot/userdata/userfactors2/it
}

integration-test-anndata()                  {
    integration-test-run-test-case extsearch/images/robot/userdata/anndata/it
}

integration-test-rim()                      {
    integration-test-run-test-case extsearch/images/robot/mrdb/rimdb/builder/it
}

integration-test-shardpreparer()            {
    integration-test-run-test-case extsearch/images/robot/index/shardpreparer/it
}

integration-test-click-sim()                {
    integration-test-run-test-case extsearch/images/robot/userdata/click_sim/it
}

integration-test-rtcvdup() {
    integration-test-run-test-case extsearch/images/robot/rtcvdup/it
}

integration-test-traceroute() {
    integration-test-run-test-case extsearch/images/robot/index/traceroute/it
}

integration-test-id2url() {
    integration-test-run-test-case extsearch/images/robot/mrdb/id2url/it
}

integration-test-mkcrcdb() {
    integration-test-run-test-case extsearch/images/robot/mrdb/mkcrcdb/it
}

integration-test-ytcloud() {
    integration-test-run-test-case extsearch/images/robot/mrdb/ytcloud/it
}

integration-test-finish()                   {
    true
}
