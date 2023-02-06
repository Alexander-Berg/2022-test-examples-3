#!/usr/bin/env bash
set -e -x

WIDTH=1280
HEIGHT=720
TMPDIR=$(mktemp -d)
SECONDS=5
FPS=8

if [ -z "$1" -o -z "$2" ]; then
    echo "please use mksplash.py with JSON configs!"
    echo "usage: $0 <thumb link> <label>" 
    exit 1
fi

frames=$((SECONDS * FPS))
curl $1 -o ${TMPDIR}/logo.png

for i in $(seq 0 $frames); do
    num=$(printf "%03d" $i)
    cp ${TMPDIR}/logo.png ${TMPDIR}/logo${num}.png
done
rm ${TMPDIR}/logo.png

rm -rf splash/$2
mkdir -p splash/$2
spinner=$(pwd)/loading.gif

cd splash/$2 && ffmpeg -y \
    -f lavfi -i anullsrc=channel_layout=stereo:sample_rate=44100 \
    -f image2 -framerate ${FPS} -i ${TMPDIR}/logo%03d.png -pix_fmt yuv420p \
    -filter_complex "[1]scale=${WIDTH}:${HEIGHT}" \
    -c:v libx264 -profile:v main -preset veryslow -frames ${frames} -c:a mp3 -g ${FPS} -hls_time 1 splash.m3u8

rm -rf ${TMPDIR}
