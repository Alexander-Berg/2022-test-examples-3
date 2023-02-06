#!/usr/bin/env bash

_scenario() {
    # Init state.
    GLB index-start:
    GLB index-init-state:                                         index-start
    GLB index-create-dirs:                                        index-init-state
    GLB index-download-binaries:                                  index-create-dirs
    GLB index-download-config:                                    index-create-dirs

    # Copy tables.
    GLB index-input-start:                                        index-download-binaries index-download-config
    GLB index-input-copy-imagedb:                                 index-input-start
    GLB index-input-copy-semidupdb:                               index-input-start
    GLB index-input-copy-factordb:                                index-input-start
    GLB index-input-copy-tags:                                    index-input-start
    MRLINKDBclst index-input-copy-linkdb:                         index-input-start

    # Merge input data and assign document ids.
    GLB index-planner-start:                                      index-input-copy-linkdb index-input-copy-imagedb index-input-copy-semidupdb index-input-copy-factordb index-input-copy-tags
    GLB index-planner-define-documentid:                          index-planner-start
    MRLINKDBclst index-planner-rank-image-page:                   index-planner-define-documentid res=planner:1/7@\\2
    GLB index-planner-define-split-keys:                          index-planner-rank-image-page

    # Calculate yandex clicks and regex regex factors.
    GLB index-regexindex-start:                                   index-planner-define-documentid
    GLB index-regexindex-yabar-prepare:                           index-regexindex-start
    GLB index-regexindex-yabar-docid:                             index-regexindex-yabar-prepare
    GLB index-regexindex-yabar-finish:                            index-regexindex-yabar-docid
    GLB index-regexindex-clicks-shows-prepare:                    index-regexindex-start
    GLB index-regexindex-clicks-shows-docid:                      index-regexindex-clicks-shows-prepare
    GLB index-regexindex-clicks-shows-finish:                     index-regexindex-clicks-shows-docid
    GLB index-regexindex-merge:                                   index-regexindex-clicks-shows-finish index-regexindex-yabar-finish

    # Build input documents.
    GLB index-inputdoc-start:                                     index-planner-define-split-keys
    GLB index-inputdoc-drop-links:                                index-inputdoc-start
    GLB index-inputdoc-drop-pageann:                              index-inputdoc-start
    GLB index-inputdoc-prepare-images:                            index-inputdoc-start
    GLB index-inputdoc-prepare-images-url:                        index-inputdoc-start
    GLB index-inputdoc-prepare-ann:                               index-inputdoc-start
    GLB index-inputdoc-prepare-tags:                              index-inputdoc-start
    GLB index-inputdoc-prepare-extdata:                           index-inputdoc-start
    MRLINKDBclst index-inputdoc-prepare-links:                    index-inputdoc-drop-links res=inputdoc:1/7@\\2
    MRLINKDBclst index-inputdoc-prepare-pageann:                  index-inputdoc-drop-pageann res=inputdoc:1/7@\\2
    MRINDEXclst index-inputdoc-merge:                             index-inputdoc-prepare-links index-inputdoc-prepare-images-url index-inputdoc-prepare-images index-inputdoc-prepare-ann index-inputdoc-prepare-pageann index-inputdoc-prepare-tags index-inputdoc-prepare-extdata res=inputdoc:1/7@\\2

    # Build meta documents from input documents.
    MRINDEXclst index-metadoc-build:                              index-inputdoc-merge res=metadoc:1/6@\\2
    MRINDEXclst index-selection-rank-dump:                        index-metadoc-build

    # Calculate user factors.
    GLB index-userindex-assign-document:                          index-planner-define-documentid
    GLB index-userindex-userdoc:                                  index-userindex-assign-document
    GLB index-userindex-usertrie:                                 index-userindex-assign-document index-metadoc-build

    # Series.
    MRINDEXclst index-series-find-potential:                      index-inputdoc-merge res=metadoc:1/6@\\2
    MRINDEXclst index-series-sort-potential:                      index-series-find-potential res=metadoc:1/6@\\2
    GLB index-series-merge-pageattrs:                             index-series-sort-potential
    GLB index-series-detect:                                      index-series-sort-potential index-series-merge-pageattrs
    GLB index-series-filter:                                      index-series-detect
    GLB index-series:                                             index-series-filter

    # Primary tier.
    GLB index-selection-rank-primary-tier:                        index-selection-rank-dump
    MRINDEXclst index-remap-metadoc-primary-tier:                 index-selection-rank-primary-tier res=metadoc:1/6@\\2
    GLB index-remap-regex-primary-tier:                           index-selection-rank-primary-tier index-regexindex-merge
    GLB index-remap-userdoc-primary-tier:                         index-selection-rank-primary-tier index-userindex-userdoc
    GLB index-remap-series-primary-tier:                          index-selection-rank-primary-tier index-series
    GLB index-merge-metadoc-primary-tier:                         index-remap-metadoc-primary-tier index-remap-series-primary-tier
    GLB index-create-portion-primary-tier:                        index-merge-metadoc-primary-tier
    GLB index-dump-hosts:                                         index-merge-metadoc-primary-tier

    # Platinum tier.
    GLB index-selection-rank-platinum-tier:                       index-selection-rank-primary-tier
    MRINDEXclst index-remap-metadoc-platinum-tier:                index-selection-rank-platinum-tier res=metadoc:1/6@\\2
    GLB index-remap-regex-platinum-tier:                          index-selection-rank-platinum-tier index-regexindex-merge
    GLB index-remap-userdoc-platinum-tier:                        index-selection-rank-platinum-tier index-userindex-userdoc
    GLB index-remap-series-platinum-tier:                         index-selection-rank-platinum-tier index-series
    GLB index-merge-metadoc-platinum-tier:                        index-remap-metadoc-platinum-tier index-remap-series-platinum-tier
    GLB index-create-portion-platinum-tier:                       index-merge-metadoc-platinum-tier

    # Cbir tier.
    GLB index-cbir-prepare:                                       index-merge-metadoc-primary-tier

    # Count index state statistics.
    GLB index-statistics-request:                                 index-merge-metadoc-primary-tier index-merge-metadoc-platinum-tier
    GLB index-urltracer-request:                                  index-statistics-request

    # Imtubs.
    GLB index-dump-thumbids:                                      index-merge-metadoc-primary-tier
    GLB index-write-generation:                                   index-dump-thumbids
    GLB index-process-generationdb:                               index-write-generation
    GLB index-diff-thumbids:                                      index-process-generationdb
    GLB index-process-taas-thumbs:                                index-dump-thumbids
    GLB index-build-alivelist:                                    index-process-generationdb

    GLB index-finish-primary-tier:                                index-create-portion-primary-tier index-remap-regex-primary-tier index-remap-userdoc-primary-tier index-userindex-usertrie index-cbir-prepare
    GLB index-finish-platinum-tier:                               index-create-portion-platinum-tier index-remap-regex-platinum-tier index-remap-userdoc-platinum-tier

    # Finish.
    GLB index-save-logs:                                          index-finish-primary-tier index-finish-platinum-tier index-finish-imtub index-urltracer-request index-dump-hosts
    GLB index-finish:                                             index-save-logs
}

#!+INCLUDES
source test2.rc
source common.sh
source big_index.scenario
source build_index.sh
source imtub.sh
#!-INCLUDES
