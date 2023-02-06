# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.models.transport import TransportType
from common.models.geo import StationType
from common.tester.factories import (
    create_station, create_thread, create_transport_subtype_color, create_transport_subtype
)
from common.tester.testcase import TestCase

from travel.rasp.api_public.api_public.v3.views.json_helpers import point2json, Company2Json, Thread2Json


class TestPoint2Json(TestCase):
    def test_station_type(self):
        station = create_station(station_type_id=StationType.BUS_STATION_ID)

        json_point = point2json(station)
        assert json_point['station_type'] == 'bus_station'
        assert json_point['station_type_name'] == 'автовокзал'


class TestCompany2Json(TestCase):
    def test_not_fail_on_none(self):
        serializer = Company2Json(logo_svg=True)
        assert serializer(None) is None


class TestThread2Json(TestCase):
    def test_thread(self):
        color = create_transport_subtype_color(color='#ff9218', code=0)
        thread = create_thread(t_subtype=create_transport_subtype(t_type_id=TransportType.SUBURBAN_ID,
                                                                  title_ru='suburban', code='last', color=color))
        json_thread = Thread2Json()(thread)
        assert ['transport_subtype']
        assert json_thread['transport_subtype']['code'] == 'last'
        assert json_thread['transport_subtype']['title'] == 'suburban'
        assert json_thread['transport_subtype']['color'] == '#ff9218'

        thread = create_thread(t_subtype=create_transport_subtype(t_type_id=TransportType.WATER_ID, title_ru='river'))
        json_thread = Thread2Json()(thread)
        assert ['transport_subtype']
        assert json_thread['transport_subtype']['code'] == ''
        assert json_thread['transport_subtype']['title'] == 'river'
        assert json_thread['transport_subtype']['color'] is None
