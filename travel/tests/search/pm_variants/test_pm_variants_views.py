# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import date

import mock
import pytest
from django.test import Client
from django.utils.http import urlencode
from hamcrest import assert_that, has_entries

from common.data_api.baris.test_helpers import mock_baris_response
from common.models.schedule import RThreadType
from common.models.transport import TransportType
from common.tester.factories import create_station, create_thread, create_company, create_settlement


pytestmark = [pytest.mark.dbuser]


PATHFINDER_RESPONSE_XML = """<?xml version="1.0" encoding="utf-8" ?>
<routes>
    <group>
        <variant>
            <route
                tr="1" start_date="2020-10-03" thread_id="t_uid"
                departure_datetime="2020-10-03 00:00" departure_station_id="101"
                arrival_datetime="2020-10-03 04:01" arrival_station_id="104"
            />
        </variant>
        <variant tr="4">
            <route
                tr="1" start_date="2020-10-03" thread_id="t_uid"
                departure_datetime="2020-10-03 00:00" departure_station_id="101"
                arrival_datetime="2020-10-03 01:00" arrival_station_id="102"
            />
            <route
                tr="2" start_date="2020-10-03" thread_id="NULL"
                departure_datetime="2020-10-03 01:00" departure_station_id="102"
                arrival_datetime="2020-10-03 02:00" arrival_station_id="103"
            />
            <route
                tr="3" start_date="2020-10-03" thread_id="SU-1_20201212_c1_12"
                departure_datetime="2020-10-03 03:00" departure_station_id="103"
                arrival_datetime="2020-10-03 04:00" arrival_station_id="104"
            />
        </variant>
    </group>
</routes>
"""


ONE_DAY_P2P_BARIS_RESPONSE = {
    'departureStations': [103],
    'arrivalStations': [104],
    'flights': [
        {
            'airlineID': 302,
            'title': 'SU 1',
            'departureDatetime': '2020-10-03 03:00:00+03:00',
            'departureTerminal': 'A',
            'departureStation': 103,
            'arrivalDatetime': '2020-10-03 04:00:00+03:00',
            'arrivalTerminal': '',
            'arrivalStation': 104,
            'transportModelID': 202,
            'route': [103, 104],
            'source': 'flight-board'
        }
    ]
}


def _build_pathfinder_maps_variants_url(point_from, point_to, dt, national_version):
    base_url = '/{}/search/pathfinder-maps-variants/?'.format(national_version)
    request_params = {
        'pointFrom': point_from.point_key,
        'pointTo': point_to.point_key,
        'when': dt.strftime('%Y-%m-%d')
    }
    return base_url + urlencode(request_params)


def test_pm_variants():
    settlement1 = create_settlement(id=11, title='c1')
    settlement2 = create_settlement(id=12, title='c2')
    settlement3 = create_settlement(id=13, title='c3')
    settlement4 = create_settlement(id=14, title='c4')
    station1 = create_station(id=101, title='s1', settlement=settlement1)
    station2 = create_station(id=102, title='s2', settlement=settlement2)
    create_station(id=103, title='s3', settlement=settlement3)
    station4 = create_station(id=104, title='s4', settlement=settlement4)

    company1 = create_company(id=301, title='p1')

    create_thread(
        uid='t_uid', number='t_number', title='t_title',
        type=RThreadType.THROUGH_TRAIN_ID,
        t_type=TransportType.TRAIN_ID,
        company=company1,
        schedule_v1=[
            (None, 0, station1),
            (60, 70, station2),
            (90, None, station4)
        ]
    )

    with mock.patch('route_search.transfers.transfers.get_pathfinder_response', return_value=PATHFINDER_RESPONSE_XML):
        with mock_baris_response(ONE_DAY_P2P_BARIS_RESPONSE):
            response = Client().get(_build_pathfinder_maps_variants_url(station1, station4, date(2020, 10, 3), 'ru'))
            result = json.loads(response.content)

    assert len(result['variants']) == 2
    variant_0, variant_1 = result['variants']

    assert len(variant_0['segments']) == 1
    segment_0_0 = variant_0['segments'][0]

    assert_that(
        segment_0_0, has_entries({
            'departure': '2020-10-02T21:00:00+00:00',
            'arrival': '2020-10-03T01:01:00+00:00',
            'thread': has_entries({
                'number': 't_number',
                'uid': 't_uid',
                'title': 't_title',
                'full_path': [101, 102, 104]
            }),
            'transport': has_entries({
                'code': 'train',
                'id': 1
            }),
            'departure_station': has_entries({
                'id': 101,
                'title': 's1',
                'settlement': has_entries({
                    'id': 11,
                    'title': 'c1'
                })
            }),
            'arrival_station': has_entries({
                'id': 104,
                'title': 's4',
                'settlement': has_entries({
                    'id': 14,
                    'title': 'c4'
                })
            }),
        })
    )

    assert len(variant_1['segments']) == 2
    segment_1_0, segment_1_1 = variant_1['segments']
    assert_that(
        segment_1_0, has_entries({
            'departure': '2020-10-02T21:00:00+00:00',
            'arrival': '2020-10-02T22:00:00+00:00',
            'thread': has_entries({
                'number': 't_number',
                'uid': 't_uid',
                'title': 't_title',
                'full_path': [101, 102]
            }),
            'transport': has_entries({
                'code': 'train',
                'id': 1
            }),
            'departure_station': has_entries({
                'id': 101,
                'title': 's1',
                'settlement': has_entries({
                    'id': 11,
                    'title': 'c1'
                })
            }),
            'arrival_station': has_entries({
                'id': 102,
                'title': 's2',
                'settlement': has_entries({
                    'id': 12,
                    'title': 'c2'
                })
            }),
        })
    )

    assert_that(
        segment_1_1, has_entries({
            'departure': '2020-10-03T00:00:00+00:00',
            'arrival': '2020-10-03T01:00:00+00:00',
            'thread': has_entries({
                'number': 'SU 1',
                'uid': '',
                'title': 'c3 \u2013 c4'
            }),
            'transport': has_entries({
                'code': 'plane',
                'id': 2
            }),
            'departure_station': has_entries({
                'id': 103,
                'title': 's3',
                'settlement': has_entries({
                    'id': 13,
                    'title': 'c3'
                })
            }),
            'arrival_station': has_entries({
                'id': 104,
                'title': 's4',
                'settlement': has_entries({
                    'id': 14,
                    'title': 'c4'
                })
            }),
        })
    )
