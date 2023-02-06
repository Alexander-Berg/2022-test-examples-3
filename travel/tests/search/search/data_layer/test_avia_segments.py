# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import timedelta, date
from dateutil import parser

import pytest

from common.tester.factories import create_settlement, create_station, create_company, create_transport_model
from common.tester.utils.datetime import replace_now
from common.data_api.baris.helpers import BarisData
from common.data_api.baris.service import BarisResponse
from common.models.transport import TransportType

from travel.rasp.morda_backend.morda_backend.search.search.data_layer.avia_segments import (
    SearchAviaSegment, OneDayAviaSegment, AllDaysAviaSegment
)


pytestmark = [pytest.mark.dbuser]
create_station = create_station.mutate(t_type=TransportType.PLANE_ID)


def test_base_avia_segment():
    settlement1 = create_settlement(title='От')
    settlement2 = create_settlement(title='До')
    create_station(settlement=settlement1, id=101)
    create_station(settlement=settlement2, id=102)
    create_station(id=103, title='От станции')
    create_station(id=104, title='До станции')
    company = create_company(id=301)
    model_type = create_transport_model(id=201, title='Модель')

    flight = {
        'airlineID': 301,
        'title': 'SU 1',
        'departureStation': 101,
        'arrivalStation': 102,
        'route': [101, 102],
        'transportModelID': 201,
    }

    baris_data = BarisData(BarisResponse([flight], {101, 102}, {301}, {201}))
    segment = SearchAviaSegment(flight, baris_data)

    assert segment.transport.id == TransportType.PLANE_ID
    assert segment.transport.code == 'plane'
    assert segment.transport.title == 'Самолёт'
    assert segment.transport.model == model_type

    assert segment.thread.number == 'SU 1'
    assert segment.thread.title == u'От \u2013 До'
    assert segment.thread.uid == ''
    assert segment.thread.is_express is False
    assert segment.thread.is_aeroexpress is False
    assert segment.thread.begin_time is None
    assert segment.thread.end_time is None
    assert segment.thread.density == ''
    assert segment.thread.comment == ''
    assert segment.thread.schedule_plan is None
    assert segment.thread.is_basic is True
    assert segment.thread.displace_yabus is None

    assert segment.number == 'SU 1'
    assert segment.title == u'От \u2013 До'
    assert segment.station_from.id == 101
    assert segment.station_to.id == 102

    assert segment.company == company

    assert segment.is_interval is False
    assert segment.is_through_train is False
    assert segment.suburban_facilities is None
    assert segment.arrival_event is None
    assert segment.departure_event is None
    assert segment.arrival_event_key is None
    assert segment.departure_event_key is None
    assert segment.has_train_tariffs is None
    assert segment.stops == ''

    flight = {
        'airlineID': 301,
        'title': 'SU 2',
        'departureStation': 103,
        'arrivalStation': 104,
        'route': [103, 104],
        'transportModelID': 201,
    }

    baris_data = BarisData(BarisResponse([flight], {103, 104}, {301}, {201}))
    segment = SearchAviaSegment(flight, baris_data)

    assert segment.thread.number == 'SU 2'
    assert segment.thread.title == u'От станции \u2013 До станции'

    assert segment.number == 'SU 2'
    assert segment.title == u'От станции \u2013 До станции'
    assert segment.station_from.id == 103
    assert segment.station_to.id == 104


def test_one_day_avia_segment():
    create_station(id=101)
    create_station(id=102)

    flight = {
        'airlineID': 301,
        'title': 'SU 1',
        'departureStation': 101,
        'departureDatetime': '2020-06-01T01:30:00+03:00',
        'arrivalStation': 102,
        'arrivalDatetime': '2020-06-01T04:30:00+05:00',
        'route': [101, 102],
        'transportModelID': 201,
        'codeshares': [{
            'title': 'SU 2',
            'airlineID': 302
        }]
    }

    baris_data = BarisData(BarisResponse([flight], {101, 102}, {}, {}))
    segment = OneDayAviaSegment(flight, baris_data, is_nearest=False)

    assert segment.number == 'SU 1'
    assert segment.station_from.id == 101
    assert segment.station_to.id == 102
    assert not hasattr(segment.transport, 'model')
    assert not hasattr(segment, 'company')

    assert segment.departure.isoformat() == '2020-06-01T01:30:00+03:00'
    assert segment.arrival.isoformat() == '2020-06-01T04:30:00+05:00'
    assert segment.duration == timedelta(hours=1)
    assert segment.start_date == date(2020, 6, 1)

    assert len(segment.tariffs_keys) == 1
    assert segment.tariffs_keys[0] == 'daemon SU-1 0601'

    assert len(segment.codeshares) == 1
    assert segment.codeshares[0]['number'] == 'SU 2'
    assert segment.codeshares[0]['tariffs_keys'] == ['daemon SU-2 0601']

    segment = OneDayAviaSegment(flight, baris_data, is_nearest=True)
    assert len(segment.tariffs_keys) == 1
    assert segment.tariffs_keys[0] == 'SU 1'


@replace_now('2020-06-01')
def test_all_days_avia_segment():
    create_station(id=101, time_zone='Europe/Moscow')
    create_station(id=102)

    flight = {
        'airlineID': 301,
        'title': 'SU 1',
        'departureStation': 101,
        'departure': parser.parse('2020-06-01T01:30:00+03:00'),
        'arrivalStation': 102,
        'arrival': parser.parse('2020-06-01T04:30:00+05:00'),
        'route': [101, 102],
        'transportModelID': 201,
        'daysText': 'ежедневно',
        'runDays': {
            '2020': {'6': [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]}
        }
    }

    baris_data = BarisData(BarisResponse([flight], {101, 102}, {}, {}))
    segment = AllDaysAviaSegment(flight, baris_data)

    assert segment.number == 'SU 1'
    assert segment.station_from.id == 101
    assert segment.station_to.id == 102
    assert not hasattr(segment.transport, 'model')
    assert not hasattr(segment, 'company')

    assert segment.departure.isoformat() == '2020-06-01T01:30:00+03:00'
    assert segment.arrival.isoformat() == '2020-06-01T04:30:00+05:00'
    assert segment.duration == timedelta(hours=1)
    assert segment.start_date == date(2020, 6, 1)

    assert segment.days_by_tz == {'Europe/Moscow': {'days_text': 'ежедневно'}}
    assert segment.run_days == {
        '2020': {'6': [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]}
    }

    assert len(segment.tariffs_keys) == 1
    assert segment.tariffs_keys[0] == 'SU 1'
