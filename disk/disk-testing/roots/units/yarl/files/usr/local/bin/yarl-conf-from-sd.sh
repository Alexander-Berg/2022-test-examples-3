#!/bin/bash

DEPLOY_STAGE_ID=$1
DEPLOY_UNIT_ID=$2
TMPL_FILENAME="/etc/yarl/yarl.tmpl"


REALS=$(reals_resolver -from sd -id "$DEPLOY_STAGE_ID.$DEPLOY_UNIT_ID" | sort)

[ -z "$REALS" ] && exit 0

QUOTA_UPSTREAMS=""
COUNTERS_UPSTREAMS=""

for real in $REALS; do
    QUOTA_UPSTREAMS="$QUOTA_UPSTREAMS
      - type: grpc
        endpoint: \"$real:14589\"
        grpc: *grpc"
    COUNTERS_UPSTREAMS="$COUNTERS_UPSTREAMS
       - endpoint: \"$real:14589\""
done


export QUOTA_UPSTREAMS
export COUNTERS_UPSTREAMS

envsubst \$QUOTA_UPSTREAMS,\$COUNTERS_UPSTREAMS < $TMPL_FILENAME
