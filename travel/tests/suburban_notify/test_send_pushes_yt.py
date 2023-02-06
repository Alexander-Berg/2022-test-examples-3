# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from hamcrest import assert_that, contains_inanyorder

from common.tester.factories import create_station
from common.tester.utils.datetime import replace_now
from travel.rasp.info_center.info_center.suburban_notify.changes.models import (
    SubscriptionChanges, Change, RTSChange, ThreadData
)
from travel.rasp.info_center.info_center.suburban_notify.changes.find import ChangeType
from travel.rasp.info_center.info_center.suburban_notify.subscriptions.models import Subscription
from travel.rasp.info_center.info_center.suburban_notify.send_pushes_yt import YtSendPushes


pytestmark = [pytest.mark.dbuser]


@replace_now(datetime(2019, 1, 10))
def test_make_pushes_dicts():
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
            )
        ],
        subscription=Subscription(
            point_from_key='c213',
            point_to_key='s221',
            interval_from=0,
            interval_to=1440,
            frequency='every_day',
            importance='any'
        )
    )
    create_station(id=666)
    create_station(id=667, title_ru='туда')

    pushes_dicts = YtSendPushes.make_pushes_dicts({sub_changes: sub_changes.changes})

    assert_that(pushes_dicts, contains_inanyorder(
        {
            'receivers': ["tag:app_id in ('ru.yandex.rasp.sndbx', 'ru.yandex.mobile.trains.adhoc') && uid==123"],
            'text': '10:00 нитка отправится в 12:00, прибудет в туда в 14:00',
            'image': 'ic_notification',
            'high_priority': True,
            'data': {
                'point_to_key': 's221',
                'point_from_key': 'c213'
            },
            'dry_run': False,
            'title': 'Изменения на 10.01',
            'url': 'yandextrains://subscriptionfeed/?point_from_key=c213&point_to_key=s221',
            'project': 'suburban',
            'image_url': '',
            'device_id_policy': 'suburban_default_d8_device_id',
            'install_id_policy': 'suburban_h6_d14_install_id'
        }
    ))
