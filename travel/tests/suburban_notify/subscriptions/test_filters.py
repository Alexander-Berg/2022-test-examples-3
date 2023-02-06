# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta, time

import pytest
import pytz
from hamcrest import assert_that, contains_inanyorder, has_properties

from common.tester.factories import create_change_thread, create_settlement
from common.tester.utils.datetime import replace_now
from travel.rasp.info_center.info_center.suburban_notify.subscriptions.filters import get_subscriptions_filtered
from travel.rasp.info_center.tests.suburban_notify.utils import (
    create_subscription, create_station, create_thread, BaseNotificationTest
)


pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


class TestFilters(BaseNotificationTest):
    def test_filters(self):
        ekb_tz = pytz.timezone('Asia/Yekaterinburg')
        st_1, st_2 = create_station(id=111), create_station(id=222),
        st_3, st_4 = create_station(id=333, time_zone=ekb_tz), create_station(id=444)
        create_settlement(id=500)
        sub_data_1 = {
            'point_from_key': 's111',
            'point_to_key': 's222',
            'interval_from': 500,
            'interval_to': 1400
        }
        sub_data_2 = {
            'point_from_key': 's333',
            'point_to_key': 's222',
            'interval_from': 100,
            'interval_to': 200
        }
        sub_data_3 = {
            'point_from_key': 's111',
            'point_to_key': 's222',
            'interval_from': 100,
            'interval_to': 200
        }
        sub_data_4 = {
            'point_from_key': 'c500',
            'point_to_key': 'c600',
            'interval_from': 500,
            'interval_to': 1400
        }
        sub_data_5 = {
            'point_from_key': 's444',
            'point_to_key': 's333',
            'interval_from': 500,
            'interval_to': 1400
        }
        create_subscription(uid='123', **sub_data_1)
        create_subscription(uid='456', **sub_data_2)
        create_subscription(uid='789', **sub_data_3)
        create_subscription(uid='012', **sub_data_4)
        create_subscription(uid='012', **sub_data_5)

        day = datetime(2019, 5, 12)
        thread_data_1 = create_thread(
            schedule_v1=[
                [None, 0, st_1],
                [120, None, st_2],
            ],
            year_days=[day, day + timedelta(days=1)],
            tz_start_time=time(13, 30)
        )

        create_change_thread(
            thread_data_1,
            [day],
            tz_start_time=time(13, 40),
            changes={
                st_1: {'departure': +10},
                st_2: {'arrival': +10},
            }
        )

        thread_data_5 = create_thread(
            schedule_v1=[
                [None, 0, st_4],
                [120, None, st_3],
            ],
            year_days=[day, day + timedelta(days=1)],
            tz_start_time=time(13, 30)
        )

        with replace_now(datetime(2019, 5, 12, 12, 40)):
            subs = get_subscriptions_filtered(day, 700, 800)

        assert_that(subs, contains_inanyorder(
            has_properties({'key': {'point_to_key': 's222', 'point_from_key': 's111', 'uid': '123'}}),
            has_properties({'key': {'point_to_key': 's222', 'point_from_key': 's111', 'uid': '789'}}),
            has_properties({'key': {'point_to_key': 'c600', 'point_from_key': 'c500', 'uid': '012'}})
        ))

        create_change_thread(
            thread_data_5,
            [day],
            tz_start_time=time(13, 40),
            changes={
                st_1: {'departure': +10},
                st_2: {'arrival': +10},
            }
        )
        with replace_now(datetime(2019, 5, 12, 12, 40)):
            subs = get_subscriptions_filtered(day)

        assert_that(subs, contains_inanyorder(
            has_properties({'key': {'point_to_key': 's222', 'point_from_key': 's111', 'uid': '123'}}),
            has_properties({'key': {'point_to_key': 's222', 'point_from_key': 's111', 'uid': '789'}}),
            has_properties({'key': {'point_to_key': 'c600', 'point_from_key': 'c500', 'uid': '012'}}),
            has_properties({'key': {'point_to_key': 's222', 'point_from_key': 's333', 'uid': '456'}}),
            has_properties({'key': {'point_to_key': 's333', 'point_from_key': 's444', 'uid': '012'}})
        ))
