# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, timedelta, time

import pytest
import pytz
from hamcrest import assert_that, has_entries, contains_inanyorder

from common.models.schedule import RTStation, RThreadType
from common.models.transport import TransportType
from common.tester.factories import create_change_thread
from common.utils.date import MSK_TZ
from travel.rasp.info_center.info_center.suburban_notify.db import load_db_data, clear_caches
from travel.rasp.info_center.info_center.suburban_notify.changes.find import (
    get_changes_for_interval, get_basic_thread_changes, ChangeType, get_segment_changes,
    get_rts_event_changes, get_new_segment_changes
)
from travel.rasp.info_center.info_center.suburban_notify.utils import get_interval
from travel.rasp.info_center.tests.suburban_notify.utils import convert_db_objs, convert_db_obj, create_thread, create_station

suburban_type = TransportType.objects.get(id=TransportType.SUBURBAN_ID)


pytestmark = [pytest.mark.dbuser('module')]


def load_db_data_with_clear(*args, **kwargs):
    clear_caches()
    load_db_data(*args, **kwargs)


def _get_segments_for_threads(threads, st_from, st_to):
    """
    Формируем список сегментов (пар rts) для списка тредов и пары станций -
    эмуляция поиска на все дни для тестов, чтобы не запускать реальные поиски.
    """
    segments = []
    for thread in threads:
        rts_from, rts_to = None, None
        for rts in thread.path:
            if not rts_from:
                if rts.station == st_from:
                    rts_from = rts
            elif rts.station == st_to:
                rts_to = rts
                break
        if rts_from and rts_to:
            segments.append((rts_from.id, rts_to.id))

    return segments


def has_change(basic_thread, rel_thread, change_type=ChangeType.CHANGED):
    return has_entries(
        basic_thread=convert_db_obj(basic_thread),
        rel_thread=convert_db_obj(rel_thread),
        type=change_type,
    )


def test_get_rts_event_changes():
    assert get_rts_event_changes(1, 3, 0) == {'type': ChangeType.CHANGED, 'diff': 2}
    assert get_rts_event_changes(1, -3, 0) == {'type': ChangeType.CHANGED, 'diff': -4}
    assert get_rts_event_changes(1, -3, 4) == {'type': ChangeType.NOT_CHANGED}
    assert get_rts_event_changes(None, None, 100) == {'type': ChangeType.NOT_CHANGED}
    assert get_rts_event_changes(None, 3, 100) == {'type': ChangeType.ADDED}
    assert get_rts_event_changes(None, -100, 100) == {'type': ChangeType.ADDED}
    assert get_rts_event_changes(3, None, 100) == {'type': ChangeType.CANCELLED}
    assert get_rts_event_changes(-3, None, 100) == {'type': ChangeType.CANCELLED}


class TestGetSegmentChanges(object):
    def test_valid(self):
        st_from,  st_mid1,  st_mid2,  st_mid3,  st_to = [create_station() for _ in range(5)]

        day1 = datetime(2019, 1, 20)
        thread = create_thread(
            year_days=[day1],
            tz_start_time=time(23, 50),
            schedule_v1=[
                [None, 0, st_from],               # 23:50
                [5, 15, st_mid1],                 # 23:55 - 00:05 +1day
                [1440 + 5, 1440 + 15, st_mid2],   # 23:55 +1day - 00:05 +2day
                [1440 + 20, 1440 + 30, st_mid3],  # 00:10 +2day - 00:20 +2day
                [1440 + 40, None, st_to],         # 00:30 +2day
            ],
        )

        thread_ch = create_change_thread(
            thread,
            year_days=[day1],
            tz_start_time=time(23, 55),
            changes={
                st_mid1: {'cancelled': True},
                st_mid2: {'departure': -10},  # arrival == departure => no_stop
                st_mid3: {'arrival': -5, 'departure': -5},
                st_to: {'arrival': -4, 'departure': 10},
            }
        )

        load_db_data_with_clear(RTStation.objects.values_list('id', flat=True))

        tthread, trts_from, trts_mid1, trts_mid2, trts_mid3, trts_to = convert_db_objs(thread, *thread.path)
        tthread_ch, trts_from_ch, trts_mid2_ch, trts_mid3_ch, trts_to_ch = convert_db_objs(
            thread_ch, *thread_ch.path
        )

        # Отправление поменялось за счет изменения старта нитки.
        # Прибытие поменялось за счет изменения старта нитки и сдвига по станции.
        # Появилось отправление со станции прибытия, но нас это не волнует.
        change = get_segment_changes(trts_from, trts_to, tthread_ch)
        assert change == {
            'from': {
                'type': ChangeType.CHANGED,
                'rts': trts_from,
                'rel_rts': trts_from_ch,
                'diff': +5,
            },
            'to': {
                'type': ChangeType.CHANGED,
                'rts': trts_to,
                'rel_rts': trts_to_ch,
                'diff': +1,
            },
        }

        # станция отменена
        change = get_segment_changes(trts_from, trts_mid1, tthread_ch)
        assert change == {
            'from': {
                'type': ChangeType.CHANGED,
                'rts': trts_from,
                'rel_rts': trts_from_ch,
                'diff': +5,
            },
            'to': {
                'type': ChangeType.CANCELLED,
                'rts': trts_mid1,
            },
        }

        # Время отправления изменилось.
        # Время прибытия изменилось в нитке-изменении, но фактически разницы с основной ниткой нет,
        # т.к. изменения компенсировались изменением старта нитки.
        change = get_segment_changes(trts_from, trts_mid3, tthread_ch)
        assert change == {
            'from': {
                'type': ChangeType.CHANGED,
                'rts': trts_from,
                'rel_rts': trts_from_ch,
                'diff': +5,
            },
            'to': {
                'type': ChangeType.NOT_CHANGED,
                'rts': trts_mid3,
            }
        }

        change = get_segment_changes(trts_mid2, trts_to, tthread_ch)
        assert change == {
            'from': {
                'type': ChangeType.NO_STOP,
                'rts': trts_mid2,
                'rel_rts': trts_mid2_ch,
            },
            'to': {
                'type': ChangeType.CHANGED,
                'rts': trts_to,
                'rel_rts': trts_to_ch,
                'diff': +1,
            },
        }


class TestGetBasicThreadChanges(object):
    def test_valid(self):
        st_from = create_station()
        st_mid1 = create_station()
        st_mid2 = create_station()
        st_to = create_station()

        day1 = datetime(2019, 1, 20)
        day2, day3, day4 = [day1 + timedelta(i) for i in range(1, 4)]

        thread = create_thread(
            year_days=[day1, day2, day3],
            tz_start_time=time(23, 50),
            schedule_v1=[
                [None, 0, st_from],                # 23:50
                [5, 15, st_mid1],                  # 23:55 - 00:05 +1day
                [1440 + 5, 1440 + 15, st_mid2],    # 23:55 +1day - 00:05 +2day
                [1440 + 20, None, st_to],          # 00:10 +2day
            ],
        )
        rts_from, rts_mid1, rts_mid2, rts_to = thread.path

        thread_ch1 = create_change_thread(thread, [day1], changes={
            st_mid2: {'arrival': -1, 'departure': +8},
            st_to: {'arrival': +100},
        })
        rts_from_ch1, rts_mid1_ch1, rts_mid2_ch1, rts_to_ch1 = thread_ch1.path

        thread_ch2 = create_change_thread(thread, [day2], changes={
            st_mid2: {'cancelled': True},
            st_to: {'arrival': -42},
        })
        rts_from_ch2, rts_mid1_ch2, rts_to_ch2 = thread_ch2.path

        thread_ch3 = create_thread(
            basic_thread=thread,
            type=RThreadType.CANCEL_ID,
            year_days=[day4],
        )

        load_db_data_with_clear(RTStation.objects.values_list('id', flat=True))
        tthread, trts_mid2, trts_to = convert_db_objs(thread, rts_mid2, rts_to)
        tthread_ch1, trts_mid2_ch1, trts_to_ch1 = convert_db_objs(thread_ch1, rts_mid2_ch1, rts_to_ch1)
        tthread_ch2, trts_mid1_ch2, trts_to_ch2 = convert_db_objs(thread_ch2, rts_mid1_ch2, rts_to_ch2)

        changes = get_basic_thread_changes(tthread, day1.date(), trts_mid2, trts_to)
        assert changes == {
            'type': ChangeType.CHANGED,
            'basic_thread': tthread,
            'rel_thread': tthread_ch1,
            'start_date': day1.date(),
            'from': {
                'type': ChangeType.CHANGED,
                'rts': trts_mid2,
                'rel_rts': trts_mid2_ch1,
                'diff': +8,
            },
            'to': {
                'type': ChangeType.CHANGED,
                'rts': trts_to,
                'rel_rts': trts_to_ch1,
                'diff': +100,
            },
        }

        changes = get_basic_thread_changes(tthread, day2.date(), trts_mid2, trts_to)
        assert changes == {
            'basic_thread': tthread,
            'type': ChangeType.CHANGED,
            'rel_thread': convert_db_obj(thread_ch2),
            'start_date': day2.date(),
            'from': {
                'type': ChangeType.CANCELLED,
                'rts': trts_mid2,
            },
            'to': {
                'type': ChangeType.CHANGED,
                'rts': trts_to,
                'rel_rts': trts_to_ch2,
                'diff': -42,
            },
        }

        changes = get_basic_thread_changes(tthread, day3.date(), trts_mid2, trts_to)
        assert changes is None

        changes = get_basic_thread_changes(tthread, day4.date(), trts_mid2, trts_to)
        assert changes == {
            'basic_thread': tthread,
            'rel_thread': convert_db_obj(thread_ch3),
            'type': ChangeType.CANCELLED,
            'start_date': day4.date(),
            'from': {'type': 'cancelled', 'rts': trts_mid2},
            'to': {'type': 'cancelled', 'rts': trts_to}
        }


class TestGetNewSegmentChanges(object):
    def test_valid(self):
        st_from, st_to = [create_station() for _ in range(2)]
        day1 = MSK_TZ.localize(datetime(2019, 1, 20))

        thread = create_thread(
            type=RThreadType.ASSIGNMENT_ID,
            year_days=[day1],
            tz_start_time=time(23, 50),
            schedule_v1=[
                [None, 0, st_from],
                [20, None, st_to],
            ],
        )

        load_db_data_with_clear(RTStation.objects.values_list('id', flat=True))
        tthread, trts_from, trts_to = convert_db_objs(thread, *thread.path)

        change = get_new_segment_changes(day1.date(), trts_from, trts_to)
        assert change == {
            'rel_thread': tthread,
            'type': ChangeType.ADDED,
            'start_date': day1.date(),
            'from': {
                'type': ChangeType.ADDED,
                'rel_rts': trts_from,
            },
            'to': {
                'type': ChangeType.ADDED,
                'rel_rts': trts_to,
            }
        }


class TestGetChangesForSubscription(object):
    def test_valid(self):
        st_from, st_mid1, st_mid2, st_to = [create_station() for _ in range(4)]

        day1 = MSK_TZ.localize(datetime(2019, 1, 20))
        day2 = day1 + timedelta(1)
        day3 = day1 + timedelta(2)
        day4 = day1 + timedelta(3)
        day_prev1 = day1 + timedelta(-1)

        thread = create_thread(
            schedule_v1=[
                [None, 0, st_from],                # 23:50
                [5, 15, st_mid1],                  # 23:55 - 00:05 +1day
                [1440 + 5, 1440 + 15, st_mid2],    # 23:55 +1day - 00:05 +2day
                [1440 + 20, None, st_to],          # 00:10 +1day
            ],
            year_days=[day1, day2, day3, day4, day_prev1],
            tz_start_time=time(23, 50),
        )

        thread_ch = create_change_thread(thread, [day1], changes={
            st_mid2: {'arrival': -1, 'departure': +8},
            st_to: {'arrival': +100},
        })

        load_db_data_with_clear(RTStation.objects.values_list('id', flat=True))
        tthread, trts_from, trts_mid1, trts_mid2, trts_to = convert_db_objs(thread, *thread.path)
        tthread_ch, trts_from_ch, trts_mid1_ch, trts_mid2_ch, trts_to_ch = convert_db_objs(thread_ch, *thread_ch.path)

        segments = _get_segments_for_threads([thread, thread_ch], st_mid2, st_to)

        interval_from, interval_to = get_interval(day3, 4, 6, MSK_TZ)
        changes = get_changes_for_interval(interval_from, interval_to, segments)
        assert len(changes) == 1
        assert changes[0] == {
            'basic_thread': tthread,
            'rel_thread': tthread_ch,
            'type': ChangeType.CHANGED,
            'start_date': day1.date(),
            'from': {
                'type': ChangeType.CHANGED,
                'rts': trts_mid2,
                'rel_rts': trts_mid2_ch,
                'diff': +8,
            },
            'to': {
                'type': ChangeType.CHANGED,
                'rts': trts_to,
                'rel_rts': trts_to_ch,
                'diff': +100,
            }
        }

    def test_interval_cases(self):
        st_from, st_mid1, st_to = create_station(), create_station(), create_station()

        day1 = MSK_TZ.localize(datetime(2019, 2, 15))
        day2 = day1 + timedelta(1)
        day3 = day1 + timedelta(2)
        day_prev1 = day1 + timedelta(-1)

        thread1 = create_thread(
            id=1,
            year_days=[day_prev1, day3],
            tz_start_time=time(23, 42),
            schedule_v1=[
                [None, 0, st_from],               # 23:42
                [1440 + 5, 1440 + 15, st_mid1],   # 23:47 - 23:57 +1day
                [1440 + 30, None, st_to],         # 00:12 +1day
            ],
        )
        thread1_ch1 = create_change_thread(thread1, [day1], id=11, changes={st_mid1: {'departure': 11}})
        thread1_ch2 = create_change_thread(thread1, [day2], id=12, changes={st_mid1: {'departure': 13}})

        thread2 = create_thread(
            id=2,
            year_days=[day1, day2, day3],
            tz_start_time=time(22, 50),
            schedule_v1=[
                [None, 0, st_from],              # 22:50
                [1440 + 5, 1440 + 15, st_mid1],  # 22:55 - 23:05 +1day
                [1440 + 20, None, st_to],        # 23:10 +1day
            ],
        )
        thread2_ch1 = create_change_thread(thread2, [day2], id=21, changes={st_mid1: {'departure': 14}})

        thread3 = create_thread(
            id=3,
            year_days=[day1, day2],
            tz_start_time=time(21, 40),
            schedule_v1=[
                [None, 0, st_from],              # 21:50
                [1440 + 5, 1440 + 15, st_mid1],  # 21:45 - 21:55 +1day
                [1440 + 20, None, st_to],        # 22:00 +1day
            ],
        )
        thread3_ch1 = create_change_thread(thread3, [day2], id=31, changes={st_mid1: {'departure': 1}})

        thread4 = create_thread(
            id=4,
            type=RThreadType.ASSIGNMENT_ID,
            year_days=[day_prev1],
            tz_start_time=time(22, 56),
            schedule_v1=[
                [None, 0, st_from],              # 22:56
                [1440 + 5, 1440 + 15, st_mid1],  # 23:01 - 00:11 +1day
                [1440 + 20, None, st_to],        # 00:16 +1day
            ],
        )

        load_db_data_with_clear(RTStation.objects.values_list('id', flat=True))
        segments = _get_segments_for_threads(
            [thread1, thread1_ch1, thread1_ch2, thread2, thread2_ch1, thread3, thread3_ch1, thread4],
            st_mid1, st_to
        )

        def check(day, int_from, int_to, expected_changes, tz=MSK_TZ):
            interval_from, interval_to = get_interval(day, int_from, int_to, tz)
            changes = get_changes_for_interval(interval_from, interval_to, segments)

            assert_that(changes, contains_inanyorder(*expected_changes))

        # Т.к. st_mid1 отстоит от старта любой нашей нитки на 1 день, ниже проверки идут +1 day.
        # 1 нитка изменена, 2 нет
        check(day2, 23 * 60, 24 * 60, [has_change(thread1, thread1_ch1)])

        # в такой же интервал по екб нитки не попадают
        ekb_tz = pytz.timezone('Asia/Yekaterinburg')
        check(day2, 23 * 60, 24 * 60, [], tz=ekb_tz)

        # по екб эти изменения приходятся на day3.
        # Поэтому интервал надо сместить на начало дня, чтобы получить тот же результат
        check(day3, 60, 120, [has_change(thread1, thread1_ch1)], tz=ekb_tz)

        # 2 нитка не попадает в интервал
        check(day2, 23 * 60 + 10, 24 * 60, [has_change(thread1, thread1_ch1)])
        check(day3, 70, 120, [has_change(thread1, thread1_ch1)], tz=ekb_tz)

        # 1 нитка не попадает в интервал, 2 нитка не имеет изменений
        check(day2, 23 * 60, 23 * 60 + 20, [])

        # 1 и 2 нитки изменены, 3я не попадает
        check(day3, 22 * 60, 24 * 60, [has_change(thread1, thread1_ch2), has_change(thread2, thread2_ch1)])

        # 1 и 2 нитки изменены, 3я не попадает, но попадает ее изменение
        check(day3, 22 * 60 - 4, 24 * 60, [
            has_change(thread1, thread1_ch2),
            has_change(thread2, thread2_ch1),
            has_entries(
                rel_thread=convert_db_obj(thread3_ch1),
                type=ChangeType.ADDED,
            )
        ])

        # day-1. Нитка 1 без изменений, нитка-назначение 4 появилась
        check(day1, 23 * 60, 24 * 60, [
            has_entries(
                rel_thread=convert_db_obj(thread4),
                type=ChangeType.ADDED,
            )
        ])
