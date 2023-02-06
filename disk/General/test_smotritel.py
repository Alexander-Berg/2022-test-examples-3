#!/usr/bin/python

from streaming.smotritel import test_batch
from common.graphite import send_batch

import logging

if __name__ == "__main__":
    logging.basicConfig(level=logging.DEBUG)

    playlists = ["https://bitdash-a.akamaihd.net/content/sintel/hls/video/10000kbit.m3u8" for i in range(10)]

    result = test_batch(playlists)
    start_times = result['start_times']

    send_batch([("media.disk.streaming.smotritel.test_external.first_chunk_time.p" + key, start_times[key]) for key in start_times.keys()])
