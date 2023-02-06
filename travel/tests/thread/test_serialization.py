# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime

import pytest
from hamcrest import assert_that, contains, has_entries

from common.models.geo import Country, StationType
from common.models.schedule import RThreadType
from common.models.transport import TransportType
from common.tester.factories import (
    create_thread, create_station, create_company, create_settlement, create_deluxe_train
)
from travel.rasp.morda_backend.morda_backend.thread.data_layer import ThreadSegment, UnitedThreadSegment, RelatedThread
from travel.rasp.morda_backend.morda_backend.thread.serialization import ThreadContextQuerySchema, ResponseSchema


pytestmark = pytest.mark.dbuser


def test_thread_response_schema():
    settlement_from = create_settlement(id=66, title='settlement_66')
    russia = Country.objects.get(id=Country.RUSSIA_ID)
    station_from = create_station(title='from', id=666, slug='s666', hidden=True, settlement=settlement_from, country=russia)
    station_to = create_station(title='to', id=667, slug='s667')
    company = create_company(title='название', address='адрес', url='сайт', email='мыло', phone='телефон',
                             description='описание', hidden=True, strange=False)

    thread = create_thread(
        uid='123',
        canonical_uid='T_123',
        number='456',
        title='нитка',
        company=company,
        t_type=TransportType.WATER_ID,
        year_days=[datetime(2018, 11, 5), datetime(2018, 11, 20)],
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ]
    )
    thread.run_days_text = 'дни хождения'
    thread.is_no_change_wagon = False

    rts_from = thread.path[0]
    rts_from.platform = 'платформа'
    rts_from.is_technical_stop = True
    rts_from.save()

    thread.full_title = 'Теплоход 456 нитка'
    thread.tariffs_keys = ['tariff_key_1', 'tariff_key_2']
    thread.capitals = [{
        'slug': 'moscow',
        'time_zone': 'Europe/Moscow',
        'title_genitive': 'Москвы',
        'title': 'Москва',
        'abbr_title': 'МСК',
    }]

    thread.other_today_threads = [{
        'uid': '123',
        'title': 'нитка',
        'start_departure_time': '02:00',
        'stop_arrival_time': '22:00',
        'departure_dt': '2018-11-05T02:00:00+03:00'
    }]

    path = list(thread.path)
    path[0].is_station_from = True
    path[0].is_station_to = False
    path[0].capital_time_offset = '+01:00'

    result, errors = ResponseSchema().dump({
        'thread': thread,
        'rtstations': path
    })

    assert not errors
    assert_that(result, has_entries({
        'thread': has_entries({
            'daysText': 'дни хождения',
            'company': has_entries({
                'url': 'сайт',
                'phone': 'телефон',
                'address': 'адрес',
                'email': 'мыло',
                'title': 'название',
                'description': 'описание',
                'hidden': True,
                'strange': False
            }),
            'number': '456',
            'uid': '123',
            'canonicalUid': 'T_123',
            'fullTitle': 'Теплоход 456 нитка',
            'title': 'нитка',
            'transportType': 'water',
            'tariffsKeys': ['tariff_key_1', 'tariff_key_2'],
            'isInterval': False,
            'isNoChangeWagon': False,
            'comment': '',
            'capitals': [{
                'slug': 'moscow',
                'timeZone': 'Europe/Moscow',
                'titleGenitive': 'Москвы',
                'title': 'Москва',
                'abbrTitle': 'МСК',
            }],
            'otherTodayThreads': [{
                'uid': '123',
                'title': 'нитка',
                'startDepartureTime': '02:00',
                'stopArrivalTime': '22:00',
                'departureDt': '2018-11-05T02:00:00+03:00'
            }]
        }),
        'rtstations': contains(
            has_entries({
                'url': '/station/666',
                'platform': 'платформа',
                'timezone': 'Europe/Moscow',
                'id': 666,
                'slug': 's666',
                'title': 'from',
                'isFuzzy': False,
                'isTechnicalStop': True,
                'hidden': True,
                'isNoStop': False,
                'isCombined': False,
                'isStationFrom': True,
                'isStationTo': False,
                'capitalTimeOffset': '+01:00',
                'settlement': {
                    'id': 66,
                    'title': 'settlement_66'
                },
                'country': {
                    'id': 225,
                    'code': 'RU',
                    'title': 'Россия'
                }
            }),
            has_entries({
                'url': '/station/667',
                'platform': None,
                'timezone': 'Europe/Moscow',
                'id': 667,
                'slug': 's667',
                'title': 'to',
                'isFuzzy': False,
                'isTechnicalStop': False,
                'hidden': False,
                'isNoStop': False,
                'isCombined': False,
                'settlement': {},
                'country': {}
            })
        )
    }))


def test_thread_response_schema_suburban():
    station_from = create_station(title='from', id=555, station_type_id=StationType.AIRPORT_ID)
    station_no_stop = create_station(title='no_stop', id=666)
    station_is_combined = create_station(title='is_combined', id=667)
    station_to = create_station(title='to', id=777, station_type_id=StationType.PLATFORM_ID)

    thread = create_thread(
        uid='123',
        canonical_uid='T_123',
        number='456',
        title='нитка',
        t_type=TransportType.SUBURBAN_ID,
        schedule_v1=[
            [None, 0, station_from],
            [15, 20, station_is_combined, {'is_combined': True}],
            [30, 30, station_no_stop],  # (arrival == departure) => no_stop
            [60, None, station_to],
        ]
    )
    thread.run_days_text = 'дни хождения'

    result, errors = ResponseSchema().dump({
        'thread': thread,
        'rtstations': thread.path
    })

    assert not errors
    assert_that(result, has_entries({
        'rtstations': contains(
            has_entries({
                'id': station_from.id,
                'title': station_from.L_title_with_railway_prefix(),
                'isNoStop': False,
                'isCombined': False,
            }),
            has_entries({
                'id': station_is_combined.id,
                'title': station_is_combined.L_title_with_railway_prefix(),
                'isNoStop': False,
                'isCombined': True,
            }),
            has_entries({
                'id': station_no_stop.id,
                'title': station_no_stop.L_title_with_railway_prefix(),
                'isNoStop': True,
                'isCombined': False,
            }),
            has_entries({
                'id': station_to.id,
                'title': station_to.L_title_with_railway_prefix(),
                'isNoStop': False,
                'isCombined': False,
            }),
        )
    }))


def test_selector_suburban():
    station_from = create_station()
    station_to = create_station()

    thread_1 = create_thread(
        id=1,
        uid='1',
        canonical_uid='T_1',
        title='нитка',
        t_type=TransportType.SUBURBAN_ID,
        type=RThreadType.BASIC_ID,
        schedule_v1=[
            [None, 0, station_from],
            [60, None, station_to],
        ]
    )

    segment_1 = ThreadSegment(thread_1, station_from, station_to)
    segment_1.start_departure_time = '02:00'
    segment_1.stop_arrival_time = '03:00'
    segment_1.first_departure = '2019-05-01'
    segment_1.days_text = 'Дни хождения 1'
    segment_1.current = True
    segment_1.in_context = True

    thread_2 = create_thread(
        id=2,
        uid='2',
        canonical_uid='T_1',
        title='нитка',
        t_type=TransportType.SUBURBAN_ID,
        type=RThreadType.BASIC_ID,
        schedule_v1=[
            [None, 0, station_from],
            [50, None, station_to],
        ]
    )

    segment_2 = ThreadSegment(thread_2, station_from, station_to)
    segment_2.start_departure_time = '02:20'
    segment_2.stop_arrival_time = '03:10'
    segment_2.first_departure = '2019-05-02'
    segment_2.days_text = 'Дни хождения 2'
    segment_2.current = False
    segment_2.in_context = True

    thread_3 = create_thread(
        id=3,
        uid='3',
        canonical_uid='T_1',
        title='нитка',
        t_type=TransportType.SUBURBAN_ID,
        type=RThreadType.CHANGE_ID,
        schedule_v1=[
            [None, 0, station_from],
            [55, None, station_to],
        ]
    )

    segment_3 = ThreadSegment(thread_3, station_from, station_to)
    segment_3.start_departure_time = '02:25'
    segment_3.stop_arrival_time = '03:20'
    segment_3.first_departure = '2019-05-03'
    segment_3.days_text = 'Дни хождения 3'
    segment_3.current = False
    segment_3.in_context = True

    thread_4 = create_thread(
        id=4,
        uid='4',
        canonical_uid='T_1',
        title='нитка',
        t_type=TransportType.SUBURBAN_ID,
        type=RThreadType.CANCEL_ID,
        schedule_v1=[
            [None, 0, station_from],
            [60, None, station_to],
        ]
    )

    segment_4 = ThreadSegment(thread_4, station_from, station_to)
    segment_4.start_departure_time = '02:00'
    segment_4.stop_arrival_time = '03:00'
    segment_4.first_departure = '2019-05-04'
    segment_4.days_text = 'Дни хождения 4'
    segment_4.current = False
    segment_4.in_context = True

    result, errors = ResponseSchema().dump({
        'thread': thread_1,
        'rtstations': thread_1.path,
        'threads': [segment_1, segment_2, segment_3, segment_4]
    })

    assert not errors
    assert 'threads' in result
    assert result['threads'] == {
        1: {
            'uid': '1',
            'title': 'нитка',
            'startDepartureTime': '02:00',
            'stopArrivalTime': '03:00',
            'firstDeparture': '2019-05-01',
            'daysText': 'Дни хождения 1',
            'type': 'basic',
            'current': True,
            'inContext': True
        },
        2: {
            'uid': '2',
            'title': 'нитка',
            'startDepartureTime': '02:20',
            'stopArrivalTime': '03:10',
            'firstDeparture': '2019-05-02',
            'daysText': 'Дни хождения 2',
            'type': 'basic',
            'current': False,
            'inContext': True
        },
        3: {
            'uid': '3',
            'title': 'нитка',
            'startDepartureTime': '02:25',
            'stopArrivalTime': '03:20',
            'firstDeparture': '2019-05-03',
            'daysText': 'Дни хождения 3',
            'type': 'change',
            'current': False,
            'inContext': True
        },
        4: {
            'uid': '4',
            'title': 'нитка',
            'startDepartureTime': '02:00',
            'stopArrivalTime': '03:00',
            'firstDeparture': '2019-05-04',
            'daysText': 'Дни хождения 4',
            'type': 'cancel',
            'current': False,
            'inContext': True
        }
    }


def test_selector_train():
    tumen = create_station()
    ekb = create_station()
    perm = create_station()
    thread_1 = create_thread(
        id=1,
        uid='1',
        title='Тюмень-Пермь',
        t_type=TransportType.TRAIN_ID,
        type=RThreadType.BASIC_ID,
        schedule_v1=[
            [None, 0, tumen],
            [290, 300, ekb],
            [600, None, perm],
        ]
    )

    segment_1 = UnitedThreadSegment(('Тюмень-Пермь', '05:00', '10:00', True, True, 'basic'))
    segment_1.id = 1
    segment_1.uid = '1'
    segment_1.first_departure = '2019-05-01'
    segment_1.days_text = 'Дни хождения 1'
    segment_1.current = True

    segment_2 = UnitedThreadSegment(('Тюмень-Пермь', '06:00', '10:00', True, True, 'basic'))
    segment_2.id = 2
    segment_2.uid = '2'
    segment_2.first_departure = '2019-05-02'
    segment_2.days_text = 'Дни хождения 2'
    segment_2.current = False

    segment_3 = UnitedThreadSegment(('Тюмень-Екб', '01:00', '05:00', False, True, 'basic'))
    segment_3.id = 3
    segment_3.uid = '3'
    segment_3.first_departure = '2019-05-03'
    segment_3.days_text = 'Дни хождения 3'
    segment_3.current = False

    thread_4 = create_thread(
        id=4,
        uid='4',
        title='Екб-Пермь',
        t_type=TransportType.TRAIN_ID,
        type=RThreadType.BASIC_ID,
        schedule_v1=[
            [None, 0, ekb],
            [300, None, perm],
        ]
    )

    segment_4 = ThreadSegment(thread_4, ekb, perm)
    segment_4.start_departure_time = '02:00'
    segment_4.stop_arrival_time = '07:00'
    segment_4.first_departure = '2019-05-04'
    segment_4.days_text = 'Дни хождения 4'
    segment_4.current = False
    segment_4.in_context = True

    result, errors = ResponseSchema().dump({
        'thread': thread_1,
        'rtstations': thread_1.path,
        'threads': [segment_1, segment_2, segment_3, segment_4]
    })

    assert not errors
    assert 'threads' in result
    assert result['threads'] == {
        1: {
            'uid': '1',
            'title': 'Тюмень-Пермь',
            'startDepartureTime': '05:00',
            'stopArrivalTime': '10:00',
            'firstDeparture': '2019-05-01',
            'daysText': 'Дни хождения 1',
            'type': 'basic',
            'current': True,
            'inContext': True
        },
        2: {
            'uid': '2',
            'title': 'Тюмень-Пермь',
            'startDepartureTime': '06:00',
            'stopArrivalTime': '10:00',
            'firstDeparture': '2019-05-02',
            'daysText': 'Дни хождения 2',
            'type': 'basic',
            'current': False,
            'inContext': True
        },
        3: {
            'uid': '3',
            'title': 'Тюмень-Екб',
            'startDepartureTime': '01:00',
            'stopArrivalTime': '05:00',
            'firstDeparture': '2019-05-03',
            'daysText': 'Дни хождения 3',
            'type': 'basic',
            'current': False,
            'inContext': False
        },
        4: {
            'uid': '4',
            'title': 'Екб-Пермь',
            'startDepartureTime': '02:00',
            'stopArrivalTime': '07:00',
            'firstDeparture': '2019-05-04',
            'daysText': 'Дни хождения 4',
            'type': 'basic',
            'current': False,
            'inContext': True
        }
    }


def test_deluxe_train():
    thread1 = create_thread(t_type=TransportType.TRAIN_ID, number='1111')
    thread2 = create_thread(t_type=TransportType.TRAIN_ID, number='2222')
    deluxe1 = create_deluxe_train(deluxe=False, title_ru='Тарахтелка', numbers='1111')
    deluxe2 = create_deluxe_train(deluxe=True, title_ru='Фирмач', numbers='2222')
    thread1.deluxe = deluxe1
    thread2.deluxe = deluxe2

    result, errors = ResponseSchema().dump({'thread': thread1})
    assert_that(result, has_entries({
        'thread': has_entries({
            'deluxeTrain': has_entries({
                'isDeluxe': False,
                'title': 'Тарахтелка',
                'shortTitle': '«Тарахтелка»'
            })
        })
    }))

    result, errors = ResponseSchema().dump({'thread': thread2})
    assert_that(result, has_entries({
        'thread': has_entries({
            'deluxeTrain': has_entries({
                'isDeluxe': True,
                'title': 'Фирмач',
                'shortTitle': 'фирменный «Фирмач»'
            })
        })
    }))


def test_no_change_wagons():
    thread_1 = create_thread(
        canonical_uid='R_1',
        title='Базовый поезд',
        t_type=TransportType.TRAIN_ID,
        type=RThreadType.BASIC_ID,
    )
    thread_1.is_no_change_wagon = False
    thread_2 = create_thread(
        canonical_uid='R_2',
        title='Вагон',
        t_type=TransportType.TRAIN_ID,
        type=RThreadType.THROUGH_TRAIN_ID,
    )
    thread_2.is_no_change_wagon = True

    base_train = RelatedThread('R_1', 'Базовый поезд', RelatedThread.BASIC_TRAIN_RELATION)
    wagon2 = RelatedThread('R_2', 'Вагон', RelatedThread.NO_CHANGE_WAGON_RELATION)
    wagon3 = RelatedThread('R_3', 'Другой вагон', RelatedThread.NO_CHANGE_WAGON_RELATION)

    result, errors = ResponseSchema().dump({
        'thread': thread_1,
        'related_threads': [wagon2, wagon3]
    })

    assert_that(result, has_entries({
        'thread': has_entries({
            'isNoChangeWagon': False
        }),
        'relatedThreads': contains(
            has_entries({
                "relationType": RelatedThread.NO_CHANGE_WAGON_RELATION,
                "canonicalUid": "R_2",
                "title": "Вагон"
            }),
            has_entries({
                "relationType": RelatedThread.NO_CHANGE_WAGON_RELATION,
                "canonicalUid": "R_3",
                "title": "Другой вагон"
            }),
        )
    }))

    result, errors = ResponseSchema().dump({
        'thread': thread_2,
        'related_threads': [base_train, wagon3]
    })

    assert_that(result, has_entries({
        'thread': has_entries({
            'isNoChangeWagon': True
        }),
        'relatedThreads': contains(
            has_entries({
                "relationType": RelatedThread.BASIC_TRAIN_RELATION,
                "canonicalUid": "R_1",
                "title": "Базовый поезд"
            }),
            has_entries({
                "relationType": RelatedThread.NO_CHANGE_WAGON_RELATION,
                "canonicalUid": "R_3",
                "title": "Другой вагон"
            }),
        )
    }))


def test_thread_query_schema_departure_from_date():
    mixed_uid = 'R_123_asd'
    station_from = create_station(title='from', id=666)
    departure_from_date = date(2019, 1, 2)
    query_params = {
        'mixed_uid': mixed_uid,
        'departure_from': departure_from_date.isoformat(),
        'station_from': station_from.id
    }

    context, errors = ThreadContextQuerySchema().load(query_params)

    assert errors == {}
    assert context.departure_from_date == departure_from_date
    assert not context.departure_from
