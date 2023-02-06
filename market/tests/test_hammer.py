# -*- coding: utf-8 -*-
from market.pylibrary.hammer import AsyncProcess, AsyncThread

import os
import time
import unittest


class MaliciousFuncException(Exception):
    pass


def separate_func(input_seconds):
    time.sleep(input_seconds)
    return os.getpid()


def malicious_func():
    raise MaliciousFuncException('Темная сторона силы')


class TestHammer(unittest.TestCase):
    def test_me(self):
        def generate(factory, length):
            return [factory(separate_func, [0.01]) for _ in range(1, length + 1)]

        self.assertEqual(10, len(set([async_process.wait_result()
                                      for async_process in generate(AsyncProcess, 10)])))
        self.assertEqual(1, len(set([async_thread.wait_result()
                                      for async_thread in generate(AsyncThread, 10)])))

    def test_exception(self):
        self.assertRaises(MaliciousFuncException, AsyncThread(malicious_func, []).wait_result)
        self.assertRaises(MaliciousFuncException, AsyncProcess(malicious_func, []).wait_result)

    def test_safe_exception(self):
        async_thread = AsyncThread(malicious_func, [])
        self.assertTrue(isinstance(async_thread.safe_wait_result(), MaliciousFuncException))
        self.assertFalse(async_thread.safe_was_ok())


if '__main__' == __name__:
    unittest.main()
