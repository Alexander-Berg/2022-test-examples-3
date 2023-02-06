# coding: utf-8

from unittest import TestCase

from market.pylibrary.common.errors import safe_call, ignore_errors, has_error


class TestSafeCall(TestCase):

    def test_common_case(self):
        safe_call(_test_func)(1, key='value', error=RuntimeError)
        _test_func_decorator(1, key='value', error=RuntimeError)

    def test_with_log(self):
        def check_log(storage, error_type):
            self.assertEqual(
                list(storage.args[:-1]),
                [
                    'func %s(*%r, **%r) caught exception %s: %s',
                    '_test_func',
                    (1,),
                    {'key': 'value', 'error': error_type},
                    error_type.__name__,
                ]
            )
            self.assertIsInstance(storage.args[-1], error_type)
            self.assertEqual(storage.kwargs, {})

        storage = _ArgsStorage()

        error_type = RuntimeError
        safe_call(_test_func, log_func=storage)(1, key='value', error=error_type)
        check_log(storage, error_type)

        _test_func_decorator_log(1, key='value', error=error_type)
        check_log(storage, error_type)

    def test_raise_error(self):
        with self.assertRaises(RuntimeError):
            safe_call(_test_func, ValueError)(1, key='value', error=RuntimeError)
            _test_func_decorator(1, key='value', error=ValueError)

    def test_errors_list(self):
        with self.assertRaises(RuntimeError):
            safe_call(_test_func, [ValueError, KeyError])(1, key='value', error=RuntimeError)
            _test_func_decorator_list(1, key='value', error=RuntimeError)
        safe_call(_test_func, [ValueError, KeyError])(1, key='value', error=KeyError)
        _test_func_decorator_list(1, key='value', error=KeyError)

    def test_check_to_ignore(self):
        with self.assertRaises(CustomError):
            safe_run = safe_call(_test_func, CustomError, lambda e: e.errno != CustomError.ERR_CODE)
            safe_run(1, 'value', CustomError, error_args=['msg', CustomError.ERR_CODE])
            _test_func_decorator_ignore(1, key='value', error=CustomError, error_args=['msg', CustomError.FATAL_CODE])

        safe_run = safe_call(_test_func, CustomError, lambda e: e.errno == CustomError.ERR_CODE)
        safe_run(1, 'value', CustomError, error_args=['msg', CustomError.ERR_CODE])
        _test_func_decorator_ignore(1, key='value', error=CustomError, error_args=['msg', CustomError.ERR_CODE])


class TestHasError(TestCase):

    def test_errors(self):
        self.assertFalse(has_error(lambda: 'ok')())
        self.assertTrue(has_error(_test_func)(1, key='value', error=RuntimeError))
        self.assertFalse(
            has_error(_test_func, errors=ValueError)(1, key='value', error=RuntimeError)
        )
        self.assertTrue(
            has_error(_test_func, errors=ValueError)(1, key='value', error=ValueError)
        )

    def test_with_log(self):
        storage = _ArgsStorage()

        error_type = RuntimeError
        self.assertTrue(
            has_error(_test_func, log_func=storage)(1, key='value', error=error_type)
        )

        self.assertEqual(
            list(storage.args[:-1]),
            [
                'func %s(*%r, **%r) caught exception %s: %s',
                '_test_func',
                (1,),
                {'key': 'value', 'error': error_type},
                error_type.__name__,
            ]
        )
        self.assertIsInstance(storage.args[-1], error_type)
        self.assertEqual(storage.kwargs, {})

    def test_errors_list(self):
        self.assertFalse(
            has_error(_test_func, errors=[ValueError, KeyError])
            (1, key='value', error=RuntimeError)
        )
        self.assertTrue(
            has_error(_test_func, errors=[ValueError, KeyError])
            (1, key='value', error=KeyError)
        )

    def test_check_to_ignore(self):
        check_call = has_error(
            _test_func,
            errors=CustomError,
            check_error=lambda e: e.errno != CustomError.ERR_CODE
        )
        self.assertFalse(
            check_call(1, 'value', CustomError, error_args=['msg', CustomError.ERR_CODE])
        )

        check_call = has_error(
            _test_func,
            errors=CustomError,
            check_error=lambda e: e.errno == CustomError.ERR_CODE
        )
        self.assertTrue(
            check_call(1, 'value', CustomError, error_args=['msg', CustomError.ERR_CODE])
        )


class TestIgnoreErrors(TestCase):

    def test_common_case(self):
        with ignore_errors():
            _test_func(1, key='value', error=RuntimeError)

    def test_with_log(self):
        storage = _ArgsStorage()

        error_type = RuntimeError
        with ignore_errors(error_type, log_func=storage):
            _test_func(1, key='value', error=error_type)

        self.assertEqual(
            list(storage.args[:-1]),
            [
                '%s: %s',
                error_type.__name__,
            ]
        )
        self.assertIsInstance(storage.args[-1], error_type)
        self.assertEqual(storage.kwargs, {})

    def test_raise_error(self):
        with self.assertRaises(RuntimeError):
            with ignore_errors(ValueError):
                _test_func(1, key='value', error=RuntimeError)

    def test_errors_list(self):
        with self.assertRaises(RuntimeError):
            with ignore_errors([ValueError, KeyError]):
                _test_func(1, key='value', error=RuntimeError)

        with ignore_errors([ValueError, KeyError]):
            _test_func(1, key='value', error=KeyError)
            raise RuntimeError

    def test_check_to_ignore(self):
        with self.assertRaises(CustomError):
            with ignore_errors(CustomError, lambda e: e.errno != CustomError.ERR_CODE):
                _test_func(1, 'value', CustomError, error_args=['msg', CustomError.ERR_CODE])

        with ignore_errors(CustomError, lambda e: e.errno == CustomError.ERR_CODE):
            _test_func(1, 'value', CustomError, error_args=['msg', CustomError.ERR_CODE])


class CustomError(Exception):

    ERR_CODE = 0
    FATAL_CODE = 1

    def __init__(self, msg, errno=None, *args):
        self.errno = errno
        super(CustomError, self).__init__(msg, *args)


class _ArgsStorage(object):

    def __init__(self):
        self.args = []
        self.kwargs = {}

    def __call__(self, *args, **kwargs):
        self.args = args
        self.kwargs = kwargs


def _test_func(x, key, error, error_args=None):
    error_args = error_args or []
    raise error(*error_args)


@safe_call(errors=RuntimeError)
def _test_func_decorator(x, key, error, error_args=None):
    _test_func(x, key, error, error_args=error_args)


@safe_call(errors=RuntimeError, log_func=_ArgsStorage())
def _test_func_decorator_log(x, key, error, error_args=None):
    _test_func(x, key, error, error_args=error_args)


@safe_call(errors=[ValueError, KeyError])
def _test_func_decorator_list(x, key, error, error_args=None):
    _test_func(x, key, error, error_args=error_args)


@safe_call(errors=CustomError, check_to_ignore=lambda e: e.errno == CustomError.ERR_CODE)
def _test_func_decorator_ignore(x, key, error, error_args=None):
    _test_func(x, key, error, error_args=error_args)
