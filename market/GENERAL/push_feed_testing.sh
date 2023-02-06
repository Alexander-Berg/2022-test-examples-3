#!/usr/bin/env bash

# usage:
# $ ya make --checkout
# $ export TVM_SECRET=your_tvm_secret
# $ chmod 744 push_feed_testing.sh  # if needed
# fill SHOP_ID, FEED_WHID, URL
# $ ./push_feed_testing.sh

# for example: 10264169
SHOP_ID=
# for example: 0
BUSINESS_ID=
# for example: "200396944,172 200369699,147 200344511,145 200396943,171"
FEED_WHID=
# for example: "https://market-mbi-test.s3.mdst.yandex.net/supplier-feed/suppliers/10264169/feeds/200344511/data"
URL=

TOPIC="market-indexer/testing/white/datacamp-update-tasks"
TVM_CLIENT_ID=2002296

./push_update_task --shop-id $SHOP_ID --feedurl $URL --feeds $FEED_WHID --topic $TOPIC --tvm-client-id $TVM_CLIENT_ID --business-id $BUSINESS_ID
