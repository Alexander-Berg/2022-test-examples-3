#!/usr/bin/env bash

read_yt_table() {
    table=$1
    yt read --format json ${table}
}

upload_to_sandbox() {
    base=$1
    ya upload -d "${base} base YT table content for video delayed view"\
        --skynet --do-not-remove --ttl inf ./${base}_base
}

get_base_table() {
    if [ $1 == "entity" ]
    then
        echo "//home/videodev/bidzilya/VIDEOPOISK-9078/test_entity_base"
    elif [ $1 == "serial" ]
    then
        echo "//home/videoquality/series/exports/series_data_for_video_delayed_view"
    fi
}

for base in entity serial
do
    echo "Start uploading ${base} base"
    base_table=$(get_base_table ${base})
    read_yt_table ${base_table} > ./${base}_base
    upload_to_sandbox ${base}
    rm ./${base}_base
done
