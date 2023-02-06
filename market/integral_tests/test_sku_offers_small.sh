#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

$DIR/../bin/buybox_report_parser \
--input_path $DIR/../data/input.tsv \
--output_path //tmp/inenakhov/$(date +"%T") \
--batch_size 1 \
--report_host "rb.vs.market.yandex.net" \
--user_region 213 \
--cart_string "" \
--rearr_flags "market_blue_buybox_by_gmv_ue=0;market_blue_buybox_max_exchange=100;market_blue_buybox_max_price_rel=1000;market_blue_buybox_max_gmv_rel=1000;market_blue_buybox_elasticity_decreasing=0;"
