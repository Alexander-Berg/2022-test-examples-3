# coding: utf-8

from __future__ import absolute_import
from __future__ import unicode_literals
from __future__ import division

from random import Random

from search.resonance.tester.proto.script_pb2 import TSelectorConfig


class Selector(object):
    seed: int
    sample_size: int
    sample_part: float
    inversed: bool

    def __init__(self, config: TSelectorConfig):
        self.seed = config.Seed or None
        self.inversed = config.Inversed
        if config.SampleSize:
            self.sample_size = config.SampleSize  # will be returned block sized $sample_size$ on query
            self.sample_part = None
        else:
            self.sample_part = config.SamplePart  # will be returned block sized $sample_part$ * size on query
            self.sample_size = None

    def select(self, values, inversed: bool = False):
        values = list(values)

        Random(self.seed).shuffle(values)

        sample_size = self.sample_size
        if sample_size is None:
            sample_size = int(self.sample_part * len(values))

        if self.inversed:
            inversed = not inversed

        if inversed:
            return values[sample_size:]
        else:
            return values[:sample_size]
