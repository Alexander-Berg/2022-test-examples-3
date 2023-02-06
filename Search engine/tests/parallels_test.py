#!/usr/bin/env python
# -*- coding: utf-8 -*-

import unittest

from six.moves import range

from search.geo.tools.ranking import parallels


class DummyWorker(parallels.Worker):
    def work(self, src):
        return src * 10


class DummyProducerConsumer(parallels.Producer, parallels.Consumer):
    def __init__(self, n):
        self.n = n
        self.data = []

    def produce(self):
        for x in range(self.n):
            self.data.append('produce {0}'.format(x))
            yield x

    def consume(self, srcs_dsts):
        for src, dst in srcs_dsts:
            self.data.append('consume')


class TestParallels(unittest.TestCase):
    def test_single_onebyone_unlimited(self):

        prod_cons = DummyProducerConsumer(5)
        workers = [DummyWorker(), DummyWorker(), DummyWorker()]

        parallels.run_single_onebyone(prod_cons, workers, prod_cons, max_in_queue=0)
        self.assertEqual(prod_cons.data, [
            'produce 0',
            'produce 1',
            'produce 2',
            'consume',
            'produce 3',
            'consume',
            'produce 4',
            'consume',
            'consume',
            'consume'
        ])

    def test_single_onebyone_limited(self):
        prod_cons = DummyProducerConsumer(5)
        workers = [DummyWorker(), DummyWorker(), DummyWorker()]

        parallels.run_single_onebyone(prod_cons, workers, prod_cons, max_in_queue=2)
        self.assertEqual(prod_cons.data, [
            'produce 0',
            'produce 1',
            'consume',
            'produce 2',
            'consume',
            'produce 3',
            'consume',
            'produce 4',
            'consume',
            'consume'
        ])

    def test_attempts(self):
        class FirstFailWorker(parallels.Worker):
            def __init__(self):
                self.was = set()

            def work(self, src):
                if src in self.was:
                    return src
                else:
                    self.was.add(src)
                    raise ValueError()

        for kind in ('threading', 'multiprocessing'):
            prod_cons = DummyProducerConsumer(20)
            parallels.run_single_balancing(prod_cons, [FirstFailWorker() for _ in range(10)], prod_cons, attempts=2, silent=True, kind=kind)

            prod_cons = DummyProducerConsumer(20)
            self.assertRaises(parallels.ParallelsError, parallels.run_single_balancing, prod_cons, [FirstFailWorker() for _ in range(10)], prod_cons, attempts=1, silent=True, kind=kind)

    def test_max_error_count(self):
        class FirstFailWorker(parallels.Worker):
            def __init__(self):
                self.was = set()

            def work(self, src):
                if src % 2 == 1:
                    return src
                else:
                    raise ValueError()

        for kind in ('threading', 'multiprocessing'):
            prod_cons = DummyProducerConsumer(10)
            self.assertRaises(parallels.ParallelsError, parallels.run_single_onebyone, prod_cons, [FirstFailWorker()], prod_cons, max_error_count=4, silent=True, kind=kind)
            self.assertEqual(prod_cons.data.count('consume'), 4)
