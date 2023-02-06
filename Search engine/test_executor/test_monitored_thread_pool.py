# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import time

from search.martylib.executor import MonitoredThreadPoolExecutor
from search.martylib.test_utils import TestCase
from search.martylib.unistat.metrics import merge_data


class TestMonitoredThreadPoolExecutor(TestCase):
    @staticmethod
    def dummy():
        time.sleep(2)

    def test_named(self):
        executor = MonitoredThreadPoolExecutor(max_workers=10, thread_name_prefix='test')
        for _ in range(15):
            executor.submit(self.dummy)

        # Let MonitoredThreadPoolExecutor.metric_updater update values.
        time.sleep(1)

        metrics = merge_data(*(
            pool.get_metrics()
            for pool in MonitoredThreadPoolExecutor.instances
        ))

        # 10 tasks pending, 5 waiting in queue.
        self.assertEqual(metrics.numerical['test-thread-pool-queue-size_ammm'], 5)
        self.assertEqual(metrics.numerical['test-thread-pool-overflow-perc_ammm'], 50)
