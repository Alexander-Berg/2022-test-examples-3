#!/bin/bash

set -o noglob

if [ -z "$CH_PASSWORD" ]; then
    echo "Need to set clickhouse password, like 'export CH_PASSWORD=...'"
    exit 1
fi
# {,} — multiplies file x 2
cat urls.txt{,}{,} | xargs -n 1 -P 32 -I '{}' ./runner.sh '{}'
