#!/bin/bash

set -euxo pipefail

source teamcity.sh
source tsum_mt.sh

DEPLOY_TIMEOUT_SECONDS=3600
CLEANUP_TIMEOUT_SECONDS=300

PROJECT="$1"
ENVIRONMENT="$2"
BRANCH="$3"

# Освобождаем среду
teamcity_log_block_begin "Освобождение среды $PROJECT $ENVIRONMENT"
cleanup_environment_retry "$PROJECT" "$ENVIRONMENT" "$DEPLOY_TIMEOUT_SECONDS"
wait_for_environment_to_become_idle "$PROJECT" "$ENVIRONMENT" "$CLEANUP_TIMEOUT_SECONDS"
teamcity_log_block_end "Освобождение среды $PROJECT $ENVIRONMENT"


teamcity_log_block_begin "Выкладка на $PROJECT $ENVIRONMENT"

# Строим тело запроса с ручными ресурсами
LAUNCH_REQUEST_BODY="$(
    get_environment "$PROJECT" "$ENVIRONMENT" |
        jq  --arg branch "$BRANCH" '{manualResources: (.defaultPipelineResources + {"ru.yandex.market.tsum.pipelines.common.resources.BranchRef": {"name":$branch}})}'
)"

# запускаем выкладку
LAUNCH_ID="$(launch_environment "$PROJECT" "$ENVIRONMENT" <<< "$LAUNCH_REQUEST_BODY")"

# ждём успешного окончания выкладки
wait_for_environment_to_become_ready "$PROJECT" "$ENVIRONMENT" "$DEPLOY_TIMEOUT_SECONDS" "$LAUNCH_ID"

teamcity_log_block_end "Выкладка на $PROJECT $ENVIRONMENT"


# Освобождаем среду
teamcity_log_block_begin "Освобождение среды $PROJECT $ENVIRONMENT"
cleanup_environment_retry "$PROJECT" "$ENVIRONMENT" "$DEPLOY_TIMEOUT_SECONDS"
wait_for_environment_to_become_idle "$PROJECT" "$ENVIRONMENT" "$CLEANUP_TIMEOUT_SECONDS"
teamcity_log_block_end "Освобождение среды $PROJECT $ENVIRONMENT"
