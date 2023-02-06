#!/usr/bin/env bash

set -ex

PrintUsageAndExit() {
    set +ex
    echo "Usage:"
    echo "$BASH_SOURCE destination_path grouping_url"
    exit
}

if [[ "$#" -lt 2 ]]; then
    PrintUsageAndExit
fi

export YT_PREFIX=//home/videoindex/

src=full/docbase/newdata/index
dst=$1
gu=$2

if [[ "$(yt read --format json $src/input/docids[\"$gu\"] | wc -l)" -lt 1 ]]; then
    set +ex
    echo "Grouping url $gu was not found in input"
    PrintUsageAndExit
fi

yt create -ir map_node $dst
yt create -ir map_node $dst/input
yt create -ir map_node $dst/output
yt create -ir map_node $dst/tmp

for table in $(yt list $src/input); do
    echo Getting $table...
    yt merge --mode sorted --src $src/input/$table"[\"$gu\"]" --dst $dst/input/$table
done
