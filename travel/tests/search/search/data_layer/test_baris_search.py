# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, timedelta

import pytest
from django.http import QueryDict

from common.data_api.baris.test_helpers import mock_baris_response
from common.tester.factories import create_station, create_company, create_settlement, create_transport_model
from common.tester.utils.datetime import replace_now
from common.models.transport import TransportType

from travel.rasp.morda_backend.morda_backend.search.search.data_layer.baris_search import (
    BarisOneDaySearch, BarisNearestSearch, BarisAllDaysSearch
)
from travel.rasp.morda_backend.morda_backend.search.search.serialization.request_serialization import ContextQuerySchema


pytestmark = [pytest.mark.dbuser]

create_plane_station = create_station.mutate(t_type=TransportType.PLANE_ID, time_zone='Etc/GMT-3', type_choices='tablo')


def _create_db_items():
    settlement1 = create_settlement(id=91, title='От', slug='ot', time_zone='Etc/GMT-3')
    settlement2 = create_settlement(id=92, title='До', slug='do', time_zone='Etc/GMT-3')
    create_plane_station(id=101, settlement=settlement1, title='от')
    create_plane_station(id=102, settlement=settlement2, title='до')
    create_plane_station(id=111, settlement=settlement1, title='от от')
    create_plane_station(id=112, settlement=settlement2, title='до до')
    create_company(id=301, title='Компания1', url='url1', yandex_avia_code='Company1')
    create_company(id=302, title='Компания2', url='url2', yandex_avia_code='Company2')
    create_company(id=303, title='Компания3', url='url3', yandex_avia_code='Company3')
    create_transport_model(id=201, title='Самолет1')


def _get_search_result(baris_response, when=None, nearest=False):
    with mock_baris_response(baris_response):
        params_dict = {
            'pointFrom': 'c91',
            'pointTo': 'c92',
            'transportType': 'plane'
        }
        if when:
            params_dict['when'] = when

        query_dict = QueryDict(mutable=True)
        query_dict.update(params_dict)
        context, _ = ContextQuerySchema().load(query_dict)

        if nearest:
            search = BarisNearestSearch(context)
        elif when:
            search = BarisOneDaySearch(context)
        else:
            search = BarisAllDaysSearch(context)
        return search.find_segments(context)


ONE_DAY_P2P_BARIS_RESPONSE = {
    'departureStations': [101, 111],
    'arrivalStations': [102, 112],
    'flights': [
        {
            'airlineID': 301,
            'title': 'SU 1',
            'departureDatetime': '2020-06-01T01:00:00+03:00',
            'departureStation': 101,
            'arrivalDatetime': '2020-06-01T03:00:00+03:00',
            'arrivalStation': 102,
            'transportModelID': 201,
            'route': [101, 102],
            'codeshares': [
                {
                    'title': 'SU 12',
                    'airlineID': 302,
                },
                {
                    'title': 'SU 13',
                    'airlineID': 303,
                }
            ]
        },
        {
            'airlineID': 301,
            'title': 'SU 2',
            'departureDatetime': '2020-06-01T11:00:00+03:00',
            'departureStation': 111,
            'arrivalDatetime': '2020-06-01T13:00:00+03:00',
            'arrivalStation': 112,
            'transportModelID': 201,
            'route': [111, 112],
        },
        {
            'airlineID': 301,
            'title': 'SU 3',
            'departureDatetime': '2020-06-02T02:00:00+03:00',
            'departureStation': 101,
            'arrivalDatetime': '2020-06-02T04:00:00+03:00',
            'arrivalStation': 112,
            'transportModelID': 201,
            'route': [101, 112],
        }
    ]
}


@replace_now('2020-06-02')
def test_one_day_search():
    _create_db_items()

    result = _get_search_result({}, '2020-06-03')

    assert result.transport_types == set()
    assert result.latest_datetime.isoformat() == '2020-06-04T04:00:00+03:00'
    assert result.canonical is None
    assert len(result.segments) == 0

    result = _get_search_result({}, 'today')

    assert result.transport_types == set()
    assert result.latest_datetime.isoformat() == '2020-06-03T04:00:00+03:00'
    assert result.canonical is None
    assert len(result.segments) == 0

    result = _get_search_result({}, 'tomorrow')

    assert result.transport_types == set()
    assert result.latest_datetime.isoformat() == '2020-06-04T04:00:00+03:00'
    assert result.canonical is None
    assert len(result.segments) == 0

    result = _get_search_result(ONE_DAY_P2P_BARIS_RESPONSE, '2020-06-01')

    assert result.transport_types == {'plane'}
    assert result.latest_datetime.isoformat() == '2020-06-02T04:00:00+03:00'
    assert result.canonical is not None
    assert result.canonical['transport_type'] == 'plane'
    assert result.canonical['point_from'].id == 91
    assert result.canonical['point_to'].id == 92
    assert len(result.segments) == 3

    segment = result.segments[0]

    assert segment.station_from.id == 101
    assert segment.station_to.id == 102
    assert segment.number == 'SU 1'
    assert segment.title == 'От \u2013 До'
    assert segment.thread.number == 'SU 1'
    assert segment.thread.title == 'От \u2013 До'

    assert segment.transport.id == 2
    assert segment.transport.code == 'plane'
    assert segment.transport.title == 'Самолёт'
    assert segment.transport.model.title == 'Самолет1'
    assert segment.company.title == 'Компания1'

    assert segment.departure.isoformat() == '2020-06-01T01:00:00+03:00'
    assert segment.arrival.isoformat() == '2020-06-01T03:00:00+03:00'
    assert segment.duration == timedelta(hours=2)
    assert segment.start_date == date(2020, 6, 1)
    assert segment.tariffs_keys == ['daemon SU-1 0601']

    assert len(segment.codeshares) == 2
    assert segment.codeshares[0]['number'] == 'SU 12'
    assert segment.codeshares[0]['tariffs_keys'] == ['daemon SU-12 0601']
    assert segment.codeshares[0]['company'].id == 302
    assert segment.codeshares[1]['number'] == 'SU 13'
    assert segment.codeshares[1]['tariffs_keys'] == ['daemon SU-13 0601']
    assert segment.codeshares[1]['company'].id == 303

    segment = result.segments[1]

    assert segment.station_from.id == 111
    assert segment.station_to.id == 112
    assert segment.number == 'SU 2'
    assert segment.title == 'От \u2013 До'
    assert segment.thread.number == 'SU 2'
    assert segment.thread.title == 'От \u2013 До'

    assert segment.transport.id == 2
    assert segment.transport.code == 'plane'
    assert segment.transport.title == 'Самолёт'
    assert segment.transport.model.title == 'Самолет1'
    assert segment.company.title == 'Компания1'

    assert segment.departure.isoformat() == '2020-06-01T11:00:00+03:00'
    assert segment.arrival.isoformat() == '2020-06-01T13:00:00+03:00'
    assert segment.duration == timedelta(hours=2)
    assert segment.start_date == date(2020, 6, 1)
    assert segment.tariffs_keys == ['daemon SU-2 0601']

    segment = result.segments[2]

    assert segment.station_from.id == 101
    assert segment.station_to.id == 112
    assert segment.number == 'SU 3'
    assert segment.title == 'От \u2013 До'
    assert segment.thread.number == 'SU 3'
    assert segment.thread.title == 'От \u2013 До'

    assert segment.transport.id == 2
    assert segment.transport.code == 'plane'
    assert segment.transport.title == 'Самолёт'
    assert segment.transport.model.title == 'Самолет1'
    assert segment.company.title == 'Компания1'

    assert segment.departure.isoformat() == '2020-06-02T02:00:00+03:00'
    assert segment.arrival.isoformat() == '2020-06-02T04:00:00+03:00'
    assert segment.duration == timedelta(hours=2)
    assert segment.start_date == date(2020, 6, 2)
    assert segment.tariffs_keys == ['daemon SU-3 0602']


@replace_now('2020-06-01')
def test_nearest_search():
    _create_db_items()

    result = _get_search_result({}, when=None, nearest=True)

    assert result.transport_types == set()
    assert result.latest_datetime is None
    assert result.canonical is None
    assert len(result.segments) == 0

    result = _get_search_result(ONE_DAY_P2P_BARIS_RESPONSE, when=None, nearest=True)

    assert result.transport_types == {'plane'}
    assert result.latest_datetime.isoformat() == '2020-06-02T02:00:00+03:00'
    assert result.canonical is None
    assert len(result.segments) == 3

    segment = result.segments[0]

    assert segment.station_from.id == 101
    assert segment.station_to.id == 102
    assert segment.number == 'SU 1'
    assert segment.title == 'От \u2013 До'
    assert segment.thread.number == 'SU 1'
    assert segment.thread.title == 'От \u2013 До'

    assert segment.transport.id == 2
    assert segment.transport.code == 'plane'
    assert segment.transport.title == 'Самолёт'
    assert segment.transport.model.title == 'Самолет1'
    assert segment.company.title == 'Компания1'

    assert segment.departure.isoformat() == '2020-06-01T01:00:00+03:00'
    assert segment.arrival.isoformat() == '2020-06-01T03:00:00+03:00'
    assert segment.duration == timedelta(hours=2)
    assert segment.start_date == date(2020, 6, 1)
    assert segment.tariffs_keys == ['SU 1']
    assert not hasattr(segment, 'codeshares')

    segment = result.segments[1]

    assert segment.station_from.id == 111
    assert segment.station_to.id == 112
    assert segment.number == 'SU 2'
    assert segment.title == 'От \u2013 До'
    assert segment.thread.number == 'SU 2'
    assert segment.thread.title == 'От \u2013 До'

    assert segment.transport.id == 2
    assert segment.transport.code == 'plane'
    assert segment.transport.title == 'Самолёт'
    assert segment.transport.model.title == 'Самолет1'
    assert segment.company.title == 'Компания1'

    assert segment.departure.isoformat() == '2020-06-01T11:00:00+03:00'
    assert segment.arrival.isoformat() == '2020-06-01T13:00:00+03:00'
    assert segment.duration == timedelta(hours=2)
    assert segment.start_date == date(2020, 6, 1)
    assert segment.tariffs_keys == ['SU 2']

    segment = result.segments[2]

    assert segment.station_from.id == 101
    assert segment.station_to.id == 112
    assert segment.number == 'SU 3'
    assert segment.title == 'От \u2013 До'
    assert segment.thread.number == 'SU 3'
    assert segment.thread.title == 'От \u2013 До'

    assert segment.transport.id == 2
    assert segment.transport.code == 'plane'
    assert segment.transport.title == 'Самолёт'
    assert segment.transport.model.title == 'Самолет1'
    assert segment.company.title == 'Компания1'

    assert segment.departure.isoformat() == '2020-06-02T02:00:00+03:00'
    assert segment.arrival.isoformat() == '2020-06-02T04:00:00+03:00'
    assert segment.duration == timedelta(hours=2)
    assert segment.start_date == date(2020, 6, 2)
    assert segment.tariffs_keys == ['SU 3']


ALL_DAYS_P2P_BARIS_RESPONSE = {
    'departureStations': [101, 111],
    'arrivalStations': [102, 112],
    'flights': [
        {
            'airlineID': 301,
            'title': 'SU 1',
            'departureTime': '01:00',
            'departureStation': 101,
            'arrivalTime': '03:00',
            'arrivalStation': 102,
            'arrivalDayShift': 0,
            'transportModelID': 201,
            'route': [101, 102],
            'masks': [
                {
                    'from': '2020-06-02',
                    'until': '2020-06-02',
                    'on': 2
                }
            ]
        },
        {
            'airlineID': 301,
            'title': 'SU 2',
            'departureTime': '11:00',
            'departureStation': 111,
            'arrivalTime': '13:00',
            'arrivalStation': 112,
            'arrivalDayShift': 0,
            'transportModelID': 201,
            'route': [111, 112],
            'masks': [
                {
                    'from': '2020-06-03',
                    'until': '2020-06-03',
                    'on': 3
                }
            ]
        }
    ]
}


@replace_now('2020-06-01')
def test_all_days_search():
    _create_db_items()

    result = _get_search_result({}, when=None, nearest=False)

    assert result.transport_types == set()
    assert result.latest_datetime is None
    assert result.canonical is None
    assert len(result.segments) == 0

    result = _get_search_result(ALL_DAYS_P2P_BARIS_RESPONSE, when=None, nearest=False)

    assert result.transport_types == {'plane'}
    assert result.latest_datetime is None
    assert result.canonical is not None
    assert result.canonical['transport_type'] == 'plane'
    assert result.canonical['point_from'].id == 91
    assert result.canonical['point_to'].id == 92
    assert len(result.segments) == 2

    segment = result.segments[0]

    assert segment.station_from.id == 101
    assert segment.station_to.id == 102
    assert segment.number == 'SU 1'
    assert segment.title == 'От \u2013 До'
    assert segment.thread.number == 'SU 1'
    assert segment.thread.title == 'От \u2013 До'

    assert segment.transport.id == 2
    assert segment.transport.code == 'plane'
    assert segment.transport.title == 'Самолёт'
    assert segment.transport.model.title == 'Самолет1'
    assert segment.company.title == 'Компания1'

    assert segment.departure.isoformat() == '2020-06-02T01:00:00+03:00'
    assert segment.arrival.isoformat() == '2020-06-02T03:00:00+03:00'
    assert segment.duration == timedelta(hours=2)
    assert segment.start_date == date(2020, 6, 2)
    assert segment.tariffs_keys == ['SU 1']

    segment = result.segments[1]

    assert segment.station_from.id == 111
    assert segment.station_to.id == 112
    assert segment.number == 'SU 2'
    assert segment.title == 'От \u2013 До'
    assert segment.thread.number == 'SU 2'
    assert segment.thread.title == 'От \u2013 До'

    assert segment.transport.id == 2
    assert segment.transport.code == 'plane'
    assert segment.transport.title == 'Самолёт'
    assert segment.transport.model.title == 'Самолет1'
    assert segment.company.title == 'Компания1'

    assert segment.departure.isoformat() == '2020-06-03T11:00:00+03:00'
    assert segment.arrival.isoformat() == '2020-06-03T13:00:00+03:00'
    assert segment.duration == timedelta(hours=2)
    assert segment.start_date == date(2020, 6, 3)
    assert segment.tariffs_keys == ['SU 2']
