# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date

import pytest
from lxml import etree

from common.tester.factories import create_station
from route_search.transfers.pathfinder_segment import PathfinderSegment


pytestmark = [pytest.mark.dbuser]

ONE_VARIANT_XML = b"""<?xml version="1.0" encoding="utf-8" ?>
<routes><group><variant>
    <route
        tr="111" start_date="2020-10-03" thread_id="SU-1_20201003_c1_12"
        departure_datetime="2020-10-03 20:52" departure_station_id="101"
        arrival_datetime="2020-10-05 08:38" arrival_station_id="102"
    />
</variant></group></routes>"""

WRONG_VARIANT_XML = b"""<?xml version="1.0" encoding="utf-8" ?>
<routes><group><variant>
    <route
        start_date="2020-10-03" thread_id="SU-1_20201003_c1_12"
        departure_datetime="2008-01-01 20:52" departure_station_id="101"
        arrival_datetime="2008-01-02 08:38" arrival_station_id="102"
    />
</variant></group></routes>"""


def test_pathfinder_segment():
    tree = etree.fromstring(ONE_VARIANT_XML)
    segment = PathfinderSegment(tree.find('group').find('variant').find('route'))

    assert segment.thread_uid == 'SU-1_20201003_c1_12'
    assert segment.flight_number == 'SU-1'
    assert segment.is_baris_segment is True
    assert segment.msk_departure.isoformat() == '2020-10-03T20:52:00+03:00'
    assert segment.msk_arrival.isoformat() == '2020-10-05T08:38:00+03:00'
    assert segment.msk_start_date == date(2020, 10, 3)
    assert segment.station_from_id == 101
    assert segment.station_to_id == 102
    assert segment.convenience == 111
    assert segment.transfer_segment is None
    assert segment.is_valid is False

    segment.station_from = create_station()
    assert segment.is_valid is False
    segment.station_to = create_station()
    assert segment.is_valid is True

    msk_arrival = segment.msk_arrival
    segment.msk_arrival = None
    assert segment.is_valid is False
    segment.msk_arrival = msk_arrival

    segment.msk_departure = None
    assert segment.is_valid is False

    tree = etree.fromstring(WRONG_VARIANT_XML)
    segment2 = PathfinderSegment(tree.find('group').find('variant').find('route'))
    segment2.station_from = create_station()
    segment2.station_to = create_station()
    assert segment2.is_valid is False

    segment2.msk_arrival = segment2.msk_departure
    segment2.msk_departure = segment.msk_arrival
    assert segment2.is_valid is False

    segment2.msk_arrival = segment.msk_arrival
    assert segment2.is_valid is True
