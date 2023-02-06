#!/bin/bash
set -exo pipefail

source familypay_lbconsumer_base.sh

export FAMILY_EVENTS_CLIENT_ID=/familypay/passport-test-family-events-consumer
export FAMILY_EVENTS_TOPICS=passport/passport-family-events-testing-log
export CARD_EVENTS_CLIENT_ID=/familypay/cards-events-consumer-test
export CARD_EVENTS_TOPICS=trust/test/card-events
export GLOGOUT_EVENTS_CLIENT_ID=/familypay/glogout-events-consumer-test
export GLOGOUT_EVENTS_TOPICS=passport/passport-account-modification-log-fast-testing
export PHONE_EVENTS_CLIENT_ID=/familypay/phone-events-consumer-test
export PHONE_EVENTS_TOPICS=passport/passport-account-modification-log-fast-testing
export TVM_CLIENT_ID=2021716
export EXTRA_CONFIG="debug-flags.conf"
export DEBUG_FLAGS=log-payload
export TARGET_HOST=https://familypay-backend-test.so.yandex.net
export FAMILYPAY_HOST=https://familypay-backend-test.so.yandex.net
export YANDEXPAY_HOST=http://localhost:$SNITCH_PORT
export YANDEXPAY_TVM_CLIENT_ID=$TVM_CLIENT_ID

