#!/usr/bin/env python

import yt.wrapper as yt

import argparse
import random


class SplitMapper(object):
    def __init__(self, ratio, seed):
        self.ratio = ratio
        if seed is not None:
            random.seed(int(seed))

    def __call__(self, row):
        if random.uniform(0, 1) < self.ratio:
            yield yt.create_table_switch(1)
        else:
            yield yt.create_table_switch(0)
        yield row


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--proxy', required=True)
    ap.add_argument('--input', required=True)
    ap.add_argument('--train', required=True)
    ap.add_argument('--test', required=True)
    ap.add_argument('--ratio', required=True)
    ap.add_argument('--seed', required=False)
    args = ap.parse_args()
    client = yt.YtClient(proxy=args.proxy)
    with client.Transaction():
        attrs = {"schema": [{"name": "key", "type": "string"}, {"name": "value", "type": "string"}]}
        client.run_map(SplitMapper(float(args.ratio), args.seed), args.input, [yt.TablePath(args.train, attributes=attrs), yt.TablePath(args.test, attributes=attrs)])
