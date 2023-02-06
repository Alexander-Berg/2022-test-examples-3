#!/usr/bin/env python
import argparse
import json
import subprocess


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--config', type=argparse.FileType(), required=True, help='extsearch/video/robot/crawling/player_testing/config/live_proxy.prod.json')
    ap.add_argument('--channels', type=argparse.FileType(), required=True, help='alice/bass/data/restreamed_channels.json')
    args = ap.parse_args()
    conf = json.load(args.config)['channels']
    channels = json.loads('\n'.join(args.channels.read().split('\n')[1:]))
    for key, item in conf.iteritems():
        content_id = item['content_id']
        thumb = 'https:{}'.format(channels[content_id]['thumbnail'])
        print key, thumb
        subprocess.check_call(['bash', '-c', './mksplash.sh {} {}'.format(thumb, key)])
