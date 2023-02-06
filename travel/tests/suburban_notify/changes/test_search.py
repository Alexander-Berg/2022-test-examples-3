# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, time

import pytest
from hamcrest import assert_that, contains_inanyorder, has_properties, has_entries

from common.tester.utils.datetime import replace_now
from travel.rasp.info_center.info_center.suburban_notify.changes.models import SubscriptionChanges
from travel.rasp.info_center.info_center.suburban_notify.search import (
    Searcher, get_mczk_stations, get_znr_segments, filter_znr_segments
)
from travel.rasp.info_center.tests.suburban_notify.utils import create_station, create_thread, BaseNotificationTest

# dbripper нужен, т.к. в тестах мы создаем объекты в базе, которые должны быть
# доступны в форкнутых процессах и на новом соединении с базой.
# Соответственно, обычные наши транзакции в тестах не подходят - форки не видят данные незакоммиченной транзакции
pytestmark = [pytest.mark.mongouser, pytest.mark.dbripper]


class TestSearcher(BaseNotificationTest):
    @replace_now(datetime(2019, 5, 12))
    def test_precalc_searches(self):
        st_1, st_2, st_3 = create_station(id='111'), create_station(id='222'), create_station(id='333')

        sub_changes_1 = SubscriptionChanges(
            calc_date=datetime(2019, 5, 12),
            uid='123',
            point_from_key='s111',
            point_to_key='s222',
            interval_from=0,
            interval_to=1440,
        )
        sub_changes_2 = SubscriptionChanges(
            calc_date=datetime(2019, 5, 12),
            uid='123',
            point_from_key='s111',
            point_to_key='s333',
            interval_from=0,
            interval_to=1440,
        )
        subs = [sub_changes_1, sub_changes_2]

        create_thread(
            year_days=[datetime(2019, 5, 12)],
            tz_start_time=time(12, 30),
            schedule_v1=[
                [None, 0, st_1, {'id': 21}],
                [20, None, st_2, {'id': 22}],
            ],
        )
        create_thread(
            year_days=[datetime(2019, 5, 12)],
            tz_start_time=time(15),
            schedule_v1=[
                [None, 0, st_1, {'id': 31}],
                [20, None, st_3, {'id': 32}],
            ],
        )

        searcher = Searcher()
        searcher.precalc_searches(subs, pool_size=2)

        assert_that(searcher.searches_cache, has_entries({
            ('s111', 's222'): has_entries({
                'point_to_old': 's222',
                'point_to_new': 's222',
                'point_from_old': 's111',
                'point_from_new': 's111',
                'segments': [[21, 22]]
            }),
            ('s111', 's333'): has_entries({
                'point_to_old': 's333',
                'point_to_new': 's333',
                'point_from_old': 's111',
                'point_from_new': 's111',
                'segments': [[31, 32]]
            })
        }))

    @replace_now(datetime(2019, 5, 12))
    def test_search(self):
        st_1, st_2, st_3 = create_station(id='111'), create_station(id='222'), create_station(id='333')

        create_thread(
            year_days=[datetime(2019, 5, 12)],
            tz_start_time=time(12, 30),
            schedule_v1=[
                [None, 0, st_1, {'id': 21}],
                [20, None, st_2, {'id': 22}],
            ],
        )
        create_thread(
            year_days=[datetime(2019, 5, 12)],
            tz_start_time=time(15),
            schedule_v1=[
                [None, 0, st_1, {'id': 31}],
                [20, None, st_3, {'id': 32}],
            ],
        )

        searcher = Searcher()
        segments = searcher.search('s111', 's222')
        assert segments == {
            'point_to_old': 's222',
            'point_to_new': 's222',
            'point_from_old': 's111',
            'point_from_new': 's111',
            'segments': [[21, 22]]
        }

        searcher.precached = True
        searcher.searches_cache = {
            ('s111', 's222'):  {'fake': 'fake'}
        }
        segments = searcher.search('s111', 's222')
        assert segments == {'fake': 'fake'}

    def test_get_mczk_stations(self):
        st_1, st_2, st_3 = create_station(id='111'), create_station(id='222'), create_station(id='333')

        create_thread(
            uid='MCZK123',
            year_days=[datetime(2019, 5, 12)],
            tz_start_time=time(12, 30),
            schedule_v1=[
                [None, 0, st_1],
                [20, None, st_2],
            ],
        )
        create_thread(
            year_days=[datetime(2019, 5, 12)],
            tz_start_time=time(15),
            schedule_v1=[
                [None, 0, st_1],
                [20, None, st_3],
            ],
        )

        stations = get_mczk_stations()
        assert stations == {111, 222}

    def test_get_znr_segments(self):
        st_1, st_2, st_3 = create_station(id='111'), create_station(id='222'), create_station(id='333')

        th = create_thread(
            year_days=[datetime(2019, 5, 12)],
            tz_start_time=time(12, 30),
            schedule_v1=[
                [None, 0, st_1, {'id': 21}],
                [20, None, st_2, {'id': 22}],
            ],
        )

        segments = get_znr_segments([[st_1, st_2], [st_3, st_1]])

        assert_that(segments, has_entries({
            (st_1, st_2): contains_inanyorder(th),
            (st_3, st_1): []
        }))

    def test_get_threads_from_znoderoute(self):
        st_1, st_2, st_3 = create_station(id='111'), create_station(id='222'), create_station(id='333')

        th = create_thread(
            year_days=[datetime(2019, 5, 12)],
            tz_start_time=time(12, 30),
            schedule_v1=[
                [None, 0, st_1, {'id': 21}],
                [20, None, st_2, {'id': 22}],
            ]
        )
        th.station_from_id = 111
        th.station_to_id = 222
        th.rtstation_from_id = 21
        th.rtstation_to_id = 22
        th.stops_translations = {'ru': ''}

        pp_threads = {
            (st_1, st_2): [th],
            (st_2, st_3): []
        }

        segments = filter_znr_segments(pp_threads)

        assert_that(segments, has_entries({
            (st_1, st_2): contains_inanyorder(
                has_properties({
                    'thread': th
                })
            ),
            (st_2, st_3): []
        }))
