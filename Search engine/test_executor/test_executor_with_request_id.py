# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.core.storage import Storage
from search.martylib.executor import ExecutorWithRequestId
from search.martylib.test_utils import TestCase



class TestExecutorWithRequestId(TestCase):
    def test_executor(self):
        executor = ExecutorWithRequestId(max_workers=10)
        Storage().set_request_id('req_id1')

        def check():
            self.assertEqual(Storage().thread_local.request_id, 'req_id1')

        future = executor.submit(check)
        future.result()

        Storage().clear_request_id()

        ###

        Storage().set_request_id('req_id2')

        def check2():
            self.assertEqual(Storage().thread_local.request_id, 'req_id2')

        future = executor.submit(check2)
        future.result()

        Storage().clear_request_id()

        ###

        def check3():
            self.assertEqual(Storage().thread_local.request_id, 'NO REQUEST ID')

        future = executor.submit(check3)
        future.result()
