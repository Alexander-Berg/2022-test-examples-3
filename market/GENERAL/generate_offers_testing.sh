#!/usr/bin/env bash

# usage:
# $ ya make --checkout
# $ export TVM_SECRET=your_tvm_secret
# or
# $ export TVM_SECRET_PATH=your_tvm_secret_path

# $ chmod 744 generate_offers_testing.sh  # if needed
# $ ./generate_offers_testing.sh

TOPIC="market-indexer/testing/blue/datacamp-offers"
PARTITION_COUNT=4
TVM_CLIENT_ID=2011472

SHOP_ID=10369797
OFFER_ID_FROM=0
OFFER_ID_TO=750000

./generate_offers --topic $TOPIC --tvm-client-id $TVM_CLIENT_ID --partition-count $PARTITION_COUNT --shop-id $SHOP_ID --offer-id-from $OFFER_ID_FROM --offer-id-to $OFFER_ID_TO
