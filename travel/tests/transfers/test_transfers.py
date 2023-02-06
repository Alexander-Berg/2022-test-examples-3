# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime

import mock
import pytest
from django.conf import settings
from hamcrest import has_entries, assert_that, contains_inanyorder, contains

from common.models.transport import TransportType
from common.tester.factories import (
    create_settlement, create_station, create_company, create_transport_model, create_thread
)
from common.data_api.baris.test_helpers import mock_baris_response

from route_search.transfers.transfer_segment import RaspDBTransferSegment, BarisTransferSegment
from route_search.transfers.transfers import (
    _pathfinder_request_params, MAX_TRANSFER_MINUTES, get_pathfinder_response, get_transfer_variants,
    parse_pathfinder_response
)


pytestmark = [pytest.mark.dbuser]


EMPTY_XML = '<?xml version="1.0" encoding="utf-8" ?><routes />'

ONE_VARIANT_XML = """<?xml version="1.0" encoding="utf-8" ?>
<routes><group><variant>
    <route
        start_date="2016-10-03" thread_id="1"
        departure_datetime="2016-10-03 20:52" departure_station_id="101"
        arrival_datetime="2016-10-05 08:38" arrival_station_id="102"
    />
</variant></group></routes>"""


MANY_GROUPS_XML = """<?xml version="1.0" encoding="utf-8" ?>
<routes>
    <group>
        <!-- Correct segments from rasp db -->
        <variant>
            <route
                start_date="2020-10-03" thread_id="uid1"
                departure_datetime="2020-10-03 00:00" departure_station_id="101"
                arrival_datetime="2020-10-03 01:00" arrival_station_id="102"
            />
            <route
                start_date="2020-10-03" thread_id="NULL"
                departure_datetime="2020-10-03 01:00" departure_station_id="102"
                arrival_datetime="2020-10-03 02:00" arrival_station_id="103"
            />
            <route
                start_date="2020-10-03" thread_id="uid2"
                departure_datetime="2020-10-03 03:00" departure_station_id="103"
                arrival_datetime="2020-10-03 04:00" arrival_station_id="104"
            />
        </variant>

        <!-- Correct segments from BARiS  -->
        <variant>
            <route
                start_date="2020-10-03" thread_id="SU-1_20201003_c1_12"
                departure_datetime="2020-10-03 00:00" departure_station_id="101"
                arrival_datetime="2020-10-03 01:00" arrival_station_id="102"
            />
            <route
                start_date="2020-10-03" thread_id="NULL"
                departure_datetime="2020-10-03 01:00" departure_station_id="102"
                arrival_datetime="2020-10-03 02:00" arrival_station_id="103"
            />
            <route
                start_date="2020-10-03" thread_id="SU-2_20201003_c1_12"
                departure_datetime="2020-10-03 03:00" departure_station_id="103"
                arrival_datetime="2020-10-03 04:00" arrival_station_id="104"
            />
        </variant>
    </group>

    <!-- Single variants -->
    <group>
        <!-- Without transfers from rasp db -->
        <variant>
            <route
                start_date="2020-10-03" thread_id="uid1"
                departure_datetime="2020-10-03 00:00" departure_station_id="101"
                arrival_datetime="2020-10-03 01:00" arrival_station_id="102"
            />
        </variant>

        <!-- Without transfers from BARiS -->
        <variant>
            <route
                start_date="2020-10-03" thread_id="SU-1_20201003_c1_12"
                departure_datetime="2020-10-03 00:00" departure_station_id="101"
                arrival_datetime="2020-10-03 01:00" arrival_station_id="102"
            />
        </variant>
    </group>

    <!-- Incorrect segments -->
    <group>
        <!-- Station is not exists -->
        <variant>
            <route
                start_date="2020-10-03" thread_id="uid1"
                departure_datetime="2020-10-03 00:00" departure_station_id="101"
                arrival_datetime="2020-10-03 01:00" arrival_station_id="102"
            />
            <route
                start_date="2020-10-03" thread_id="NULL"
                departure_datetime="2020-10-03 01:00" departure_station_id="102"
                arrival_datetime="2020-10-03 02:00" arrival_station_id="103"
            />
            <route
                start_date="2020-10-03" thread_id="uid2"
                departure_datetime="2020-10-03 03:00" departure_station_id="103"
                arrival_datetime="2020-10-03 04:00" arrival_station_id="105"
            />
        </variant>

        <!-- Middle station is equal to start station -->
        <variant>
            <route
                start_date="2020-10-03" thread_id="uid1"
                departure_datetime="2020-10-03 00:00" departure_station_id="101"
                arrival_datetime="2020-10-03 01:00" arrival_station_id="101"
            />
            <route
                start_date="2020-10-03" thread_id="NULL"
                departure_datetime="2020-10-03 01:00" departure_station_id="101"
                arrival_datetime="2020-10-03 02:00" arrival_station_id="103"
            />
            <route
                start_date="2020-10-03" thread_id="uid2"
                departure_datetime="2020-10-03 03:00" departure_station_id="103"
                arrival_datetime="2020-10-03 04:00" arrival_station_id="104"
            />
        </variant>

        <!-- Middle station is equal to finish station -->
        <variant>
            <route
                start_date="2020-10-03" thread_id="uid1"
                departure_datetime="2020-10-03 00:00" departure_station_id="101"
                arrival_datetime="2020-10-03 01:00" arrival_station_id="104"
            />
            <route
                start_date="2020-10-03" thread_id="NULL"
                departure_datetime="2020-10-03 01:00" departure_station_id="104"
                arrival_datetime="2020-10-03 02:00" arrival_station_id="103"
            />
            <route
                start_date="2020-10-03" thread_id="uid2"
                departure_datetime="2020-10-03 03:00" departure_station_id="103"
                arrival_datetime="2020-10-03 04:00" arrival_station_id="104"
            />
        </variant>
    </group>
</routes>
"""


ONE_DAY_P2P_BARIS_RESPONSE = {
    'departureStations': [101, 103],
    'arrivalStations': [102, 104],
    'flights': [
        {
            'airlineID': 301,
            'title': 'SU 1',
            'departureDatetime': '2020-10-03 00:00:00+03:00',
            'departureTerminal': 'A',
            'departureStation': 101,
            'arrivalDatetime': '2020-10-03 01:00:00+03:00',
            'arrivalTerminal': '',
            'arrivalStation': 102,
            'transportModelID': 201,
            'route': [101, 102],
            'source': 'flight-board',
        },
        {
            'airlineID': 301,
            'title': 'SU 2',
            'departureDatetime': '2020-10-03 03:00:00+03:00',
            'departureTerminal': 'A',
            'departureStation': 103,
            'arrivalDatetime': '2020-10-03 04:00:00+03:00',
            'arrivalTerminal': '',
            'arrivalStation': 104,
            'transportModelID': 201,
            'route': [103, 104],
            'source': 'flight-board',
        }
    ]
}


def test_pathfinder_request_params():
    settlement1 = create_settlement(id=101, time_zone='Etc/GMT-5')
    settlement2 = create_settlement(id=102)
    station1 = create_station(id=201, settlement=settlement1, time_zone='Etc/GMT-5')
    station2 = create_station(id=202, settlement=settlement2)

    params = _pathfinder_request_params(settlement1, settlement2, datetime(2020, 10, 3, 2, 0, 0), ['plane', 'train'])
    assert_that(params, contains_inanyorder(
        ('from_type', 'settlement'),
        ('from_id', 101),
        ('to_type', 'settlement'),
        ('to_id', 102),
        ('date', '2020-10-03 02:00:00'),
        ('ttype', [TransportType.TRAIN_ID, TransportType.PLANE_ID]),
        ('boarding', 1440),
        ('max_delay', MAX_TRANSFER_MINUTES),
    ))

    params = _pathfinder_request_params(settlement1, station2, datetime(2020, 10, 3, 12, 30, 0), 'suburban')
    assert_that(params, contains_inanyorder(
        ('from_type', 'settlement'),
        ('from_id', 101),
        ('to_type', 'station'),
        ('to_id', 202),
        ('date', '2020-10-03 12:30:00'),
        ('ttype', [TransportType.SUBURBAN_ID]),
        ('boarding', 1440),
        ('max_delay', MAX_TRANSFER_MINUTES),
        ('can_change_in_any_town', 1)
    ))

    params = _pathfinder_request_params(station1, settlement2, date(2020, 10, 4), None)
    assert_that(params, contains_inanyorder(
        ('from_type', 'station'),
        ('from_id', 201),
        ('to_type', 'settlement'),
        ('to_id', 102),
        ('date', '2020-10-03 22:00:00'),
        ('ttype', [
            TransportType.TRAIN_ID, TransportType.PLANE_ID, TransportType.BUS_ID,
            TransportType.RIVER_ID, TransportType.SEA_ID, TransportType.SUBURBAN_ID, TransportType.WATER_ID
        ]),
        ('boarding', 1440),
        ('max_delay', MAX_TRANSFER_MINUTES),
    ))

    params = _pathfinder_request_params(
        settlement1, settlement2, datetime(2020, 10, 3, 2, 0, 0), ['spaceship', 'helicopter']
    )
    assert params is None


def _check_query_response(httpretty, url, params, expected_response):
    response = get_pathfinder_response(url, params)

    assert_that(httpretty.last_request.querystring, has_entries(
        date=contains('2020-10-03 00:00:00'),
        from_id=contains('101'),
        from_type=contains('station'),
        to_id=contains('102'),
        to_type=contains('station'),
        ttype=contains_inanyorder(str(TransportType.PLANE_ID))
    ))
    assert response == expected_response


def test_get_pathfinder_response(httpretty):
    station1 = create_station(id=101)
    station2 = create_station(id=102)
    request_params = _pathfinder_request_params(station1, station2, date(2020, 10, 3), 'plane')

    httpretty.register_uri(httpretty.GET, settings.PATHFINDER_CORE_URL, body=ONE_VARIANT_XML, status=200)
    _check_query_response(httpretty, settings.PATHFINDER_CORE_URL, request_params, ONE_VARIANT_XML)

    httpretty.register_uri(httpretty.GET, settings.PATHFINDER_CORE_URL, body=ONE_VARIANT_XML, status=500)
    _check_query_response(httpretty, settings.PATHFINDER_CORE_URL, request_params, EMPTY_XML)

    httpretty.register_uri(httpretty.GET, settings.PATHFINDER_CORE_URL, body='', status=200)
    _check_query_response(httpretty, settings.PATHFINDER_CORE_URL, request_params, EMPTY_XML)


def _check_rasp_segment(
    segment, thread, station_from, station_to,
    msk_departure, msk_arrival, start_date, is_valid
):
    assert isinstance(segment, RaspDBTransferSegment) is True
    assert segment.thread == thread
    assert segment.station_from == station_from
    assert segment.station_to == station_to
    assert segment.msk_departure.isoformat() == msk_departure
    assert segment.msk_arrival.isoformat() == msk_arrival
    assert segment.start_date == start_date
    assert segment.is_valid is is_valid


def _check_baris_segment(
    segment, thread_number, station_from, station_to,
    msk_departure, msk_arrival, transport_model, company, is_valid
):
    assert isinstance(segment, BarisTransferSegment) is True
    assert segment.thread.number == thread_number
    assert segment.station_from == station_from
    assert segment.station_to == station_to
    assert segment.msk_departure.isoformat() == msk_departure
    assert segment.msk_arrival.isoformat() == msk_arrival
    assert segment.transport.model == transport_model
    assert segment.company == company
    assert segment.is_valid is is_valid


@pytest.mark.parametrize('filter_single_variants, groups_count', [
    (True, 1),
    (False, 2)
])
def test_parse_pathfinder_response(filter_single_variants, groups_count):
    station1 = create_station(id=101)
    station2 = create_station(id=102)
    station3 = create_station(id=103)
    station4 = create_station(id=104)
    company = create_company(id=301)
    transport_model = create_transport_model(id=201)

    thread1 = create_thread(uid='uid1', schedule_v1=[
        [None, 0, station1], [50, 60, station2], [110, 120, station3], [180, None, station4]
    ])
    thread2 = create_thread(uid='uid2', schedule_v1=[
        [None, 0, station1], [50, 60, station2], [110, 120, station3], [180, None, station4]
    ])

    def check_rasp_variant(groups):
        assert len(groups[0].variants[0].segments) == 2
        _check_rasp_segment(
            groups[0].variants[0].segments[0],
            thread1, station1, station2,
            '2020-10-03T00:00:00+03:00', '2020-10-03T01:00:00+03:00',
            date(2020, 10, 3), True
        )
        _check_rasp_segment(
            groups[0].variants[0].segments[1],
            thread2, station3, station4,
            '2020-10-03T03:00:00+03:00', '2020-10-03T04:00:00+03:00',
            date(2020, 10, 3), True
        )
        if not filter_single_variants:
            assert len(groups[1].variants[0].segments) == 1
            _check_rasp_segment(
                groups[1].variants[0].segments[0],
                thread1, station1, station2,
                '2020-10-03T00:00:00+03:00', '2020-10-03T01:00:00+03:00',
                date(2020, 10, 3), True
            )

    def check_baris_variant(groups):
        assert len(groups[0].variants[1].segments) == 2
        _check_baris_segment(
            groups[0].variants[1].segments[0], 'SU 1', station1, station2,
            '2020-10-03T00:00:00+03:00', '2020-10-03T01:00:00+03:00', transport_model, company, True
        )
        _check_baris_segment(
            groups[0].variants[1].segments[1], 'SU 2', station3, station4,
            '2020-10-03T03:00:00+03:00', '2020-10-03T04:00:00+03:00', transport_model, company, True
        )

        if not filter_single_variants:
            assert len(groups[1].variants[1].segments) == 1
            _check_baris_segment(
                groups[1].variants[1].segments[0], 'SU 1', station1, station2,
                '2020-10-03T00:00:00+03:00', '2020-10-03T01:00:00+03:00', transport_model, company, True
            )

    with mock_baris_response(ONE_DAY_P2P_BARIS_RESPONSE):
        groups = parse_pathfinder_response(
            MANY_GROUPS_XML,
            point_from=station1, point_to=station4, filter_single_variants=filter_single_variants
        )

        assert len(groups) == groups_count
        assert all(len(group.variants) == 2 for group in groups)
        check_rasp_variant(groups)
        check_baris_variant(groups)


def test_get_transfer_variants():
    station1 = create_station()
    station2 = create_station()
    request_params = _pathfinder_request_params(station1, station2, date(2020, 10, 3), 'plane')

    with mock.patch('route_search.transfers.transfers.get_pathfinder_response', return_value=EMPTY_XML) \
            as m_get_pathfinder_response:
        with mock.patch('route_search.transfers.transfers.parse_pathfinder_response', return_value=[]) \
                as m_parse_pathfinder_response:

            variants = get_transfer_variants(station1, station2, date(2020, 10, 3), 'plane')

            assert list(variants) == []
            m_get_pathfinder_response.assert_called_once_with(settings.PATHFINDER_CORE_URL, request_params)
            m_parse_pathfinder_response.assert_called_once_with(
                EMPTY_XML, station1, station2, True, '\u2013'
            )
