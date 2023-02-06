# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.executor import ExecutorWithMessageId
from search.martylib.test_utils import TestCase


class TestExecutorWithMessageId(TestCase):
    actual_request_id = None
    executor = ExecutorWithMessageId(max_workers=1)

    def test_request_id(self):
        expected_request_id = 'test-id'

        # noinspection PyUnusedLocal
        def fn(message):
            TestExecutorWithMessageId.actual_request_id = self.storage.thread_local.request_id

        future = self.executor.submit(fn, {'MessageId': expected_request_id})
        future.result()

        self.assertEqual(self.actual_request_id, expected_request_id)

    def test_invalid_processor(self):
        def fn():
            pass

        future = self.executor.submit(fn, {})

        with self.assertRaisesRegexp(TypeError, r'(takes 0 positional arguments but 1 was given|takes no arguments)'):
            future.result()
