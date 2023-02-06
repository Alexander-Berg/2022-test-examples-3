# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import concurrent.futures

import six

from typing import AnyStr, ClassVar

from search.martylib.core.date_utils import mock_now

from search.priemka.yappy.proto.structures.scheduler_pb2 import ProcessorTask, ProcessorModules

from search.priemka.yappy.src.processor.processor import Processor
from search.priemka.yappy.src.yappy_lib.clients import get_clients
from search.priemka.yappy.src.yappy_lib.config_utils import get_test_config
from search.priemka.yappy.src.yappy_lib.exceptions import IntervalTooShort
from search.priemka.yappy.src.yappy_lib.queue import PartitionedMessage, SinkOverflow

from search.priemka.yappy.tests.utils.test_cases import TestCase

if six.PY2:
    import mock
else:
    import unittest.mock as mock


class BaseProcessorTestCase(TestCase):

    @classmethod
    def processor_class(cls):
        # type: () -> ClassVar
        class TestInput(Processor.INPUT_QUEUE_CLASS):
            pass

        class TestOutput(Processor.OUTPUT_QUEUE_CLASS):
            pass

        class TestProcessor(Processor):
            INPUT_QUEUE_CLASS = TestInput
            OUTPUT_QUEUE_CLASS = TestOutput

        return TestProcessor


class ProcessorTestCase(BaseProcessorTestCase):

    processor = None  # type: Processor

    @property
    def messages(self):
        # type: () -> list[PartitionedMessage]
        return [
            PartitionedMessage(
                ProcessorTask(
                    meta=ProcessorTask.Meta(
                        id='task-{}'.format(i),
                    ),
                    modules=ProcessorModules(
                        modules=[
                            ProcessorModules.ProcessorModule.BETA_TRANSLATOR,
                            ProcessorModules.ProcessorModule.BETA_VERIFICATOR,
                        ],
                    )
                )
            )
            for i in range(2)
        ]

    @classmethod
    def setUpClass(cls):
        config = get_test_config()
        config.processor.thread_count = 1
        clients = get_clients(config)
        with \
                mock.patch('search.priemka.yappy.src.yappy_lib.queue.partitions_manager.DBPartitionsManager.start'), \
                mock.patch.object(clients.__class__, 'sqs', new_callable=mock.PropertyMock):
            cls.processor = cls.processor_class().from_config(config)
        cls.original_modules_map = cls.processor.modules_map
        cls.input_queue_patch = mock.patch.object(cls.processor.input_queue, 'poll')
        cls.output_queue_patch = mock.patch.object(
            cls.processor.__class__, 'output_queue', new_callable=mock.PropertyMock,
        )
        cls.verify_message_patch = mock.patch.object(
            cls.processor.output_queue,
            'verify_message_revision',
            return_value=True,
        )
        cls.apply_filters_patch = mock.patch.object(
            cls.processor.input_queue.partitions_manager,
            'apply_partition_filters',
            side_effect=lambda x: x,
        )
        cls.increase_sleep_patch = mock.patch.object(cls.processor, 'increase_sleep_interval')
        cls.decrease_sleep_patch = mock.patch.object(cls.processor, 'decrease_sleep_interval')
        cls.set_sleep_interval_patch = mock.patch.object(cls.processor, 'set_sleep_interval')
        cls.adjust_sleep_interval_patch = mock.patch.object(cls.processor, 'adjust_sleep_interval')

    def setUp(self):
        self.input_queue_patch.start().return_value = self.messages
        self.output_queue = self.output_queue_patch.start()
        self.processor.modules_map = {}
        self.processor.first_run = 0
        self.processor._last_interval_adjustment = 0
        self.processor._last_sink_overflow = 0
        for message in self.messages:
            for m in message.content.modules.modules:
                self.processor.modules_map[m] = mock.Mock()
        self.verify_message_patch.start()
        self.apply_filters_patch.start()
        self.increase_sleep_patch.start()
        self.decrease_sleep_patch.start()
        self.set_sleep_interval_patch.start()
        self.adjust_sleep_interval_patch.start()

        self.addCleanup(self.input_queue_patch.stop)
        self.addCleanup(self.output_queue_patch.stop)
        self.addCleanup(self.verify_message_patch.stop)
        self.addCleanup(self.apply_filters_patch.stop)
        self.addCleanup(self.increase_sleep_patch.stop)
        self.addCleanup(self.decrease_sleep_patch.stop)
        self.addCleanup(self.set_sleep_interval_patch.stop)
        self.addCleanup(self.adjust_sleep_interval_patch.stop)

        self.output_queue.return_value.message_class = PartitionedMessage
        self.output_queue.return_value.idle_metric_value = None

    def mock_processor_name(self, module):
        # type: (ProcessorModules.ProcessorModule) -> AnyStr
        module_name = ProcessorModules.ProcessorModule[module].lower()
        return '{}_processor'.format(module_name)

    def test_run_once(self):
        """ Test that 'run_once' overall logic is fine: processor modules executed and results published """
        manager = mock.Mock(name='manager')
        manager.attach_mock(self.processor.output_queue.send_message, 'send_message')
        for module, mock_processor in self.processor.modules_map.items():
            manager.attach_mock(mock_processor.run, self.mock_processor_name(module))

        expected_calls = []
        for message in self.processor.input_queue.poll.return_value:
            task = message.content
            expected_calls += [
                getattr(mock.call, self.mock_processor_name(module))(task)
                for module in message.content.modules.modules
            ]
            expected_calls.append(mock.call.send_message(PartitionedMessage(task)))

        self.processor.run_once()
        concurrent.futures.wait(self.processor.futures)

        manager.assert_has_calls(expected_calls)

    def test_increase_sleep_interval_after_overflow(self):
        self.processor.input_queue.poll.return_value = ['dummy'] * 2
        with mock.patch.object(self.processor, '_process_request', side_effect=SinkOverflow):
            self.processor._run()
            concurrent.futures.wait(self.processor.futures)
            self.processor._run()
            concurrent.futures.wait(self.processor.futures)
            self.processor.increase_sleep_interval.assert_called_once()

    def test_skip_run_after_overflow(self):
        self.processor.input_queue.poll.return_value = ['dummy'] * 2
        with mock.patch.object(self.processor, '_process_request', side_effect=SinkOverflow) as process:
            self.processor._run()
            concurrent.futures.wait(self.processor.futures)
            process.side_effect = None
            process.reset_mock()
            self.processor._run()
            concurrent.futures.wait(self.processor.futures)
            process.assert_not_called()

    def test_skip_only_one_run_after_overflow(self):
        self.processor.input_queue.poll.return_value = ['dummy'] * 2
        with mock.patch.object(self.processor, '_process_request', side_effect=SinkOverflow) as process:
            self.processor._run()
            process.reset_mock()
            concurrent.futures.wait(self.processor.futures)
            process.side_effect = None
            self.processor._run()
            concurrent.futures.wait(self.processor.futures)
            self.processor._run()
            concurrent.futures.wait(self.processor.futures)
            process.assert_called()

    def test_decrease_sleep_interval_when_idle(self):
        self.processor.input_queue.poll.return_value = ['dummy'] * 2
        idle_patch = mock.patch.object(self.processor.__class__, 'is_sink_idle', new_callable=mock.PropertyMock, return_value=True)
        timeout_patch = mock.patch.object(
            self.processor.__class__,
            'speedup_timeout',
            new_callable=mock.PropertyMock,
            return_value=False,
        )
        with idle_patch, timeout_patch:
            self.processor._run()
            concurrent.futures.wait(self.processor.futures)
            self.processor.decrease_sleep_interval.assert_called_once()

    def test_dont_decrease_sleep_interval_when_idle_but_source_is_empty(self):
        self.processor.input_queue.poll.return_value = []
        with mock.patch.object(self.processor.__class__, 'is_sink_idle', new_callable=mock.PropertyMock, return_value=True):
            self.processor._run()
            concurrent.futures.wait(self.processor.futures)
            self.processor.decrease_sleep_interval.assert_not_called()

    def test_dont_decrease_sleep_interval_when_idle_but_source_is_shallow(self):
        self.processor.input_queue.poll.side_effect = [['dummy'] * 2, []]
        self.processor._thread_count = len(self.processor.input_queue.poll.return_value) * 3
        with \
                mock.patch.object(self.processor.__class__, 'is_sink_idle', new_callable=mock.PropertyMock, return_value=True), \
                mock.patch.object(self.processor, 'is_source_shallow', new_callable=mock.PropertyMock, return_value=True):
            self.processor._run()
            concurrent.futures.wait(self.processor.futures)
            self.processor.decrease_sleep_interval.assert_not_called()

    def test_dont_decrease_if_was_shallow_recently(self):
        self.decrease_sleep_patch.stop()
        self.processor.input_queue.poll.side_effect = [['dummy'] * 2, []]
        _NOW = 10000
        with \
                mock_now(_NOW), \
                mock.patch.object(self.processor.__class__, 'last_shallow', new_callable=mock.PropertyMock) as last_shallow:
            last_shallow.return_value = _NOW - 0.9 * self.processor.interval_adjustment_timeout
            self.processor.decrease_sleep_interval()
            self.processor.adjust_sleep_interval.assert_not_called()

    def test_dont_decrease_sleep_interval_when_not_idle(self):
        self.processor.input_queue.poll.return_value = ['dummy'] * 2
        with mock.patch.object(self.processor.__class__, 'is_sink_idle', new_callable=mock.PropertyMock, return_value=False):
            self.processor._run()
            concurrent.futures.wait(self.processor.futures)
            self.processor.decrease_sleep_interval.assert_not_called()

    def test_dont_adjust_sleep_interval_below_minimal(self):
        self.adjust_sleep_interval_patch.stop()
        with mock.patch.object(self.processor.__class__, 'minimal_sleep_interval', new_callable=mock.PropertyMock) as min_value:
            min_value.return_value = self.processor.sleep_interval
            self.assertRaises(
                IntervalTooShort,
                self.processor.adjust_sleep_interval,
                0.999999,
            )

    def test_dont_decrease_if_overflow_was_recently(self):
        NOW_ = 10000
        self.decrease_sleep_patch.stop()
        self.processor.input_queue.poll.return_value = ['dummy'] * 2
        patches = [
            mock.patch.object(
                self.processor.__class__,
                prop,
                new_callable=mock.PropertyMock,
                return_value=False,
            )
            for prop in ('first_run_timeout', 'source_shallow_timeout', 'sink_idle_timeout')
        ]
        process_patch = mock.patch.object(self.processor, '_process_request', side_effect=SinkOverflow)
        with patches[0], patches[1], patches[2], process_patch as process:
            self.processor._run()  # get overflow
            concurrent.futures.wait(self.processor.futures)
            process.side_effect = None
            process.reset_mock()
            with mock_now(NOW_):
                self.processor._run()  # increase due to overflow
            concurrent.futures.wait(self.processor.futures)
            self.processor.adjust_sleep_interval.reset_mock()
            with mock_now(NOW_ + 1):
                self.processor.decrease_sleep_interval()
                self.processor.adjust_sleep_interval.assert_not_called()
        concurrent.futures.wait(self.processor.futures)

    def test_first_run_timeout_if_not_started(self):
        self.assertTrue(self.processor.first_run_timeout)

    def test_first_run_timeout_just_started_never_adjusted(self):
        NOW_ = 100000
        with mock_now(NOW_):
            self.processor.first_run = NOW_ - 0.0001
            self.assertTrue(self.processor.first_run_timeout)

    def test_first_run_timeout_just_started_already_adjusted(self):
        NOW_ = 100000
        with mock_now(NOW_):
            self.processor.first_run = NOW_ - 0.0001
            self.processor._last_interval_adjustment = NOW_ - 0.00005
            self.assertFalse(self.processor.first_run_timeout)


class ProcessorConfigTestCase(BaseProcessorTestCase):
    config = None  # type: Mock
    processor = None  # type: Processor
    QUEUE_LIMIT = 432

    def setUp(self):
        self.config = mock.Mock()
        self.config.processor.sleep_interval = 777  # need a numeric value for `*` operation
        self.config.processor.flush_idle_criterion = 0.1  # need a numeric value for `*` operation
        self.config.processor.thread_count = 10  # need a numeric value to compare with 0
        TestProcessor = self.processor_class()
        mock.patch.object(TestProcessor, '_sink_queue_limit', return_value=self.QUEUE_LIMIT).start()
        with mock.patch('search.priemka.yappy.src.yappy_lib.queue.partitions_manager.DBPartitionsManager.start'):
            self.processor = TestProcessor.from_config(self.config)

    def test_task_sleep_interval(self):
        self.assertEqual(
            self.processor.input_queue.default_task_sleep_interval,
            self.config.scheduler.task_sleep_interval,
        )

    def test_sink_flush_interval(self):
        self.assertEqual(
            self.processor.output_queue.flush_interval,
            self.processor.sleep_interval * self.processor.flush_idle_criterion,
        )

    def test_sink_queue_limit(self):
        self.assertEqual(
            self.processor.output_queue.queue.maxsize,
            self.processor._sink_queue_limit(self.config),
        )


class ProcessorSinkCacheLimitTestCase(TestCase):
    def test_sink_queue_limit_capacity(self):
        config = mock.Mock()
        config.scheduler.db_sink_cache_capacity = 12131
        self.assertEqual(
            Processor._sink_queue_limit(config),
            config.scheduler.db_sink_cache_capacity,
        )

    def test_sink_queue_limit_factor(self):
        config = mock.Mock()
        config.scheduler.db_sink_cache_capacity = 0
        config.scheduler.db_sink_cache_capacity_factor = 2.1235
        config.processor.thread_count = 7
        self.assertEqual(
            Processor._sink_queue_limit(config),
            int(config.scheduler.db_sink_cache_capacity_factor * config.processor.thread_count),
        )
