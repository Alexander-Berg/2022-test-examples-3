# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import pytest
from django.test import Client
from hamcrest import has_entries, assert_that, contains_inanyorder

from common.models.geo import Station2Settlement, Country
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station, create_settlement
from common.tester.utils.datetime import replace_now
from common.data_api.baris.test_helpers import mock_baris_response


pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def get_response(point_from, point_to, transport_type=None, baris_response=None):
    params = {
        'pointFrom': point_from.point_key,
        'pointTo': point_to.point_key
    }

    if transport_type:
        params['transportType'] = transport_type

    with mock_baris_response(baris_response or {}):
        return json.loads(Client().get('/ru/search/canonicals/', params).content)


def get_baris_response(station_from, station_to):
    return {'flights': [{
        'departureStation': station_from.id,
        'arrivalStation': station_to.id,
        'airlineID': 1,
        'arrivalDayShift': 0,
        'departureTime': '01:00',
        'arrivalTime': '05:00',
        'masks': [{
            'from': '2020-09-01',
            'until': '2020-09-25',
            'on': 34
        }],
        'route': [station_from.id, station_to.id]
    }]}


def create_any_thread(station_from, station_to, t_type):
    create_thread(
        __={'calculate_noderoute': True},
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ],
        t_type=t_type
    )


@replace_now('2020-09-01 12:00:00')
def test_canonicals():
    settlement_from = create_settlement(slug='settlement_slug_from', title_ru='город from', country=Country.RUSSIA_ID)
    settlement_to = create_settlement(slug='settlement_slug_to', title_ru='город to', country=Country.RUSSIA_ID)

    train_station_from = create_station(
        settlement=settlement_from, slug='slug_from', title_ru='станция from', type_choices='train,suburban'
    )
    another_station_from = create_station(
        settlement=settlement_from, slug='another_slug_from', type_choices='train,suburban'
    )
    train_station_to = create_station(
        settlement=settlement_to, slug='slug_to', title_ru='станция to', type_choices='train,suburban'
    )
    station_to_without_settlement = create_station(slug='slug_to_without')

    bus_station_from = create_station(
        settlement=settlement_from, slug='slug_bus_from', title_ru='станция from', type_choices='schedule'
    )
    bus_station_to = create_station(
        settlement=settlement_to, slug='slug_bus_to', title_ru='станция to', type_choices='schedule'
    )

    plane_station_from = create_station(
        settlement=settlement_from, slug='plane_slug_from', t_type='plane', type_choices='tablo'
    )
    plane_station_to = create_station(
        settlement=settlement_to, slug='plane_slug_to',  t_type='plane', type_choices='tablo'
    )

    create_any_thread(bus_station_from, bus_station_to, TransportType.BUS_ID)
    create_any_thread(train_station_from, train_station_to, TransportType.TRAIN_ID)
    response = get_response(settlement_from, settlement_to, 'bus')
    assert_that(response['result']['canonicals'], contains_inanyorder(
        has_entries({
            'canonical': {
                'pointFrom': train_station_from.slug,
                'pointTo': train_station_to.slug,
                'transportType': 'train'
            },
            'pointFrom': {
                'title': 'станция from'
            },
            'pointTo': {
                'title': 'станция to'
            }
        })
    ))

    create_any_thread(train_station_from, train_station_to, TransportType.SUBURBAN_ID)
    response = get_response(
        settlement_from, settlement_to, 'bus', get_baris_response(plane_station_from, plane_station_to)
    )
    assert_that(response['result']['canonicals'], contains_inanyorder(
        has_entries({'canonical': {
            'pointFrom': train_station_from.slug,
            'pointTo': train_station_to.slug,
            'transportType': 'train'
        }}),
        has_entries({'canonical': {
            'pointFrom': train_station_from.slug,
            'pointTo': train_station_to.slug,
            'transportType': 'suburban'
        }}),
        has_entries({
            'canonical': {
                'pointFrom': settlement_from.slug,
                'pointTo': settlement_to.slug,
                'transportType': 'plane'
            },
            'pointFrom': {
                'title': 'город from'
            },
            'pointTo': {
                'title': 'город to'
            }
        }),
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': None
        }})
    ))

    response = get_response(
        settlement_from, settlement_to, baris_response=get_baris_response(plane_station_from, plane_station_to)
    )
    assert_that(response['result']['canonicals'], contains_inanyorder(
        has_entries({'canonical': {
            'pointFrom': train_station_from.slug,
            'pointTo': train_station_to.slug,
            'transportType': 'train'
        }}),
        has_entries({'canonical': {
            'pointFrom': train_station_from.slug,
            'pointTo': train_station_to.slug,
            'transportType': 'suburban'
        }}),
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': 'plane'
        }}),
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': 'bus'
        }})
    ))

    # stations
    response = get_response(train_station_from, train_station_to)
    assert_that(response['result']['canonicals'], contains_inanyorder(
        has_entries({'canonical': {
            'pointFrom': train_station_from.slug,
            'pointTo': train_station_to.slug,
            'transportType': 'train'
        }}),
        has_entries({'canonical': {
            'pointFrom': train_station_from.slug,
            'pointTo': train_station_to.slug,
            'transportType': 'suburban'
        }}),
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': 'bus'
        }})
    ))

    response = get_response(train_station_from, station_to_without_settlement)
    assert response['result']['canonicals'] == []

    create_any_thread(another_station_from, train_station_to, TransportType.TRAIN_ID)
    response = get_response(
        train_station_from, train_station_to, 'suburban', baris_response=get_baris_response(plane_station_from, plane_station_to)
    )
    assert_that(response['result']['canonicals'], contains_inanyorder(
        has_entries({
            'canonical': {
                'pointFrom': settlement_from.slug,
                'pointTo': train_station_to.slug,
                'transportType': 'train'
            },
            'pointFrom': {
                'title': 'город from'
            },
            'pointTo': {
                'title': 'станция to'
            }
        }),
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': 'plane'
        }}),
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': 'bus'
        }}),
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': None
        }})
    ))

    response = get_response(
        train_station_from, train_station_to, 'plane', baris_response=get_baris_response(plane_station_from, plane_station_to)
    )
    assert_that(response['result']['canonicals'], contains_inanyorder(
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': train_station_to.slug,
            'transportType': 'train'
        }}),
        has_entries({'canonical': {
            'pointFrom': train_station_from.slug,
            'pointTo': train_station_to.slug,
            'transportType': 'suburban'
        }}),
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': 'bus'
        }}),
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': None
        }})
    ))


@replace_now('2020-09-01 12:00:00')
def test_railway_canonicals():
    settlement_from = create_settlement(slug='settlement_slug_from')
    settlement_to = create_settlement(slug='settlement_slug_to')
    station_from = create_station(settlement=settlement_from, slug='slug_from')
    station_to = create_station(settlement=settlement_to, slug='slug_to')
    another_station_from = create_station(settlement=settlement_from, slug='another_slug_from')
    another_station_to = create_station(settlement=settlement_to, slug='another_slug_to')

    create_any_thread(station_from, station_to, TransportType.TRAIN_ID)
    response = get_response(settlement_from, settlement_to, 'bus')
    assert_that(response['result']['canonicals'], contains_inanyorder(
        has_entries({'canonical': {
            'pointFrom': station_from.slug,
            'pointTo': station_to.slug,
            'transportType': 'train'
        }})
    ))

    create_any_thread(another_station_from, another_station_to, TransportType.TRAIN_ID)
    response = get_response(settlement_from, settlement_to)
    assert_that(response['result']['canonicals'], contains_inanyorder(
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': 'train'
        }})
    ))

    create_any_thread(station_from, station_to, TransportType.SUBURBAN_ID)
    response = get_response(settlement_from, settlement_to)
    assert_that(response['result']['canonicals'], contains_inanyorder(
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': 'train'
        }}),
        has_entries({'canonical': {
            'pointFrom': station_from.slug,
            'pointTo': station_to.slug,
            'transportType': 'suburban'
        }})
    ))

    create_any_thread(station_from, another_station_to, TransportType.SUBURBAN_ID)
    response = get_response(settlement_from, settlement_to)
    assert_that(response['result']['canonicals'], contains_inanyorder(
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': 'train'
        }}),
        has_entries({'canonical': {
            'pointFrom': station_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': 'suburban'
        }})
    ))


@replace_now('2020-09-01 12:00:00')
def test_station_2_settlement():
    settlement_from = create_settlement(slug='settlement_slug_from')
    settlement_to = create_settlement(slug='settlement_slug_to')
    station_from = create_station(settlement=settlement_from, slug='slug_from')
    station_to = create_station(settlement=settlement_to, slug='slug_to')

    create_any_thread(station_from, station_to, TransportType.BUS_ID)
    station_from.settlement = None
    station_from.save()
    response = get_response(station_from, station_to)
    assert response['result']['canonicals'] == []

    s_2_s = Station2Settlement.objects.create(station=station_from, settlement=settlement_from)
    s_2_s.save()
    response = get_response(settlement_from, settlement_to)
    assert_that(response['result']['canonicals'], contains_inanyorder(
        has_entries({'canonical': {
            'pointFrom': settlement_from.slug,
            'pointTo': settlement_to.slug,
            'transportType': 'bus'
        }})
    ))


@replace_now('2020-09-01 12:00:00')
def test_same_settlements_canonicals():
    settlement = create_settlement(slug='settlement_slug', title_ru='город', country=Country.RUSSIA_ID)

    station_from = create_station(settlement=settlement, slug='slug_from', title_ru='станция from')
    station_to = create_station(settlement=settlement, slug='slug_to', title_ru='станция to')

    create_any_thread(station_from, station_to, TransportType.BUS_ID)
    response = get_response(station_from, station_to)
    assert_that(response['result'], has_entries({
        'canonicals': []
    }))
