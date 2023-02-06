# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from lxml import etree

import pytest

from travel.rasp.library.python.common23.date import environment
from common.tester.factories import create_station, create_thread
from route_search.transfers.transfer_segment import RaspDBTransferSegment
from route_search.transfers.variant import Variant
from route_search.models import ZNodeRoute2
from route_search.transfers.fill_from_rasp_db import (
    fetch_stations, _fetch_threads, _fetch_rtstaitons, _add_stops_translations, fill_rasp_db_segments
)


pytestmark = [pytest.mark.dbuser]


class PathfinderSegmentStub(object):
    def __init__(self, station_from_id=None, station_to_id=None, thread_uid=None, transfer_segment=None):
        self.station_from_id = station_from_id
        self.station_to_id = station_to_id
        self.thread_uid = thread_uid
        self.station_from = None
        self.station_to = None
        self.transfer_segment = transfer_segment


class TransferSegmentStub(object):
    def __init__(self, station_from=None, station_to=None, thread=None):
        self.station_from = station_from
        self.station_to = station_to
        self.thread = thread
        self.is_valid = True


def test_fetch_stations():
    station1 = create_station(id=101)
    station2 = create_station(id=102)

    pathfinder_segments = [
        PathfinderSegmentStub(101, 102),
        PathfinderSegmentStub(102, 101),
        PathfinderSegmentStub(101, 103),
        PathfinderSegmentStub(103, 102),
    ]

    valid_segments = fetch_stations(pathfinder_segments)

    assert len(valid_segments) == 2
    assert valid_segments[0].station_from == station1
    assert valid_segments[0].station_to == station2
    assert valid_segments[1].station_from == station2
    assert valid_segments[1].station_to == station1


def test_fetch_threads():
    thread = create_thread(uid='uid')
    pathfinder_segments = [
        PathfinderSegmentStub(thread_uid='uid', transfer_segment=TransferSegmentStub()),
        PathfinderSegmentStub(thread_uid='uid2'),
    ]
    segments = _fetch_threads(pathfinder_segments)

    assert len(segments) == 1
    assert segments[0].thread == thread


def test_fetch_rtstations():
    station_from = create_station(id=101)
    station_to = create_station(id=102)
    thread0 = create_thread(uid='uid0', schedule_v1=[[None, 0, station_from], [10, None, station_to]])
    thread1 = create_thread(uid='uid1', schedule_v1=[[None, 0, station_to], [10, None, station_from]])

    segments = [
        TransferSegmentStub(station_from, station_to, thread0),
        TransferSegmentStub(station_to, station_from, thread1),
    ]
    _fetch_rtstaitons(segments)

    assert segments[0].rtstation_from is not None
    assert segments[0].rtstation_from.thread_id == thread0.id
    assert segments[0].rtstation_from.station_id == station_from.id
    assert segments[0].rtstation_to.thread_id == thread0.id
    assert segments[0].rtstation_to.station_id == station_to.id

    assert segments[1].rtstation_from is not None
    assert segments[1].rtstation_from.thread_id == thread1.id
    assert segments[1].rtstation_from.station_id == station_to.id
    assert segments[1].rtstation_to.thread_id == thread1.id
    assert segments[1].rtstation_to.station_id == station_from.id


def test_add_stops_translations():
    stops_text = u'остановки'
    station_from = create_station(id=101)
    station_to = create_station(id=102)
    thread = create_thread(uid='uid', schedule_v1=[[None, 0, station_from], [10, None, station_to]])
    ZNodeRoute2(
        route_id=thread.route_id,
        thread=thread,
        t_type_id=thread.t_type_id,
        station_from_id=station_from.id,
        station_to_id=station_to.id,
        stops_translations=stops_text
    ).save()

    segment = TransferSegmentStub(station_from, station_to, thread)
    _add_stops_translations([segment])

    assert segment.stops_translations == stops_text


VARIANT_XML = b"""<?xml version="1.0" encoding="utf-8" ?>
<routes>
    <group>
        <variant>
            <route
                start_date="2016-03-10" thread_id="uid"
                departure_datetime="2016-03-10 15:17" departure_station_id="101"
                arrival_datetime="2016-03-11 17:58" arrival_station_id="102"
            />
            <route
                start_date="2016-03-10" thread_id="NULL"
                departure_datetime="2016-03-10 15:17" departure_station_id="102"
                arrival_datetime="2016-03-11 17:58" arrival_station_id="102"
            />
            <route
                start_date="2016-03-11" thread_id="SU-1_20160311_c1_12"
                departure_datetime="2016-03-11 19:30" departure_station_id="102"
                arrival_datetime="2016-03-11 23:30" arrival_station_id="103"
            />
        </variant>
    </group>
</routes>
"""


def test_fill_rasp_db_segments():
    tree = etree.fromstring(VARIANT_XML)
    variant = Variant(tree.find('group').find('variant'))
    station_from = create_station(id=101)
    station_to = create_station(id=102)
    thread = create_thread(uid='uid', schedule_v1=[[None, 0, station_from], [10, None, station_to]])

    fetch_stations(variant.pathfinder_segments)
    fill_rasp_db_segments(variant.pathfinder_segments, environment.now_aware())

    assert variant.pathfinder_segments[0].transfer_segment is not None
    segment = variant.pathfinder_segments[0].transfer_segment
    assert isinstance(segment, RaspDBTransferSegment) is True
    assert segment.station_from == station_from
    assert segment.station_to == station_to
    assert segment.thread == thread
    assert segment.rtstation_from is not None
    assert segment.rtstation_from.thread_id == thread.id
    assert segment.rtstation_from.station_id == station_from.id
    assert segment.rtstation_to is not None
    assert segment.rtstation_to.thread_id == thread.id
    assert segment.rtstation_to.station_id == station_to.id

    assert variant.pathfinder_segments[1].transfer_segment is None
