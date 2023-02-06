#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

$DIR/../bin/buybox_report_parser \
--input_path $DIR/../data/top_gmv_buybox_active_msku.tsv \
--output_path //tmp/inenakhov/$(date +"%T") \
--batch_size 1 \
--user_region 2 \
--report_host rb.vs.market.yandex.net \
--cart_string "KYW9SFj-PpIs-4mAtY_uKw,EkglCsA_8VOHpBTrCUSW-w"
