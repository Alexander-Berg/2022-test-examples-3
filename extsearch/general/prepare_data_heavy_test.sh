#!/bin/bash
ya make -r ../tools/loops_exporter ../tools/pregen_exporter || exit 1
mkdir data_tmp
../tools/loops_exporter/loops_exporter --skip_generation_check --table //home/music/generative/testing_loops data_tmp/loopsinfo.pb || exit 1
../tools/pregen_exporter/pregen_exporter --table //home/music/generative/testing_tracks \
    -l data_tmp/loopsinfo.pb data_tmp/pregeninfo.pb || exit 1
mv data data_bak
mv data_tmp data
ya upload  --ttl=365 -T GENERATIVE_MUSIC_TEST_DATA --tar data
rm -rf data
mv data_bak data
