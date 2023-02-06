# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import requests
import requests_mock
import unittest
from requests.exceptions import Timeout, SSLError
# from testfixtures import logcapture  # FIXME: testfixtures are not present in Arcadia (yet)

from search.martylib.http.session import InstrumentedSession


def match_cookie(name, value):
    """
    Matches request, if it has selected cookie.
    """
    def _matcher(request):
        # noinspection PyProtectedMember
        return request._request._cookies.get(name) == value
    return _matcher


class TestInstrumentedSessionCompatibility(unittest.TestCase):
    """
    Tests for compatibility with `requests.Session`.
    """

    def test_isinstance(self):
        self.assertIsInstance(InstrumentedSession(), requests.Session)

    def test_timeout(self):
        instrumented_session = InstrumentedSession(timeout=3.0)

        stock_session = requests.Session()
        stock_session.timeout = 3.0

        self.assertEqual(instrumented_session.timeout, stock_session.timeout)

    def test_verify(self):
        instrumented_session = InstrumentedSession(verify='/etc/ssl/certs')

        stock_session = requests.Session()
        stock_session.verify = '/etc/ssl/certs'

        self.assertEqual(instrumented_session.verify, stock_session.verify)

    def test_hooks(self):
        # Default hooks.
        instrumented_session = InstrumentedSession()
        stock_session = requests.Session()
        self.assertEqual(instrumented_session.hooks, stock_session.hooks)

        # noinspection PyUnusedLocal
        def custom_hook(response, **kwargs):
            pass

        instrumented_session = InstrumentedSession(hooks=dict(response=custom_hook))

        stock_session = requests.Session()
        stock_session.hooks = dict(response=custom_hook)

        self.assertEqual(instrumented_session.hooks, stock_session.hooks)


class TestInstrumentedSession(unittest.TestCase):
    """
    `InstrumentedSession` functionality tests.
    """

    def test_content_type(self):
        content_type = 'application/json'
        session = InstrumentedSession(content_type=content_type)
        self.assertEqual(session.headers['accept'], content_type)
        self.assertEqual(session.headers['content-type'], content_type)

    def test_auth_required(self):
        with self.assertRaises(ValueError) as exc:
            InstrumentedSession(auth_required=True)
        self.assertEqual(exc.exception.args[0], 'oauth_token or session_id is required')

    def test_oauth_token(self):
        session = InstrumentedSession(oauth_token='XXX')
        self.assertEqual(session._oauth_token, 'XXX')

        with requests_mock.Mocker() as server:
            server.get(
                'https://yandex.ru',
                text='OK', status_code=200,
                request_headers={'authorization': 'OAuth XXX'}
            )

            r = session.get('https://yandex.ru')
            self.assertEqual(r.status_code, 200)
            self.assertEqual(r.text, 'OK')

    def test_session_id(self):
        session = InstrumentedSession(session_id='XXX')
        self.assertEqual(session._session_id, 'XXX')

        with requests_mock.Mocker() as server:
            server.get(
                'https://yandex.ru',
                text='OK', status_code=200,
                additional_matcher=match_cookie('Session_id', 'XXX')
            )

            r = session.get('https://yandex.ru')
            self.assertEqual(r.status_code, 200)
            self.assertEqual(r.text, 'OK')

    # FIXME: uncomment after adding testfixtures to Arcadia contrib
    # def test_logging(self):
    #     with requests_mock.Mocker() as server:
    #         server.get(
    #             'https://yandex.ru',
    #             text='OK', status_code=200,
    #             headers={'X-Worker': 'test-worker'}
    #         )
    #
    #         with logcapture.LogCapture(names=('martylib.http', ), level=20) as captured:
    #             # Without response headers logging.
    #             session = InstrumentedSession()
    #             session.get('https://yandex.ru', headers={'x-req-id': 'foo'})
    #
    #             # With response headers logging.
    #             session = InstrumentedSession(log_response_headers=True)
    #             session.get('https://yandex.ru', headers={'x-req-id': 'foo'})
    #
    #         self.assertEqual(len(captured.records), 2)
    #         self.assertTrue(captured.records[0].getMessage().endswith("[200] <foo> GET https://yandex.ru "))
    #         self.assertTrue(captured.records[1].getMessage().endswith("[200] <foo> GET https://yandex.ru {u'X-Worker': u'test-worker'}"))

    def test_retry_hooks(self):
        # noinspection PyUnusedLocal
        def timeout_hook(exception, **kwargs):
            raise RuntimeError('timeout')

        # noinspection PyUnusedLocal
        def ssl_error_hook(exception, **kwargs):
            raise RuntimeError('ssl')

        session = InstrumentedSession(retry_hooks={Timeout: timeout_hook, SSLError: ssl_error_hook})

        with requests_mock.Mocker() as server:
            server.get('http://yandex.ru', exc=Timeout)
            server.get('https://yandex.ru', exc=SSLError)

            with self.assertRaises(RuntimeError) as exc:
                session.get('http://yandex.ru')
            self.assertEqual(exc.exception.args[0], 'timeout')

            with self.assertRaises(RuntimeError) as exc:
                session.get('https://yandex.ru')
            self.assertEqual(exc.exception.args[0], 'ssl')

    def test_base_url(self):
        """
        Works for all requests when configured via `__init__`.
        """
        session = InstrumentedSession(base_url='https://yandex.ru')
        with requests_mock.Mocker() as server:
            server.get('https://yandex.ru/about', text='')
            session.get('/about')
            self.assertEqual(server.request_history[0].url, 'https://yandex.ru/about')

    def test_base_url_with_local_kwarg(self):
        """
        Works when passed via `kwargs` for a single request.
        """
        with requests_mock.Mocker() as server:
            server.get('https://yandex.ru/about', text='')
            InstrumentedSession().get('/about', base_url='https://yandex.ru')
            self.assertEqual(server.request_history[0].url, 'https://yandex.ru/about')

    def test_base_url_local_kwarg_priority(self):
        """
        Local `base_url` has higher priority.
        """
        with requests_mock.Mocker() as server:
            server.get('https://google.com/about', text='')
            InstrumentedSession(base_url='https://yandex.ru').get('/about', base_url='https://google.com')
            self.assertEqual(server.request_history[0].url, 'https://google.com/about')

    def test_base_url_with_explicit_scheme(self):
        """
        Base URL is ignored for requests with explicit scheme.
        """
        with requests_mock.Mocker() as server:
            server.get('https://yandex.ru/about', text='')
            InstrumentedSession(base_url='https://yandex.ru').get('https://yandex.ru/about')
            self.assertEqual(server.request_history[0].url, 'https://yandex.ru/about')

    def test_adapters(self):
        test_params = (
            (InstrumentedSession(), 10),
            (InstrumentedSession(adapter_pool_maxsize=100), 100),
        )

        for session, expected_pool_maxsize in test_params:
            for schema in ('http://', 'https://'):
                pm = session.adapters[schema].poolmanager
                self.assertEqual(pm.connection_pool_kw.get('maxsize'), expected_pool_maxsize)
