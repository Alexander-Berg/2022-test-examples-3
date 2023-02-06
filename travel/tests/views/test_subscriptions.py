# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime
from hamcrest import assert_that, has_entries, contains_inanyorder, has_properties, contains
from urllib import urlencode

import mock
import pytest
from django.test import Client

from common.tester.factories import create_station
from common.tester.utils.datetime import replace_now
from travel.rasp.info_center.info_center.suburban_notify.changes.find import ChangeType
from travel.rasp.info_center.info_center.suburban_notify.changes.models import SubscriptionChanges, Change, ThreadData, RTSChange
from travel.rasp.info_center.info_center.suburban_notify.subscriptions.models import Subscription, Frequency, Importance
from travel.rasp.info_center.tests.suburban_notify.utils import create_changes, create_subscription, BaseNotificationTest


pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


class TestViews(BaseNotificationTest):
    def test_get_subscription(self):
        sub_data = {
            'point_from_key': 'c213',
            'point_to_key': 's221',
            'interval_to': 200,
            'importance': Importance.ANY,
            'frequency': Frequency.EVERY_DAY,
            'interval_from': 10
        }

        params = {
            'point_from_key': 'c213',
            'point_to_key': 's221',
        }

        with mock.patch('travel.rasp.info_center.info_center.views.subscriptions.get_blackbox_info', return_value=mock.Mock(uid='123')):
            response = Client().get('/subscription/get/?{}'.format(urlencode(params)))
            assert response.status_code == 200
            assert json.loads(response.content) == {}

            create_subscription(uid='123', **sub_data)

            response = Client().get('/subscription/get/?{}'.format(urlencode(params)))
            assert response.status_code == 200
            data = json.loads(response.content)
            assert_that(data, has_entries(sub_data))

    def test_get_all_subscriptions(self):
        sub_data_1 = {
            'point_from_key': 'c213',
            'point_to_key': 's221',
            'interval_from': 0,
            'interval_to': 100,
            'importance': Importance.ANY,
            'frequency': Frequency.EVERY_DAY
        }
        sub_data_2 = {
            'point_from_key': 's114',
            'point_to_key': 's115',
            'interval_from': 200,
            'interval_to': 1000,
            'importance': Importance.ONLY_IMPORTANT,
            'frequency': Frequency.FIRST_DAY
        }

        with mock.patch('travel.rasp.info_center.info_center.views.subscriptions.get_blackbox_info', return_value=mock.Mock(uid='123')):
            create_subscription(uid='123', **sub_data_1)
            create_subscription(uid='123', **sub_data_2)

            response = Client().get('/subscription/get_all/')
            assert response.status_code == 200
            data = json.loads(response.content)
            assert_that(data, contains_inanyorder(
                has_entries(sub_data_1),
                has_entries(sub_data_2)
            ))

    def test_delete_subscription(self):
        sub_data = {
            'point_from_key': 'c213',
            'point_to_key': 's221',
            'interval_to': 200,
            'interval_from': 10,
            'importance': Importance.ANY,
            'frequency': Frequency.EVERY_DAY
        }

        create_subscription(uid='123', **sub_data)

        params = {
            'point_from_key': 'c213',
            'point_to_key': 's221',
        }

        with mock.patch('travel.rasp.info_center.info_center.views.subscriptions.get_blackbox_info', return_value=mock.Mock(uid='123')):
            response = Client().get('/subscription/delete/?{}'.format(urlencode(params)))
            assert response.status_code == 200

        assert Subscription.get_subscription(uid='123', **params) is None

    def test_modify_subscription(self):
        sub_data = {
            'point_from_key': 'c213',
            'point_to_key': 's221',
            'interval_from': 10,
            'interval_to': 200,
            'importance': Importance.ANY,
            'frequency': Frequency.EVERY_DAY
        }

        with mock.patch('travel.rasp.info_center.info_center.views.subscriptions.get_blackbox_info', return_value=mock.Mock(uid='123')):
            response = Client().get('/subscription/modify/?{}'.format(urlencode(sub_data)))
        assert response.status_code == 200

        subscription = Subscription.get_subscription(uid='123', point_from_key='c213', point_to_key='s221')
        assert_that(subscription, has_properties(sub_data))

    @replace_now(datetime(2019, 1, 10))
    def test_change_view(self):
        sub_data = {
            'point_from_key': 'c213',
            'point_to_key': 's221',
            'interval_from': 0,
            'interval_to': 1440,
            'importance': Importance.ANY,
            'frequency': Frequency.EVERY_DAY
        }

        create_station(id=666)
        create_station(id=667, title_ru='туда')
        create_subscription(uid='123', **sub_data)

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
                    basic_thread=ThreadData(key=31, is_first_run_day=False, uid=220, number=5, title='нитка'),
                    rel_thread=ThreadData(key=41, is_first_run_day=True, uid=221, number=25, title='нитка изменение'),
                    rts_from=RTSChange(
                        type=ChangeType.CHANGED, station=666, first_station=666, last_station=667,
                        actual_time=datetime(2019, 1, 11, 12), schedule_time=datetime(2019, 1, 11, 10)
                    ),
                    rts_to=RTSChange(
                        type=ChangeType.NOT_CHANGED, station=667, first_station=666, last_station=667,
                        actual_time=datetime(2019, 1, 11, 14), schedule_time=datetime(2019, 1, 11, 14)
                    )
                ),
                Change(
                    type=ChangeType.CANCELLED,
                    start_date=datetime(2019, 1, 10),
                    basic_thread=ThreadData(key=31, is_first_run_day=True, uid=33, number=5, title='нитка'),
                    rel_thread=ThreadData(key=41, is_first_run_day=False, uid=43, number=25, title='нитка отмены'),
                    rts_from=RTSChange(type=ChangeType.NOT_CHANGED, station=666, schedule_time=datetime(2019, 1, 11, 8))
                ),
                Change(
                    type=ChangeType.ADDED,
                    start_date=datetime(2019, 1, 10),
                    rel_thread=ThreadData(key=55, is_first_run_day=True, uid=55, number=76, title='нитка назначение'),
                    rts_from=RTSChange(type=ChangeType.ADDED, station=666, actual_time=datetime(2019, 1, 11, 9)),
                    rts_to=RTSChange(type=ChangeType.ADDED, station=667, actual_time=datetime(2019, 1, 11, 15)),
                )
            ]
        )

        create_changes(**sub_changes.to_dict())

        params = {
            'point_from_key': 'c213',
            'point_to_key': 's221',
        }

        with mock.patch('travel.rasp.info_center.info_center.views.subscriptions.get_blackbox_info', return_value=mock.Mock(uid='123')):
            response = Client().get('/subscription/changes/?{}'.format(urlencode(params)))
        assert response.status_code == 200
        data = json.loads(response.content)

        assert_that(data, has_entries({
            'subscription': has_entries(sub_data),
            'changes': contains_inanyorder(
                has_entries({
                    'date': '2019-01-10',
                    'changes_list': contains(
                        {
                            'is_first_run_day': False,
                            'is_important': True,
                            'description': 'отменен',
                            'title': '08:00 нитка'
                        },
                        {
                            'is_first_run_day': True,
                            'is_important': True,
                            'description': 'будет отправляться в 09:00 и прибывать в 15:00',
                            'title': '09:00 нитка назначение'
                        },
                        {
                            'is_first_run_day': True,
                            'is_important': True,
                            'description': 'отправится в 12:00, прибудет в туда в 14:00',
                            'title': '10:00 нитка'
                        }
                    )
                })
            )
        }))
