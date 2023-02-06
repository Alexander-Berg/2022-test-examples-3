# coding: utf8
from __future__ import unicode_literals

import json

import pytest
from django.test import Client
from freezegun import freeze_time
from hamcrest import assert_that, has_entries

from common.data_api.baris.test_helpers import mock_baris_response
from common.models.factories import create_teaser as common_create_teaser
from travel.rasp.library.python.common23.tester.factories.factories import (
    create_external_direction, create_external_direction_marker
)
from common.models.geo import Country
from common.models.teasers import Teaser
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station, create_settlement


pytestmark = pytest.mark.mongouser('module')

ALL_MODES = [m[0] for m in Teaser.MODES]
DEFAULT_MODE = 'normal'

create_teaser = common_create_teaser.mutate(mode=DEFAULT_MODE, title='Тизер',
                                            content='Содержимое тизера', is_active_rasp=True)


@pytest.yield_fixture(scope='module', autouse=True)
def freeze_module_time():
    with freeze_time('2016-01-10'):
        yield


def search_request(query):
    return json.loads(Client().get('/{}/search/search/'.format('ru'), query).content)


@pytest.mark.dbuser
@pytest.mark.parametrize('mode', ALL_MODES)
def test_search_teasers(mode):
    station_from = create_station()
    station_to = create_station()
    create_thread(__={'calculate_noderoute': True}, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to],
    ])

    teaser = create_teaser(mode=mode, pages=[{'code': 'search'}])

    if mode == 'ahtung':
        mode = 'attention'

    response = search_request({
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'when': '2016-01-20',
        'nationalVersion': 'ru',
        'transportType': 'bus'
    })

    teasers = response['result']['teasers']
    assert_that(teasers, has_entries({mode: has_entries({
        'id': teaser.id,
        'title': teaser.title,
        'content': teaser.content,
    })}))


@pytest.mark.dbuser
@pytest.mark.parametrize('mode', ALL_MODES)
def test_suburban_search_teasers(mode):
    station_from = create_station(t_type=TransportType.TRAIN_ID)
    station_to = create_station(t_type=TransportType.TRAIN_ID)
    create_thread(__={'calculate_noderoute': True}, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to],
    ], t_type=TransportType.SUBURBAN_ID)

    teaser = create_teaser(mode=mode, pages=[{'code': 'search_suburban'}])

    response = search_request({
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'when': '2016-01-20',
        'transportType': 'suburban',
        'nationalVersion': 'ru'
    })

    if mode == 'ahtung':
        mode = 'attention'

    teasers = response['result']['teasers']
    assert_that(teasers, has_entries({mode: has_entries({
        'id': teaser.id,
        'title': teaser.title,
        'content': teaser.content,
    })}))


@pytest.mark.dbuser
@pytest.mark.parametrize('page,t_type_ids,show_teaser', [
    ('search_trains', [TransportType.TRAIN_ID, TransportType.PLANE_ID], True),
    ('search_trains_only', [TransportType.TRAIN_ID, TransportType.PLANE_ID], False),
    ('search_trains', [TransportType.PLANE_ID], False),
    ('search_planes', [TransportType.TRAIN_ID, TransportType.PLANE_ID], True),
    ('search_planes_only', [TransportType.TRAIN_ID, TransportType.PLANE_ID], False),
    ('search_planes', [TransportType.TRAIN_ID], False),
    ('search_trains_only', [TransportType.TRAIN_ID], True),
    ('search_trains_only', [TransportType.TRAIN_ID, TransportType.BUS_ID], True),  # Наследие кода, так работало??
    ('search_planes_only', [TransportType.PLANE_ID], True),
    ('search_planes_only', [TransportType.PLANE_ID, TransportType.BUS_ID], True),  # Наследие кода, так работало??
])
def test_t_type_cases(page, t_type_ids, show_teaser):
    settlement_from = create_settlement(country=Country.RUSSIA_ID)
    settlement_to = create_settlement(country=Country.RUSSIA_ID)
    type_choices = {TransportType.TRAIN_ID: 'train', TransportType.PLANE_ID: 'tablo', TransportType.BUS_ID: 'schedule'}

    for t_type_id in t_type_ids:
        station_from = create_station(
            t_type=t_type_id, settlement=settlement_from, type_choices=type_choices[t_type_id]
        )
        station_to = create_station(
            t_type=t_type_id, settlement=settlement_to, type_choices=type_choices[t_type_id]
        )
        if t_type_id == TransportType.PLANE_ID:
            avia_station_from = station_from
            avia_station_to = station_to
        else:
            create_thread(__={'calculate_noderoute': True}, schedule_v1=[
                [None, 0, station_from],
                [10, None, station_to],
            ], t_type=t_type_id)

    create_teaser(pages=[{'code': page}])

    if TransportType.PLANE_ID not in t_type_ids:
        baris_data = {}
    else:
        baris_data = {
            'flights': [{
                'airlineID': 301,
                'departureStation': avia_station_from.id,
                'arrivalStation': avia_station_to.id,
                'title': 'SU 1',
                'route': [avia_station_from.id, avia_station_to.id],
                'transportModelID': 201,
                'departureDatetime': '2016-01-20T01:00:00+05:00',
                'arrivalDatetime': '2016-01-20T02:00:00+05:00',
            }]
        }

    with mock_baris_response(baris_data):
        response = search_request({
            'pointFrom': settlement_from.point_key,
            'pointTo': settlement_to.point_key,
            'when': '2016-01-20'
        })

    assert show_teaser == (DEFAULT_MODE in response['result']['teasers'])


@pytest.mark.dbuser
@pytest.mark.parametrize('teaser_national_version,query_national_version,show_teaser', [
    ('ru', 'ru', True),
    ('ru', None, True),
    ('ru', 'ua', False),
    ('ua', None, False),
    ('ua', 'ua', True),
    ('ua', 'ru', False),
])
def test_national_version_cases(teaser_national_version, query_national_version, show_teaser):
    station_from = create_station()
    station_to = create_station()
    create_thread(__={'calculate_noderoute': True}, schedule_v1=[
        [None, 0, station_from],
        [10, None, station_to],
    ])

    create_teaser(pages=[{'code': 'search'}], national_version=teaser_national_version)
    query = {
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'when': '2016-01-20',
        'transportType': 'bus'
    }
    if query_national_version:
        query['nationalVersion'] = query_national_version

    response = search_request(query)

    assert show_teaser == (DEFAULT_MODE in response['result']['teasers'])


@pytest.mark.dbuser
@pytest.mark.parametrize('direction_used', [True, False])
def test_direction_case(direction_used):
    station_from = create_station(country=Country.RUSSIA_ID, t_type=TransportType.TRAIN_ID)
    station_to = create_station(country=Country.RUSSIA_ID, t_type=TransportType.TRAIN_ID)
    create_thread(
        __={'calculate_noderoute': True}, t_type=TransportType.SUBURBAN_ID,
        schedule_v1=[[None, 0, station_from], [10, None, station_to]]
    )

    direction = create_external_direction()
    create_external_direction_marker(external_direction=direction, station=station_from)
    create_external_direction_marker(external_direction=direction, station=station_to)

    create_teaser(pages=[{'code': 'some_logic_page'}], external_directions=[direction] if direction_used else [])

    response = search_request({
        'pointFrom': station_from.point_key,
        'pointTo': station_to.point_key,
        'when': '2016-01-20',
        'transportType': 'suburban'
    })

    assert direction_used == (DEFAULT_MODE in response['result']['teasers'])
