# coding: utf-8

"""
FIXME
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import contextdecorator
import contextlib
import grpc
import logging
import os
import random
import re
import six
import time
import unittest
# noinspection PyProtectedMember
from grpc._server import _Context, _RPCState

from search.martylib.webauth import WebAuthClientMock
from search.martylib.core.date_utils import set_timezone
from search.martylib.core.exceptions import UnhandledMiddlewareException
from search.martylib.core.logging_utils import configure_basic_logging
from search.martylib.core.storage import Storage
from search.martylib.mode import Mode, get_mode, set_mode

from search.martylib.proto.structures import auth_pb2


@contextlib.contextmanager
def fuzz(min_sleep=0.5, max_sleep=2.0, sleep_before=True, sleep_after=False):
    """
    Sleeps for a random time in Mode.TEST.

    :param min_sleep: min seconds to sleep
    :param max_sleep: max seconds to sleep
    :param sleep_after: whether to sleep before yielding
    :param sleep_before: whether to sleep after yielding
    """
    if sleep_before and get_mode() == Mode.TEST:
        time.sleep(random.randint(min_sleep * 1000, max_sleep * 1000) / 1000)

    yield

    if sleep_after and get_mode() == Mode.TEST:
        time.sleep(random.randint(min_sleep * 1000, max_sleep * 1000) / 1000)


def setup(additional_loggers=None, logger_class=None):
    set_timezone('Europe/Moscow')
    configure_basic_logging('test', skip_multiple_call=True, loggers=additional_loggers or set(), logger_class=logger_class)


class MockAuth(contextdecorator.ContextDecorator):
    # FIXME: Refactor class -> function when python 3 is in Arcadia.

    def __init__(self, login='', roles=None, auth_info=None):
        self._login = login
        self._roles = roles
        self._auth_info = auth_info
        self._previous_info = None

    def __enter__(self):
        # In case `MockAuth` is called from thread.
        TestCase.storage.thread_local.auth_client = WebAuthClientMock()

        # noinspection PyProtectedMember
        self._previous_info = TestCase.storage.thread_local._auth_info

        if self._auth_info is None:
            TestCase.storage.thread_local._auth_info = auth_pb2.AuthInfo(login=self._login, roles=self._roles)
        else:
            TestCase.storage.thread_local._auth_info = self._auth_info

    def __exit__(self, exc_type, exc_value, traceback):
        TestCase.storage.thread_local._auth_info = self._previous_info


class TestCase(unittest.TestCase):
    """
    Unittest test case with configured logging.
    """
    ADDITIONAL_LOGGERS = None
    LOGGER_CLASS = None

    maxDiff = None

    logger = logging.getLogger('martylib.test')
    mock_auth = MockAuth
    storage = Storage()

    @classmethod
    def setUpClass(cls):
        set_mode(Mode.TEST)
        setup(additional_loggers=cls.ADDITIONAL_LOGGERS, logger_class=cls.LOGGER_CLASS)
        Storage().thread_local.auth_client = WebAuthClientMock()

    @contextlib.contextmanager
    def mock_request(self, expected_grpc_status_code=None, expected_grpc_details_regexp=None, *args, **kwargs):
        ctx = GrpcContextMock(*args, **kwargs)
        try:
            TestCase.storage.set_request_id_from_grpc_metadata(ctx)
            yield ctx
        finally:
            TestCase.storage.clear_request_id()

            if expected_grpc_status_code is not None:
                self.assertGrpcStatusEquals(ctx, expected_grpc_status_code)
            if expected_grpc_details_regexp is not None:
                if six.PY3 and isinstance(ctx._state.details, bytes):
                    self.assertRegexpMatches(ctx._state.details.decode('utf-8'), re.compile(expected_grpc_details_regexp))
                else:
                    self.assertRegexpMatches(ctx._state.details, re.compile(expected_grpc_details_regexp))

    @staticmethod
    def require_postgres(target):
        return unittest.skipIf(os.environ.get('DB_TESTS') != '1', "DB_TESTS doesn't equal to 1")(target)

    @contextlib.contextmanager
    def assertRaisesWithMessage(self, exc_class, callable_obj=None, message='', *args, **kwargs):
        try:
            if six.PY2:
                with self.assertRaises(excClass=exc_class, callableObj=callable_obj, *args, **kwargs):
                    yield
            else:
                if callable_obj:
                    args = (callable_obj,) + args
                with self.assertRaises(expected_exception=exc_class, *args, **kwargs):
                    yield
        except AssertionError as e:
            raise AssertionError('{}: {}'.format(message, e))

    @contextlib.contextmanager
    def assertRaisesUnhandledMiddlewareException(self, exc_class, callable_obj=None, message='', expected_regexp=None):
        if six.PY2:
            with self.assertRaises(excClass=UnhandledMiddlewareException, callableObj=callable_obj) as context:
                yield
        else:
            args = (callable_obj,) if callable_obj else ()
            with self.assertRaises(expected_exception=UnhandledMiddlewareException, *args) as context:
                yield

        try:
            unhandled_exception = context.exception.unhandled_exception  # type: Exception
            self.assertIsInstance(unhandled_exception, exc_class)

            if expected_regexp is not None:
                self.assertRegexpMatches(unhandled_exception.message, re.compile(expected_regexp))

        except AssertionError as e:
            raise AssertionError('{}: {}'.format(message, e))

    def assertGrpcStatusEquals(self, grpc_context, grpc_status_code):
        try:
            for _ in grpc_status_code:
                pass
            test_func = self.assertIn
        except TypeError:
            test_func = self.assertEqual

        expected = grpc_status_code
        actual = grpc_context._state.code or grpc.StatusCode.OK

        test_func(
            actual,
            expected,
            'expected code: {}, actual code: {}, details: {}'.format(expected, actual, grpc_context._state.details),
        )


class ServerTestCase(TestCase):
    @classmethod
    def setUpClass(cls):
        super(ServerTestCase, cls).setUpClass()
        Storage().set_request_id('TEST_REQUEST_ID')


class PytestCase(object):
    @classmethod
    def setup_class(cls):
        setup()


class GrpcContextMock(_Context):
    # noinspection PyMissingConstructor
    def __init__(self, metadata=None, time_remaining=60.0):
        self._metadata = metadata if metadata else ()
        if isinstance(self._metadata, dict):
            self._metadata = tuple(self._metadata.items())

        self._time_remaining = time_remaining
        self._state = _RPCState()

    def invocation_metadata(self):
        return self._metadata

    def time_remaining(self):
        return self._time_remaining

    def __str__(self):
        return '<{} status={} details="">'.format(self.__class__.__name__, self._state.code, self._state.details)

    __repr__ = __str__
