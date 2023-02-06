# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
from django.test import Client

from common.models.geo import Country
from common.tester.factories import create_settlement, create_country
from common.tester.testcase import TestCase


def _fake_search_sample_point_json(point, language):
    return 'point json - ' + str(point.id)


@mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.search_sample_point.search_sample_point_json',
            side_effect=_fake_search_sample_point_json)
class TestSearchSamplePoints(TestCase):
    def setUp(self):
        self.client = Client()

    def test_russia(self, m_search_sample_point_json):
        """
        Переданы на вход: русский город, русская национальная версия, русский язык.
        Проверяем:
        - с нужными параметрами вызываются функции data_layer'а и json-сериализации;
        - из БД выбираются нужные города.
        """
        settlement_id = 987
        create_settlement(id=settlement_id, country_id=Country.RUSSIA_ID)
        moscow = create_settlement(213)
        ekb = create_settlement(id=54, title='Екатеринбург', country_id=Country.RUSSIA_ID)

        points_keys = ('c213', 'c54')

        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.search_sample_points.get_search_sample_points_keys',
                        return_value=points_keys) as m_get_search_sample_points_keys:
            response = self.client.get(
                '/ru/settlement/{}/search-sample-points/?national_version=ru'.format(settlement_id))
            assert response.status_code == 200
            data = json.loads(response.content)

            assert data == {
                'point_from': 'point json - 213',
                'point_to': 'point json - 54'
            }

        m_get_search_sample_points_keys.assert_called_once_with(settlement_id, 'ru')

        assert m_search_sample_point_json.call_count == 2
        m_search_sample_point_json.assert_has_calls(
            [
                mock.call(ekb, 'ru'),
                mock.call(moscow, 'ru'),
            ],
            any_order=True
        )

    def test_ukraine(self, m_search_sample_point_json):
        """
        Переданы на вход: украинский город, украинская национальная версия, украинский язык.
        Проверяем:
        - с нужными параметрами вызываются функции data_layer'а и json-сериализации;
        - из БД выбираются нужные города.
        """
        settlement_id = 987
        create_country(id=Country.UKRAINE_ID, title='Украина')
        create_settlement(id=settlement_id, country_id=Country.UKRAINE_ID)
        kiev = create_settlement(id=143, title='Киев', country_id=Country.UKRAINE_ID)
        lvov = create_settlement(id=144, title='Львов', country_id=Country.UKRAINE_ID)

        points_keys = ('c143', 'c144')

        with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.data_layer.search_sample_points.get_search_sample_points_keys',
                        return_value=points_keys) as m_get_search_sample_points_keys:
            response = self.client.get(
                '/uk/settlement/{}/search-sample-points/?national_version=ua'.format(settlement_id))
            assert response.status_code == 200
            data = json.loads(response.content)

            assert data == {
                'point_from': 'point json - 143',
                'point_to': 'point json - 144'
            }

        m_get_search_sample_points_keys.assert_called_once_with(settlement_id, 'ua')

        assert m_search_sample_point_json.call_count == 2
        m_search_sample_point_json.assert_has_calls(
            [
                mock.call(kiev, 'uk'),
                mock.call(lvov, 'uk'),
            ],
            any_order=True
        )
