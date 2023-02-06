# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytz
import pytest
from django.http import QueryDict
from hamcrest import has_entries, assert_that

from common.tester.utils.datetime import replace_now
from common.tester.factories import create_station
from common.models.transport import TransportType
from travel.rasp.morda_backend.morda_backend.search.search.data_layer.all_types_search import (
    AllTypesOneDaySearch, AllTypesNearestSearch, AllTypesAllDaysSearch
)

from travel.rasp.morda_backend.morda_backend.search.search.data_layer.base_search import SearchResult
from travel.rasp.morda_backend.morda_backend.search.search.serialization.request_serialization import ContextQuerySchema


pytestmark = [pytest.mark.dbuser]


class _MockSegment(object):
    def __init__(self, naive_departure, t_type):
        self.departure = pytz.timezone('Etc/GMT-3').localize(naive_departure)
        self.t_type = t_type


@replace_now('2020-06-01')
def test_merge_results():
    station1 = create_station(id=101)
    station2 = create_station(id=102)

    params_dict = {
        'pointFrom': 's101',
        'pointTo': 's102'
    }
    query_dict = QueryDict(mutable=True)
    query_dict.update(params_dict)
    context, _ = ContextQuerySchema().load(query_dict)

    rasp_db_segments = [
        _MockSegment(datetime(2020, 6, 1, 1, 0, 0), TransportType.get_train_type()),
        _MockSegment(datetime(2020, 6, 1, 11, 0, 0), TransportType.get_bus_type())
    ]
    rasp_db_latest_datetime = pytz.timezone('Etc/GMT-3').localize(datetime(2020, 6, 2, 4, 0, 0))
    rasp_db_canonical = {
        'point_from': 'c1',
        'point_to': 'c2',
        'transport_type': None
    }
    rasp_sb_result = SearchResult(rasp_db_segments, {'train', 'bus'}, rasp_db_latest_datetime, rasp_db_canonical)

    baris_segments = [
        _MockSegment(datetime(2020, 6, 1, 5, 0, 0), TransportType.get_plane_type()),
        _MockSegment(datetime(2020, 6, 1, 15, 0, 0), TransportType.get_plane_type()),
    ]
    baris_latest_datetime = pytz.timezone('Etc/GMT-3').localize(datetime(2020, 6, 2, 4, 0, 0))
    baris_canonical = {
        'point_from': 'c1',
        'point_to': 'c2',
        'transport_type': {'plane'}
    }
    baris_result = SearchResult(baris_segments, {'plane'}, baris_latest_datetime, baris_canonical)

    search = AllTypesOneDaySearch(context)
    result = search.merge_results(context, rasp_sb_result, baris_result)

    assert result.transport_types == {'plane', 'train', 'bus'}
    assert result.latest_datetime == rasp_db_latest_datetime
    assert_that(result.canonical, has_entries({
        'point_from': station1,
        'point_to': station2,
        'transport_type': None
    }))

    segments = result.segments
    assert len(segments) == 4
    assert segments[0].departure.isoformat() == '2020-06-01T01:00:00+03:00'
    assert segments[1].departure.isoformat() == '2020-06-01T05:00:00+03:00'
    assert segments[2].departure.isoformat() == '2020-06-01T11:00:00+03:00'
    assert segments[3].departure.isoformat() == '2020-06-01T15:00:00+03:00'

    search = AllTypesNearestSearch(context)
    result = search.merge_results(context, rasp_sb_result, baris_result)

    assert result.transport_types == {'plane', 'train', 'bus'}
    assert result.latest_datetime.isoformat() == '2020-06-01T15:00:00+03:00'
    assert result.canonical is None

    segments = result.segments
    assert len(segments) == 4
    assert segments[0].departure.isoformat() == '2020-06-01T01:00:00+03:00'
    assert segments[1].departure.isoformat() == '2020-06-01T05:00:00+03:00'
    assert segments[2].departure.isoformat() == '2020-06-01T11:00:00+03:00'
    assert segments[3].departure.isoformat() == '2020-06-01T15:00:00+03:00'

    rasp_db_segments.append(_MockSegment(datetime(2020, 6, 2, 8, 0, 0), TransportType.get_train_type()))
    baris_segments.append(_MockSegment(datetime(2020, 6, 2, 7, 0, 0), TransportType.get_plane_type()))

    search = AllTypesAllDaysSearch(context)
    result = search.merge_results(context, rasp_sb_result, baris_result)

    assert result.transport_types == {'plane', 'train', 'bus'}
    assert result.latest_datetime is None
    assert_that(result.canonical, has_entries({
        'point_from': station1,
        'point_to': station2,
        'transport_type': None
    }))

    segments = result.segments
    assert len(segments) == 6
    assert segments[0].departure.isoformat() == '2020-06-01T01:00:00+03:00'
    assert segments[1].departure.isoformat() == '2020-06-01T05:00:00+03:00'
    assert segments[2].departure.isoformat() == '2020-06-02T07:00:00+03:00'
    assert segments[3].departure.isoformat() == '2020-06-02T08:00:00+03:00'
    assert segments[4].departure.isoformat() == '2020-06-01T11:00:00+03:00'
    assert segments[5].departure.isoformat() == '2020-06-01T15:00:00+03:00'
