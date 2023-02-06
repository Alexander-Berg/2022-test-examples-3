# coding: utf-8

import time
import unittest

from market.pylibrary.putil.protector import retry


class TestRetry(unittest.TestCase):

    def test_basic_retry(self):

        @retry(3, (RuntimeError, ValueError))
        def test_func(raise_exception_count, exception):
            self.counter += 1
            if self.counter <= raise_exception_count:
                raise exception

        self.counter = 0
        test_func(3, ValueError)

        self.counter = 0
        with self.assertRaises(RuntimeError):
            test_func(4, RuntimeError)

        self.counter = 0
        with self.assertRaises(AttributeError):
            test_func(1, AttributeError)

    def test_fixed_timeout(self):
        timeout = 0.01

        @retry(2, RuntimeError, timeout=timeout)
        def test_func(max_count, exception):
            self.counter += 1
            if self.counter <= max_count:
                raise exception

        self.counter = 0
        start = time.time()
        test_func(2, RuntimeError)
        elapsed_time = time.time() - start
        self.assertGreaterEqual(elapsed_time, 2 * timeout)

    def test_variadic_timeout(self):
        timeout = (0.01 * i for i in range(5))

        @retry(5, RuntimeError, timeout=timeout)
        def test_func(max_count, exception):
            self.counter += 1
            if self.counter <= max_count:
                raise exception

        self.counter = 0
        start = time.time()
        test_func(5, RuntimeError)
        elapsed_time = time.time() - start
        self.assertGreaterEqual(elapsed_time, 0.1)

        timeout = (0.01 * i for i in range(3))

        @retry(5, RuntimeError, timeout=timeout)
        def test_func(max_count, exception):
            self.counter += 1
            if self.counter <= max_count:
                raise exception

        self.counter = 0
        start = time.time()
        test_func(5, RuntimeError)
        elapsed_time = time.time() - start
        self.assertGreaterEqual(elapsed_time, 0.07)

    @retry(retries_count=3)
    def _member_func(self, max_count):
        self.counter += 1
        if self.counter <= max_count:
            raise RuntimeError

    def test_member_func(self):
        self.counter = 0
        self._member_func(3)


if '__main__' == __name__:
    unittest.main()
