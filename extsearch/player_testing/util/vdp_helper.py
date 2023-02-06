#!/usr/bin/env python
import argparse
import json
from urllib import quote


SANDBOX_URL = 'https://yastatic.net/video-player/0xef2b21c/pages-common/default/default.html'


def get_snail_url(code):
    return '{}#html={}'.format(SANDBOX_URL, quote(code))


def get_snail_params(code, device, country):
    return 'browser={},device={},country={}'.format('firefox' if code.find('//www.ivi.ru/') != -1 else 'chromium',
                                                  device.lower(),
                                                  country.lower())


def add_snail_params(args):
    data = json.load(args.input)
    for item in data:
        item['snail_url'] = get_snail_url(item['code'])
        item['snail_params'] = get_snail_params(item['code'], item['device'], item['country'])
    json.dump(data, args.output)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    sp = ap.add_subparsers(dest='cmd')
    add_player_sp = sp.add_parser('AddSnailParams')
    add_player_sp.add_argument('--input', required=True, type=argparse.FileType('r'))
    add_player_sp.add_argument('--output', required=True, type=argparse.FileType('w'))
    args = ap.parse_args()
    if args.cmd == 'AddSnailParams':
        add_snail_params(args)
    else:
        raise Exception('invalid cmd {}'.format(args.cmd))
