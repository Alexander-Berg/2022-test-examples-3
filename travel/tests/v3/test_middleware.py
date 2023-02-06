# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
from django.http import HttpResponse
from django.test.client import RequestFactory

from common.tester.testcase import TestCase

from travel.rasp.api_public.api_public.middleware import CheckKeyValid, RedirectToTerminalSlash, ApiLanguage
from travel.rasp.api_public.api_public.v3.core.api_errors import ApiError


class TestCheckKeyValid(TestCase):
    factory = RequestFactory()
    middleware = CheckKeyValid()

    valid_keys_by_url = {
        '/partners/bzd/basic_threads_uids': [
            '123',
        ]
    }

    def test_key_valid(self):
        response = HttpResponse({"data": "data"})
        with mock.patch('travel.rasp.api_public.api_public.middleware.apikeys.check_key', autospec=True) as m_check_key, \
                mock.patch('travel.rasp.api_public.api_public.middleware.apikeys.update_counter', autospec=True) as m_update_counter:
            request = self.factory.get('/v3/copyright/')
            self.middleware.process_request(request)
            assert request.valid_key is True
            m_check_key.assert_called_once_with(request)
            self.middleware.process_response(request, response)
            m_update_counter.assert_called_once_with(request, hits=1, bytes=len(response.content))

            m_check_key.side_effect = ApiError('error')
            self.middleware.process_request(request)
            assert request.valid_key is False
            assert m_check_key.call_args_list == [mock.call(request), mock.call(request)]
            self.middleware.process_response(request, response)
            assert m_update_counter.call_count == 1

            request = self.factory.get('/ping/')
            self.middleware.process_request(request)
            assert not hasattr(request, 'valid_key')
            self.middleware.process_response(request, response)
            assert m_update_counter.call_count == 1


class TestRedirectToTerminalSlash(TestCase):
    def test_redirect(self):
        middleware = RedirectToTerminalSlash()
        factory = RequestFactory()
        request = factory.get('/v3/copyright')
        response = middleware.process_request(request)
        assert response['location'] == '/v3/copyright/'
        assert response.status_code == 302

        request = factory.get('/v3/copyright/')
        assert middleware.process_request(request) is None


class TestApiLanguage(TestCase):
    middleware = ApiLanguage()
    factory = RequestFactory()

    def test_valid(self):
        # no lang specified
        request = self.factory.get('/v3/some')
        self.middleware.process_request(request)
        assert request.tld == 'ru'
        assert request.national_version == 'ru'

        # known tld and localization
        request = self.factory.get('/v3/some', {'lang': 'tr_TR'})
        self.middleware.process_request(request)
        assert request.tld == 'com.tr'
        assert request.national_version == 'tr'

        request = self.factory.get('/v3/some', {'lang': 'TR_tr'})
        self.middleware.process_request(request)
        assert request.tld == 'com.tr'
        assert request.national_version == 'tr'

        # known tld but unknown localization
        request = self.factory.get('/v3/some', {'lang': 'ru_KZ'})
        self.middleware.process_request(request)
        assert request.tld == 'kz'
        assert request.national_version == 'ru'

        # unknown country
        request = self.factory.get('/v3/some', {'lang': 'ru_US'})
        response = self.middleware.process_request(request)
        assert response.status_code == 400

        # unknown lang
        request = self.factory.get('/v3/some', {'lang': 'kz_RU'})
        response = self.middleware.process_request(request)
        assert response.status_code == 400

        # wrong lang format
        request = self.factory.get('/v3/some', {'lang': 'ru'})
        response = self.middleware.process_request(request)
        assert response.status_code == 400
