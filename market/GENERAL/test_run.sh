TIME=$(date "+%Y-%m-%dT%H:%M:%S") && \
SPACED_TIME="${TIME/T/ }" && \
../../../../ya tool yt --proxy arnold copy -f -r \
'//home/market/production/monetize/dynamic_pricing/parsing/rthub/full_state_parsed/latest' \
'//tmp/fookh/parsed_state/'$TIME && \
../../../../ya tool yt --proxy arnold link -f -r \
'//tmp/fookh/parsed_state/'$TIME \
'//tmp/fookh/parsed_state/latest' && \
export YT_TOKEN=`cat ~/.yt/token` && \
export YQL_TOKEN=`cat ~/.yql/token` && \
./parse_html_state \
--yt-cluster arnold \
--yt-pool fookh \
--html-dir '//home/market/production/monetize/dynamic_pricing/parsing/rthub/full_state' \
--parsed-table '//tmp/fookh/parsed_state/'$TIME \
--timestamp "$SPACED_TIME" \
--window-in-month 6 \
--parsers-table '//home/market/production/monetize/dynamic_pricing/parsing/ref_shops_parsers/latest'
