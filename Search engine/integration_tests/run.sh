#!/bin/bash -i

type ya &> /dev/null || {
    # set a variable ARCADIA if it is empty
    : ${ARCADIA:=$ARCADIA_ROOT}
    # fallback to the source path with /search* stripped
    : ${ARCADIA:=${BASH_SOURCE%%/search*}}
    ya() { "$ARCADIA/ya" "$@"; }
}

# Все данные, необходимые для запуска тестов вручную, хранятся в Секретнице
# https://yav.yandex-team.ru/secret/sec-01e7qvnscyeq4pa59ctyngsn39
# ПРОВЕРЬТЕ СВОИ ПРАВА НА СЕКРЕТ !!!
# НЕ ЗАБУДЬТЕ УСТАНОВИТЬ ALIAS с абсолютным путем к ya tool, например: alias ya="/arcadia/ya"

# alias ya-vault="ya vault get version ${SECRET_VERSION} --json"
# alias ya-jq="ya tool jq"
# ya-vault | ya-jq -ar '.value.TEST_XML_PARTNERS' | ya-jq -S . > ${TEST_XML_PARTNERS_PATH}

set -x

# by default we use https://yav.yandex-team.ru/secret/sec-01f3dnxszdxw71xyv8fszr22hy
export SECRET_VERSION

# SOY_MODE=[0|1]
export SOY_MODE=${SOY_MODE:-0}

# SOY_MAP_OP_TYPE=[http|scraper]
export SOY_MAP_OP_TYPE=${SOY_MAP_OP_TYPE:-http}

# LOGGING_MODE=[DEBUG|INFO|ERROR] see python doc for logging
export LOGGING_MODE=${LOGGING_MODE:-DEBUG}

# example hosts for var BETA_HOST: graph-mapping-web-199-1.hamster.yandex.ru, report-web-1.hamster.yandex.ru
export BETA_HOST=${BETA_HOST:-'hamster.yandex.ru'}

# with this token we authorize in the yav.yandex-team.ru And you need read access for secret[SECRET_VERSION] for this user
# by default use ssh authorization
export OAUTH_TOKEN

# path to directory where store soy_id. Set in the sandbox for aborting soy batches
export PATH_TO_SOY_BATCH

# this pool used for soy downloading
export SOY_POOL

# also we have env TEST_ID and EXP_CONFS. TODO(kozunov) need write how to use it

ulimit -n 8192 \
&& ya make -rA --test-disable-timeout --show-passed-tests --test-stderr \
   "$@"
