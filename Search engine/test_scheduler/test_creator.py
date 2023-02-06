# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six
from typing import AnyStr  # noqa

from search.priemka.yappy.src.yappy_lib.config_utils import get_test_config
from search.priemka.yappy.src.scheduler import SchedulerCreator

from search.priemka.yappy.tests.utils.test_cases import TestCaseWithDB

if six.PY2:
    import mock
else:
    import unittest.mock as mock


class SchedulerCreatorTest(TestCaseWithDB):

    @classmethod
    def setUpClass(cls):
        super(SchedulerCreatorTest, cls).setUpClass()
        cls.sqs_patch = mock.patch('search.priemka.yappy.src.yappy_lib.clients.ClientsMock.sqs')
        cls.sqs_patch.start()
        start_partitioning_patch = mock.patch('search.priemka.yappy.src.yappy_lib.queue.partitions_manager.DBPartitionsManager.start')
        with start_partitioning_patch:
            cls.creator = SchedulerCreator.from_config(config=get_test_config())

    @classmethod
    def tearDownClass(cls):
        super(SchedulerCreatorTest, cls).tearDownClass()
        cls.sqs_patch.stop()


class SchedulerCreatorRunTest(SchedulerCreatorTest):

    @classmethod
    def setUpClass(cls):
        super(SchedulerCreatorRunTest, cls).setUpClass()
        cls.apply_filters_patch = mock.patch.object(
            cls.creator.processor_tasks_source.partitions_manager,
            'apply_partition_filters',
            side_effect=lambda x: x,
        )
        cls.create_tasks_patch = mock.patch.object(cls.creator, 'create_tasks')
        cls.get_tasks_patch = mock.patch.object(cls.creator.processor_tasks_source, 'poll')
        cls.queue_post_query_patch = mock.patch.object(cls.creator.processor_tasks_source, 'post_query_hook')
        cls.queue_post_session_patch = mock.patch.object(cls.creator.processor_tasks_source, 'post_session_hook')

    def setUp(self):
        self.apply_filters = self.apply_filters_patch.start()
        self.create_tasks = self.create_tasks_patch.start()
        self.get_tasks = self.get_tasks_patch.start()
        self.queue_post_query = self.queue_post_query_patch.start()
        self.queue_post_sesison = self.queue_post_session_patch.start()
        self.create_tasks.return_value = []
        self.get_tasks.return_value = []

    def tearDown(self):
        self.create_tasks_patch.stop()
        self.get_tasks_patch.stop()
        self.queue_post_query_patch.stop()
        self.queue_post_session_patch.stop()

    def test_waiting_tasks_limit_applied(self):
        self.get_tasks_patch.stop()
        expected_limit = 100
        self.creator.config.scheduler.tasks_limit = expected_limit
        with mock.patch('search.priemka.yappy.src.scheduler.queue.waiting_schedulers_tasks') as tasks_query:
            self.creator.run_once()
            tasks_query().limit.assert_called_with(expected_limit)

    def test_task_sleep_interval(self):
        expected = self.creator.TASK_SLEEP_INTERVAL
        limit = self.creator.processor_tasks_source.default_task_sleep_interval
        self.assertEqual(limit, expected)

    @mock.patch('search.priemka.yappy.src.scheduler.scheduler.now')
    def test_run_once_calls(self, now):
        manager = mock.Mock()
        manager.attach_mock(self.get_tasks, 'poll')
        manager.attach_mock(self.create_tasks, 'create_tasks')
        self.get_tasks.return_value = mock.MagicMock('dummy tasks set')
        expected = [
            mock.call.poll(
                chunk_size=self.creator.config.scheduler.tasks_limit,
                _now=now().timestamp(),
                trace_kwargs=mock.ANY,
            ),
            mock.call.create_tasks(self.get_tasks.return_value)
        ]
        self.creator.run_once()
        self.assertEqual(manager.mock_calls, expected)

    def test_create_tasks(self):
        self.create_tasks_patch.stop()
        tasks = (mock.MagicMock(name='task-1'), mock.MagicMock(name='task-2'))
        expected = [
            mock.call(self.creator.queue.send_message, message_body=task)
            for task in tasks
        ]
        with mock.patch.object(self.creator.executor, 'submit') as submit:
            self.creator.create_tasks(tasks)
            self.assertEqual(submit.call_args_list, expected)


class SchedulerCreatorConfigTest(SchedulerCreatorTest):

    def test_config_task_sleep_interval_class_default(self):
        expected = 767987567
        self.creator.TASK_SLEEP_INTERVAL = expected
        self.creator.config.scheduler.task_sleep_interval = 0
        self.assertEqual(self.creator.task_sleep_interval, expected)

    def test_config_task_sleep_interval_config_default(self):
        expected = 23123213
        self.creator.config.scheduler.task_sleep_interval = expected
        self.assertEqual(self.creator.task_sleep_interval, expected)
