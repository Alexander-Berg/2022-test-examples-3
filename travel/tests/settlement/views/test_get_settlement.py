# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
from django.test import Client

from common.models.geo import CityMajority
from common.tester.factories import create_settlement, create_country
from common.tester.testcase import TestCase


class TestGetSetlement(TestCase):
    def setUp(self):
        self.client = Client()
        self.settlement_id = 777
        self.settlement_slug = 'slug777'
        self.geo_ids_string = u'10,20,30'
        self.geo_ids = [10, 20, 30]
        self.ip = u'144.72.36.18'
        self.user_agent = u'Mozilla'
        self.root_domain = u'org'
        self.national_version = u'ua'
        self.settlement_json = {'id': self.settlement_id, 'slug': self.settlement_slug}

        self.settlement = create_settlement(id=self.settlement_id, slug=self.settlement_slug)

        self.ukraine = create_country(domain_zone='ua')
        self.ukrainian_capital = create_settlement(country=self.ukraine, majority=CityMajority.CAPITAL_ID)

    def test_get_settlement_by_id(self):
        """
        Город найден по id
        """
        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_settlement_by_slug',
                        return_value=self.settlement) as m_get_by_slug, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.'
                           'get_visible_nearest_settlement_by_geo_ids') as m_get_by_geo_ids, \
                mock.patch('common.models.geo.Settlement.'
                           'get_default_for_root_domain') as m_get_default_for_root_domain, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_moscow',
                           return_value=self.settlement) as m_get_moscow, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.settlement.settlement_json',
                           return_value=self.settlement_json) as m_settlement_json:

            path = '/uk/settlement/?national_version=ua&id={}'.format(self.settlement_id)
            self._assert_response(path)

        m_get_by_slug.assert_not_called()
        m_get_by_geo_ids.assert_not_called()
        m_get_default_for_root_domain.assert_not_called()
        m_get_moscow.assert_not_called()
        m_settlement_json.assert_called_once_with(self.settlement, self.national_version, u'uk')

    def test_get_settlement_by_slug(self):
        """
        Город найден по slug
        """
        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_settlement_by_id',
                        return_value=self.settlement) as m_get_by_id, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.'
                           'get_visible_nearest_settlement_by_geo_ids') as m_get_by_geo_ids, \
                mock.patch('common.models.geo.Settlement.'
                           'get_default_for_root_domain') as m_get_default_for_root_domain, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_moscow',
                           return_value=self.settlement) as m_get_moscow, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.settlement.settlement_json',
                           return_value=self.settlement_json) as m_settlement_json:

            path = '/uk/settlement/?national_version=ua&slug={}'.format(self.settlement_slug)
            self._assert_response(path)

        m_get_by_id.assert_not_called()
        m_get_by_geo_ids.assert_not_called()
        m_get_default_for_root_domain.assert_not_called()
        m_get_moscow.assert_not_called()
        m_settlement_json.assert_called_once_with(self.settlement, self.national_version, u'uk')

    def test_get_settlement_by_geo_ids(self):
        """
        id передан, но город не найден по id.
        """
        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_settlement_by_id',
                        return_value=None) as m_get_by_id, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.'
                           'get_visible_nearest_settlement_by_geo_ids') as m_get_by_geo_ids, \
                mock.patch('common.models.geo.Settlement.'
                           'get_default_for_root_domain') as m_get_default_for_root_domain, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_moscow',
                           return_value=self.settlement) as m_get_moscow, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.settlement.settlement_json',
                           return_value=self.settlement_json) as m_settlement_json:

            path = '/uk/settlement/?national_version=ua&id={}&geo_ids={}'.format(self.settlement_id,
                                                                                 self.geo_ids_string)
            self._assert_response_none(path)

        m_get_by_id.assert_called_once_with(self.settlement_id)
        m_get_by_geo_ids.assert_not_called()
        m_get_default_for_root_domain.assert_not_called()
        m_get_moscow.assert_not_called()
        m_settlement_json.assert_not_called()

    def test_override_by_national_domain(self):
        """
        Переопределение города по нацдомену, у страны поселения полученного по geo_ids
        определена доменная зона.
        """
        path = '/ru/settlement/?national_version=ru&geo_ids={}&root_domain=ua'.format(self.geo_ids_string)
        response = self.client.get(path)
        assert response.status_code == 200

        data = json.loads(response.content)
        assert data['id'] == self.ukrainian_capital.id

    def test_override_by_national_domain_country_has_no_domain_zone(self):
        """
        Переопределение города по нацдомену, у страны поселения полученного по geo_ids
        доменная зона не определена.
        """
        some_country = create_country()
        some_capital = create_settlement(_geo_id=42, country=some_country, majority=CityMajority.CAPITAL_ID)

        path = '/ru/settlement/?national_version=ru&geo_ids={}&root_domain=ua'.format(some_capital._geo_id)
        response = self.client.get(path)
        assert response.status_code == 200

        data = json.loads(response.content)
        assert data['id'] == self.ukrainian_capital.id

    def test_get_settlement_by_geo_ids_absent_id(self):
        """
        id не передан. Город найден по geo_ids.
        """
        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_settlement_by_id') as m_get_by_id, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_nearest_settlement_by_geo_ids',
                           return_value=self.settlement) as m_get_by_geo_ids, \
                mock.patch('common.models.geo.Settlement.'
                           'get_default_for_root_domain') as m_get_default_for_root_domain, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_moscow',
                           return_value=self.settlement) as m_get_moscow, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.settlement.settlement_json',
                           return_value=self.settlement_json) as m_settlement_json:

            path = '/uk/settlement/?national_version=ua&geo_ids={}'.format(self.geo_ids_string)
            self._assert_response(path)

        m_get_by_id.assert_not_called()
        m_get_by_geo_ids.assert_called_once_with(self.geo_ids, self.national_version)
        m_get_default_for_root_domain.assert_not_called()
        m_get_moscow.assert_not_called()
        m_settlement_json.assert_called_once_with(self.settlement, u'ua', u'uk')

    def test_get_settlement_by_geo_ids_invalid_id(self):
        """
        id передан, но невалидный.
        """
        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_settlement_by_id') as m_get_by_id, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_nearest_settlement_by_geo_ids',
                           return_value=self.settlement) as m_get_by_geo_ids, \
                mock.patch('common.models.geo.Settlement.'
                           'get_default_for_root_domain') as m_get_default_for_root_domain, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_moscow',
                           return_value=self.settlement) as m_get_moscow, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.settlement.settlement_json',
                           return_value=self.settlement_json) as m_settlement_json:

            path = '/uk/settlement/?national_version=ua&id=qwerty&geo_ids={}'.format(self.geo_ids_string)
            self._assert_response_none(path)

        m_get_by_id.assert_not_called()
        m_get_by_geo_ids.assert_not_called()
        m_get_default_for_root_domain.assert_not_called()
        m_get_moscow.assert_not_called()
        m_settlement_json.assert_not_called()

    def test_get_settlement_by_root_domain(self):
        """
        id не передан, город не найден по geo_ids. Найден по root_domain
        """
        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_settlement_by_id') as m_get_by_id, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_nearest_settlement_by_geo_ids',
                           return_value=None) as m_get_by_geo_ids, \
                mock.patch('common.models.geo.Settlement.get_default_for_root_domain',
                           return_value=self.settlement) as m_get_default_for_root_domain, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_moscow') as m_get_moscow, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.settlement.settlement_json',
                           return_value=self.settlement_json) as m_settlement_json:

            path = '/uk/settlement/?national_version=ua&geo_ids={}&root_domain={}'.format(self.geo_ids_string,
                                                                                          self.root_domain)

            self._assert_response(path)

        m_get_by_id.assert_not_called()
        m_get_by_geo_ids.assert_called_once_with(self.geo_ids, self.national_version)
        m_get_default_for_root_domain.assert_called_once_with(self.root_domain)
        m_get_moscow.assert_not_called()
        m_settlement_json.assert_called_once_with(self.settlement, self.national_version, u'uk')

    def test_get_settlement_by_root_domain_absent_id_absent_geo_ids(self):
        """
        Параметры id, geo_ids не переданы. Город найден по root_domain
        """
        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_settlement_by_id') as m_get_by_id, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.'
                           'get_visible_nearest_settlement_by_geo_ids') as m_get_by_geo_ids, \
                mock.patch('common.models.geo.Settlement.get_default_for_root_domain',
                           return_value=self.settlement) as m_get_default_for_root_domain, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_moscow') as m_get_moscow, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.settlement.settlement_json',
                           return_value=self.settlement_json) as m_settlement_json:

            path = '/uk/settlement/?national_version=ua&root_domain={}'.format(self.root_domain)
            self._assert_response(path)

        m_get_by_id.assert_not_called()
        m_get_by_geo_ids.assert_not_called()
        m_get_default_for_root_domain.assert_called_once_with(self.root_domain)
        m_get_moscow.assert_not_called()
        m_settlement_json.assert_called_once_with(self.settlement, self.national_version, u'uk')

    def test_get_settlement_not_found(self):
        """
        id не передан, город по geo_ids, root_domain не найден. Получена Москва
        """
        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_settlement_by_id') as m_get_by_id, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_nearest_settlement_by_geo_ids',
                           return_value=None) as m_get_by_geo_ids, \
                mock.patch('common.models.geo.Settlement.get_default_for_root_domain',
                           return_value=None) as m_get_default_for_root_domain, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_moscow',
                           return_value=self.settlement) as m_get_moscow, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.settlement.settlement_json',
                           return_value=self.settlement_json) as m_settlement_json:

            path = '/uk/settlement/?national_version=ua&geo_ids={}&root_domain={}'.format(self.geo_ids_string,
                                                                                          self.root_domain)

            self._assert_response(path)

        m_get_by_id.assert_not_called()
        m_get_by_geo_ids.assert_called_once_with(self.geo_ids, self.national_version)
        m_get_default_for_root_domain.assert_called_once_with(self.root_domain)
        m_get_moscow.assert_called_once_with()
        m_settlement_json.assert_called_once_with(self.settlement, self.national_version, u'uk')

    def test_get_without_parameters(self):
        """
        Переметры id, geo_ids, root_domain не переданы. Получена Москва
        """
        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_visible_settlement_by_id') as m_get_by_id, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.'
                           'get_visible_nearest_settlement_by_geo_ids') as m_get_by_geo_ids, \
                mock.patch('common.models.geo.Settlement.'
                           'get_default_for_root_domain') as m_get_default_for_root_domain, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.settlement.get_moscow',
                           return_value=self.settlement) as m_get_moscow, \
                mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.settlement.settlement_json',
                           return_value=self.settlement_json) as m_settlement_json:

            path = '/uk/settlement/?national_version=ua'
            self._assert_response(path)

        m_get_by_id.assert_not_called()
        m_get_by_geo_ids.assert_not_called()
        m_get_default_for_root_domain.assert_not_called()
        m_get_moscow.assert_called_once_with()
        m_settlement_json.assert_called_once_with(self.settlement, self.national_version, u'uk')

    def _assert_response(self, path):
        response = self.client.get(path)
        assert response.status_code == 200
        data = json.loads(response.content)
        assert data == self.settlement_json

    def _assert_response_none(self, path):
        response = self.client.get(path)
        assert response.status_code == 200
        data = json.loads(response.content)
        assert data is None
