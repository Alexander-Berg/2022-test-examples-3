#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

$DIR/../bin/buybox_report_parser \
--input_path $DIR/../data/top_gmv_buybox_active_msku.tsv \
--output_path //tmp/${USER}/$(date +"%T") \
--batch_size 1 \
--user_region 2 \
--report_host "warehouse-report.vs.market.yandex.net:17051" \
--cart_string "KYW9SFj-PpIs-4mAtY_uKw,EkglCsA_8VOHpBTrCUSW-w" \
--rearr_flags "market_ranging_cpa_by_ue_in_top_coef_b=0;market_tweak_search_auction_white_cpa_fee_params=0.9,0.0015,1;market_tweak_search_auction_white_cpa_fee_no_text_params=0.3,0.0015,1;show_log_do_with_model_white_auction=1;enable_business_id=1;use_offer_type_priority_as_main_factor_in_do=1"
