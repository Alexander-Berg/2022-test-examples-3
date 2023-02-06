# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime
from itertools import product
from urllib import urlencode

import pytest
from django.test import Client
from hamcrest import (
    has_entries, has_entry, contains_inanyorder, has_length, has_items, anything, assert_that, is_not
)
from mock import sentinel

from common.apps.train.models import UFSNewOrderBlackList
from common.data_api.platforms.client import get_dynamic_platform_collection
from common.data_api.baris.test_helpers import mock_baris_response
from common.models.factories import create_tariff_type
from common.models.schedule import RThreadType, RThread, TrainPurchaseNumber
from common.models.transport import TransportType, TransportSubtype
from common.tester.factories import (
    create_thread, create_station, create_settlement, create_train_schedule_plan, create_country,
    create_transport_subtype
)
from common.tester.utils.django_cache import clear_cache_until_switch
from common.tester.transaction_context import transaction_fixture
from common.tester.utils.datetime import replace_now
from common.utils.date import RunMask


pytestmark = pytest.mark.dbuser


create_thread = create_thread.mutate(__={'calculate_noderoute': True})


def _get_patch_path(symbol):
    return 'travel.rasp.morda_backend.morda_backend.search.views.{}'.format(symbol)


def make_check(query, code=200, matcher=None, lang='ru'):
    qs = urlencode(query)
    with mock_baris_response({}):
        response = Client().get('/{lang}/search/search/?{qs}'.format(
            lang=lang,
            qs=qs
        ))
    assert response.status_code == code
    if matcher is not None:
        assert_that(json.loads(response.content), matcher)


def build_segment_matchers(uids_by_departure):
    expected_segments = []
    for departure, uids in uids_by_departure.items():
        expected_segments.extend([
            has_entries(number=uid, departure=departure)
            for uid in uids
        ])
    return expected_segments


@pytest.fixture(scope='module')
@transaction_fixture
def segments(request):
    station_from = create_station()
    station_to = create_station()
    threads = []
    for shift, t_type in product(range(1, 11), ['bus', 'train']):
        key = 'basic_{}_{}'.format(t_type, shift)
        threads.append(
            create_thread(
                uid=key,
                number=key,
                t_type=t_type,
                type=RThreadType.BASIC_ID,
                year_days=RunMask.range(datetime(2000, 1, shift), datetime(2000, 1, 15)),
                schedule_v1=[
                    [None, 0, station_from],
                    [10, None, station_to],
                ],
            )
        )
    return station_from, station_to, threads


def assign_plan(thread_uid, plan):
    thread = RThread.objects.get(uid=thread_uid)
    thread.schedule_plan = plan
    thread.save()


@pytest.fixture(scope='module')
@transaction_fixture
def plans(request):
    current_plan = create_train_schedule_plan(
        code='plan_code1',
        start_date=datetime(2000, 1, 1),
        end_date=datetime(2000, 1, 3)
    )
    next_plan = create_train_schedule_plan(
        code='plan_code2',
        start_date=datetime(2000, 1, 4),
        end_date=datetime(2000, 1, 5)
    )
    return current_plan, next_plan


def test_return_errors_if_wrong_context():
    """
    Если поисковый контекст задан неверно, возвращает ошибку и http код 200.
    """
    make_check(
        query={'when': '2016-01-01'},
        matcher=has_entries(
            result={},
            errors=has_entries(
                pointTo=['Missing data for required field.'],
                pointFrom=['Missing data for required field.'],
                _schema=[
                    {'point_to': 'no_such_point', 'point_from': 'no_such_point'}
                ]
            )
        ),
        code=400
    )


@replace_now('2000-01-01 00:00:00')
@pytest.mark.functional
def test_return_today_segments_if_no_when(segments):
    """
    Если when не передан, возвращаем сегменты на все дни.
    """

    station_from, station_to, _ = segments
    expected_segments = [
        has_entry('number', 'basic_{}_{}'.format(t_type, shift))
        for t_type, shift in product(['bus', 'train'], range(1, 11))
    ]

    make_check(
        query={'pointFrom': station_from.point_key, 'pointTo': station_to.point_key},
        matcher=has_entries(
            errors={},
            result=has_entries(
                segments=contains_inanyorder(*expected_segments)
            )
        )
    )


@pytest.mark.functional
def test_return_segments_on_specified_date_if_when_in_place(segments):
    """
    Если when передан, возвращаем сегменты на указанную дату.
    """
    station_from, station_to, _ = segments
    expected_uids_by_departure = {
        '2000-01-01T21:00:00+00:00': [
            'basic_train_1', 'basic_train_2', 'basic_bus_1', 'basic_bus_2'
        ],
        '2000-01-02T21:00:00+00:00': [
            'basic_train_1', 'basic_train_2', 'basic_train_3',
            'basic_bus_1', 'basic_bus_2', 'basic_bus_3'
        ]
    }
    expected_segments = build_segment_matchers(expected_uids_by_departure)

    make_check(
        query={
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'when': '2000-01-02'
        },
        matcher=has_entries(
            errors={},
            result=has_entries(
                segments=contains_inanyorder(*expected_segments)
            )
        )
    )


@pytest.mark.functional
def test_filter_by_transport_type(segments):
    """
    Если передан transportType - фильтруем по нему.
    """
    station_from, station_to, _ = segments
    expected_uids_by_departure = {
        '2000-01-01T21:00:00+00:00': [
            'basic_train_1', 'basic_train_2'
        ],
        '2000-01-02T21:00:00+00:00': [
            'basic_train_1', 'basic_train_2', 'basic_train_3'
        ]
    }
    expected_segments = build_segment_matchers(expected_uids_by_departure)

    make_check(
        query={
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'transportType': 'train',
            'when': '2000-01-02'
        },
        matcher=has_entries(
            errors={},
            result=has_entries(
                segments=contains_inanyorder(*expected_segments)
            )
        )
    )


@replace_now('2019-01-01')
def test_filter_suburbans():
    stations = [create_station(id=i) for i in range(100, 102)]
    t_lastdal_subtype = create_transport_subtype(
        t_type=TransportType.TRAIN_ID,
        code='lastdal',
        title_ru='Ласточка(common)',
        title_suburban_ru='Ласточка',
        use_in_suburban_search=True
    )

    def search(station_from, station_to, all_days=False):
        search_params = {
            'national_version': 'ru',
            'transportType': 'suburban',
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
        }
        if not all_days:
            search_params['when'] = '2019-01-01'

        response = Client().get('/ru/search/search/', search_params)
        return response.data['result']['segments']

    create_thread(t_type=TransportType.TRAIN_ID, number='20t', schedule_v1=[
        [None, 0, stations[0]],
        [60, None, stations[1]]
    ], tz_start_time='08:20')

    #  No train segments in the result
    segments = search(stations[0], stations[1], True)
    assert len(segments) == 0

    create_thread(t_type=TransportType.TRAIN_ID, t_subtype=t_lastdal_subtype, number='30l', schedule_v1=[
        [None, 0, stations[0]],
        [60, None, stations[1]]
    ], tz_start_time='08:30')
    create_thread(t_type=TransportType.SUBURBAN_ID, number="10s", schedule_v1=[
        [None, 0, stations[0]],
        [60, None, stations[1]]
    ], tz_start_time='08:10')

    clear_cache_until_switch()
    for all_days in [True, False]:
        segments = search(stations[0], stations[1], all_days)
        assert len(segments) == 2
        assert_that(segments, has_items(
            has_entries({'number': '10s',
                         'transport': has_entries({'code': 'suburban'})}),
            has_entries({'number': '30l',
                         'thread': has_entries({'isExpress': True}),
                         'transport': has_entries({
                             'code': 'train',
                             'subtype': has_entries({
                                 'code': 'lastdal',
                                 'title': 'Ласточка'
                             })
                         })}),
        ))


@replace_now('2000-01-02 00:00:00')
@pytest.mark.functional
def test_return_10_nearest_segments_if_nearest_specified(segments):
    """
    Если nearest = True - возвращаем 10 ближайших сегментов.
    """
    station_from, station_to, _ = segments
    make_check(
        query={
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
            'nearest': True,
        },
        matcher=has_entries(
            errors={},
            result=has_entries(
                segments=has_length(10)
            )
        )
    )


@replace_now('2000-01-02 00:00:00')
@pytest.mark.functional
def test_current_plan_in_response(plans):
    """Текущий План должен присутствовать в ответе"""
    make_check(
        query={
            'pointFrom': create_settlement().point_key,
            'pointTo': create_settlement().point_key,
            'transportType': 'suburban'
        },
        matcher=has_entries(
            errors={},
            result=has_entries(
                plans=has_entries(
                    current=has_entries(code='plan_code1')
                )
            )
        )
    )


@replace_now('2000-01-01 00:00:00')
@pytest.mark.functional
def test_plan_code_added_to_threads(segments, plans):
    """
    Если у сегмента есть нитка, и у нитки есть план - после сериализации у ниток
    должен быть добавлен план с единственным полем - code.
    """
    station_from, station_to, _ = segments
    current_plan, next_plan = plans

    assign_plan('basic_train_1', next_plan)
    assign_plan('basic_train_2', next_plan)
    assign_plan('basic_bus_1', current_plan)
    assign_plan('basic_bus_2', current_plan)

    make_check(
        query={
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
        },
        matcher=has_entries(
            errors={},
            result=has_entries(
                segments=has_items(
                    has_entry('thread', has_entries(
                        schedulePlanCode=next_plan.code,
                        uid='basic_train_1'
                    )),
                    has_entry('thread', has_entries(
                        schedulePlanCode=next_plan.code,
                        uid='basic_train_2',
                    )),
                    has_entry('thread', has_entries(
                        schedulePlanCode=current_plan.code,
                        uid='basic_bus_1'
                    )),
                    has_entry('thread', has_entries(
                        schedulePlanCode=current_plan.code,
                        uid='basic_bus_2'
                    )),
                )
            )
        )
    )


def test_all_days_tariff_keys():
    tariff_type = create_tariff_type()
    station_from = create_station()
    station_to = create_station()

    thread_bus = create_thread(t_type=TransportType.BUS_ID, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to]
    ])

    thread_suburban = create_thread(t_type=TransportType.SUBURBAN_ID, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to]
    ], number='3333', tariff_type=tariff_type)

    with mock_baris_response({}):
        response = json.loads(Client().get('/ru/search/search/', {
            'national_version': 'ru',
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key
        }).content)

    assert_that(response['result']['segments'], contains_inanyorder(
        has_entries({
            'tariffsKeys': has_items('static {} {} {}'.format(station_from.id, station_to.id, thread_bus.uid))
        }),
        has_entries({
            'tariffsKeys': has_items('static {} {} {}'.format(station_from.id, station_to.id, thread_suburban.uid),
                                     'suburban {} {} {}'.format(station_from.id, station_to.id, tariff_type.id))
        }),
    ))


def test_all_days_tariff_keys_for_suburban_with_train_purchase_numbers():
    tariff_type = create_tariff_type()
    station_from = create_station()
    station_to = create_station()

    subtype = create_transport_subtype(t_type=TransportSubtype.SUBURBAN_ID, code='subt', has_train_tariffs=True)

    thread_train = create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to]
    ], number='120A')

    thread_suburban = create_thread(t_type=TransportType.SUBURBAN_ID, t_subtype=subtype, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to]
    ], number='3333', tariff_type=tariff_type)

    TrainPurchaseNumber.objects.create(thread=thread_suburban, number='120A')

    with mock_baris_response({}):
        response = json.loads(Client().get('/ru/search/search/', {
            'national_version': 'ru',
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key
        }).content)

    assert_that(response['result']['segments'], contains_inanyorder(
        has_entries({
            'tariffsKeys': has_items('static {} {} {}'.format(station_from.id, station_to.id, thread_suburban.uid),
                                     'suburban {} {} {}'.format(station_from.id, station_to.id, tariff_type.id),
                                     thread_train.number,
                                     thread_suburban.number)
        })
    ))


@replace_now('2016-01-01')
@pytest.mark.parametrize('with_date', [True, False])
def test_all_days_train_country_and_station_country(with_date):
    country_from = create_country(code='from')
    country_to = create_country(code='to')

    country_first = create_country(code='fst')
    country_last = create_country(code='last')

    station_from = create_station(country=country_from)
    station_to = create_station(country=country_to)
    station_first = create_station(country=country_first)
    station_last = create_station(country=country_last)

    create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
        [None, 0, station_first],
        [10, 20, station_from],
        [30, 40, station_to],
        [50, None, station_last]
    ], tz_start_time='00:10')

    create_thread(t_type=TransportType.SUBURBAN_ID, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to]
    ], number='3333', tz_start_time='01:20')

    create_thread(t_type=TransportType.BUS_ID, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to]
    ], number='12', tz_start_time='01:23')

    search_params = {
        'national_version': 'ru',
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key
    }

    if with_date:
        search_params['when'] = '2016-01-01'

    with mock_baris_response({}):
        response = json.loads(Client().get('/ru/search/search/', search_params).content)

    assert_that(
        response['result']['segments'][0],
        has_entries({
            'thread': has_entries({
                'firstCountryCode': 'fst',
                'lastCountryCode': 'last',
            }),
            'stationFrom': has_entries(country=has_entries({'id': country_from.id, 'code': 'from'})),
            'stationTo': has_entries(country=has_entries({'id': country_to.id, 'code': 'to'}))
        })
    )

    assert_that(
        response['result']['segments'][1],
        has_entries({
            'thread': has_entries({
                'firstCountryCode': 'from',
                'lastCountryCode': 'to',
            }),
            'stationFrom': has_entries(country=has_entries({'id': country_from.id, 'code': 'from'})),
            'stationTo': has_entries(country=has_entries({'id': country_to.id, 'code': 'to'}))
        })
    )

    assert_that(
        response['result']['segments'][2],
        has_entries({
            'thread': is_not(has_entries({
                'firstCountryCode': anything(),
                'lastCountryCode': anything(),
            })),
            'stationFrom': has_entries(country=has_entries({'id': country_from.id, 'code': 'from'})),
            'stationTo': has_entries(country=has_entries({'id': country_to.id, 'code': 'to'}))
        })
    )


@replace_now('2016-01-01')
@pytest.mark.parametrize('with_date', [True, False])
def test_ufs_order_black_list(with_date):
    station_from = create_station()
    station_to = create_station()

    UFSNewOrderBlackList.objects.create(number='ZZZ1,')

    create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
        [None, 0, station_from],
        [50, None, station_to]
    ], tz_start_time='00:10', number='ZZZ1')

    create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
        [None, 0, station_from],
        [50, None, station_to]
    ], tz_start_time='00:20', number='YYY2')

    search_params = {
        'national_version': 'ru',
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'transportType': 'train'
    }

    if with_date:
        search_params['when'] = '2016-01-01'

    response = json.loads(Client().get('/ru/search/search/', search_params).content)

    assert response['result']['segments'][0].get('oldUfsOrder')
    assert response['result']['segments'][1].get('oldUfsOrder', sentinel.missing) is sentinel.missing


@replace_now('2016-01-01')
def test_segment_stations_express_codes():
    station_from = create_station(__={'codes': {'express': '222222'}})
    station_to = create_station(__={'codes': {'express': '333333'}})

    create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
        [None, 0, station_from],
        [50, None, station_to]
    ], tz_start_time='00:10')

    search_params = {
        'national_version': 'ru',
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'when': 'today',
        'transportType': 'train'
    }

    response = Client().get('/ru/search/search/', search_params)

    assert_that(response.data['result']['segments'][0], has_entries({
        'stationFrom': has_entries(codes={'express': '222222'}),
        'stationTo': has_entries(codes={'express': '333333'})
    }))


@pytest.mark.mongouser
@replace_now('2019-01-01')
def test_segment_station_platforms():
    stations = [create_station(id=i) for i in range(100, 106)]
    train_number = '4321'
    complex_train_number = '9999/{}'.format(train_number)
    platforms_coll = get_dynamic_platform_collection()
    platforms_coll.insert_many([
        {
            'date': '2019-01-01',
            'station_id': stations[1].id,
            'train_number': train_number,
            'departure_platform': 'путь1'
        },
        {
            'date': '2019-01-01',
            'station_id': stations[2].id,
            'train_number': train_number,
            'departure_platform': 'путь2'
        },
        {
            'date': '2019-01-01',
            'station_id': stations[4].id,
            'train_number': train_number,
            'departure_platform': 'путь4'
        },
    ])

    create_thread(t_type=TransportType.SUBURBAN_ID, number=train_number, schedule_v1=[
        [None, 0, stations[0], {'platform': 'пл100'}],
        [15, 20, stations[1], {'platform': 'пл101'}],
        [35, 40, stations[2]],
        [50, None, stations[3], {'platform': 'пл103'}]
    ], tz_start_time='00:10')
    create_thread(t_type=TransportType.BUS_ID, number=train_number, schedule_v1=[
        [None, 0, stations[1], {'platform': 'пл100'}],
        [15, None, stations[2], {'platform': 'пл101'}],
    ], tz_start_time='01:10')
    create_thread(t_type=TransportType.TRAIN_ID, number=complex_train_number, schedule_v1=[
        [None, 0, stations[3], {'platform': 'пл103'}],
        [15, 20, stations[4], {'platform': 'пл104'}],
        [50, None, stations[5], {'platform': 'пл105'}]
    ], tz_start_time='02:10')

    def search(station_from, station_to, all_days=False):
        search_params = {
            'national_version': 'ru',
            'pointFrom': station_from.point_key,
            'pointTo': station_to.point_key,
        }
        if not all_days:
            search_params['when'] = 'today'

        with mock_baris_response({}):
            response = Client().get('/ru/search/search/', search_params)
            return response.data['result']['segments'][0]

    assert_that(search(stations[0], stations[1]), has_entries({
        'stationFrom': has_entries(id=100, platform='пл100'),
        'stationTo': has_entries(id=101, platform='пл101')
    }))

    assert_that(search(stations[1], stations[2]), has_entries({
        'stationFrom': has_entries(id=101, platform='путь1'),
        'stationTo': has_entries(id=102, platform='')
    }))
    assert_that(search(stations[1], stations[2], all_days=True), has_entries({
        'stationFrom': has_entries(id=101, platform='пл101'),
        'stationTo': has_entries(id=102, platform='')
    }))

    assert_that(search(stations[4], stations[5]), has_entries({
        'stationFrom': has_entries(id=104, platform='путь4'),
        'stationTo': has_entries(id=105, platform='пл105')
    }))


@pytest.mark.mongouser
def test_train_purchase_numbers():
    station_from = create_station()
    station_to = create_station()

    subtype = create_transport_subtype(t_type=TransportSubtype.SUBURBAN_ID, code='subt', has_train_tariffs=True)

    create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to]
    ], number='120', tz_start_time='01:20')

    create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to]
    ], number='120A', tz_start_time='01:22')

    suburban_thread = create_thread(t_type=TransportType.SUBURBAN_ID, t_subtype=subtype, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to]
    ], number='6784', tz_start_time='01:23')

    TrainPurchaseNumber.objects.create(thread=suburban_thread, number='120')
    TrainPurchaseNumber.objects.create(thread=suburban_thread, number='120A')

    search_params = {
        'national_version': 'ru',
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key
    }

    with mock_baris_response({}):
        response = json.loads(Client().get('/ru/search/search/', search_params).content)

    segments = response['result']['segments']
    assert_that(segments[0], has_entries('trainPurchaseNumbers', contains_inanyorder('120', '120A')))
