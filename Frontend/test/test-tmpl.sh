#!/bin/bash

LANGS="${LANGS:=ru en}"

export LANGS

# Set root project directory as active
cd "$(dirname "$0")/../.."

cp -f .trendbox/dump-manifest.ru.json build/manifest.ru.json
if [ -z "$BLOCKS" ]; then
    FILE=__reports/test-tmpl.log TZ=Etc/GMT-3 ./run ./tools/test-tmpl.sh $(ls src/blocks/desktop)
else
    FILE=__reports/test-tmpl.log TZ=Etc/GMT-3 ./run ./tools/test-tmpl.sh $BLOCKS
fi
