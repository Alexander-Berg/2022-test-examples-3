# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, date

import pytz
import pytest
from django.http import QueryDict

from common.tester.factories import create_station, create_settlement, create_thread, create_country
from common.tester.utils.datetime import replace_now
from common.models.transport import TransportType

from travel.rasp.morda_backend.morda_backend.search.search.data_layer.rasp_db_search import RaspDbOneDaySearch
from travel.rasp.morda_backend.morda_backend.search.search.data_layer.base_search import BaseSearch
from travel.rasp.morda_backend.morda_backend.search.search.serialization.request_serialization import ContextQuerySchema


pytestmark = [pytest.mark.dbuser]


class _MockSegment(object):
    def __init__(self, naive_departure):
        self.departure = pytz.timezone('Etc/GMT-3').localize(naive_departure)


def _make_context(point_from_key, point_to_key):
    params_dict = {
        'pointFrom': point_from_key,
        'pointTo': point_to_key,
        'when': '2020-06-01'
    }
    query_dict = QueryDict(mutable=True)
    query_dict.update(params_dict)
    context, _ = ContextQuerySchema().load(query_dict)
    return context


@replace_now('2020-06-01')
def test_get_usual_search_latest_datetime():
    create_station(id=101)
    create_station(id=102)
    context = _make_context('s101', 's102')

    latest_datetime = BaseSearch(context).get_usual_search_latest_datetime(context)
    assert latest_datetime.isoformat() == '2020-06-02T04:00:00+03:00'


@replace_now('2020-06-01')
def test_get_nearest_search_latest_datetime():
    create_station(id=101)
    create_station(id=102)
    context = _make_context('s101', 's102')

    latest_datetime = BaseSearch(context).get_nearest_search_latest_datetime([])
    assert latest_datetime is None

    segments = [
        _MockSegment(datetime(2020, 6, 1, 1, 30, 0)),
        _MockSegment(datetime(2020, 6, 1, 18, 30, 0)),
        _MockSegment(datetime(2020, 6, 2, 1, 30, 0))
    ]
    latest_datetime = BaseSearch(context).get_nearest_search_latest_datetime(segments)
    assert latest_datetime.isoformat() == '2020-06-02T01:30:00+03:00'


def _check_extended_search(original_point_from_key, original_point_to_key,
                           point_from_key, point_to_key, has_segments):
    original_context = _make_context(original_point_from_key, original_point_to_key)
    search = RaspDbOneDaySearch(original_context)
    result, context = search._extended_search()

    assert (len(result.segments) > 0) == has_segments
    assert context.point_from.point_key == point_from_key
    assert context.point_to.point_key == point_to_key


@replace_now('2020-06-01')
def test_extended_search():
    country = create_country()
    settlement1 = create_settlement(id=91, country=country)
    settlement2 = create_settlement(id=92, country=country)
    settlement3 = create_settlement(id=93, country=country)
    station_0_1 = create_station(id=101, settlement=settlement1, t_type=TransportType.TRAIN_ID, country=country)
    station_0_2 = create_station(id=102, settlement=settlement2, t_type=TransportType.TRAIN_ID, country=country)
    create_station(id=111, settlement=settlement1, t_type=TransportType.TRAIN_ID, country=country)
    create_station(id=112, settlement=settlement2, t_type=TransportType.TRAIN_ID, country=country)
    create_station(id=103, settlement=settlement3, t_type=TransportType.TRAIN_ID, country=country)
    create_thread(
        t_type=TransportType.TRAIN_ID,
        schedule_v1=[[None, 0, station_0_1], [300, None, station_0_2]],
        year_days=[date(2020, 6, 1)], tz_start_time='08:00',
        __={'calculate_noderoute': True}
    )

    _check_extended_search('s101', 's102', 's101', 's102', True)
    _check_extended_search('s101', 'c92', 's101', 'c92', True)
    _check_extended_search('c91', 's102', 'c91', 's102', True)
    _check_extended_search('c91', 'c92', 'c91', 'c92', True)
    _check_extended_search('s111', 's102', 'c91', 's102', True)
    _check_extended_search('s111', 'c92', 'c91', 'c92', True)
    _check_extended_search('s101', 's112', 's101', 'c92', True)
    _check_extended_search('c91', 's112', 'c91', 'c92', True)
    _check_extended_search('s111', 's112', 'c91', 'c92', True)
    _check_extended_search('s103', 's102', 's103', 's102', False)
    _check_extended_search('c93', 's102', 'c93', 's102', False)
    _check_extended_search('s103', 'c92', 's103', 'c92', False)
    _check_extended_search('c93', 'c92', 'c93', 'c92', False)
    _check_extended_search('s101', 's103', 's101', 's103', False)
    _check_extended_search('s101', 'c93', 's101', 'c93', False)
    _check_extended_search('c91', 's103', 'c91', 's103', False)


@replace_now('2020-06-01')
def test_train_number_change():
    station_0 = create_station(id=101, t_type=TransportType.TRAIN_ID)
    station_1 = create_station(id=102, t_type=TransportType.TRAIN_ID)
    station_2 = create_station(id=103, t_type=TransportType.TRAIN_ID)

    create_thread(
        t_type=TransportType.TRAIN_ID,
        number='100A',
        schedule_v1=[
            [None, 0, station_0, {'train_number': '100A'}],
            [100, 120, station_1, {'train_number': '101A'}],
            [300, None, station_2, {'train_number': '101A'}]],
        year_days=[date(2020, 6, 1)],
        __={'calculate_noderoute': True}
    )

    original_context = _make_context('s101', 's103')
    search = RaspDbOneDaySearch(original_context)
    result, context = search._extended_search()

    assert result.segments[0].number == '100A'

    original_context = _make_context('s102', 's103')
    search = RaspDbOneDaySearch(original_context)
    result, context = search._extended_search()

    assert result.segments[0].number == '101A'
