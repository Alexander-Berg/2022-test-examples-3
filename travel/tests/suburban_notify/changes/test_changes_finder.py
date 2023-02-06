# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta, time

import pytest
from django.conf import settings
from hamcrest import assert_that, contains_inanyorder, has_properties, contains, has_entries

from common.apps.suburban_events.models import SuburbanKey
from common.db.mongo import databases
from common.tester.factories import create_change_thread
from common.tester.utils.datetime import replace_now
from travel.rasp.info_center.info_center.suburban_notify.changes.changes_finder import ChangesFinderWithStorage, ChangesFinder
from travel.rasp.info_center.info_center.suburban_notify.changes.find import ChangeType
from travel.rasp.info_center.info_center.suburban_notify.changes.models import SubscriptionChanges, Change, ThreadData
from travel.rasp.info_center.info_center.suburban_notify.changes.text import filter_changes
from travel.rasp.info_center.info_center.suburban_notify.subscriptions.models import Subscription, Frequency
from travel.rasp.info_center.tests.suburban_notify.utils import (
    create_changes, create_station, create_thread, BaseNotificationTest
)


pytestmark = [pytest.mark.mongouser, pytest.mark.dbripper]


class TestChangesFinderWithStorage(BaseNotificationTest):
    @replace_now(datetime(2019, 1, 10))
    def test_save_subs_changes_all_sent(self):
        sub_changes = SubscriptionChanges(
            calc_date=datetime(2019, 1, 10),
            uid='123',
            point_from_key='c213',
            point_to_key='s221',
            interval_from=0,
            interval_to=1440,
            changes=[
                Change(
                    type=ChangeType.CHANGED,
                    start_date=datetime(2019, 1, 10),
                    basic_thread=ThreadData(key=11, is_first_run_day=False, uid=220, number=5, title='нитка'),
                    rel_thread=ThreadData(key=21, is_first_run_day=True, uid=221, number=25, title='нитка изменение'),
                    push_sent=True
                ),
                Change(
                    type=ChangeType.CANCELLED,
                    start_date=datetime(2019, 1, 10),
                    basic_thread=ThreadData(key=31, is_first_run_day=True, uid=33, number=10, title='нитка'),
                    rel_thread=ThreadData(key=41, is_first_run_day=False, uid=43, number=30, title='нитка отмены'),
                    push_sent=False
                )
            ],
            subscription=Subscription(frequency=Frequency.EVERY_DAY)
        )

        create_changes(**sub_changes.to_dict())

        finder = ChangesFinderWithStorage(script_run_id='some_id')
        finder.save_subs_changes_all_sent({sub_changes: filter_changes(sub_changes, only_new=False)})
        sub = finder.get_subs_changes(
            uid=sub_changes.uid,
            point_from_key=sub_changes.point_from_key,
            point_to_key=sub_changes.point_to_key
        )[0]

        assert_that(sub, has_properties({
            'changes': contains_inanyorder(
                has_properties({
                    'push_sent': True
                }),
                has_properties({
                    'push_sent': 'some_id'
                })
            )
        }))

        finder = ChangesFinderWithStorage(script_run_id='some_id_2')
        finder.save_subs_changes_all_sent({sub_changes: sub_changes.changes})
        sub = finder.get_subs_changes(
            uid=sub_changes.uid,
            point_from_key=sub_changes.point_from_key,
            point_to_key=sub_changes.point_to_key
        )[0]

        assert_that(sub, has_properties({
            'changes': contains_inanyorder(
                has_properties({
                    'push_sent': 'some_id_2'
                }),
                has_properties({
                    'push_sent': 'some_id_2'
                })
            )
        }))

    def test_process_sub_changes(self):
        sub_changes = SubscriptionChanges(
            calc_date=datetime(2019, 1, 10),
            uid='123',
            point_from_key='c213',
            point_to_key='s221',
            interval_from=0,
            interval_to=1440,
            changes=[
                Change(
                    type=ChangeType.CHANGED,
                    start_date=datetime(2019, 1, 10),
                    push_sent=True
                )
            ]
        )
        create_changes(**sub_changes.to_dict())

        sub_changes_2 = SubscriptionChanges(
            calc_date=datetime(2019, 1, 10),
            uid='123',
            point_from_key='c213',
            point_to_key='s221',
            interval_from=0,
            interval_to=1440,
            changes=[
                Change(
                    type=ChangeType.CHANGED,
                    start_date=datetime(2019, 1, 10),
                    push_sent=False
                ),
                Change(
                    type=ChangeType.CANCELLED,
                    start_date=datetime(2019, 1, 10),
                    push_sent=False
                )
            ]
        )

        finder = ChangesFinderWithStorage(script_run_id='some_id')
        finder.process_sub_changes([sub_changes_2])

        assert_that(sub_changes_2.changes, contains(
            has_properties({
                'push_sent': True,
            }),
            has_properties({
                'push_sent': False,
            })
        ))

    def test_save_subs_changes(self):
        sub_changes = SubscriptionChanges(
            calc_date=datetime(2019, 1, 10),
            uid='123',
            point_from_key='c213',
            point_to_key='s221',
            interval_from=0,
            interval_to=1440,
            changes=[
                Change(
                    type=ChangeType.CHANGED,
                    start_date=datetime(2019, 1, 10),
                    basic_thread=ThreadData(key=11, is_first_run_day=False, uid=555, number=5, title='нитка'),
                    rel_thread=ThreadData(key=21, is_first_run_day=True, uid=556, number=25, title='нитка изменение'),
                    push_sent=True
                )
            ]
        )

        finder = ChangesFinderWithStorage(script_run_id='some_id')
        finder.save_subs_changes([sub_changes])

        key = finder._get_mongo_key(sub_changes)
        sub = databases[settings.SUBURBAN_NOTIFICATION_DATABASE_NAME].subscription_changes.find_one(key)

        assert_that(sub, has_entries({
            'uid': '123',
            'script_run_id': 'some_id',
            'calc_date': datetime(2019, 1, 10),
            'interval_to': 1440,
            'changes': contains_inanyorder(
                has_entries({
                    'basic_thread': has_entries({'uid': 555}),
                    'rel_thread': has_entries({'uid': 556}),
                    'type': 'changed'
                })
            )
        }))

        sub_changes.interval_to = 1500
        finder.save_subs_changes([sub_changes])
        sub = databases[settings.SUBURBAN_NOTIFICATION_DATABASE_NAME].subscription_changes.find_one(key)
        assert databases[settings.SUBURBAN_NOTIFICATION_DATABASE_NAME].subscription_changes.count() == 1
        assert_that(sub, has_entries({'interval_to': 1500}))

        sub_changes.uid = '456'
        finder.save_subs_changes([sub_changes])
        assert databases[settings.SUBURBAN_NOTIFICATION_DATABASE_NAME].subscription_changes.count() == 2
        key = finder._get_mongo_key(sub_changes)
        sub = databases[settings.SUBURBAN_NOTIFICATION_DATABASE_NAME].subscription_changes.find_one(key)
        assert_that(sub, has_entries({'uid': '456'}))


class TestChangesFinder(BaseNotificationTest):
    @replace_now(datetime(2019, 5, 11))
    def test_run(self):
        st_1, st_2 = create_station(id=111), create_station(id=222)

        subscription = Subscription(
            point_from_key='s111',
            point_to_key='s222',
            interval_from=0,
            interval_to=1440,
            uid='123',
            frequency=Frequency.EVERY_DAY
        )

        day = datetime(2019, 5, 12)
        thread = create_thread(
            uid='basic_uid',
            schedule_v1=[
                [None, 0, st_1, {'id': 11}],
                [120, None, st_2, {'id': 12}],
            ],
            year_days=[day, day + timedelta(days=1)],
            tz_start_time=time(13, 30)
        )

        thread_ch = create_change_thread(
            thread,
            [day],
            uid='rel_uid',
            tz_start_time=time(13, 45),
            changes={
                st_1: {'departure': +10},
                st_2: {'arrival': +100},
            }
        )

        SuburbanKey.objects.create(thread=thread, key='th_key')
        SuburbanKey.objects.create(thread=thread_ch, key='rel_key')

        subs = [subscription]
        finder = ChangesFinder(script_run_id='some_id')
        subs_changes = finder.run(subs, day)

        assert_that(subs_changes, contains_inanyorder(
            has_properties({
                'calc_date': datetime(2019, 5, 12),
                'changes': contains_inanyorder(
                    has_properties({
                        'basic_thread': has_properties({'uid': 'basic_uid'}),
                        'rel_thread': has_properties({'uid': 'rel_uid'}),
                        'rts_from': has_properties({'diff': 25}),
                        'rts_to': has_properties({'diff': 115}),
                        'type': 'changed'
                    })
                )
            })
        ))
