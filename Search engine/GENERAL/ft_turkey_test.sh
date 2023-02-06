#!/usr/local/bin/bash

LOG_LEVEL="INFO"

CMS_CONF="fasttier_turkey_test"
CMS_HOST="http://cmsearch00.yandex.ru/xmlrpc/bs"

FT_LAYER_NAME="$1"
FT_LAYER_SHARDS_NUM="$2"

ROOT_DIR="/home/andrewmaster/fasttier_turkey"
WRK_DIR="${ROOT_DIR}/wrk"
SCRIPTS_DIR="${ROOT_DIR}/scripts"
INDECES_DIR="/home/andrewmaster/shards"

FASTTIER_API="/Berkanavt/orange/release/bin/fasttierapi"
LOCK_FILE="${WRK_DIR}/ft.lock"
BSCONFIG_LOG="${ROOT_DIR}/bs.out"
INDEX_PREFIX="fasttier_turkey_test"

if [ ! -d ${WRK_DIR} ]; then
    echo "create working directory: ${WRK_DIR}"
    mkdir -p ${WRK_DIR}
fi


function prepareIndeces() {
    ${SCRIPTS_DIR}/ftClient.py \
        --cms-confname=${CMS_CONF} \
        --layer=${FT_LAYER_NAME} \
        --shards-num=${FT_LAYER_SHARDS_NUM} \
        --wrk-dir=${WRK_DIR} \
        --indeces-dir=${INDECES_DIR} \
        --fasttier-script=${FASTTIER_API} \
        --log-level=${LOG_LEVEL} \
        --cms-host=${CMS_HOST} \
	--bsconfig-log=${BSCONFIG_LOG} \
	--index-prefix=${INDEX_PREFIX}
}


function runAll() {
    prepareIndeces
}

runAll
