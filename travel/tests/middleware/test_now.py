# coding: utf-8

from datetime import datetime

import mock
import pytest
from django.test.client import RequestFactory

from common.middleware.now import Now
from common.tester.testcase import TestCase
from travel.rasp.library.python.common23.tester.utils.request import clean_context  # noqa
from common.utils.date import MSK_TZ
from travel.rasp.library.python.common23.date.environment import get_time_context


@pytest.mark.usefixtures('clean_context')
class TestNowMiddleware(TestCase):
    def setUp(self):
        self.factory = RequestFactory()

    def test_now_middleware(self):
        middleware = Now()
        request = self.factory.get('/some/')
        mock_value = datetime(2001, 1, 1, 10)
        msk_mock_value = MSK_TZ.localize(mock_value)
        naive_msk_mock_value = msk_mock_value.replace(tzinfo=None)
        with mock.patch('travel.rasp.library.python.common23.date.environment.now', return_value=mock_value) as m_now:
            middleware.process_request(request)
            assert request.now == msk_mock_value and request.msk_now == msk_mock_value
            assert request.naive_msk_now == naive_msk_mock_value
            assert get_time_context() is None
            m_now.assert_called_once_with()

        request = self.factory.get('/some/', data={'_now': '2016-10-05 12:10'})
        msk_now = MSK_TZ.localize(datetime(2016, 10, 5, 12, 10))
        naive_msk_now = msk_now.replace(tzinfo=None)
        middleware.process_request(request)
        assert request.now == msk_now and request.msk_now == msk_now
        assert request.naive_msk_now == naive_msk_now
        assert get_time_context() == naive_msk_now
        middleware.process_response(request, mock.sentinel.response)
        assert get_time_context() is None

        request = self.factory.get('/some/', data={'_now': 'invalid'})
        response = middleware.process_request(request)
        assert response.content.decode('utf-8') == u'Параметр _now имеет невалидный формат. ' \
                                                   u'Правильный формат: %Y-%m-%d %H:%M.'

    def test_del_time_context(self):
        middleware = Now()
        with mock.patch('common.middleware.now.delete_time_context') as m_del_now:
            request = self.factory.get('/some/', data={'_now': '2016-01-01'})
            middleware.del_time_from_context(request)
            m_del_now.assert_called_once_with()

            request = self.factory.get('/some/')
            middleware.del_time_from_context(request)
            assert m_del_now.call_count == 1

    def test_process_response(self):
        with mock.patch.object(Now, 'del_time_from_context', autospec=True) as m_del_context:
            middleware = Now()
            response = middleware.process_response(mock.sentinel.request, mock.sentinel.response)
            assert response == mock.sentinel.response
            m_del_context.assert_called_once_with(middleware, mock.sentinel.request)

    def test_process_exception(self):
        with mock.patch.object(Now, 'del_time_from_context', autospec=True) as m_del_context:
            middleware = Now()
            middleware.process_exception(mock.sentinel.request, mock.sentinel.exception)
            m_del_context.assert_called_once_with(middleware, mock.sentinel.request)
