#!/usr/bin/env bash

#!+INCLUDES
source statisticslight.sh
#!-INCLUDES

# --------------------------------------------------------------------------- #
# Make statistics-light after build, not after store
# --------------------------------------------------------------------------- #

statistics-light-start() {
    INDEX_STATE=$(get-full-index-lastbuilt)
    set-state-yt //home/${MR_PREFIX}/index statistics.state ${INDEX_STATE}
}

statistics-light-hostfactors() {
    # hostfactors copy from prod -> same stats
    true
}
