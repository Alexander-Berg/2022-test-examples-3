# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta

import pytest
from lxml import etree
from hamcrest import has_entries, assert_that

from common.models.transport import TransportType
from travel.rasp.library.python.common23.date import environment
from common.tester.factories import create_station
from route_search.transfers.variant import Variant
from route_search.transfers.transfer_segment import BaseTransferSegment


pytestmark = [pytest.mark.dbuser]

VARIANT_XML = b"""<?xml version="1.0" encoding="utf-8" ?>
<routes>
    <group>
        <variant tr="4">
            <route
                tr="" start_date="2020-03-10" thread_id="155QI_11_2"
                departure_datetime="2020-03-10 03:41" departure_station_id="101"
                arrival_datetime="2020-03-11 06:01" arrival_station_id="102"
            />
            <route
                tr="2" start_date="2020-03-11" thread_id="NULL"
                departure_datetime="2020-03-11 06:36" departure_station_id="102"
                arrival_datetime="2020-03-18 11:21" arrival_station_id="201"
            />
            <route
                tr="3" start_date="2020-03-11" thread_id="SU-1_20200311_c1_12"
                departure_datetime="2020-03-11 07:30" departure_station_id="201"
                arrival_datetime="2020-03-11 11:30" arrival_station_id="202"
            />
        </variant>
    </group>
</routes>
"""


def test_init_variant():
    tree = etree.fromstring(VARIANT_XML)
    variant = Variant(tree.find('group').find('variant'))

    assert variant.display_info is not None
    assert variant.gone is False
    assert variant.price is None
    assert variant.convenience == 4

    assert variant.is_valid is True
    assert variant.segments == []
    assert variant.transfers == []

    assert len(variant.pathfinder_segments) == 2
    segment = variant.pathfinder_segments[0]
    assert segment.thread_uid == '155QI_11_2'
    assert segment.is_baris_segment is False
    assert segment.transfer_convenience == 2
    assert segment.convenience is None

    segment = variant.pathfinder_segments[1]
    assert segment.thread_uid == 'SU-1_20200311_c1_12'
    assert segment.is_baris_segment is True
    assert hasattr(segment, 'transfer_convenience') is False
    assert segment.convenience == 3


def test_make_segments_and_transfers():
    tree = etree.fromstring(VARIANT_XML)
    variant = Variant(tree.find('group').find('variant'))

    for pathfinder_segment in variant.pathfinder_segments:
        pathfinder_segment.station_from = create_station()
        pathfinder_segment.station_to = create_station()
        transfer_segment = BaseTransferSegment()
        pathfinder_segment.transfer_segment = transfer_segment
        transfer_segment.init_transfer_segment(pathfinder_segment, environment.now_aware())
        transfer_segment.t_type = TransportType.get_train_type()

    variant.make_segments_and_transfers()
    assert len(variant.segments) == 2
    assert variant.is_valid is True

    variant.pathfinder_segments[0].station_from = None
    variant.make_segments_and_transfers()
    assert len(variant.segments) == 1
    assert variant.is_valid is False

    variant.pathfinder_segments[0].station_from = create_station()
    variant.pathfinder_segments[1].transfer_segment = None
    variant.make_segments_and_transfers()
    assert len(variant.segments) == 1
    assert variant.is_valid is False


def test_variant_properties():
    tree = etree.fromstring(VARIANT_XML)
    variant = Variant(tree.find('group').find('variant'))
    variant.segments = []

    station0_from = create_station(title='f0')
    station0_to = create_station(title='f1')
    station1_from = create_station(title='t0')
    station1_to = create_station(title='t1')

    train_t_type = TransportType.get_train_type()
    plane_t_type = TransportType.get_plane_type()
    pathfinder_segment = variant.pathfinder_segments[0]
    pathfinder_segment.station_from = station0_from
    pathfinder_segment.station_to = station0_to
    pathfinder_segment.transfer_segment = BaseTransferSegment()
    pathfinder_segment.transfer_segment.init_transfer_segment(pathfinder_segment, environment.now_aware())
    pathfinder_segment.transfer_segment.thread = TreadStub(train_t_type)
    pathfinder_segment.transfer_segment.transfer_convenience = 2
    variant.segments.append(pathfinder_segment.transfer_segment)

    pathfinder_segment = variant.pathfinder_segments[1]
    pathfinder_segment.station_from = station1_from
    pathfinder_segment.station_to = station1_to
    pathfinder_segment.transfer_segment = BaseTransferSegment()
    pathfinder_segment.transfer_segment.init_transfer_segment(pathfinder_segment, environment.now_aware())
    pathfinder_segment.transfer_segment.thread = TreadStub(plane_t_type)
    variant.segments.append(pathfinder_segment.transfer_segment)

    assert variant.transport_types == [train_t_type, plane_t_type]
    assert variant.station_from == station0_from
    assert variant.station_to == station1_to
    assert variant.departure.isoformat() == '2020-03-10T03:41:00+03:00'
    assert variant.msk_departure.isoformat() == '2020-03-10T03:41:00+03:00'
    assert variant.arrival.isoformat() == '2020-03-11T11:30:00+03:00'
    assert variant.duration == timedelta(days=1, hours=7, minutes=49)

    variant.add_transfers_info()

    assert 'transfer' not in variant.segments[1].display_info
    assert_that(variant.segments[0].display_info['transfer'], has_entries({
        'in': station1_from,
        'duration': timedelta(hours=1, minutes=29),
        'price': None,
        'convenience': 2,
        'from': station0_to,
        'to': station1_from
    }))


class TreadStub(object):
    def __init__(self, t_type):
        self.t_type = t_type
