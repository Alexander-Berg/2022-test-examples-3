# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six

if six.PY2:
    import mock
else:
    import unittest.mock as mock

from search.priemka.yappy.src.scheduler import SchedulerReader
from search.priemka.yappy.src.yappy_lib.config_utils import get_test_config

from search.priemka.yappy.tests.utils.test_cases import TestCase


class SchedulerReaderTestCase(TestCase):

    @classmethod
    def setUpClass(cls):
        super(SchedulerReaderTestCase, cls).setUpClass()
        cls.sqs_patch = mock.patch('search.priemka.yappy.src.yappy_lib.clients.ClientsMock.sqs')
        cls.sqs_patch.start()
        cls.reader = SchedulerReader.from_config(config=get_test_config())
        cls.queue_patch = mock.patch.object(cls.reader, 'queue')
        cls.poll_patch = mock.patch.object(cls.reader, 'poll_data')
        cls.config_patch = mock.patch.object(cls.reader, 'config')
        cls.save_data_patch = mock.patch.object(cls.reader, 'save_data')
        cls.db_queue_patch = mock.patch.object(cls.reader, 'processor_tasks_sink')

    def setUp(self):
        super(SchedulerReaderTestCase, self).setUp()
        self.queue_patch.start()
        self.poll_patch.start()
        self.config_patch.start()
        self.save_data_patch.start()
        self.db_queue_patch.start()

    def tearDown(self):
        self.queue_patch.stop()
        self.poll_patch.stop()
        self.config_patch.stop()
        self.save_data_patch.stop()
        self.db_queue_patch.stop()
        super(SchedulerReaderTestCase, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        cls.sqs_patch.stop()
        super(SchedulerReaderTestCase, cls).tearDownClass()

    def test_poll_data_queue_polled_times(self):
        self.poll_patch.stop()
        n = 2
        self.reader.config.scheduler.pool_size = n
        self.reader.poll_data()
        poll_called = len(self.reader.queue.poll.call_args_list)
        self.assertEqual(poll_called, n)

    def test_poll_data_result(self):
        self.poll_patch.stop()
        n = 2
        queue_poll_side_effect = [
            ({'Body': 'task_1'}, {'Body': 'task_2'}),
            ({'Body': 'task_3'}, {'Body': 'task_4'}, {'Body': 'task_5'}),
        ]
        self.reader.queue.poll.side_effect = queue_poll_side_effect
        self.reader.config.scheduler.pool_size = n
        expected = []
        for records in queue_poll_side_effect:
            expected.extend([r['Body'] for r in records])
        result = self.reader.poll_data()
        self.assertEqual(sorted(result), sorted(expected))

    def test_run_once_polls_data(self):
        self.reader.run_once()
        self.reader.poll_data.assert_called()

    def test_run_once_saves_data(self):
        self.reader.run_once()
        self.reader.save_data.assert_called_with(self.reader.poll_data())

    def test_save_data_writes_to_sink_db_queue(self):
        self.save_data_patch.stop()
        tasks = mock.Mock()
        self.reader.save_data(tasks)
        self.reader.processor_tasks_sink.put.assert_called_with(tasks, timeout=mock.ANY)

    def test_poll_chunk_default(self):
        self.reader.config.scheduler.reader_poll_chunk = 0
        self.assertEqual(self.reader.queue_poll_chunk, self.reader.QUEUE_POLL_CHUNK)

    def test_poll_chunk_config(self):
        chunk = 543
        self.reader.config.scheduler.reader_poll_chunk = chunk
        self.assertEqual(self.reader.queue_poll_chunk, chunk)

    def test_db_sink_qlimit_factor(self):
        pool_size = 5
        chunk_size = 7
        factor = 2.5
        expected = int(chunk_size * pool_size * factor)
        self.reader.config.scheduler.db_sink_cache_capacity = 0
        self.reader.config.scheduler.pool_size = pool_size
        self.reader.config.scheduler.db_sink_cache_capacity_factor = factor
        with mock.patch.object(self.reader.__class__, 'queue_poll_chunk', new_callable=mock.PropertyMock) as chunk:
            chunk.return_value = chunk_size
            self.assertEqual(self.reader.sink_queue_limit, expected)

    def test_db_sink_qlimit(self):
        expected = 432
        self.reader.config.scheduler.db_sink_cache_capacity = expected
        self.assertEqual(self.reader.sink_queue_limit, expected)
