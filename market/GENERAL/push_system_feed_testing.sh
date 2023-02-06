#!/usr/bin/env bash

# usage:
# $ ya make --checkout
# $ export TVM_SECRET=your_tvm_secret
# $ chmod 744 push_feed_testing.sh  # if needed
# $ ./push_feed_testing.sh

SHOP_ID=10336543
FEED_WHID="200445145,171"
URL="https://market-mbi-test.s3.mdst.yandex.net/supplier-feed/suppliers/10336543/feeds/200445143/data"
TOPIC="market-indexer/testing/blue/datacamp-update-tasks"
TVM_CLIENT_ID=2011472

./push_update_task --shop-id $SHOP_ID --feedurl $URL --feeds $FEED_WHID --topic $TOPIC --tvm-client-id $TVM_CLIENT_ID
