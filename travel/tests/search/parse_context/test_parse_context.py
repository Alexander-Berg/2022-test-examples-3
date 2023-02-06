# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import mock
import pytest
from django.test import Client
from hamcrest import assert_that, has_entries, close_to, contains

from common.models.geo import Country, CityMajority, Settlement, StationMajority
from common.models.transport import TransportType
from common.tester.factories import (
    create_settlement, create_station, create_suburban_zone, create_region
)
from common.tester.testcase import TestCase
from geosearch.models import NameSearchIndex
from geosearch.views.pointlist import PointList
from route_search.models import NearestSuburbanDirection

from travel.rasp.morda_backend.morda_backend.search.parse_context import geosearch_wrapper
from travel.rasp.morda_backend.morda_backend.search.parse_context.geosearch_wrapper import (
    init_point_lists, process_point_lists, check_ambiguous, check_same_points, process_nearest,
    remove_countries_from_point_lists, GeosearchState
)
from travel.rasp.morda_backend.morda_backend.search.parse_context import views
from travel.rasp.morda_backend.morda_backend.search.parse_context import geosearch_serialization


class TestParseContext(TestCase):
    @classmethod
    def setUpTestData(cls):
        cls.client = Client()
        cls.t_type_code = 'bus'
        cls.t_type = TransportType.objects.get(id=TransportType.BUS_ID)
        cls.from_title = 'point-from'
        cls.from_key = 'c1'
        cls.from_slug = 'slug-from'
        cls.to_title = 'point-to'
        cls.to_key = 'c2'
        cls.to_slug = 'slug-to'
        cls.client_settlement_id = 777
        cls.client_settlement = create_settlement(id=cls.client_settlement_id)
        cls.region_ekb = create_region(title_ru='Свердловская область')
        cls.region_omsk = create_region(title_ru='Омская область')
        cls.national_version = 'ua'
        cls.language = 'uk'

    def test_parse_context(self):
        """
        Тест, использующий моки (mocks).
        Проверяем корректный разбор GET-параметров.
        А так же - корректность вызовов geosearch_wrapper'а и JSON-сериализатора.
        """

        def make_state(input_state, *args, **kwargs):
            return GeosearchState(input_context=input_state.input_context, errors=[])

        p_parse_geosearch_context = mock.patch.object(geosearch_wrapper, 'apply_processors',
                                                      side_effect=make_state, autospec=True)
        p_geosearch_result_json = mock.patch.object(geosearch_serialization, 'geosearch_result_json',
                                                    return_value='context json', autospec=True)

        with p_parse_geosearch_context as m_parse_geosearch_context, p_geosearch_result_json as m_geosearch_result_json:
            args = {
                't_type': self.t_type_code,
                'from_key': self.from_key,
                'from_title': self.from_title,
                'from_slug': self.from_slug,
                'to_key': self.to_key,
                'to_title': self.to_title,
                'to_slug': self.to_slug,
                'client_settlement_id': self.client_settlement_id,
                'national_version': self.national_version
            }
            response = self.client.get('/{}/search/parse-context/'.format(self.language), args)

            assert response.status_code == 200

            data = json.loads(response.content)
            assert data == 'context json'

        m_parse_geosearch_context.assert_has_calls([
            mock.call(mock.ANY, [process_nearest, init_point_lists, remove_countries_from_point_lists]),
            mock.call(mock.ANY, [process_point_lists]),
            mock.call(mock.ANY, [check_ambiguous, check_same_points])
        ])

        input_context = m_parse_geosearch_context.call_args_list[0][0][0].input_context
        self._assert_input_context(input_context)

        input_context_for_json = m_geosearch_result_json.call_args[0][0].input_context
        self._assert_input_context(input_context_for_json)

    def _assert_input_context(self, input_context):
        """
        :type input_context: travel.rasp.morda_backend.morda_backend.data_layer.geosearch_wrapper.InputSearchContext
        """
        assert input_context.from_key == self.from_key
        assert input_context.from_title == self.from_title
        assert input_context.from_slug == self.from_slug
        assert input_context.to_key == self.to_key
        assert input_context.to_title == self.to_title
        assert input_context.to_slug == self.to_slug
        assert input_context.client_settlement == self.client_settlement
        assert input_context.t_type == self.t_type
        assert input_context.national_version == self.national_version
        assert input_context.language == self.language

    def test_integration_cities_by_titles(self):
        """
        Интеграционный тест, используются данные, предвариетельно записанные в базу данных.
        """
        settlement_from = create_settlement(
            title_ru='Екатеринбург',
            title_ru_accusative='Екатеринбург (accusative)',
            title_ru_genitive='Екатеринбурга (genitive)',
            title_ru_locative='Екатеринбурге (locative)',
            title_ru_preposition_v_vo_na='в',
            time_zone='Asia/Yekaterinburg',
            country_id=Country.RUSSIA_ID,
            majority=CityMajority.REGION_CAPITAL_ID,
            region=self.region_ekb,
            slug='ekb'
        )
        settlement_to = create_settlement(
            title_ru='Омск',
            title_ru_accusative='Омск (accusative)',
            title_ru_genitive='Омска (genitive)',
            title_ru_locative='Омске (locative)',
            title_ru_preposition_v_vo_na='в',
            time_zone='Asia/Omsk',
            country_id=Country.RUSSIA_ID,
            majority=CityMajority.REGION_CAPITAL_ID,
            region=self.region_omsk,
            slug='omsk'
        )

        index_records = NameSearchIndex.get_records()
        NameSearchIndex.objects.bulk_create(index_records)

        response = self.client.get(
            '/ru/search/parse-context/?t_type=&from_key=&from_title=Екатеринбург&to_key=&to_title=Омск&'
            'client_settlement_id=213&national_version=ru')

        assert response.status_code == 200

        data = json.loads(response.content)
        assert_that(data, has_entries({
            'transportType': 'all',
            'from': has_entries({
                'key': settlement_from.point_key,
                'title': 'Екатеринбург',
                'titleAccusative': 'Екатеринбург (accusative)',
                'titleGenitive': 'Екатеринбурга (genitive)',
                'titleLocative': 'Екатеринбурге (locative)',
                'preposition': 'в',
                'timezone': 'Asia/Yekaterinburg',
                'region': {'title': 'Свердловская область'},
                'settlement': {
                    'title': 'Екатеринбург',
                    'slug': 'ekb',
                    'key': settlement_from.point_key
                },
                'country': {
                    'code': 'RU',
                    'title': 'Россия',
                    'railwayTimezone': 'Europe/Moscow'
                }
            }),
            'to': has_entries({
                'key': settlement_to.point_key,
                'title': 'Омск',
                'titleAccusative': 'Омск (accusative)',
                'titleGenitive': 'Омска (genitive)',
                'titleLocative': 'Омске (locative)',
                'preposition': 'в',
                'timezone': 'Asia/Omsk',
                'region': {'title': 'Омская область'},
                'settlement': {
                    'title': 'Омск',
                    'slug': 'omsk',
                    'key': settlement_to.point_key
                },
                'country': {
                    'code': 'RU',
                    'title': 'Россия',
                    'railwayTimezone': 'Europe/Moscow'
                }
            }),
            'errors': []
        }))

    def test_integration_cities_by_slugs(self):
        """
        Интеграционный тест, используются данные, предвариетельно записанные в базу данных.
        """
        settlement_from = create_settlement(
            title_ru='Екатеринбург',
            time_zone='Asia/Yekaterinburg',
            country_id=Country.RUSSIA_ID,
            majority=CityMajority.REGION_CAPITAL_ID,
            slug='yekaterinburg'
        )
        settlement_to = create_settlement(
            title_ru='Омск',
            time_zone='Asia/Omsk',
            country_id=Country.RUSSIA_ID,
            majority=CityMajority.REGION_CAPITAL_ID,
            slug='omsk',
            hidden=True
        )

        index_records = NameSearchIndex.get_records()
        NameSearchIndex.objects.bulk_create(index_records)

        response = self.client.get(
            '/ru/search/parse-context/?from_slug=yekaterinburg&to_slug=omsk'
            '&client_settlement_id=213&national_version=ru')

        assert response.status_code == 200

        data = json.loads(response.content)
        assert_that(data, has_entries({
            'transportType': 'all',
            'from': has_entries({
                'key': settlement_from.point_key,
                'title': 'Екатеринбург',
                'timezone': 'Asia/Yekaterinburg',
                'slug': 'yekaterinburg',
                'country': {
                    'code': 'RU',
                    'title': 'Россия',
                    'railwayTimezone': 'Europe/Moscow'
                }
            }),
            'to': has_entries({
                'key': settlement_to.point_key,
                'title': 'Омск',
                'timezone': 'Asia/Omsk',
                'slug': 'omsk',
                'country': {
                    'code': 'RU',
                    'title': 'Россия',
                    'railwayTimezone': 'Europe/Moscow',
                }
            }),
            'errors': []
        }))

    def test_integration_stations_by_titles(self):
        station_from = create_station(
            title_ru='Екатеринбург',
            title_ru_accusative='Екатеринбург (accusative)',
            title_ru_genitive='Екатеринбурга (genitive)',
            title_ru_locative='Екатеринбурге (locative)',
            title_ru_preposition_v_vo_na='в',
            popular_title_ru='Екатеринбург (popular)',
            short_title_ru='Екатеринбург (short)',
            time_zone='Asia/Yekaterinburg',
            country_id=Country.RUSSIA_ID,
            majority=StationMajority.MAIN_IN_CITY_ID
        )
        station_to = create_station(
            title_ru='Омск',
            title_ru_accusative='Омск (accusative)',
            title_ru_genitive='Омска (genitive)',
            title_ru_locative='Омске (locative)',
            title_ru_preposition_v_vo_na='в',
            popular_title_ru='Омск (popular)',
            short_title_ru='Омск (short)',
            time_zone='Asia/Omsk',
            country_id=Country.RUSSIA_ID,
            majority=StationMajority.MAIN_IN_CITY_ID
        )

        index_records = NameSearchIndex.get_records()
        NameSearchIndex.objects.bulk_create(index_records)

        response = self.client.get(
            '/ru/search/parse-context/?t_type=&from_key=&from_title=Екатеринбург&to_key=&to_title=Омск&'
            'client_settlement_id=213&national_version=ru')

        assert response.status_code == 200

        data = json.loads(response.content)
        assert_that(data, has_entries({
            'transportType': 'all',
            'from': has_entries({
                'key': station_from.point_key,
                'title': 'Екатеринбург',
                'titleAccusative': 'Екатеринбург (accusative)',
                'titleGenitive': 'Екатеринбурга (genitive)',
                'titleLocative': 'Екатеринбурге (locative)',
                'preposition': 'в',
                'popularTitle': 'Екатеринбург (popular)',
                'shortTitle': 'Екатеринбург (short)',
                'timezone': 'Asia/Yekaterinburg',
                'country': {
                    'code': 'RU',
                    'title': 'Россия',
                    'railwayTimezone': 'Europe/Moscow'
                }
            }),
            'to': has_entries({
                'key': station_to.point_key,
                'title': 'Омск',
                'titleAccusative': 'Омск (accusative)',
                'titleGenitive': 'Омска (genitive)',
                'titleLocative': 'Омске (locative)',
                'preposition': 'в',
                'popularTitle': 'Омск (popular)',
                'shortTitle': 'Омск (short)',
                'timezone': 'Asia/Omsk',
                'country': {
                    'code': 'RU',
                    'title': 'Россия',
                    'railwayTimezone': 'Europe/Moscow'
                }
            }),
            'errors': []
        }))

    def test_integration_stations_by_slugs(self):
        station_from = create_station(
            title_ru='Екатеринбург',
            time_zone='Asia/Yekaterinburg',
            country_id=Country.RUSSIA_ID,
            majority=StationMajority.MAIN_IN_CITY_ID,
            slug='yekaterinburg'
        )
        station_to = create_station(
            title_ru='Омск',
            time_zone='Asia/Omsk',
            country_id=Country.RUSSIA_ID,
            majority=StationMajority.MAIN_IN_CITY_ID,
            slug='omsk'
        )

        index_records = NameSearchIndex.get_records()
        NameSearchIndex.objects.bulk_create(index_records)

        response = self.client.get(
            '/ru/search/parse-context/?from_slug=yekaterinburg&to_slug=omsk'
            '&client_settlement_id=213&national_version=ru')

        assert response.status_code == 200

        data = json.loads(response.content)
        assert_that(data, has_entries({
            'transportType': 'all',
            'from': has_entries({
                'key': station_from.point_key,
                'title': 'Екатеринбург',
                'timezone': 'Asia/Yekaterinburg',
                'slug': 'yekaterinburg',
                'country': {
                    'code': 'RU',
                    'title': 'Россия',
                    'railwayTimezone': 'Europe/Moscow'
                }
            }),
            'to': has_entries({
                'key': station_to.point_key,
                'title': 'Омск',
                'timezone': 'Asia/Omsk',
                'slug': 'omsk',
                'country': {
                    'code': 'RU',
                    'title': 'Россия',
                    'railwayTimezone': 'Europe/Moscow'
                }
            }),
            'errors': []
        }))


@pytest.mark.dbuser
def test_not_found_direction():
    client = Client()
    response = client.get('/ru/search/parse-context/', {
        't_type': 'suburban', 'nearest': '1',
        'from_key': '', 'from_title': '', 'from_slug': '',
        'to_key': '', 'to_title': '', 'to_slug': '',
        'client_settlement_id': '213',
        'national_version': 'ru'
    })

    assert response.status_code == 200

    data = json.loads(response.content)
    assert data == {
        'transportType': 'suburban',
        'errors': [{
            'nearest': 'default_direction_not_found',
            'type': 'nearest_default_direction_not_found'
        }],
        'from': {'timezone': None, 'title': None, 'key': None, 'country': None, 'slug': None},
        'originalFrom': {'timezone': None, 'title': None, 'key': None, 'country': None, 'slug': None},
        'to': {'timezone': None, 'title': None, 'key': None, 'country': None, 'slug': None},
        'originalTo': {'timezone': None, 'title': None, 'key': None, 'country': None, 'slug': None},
    }


@pytest.mark.dbuser
def test_found_direction():
    client = Client()
    station_from = create_station(settlement=Settlement.MOSCOW_ID)
    station_to = create_station(settlement=Settlement.MOSCOW_ID)
    NearestSuburbanDirection.objects.create(settlement_id=Settlement.MOSCOW_ID, station=station_from,
                                            transport_center=station_to)
    response = client.get('/ru/search/parse-context/', {
        't_type': 'suburban', 'nearest': '1',
        'from_key': '', 'from_title': '',
        'to_key': '', 'to_title': '',
        'client_settlement_id': Settlement.MOSCOW_ID,
        'national_version': 'ru'
    })

    assert response.status_code == 200

    country_dict = {'code': 'RU', 'railwayTimezone': 'Europe/Moscow', 'title': 'Россия'}

    data = json.loads(response.content)
    assert_that(data, has_entries({
        'transportType': 'suburban',
        'errors': [],
        'from': has_entries({
            'timezone': 'Europe/Moscow',
            'title': station_from.title,
            'key': station_from.point_key,
            'country': country_dict
        }),
        'to': has_entries({
            'timezone': 'Europe/Moscow',
            'title': station_to.title,
            'key': station_to.point_key,
            'country': country_dict
        }),
    }))


@pytest.mark.dbuser
def test_distance():
    # https://yandex.ru/maps/?ll=66.765351%2C56.429448&z=7&rl=73.36538791%2C54.98982533~-12.74414062%2C1.84153299
    settlement_from = create_settlement(title_ru='Екатеринбург', latitude=56.838607, longitude=60.605514)
    settlement_to = create_settlement(title_ru='Омск', latitude=54.917904, longitude=73.379121)

    index_records = NameSearchIndex.get_records()
    NameSearchIndex.objects.bulk_create(index_records)

    response = Client().get('/ru/search/parse-context/', {
        'from_key': settlement_from.point_key,
        'to_key': settlement_to.point_key,
        'client_settlement_id': 213,
        'national_version': 'ru'
    })

    assert_that(json.loads(response.content), has_entries({
        'errors': [],
        'distance': close_to(822, 10)
    }))

    settlement_to.latitude = None
    settlement_to.save()

    response = Client().get('/ru/search/parse-context/', {
        'from_key': settlement_from.point_key,
        'to_key': settlement_to.point_key,
        'client_settlement_id': 213,
        'national_version': 'ru'
    })

    assert_that(json.loads(response.content), has_entries({
        'errors': [],
        'distance': None
    }))


@pytest.mark.dbuser
def test_on_error_retry():
    client = Client()

    def make_error_state(input_state, *args, **kwargs):
        return GeosearchState(input_context=input_state.input_context,
                              point_list_from=None,
                              point_list_to=None,
                              errors=[{'type': 'another_error'}])

    with mock.patch.object(geosearch_wrapper, 'apply_processors',
                           side_effect=make_error_state, autospec=True) as m_parse_geosearch_context:
        response = client.get('/ru/search/parse-context/',
                              {'t_type': 'train', 'from_key': 'from_station', 'from_title': 'from_station_title',
                               'to_slug': 'to_station', 'national_version': 'ru'})

    assert response.status_code == 200
    assert m_parse_geosearch_context.call_count == 8  # 4 * 2 (for main and all types)

    for (state, processors), kwargs in m_parse_geosearch_context.call_args_list:
        context = state.input_context
        assert context.from_key == 'from_station'
        assert context.from_title == 'from_station_title'
        assert context.to_slug == 'to_station'

    for (state, processors), kwargs in m_parse_geosearch_context.call_args_list[:4]:
        context = state.input_context
        assert context.t_type.id == TransportType.TRAIN_ID

    for (state, processors), kwargs in m_parse_geosearch_context.call_args_list[4:]:
        context = state.input_context
        assert context.t_type is None


@pytest.mark.dbuser
def test_original_points():
    client = Client()

    from_settlement_original = create_settlement(id=11, title='from_original_title', slug='from_original_slug')
    from_settlement_reduced = create_settlement(id=22, title='from_reduced_title', slug='from_reduced_slug')
    to_settlement_original = create_settlement(id=101, title='to_original_title', slug='to_original_slug')
    to_settlement_reduced = create_settlement(id=202, title='to_reduced_title', slug='to_reduced_slug')

    def make_original_lists(input_state):
        return GeosearchState(input_context=input_state.input_context,
                              point_list_from=PointList(from_settlement_original),
                              point_list_to=PointList(to_settlement_original))

    def make_reduced_lists(input_state):
        return GeosearchState(input_context=input_state.input_context,
                              point_list_from=PointList(from_settlement_reduced),
                              point_list_to=PointList(to_settlement_reduced))

    with mock.patch.object(views, 'init_point_lists', side_effect=make_original_lists, autospec=True), \
            mock.patch.object(views, 'process_point_lists', side_effect=make_reduced_lists, autospec=True):
        response = client.get('/ru/search/parse-context/',
                              {'t_type': 'train', 'from_key': 'from_station', 'from_title': 'from_station_title',
                               'to_slug': 'to_station', 'national_version': 'ru'})

    assert response.status_code == 200

    data = json.loads(response.content)
    assert_that(data, has_entries({
        'from': has_entries({
            'key': from_settlement_reduced.point_key,
            'title': from_settlement_reduced.title
        }),
        'originalFrom': has_entries({
            'key': from_settlement_original.point_key,
            'title': from_settlement_original.title
        }),
        'to': has_entries({
            'key': to_settlement_reduced.point_key,
            'title': to_settlement_reduced.title
        }),
        'originalTo': has_entries({
            'key': to_settlement_original.point_key,
            'title': to_settlement_original.title
        }),
        'errors': []
    }))


@pytest.mark.dbuser
def test_original_points_geosearch_reducing():
    """
    Функциональный тест, проверяющий нужное поведение geosearch при определении найденных точек до сужения.
    Есть 2 города, найденных по имени, которые после поиска сужаются до основных станций.
    Поиск должен найти города и вернуть их в качестве точек до сужения.
    """
    sett_a = create_settlement(id=100, title='aaa', slug='sett_a', type_choices='train,suburban')
    sett_b = create_settlement(id=200, title='bbb', slug='sett_b', type_choices='train,suburban')
    station_a_from = create_station(id=111, title='aaa_station', slug='station_a_from', t_type='train',
                                    settlement=sett_a, majority=StationMajority.MAIN_IN_CITY_ID)
    station_b_to = create_station(id=211, title='bbb_station', slug='station_b_from', t_type='train',
                                  settlement=sett_b, majority=StationMajority.MAIN_IN_CITY_ID)

    index_records = NameSearchIndex.get_records()
    NameSearchIndex.objects.bulk_create(index_records)

    client = Client()
    response = client.get('/ru/search/parse-context/',
                          {'t_type': 'suburban', 'from_title': 'aaa', 'to_title': 'bbb', 'national_version': 'ru'})

    assert response.status_code == 200

    data = json.loads(response.content)
    assert_that(data, has_entries({
        'from': has_entries({
            'key': station_a_from.point_key,
            'title': station_a_from.title
        }),
        'originalFrom': has_entries({
            'key': sett_a.point_key,
            'title': sett_a.title
        }),
        'to': has_entries({
            'key': station_b_to.point_key,
            'title': station_b_to.title
        }),
        'originalTo': has_entries({
            'key': sett_b.point_key,
            'title': sett_b.title
        }),
        'errors': []
    }))


@pytest.mark.dbuser
def test_original_points_geosearch_():
    """
    Фунциональный тест, проверяющий нужное поведение geosearch при определении найденных точек до сужения.
    Есть 4 города (по 2 с совпадающими наименованиями), 2 из них в одной зоне.
    Города в одной зоне после поиска сужаются до станции.
    Поиск должен найти города и вернуть их в качестве точек до сужения.
    """

    sett_zone = create_settlement(id=100, title='zzz', slug='sett_zone', type_choices='train,suburban')
    suburban_zone_id = 1000
    sett_zone = create_suburban_zone(
        id=suburban_zone_id,
        settlement=sett_zone,
        code='zone code',
    )

    create_settlement(id=101, title='aaa', slug='sett_a1', type_choices='train,suburban')
    sett_a2 = create_settlement(id=102, title='aaa', slug='sett_a2', type_choices='train,suburban',
                                suburban_zone=sett_zone)
    create_settlement(id=201, title='bbb', slug='sett_b1', type_choices='train,suburban')
    sett_b2 = create_settlement(id=202, title='bbb', slug='sett_b2', type_choices='train,suburban',
                                suburban_zone=sett_zone)
    station_a_from = create_station(id=111, title='aaa_station', slug='station_a_from', t_type='train',
                                    settlement=sett_a2, majority=StationMajority.MAIN_IN_CITY_ID)
    station_b_to = create_station(id=211, title='bbb_station', slug='station_b_from', t_type='train',
                                  settlement=sett_b2, majority=StationMajority.MAIN_IN_CITY_ID)

    index_records = NameSearchIndex.get_records()
    NameSearchIndex.objects.bulk_create(index_records)

    client = Client()
    response = client.get('/ru/search/parse-context/',
                          {'t_type': 'suburban', 'from_title': 'aaa', 'to_title': 'bbb', 'national_version': 'ru'})

    assert response.status_code == 200

    data = json.loads(response.content)
    assert_that(data, has_entries({
        'from': has_entries({
            'key': station_a_from.point_key,
            'title': station_a_from.title
        }),
        'originalFrom': has_entries({
            'key': sett_a2.point_key,
            'title': sett_a2.title
        }),
        'to': has_entries({
            'key': station_b_to.point_key,
            'title': station_b_to.title
        }),
        'originalTo': has_entries({
            'key': sett_b2.point_key,
            'title': sett_b2.title
        }),
        'errors': []
    }))


@pytest.mark.dbuser
def test_airports():
    """
    Проверка замены наименований аэропортов
    https://st.yandex-team.ru/RASPFRONT-7570
    """
    city_from = create_settlement(id=101, title_ru='Город', slug='city', type_choices='train')
    station_from = create_station(id=111, slug='station_from', t_type='plane', settlement=city_from, type_choices='plane,suburban',
                                  title_ru='Город', title_ru_locative='Города', title_ru_accusative='Город')
    city_to = create_settlement(id=201, title_ru='Столица', slug='capital', type_choices='train')
    station_to = create_station(id=211, slug='station_to', t_type='plane', settlement=city_to, type_choices='plane,suburban',
                                title_ru='Аэропорто', title_ru_locative='Аэропорто', title_ru_accusative='Аэропорто')

    index_records = NameSearchIndex.get_records()
    NameSearchIndex.objects.bulk_create(index_records)

    client = Client()
    response = client.get('/ru/search/parse-context/',
                          {'t_type': 'suburban',
                           'from_slug': station_from.slug,
                           'to_slug': station_to.slug,
                           'national_version': 'ru'})

    assert response.status_code == 200

    data = json.loads(response.content)
    assert_that(data, has_entries({
        'from': has_entries({
            'key': station_from.point_key,
            'title': 'Аэропорт (Город)',
            'titleLocative': 'Аэропорту (Город)',
            'titleAccusative': 'Аэропорт (Город)',
        }),
        'originalFrom': has_entries({
            'key': station_from.point_key,
            'title': station_from.title_ru,
            'titleLocative': station_from.title_ru_locative,
            'titleAccusative': station_from.title_ru_accusative,
        }),
        'to': has_entries({
            'key': station_to.point_key,
            'title': 'Аэропорт Аэропорто',
            'titleLocative': 'аэропорту Аэропорто',
            'titleAccusative': 'Аэропорт Аэропорто',
        }),
        'originalTo': has_entries({
            'key': station_to.point_key,
            'title': station_to.title_ru,
            'titleLocative': station_to.title_ru_locative,
            'titleAccusative': station_to.title_ru_accusative,
        }),
        'errors': []
    }))


@pytest.mark.dbuser
def test_invalid_t_type():
    client = Client()

    response = client.get('/ru/search/parse-context/', {
        't_type': '321d321', 'nearest': '1',
        'from_key': '', 'from_title': '',
        'to_key': '', 'to_title': '',
        'national_version': 'ru'
    })

    assert response.status_code == 400

    data = json.loads(response.content)
    assert_that(data, has_entries({
        'errors': contains(has_entries({
            'field': 't_type', 'type': 't_type_not_found'
        }))
    }))


@pytest.mark.dbuser
def test_client_settlement_not_found():
    client = Client()

    response = client.get('/ru/search/parse-context/', {
        't_type': 'bus', 'nearest': '1',
        'from_key': '', 'from_title': '',
        'to_key': '', 'to_title': '',
        'client_settlement_id': 420000000,
        'national_version': 'ru'
    })

    assert response.status_code == 400

    data = json.loads(response.content)
    assert_that(data, has_entries({
        'errors': contains(has_entries({
            'field': 'client_settlement_id', 'type': 'settlement_not_found'
        }))
    }))
