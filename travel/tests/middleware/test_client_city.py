# -*- coding: utf-8 -*-

import datetime

import mock
from django.conf import settings
from django.test.client import RequestFactory

from common.geotargeting.lookup import get_city_for_geo_ids
from common.geotargeting.middleware import ClientCity as ClientCityMiddleware
from common.models.geo import Country, CityMajority
from common.tester.factories import create_settlement
from common.tester.testcase import TestCase


@mock.patch.object(ClientCityMiddleware, '_get_client_city_now')
@mock.patch.object(ClientCityMiddleware, '_get_client_city')
@mock.patch.object(ClientCityMiddleware, '_get_default_city')
@mock.patch.object(ClientCityMiddleware, '_get_city_domain')
class TestRedirectToNationalVersionMiddleware(TestCase):
    DEFAULT_CITY_TIME = datetime.datetime(2015, 1, 2)

    CLIENT_CITY_TIME = datetime.datetime(2015, 1, 1)
    MY_IP = '127.0.0.1'
    ROOT_DOMAIN = 'ru'
    ANOTHER_DOMAIN = 'ua'

    def setUp(self):
        self.DEFAULT_CITY_ID = 213
        self.DEFAULT_CITY = create_settlement()
        self.DEFAULT_CITY._geo_id = self.DEFAULT_CITY_ID

        self.CLIENT_CITY_ID = 42
        self.CLIENT_CITY = create_settlement()
        self.CLIENT_CITY._geo_id = self.CLIENT_CITY_ID

    def test_known_everything(self, m_get_city_domain, m_get_default_city,
                              m_get_client_city, m_get_client_city_now):
        middleware = ClientCityMiddleware()
        request = self.create_request()

        m_get_client_city_now.return_value = self.CLIENT_CITY_TIME
        m_get_client_city.return_value = (self.CLIENT_CITY, self.CLIENT_CITY_ID)
        m_get_default_city.return_value = self.DEFAULT_CITY
        m_get_city_domain.return_value = self.ROOT_DOMAIN

        middleware.process_request(request)

        assert request.client_city_real == self.CLIENT_CITY
        assert request.geoid == self.CLIENT_CITY_ID
        assert request.client_city == self.CLIENT_CITY
        assert request.client_city_domain == self.ROOT_DOMAIN
        assert request.default_city == self.DEFAULT_CITY
        assert request.client_time == self.CLIENT_CITY_TIME

        m_get_client_city_now.assert_called_once_with(request, self.CLIENT_CITY)
        m_get_client_city.assert_called_once_with(request, self.MY_IP)
        m_get_default_city.assert_called_once_with(self.ROOT_DOMAIN)
        m_get_city_domain.assert_called_once_with(self.ROOT_DOMAIN, self.CLIENT_CITY)

    def test_unknown_client_city(self, m_get_city_domain, m_get_default_city,
                                 m_get_client_city, m_get_client_city_now):
        middleware = ClientCityMiddleware()
        request = self.create_request()

        m_get_client_city_now.return_value = self.CLIENT_CITY_TIME
        m_get_client_city.return_value = (None, self.CLIENT_CITY_ID)
        m_get_default_city.return_value = self.DEFAULT_CITY
        m_get_city_domain.return_value = self.ROOT_DOMAIN

        middleware.process_request(request)

        assert request.client_city_real is None
        assert request.geoid == self.CLIENT_CITY_ID
        assert request.client_city == self.DEFAULT_CITY
        assert request.client_city_domain == self.ROOT_DOMAIN
        assert request.default_city == self.DEFAULT_CITY
        assert request.client_time == self.CLIENT_CITY_TIME

        m_get_client_city_now.assert_called_once_with(request, self.DEFAULT_CITY)
        m_get_client_city.assert_called_once_with(request, self.MY_IP)
        m_get_default_city.assert_called_once_with(self.ROOT_DOMAIN)
        m_get_city_domain.assert_called_once_with(self.ROOT_DOMAIN, self.DEFAULT_CITY)

    def test_unknown_client_city_id(self, m_get_city_domain, m_get_default_city,
                                    m_get_client_city, m_get_client_city_now):
        middleware = ClientCityMiddleware()
        request = self.create_request()

        m_get_client_city_now.return_value = self.CLIENT_CITY_TIME
        m_get_client_city.return_value = (self.CLIENT_CITY, None)
        m_get_default_city.return_value = self.DEFAULT_CITY
        m_get_city_domain.return_value = self.ROOT_DOMAIN

        middleware.process_request(request)

        assert request.client_city_real == self.CLIENT_CITY
        assert request.geoid == self.DEFAULT_CITY._geo_id
        assert request.client_city == self.CLIENT_CITY
        assert request.client_city_domain == self.ROOT_DOMAIN
        assert request.default_city == self.DEFAULT_CITY
        assert request.client_time == self.CLIENT_CITY_TIME

        m_get_client_city_now.assert_called_once_with(request, self.CLIENT_CITY)
        m_get_client_city.assert_called_once_with(request, self.MY_IP)
        m_get_default_city.assert_called_once_with(self.ROOT_DOMAIN)
        m_get_city_domain.assert_called_once_with(self.ROOT_DOMAIN, self.CLIENT_CITY)

    def test_ignore_domain_city_for_com(self, m_get_city_domain, m_get_default_city,
                                        m_get_client_city, m_get_client_city_now):
        middleware = ClientCityMiddleware()
        request = self.create_request()
        request.root_domain = 'com'

        m_get_client_city_now.side_effect = [self.CLIENT_CITY_TIME, self.DEFAULT_CITY_TIME]
        m_get_client_city.return_value = (self.CLIENT_CITY, self.CLIENT_CITY_ID)
        m_get_default_city.return_value = self.DEFAULT_CITY
        m_get_city_domain.return_value = 'com'

        middleware.process_request(request)

        assert request.client_city_real == self.CLIENT_CITY
        assert request.geoid == self.CLIENT_CITY_ID
        assert request.client_city == self.CLIENT_CITY
        assert request.client_city_domain == 'com'
        assert request.default_city == self.DEFAULT_CITY
        assert request.client_time == self.CLIENT_CITY_TIME

        m_get_client_city_now.assert_called_once_with(request, self.CLIENT_CITY)
        m_get_client_city.assert_called_once_with(request, self.MY_IP)
        m_get_default_city.assert_called_once_with('com')
        m_get_city_domain.assert_called_once_with('com', self.CLIENT_CITY)

    def create_request(self):
        factory = RequestFactory()

        request = factory.get('/some/')
        request.client_ip = self.MY_IP
        request.root_domain = self.ROOT_DOMAIN

        return request


class TestClientCityGetDefaultCityMiddleware(TestCase):
    def setUp(self):
        ua = Country.objects.create(title='Ukraine', domain_zone='ua')
        create_settlement(id=1, country=ua, majority=CityMajority.CAPITAL_ID)

        kz = Country.objects.create(title='Ukraine', domain_zone='kz')
        create_settlement(id=2, country=kz, majority=CityMajority.CAPITAL_ID)

        self.middleware = ClientCityMiddleware()

    def test_default_ru(self):
        assert self.middleware._get_default_city('ru').id == 213

    def test_default_ua(self):
        assert self.middleware._get_default_city('ua').id == 1

    def test_default_kz(self):
        assert self.middleware._get_default_city('kz').id == 2


class TestLookupForGeoIds(TestCase):
    YEKATERINBURG_GEO_ID = 54

    def setUp(self):
        create_settlement(_geo_id=self.YEKATERINBURG_GEO_ID)

    def _get_city(self, geo_id):
        settlement = get_city_for_geo_ids([geo_id])
        assert settlement
        return settlement._geo_id

    def test_moscow(self):
        assert self._get_city(settings.MOSCOW_GEO_ID) == settings.MOSCOW_GEO_ID

    def test_russia(self):
        assert self._get_city(settings.RUSSIA_GEO_ID) == settings.MOSCOW_GEO_ID

    def test_ekb(self):
        assert self._get_city(self.YEKATERINBURG_GEO_ID) == self.YEKATERINBURG_GEO_ID
