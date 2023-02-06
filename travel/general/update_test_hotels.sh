#!/usr/bin/env bash
for i in {0..24}; do curl "http://indexer01f.vs.yandex.net:36445/get-data?data-type=tmp_sharded_hotels.$i.index&format-version=2" | openssl zlib -d > sharded_hotels.$i.index; done
for i in {0..24}; do wget http://elliptics.test.vertis.yandex.net/upload/tours/new_sharded_hotels.$i.index --post-file=sharded_hotels.$i.index -O -; done
for i in {0..24}; do curl "http://indexer-old-01-sas.test.vertis.yandex.net:36445/reload?data-type=sharded_hotels.$i.index" | gunzip; done