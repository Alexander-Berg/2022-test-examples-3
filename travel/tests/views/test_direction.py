# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from collections import namedtuple
from contextlib import contextmanager
from datetime import date, datetime

import mock
import pytest
import pytz
from django.conf import settings
from django.test.client import Client

from common.apps.train.models import Facility
from common.models.currency import Price
from common.models.geo import Country, Settlement
from common.tester.factories import create_deluxe_train, create_settlement, create_station, create_thread
from common.tester.transaction_context import transaction_fixture
from common.utils.date import MSK_TZ, parse_date as utils_parse_date
from travel.rasp.wizards.train_wizard_api.direction.schedule_cache import schedule_cache
from travel.rasp.wizards.train_wizard_api.lib.express_system_provider import express_system_provider
from travel.rasp.wizards.train_wizard_api.lib.facility_provider import facility_provider
from travel.rasp.wizards.train_wizard_api.lib.optimal_direction_provider import optimal_direction_provider
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.tariff_direction_info_provider import (
    Place, TariffDirectionInfo, TariffDirectionUpdatedInfo, TariffDirectionUpdatedInfoRecord
)
from travel.rasp.wizards.train_wizard_api.lib.pgaas_price_store.train_info_provider import TrainInfoModel
import travel.rasp.wizards.train_wizard_api.lib.storage_timed_execute as storage_timed_execute
from travel.rasp.wizards.train_wizard_api.lib.train_city_provider import train_city_provider
from travel.rasp.wizards.train_wizard_api.views import direction
from travel.rasp.wizards.wizard_lib.cache import Translations
from travel.rasp.wizards.wizard_lib.serialization.date import parse_date


@pytest.fixture
def m_find_tdi():
    with mock.patch.object(direction.tariff_direction_info_provider, 'find', autospec=True) as m_find_tdi:
        yield m_find_tdi


@pytest.fixture
def m_timed_executor():
    with mock.patch.object(storage_timed_execute, 'wait_for_future_and_build_info', autospec=True) as m_timed_executor:
        yield m_timed_executor


@pytest.fixture
def m_find_train_info_provider():
    with mock.patch.object(direction.train_info_provider, 'find', autospec=True) as m_find_train_info_provider:
        yield m_find_train_info_provider


@pytest.fixture
def m_find_segments():
    with mock.patch.object(direction.schedule_cache, 'find_segments', autospec=True) as m_find_segments:
        yield m_find_segments


@pytest.fixture
def m_geobase():
    with mock.patch.object(optimal_direction_provider, '_geobase') as m_geobase:
        m_geobase.region_by_id.side_effect = CITY_MOCKS.__getitem__
        m_geobase.find_country_id.return_value = 225
        yield m_geobase


@contextmanager
def using_precache():
    with schedule_cache.using_precache(), \
        optimal_direction_provider.using_precache(), \
        train_city_provider.using_precache(), \
        express_system_provider.using_precache():
            yield


_DefaultQuery = namedtuple('_DefaultQuery', 'query settlement departure_station arrival_station')

# distance(CITY1, CITY2) ~= 31 km
CITY1 = {'latitude': 57.578222, 'longitude': 60.201135}
CITY2 = {'latitude': 57.574559, 'longitude': 59.686239}

CITY_MOCKS = {
    11: mock.Mock(type=6, **CITY1),
    12: mock.Mock(type=6, **CITY2),
    10: mock.Mock(type=6, latitude=1, longitude=1),
}


@pytest.fixture
@transaction_fixture
def default_query(request, fixed_now):
    settlement = create_settlement(title='some_settlement')
    departure_station = create_station(title='some_departure_station', settlement=settlement, country=Country.RUSSIA_ID)
    arrival_station = create_station(title='some_arrival_station', settlement=settlement, country=Country.RUSSIA_ID)
    return _DefaultQuery(
        query={
            'departure_point_key': departure_station.point_key,
            'arrival_point_key': arrival_station.point_key,
            'departure_date': '2000-01-01',
            'tld': 'ru'
        },
        settlement=settlement,
        departure_station=departure_station,
        arrival_station=arrival_station
    )


@pytest.mark.dbuser
def test_can_not_find_segments(m_find_train_info_provider, m_find_tdi, m_find_segments, default_query):
    m_find_train_info_provider.return_value = ()
    m_find_tdi.return_value = ((), TariffDirectionUpdatedInfo(()))
    m_find_segments.return_value = ()
    response = Client().get('/searcher/api/direction/', dict(default_query.query))
    assert response.status_code == 200
    response = response.json()
    assert response['error_code'] == 204
    assert response['error'] == 'Empty result'


@pytest.mark.dbuser
@pytest.mark.parametrize('segments_count, expected', (
    (1, {'is_the_fastest': [False], 'is_the_cheapest': [False]}),
    (2, {'is_the_fastest': [True, False], 'is_the_cheapest': [True, False]}),
))
#@mock.patch.object(default_executor, 'wait_for_future_and_build_info')
def test_format(segments_count, expected, m_find_train_info_provider, m_find_tdi, m_timed_executor,  m_find_segments, default_query):
    facility_provider.build_cache()
    query = default_query.query
    query['facility'] = 'WIFI'
    arrival_station = default_query.arrival_station
    departure_station = default_query.departure_station
    deluxe_train = create_deluxe_train(numbers='some_number', title_ru='some_deluxe_train', title='some_deluxe_train')

    m_find_train_info_provider.return_value = 'some_future', {}
    m_find_tdi.return_value = 'some_future', {}
    train_info_result = (
        TrainInfoModel(
            departure_at=datetime(2000, 1, 1, 3, 4),
            number='some_number',
            electronic_ticket=False,
            facilities_ids=[
                Facility.WIFI_ID, Facility.EATING_ID
            ],
            updated_at=datetime(2000, 1, 1)
        ),
    )

    tdi_info_result = (
        (
            TariffDirectionInfo(
                arrival_dt='2000-06-07T12:09:00+04:00',
                arrival_station_id=arrival_station.id,
                departure_dt='2000-01-01T06:04:00+03:00',
                departure_station_id=departure_station.id,
                number="some_number",
                display_number="сом_нумбер",
                has_dynamic_pricing=True,
                two_storey=False,
                is_suburban=True,
                coach_owners=['ФПК'],
                title_dict={},
                electronic_ticket=False,
                first_country_code='UA',
                last_country_code='RU',
                places=(Place(
                    coach_type='cool_type',
                    count=3,
                    max_seats_in_the_same_car=2,
                    price=Price(100, 'RUB'),
                    price_details={
                        'service_price': '100',
                        'ticket_price': '500',
                        'fee': '50',
                        'several_prices': False,
                    },
                    service_class='2Л',
                ),),
                broken_classes={'bad_type': [9, 10]},
                provider='P1',
                raw_train_name='Жигули',
            ),
        ), TariffDirectionUpdatedInfo((TariffDirectionUpdatedInfoRecord(
            datetime(2000, 1, 1, 3, 4, tzinfo=pytz.UTC),
            datetime(2005, 6, 7, 8, 9, tzinfo=pytz.UTC),
            datetime(2100, 1, 2, 3, 4, tzinfo=pytz.UTC)
        ),))
    )

    m_timed_executor.side_effect = [tdi_info_result, train_info_result]

    one_segment = mock.Mock(**{
        'thread.number': 'some_number',
        'thread.title': Translations(**{lang: 'title' for lang in settings.MODEL_LANGUAGES}),
        'thread.type.code': 'basic',
        'departure_dt': datetime(2000, 1, 1, 3, 4, tzinfo=pytz.UTC),
        'departure_station': departure_station,
        'arrival_dt': datetime(2000, 6, 7, 8, 9, tzinfo=pytz.UTC),
        'arrival_station': arrival_station,
    })
    m_find_segments.return_value = [one_segment] * segments_count

    response = Client().get('/searcher/api/direction/', dict(query))
    assert response.status_code == 200

    expected_search_path = '/some_departure_station--some_arrival_station/?when=2000-01-01'
    expected_order_query = (
        '?fromName=some_departure_station'
        '&fromId={departure_point}'
        '&toName=some_arrival_station'
        '&toId={arrival_point}'
        '&when=2000-01-01'
        '&transportType=train&number=some_number&time=06:04'.format(
            departure_point=departure_station.point_key,
            arrival_point=arrival_station.point_key
        ) +
        '&provider=P1'
    )
    expected_segments = [{
        'arrival': {
            'station': {
                'key': arrival_station.point_key,
                'title': 'some_arrival_station'
            },
            'settlement': {
                'key': default_query.settlement.point_key,
                'title': 'some_settlement'
            },
            'local_datetime': {
                'timezone': 'Europe/Moscow',
                'value': '2000-06-07T12:09:00+04:00'
            }
        },
        'departure': {
            'station': {
                'key': departure_station.point_key,
                'title': 'some_departure_station'
            },
            'settlement': {
                'key': default_query.settlement.point_key,
                'title': 'some_settlement'
            },
            'local_datetime': {
                'timezone': 'Europe/Moscow',
                'value': '2000-01-01T06:04:00+03:00'
            }
        },
        'duration': 227825.0,
        'facilities': ['WIFI', 'EAT'],
        'is_the_fastest': expected['is_the_fastest'][i],
        'is_the_cheapest': expected['is_the_cheapest'][i],
        'minimum_price': {'currency': 'RUB', 'value': 100},
        'order_touch_url': 'https://travel.yandex.ru/trains/order/' + expected_order_query,
        'order_url': 'https://travel.yandex.ru/trains/order/' + expected_order_query,
        'places': {
            'records': [
                {
                    'count': 3,
                    'max_seats_in_the_same_car': 2,
                    'price': {
                        'currency': 'RUB', 'value': 100,
                    },
                    'price_details': {
                        'service_price': '100',
                        'ticket_price': '500',
                        'fee': '50',
                        'several_prices': False,
                    },
                    'coach_type': 'cool_type',
                    'service_class': '2Л',
                }
            ],
            'electronic_ticket': False,
            'updated_at': {
                'timezone': 'UTC',
                'value': '2100-01-02T03:04:00+00:00'
            }
        },
        'broken_classes': {'bad_type': [9, 10]},
        'train': {
            'brand': {
                'is_deluxe': False,
                'id': deluxe_train.id,
                'is_high_speed': False,
                'title': 'some_deluxe_train',
                'short_title': '«some_deluxe_train»',
            },
            'number': 'some_number',
            'title': 'title',
            'display_number': 'сом_нумбер',
            'has_dynamic_pricing': True,
            'two_storey': False,
            'is_suburban': True,
            'coach_owners': ['ФПК'],
            'thread_type': 'basic',
            'first_country_code': 'UA',
            'last_country_code': 'RU',
            'provider': 'P1',
            'raw_train_name': 'Жигули',
        },
    } for i in xrange(segments_count)]

    assert response.json() == {
        'found_departure_date': '2000-01-01',
        'minimum_price': {'currency': 'RUB', 'value': 100},
        'minimum_duration': 227825.0,
        'path_items': [
            {
                'text': 'travel.yandex.ru/trains/',
                'touch_url': 'https://travel.yandex.ru/trains/',
                'url': 'https://travel.yandex.ru/trains/',
            },
            {
                'text': 'Билеты на поезд из some_departure_station в some_arrival_station',
                'touch_url': 'https://travel.yandex.ru/trains' + expected_search_path,
                'url': 'https://travel.yandex.ru/trains' + expected_search_path,
            }
        ],
        'query': {
            'departure_point': {
                'key': departure_station.point_key,
                'title': 'some_departure_station'
            },
            'original_departure_point': None,
            'departure_date': '2000-01-01',
            'order_by': 'best',
            'arrival_point': {
                'key': arrival_station.point_key,
                'title': 'some_arrival_station'
            },
            'original_arrival_point': None,
            'language': 'ru'
        },
        'segments': expected_segments,
        'total': segments_count,
        'search_touch_url': 'https://travel.yandex.ru/trains' + expected_search_path,
        'search_url': 'https://travel.yandex.ru/trains' + expected_search_path,
        'search_props': {
            'train_common_wizard': '0',
            'train_pp_wizard': '1',
            'train_wizard_api_timeout': 0,
            'train_wizard_type': 'pp',
        },
        'title': {u'__hl': 'some_departure_station — some_arrival_station: билеты на поезд, 1 января'},
        'filters': {
            'place_count': [
                {'available': True, 'selected': False, 'value': 1},
                {'available': True, 'selected': False, 'value': 2},
                {'available': False, 'selected': False, 'value': 3},
                {'available': False, 'selected': False, 'value': 4}],
            'price': [
                {'available': True, 'selected': False, 'value': '-1000'},
                {'available': False, 'selected': False, 'value': '1000-2000'},
                {'available': False, 'selected': False, 'value': '2000-3000'},
                {'available': False, 'selected': False, 'value': '3000-'}
            ],
            'arrival_time': [
                {'available': False, 'selected': False, 'value': '00:00-06:00'},
                {'available': False, 'selected': False, 'value': '06:00-12:00'},
                {'available': True, 'selected': False, 'value': '12:00-18:00'},
                {'available': False, 'selected': False, 'value': '18:00-24:00'}
            ],
            'brand': [
                {
                    'available': True,
                    'is_high_speed': False,
                    'minimum_price': {'currency': 'RUB', 'value': 100},
                    'selected': False,
                    'value': deluxe_train.id,
                    'title': 'some_deluxe_train',
                }
            ],
            'departure_time': [
                {'available': False, 'selected': False, 'value': '00:00-06:00'},
                {'available': True, 'selected': False, 'value': '06:00-12:00'},
                {'available': False, 'selected': False, 'value': '12:00-18:00'},
                {'available': False, 'selected': False, 'value': '18:00-24:00'}],
            'coach_type': [
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'common'},
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'compartment'},
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'platzkarte'},
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'sitting'},
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'soft'},
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'suite'},
                {'available': False, 'selected': False, 'minimum_price': None, 'value': 'unknown'}
            ],
            'facility': [
                {'available': False,
                 'selected': False,
                 'value': 'BED'},
                {'available': False,
                 'selected': False,
                 'value': 'COND'},
                {'available': True,
                 'selected': False,
                 'value': 'EAT'},
                {'available': False,
                 'selected': False,
                 'value': 'PAP'},
                {'available': False,
                 'selected': False,
                 'value': 'SAN'},
                {'available': False,
                 'selected': False,
                 'value': 'TRAN'},
                {'available': False,
                 'selected': False,
                 'value': 'TV'},
                {'available': True,
                 'selected': True,
                 'value': 'WIFI'},
                {'available': False,
                 'selected': False,
                 'value': 'nearToilet'},
                {'available': False,
                 'selected': False,
                 'value': 'side'},
                {'available': False,
                 'selected': False,
                 'value': 'upper'}
            ]
        }
    }


@pytest.mark.dbuser
def test_in_radius_search_for_experimental_flags(m_geobase):
    SOME_DEPARTURE_GEOID = 12
    SOME_ARRIVAL_GEOID = 10

    departure_settlement = create_settlement(
        title='some_departure_settlement',
        _geo_id=11,
        country=Country.RUSSIA_ID,
        **CITY1
    )
    departure_settlement_30km = create_settlement(
        title='some_departure_settlement',
        _geo_id=SOME_DEPARTURE_GEOID,
        country=Country.RUSSIA_ID,
        **CITY2
    )
    arrival_settlement = create_settlement(
        title='some_arrival_settlement',
        _geo_id=SOME_ARRIVAL_GEOID,
        country=Country.RUSSIA_ID,
    )

    departure_station = create_station(
        title='some_departure_station',
        t_type='train',
        settlement=departure_settlement
    )
    create_station(
        title='some_departure_station',
        t_type='train',
        settlement=departure_settlement_30km
    )
    arrival_station = create_station(
        title='some_arrival_station',
        t_type='train',
        settlement=arrival_settlement
    )
    create_thread(
        schedule_v1=[
            [None, 0, departure_station],
            [SOME_ARRIVAL_GEOID, None, arrival_station],
        ],
        t_type='train',
    )

    url = '/searcher/public-api/direction/'
    query = {
        'departure_settlement_geoid': SOME_DEPARTURE_GEOID,
        'arrival_settlement_geoid': SOME_ARRIVAL_GEOID,
        'exp_flags': ''
    }

    with using_precache():
        departure_point = Settlement.hidden_manager.get(_geo_id=SOME_DEPARTURE_GEOID)
        expected_departure_point = {'key': 'c%d' % departure_point.pk, 'title': departure_point.L_title()}

        response = Client().get(url, dict(query, **{'exp_flags': 'RASPWIZARDS-557'}))
        assert response.status_code == 200

        response = Client().get(url, dict(query, **{'exp_flags': 'RASPWIZARDS-715-50km'}))
        assert response.status_code == 200
        assert response.json()['query']['original_departure_point'] == expected_departure_point
        assert response.json()['query']['original_arrival_point'] is None
        assert response.json()['nearest_points_snippet'] == {
            '__hl': 'Из %s нет рейсов. Ближайший город — %s' % (
                departure_settlement_30km.L_title(case='genitive'),
                departure_settlement.L_title(),
            )
        }

        response = Client().get(url, dict(query, **{'exp_flags': 'RASPWIZARDS-715-20km'}))
        assert response.status_code == 200
        assert response.json()['error_code'] == 204


@pytest.mark.dbuser
def test_green_url_second_city_case(m_geobase):
    departure_settlement = create_settlement(
        title_ru='Архангельск',
        _geo_id=20,
        country=Country.RUSSIA_ID,
    )
    arrival_settlement = create_settlement(
        title_ru='Вологда',
        _geo_id=21,
        country=Country.RUSSIA_ID,
    )
    departure_station = create_station(
        title_ru='some_departure_station',
        t_type='train',
        settlement=departure_settlement
    )
    arrival_station = create_station(
        title_ru='some_arrival_station',
        t_type='train',
        settlement=arrival_settlement
    )
    create_thread(
        schedule_v1=[
            [None, 0, departure_station],
            [10, None, arrival_station],
        ],
        t_type='train',
    )

    url = '/searcher/public-api/direction/'
    query = {
        'departure_settlement_geoid': 20,
        'arrival_settlement_geoid': 21,
    }

    with using_precache():
        response = Client().get(url, dict(query))
        assert response.status_code == 200
        assert response.json()['path_items'][-1]['text'] == 'Билеты на поезд из Архангельска в Вологду'


@pytest.mark.dbuser
def test_filters_in_search_url(m_find_train_info_provider, m_find_tdi, m_timed_executor, m_find_segments, default_query):
    facility_provider.build_cache()
    query = default_query.query
    query['coach_type'] = ['platzkarte', 'compartment']
    arrival_station = default_query.arrival_station
    departure_station = default_query.departure_station
    deluxe_train = create_deluxe_train(numbers='some_number', title_ru='some_deluxe_train', title='some_deluxe_train')
    query['brand'] = [deluxe_train.id]

    m_find_train_info_provider.return_value = 'some_future', {}
    m_find_tdi.return_value = 'some_future', {}
    train_info_result = (
        TrainInfoModel(
            departure_at=datetime(2000, 1, 1, 3, 4),
            number='some_number',
            electronic_ticket=False,
            facilities_ids=[],
            updated_at=datetime(2000, 1, 1)
        ),
    )
    tdi_info_result = (
        (
            TariffDirectionInfo(
                arrival_dt='2000-06-07T12:09:00+04:00',
                arrival_station_id=arrival_station.id,
                departure_dt='2000-01-01T06:04:00+03:00',
                departure_station_id=departure_station.id,
                number="some_number",
                display_number="сом_нумбер",
                has_dynamic_pricing=True,
                two_storey=False,
                is_suburban=True,
                coach_owners=['ФПК'],
                title_dict={},
                electronic_ticket=False,
                first_country_code='UA',
                last_country_code='RU',
                places=set(),
                broken_classes={},
                provider='P1',
                raw_train_name='Жигули',
            ),
        ), TariffDirectionUpdatedInfo((TariffDirectionUpdatedInfoRecord(
            datetime(2000, 1, 1, 3, 4, tzinfo=pytz.UTC),
            datetime(2005, 6, 7, 8, 9, tzinfo=pytz.UTC),
            datetime(2100, 1, 2, 3, 4, tzinfo=pytz.UTC)
        ),))
    )
    m_timed_executor.side_effect = [tdi_info_result, train_info_result]
    m_find_segments.return_value = (
        mock.Mock(**{
            'thread.number': 'some_number',
            'thread.title': Translations(**{lang: 'title' for lang in settings.MODEL_LANGUAGES}),
            'thread.type.code': 'basic',
            'departure_dt': datetime(2000, 1, 1, 3, 4, tzinfo=pytz.UTC),
            'departure_station': departure_station,
            'arrival_dt': datetime(2000, 6, 7, 8, 9, tzinfo=pytz.UTC),
            'arrival_station': arrival_station,
        }),
    )

    url = '/searcher/public-api/direction/'
    travel_base_path = 'https://travel.yandex.ru/trains/'
    expected_search_url = '{}{}--{}/'.format(
        travel_base_path, 'some_departure_station', 'some_arrival_station'
    )
    query_string = '?{}'.format('&'.join([
        'when={}'.format('2000-01-01'),
        'highSpeedTrain={}'.format(deluxe_train.id),
        'trainTariffClass=compartment&trainTariffClass=platzkarte',
    ]))

    response = Client().get(url, dict(query)).json()
    assert response['search_url'] == expected_search_url + query_string
    assert response['search_touch_url'] == expected_search_url + query_string


@pytest.mark.dbuser
@pytest.mark.parametrize('transport, tickets, expected', (
    (None, None, 400),
    ('train', None, 400),
    (None, 1, 400),
    ('another_transport', 1, 400),
    ('train', 0, 400),
    ('train', 1, 200),
))
def test_format_pointless_query_parameters(transport, tickets, expected):
    settlement = create_settlement(
        title='some_settlement',
        _geo_id=11111111,
        country=Country.RUSSIA_ID,
    )
    create_station(title='some_station', settlement=settlement, country=Country.RUSSIA_ID)
    query = {
        'exp_flags': 'RASPWIZARDS-704',
    }
    if transport is not None:
        query.update({
            'transport': transport,
        })
    if tickets is not None:
        query.update({
            'tickets': tickets,
        })
    response = Client().get('/searcher/api/direction/', dict(query)).json()

    if expected == 200:
        assert 'error' not in response
    else:
        assert 'error' in response
        assert response['error_code'] == expected


@pytest.mark.dbuser
@pytest.mark.parametrize('points, query_params, expected_title, expected_query_departure_date, expected_path_item_text', (
    (
        ['departure_point'],
        {'departure_settlement_geoid': '11111111', 'departure_date': '2000-01-01'},
        {u'__hl': 'Билеты на поезд из some_settlement на Яндекс.Путешествиях, 1\xa0января'},
        '2000-01-01',
        'Билеты на поезд из some_settlement',
    ),
    (
        ['departure_point'],
        {'geo_id': '11111111'},
        {u'__hl': 'Билеты на поезд из some_settlement на Яндекс.Путешествиях, 2\xa0января'},
        None,
        'Билеты на поезд из some_settlement',
    ),
    (
        ['arrival_point'],
        {'arrival_settlement_geoid': '11111111'},
        {u'__hl': 'Билеты на поезд в some_settlement на Яндекс.Путешествиях, 2\xa0января'},
        None,
        'Билеты на поезд в some_settlement',
    ),
    (
        {},
        {},
        {u'__hl': 'Билеты на поезд на Яндекс.Путешествиях, 2\xa0января'},
        None,
        'Билеты на поезд',
    ),
))
def test_format_pointless_query(points, query_params, expected_title, expected_query_departure_date, expected_path_item_text, fixed_now):
    settlement = create_settlement(
        title='some_settlement',
        _geo_id=11111111,
        country=Country.RUSSIA_ID,
    )
    create_station(title='some_station', settlement=settlement, country=Country.RUSSIA_ID)
    query = {
        'tld': 'ru',
        'exp_flags': 'RASPWIZARDS-704',
        'transport': 'train',
        'tickets': 1,
    }
    query.update(query_params)

    if 'departure_date' in query:
        mock.patch(
            'travel.rasp.wizards.train_wizard_api.serialization.direction.parse_date',
            return_value=utils_parse_date('2000-01-01')
        )

    response = Client().get('/searcher/api/direction/', dict(query))

    assert response.status_code == 200
    response = response.json()
    assert response['found_departure_date'] == expected_query_departure_date if expected_query_departure_date is not None \
        else '2000-01-02'
    assert response['path_items'] == [
        {
            'text': 'Яндекс.Путешествия',
            'touch_url': 'https://travel.yandex.ru/trains/',
            'url': 'https://travel.yandex.ru/trains/',
        },
        {
            'text': expected_path_item_text,
            'touch_url': 'https://travel.yandex.ru/trains/',
            'url': 'https://travel.yandex.ru/trains/',
        },
    ]
    assert response['title'] == expected_title
    settlement_point = {
        'key': settlement.point_key,
        'title': 'some_settlement'
    }
    expected_response_query = {
        'arrival_point': None,
        'original_arrival_point': None,
        'departure_date': expected_query_departure_date,
        'departure_point': settlement_point if 'departure_point' in points else None,
        'original_departure_point': None,
        'language': 'ru',
        'order_by': 'best'
    }
    for point in points:
        expected_response_query[point] = settlement_point
    assert response['query'] == expected_response_query
    assert 'filters' not in response


@pytest.mark.dbuser
@pytest.mark.parametrize('query_params, expected_title', (
    (
        {'brand', 'departure', 'arrival', 'date'},
        'Сапсаны Москва\xa0— Вологда: билеты на поезд'
    ),
    ({'brand', 'departure', 'arrival', 'date'}, 'Сапсаны Москва\xa0— Вологда: билеты на поезд'),
    ({'brand', 'departure', 'date', 'is_pointless'}, 'Билеты на поезд Сапсаны из Москвы на Яндекс.Путешествиях'),
    ({'brand', 'arrival', 'date', 'is_pointless'}, 'Билеты на поезд Сапсаны в Вологду на Яндекс.Путешествиях'),
    ({'brand', 'date', 'is_pointless'}, 'Билеты на поезд Сапсаны на Яндекс.Путешествиях'),
    ({'departure', 'arrival', 'date'}, 'Москва\xa0— Вологда: билеты на поезд, 1 января 2000'),
    ({'departure', 'arrival'}, 'Москва\xa0— Вологда: билеты на поезд'),
    ({'departure', 'date', 'date', 'is_pointless'}, 'Билеты на поезд из Москвы на Яндекс.Путешествиях, 1 января 2000'),
    ({'arrival', 'date', 'date', 'is_pointless'}, 'Билеты на поезд в Вологду на Яндекс.Путешествиях, 1 января 2000'),
    ({'date', 'date', 'is_pointless'}, 'Билеты на поезд на Яндекс.Путешествиях, 1 января 2000'),
))
def test__get_texts(query_params, expected_title, rur):
    settlement = create_settlement(title='some_settlement')
    departure_station = create_station(title='Москва', title_ru_genitive='Москвы', settlement=settlement, country=Country.RUSSIA_ID)
    arrival_station = create_station(title='Вологда', title_ru_accusative='Вологду', settlement=settlement, country=Country.RUSSIA_ID)

    departure_point, arrival_point, departure_date, is_brand, brand_title, minimum_price, is_pointless, experiments = \
        None, None, None, False, None, None, False, []
    if 'brand' in query_params:
        brand_title = 'Сапсаны'
        is_brand = True
    if 'departure' in query_params:
        departure_point = departure_station
    if 'arrival' in query_params:
        arrival_point = arrival_station
    if 'date' in query_params:
        departure_date = date(2000, 1, 1)
    if 'is_pointless' in query_params:
        is_pointless = True

    assert direction._get_texts(
        departure_point, arrival_point, departure_date, is_brand, brand_title, is_pointless, experiments
    )['title']['__hl'] == expected_title
