#!/bin/bash

zlib='python -c "import sys,zlib; sys.stdout.write(zlib.decompress(sys.stdin.read()));"'

for i in {0..24}; do
  curl "indexer-old-01-sas.test.vertis.yandex.net:36445/get-data?data-type=tmp_sharded_hotels.${i}.index&format-version=2" | eval "$zlib" \
    | curl --data-binary @- "http://elliptics.test.vertis.yandex.net:80/upload/tours/new_sharded_hotels.${i}.index" -H "Content-Type: application/octet-stream"

done

for i in {0..24}; do
    curl "indexer-old-01-sas.test.vertis.yandex.net:36445/reload?data-type=sharded_hotels.${i}.index" -I
done