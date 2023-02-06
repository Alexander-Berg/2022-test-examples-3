#!/usr/bin/env python
# coding=utf-8

from timeit import default_timer as timer

class benchmark(object):
    def __init__(self):
        self.time = 0

    def start(self):
        self.start = timer()

    def stop(self, *args):
        self.time = timer() - self.start

    def get_elapsed(self):
        return self.time

