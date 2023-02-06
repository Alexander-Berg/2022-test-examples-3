#!/bin/bash
set -exo pipefail

source ohio_lbconsumer_base.sh

export TRUST_CLIENT_ID=/order-history-importer/trust-data-consumer-test
export TRUST_TOPICS=trust/test/payments
export YANDEXPAY_CLIENT_ID=/order-history-importer/yandexpay-test-consumer
export YANDEXPAY_TOPICS=yandex-pay/test/payments
export SRC_CLIENT_ID=2021145
export EXTRA_CONFIG="debug-flags.conf"
export DEBUG_FLAGS=log-payload
export PRODUCER_HOST=https://ohio-backend-test.so.yandex.net:8443

