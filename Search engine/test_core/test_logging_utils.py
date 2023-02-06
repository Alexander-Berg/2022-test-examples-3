# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import logging
import uuid

from search.martylib.core.logging_utils import MonitoredLogger
from search.martylib.core.logging_utils.binlog import Tracer
from search.martylib.test_utils import TestCase
from search.martylib.unistat.metrics import GlobalMetricStorage


class TestMonitoredLogger(TestCase):
    LOGGER_CLASS = MonitoredLogger

    metrics = GlobalMetricStorage()

    def test_empty_metric_name(self):
        logger = logging.getLogger('martylib.{}'.format(uuid.uuid4()))
        logger.info('hopefully nobody will ever read this code twice')

    def test_non_empty_metric_name(self):
        logger = logging.getLogger('martylib.{}'.format(uuid.uuid4()))
        logger.info('hopefully nobody will ever read this code twice', metric_name='escaped')

        self.assertEqual(
            self.metrics.to_protobuf().numerical['global-{}-escaped_summ'.format(logger.name)], 1
        )

    def test_metric_name_escaping(self):
        logger = logging.getLogger('martylib.{}'.format(uuid.uuid4()))
        logger.info('hopefully nobody will ever read this code twice', metric_name='not escaped')

        self.assertEqual(
            self.metrics.to_protobuf().numerical['global-{}-not-escaped_summ'.format(logger.name)], 1
        )


class TestBinlogTracer(TestCase):

    def test_invalid_log_handling(self):
        logger = logging.getLogger('binlog_test_logger')
        handler = Tracer(directory='.', base_filename='test')
        level = handler.global_trace_manager.logging_level
        logger.addHandler(handler)
        logger.propagate = False
        logger.level = level
        try:
            logger.log(level, '%s %s', 'arg')
        except TypeError as err:
            self.fail('unexpected exception: {}'.format(err))
