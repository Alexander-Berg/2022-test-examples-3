# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import time

from search.martylib.core.decorators import *
from search.martylib.core.exceptions import MaxRetriesReached
from search.martylib.test_utils import TestCase


class TestDecorators(TestCase):
    def test_classproperty(self):
        class C(object):
            attr = 42

            @classproperty
            def prop(self):
                return self.attr

        self.assertEqual(C.prop, 42)

    def test_retry_params_check(self):
        with self.assertRaises(ValueError):
            @retry(backoff='string')
            def f():
                pass

    def test_retry_with_progressive_timeouts(self):
        test_start = time.time()

        @retry(backoff=(0.1, 0.5), max_retries=10)
        def f():
            raise RuntimeError

        with self.assertRaises(MaxRetriesReached):
            f()

        self.assertTrue(time.time() - test_start > 4)

    def test_retry_basic(self):
        @retry(max_retries=3)
        def f():
            return 1 / 0

        with self.assertRaises(MaxRetriesReached) as exc:
            f()

        self.assertEqual(exc.exception.args[0], '3/3 f')
        self.assertEqual(len(exc.exception.args[1]), 3)

    def test_retry_convenient(self):
        @retry
        def f():
            return 1 / 0

        with self.assertRaises(MaxRetriesReached) as exc:
            f()

        self.assertEqual(exc.exception.args[0], '3/3 f')

    def test_retry_hooks(self):
        def f(e):
            raise e

        # noinspection PyUnusedLocal
        def custom_hook(exception, current_try, max_retries, function_name):
            raise TypeError('custom hook')

        hooks = {
            ZeroDivisionError: stop_trying,
            RuntimeError: ignore,
            ValueError: custom_hook,
        }

        with self.assertRaises(MaxRetriesReached) as exc:
            retry(hooks=hooks)(f)(RuntimeError)
        self.assertEqual(exc.exception.args[0], '3/3 f')

        expected_message = 'custom hook'
        with self.assertRaises(TypeError) as exc:
            retry(hooks=hooks)(f)(ValueError)
        self.assertEqual(str(exc.exception), expected_message)

        with self.assertRaises(ZeroDivisionError):
            retry(hooks=hooks)(f)(ZeroDivisionError)

    def test_convenient_decorator(self):
        @convenient_decorator
        def decorator(f, k=1):
            def wrapped(*args, **kwargs):
                return k * f(*args, **kwargs)
            return wrapped

        @decorator
        def f1(x):
            return x

        @decorator()
        def f2(x):
            return x

        @decorator(k=2)
        def f3(x):
            return x

        self.assertEqual(f1(10), 10)
        self.assertEqual(f2(10), 10)
        self.assertEqual(f3(10), 20)
