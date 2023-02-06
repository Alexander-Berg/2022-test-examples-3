# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime

import pytest
from lxml import etree

from common.data_api.baris.instance import baris
from common.data_api.baris.helpers import BarisData
from common.data_api.baris.test_helpers import mock_baris_response
from common.tester.factories import create_station, create_thread
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment
from common.models.schedule import RTStation
from route_search.transfers.transfer_segment import BaseTransferSegment, RaspDBTransferSegment, BarisTransferSegment
from route_search.transfers.pathfinder_segment import PathfinderSegment


pytestmark = [pytest.mark.dbuser]

ONE_VARIANT_XML = b"""<?xml version="1.0" encoding="utf-8" ?>
<routes><group><variant>
    <route
        start_date="2020-10-03" thread_id="SU-1_20201003_c1_12"
        departure_datetime="2020-10-03 20:52" departure_station_id="101"
        arrival_datetime="2020-10-05 08:38" arrival_station_id="102"
    />
</variant></group></routes>"""

ONE_DAY_P2P_BARIS_RESPONSE = {
    'departureStations': [101],
    'arrivalStations': [102],
    'flights': [
        {
            'airlineID': 301,
            'title': 'SU 1',
            'departureDatetime': '2020-10-03 20:52:00+03:00',
            'departureTerminal': 'A',
            'departureStation': 101,
            'arrivalDatetime': '2020-10-05 08:38:00+03:00',
            'arrivalTerminal': '',
            'arrivalStation': 102,
            'startDatetime': '2020-10-03 20:52:00+03:00',
            'transportModelID': 201,
            'codeshares': [{
                'airlineID': 302,
                'title': 'SV 1'
            }],
            'route': [101, 102],
            'source': 'flight-board',
        }
    ]
}


def _create_pathfinder_segment():
    tree = etree.fromstring(ONE_VARIANT_XML)
    pathfinder_segment = PathfinderSegment(tree.find('group').find('variant').find('route'))
    station_from = create_station(id=101, title='A1', time_zone='Etc/GMT-5')
    station_to = create_station(id=102, title='A2', time_zone='Etc/GMT-6')
    pathfinder_segment.station_from = station_from
    pathfinder_segment.station_to = station_to
    return pathfinder_segment


def _check_base_transfer_segment(segment, station_from, station_to):
    assert segment.msk_departure.isoformat() == '2020-10-03T20:52:00+03:00'
    assert segment.msk_arrival.isoformat() == '2020-10-05T08:38:00+03:00'
    assert segment.msk_start_date == date(2020, 10, 3)
    assert segment.station_from == station_from
    assert segment.station_to == station_to
    assert segment.departure.isoformat() == '2020-10-03T22:52:00+05:00'
    assert segment.arrival.isoformat() == '2020-10-05T11:38:00+06:00'
    assert segment.gone is True
    assert segment.is_transfer_segment is True
    assert segment.display_info is not None
    assert segment.price is None
    assert segment.transfer_price is None
    assert segment.transfer_convenience is None


@replace_now('2020-10-04')
def test_base_transfer_segment():
    pathfinder_segment = _create_pathfinder_segment()
    segment = BaseTransferSegment()
    segment.init_transfer_segment(pathfinder_segment, environment.now_aware())
    _check_base_transfer_segment(segment, pathfinder_segment.station_from, pathfinder_segment.station_to)


@replace_now('2020-10-04')
def test_rasp_db_transfer_segment():
    pathfinder_segment = _create_pathfinder_segment()
    segment = RaspDBTransferSegment(pathfinder_segment, environment.now_aware())

    assert segment.thread is None
    assert segment.rtstation_from is None
    assert segment.rtstation_to is None

    segment.thread = create_thread(
        year_days=[date(2020, 10, 3)],
        schedule_v1=[
            [None, 0, pathfinder_segment.station_from],
            [10, None, pathfinder_segment.station_to],
        ])
    segment.rtstation_from = RTStation.objects.get(
        thread_id=segment.thread.id, station_id=pathfinder_segment.station_from.id
    )
    segment.rtstation_to = RTStation.objects.get(
        thread_id=segment.thread.id, station_id=pathfinder_segment.station_to.id
    )

    _check_base_transfer_segment(segment, pathfinder_segment.station_from, pathfinder_segment.station_to)
    assert segment.is_valid is True
    assert segment.start_date == date(2020, 10, 3)


@replace_now('2020-10-04')
def test_baris_transfer_segment():
    pathfinder_segment = _create_pathfinder_segment()

    with mock_baris_response(ONE_DAY_P2P_BARIS_RESPONSE):
        baris_data = BarisData(baris.get_p2p_search(
            [101], [102],
            after=datetime(2020, 10, 3, 0, 0, 0), before=datetime(2020, 10, 4, 0, 0, 0)
        ))
        flight = baris_data.flights[0]

        segment = BarisTransferSegment(flight, baris_data, pathfinder_segment, environment.now_aware())

        _check_base_transfer_segment(segment, pathfinder_segment.station_from, pathfinder_segment.station_to)
        assert segment.is_valid is True

        assert segment.start.isoformat() == '2020-10-03T20:52:00+03:00'

        assert segment.station_from == pathfinder_segment.station_from
        assert segment.station_to == pathfinder_segment.station_to
        assert segment.number == 'SU 1'
        assert segment.title == 'A1 \u2013 A2'
        assert segment.transport.code == 'plane'

        assert segment.train_numbers is None
        assert segment.is_interval is False
        assert segment.is_through_train is False

        assert segment.thread.number == 'SU 1'
        assert segment.thread.t_type.code == 'plane'
