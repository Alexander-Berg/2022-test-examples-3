#!/usr/bin/env bash

# usage:
# $ ya make --checkout
# $ export TVM_SECRET=your_tvm_secret
# $ chmod 744 push_feeds_testing.sh  # if needed
# scp mi01ht.market.yandex.net:/var/lib/yandex/getter/mbi/recent/suppliers.dat.testing ./
# $ ./push_feeds_testing.sh


TOPIC="market-indexer/testing/blue/datacamp-update-tasks"
TVM_CLIENT_ID=2011472

./push_update_tasks --topic $TOPIC  --tvm-client-id $TVM_CLIENT_ID --shopsdat ~/suppliers.dat.testing
