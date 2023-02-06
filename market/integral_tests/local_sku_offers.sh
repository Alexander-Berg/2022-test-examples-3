#!/usr/bin/env bash

./buybox_report_parser --user_region 213 --report_host warehouse-report.vs.market.yandex.net:17051 --stdout True --msku_id 100523529819 | jq
./buybox_report_parser --user_region 213 --report_host rb.vs.market.yandex.net --stdout True --prime True --query iphone | jq
