#!/usr/bin/env bash

cd "$(dirname ${BASH_SOURCE[0]})"
../../../../../../../../../../scripts/nanny/run-tool.sh \
    --host sas1-1102-023-sas-market-test--f93-15453.gencfg-c.yandex.net SetHeavyGoodFlagTool2 \
    --card-api-url http://mbo-card-api.tst.vs.market.yandex.net:33714 \
    $@
