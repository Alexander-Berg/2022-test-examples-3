#!/usr/bin/env python
# -*- coding: utf-8 -*-


class TestShops(object):
    def __init__(self, test_shops):
        self.test_shops = test_shops

    def save(self, out):
        out.write('\n'.join([str(x) for x in self.test_shops]))
