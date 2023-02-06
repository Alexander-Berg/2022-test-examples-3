#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

$DIR/../bin/buybox_report_parser \
--input_path $DIR/../data/top_queries.tsv \
--output_path //tmp/${USER}/$(date +"%T") \
--batch_size 1 \
--report_host "warehouse-report.vs.market.yandex.net:17051" \
--user_region 213 \
--cart_string "" \
--prime \
--rearr_flags "market_blue_buybox_by_gmv_ue=1;market_blue_buybox_max_exchange=100;market_blue_buybox_max_price_rel=1000;market_blue_buybox_max_gmv_rel=1000;market_blue_buybox_elasticity_decreasing=0;"
