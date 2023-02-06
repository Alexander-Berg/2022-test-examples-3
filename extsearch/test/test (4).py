#!/usr/bin/env python3

import sys
import urllib.parse
import urllib.request
import json
import argparse


class TRequestMaker(object):
    def __init__(self, *args, **kwargs):
        self._TestUrl = 'https://video-station-starter-testing.n.yandex-team.ru/'
        self._SearchUrl = 'https://hamster.yandex.ru/video/search?text=%s&nocache=da&json_dump=searchdata.clips.*&srcask=VIDEO'
        self._Uid = "1"
        self._ReqId = "2"
        self._DeviceId = "3"

    def make_request(self, data):
        req = urllib.request.Request(self._TestUrl, headers={'X-Uid': self._Uid, 'X-Req-Id': self._ReqId, 'Content-Type': "application/json"}, data=json.dumps(data).encode())
        resp = urllib.request.urlopen(req)
        return resp

    def make_data(self, data):
        return {
            'device': self._DeviceId,
            'msg': {
                'provider_item_id': data['url'],
                'source_host': data['Host'],
                'player_id': data['PlayerId'],
                'play_uru': data['HtmlVideoPlayer'],
                'provider_name': 'yandex.ru',
                'type': 'video'
            }
        }

    def make_query(self, query):
        try:
            resp = urllib.request.urlopen(self._SearchUrl % urllib.parse.quote(query))
            resp = json.loads(resp.read().decode('utf-8'))
            for i in resp["searchdata.clips.*"][:5]:
                try:
                    data = self.make_data(i)
                    yield (data, self.make_request(data))
                except Exception as e:
                    print('got exception:', e, i['url'])
        except Exception as e:
            print('error:', e)


def get_args():
    parser = argparse.ArgumentParser()
    parser.add_argument('--reqid', default='2', type=str, help='to ddefine X-Req-Id')
    parser.add_argument('--device', default='3',  type=str, help='device id for push')
    parser.add_argument('--uid', default='1', type=str, help='user id')
    return parser.parse_args()


def main():
    args = get_args()
    requester = TRequestMaker(args)
    for i in sys.stdin.readlines():
        query = i.strip().encode('utf-8')
        if not query:
            continue
        result = requester.make_query(query)
        for inp, out in result:
            try:
                out = json.loads(out.read().decode('utf-8'))
                print('%s: %s' % (inp['msg']['provider_item_id'], out['code']))
            except Exception as e:
                print('got Eexception:', e)


if __name__ == "__main__":
    main()
