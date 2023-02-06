from __future__ import unicode_literals

import pytest
import requests
import requests_mock
from django.conf import settings
from mock import Mock
from travel.library.python.tvm_ticket_provider import provider_fabric

from travel.avia.ticket_daemon_api.tests.daemon_tester import create_query
from travel.avia.ticket_daemon_api.jsonrpc.lib.internal_daemon_client import (
    InternalDaemonClient, InternalDaemonException
)

from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.library.python.tester.testcase import TestCase

TD_URL = 'http://internal-daemon.mock'


class InternalDaemonClientPingTest(TestCase):
    def setUp(self):
        self._client = InternalDaemonClient(
            tvm_provider=provider_fabric.create(settings),
            ticket_daemon_host=TD_URL
        )

    @requests_mock.mock()
    def test_ping(self, m):
        m.get(TD_URL + '/api/1.0/ping', text='data')
        self._client.ping()

    @requests_mock.mock()
    def test_ping_connection_error(self, m):
        m.get(
            TD_URL + '/api/1.0/ping',
            exc=requests.exceptions.ConnectionError
        )

        with pytest.raises(InternalDaemonException) as e:
            self._client.ping()

        assert e.value.code == 499
        assert 'ConnectionError' in e.value.message

    @requests_mock.mock()
    def test_ping_timeout_error(self, m):
        m.get(
            TD_URL + '/api/1.0/ping',
            exc=requests.exceptions.Timeout
        )

        with pytest.raises(InternalDaemonException) as e:
            self._client.ping()

        assert e.value.code == 499
        assert 'Timeout' in e.value.message

    @requests_mock.mock()
    def test_ping_500(self, m):
        m.get(
            TD_URL + '/api/1.0/ping',
            status_code=500
        )

        with pytest.raises(InternalDaemonException) as e:
            self._client.ping()

        assert e.value.code == 500
        assert '500' in e.value.message


class InternalDaemonClientRedirectTest(TestCase):
    def setUp(self):
        self._client = InternalDaemonClient(
            tvm_provider=provider_fabric.create(settings),
            ticket_daemon_host=TD_URL
        )

        self._fake_url = str(Mock(name='url'))
        self._fake_mobile_url = str(Mock(name='mobile_url'))
        self._fake_post_data = str(Mock(name='post_data'))

        self._fake_order_data = str(Mock(name='order_data'))
        self._fake_user_info = str(Mock(name='user_info'))
        self._fake_utm_source = str(Mock(name='utm_source'))
        self._fake_query_source = str(Mock(name='query_source'))
        self._fake_additional_data = str(Mock(name='additional_data'))

    def _make_redirect(self):
        return self._client.redirect(
            order_data=self._fake_order_data,
            user_info=self._fake_user_info,
            utm_source=self._fake_utm_source,
            query_source=self._fake_query_source,
            additional_data=self._fake_additional_data,
            timeout=20,
        )

    def _check_body(self, adapter):
        actual_body = adapter.last_request.json()
        assert actual_body == {
            'order_data': self._fake_order_data,
            'user_info': self._fake_user_info,
            'utm_source': self._fake_utm_source,
            'query_source': self._fake_query_source,
            'additional_data': self._fake_additional_data,
        }

    @requests_mock.mock()
    def test_redirect(self, m):
        adapter = m.post(TD_URL + '/api/1.0/cook_redirect/', json={
            'url': str(self._fake_url),
            'm_url': str(self._fake_mobile_url),
            'post_data': str(self._fake_post_data),
        })

        redirect_data = self._make_redirect()
        self._check_body(adapter)

        # check redirect_data
        assert redirect_data.query_source == self._fake_query_source
        assert redirect_data.m_url == self._fake_mobile_url
        assert redirect_data.url == self._fake_url
        assert redirect_data.post == self._fake_post_data

    @requests_mock.mock()
    def test_redirect_connection_error(self, m):
        adapter = m.post(
            TD_URL + '/api/1.0/cook_redirect/',
            exc=requests.exceptions.ConnectionError
        )

        with pytest.raises(InternalDaemonException) as e:
            self._make_redirect()

        self._check_body(adapter)
        assert e.value.code == 499
        assert 'ConnectionError' in e.value.message

    @requests_mock.mock()
    def test_redirect_timeout_error(self, m):
        adapter = m.post(
            TD_URL + '/api/1.0/cook_redirect/',
            exc=requests.exceptions.Timeout
        )
        with pytest.raises(InternalDaemonException) as e:
            self._make_redirect()

        self._check_body(adapter)
        assert e.value.code == 499
        assert 'Timeout' in e.value.message

    @requests_mock.mock()
    def test_redirect_500(self, m):
        adapter = m.post(
            TD_URL + '/api/1.0/cook_redirect/',
            status_code=500
        )

        with pytest.raises(InternalDaemonException) as e:
            self._make_redirect()

        self._check_body(adapter)
        assert e.value.code == 500
        assert '500' in e.value.message


class InternalDaemonClientBookRedirectTest(TestCase):
    def setUp(self):
        self._client = InternalDaemonClient(
            tvm_provider=provider_fabric.create(settings),
            ticket_daemon_host=TD_URL
        )
        self._fake_url = str(Mock(name='url'))
        self._fake_mobile_url = str(Mock(name='mobile_url'))
        self._fake_post_data = str(Mock(name='post_data'))

        self._fake_order_data = str(Mock(name='order_data'))
        self._fake_variant = str(Mock(name='variant'))
        self._fake_flights = str(Mock(name='flights'))
        self._fake_user_info = str(Mock(name='user_info'))
        self._fake_utm_source = str(Mock(name='utm_source'))
        self._fake_query_source = str(Mock(name='query_source'))
        self._fake_additional_data = str(Mock(name='additional_data'))
        self._fake_variant_test_context = str(Mock(name='variant_test_context'))

    def _make_book(self):
        return self._client.book(
            order_data=self._fake_order_data,
            variant=self._fake_variant,
            flights=self._fake_flights,
            user_info=self._fake_user_info,
            utm_source=self._fake_utm_source,
            query_source=self._fake_query_source,
            additional_data=self._fake_additional_data,
            timeout=6.0,
            variant_test_context=self._fake_variant_test_context,
        )

    def _check_body(self, adapter):
        actual_body = adapter.last_request.json()
        assert actual_body == {
            'order_data': self._fake_order_data,
            'variant': self._fake_variant,
            'flights': self._fake_flights,
            'user_info': self._fake_user_info,
            'utm_source': self._fake_utm_source,
            'query_source': self._fake_query_source,
            'additional_data': self._fake_additional_data,
            InternalDaemonClient.BOY_VARIANT_TEST_CONTEXT_PARAM: self._fake_variant_test_context,
        }

    @requests_mock.mock()
    def test_book(self, m):
        adapter = m.post(TD_URL + '/api/1.0/book_redirect/', json={
            'url': str(self._fake_url),
            'm_url': str(self._fake_mobile_url),
            'post_data': str(self._fake_post_data),
        })

        redirect_data = self._make_book()
        self._check_body(adapter)

        assert redirect_data.query_source == self._fake_query_source
        assert redirect_data.m_url == self._fake_mobile_url
        assert redirect_data.url == self._fake_url
        assert redirect_data.post == self._fake_post_data

    @requests_mock.mock()
    def test_redirect_connection_error(self, m):
        adapter = m.post(
            TD_URL + '/api/1.0/book_redirect/',
            exc=requests.exceptions.ConnectionError
        )

        with pytest.raises(InternalDaemonException) as e:
            self._make_book()

        self._check_body(adapter)
        assert e.value.code == 499
        assert 'ConnectionError' in e.value.message

    @requests_mock.mock()
    def test_redirect_timeout_error(self, m):
        adapter = m.post(
            TD_URL + '/api/1.0/book_redirect/',
            exc=requests.exceptions.Timeout
        )
        with pytest.raises(InternalDaemonException) as e:
            self._make_book()

        self._check_body(adapter)
        assert e.value.code == 499
        assert 'Timeout' in e.value.message

    @requests_mock.mock()
    def test_redirect_500(self, m):
        adapter = m.post(
            TD_URL + '/api/1.0/book_redirect/',
            status_code=500
        )

        with pytest.raises(InternalDaemonException) as e:
            self._make_book()

        self._check_body(adapter)
        assert e.value.code == 500
        assert '500' in e.value.message


class InternalDaemonClientQueryTest(TestCase):
    def setUp(self):
        reset_all_caches()
        self._client = InternalDaemonClient(
            tvm_provider=provider_fabric.create(settings),
            ticket_daemon_host=TD_URL
        )

        self._fake_query = create_query()
        self._fake_query.partner_codes = str(Mock(name='partners'))
        self._fake_ignore_cache = str(Mock(name='ignore_cache'))
        self._fake_test_id = str(Mock(name='test_id'))

        self._fake_data = str(Mock(name='data'))

    def _make_query(self):
        return self._client.query(
            query=self._fake_query,
            ignore_cache=self._fake_ignore_cache,
            test_id=self._fake_test_id,
            base_qid=self._fake_query.id,
        )

    @requests_mock.mock()
    def test_query(self, m):
        m.get(
            TD_URL + '/api/1.0/query_partners_pure/',
            json={
                'data': str(self._fake_data)
            }
        )

        query_response = self._make_query()
        assert query_response == self._fake_data

    @requests_mock.mock()
    def test_query_with_wizard_caches(self, m):
        m.get(
            TD_URL + '/api/1.0/query_partners_pure/',
            json={
                'data': str(self._fake_data)
            }
        )
        wizard_caches = 'wizard_results,wizard_results_experimental'
        self._fake_query.meta = {'wizard_caches': wizard_caches}

        self._make_query()

        assert m.last_request.qs.get('wizard_caches') == [wizard_caches]

    @requests_mock.mock()
    def test_query_connection_error(self, m):
        m.get(
            TD_URL + '/api/1.0/query_partners_pure/',
            exc=requests.exceptions.ConnectionError
        )

        with pytest.raises(InternalDaemonException) as e:
            self._make_query()

        assert e.value.code == 499
        assert 'ConnectionError' in e.value.message

    @requests_mock.mock()
    def test_query_timeout_error(self, m):
        m.get(
            TD_URL + '/api/1.0/query_partners_pure/',
            exc=requests.exceptions.Timeout
        )
        with pytest.raises(InternalDaemonException) as e:
            self._make_query()

        assert e.value.code == 499
        assert 'Timeout' in e.value.message

    @requests_mock.mock()
    def test_redirect_500(self, m):
        m.get(
            TD_URL + '/api/1.0/query_partners_pure/',
            status_code=500
        )

        with pytest.raises(InternalDaemonException) as e:
            self._make_query()

        assert e.value.code == 500
        assert '500' in e.value.message
