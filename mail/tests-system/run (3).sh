#!/bin/bash

set -eEo pipefail

if [[ ! -v BUILD_ROOT_DIR ]]; then
    BUILD_ROOT_DIR=".."
    echo warning: expecting build root at ${BUILD_ROOT_DIR}
fi;

function get_secrets() {
    echo get secrets
    ${BUILD_ROOT_DIR}/../../ya make ${BUILD_ROOT_DIR}/../imap/tests-system/get_secrets/
    ${BUILD_ROOT_DIR}/../imap/tests-system/get_secrets/get_secrets
    chmod 0600 .pgpass
    SECRETSDIR=app/secrets
    mkdir -p $SECRETSDIR && rm -rf $SECRETSDIR/*
    mv .pgpass .xiva_api_token.yml tvm_secret cert.pem $SECRETSDIR
}

function copy_configs {
    echo copy configs
    CONFDIR=app/config
    mkdir -p $CONFDIR && rm -rf $CONFDIR/*
    cp -a ${BUILD_ROOT_DIR}/config/* $CONFDIR/
    ln -sf ${BUILD_ROOT_DIR}/../../../macs_pg/etc/query.conf $CONFDIR/query.conf
}

copy_configs
get_secrets
echo run pop3
PGPASSFILE=app/config/.pgpass ${BUILD_ROOT_DIR}/app app/config/dev.yml