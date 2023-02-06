# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime

import pytest
from django.test import Client
from hamcrest import has_entries, assert_that, contains_inanyorder

from common.apps.archival_data.factories import ArchivalSearchDataFactory, ArchivalSettlementsDataFactory
from common.data_api.baris.test_helpers import mock_baris_response
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station, create_settlement
from common.tester.utils.datetime import replace_now


create_thread = create_thread.mutate(__={'calculate_noderoute': True})
pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def get_response(point_from, point_to, when=None, transport_type=None):
    params = {
        'national_version': 'ru',
        'pointFrom': point_from.point_key,
        'pointTo': point_to.point_key
    }
    if when:
        params['when'] = when

    if transport_type:
        params['transportType'] = transport_type

    with mock_baris_response({}):
        return json.loads(Client().get('/ru/search/search/', params).content)


@replace_now('2020-01-01 12:00:00')
def test_archival_data():
    station_from = create_station(settlement=create_settlement(slug='settlement_slug_from'), slug='slug_from')
    station_to = create_station(settlement=create_settlement(slug='settlement_slug_to'), slug='slug_to')

    response = get_response(station_from, station_to)
    assert response['result']['archivalData'] is None

    ArchivalSearchDataFactory(
        point_from=station_from.point_key,
        point_to=station_to.point_key,
        transport_type=TransportType.TRAIN_ID
    )

    response = get_response(station_from, station_to)
    assert_that(response['result']['archivalData'],
                has_entries({
                    'transportTypes': ['train'],
                    'canonical': has_entries({
                        'pointFrom': station_from.slug,
                        'pointTo': station_to.slug,
                        'transportType': 'train'
                    })
                }))

    ArchivalSearchDataFactory(
        point_from=station_from.point_key,
        point_to=station_to.point_key,
        transport_type=TransportType.BUS_ID
    )

    train_bus_dict = {
        'transportTypes': ['train', 'bus'],
        'canonical': has_entries({
            'pointFrom': station_from.slug,
            'pointTo': station_to.slug,
            'transportType': None
        })
    }

    response = get_response(station_from, station_to)
    assert_that(response['result']['archivalData'],
                has_entries(
                    train_bus_dict
    ))

    response = get_response(station_from, station_to, when='2020-01-01')
    assert_that(response['result']['archivalData'],
                has_entries(
                    train_bus_dict
    ))

    response = get_response(station_from, station_to, transport_type='bus')
    assert_that(response['result']['archivalData'],
                has_entries({
                    'transportTypes': ['bus'],
                    'canonical': has_entries({
                        'transportType': 'bus'
                    })
                }))

    response = get_response(station_from, station_to, transport_type='train')
    assert_that(response['result']['archivalData'],
                has_entries({
                    'transportTypes': ['train'],
                    'canonical': has_entries({
                        'transportType': 'train'
                    })
                }))

    create_thread(t_type=TransportType.BUS_ID, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to]
    ])

    response = get_response(station_from, station_to)
    assert response['result']['archivalData'] is None


@replace_now('2020-01-01 12:00:00')
def test_archival_segments():
    settlement_from = create_settlement(slug='settlement_slug_from')
    settlement_to = create_settlement(slug='settlement_slug_to')
    station_from = create_station(settlement=settlement_from, slug='slug_from')
    station_to = create_station(settlement=settlement_to, slug='slug_to')
    train = TransportType.objects.get(id=TransportType.TRAIN_ID)
    bus = TransportType.objects.get(id=TransportType.BUS_ID)

    response = get_response(settlement_from, settlement_to)
    assert response['result']['archivalData'] is None

    ArchivalSearchDataFactory(
        point_from=settlement_from.point_key,
        point_to=settlement_to.point_key,
        transport_type=train.id
    )

    ArchivalSearchDataFactory(
        point_from=settlement_from.point_key,
        point_to=settlement_to.point_key,
        transport_type=bus.id
    )

    ArchivalSettlementsDataFactory(
        point_from=settlement_from.point_key,
        point_to=settlement_to.point_key,
        transport_type=TransportType.TRAIN_ID,
        segments=[
            {
                'title': 'basic_segment',
                'arrival': datetime(2020, 5, 20, 14),
                'departure': datetime(2020, 5, 20, 13),
                'station_from': {'id': station_from.id},
                'station_to': {'id': station_to.id},
                'transport_type': {
                    'id': train.id,
                    'code': train.code,
                    'subtype': {
                        'id': 5,
                        'color': '#f'
                    }
                },
                'thread': {'number': '123'},
                'run_days_by_tz': {
                    'Europe/Moscow': {
                        'days_text': 'только 21\xa0мая, 10\xa0июня'
                    }
                }
            }
        ]
    )
    ArchivalSettlementsDataFactory(
        point_from=settlement_from.point_key,
        point_to=settlement_to.point_key,
        transport_type=TransportType.BUS_ID,
        segments=[
            {
                'title': 'interval_segment',
                'station_from': {'id': station_from.id},
                'station_to': {'id': station_to.id},
                'transport_type': {
                    'id': bus.id,
                    'code': bus.code,
                },
                'thread': {
                    'number': '456',
                    'density': 'density',
                    'begin_time': '10:00:00',
                    'end_time': '22:00:00',
                },
                'run_days_by_tz': {
                    'Europe/Moscow': {
                        'days_text': 'только 28\xa0мая'
                    }
                }
            }
        ]
    )

    response = get_response(settlement_from, settlement_to)
    assert_that(response['result']['archivalData'],
                has_entries({
                    'transportTypes': ['train', 'bus'],
                    'canonical': has_entries({
                        'pointFrom': settlement_from.slug,
                        'pointTo': settlement_to.slug,
                        'transportType': None,
                    }),
                    'segments': [
                        {
                            'title': 'basic_segment',
                            'arrival': '2020-05-20T14:00:00+00:00',
                            'departure': '2020-05-20T13:00:00+00:00',
                            'stationFrom': {'id': station_from.id},
                            'stationTo': {'id': station_to.id},
                            'transport': {
                                'id': train.id,
                                'code': train.code,
                                'subtype': {
                                    'id': 5,
                                    'titleColor': '#f'
                                }
                            },
                            'thread': {'number': '123'},
                            'daysByTimezone': {
                                'Europe/Moscow': {
                                    'text': 'только 21\xa0мая, 10\xa0июня'
                                }
                            }
                        },
                        {
                            'title': 'interval_segment',
                            'stationFrom': {'id': station_from.id},
                            'stationTo': {'id': station_to.id},
                            'transport': {
                                'id': bus.id,
                                'code': bus.code,
                            },
                            'thread': {
                                'number': '456',
                                'density': 'density',
                                'beginTime': '10:00:00',
                                'endTime': '22:00:00',
                            },
                            'daysByTimezone': {
                                'Europe/Moscow': {
                                    'text': 'только 28\xa0мая'
                                }
                            }
                        }
                    ]
                }))

    response = get_response(settlement_from, settlement_to,  transport_type='bus')
    assert_that(response['result']['archivalData'],
                has_entries({
                    'transportTypes': ['bus'],
                    'canonical': has_entries({
                        'pointFrom': settlement_from.slug,
                        'pointTo': settlement_to.slug,
                        'transportType': 'bus',
                    }),
                    'segments': contains_inanyorder(
                        has_entries({
                            'title': 'interval_segment',
                            'transport': {
                                'id': bus.id,
                                'code': bus.code,
                            }
                        })
                    )
                }))

    response = get_response(settlement_from, settlement_to, when='2020-01-01')
    assert_that(response['result']['archivalData'],
                has_entries({
                    'segments': None
                }))


def test_archival_segments_extended_settlements():
    settlement_from = create_settlement(slug='settlement_slug_from')
    settlement_to = create_settlement(slug='settlement_slug_to')
    station_from = create_station(settlement=settlement_from, slug='slug_from')
    station_from_2 = create_station(settlement=settlement_from, slug='slug_from_2')
    station_to = create_station(settlement=settlement_to, slug='slug_to')
    station_to_2 = create_station(settlement=settlement_to, slug='slug_to_2')

    ArchivalSearchDataFactory(
        point_from=station_from.point_key,
        point_to=station_to.point_key,
        transport_type=TransportType.TRAIN_ID
    )

    ArchivalSettlementsDataFactory(
        point_from=settlement_from.point_key,
        point_to=settlement_to.point_key,
        transport_type=TransportType.TRAIN_ID,
        segments=[
            {
                'title': 'basic_segment',
                'station_from': {'id': station_from.id},
                'station_to': {'id': station_to.id}
            },
            {
                'title': 'filtered_segment_from',
                'station_from': {'id': station_from_2.id},
                'station_to': {'id': station_to.id}
            },
            {
                'title': 'filtered_segment_to',
                'station_from': {'id': station_from.id},
                'station_to': {'id': station_to_2.id}
            }
        ]
    )
    response = get_response(station_from, station_to)
    assert_that(response['result']['archivalData'],
                has_entries({
                    'transportTypes': ['train'],
                    'canonical': has_entries({
                        'pointFrom': station_from.slug,
                        'pointTo': station_to.slug,
                        'transportType': 'train',
                    }),
                    'segments': contains_inanyorder(
                        has_entries({
                            'title': 'basic_segment',
                            'stationFrom': {'id': station_from.id},
                            'stationTo': {'id': station_to.id},
                        })
                    )
                }))
