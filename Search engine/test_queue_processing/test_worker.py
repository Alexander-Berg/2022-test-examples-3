# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import concurrent.futures
import mock

from search.martylib.queue_processing.worker import BaseWorker
from search.martylib.queue_processing.wrapper import QueueWrapperMock

from search.martylib.test_utils import TestCase


class BaseWorkerTestCase(TestCase):
    class DummyResult(object):
        def __init__(self):
            self.meta = mock.Mock()

    class DummyException(Exception):
        """ Exception for test purposes """

    @staticmethod
    def mock_poll_all(chunk_size):
        return ['dummy'] * chunk_size

    @staticmethod
    def mock_poll_less_or_one(chunk_size):
        return ['dummy'] * max(1, chunk_size - 1)

    @staticmethod
    def mock_poll_none(chunk_size):
        return []

    def test_get_message_body_dict_key(self):
        worker = BaseWorker()
        msg = mock.MagicMock()
        worker.get_message_body(msg)
        msg.__getitem__.assert_called_with('Body')

    def test_get_message_body_mock_message(self):
        worker = BaseWorker()
        body = expected = 'test message body'
        msg = QueueWrapperMock._mock_sqs_message(body)
        result = worker.get_message_body(msg)
        self.assertEqual(result, expected)

    def test_data_polling_full_data(self):
        threads = 27
        worker = BaseWorker(thread_count=threads, input_queue=mock.Mock())
        worker.input_queue.poll.side_effect = self.mock_poll_all
        with mock.patch.object(worker.executor, 'submit'):
            submitted = worker.run_once()
        self.assertEqual(submitted, threads)

    def test_data_polling_less_data(self):
        threads = 27
        worker = BaseWorker(thread_count=threads, input_queue=mock.Mock())
        expected = len(self.mock_poll_less_or_one(min(worker.POLL_CHUNK_SIZE, threads)))
        worker.input_queue.poll.side_effect = self.mock_poll_less_or_one
        with mock.patch.object(worker.executor, 'submit'):
            submitted = worker.run_once()
        self.assertEqual(submitted, expected)

    def test_data_polling_less_data_custom_chunk_size(self):
        threads = 27
        worker = BaseWorker(thread_count=threads, input_queue=mock.Mock())
        worker.POLL_CHUNK_SIZE = 20
        expected = len(self.mock_poll_less_or_one(min(worker.POLL_CHUNK_SIZE, threads)))
        worker.input_queue.poll.side_effect = self.mock_poll_less_or_one
        with mock.patch.object(worker.executor, 'submit'):
            submitted = worker.run_once()
        self.assertEqual(submitted, expected)

    def test_data_polling_less_data_custom_huge_chunk_size(self):
        threads = 27
        worker = BaseWorker(thread_count=threads, input_queue=mock.Mock())
        worker.POLL_CHUNK_SIZE = 200
        expected = len(self.mock_poll_less_or_one(min(worker.POLL_CHUNK_SIZE, threads)))
        worker.input_queue.poll.side_effect = self.mock_poll_less_or_one
        with mock.patch.object(worker.executor, 'submit'):
            submitted = worker.run_once()
        self.assertEqual(submitted, expected)

    def test_data_polling_no_data(self):
        worker = BaseWorker(input_queue=mock.Mock())
        worker.input_queue.poll.side_effect = self.mock_poll_none
        with mock.patch.object(worker.executor, 'submit'):
            submitted = worker.run_once()
        self.assertEqual(submitted, 0)

    def test_result_message_hook(self):
        """ Just make sure it's called in a proper place """
        worker = BaseWorker()
        worker.RESULT_TYPE = self.DummyResult
        content = mock.Mock(name='input')
        inp = QueueWrapperMock._mock_sqs_message(content)
        result = self.DummyResult()
        excpected_output = 'result message'
        with \
                mock.patch.object(worker, 'publish_result') as publish, \
                mock.patch.object(worker, 'result_message') as message, \
                mock.patch.object(worker, 'process_message') as process:
            process.return_value = result
            message.return_value = excpected_output
            manager = mock.Mock()
            manager.attach_mock(message, 'message')
            manager.attach_mock(publish, 'publish')
            worker._process_request(inp)
            expected_calls = [
                mock.call.message(result, inp),
                mock.call.publish(excpected_output),
            ]
            manager.assert_has_calls(expected_calls)

    def test_exception_propagation(self):
        expected_exception = self.DummyException()

        class DummyWorker(BaseWorker):
            propagate_exceptions = (self.DummyException, )

            def before_process(self, message):  # any method called within try/except in `_process_request`
                raise expected_exception

        worker = DummyWorker()
        worker._input_queue = mock.Mock()
        mock_content = mock.Mock(**{'meta.build_via': 'dummy', 'meta.build_at': 'dummy'})
        worker.input_queue.poll.side_effect = ([QueueWrapperMock._mock_sqs_message(mock_content)], [])
        with mock.patch.object(worker, 'handle_exceptions') as handle_exceptions:
            worker._run()
            concurrent.futures.wait(worker.futures)
            worker._run()
            handle_exceptions.assert_called_with([expected_exception])

    def test_exception_not_propagated(self):
        unexpected_exception = self.DummyException()

        class DummyWorker(BaseWorker):
            propagate_exceptions = (NameError, )

            def before_process(self, message):  # any method called within try/except in `_process_request`
                raise unexpected_exception

        worker = DummyWorker()
        worker._input_queue = mock.Mock()
        mock_content = mock.Mock(**{'meta.build_via': 'dummy', 'meta.build_at': 'dummy'})
        worker.input_queue.poll.side_effect = ([QueueWrapperMock._mock_sqs_message(mock_content)], [])
        with mock.patch.object(worker, 'handle_exceptions') as handle_exceptions:
            worker._run()
            concurrent.futures.wait(worker.futures)
            handle_exceptions.reset_mock()
            worker._run()
            handle_exceptions.assert_called_once_with([])
