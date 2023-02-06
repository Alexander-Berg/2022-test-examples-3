#!/usr/bin/env python
from __init__ import SQSClient
import argparse


class DumbConfig(object):
    def __init__(self, acc):
        self.visibility_timeout = 180
        self.max_receive_count = 1
        self.endpoint_url = 'http://sqs.yandex.net:8771'
        self.account = acc


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--action', required=True, type=str)
    ap.add_argument('--queue', required=True, type=str)
    ap.add_argument('--account', default='robotvideo', type=str)
    args = ap.parse_args()
    conf = DumbConfig(args.account)
    client = SQSClient(conf)
    if args.action == 'create':
        client.get_queue(args.queue)
    elif args.action == 'drop':
        client.drop_queue(args.queue)
    else:
        raise Exception('invalid action {}'.format(args.queue))
