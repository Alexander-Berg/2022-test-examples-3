# -*- coding: utf-8 -*-

import urlparse

from django.http import HttpResponseRedirect
from django.test.client import RequestFactory
from mock import patch

from travel.avia.library.python.common.geotargeting.middleware import RedirectToNationalVersion as RedirectToNationalVersionMiddleware

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.utils.replace_setting import replace_setting


class TestRedirectToNationalVersionMiddleware(TestCase):
    def setUp(self):
        self.factory = RequestFactory()

    def test_ignore(self):
        middleware = RedirectToNationalVersionMiddleware()

        with patch.object(RedirectToNationalVersionMiddleware, '_redirect_is_disabled', return_value=False):
            request = self.factory.get('/some/')
            request.META['HTTP_X_REQUESTED_WITH'] = None
            request.root_domain = 'com'

            result = middleware.process_request(request)
            assert result is None

        with patch.object(RedirectToNationalVersionMiddleware, '_redirect_is_disabled', return_value=True):
            request = self.factory.get('/some/')
            request.META['HTTP_X_REQUESTED_WITH'] = None
            request.root_domain = 'ru'

            result = middleware.process_request(request)
            assert result is None

        with patch.object(RedirectToNationalVersionMiddleware, '_redirect_is_disabled', return_value=False):
            request = self.factory.get('/some/')
            request.META['HTTP_X_REQUESTED_WITH'] = 'XMLHttpRequest'
            request.root_domain = 'ru'

            result = middleware.process_request(request)
            assert result is None

    def test_redirect_to_known_host(self):
        middleware = RedirectToNationalVersionMiddleware()
        with replace_setting('SUPPORTED_HOSTS', ['piu.ru', 'piu.com']):
            with patch.object(RedirectToNationalVersionMiddleware, '_redirect_is_disabled', return_value=False):
                request = self.factory.get('/some/')
                request.META['HTTP_X_REQUESTED_WITH'] = None
                request.root_domain = 'ru'
                request.client_city_domain = 'com'
                request.port = ''

                result = middleware.process_request(request)
                assert isinstance(result, HttpResponseRedirect)

                redirect_url = urlparse.urlparse(result['location'])
                assert redirect_url.hostname == 'piu.com'

    def test_redirect_to_unknown_host(self):
        middleware = RedirectToNationalVersionMiddleware()
        with replace_setting('SUPPORTED_HOSTS', ['piu.ru', 'piu.ua']):
            with patch.object(RedirectToNationalVersionMiddleware, '_redirect_is_disabled', return_value=False):
                request = self.factory.get('/some/')
                request.META['HTTP_X_REQUESTED_WITH'] = None
                request.root_domain = 'ru'
                request.client_city_domain = 'com'

                result = middleware.process_request(request)
                assert result is None
