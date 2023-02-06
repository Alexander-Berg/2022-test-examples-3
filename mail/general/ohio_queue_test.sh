#!/bin/bash
set -exo pipefail

export DATA_DIR=/data
export ADDITIONAL_ARGS="
-DPORTO=1
-Dzoolooser.traffic-compress.zstd=false
-XX:ActiveProcessorCount=2
-Dru.yandex.lucene.default-cache-size=$((32 * 1024 * 1024))
-Dru.yandex.lucene.fsync-bytes-interval=$((32 * 1024 * 1024))
-Dru.yandex.lucene-storage.index-threads=1
-Dru.yandex.lucene-storage.merge-threads=1
-Dru.yandex.lucene-storage.max-live-data-size=$((32 * 1024 * 1024))
-Dru.yandex.lucene-storage.max-cache-weight=$((32 * 1024 * 1024))
-Dru.yandex.lucene-storage.max-segment-size=$((32 * 1024 * 1024))
-Dru.yandex.lucene-queue.ram-buffer-size-mb=256
-Dru.yandex.lucene-storage.zoo-hash-bloom-filter=false
-Dru.yandex.lucene-storage.in-memory-fields-index=false"
export MEMORY_GAP=$((128 * 1024 * 1024))
export CONFIG_TEMPLATE=queue-test.cfg.template
export ARGS="queue.cfg server.conf"

