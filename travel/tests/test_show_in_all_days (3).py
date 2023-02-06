# -*- coding: utf-8 -*-

from datetime import datetime, date, timedelta, time

import pytest
from django.conf import settings

from common.models.schedule import RThreadType
from common.tester import transaction_context
from common.tester.factories import create_station, create_thread
from route_search.base import PlainSegmentSearch, IntervalSegmentSearch


@pytest.fixture(scope='module', params=['simple', 'interval'])
def schedule_params(request):
    old_now = getattr(settings, 'ENVIRONMENT_NOW', None)
    settings.ENVIRONMENT_NOW = datetime(2015, 1, 1)

    atomic = transaction_context.enter_atomic()

    schedule = {}

    schedule['station_1'] = create_station()
    schedule['station_2'] = create_station()
    schedule['station_3'] = create_station()

    schedule['thread'] = create_thread({
        '__': {'calculate_noderoute': True},
        'tz_start_time': time(10),
        'schedule_v1': [
            [None,    0, schedule['station_1']],
            [120,  130, schedule['station_2']],
            [240, None, schedule['station_3']],
        ]
    })

    thread = schedule['thread']

    segment_search_cls = PlainSegmentSearch
    if request.param == 'interval':
        thread.type_id = RThreadType.INTERVAL_ID
        thread.begin_time = time(0)
        thread.end_time = time(23)
        thread.save()

        segment_search_cls = IntervalSegmentSearch

    def fin():
        settings.ENVIRONMENT_NOW = old_now

        transaction_context.rollback_atomic(atomic)

    request.addfinalizer(fin)

    return schedule, segment_search_cls


def get_1to2_search(segment_search_cls, schedule, date):
    point_from = schedule['station_1']
    point_to = schedule['station_2']
    from_dt_aware = point_from.pytz.localize(datetime.combine(date, time(0)))
    to_dt_aware = from_dt_aware + timedelta(1)

    search_cls = segment_search_cls
    search = search_cls(point_from, point_to)
    if isinstance(search, IntervalSegmentSearch):
        segments = search.search_by_day(date)
    else:
        segments = search.search(from_dt_aware, to_dt_aware)

    return segments, search.all_days_search()


@pytest.mark.dbuser
def test_notshowinalldayspages(schedule_params):
    schedule, segment_search_cls = schedule_params
    thread = schedule['thread']
    thread.show_in_alldays_pages = False
    thread.save()
    segments, all_day_segments = get_1to2_search(segment_search_cls, schedule, date(2015, 1, 1))

    assert len(segments) == 1
    assert len(all_day_segments) == 0


@pytest.mark.dbuser
def test_showinalldayspages(schedule_params):
    schedule, segment_search_cls = schedule_params
    thread = schedule['thread']
    thread.show_in_alldays_pages = True
    thread.save()
    segments, all_day_segments = get_1to2_search(segment_search_cls, schedule,
                                                 date(2015, 1, 1))

    assert len(segments) == 1
    assert len(all_day_segments) == 1
