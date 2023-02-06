# coding: utf8

import pytest
from django.test.client import RequestFactory

from travel.rasp.export.export.middleware import LanguageAndNationalVersion


pytestmark = [pytest.mark.dbuser('module'), pytest.mark.mongouser('module')]


class TestApiLanguage(object):
    middleware = LanguageAndNationalVersion()
    factory = RequestFactory()

    def test_valid_v3(self):
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

        # unknown country
        request = self.factory.get('/v3/some', {'lang': 'ru_US'})
        response = self.middleware.process_request(request)
        assert response.data['error']['status_code'] == 400

        # unknown lang
        request = self.factory.get('/v3/some', {'lang': 'kz_RU'})
        response = self.middleware.process_request(request)
        assert response.data['error']['status_code']

        # wrong lang format
        request = self.factory.get('/v3/some', {'lang': 'ru'})
        response = self.middleware.process_request(request)
        assert response.data['error']['status_code'] == 400

    def test_valid_v1_v2(self):
        # no lang specified
        request = self.factory.get('/v1/some')
        self.middleware.process_request(request)
        assert request.tld == 'ru'
        assert request.national_version == 'ru'
        assert request.language_code == 'ru'

        # known tld and localization
        request = self.factory.get('/v1/some', {'lang': 'ru', 'national_version': 'TR'})
        self.middleware.process_request(request)
        assert request.tld == 'com.tr'
        assert request.national_version == 'tr'
        assert request.language_code == 'ru'

        # unknown tld and localization
        request = self.factory.get('/v1/some', {'lang': 'uk', 'national_version': 'US'})
        self.middleware.process_request(request)
        assert request.tld == 'ua'
        assert request.national_version == 'ua'
        assert request.language_code == 'uk'

        # unknown lang
        request = self.factory.get('/v1/some', {'lang': 'kz'})
        self.middleware.process_request(request)
        assert request.tld == 'ru'
        assert request.national_version == 'ru'
        assert request.language_code == 'ru'
