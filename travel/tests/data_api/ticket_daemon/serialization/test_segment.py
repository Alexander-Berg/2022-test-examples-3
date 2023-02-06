# -*- coding: utf-8 -*-

from datetime import datetime, timedelta

import pytest
import pytz
from django.utils import translation

from common.data_api.ticket_daemon.factories import create_segment
from common.data_api.ticket_daemon.serialization.segment import Segment, SegmentSchema
from common.models.transport import TransportType
from common.tester.factories import create_company, create_station


def test_Segment_new():
    with pytest.raises(TypeError) as excinfo:
        Segment(foo=1)

    assert excinfo.value[0].endswith('got an unexpected keyword argument \'foo\'')

    segment = create_segment()

    assert segment.display_t_code is None
    assert segment.supplier_code is None
    assert segment.number is None
    assert segment.station_from is None
    assert segment.station_to is None
    assert segment.departure is None
    assert segment.arrival is None
    assert segment.company is None
    assert segment.electronic_ticket is None
    assert segment.first_station is None
    assert segment.last_station is None
    assert segment.number_of_variants is None
    assert segment.duration_in_seconds is None


@pytest.mark.dbuser
def test_SegmentSchema_load():
    station_from = create_station()
    station_to = create_station()
    company = create_company()

    schema = SegmentSchema(context={
        'stations': {station_from.id: station_from, station_to.id: station_to},
    })
    assert schema.load({'key': 'segment key'}).data == create_segment()

    segment_data = {
        'key': 'segment key',
        't_type_code': 'plane',
        'supplier_code': 'supplier code',
        'number': 'plane number',
        'station_from': station_from.id,
        'station_to': station_to.id,
        'departure': {'tzname': 'UTC', 'local': '2000-01-01 12:00:00'},
        'arrival': {'tzname': 'UTC', 'local': '2000-01-02 12:00:00'},
        'company': None,
        'electronic_ticket': None,
        'first_station': None,
        'last_station': None,
        'number_of_variants': None,
        'duration_in_seconds': None,
    }

    assert schema.load(segment_data).data == create_segment(
        display_t_code='plane',
        supplier_code='supplier code',
        number='plane number',
        station_from=station_from,
        station_to=station_to,
        departure=datetime(2000, 1, 1, 12, tzinfo=pytz.UTC),
        arrival=datetime(2000, 1, 2, 12, tzinfo=pytz.UTC),
    )

    assert schema.load(dict(segment_data, **{
        'company': company.id,
        'electronic_ticket': True,
        'first_station': station_to.id,
        'last_station': station_from.id,
        'number_of_variants': 100,
        'duration_in_seconds': 1000,
        'url': 'ya.test.url',
    })).data == create_segment(
        display_t_code='plane',
        supplier_code='supplier code',
        number='plane number',
        station_from=station_from,
        station_to=station_to,
        departure=datetime(2000, 1, 1, 12, tzinfo=pytz.UTC),
        arrival=datetime(2000, 1, 2, 12, tzinfo=pytz.UTC),
        company=company,
        electronic_ticket=True,
        first_station=station_to,
        last_station=station_from,
        number_of_variants=100,
        duration_in_seconds=1000,
        url='ya.test.url',
    )


@pytest.mark.dbuser
def test_Segment_t_type():
    plane = TransportType.objects.get(id=TransportType.PLANE_ID)
    assert create_segment(display_t_code='plane').t_type == plane


@pytest.mark.dbuser
@translation.override('ru')
def test_Segment_L_title():
    station_from = create_station(title=u'Станция отправления')
    station_to = create_station(title=u'Станция прибытия')
    segment = create_segment(station_from=station_from, station_to=station_to)

    assert segment.L_title() == u'Станция отправления \N{em dash} Станция прибытия'

    station_from = create_station(title=u'Станция отправления', settlement={'title': u'Город отправления'})
    station_to = create_station(title=u'Станция прибытия', settlement={'title': u'Город прибытия'})
    segment = create_segment(station_from=station_from, station_to=station_to)

    assert segment.L_title() == u'Город отправления \N{em dash} Город прибытия'


@pytest.mark.dbuser
@translation.override('ru')
def test_Segment_get_popular_title():
    station_from = create_station(title=u'Станция отправления', popular_title=u'Станция 1')
    station_to = create_station(title=u'Станция прибытия', popular_title=u'Станция 2')
    segment = Segment(station_from=station_from, station_to=station_to)

    assert segment.get_popular_title() == u'Станция 1 \N{em dash} Станция 2'


def test_Segment_msk_departure():
    assert create_segment().msk_departure is None
    assert create_segment(departure=datetime(2000, 1, 1, 12, tzinfo=pytz.UTC)).msk_departure == datetime(2000, 1, 1, 15)


def test_Segment_msk_arrival():
    assert create_segment().msk_arrival is None
    assert create_segment(arrival=datetime(2000, 1, 1, 12, tzinfo=pytz.UTC)).msk_arrival == datetime(2000, 1, 1, 15)


def test_Segment_duration():
    assert create_segment().duration is None
    assert create_segment(departure=datetime(2000, 1, 1, 12, tzinfo=pytz.UTC)).duration is None
    assert create_segment(arrival=datetime(2000, 1, 1, 12, tzinfo=pytz.UTC)).duration is None
    assert create_segment(
        departure=datetime(2000, 1, 1, 12, tzinfo=pytz.UTC),
        arrival=datetime(2000, 1, 2, 12, tzinfo=pytz.UTC)
    ).duration == timedelta(days=1)
