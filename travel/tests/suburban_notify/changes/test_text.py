# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from hamcrest import assert_that, has_entries

from common.tester.factories import create_station
from common.tester.utils.datetime import replace_now
from travel.rasp.info_center.info_center.suburban_notify.changes.find import ChangeType
from travel.rasp.info_center.info_center.suburban_notify.changes.models import SubscriptionChanges, Change, RTSChange, ThreadData
from travel.rasp.info_center.info_center.suburban_notify.changes.text import TextGenerator, filter_changes
from travel.rasp.info_center.info_center.suburban_notify.subscriptions.models import Subscription, Frequency, Importance
from travel.rasp.info_center.tests.suburban_notify.utils import BaseNotificationTest

pytestmark = [pytest.mark.dbuser]


class TestTextGenerator(BaseNotificationTest):
    def test_get_text_for_change(self):
        create_station(id=666,  title_ru='откуда')
        create_station(id=667, title_ru='туда')

        def create_change(change_type, rts_from_type, rts_to_type, title='нитка', rel_title='нитка изменение'):
            return Change(
                type=change_type,
                start_date=datetime(2019, 1, 10),
                basic_thread=ThreadData(key=31, is_first_run_day=False, uid=220, number=5, title=title),
                rel_thread=ThreadData(key=41, is_first_run_day=True, uid=221, number=25, title=rel_title),
                rts_from=RTSChange(
                    type=rts_from_type, station=666, first_station=666, last_station=667,
                    actual_time=datetime(2019, 1, 11, 12), schedule_time=datetime(2019, 1, 11, 10)
                ),
                rts_to=RTSChange(
                    type=rts_to_type, station=667, first_station=666, last_station=667,
                    actual_time=datetime(2019, 1, 11, 14), schedule_time=datetime(2019, 1, 11, 14)
                )
            )

        sub_changes = SubscriptionChanges(
            calc_date=datetime(2019, 1, 10),
            uid='123',
            point_from_key='c213',
            point_to_key='s221',
            interval_from=0,
            interval_to=1440,
            changes=[
                create_change(ChangeType.CANCELLED, ChangeType.NOT_CHANGED, ChangeType.NOT_CHANGED),
                create_change(ChangeType.ADDED, ChangeType.CHANGED, ChangeType.NOT_CHANGED),
                create_change(ChangeType.CHANGED, ChangeType.CHANGED, ChangeType.NOT_CHANGED),
                create_change(ChangeType.CHANGED, ChangeType.NOT_CHANGED, ChangeType.CHANGED),
                create_change(ChangeType.CHANGED, ChangeType.CHANGED, ChangeType.CHANGED),
                create_change(ChangeType.CHANGED, ChangeType.NOT_CHANGED, ChangeType.NO_STOP),
                create_change(ChangeType.CHANGED, ChangeType.CHANGED, ChangeType.NO_STOP),
                create_change(ChangeType.CHANGED, ChangeType.NO_STOP, ChangeType.NOT_CHANGED),
                create_change(ChangeType.CHANGED, ChangeType.CANCELLED, ChangeType.NOT_CHANGED),
                create_change(ChangeType.CHANGED, ChangeType.NOT_CHANGED, ChangeType.CANCELLED)
            ]
        )

        text_gen = TextGenerator()
        text_gen.load_objects([sub_changes])

        texts = text_gen.get_text_for_change(sub_changes.changes[0])
        assert_that(texts, has_entries({
            'thread': '10:00 нитка',
            'description': 'отменен',
            'full_text': '10:00 нитка отменен'
        }))

        texts = text_gen.get_text_for_change(sub_changes.changes[1])
        assert_that(texts, has_entries({
            'thread': '12:00 нитка изменение',
            'description': 'будет отправляться в 12:00 и прибывать в 14:00',
            'full_text': '12:00 нитка изменение будет отправляться в 12:00 и прибывать в 14:00'
        }))

        texts = text_gen.get_text_for_change(sub_changes.changes[2])
        assert_that(texts, has_entries({
            'thread': '10:00 нитка',
            'description': 'отправится в 12:00, прибудет в туда в 14:00',
            'full_text': '10:00 нитка отправится в 12:00, прибудет в туда в 14:00'
        }))

        texts = text_gen.get_text_for_change(sub_changes.changes[3])
        assert_that(texts, has_entries({
            'thread': '10:00 нитка',
            'description': 'отправится по расписанию в 10:00, прибудет в туда в 14:00',
            'full_text': '10:00 нитка отправится по расписанию в 10:00, прибудет в туда в 14:00'
        }))

        texts = text_gen.get_text_for_change(sub_changes.changes[4])
        assert_that(texts, has_entries({
            'thread': '10:00 нитка',
            'description': 'отправится в 12:00, прибудет в туда в 14:00',
            'full_text': '10:00 нитка отправится в 12:00, прибудет в туда в 14:00'
        }))

        texts = text_gen.get_text_for_change(sub_changes.changes[5])
        assert_that(texts, has_entries({
            'thread': '10:00 нитка',
            'description': 'проследует станцию туда без остановки',
            'full_text': '10:00 нитка проследует станцию туда без остановки'
        }))

        texts = text_gen.get_text_for_change(sub_changes.changes[6])
        assert_that(texts, has_entries({
            'thread': '10:00 нитка',
            'description': 'отправится в 12:00 и проследует станцию туда без остановки',
            'full_text': '10:00 нитка отправится в 12:00 и проследует станцию туда без остановки'
        }))

        texts = text_gen.get_text_for_change(sub_changes.changes[7])
        assert_that(texts, has_entries({
            'thread': '10:00 нитка',
            'description': 'проследует станцию откуда без остановки, посадка невозможна',
            'full_text': '10:00 нитка проследует станцию откуда без остановки, посадка невозможна'
        }))

        texts = text_gen.get_text_for_change(sub_changes.changes[8])
        assert_that(texts, has_entries({
            'thread': '10:00 нитка',
            'description': 'отменен, будет следовать из откуда',
            'full_text': '10:00 нитка отменен, будет следовать из откуда'
        }))

        texts = text_gen.get_text_for_change(sub_changes.changes[9])
        assert_that(texts, has_entries({
            'thread': '10:00 нитка',
            'description': 'проезд до станции туда невозможен, поезд следует до станции туда',
            'full_text': '10:00 нитка проезд до станции туда невозможен, поезд следует до станции туда'
        }))

    def test_get_text_for_sub_changes(self):
        create_station(id=666, title_ru='откуда')
        create_station(id=667, title_ru='туда')

        sub_changes = SubscriptionChanges(
            calc_date=datetime(2019, 1, 10),
            uid='123',
            point_from_key='s666',
            point_to_key='s667',
            interval_from=0,
            interval_to=1440,
            changes=[
                Change(
                    type=ChangeType.CANCELLED,
                    start_date=datetime(2019, 1, 10),
                    basic_thread=ThreadData(key=31, is_first_run_day=True, uid=33, number=5, title='нитка'),
                    rel_thread=ThreadData(key=41, is_first_run_day=False, uid=43, number=25, title='нитка отмены'),
                    rts_from=RTSChange(type=ChangeType.NOT_CHANGED, station=666,
                                       schedule_time=datetime(2019, 1, 11, 13))
                ),
                Change(
                    type=ChangeType.ADDED,
                    start_date=datetime(2019, 1, 10),
                    basic_thread=ThreadData(key=31, is_first_run_day=False, uid=220, number=5, title='нитка'),
                    rel_thread=ThreadData(key=41, is_first_run_day=True, uid=221, number=25,
                                          title='нитка изменение'),
                    rts_from=RTSChange(
                        type=ChangeType.CHANGED, station=666, first_station=666, last_station=667,
                        actual_time=datetime(2019, 1, 11, 12), schedule_time=datetime(2019, 1, 11, 10)
                    ),
                    rts_to=RTSChange(
                        type=ChangeType.NOT_CHANGED, station=667, first_station=666, last_station=667,
                        actual_time=datetime(2019, 1, 11, 14), schedule_time=datetime(2019, 1, 11, 14)
                    )
                )
            ]
        )

        text_gen = TextGenerator()
        text_gen.load_objects([sub_changes])

        assert text_gen.get_text_for_sub_changes(sub_changes, []) is None

        texts = text_gen.get_text_for_sub_changes(sub_changes, sub_changes.changes)
        assert_that(texts, has_entries({
            'title': 'Изменения на 10.01',
            'text': 'Изменения по направлению откуда - туда',
            'texts': [
                '13:00 нитка отменен',
                '12:00 нитка изменение будет отправляться в 12:00 и прибывать в 14:00'
            ]
        }))

        texts = text_gen.get_text_for_sub_changes(sub_changes, [sub_changes.changes[0]])
        assert_that(texts, has_entries({
            'title': 'Изменения на 10.01',
            'text': '13:00 нитка отменен',
            'texts': ['13:00 нитка отменен']
        }))


def test_filter_changes():
    create_station(id=666, title_ru='откуда')
    create_station(id=667, title_ru='туда')

    change_1 = Change(
        type=ChangeType.CANCELLED,
        push_sent=True
    )

    change_2 = Change(
        type=ChangeType.ADDED,
        push_sent=False
    )

    change_3 = Change(
        type=ChangeType.CANCELLED,
        rel_thread=ThreadData(key=41, uid=221, number=25),
        push_sent=False
    )

    change_4 = Change(
        type=ChangeType.CHANGED,
        start_date=datetime(2019, 1, 10),
        rts_from=RTSChange(type=ChangeType.CHANGED, diff=3, station=666),
        rts_to=RTSChange(type=ChangeType.CHANGED, diff=2, station=666),
        rel_thread=ThreadData(key=41, is_first_run_day=True, uid=221, number=25),
        push_sent=False
    )

    change_5 = Change(
        type=ChangeType.ADDED,
        start_date=datetime(2019, 1, 10),
        rts_from=RTSChange(type=ChangeType.CHANGED, station=666, schedule_time=datetime(2019, 3, 20, 12)),
        rel_thread=ThreadData(key=41, is_first_run_day=True, uid=221, number=25),
        push_sent=False
    )

    sub_changes = SubscriptionChanges(
        calc_date=datetime(2019, 1, 10),
        uid='123',
        interval_from=0,
        interval_to=1440,
        subscription=Subscription(frequency=Frequency.EVERY_DAY)
    )

    sub_changes.changes = [change_1, change_2]
    filtered_changes = filter_changes(sub_changes, only_new=False)
    assert filtered_changes == [change_2]

    sub_changes.changes = [change_3]
    sub_changes.subscription.frequency = Frequency.FIRST_DAY
    filtered_changes = filter_changes(sub_changes, only_new=False)
    assert filtered_changes == []
    change_3.rel_thread.is_first_run_day = True
    filtered_changes = filter_changes(sub_changes, only_new=False)
    assert filtered_changes == [change_3]

    sub_changes.changes = [change_4]
    filtered_changes = filter_changes(sub_changes, only_new=False)
    assert filtered_changes == [change_4]
    sub_changes.subscription.importance = Importance.ONLY_IMPORTANT
    filtered_changes = filter_changes(sub_changes, only_new=False)
    assert filtered_changes == []

    sub_changes.subscription.importance = Importance.ANY
    with replace_now(datetime(2019, 3, 20, 13, 30)):
        sub_changes.changes = [change_5]
        filtered_changes = filter_changes(sub_changes, only_new=True)
        assert filtered_changes == []

    with replace_now(datetime(2019, 3, 20, 12, 30)):
        sub_changes.changes = [change_5]
        filtered_changes = filter_changes(sub_changes, only_new=True)
        assert filtered_changes == [change_5]
