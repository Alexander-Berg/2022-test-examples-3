#!/bin/bash
set -eu
set -o pipefail

function print_usage {
    echo "Run WMS load testing"
    echo "Usage:"
    echo " * load test from local machine: run_load_test -l path_to_config"
    echo " * load test from tank:          run_load_test -r path_to_config TICKET-XXXX"
    echo ""
    echo "   path_to_config - load test configuration file (generally locate in one of sc/ subfolders)"
    echo "   TICKET-XXXX    - load test will be linked to this ticket"
    exit 1
}

function before_testing_action() {
    START_TIMESTAMP=$(date +%s)
    START_TIMESTAMP=$((${START_TIMESTAMP}-300)) #вычитаем 5 минут
    echo ""
    echo "Load testing started: $(date)"
    echo ""
    echo " - Metrics dashboard (last hour):"
    echo "    1: https://grafana.yandex-team.ru/d/XWonO7iGz/wms-health?orgId=1&from=now-1h&to=now&var-host=wms-load-app01e_market_yandex_net&var-wrapInfor=fulfillment_infor_rov&var-solomon=Load%20testing&var-solomon_host=wms-load-app01e&var-warehouse=04vt&var-vhost=wms-load_tst_vs_market_yandex_net&var-rabbitmq_cluster=mq_testing"
    echo "    2: https://grafana.yandex-team.ru/d/XWonO7iGz/wms-health?orgId=1&from=now-1h&to=now&var-host=wms-load-app02e_market_yandex_net&var-wrapInfor=fulfillment_infor_rov&var-solomon=Load%20testing&var-solomon_host=wms-load-app02e&var-warehouse=04vt&var-vhost=wms-load_tst_vs_market_yandex_net&var-rabbitmq_cluster=mq_testing"
    echo ""
}

export -f before_testing_action

function after_testing_action() {
    echo ""
    echo ""
    echo "Load testing stopped: $(date)"

    END_TIMESTAMP=$(date +%s)
    END_TIMESTAMP=$((${END_TIMESTAMP}+300)) #прибавляем 5 минут

    echo ""
    echo " - Metrics dashboard (test duration):"
    echo "    1: https://grafana.yandex-team.ru/d/XWonO7iGz/wms-health?orgId=1&from=${START_TIMESTAMP}000&to=${END_TIMESTAMP}000&var-host=wms-load-app01e_market_yandex_net&var-wrapInfor=fulfillment_infor_rov&var-solomon=Load%20testing&var-solomon_host=wms-load-app01e&var-warehouse=04vt&var-vhost=wms-load_tst_vs_market_yandex_net&var-rabbitmq_cluster=mq_testing"
    echo "    2: https://grafana.yandex-team.ru/d/XWonO7iGz/wms-health?orgId=1&from=${START_TIMESTAMP}000&to=${END_TIMESTAMP}000&var-host=wms-load-app02e_market_yandex_net&var-wrapInfor=fulfillment_infor_rov&var-solomon=Load%20testing&var-solomon_host=wms-load-app02e&var-warehouse=04vt&var-vhost=wms-load_tst_vs_market_yandex_net&var-rabbitmq_cluster=mq_testing"
}

export -f after_testing_action


#####
# Устанавливаем переменные из флагов запуска
#####

if [ -z "${1:-}" ]; then
    print_usage
fi

case $1 in
-l)
    TEST_MODE=local
    ;;
-r)
    TEST_MODE=remote
    ;;
esac

if [ -z "${2:-}" ]; then
    print_usage
fi
export CONFIG_PATH=$2

if [ "$TEST_MODE" = "remote" ]; then
    if [ -z "${3:-}" ]; then
        print_usage
    fi
    export LOAD_TICKET=$3
fi

if [ -z "${YA_BIN:-}" ]; then
    export YA_BIN=../../../ya
    if [ ! -f "$YA_BIN" ]; then
        echo "'ya' binary not found at $YA_BIN"
        exit 1
    fi
fi

if [ -z "${WMS_LOAD_TMP_DIR:-}" ]; then
    export WMS_LOAD_TMP_DIR=/tmp/wms_load/conf
fi

if [ -z "${WMS_LOAD_TESTING_LOG:-}" ]; then
    export WMS_LOAD_TESTING_LOG=$WMS_LOAD_TMP_DIR/fire.log
fi

if [ -z "${WMS_LOAD_GEN_LOG:-}" ]; then
    export WMS_LOAD_GEN_LOG=$WMS_LOAD_TMP_DIR/gen.log
fi

if [ -z "${WMS_LOAD_USER:-}" ]; then
    export WMS_LOAD_USER=$USER
fi

#####
# Собираем приложение
#####

echo " - Building application"
$YA_BIN make


#####
# Подготавливаем окружение
#####

rm -rf $WMS_LOAD_TMP_DIR
mkdir -p $WMS_LOAD_TMP_DIR


#####
# Запускаем генерацию патронов
#####

echo ""
echo ""
echo " - Generating load testing input"
bin/generate_ammo.sh
echo "   Configs generated"


#####
# Запускаем нагрузочный тест
#####

echo ""
echo ""
case $TEST_MODE in
local)
    echo " - Launching load testing from local machine"
    bin/load_local.sh
    ;;
remote)
    echo " - Launching load testing from remote machine"
    bin/load_remote.sh
    ;;
esac