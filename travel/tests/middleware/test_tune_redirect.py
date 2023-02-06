# -*- coding: utf-8 -*-

import urlparse

from django.http import HttpResponseRedirect
from django.test.client import RequestFactory
from mock import patch

from travel.avia.library.python.common.geotargeting.middleware import RedirectByTune as RedirectByTuneMiddleware

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.utils.replace_setting import replace_setting


class TestRedirectByTuneMiddleware(TestCase):

    def setUp(self):
        self.factory = RequestFactory()

    def test_ignore(self):
        with replace_setting('SUPPORTED_HOSTS', ['piu.com']):
            middleware = RedirectByTuneMiddleware()

            request = self.factory.get('/some/')
            result = middleware.process_request(request)

            assert result is None

    def test_redirect_to_known_supported_host(self):
        with patch.object(RedirectByTuneMiddleware, '_log', return_value=None):
            with replace_setting('SUPPORTED_HOSTS', ['piu.com']):
                middleware = RedirectByTuneMiddleware()

                request = self.factory.get('/some/', {
                    'domredir': '1',
                    'some_param': 'some_value'
                })
                request.client_city_domain = 'com'
                request.port = ''

                result = middleware.process_request(request)
                assert isinstance(result, HttpResponseRedirect)

                redirect_url = urlparse.urlparse(result['location'])
                assert redirect_url.hostname == 'piu.com'

                params = urlparse.parse_qs(redirect_url.query)
                assert len(params) == 1
                assert params['some_param'][0] == 'some_value'

    def test_redirect_to_unknown_supported_host(self):
        with patch.object(RedirectByTuneMiddleware, '_log', return_value=None):
            with replace_setting('SUPPORTED_HOSTS', ['piu.ru', 'piu.ua']):
                middleware = RedirectByTuneMiddleware()

                request = self.factory.get('/some/', {
                    'domredir': '1',
                    'some_param': 'some_value'
                })
                request.client_city_domain = 'com'
                request.port = ''

                result = middleware.process_request(request)
                assert isinstance(result, HttpResponseRedirect)

                redirect_url = urlparse.urlparse(result['location'])
                assert redirect_url.hostname == 'piu.ru'

                params = urlparse.parse_qs(redirect_url.query)
                assert len(params) == 1
                assert params['some_param'][0] == 'some_value'
