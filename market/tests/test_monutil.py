# coding: utf-8

import unittest

from market.pylibrary.putil.monutil import safe_mon_run


class TestRetry(unittest.TestCase):

    def test_exception(self):
        @safe_mon_run
        def test_func(arg):
            raise RuntimeError('raise RuntimeError')

        result = test_func(42)
        self.assertTrue(result.startswith('2;'))

    def test_bad_output(self):
        @safe_mon_run
        def test_func(arg):
            return 'RuntimeError'

        result = test_func(42)
        self.assertTrue(result.startswith('2;'))

    def test_good_result(self):
        @safe_mon_run
        def test_func():
            return '0;OK'

        result = test_func()
        self.assertTrue(result.startswith('0;'))


if '__main__' == __name__:
    unittest.main()
