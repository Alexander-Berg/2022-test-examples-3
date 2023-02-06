#!/usr/bin/env bash

set -e

target_path="//home/metrics/metrics_executable/metrics_debug"
echo target_path: ${target_path}


for proxy in "hahn"
do
    echo "Uploading to ${proxy}..."
    cat ./metrics | yt upload ${target_path} --executable --proxy ${proxy}
    echo "Uploaded metrics binary to ${proxy}";
done
