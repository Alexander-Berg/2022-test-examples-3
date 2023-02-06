#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

$DIR/../bin/buybox_report_parser \
--input_path $DIR/../data/input.tsv \
--output_path //tmp/${USER}/$(date +"%T") \
--batch_size 1 \
--report_host "warehouse-report.vs.market.yandex.net:17051" \
--user_region 213 \
--cart_string "" \
--enable_top6
