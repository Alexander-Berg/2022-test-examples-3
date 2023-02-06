#!/bin/bash

cd "$(dirname "$0")"

TOKEN="$(../get-token.sh)"

FROM="$(date -d '-7 days' '+%Y-%m-%d 00:00:00')"
TO="$(date '+%Y-%m-%d 23:59:59')"

SCENARIO_ID="2820"

curl "https://smartcalls.io/api/v2/calls/searchCalls?call_direction=incoming&scenario_id=${SCENARIO_ID}&page=1&datetime_start=${FROM}&datetime_end=${TO}&domain=yamarket&access_token=${TOKEN}" | jq '.'
