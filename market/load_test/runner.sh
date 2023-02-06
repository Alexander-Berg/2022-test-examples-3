#!/bin/bash

set -o noglob

SCHEMA='http'
CH_HOSTNAME='localhost:1234'
# SCHEMA='https'
# CH_HOSTNAME='mstat-ch-cache.tst.vs.market.yandex.net'
# CH_HOSTNAME='mstat-ch-cache.vs.market.yandex.net'
q=$1
# echo ${q}
curl -s --insecure -o /dev/null -w "%{http_code}\n" -XPOST -g -d "${q}" "${SCHEMA}://cubes:${CH_PASSWORD}@${CH_HOSTNAME}/?database=cubes&count_distinct_implementation=uniqCombined"
