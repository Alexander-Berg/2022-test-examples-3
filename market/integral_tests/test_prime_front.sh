#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

$DIR/../bin/buybox_report_parser \
--input_path $DIR/../data/prime_input_recommendator_msc.json \
--output_path //tmp/${USER}/$(date +"%T") \
--user_region 213 \
--cart_string "" \
--prime \
--json_format \
--front_request
