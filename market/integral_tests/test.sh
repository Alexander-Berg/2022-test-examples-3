#!/usr/bin/env bash

../bin/buybox_delivery_parser \
--input_path ../data/test.tsv \
--output_path //tmp/inenakhov/deliver_parsing_test8 \
--report_host "warehouse-report.vs.market.yandex.net:17051" \
--user_region 213 \
--timeout 0.3
