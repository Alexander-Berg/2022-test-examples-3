# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime

import pytest
from marshmallow import ValidationError

from common.models.geo import StationType
from common.models.transport import TransportType
from common.tester.testcase import TestCase
from common.tester.utils.datetime import replace_now

from travel.rasp.api_public.api_public.v3.request_schemas import StationTypesField, TransportTypesField, DateField


class TestTransportTypesField(TestCase):
    def test_get_transport_types(self):
        transport_type_field = TransportTypesField()
        transport_type_codes = ['bus', 'suburban']
        transport_types = transport_type_field.deserialize(','.join(transport_type_codes))
        assert set(transport_types) == set(TransportType.objects.filter(code__in=transport_type_codes))

        water_type_codes = ['sea', 'river', 'water']
        for water_type_code in water_type_codes:
            transport_types = transport_type_field.deserialize(water_type_code)
            assert set(transport_types) == set(TransportType.objects.filter(code__in=water_type_codes))

        TransportType.objects.get(code='sea').delete()
        transport_types = transport_type_field.deserialize(water_type_code)
        assert set(transport_types) == set(TransportType.objects.filter(code__in=water_type_codes[1:]))

        with pytest.raises(ValidationError):
            transport_type_field.deserialize(','.join(['bus', 'unknown']))


class TestStationTypesField(TestCase):
    def test_get_transport_types(self):
        station_type_field = StationTypesField()
        station_type_names = [u'airport', u'bus_stop', u'train_station']
        station_type_ids = [StationType.AIRPORT_ID, StationType.BUS_STOP_ID, StationType.TRAIN_STATION_ID]
        station_types = station_type_field.deserialize(u','.join(station_type_names))
        assert set(station_types) == set(StationType.objects.filter(id__in=station_type_ids))

        with pytest.raises(ValidationError):
            station_type_field.deserialize(u','.join([u'train_station', u'unknown_type']))


class TestDateField(object):
    date_field = DateField()

    @replace_now('2000-01-10 00:00:00')
    def test_parse_date(self):
        with pytest.raises(ValidationError):
            self.date_field.deserialize('2000.01.01')

        obtained_date = self.date_field.deserialize('2000-01-01')
        assert obtained_date == datetime(2000, 01, 01, 0, 0)

        obtained_date = self.date_field.deserialize('')
        assert obtained_date is None

    @replace_now('2000-02-02 00:00:00')
    def test_date_range(self):
        str_date = '2000-01-02'
        with pytest.raises(ValidationError):
            self.date_field.deserialize(str_date)

        str_date = '2001-01-03'
        with pytest.raises(ValidationError):
            self.date_field.deserialize(str_date)
