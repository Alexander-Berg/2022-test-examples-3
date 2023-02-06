# coding: utf8

from datetime import datetime, time, timedelta

import pytest
from hamcrest import assert_that, has_properties, contains

from common.apps.suburban_events.factories import ThreadEventsFactory, StationEventFactory
from common.apps.suburban_events.models import ThreadEvents, SuburbanKey, UpdatedThread, ThreadKey
from common.apps.suburban_events.scripts.precalc_thread_events import (
    get_expected_thread_events_on_date, precalc_all_expected_thread_events
)
from common.apps.suburban_events.utils import ThreadEventsTypeCodes, get_thread_suburban_key
from common.models.schedule import RThreadType
from common.tester.factories import create_thread, create_station, create_change_thread
from common.tester.utils.datetime import replace_now
from common.utils.date import RunMask

create_thread = create_thread.mutate(t_type="suburban")
create_station = create_station.mutate(t_type="suburban")

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def test_get_date_thread_events():
    station_from, station_to = create_station(id=121), create_station(id=122)
    station_mid_1, station_mid_2 = create_station(id=123), create_station(id=124)

    start_date_1 = datetime(2017, 3, 20, 13)
    start_date_2 = datetime(2017, 3, 25, 15)

    thread_1 = create_thread(
        uid=u'uid_1',
        number=u'number_1',
        id=121,
        tz_start_time=time(13),
        year_days=[start_date_1],
        schedule_v1=[
            [None, 0, station_from],
            [10, 20, station_mid_1],
            [50, None, station_to],
        ],
    )

    thread_2 = create_thread(
        uid=u'uid_2',
        number=u'number_2',
        id=123,
        tz_start_time=time(15),
        year_days=[start_date_2],
        schedule_v1=[
            [None, 0, station_from],
            [25, 25, station_mid_1],
            [35, 40, station_mid_2],
            [50, 60, station_to],
            [70, None, station_from],
        ],
    )

    thread_rts = {
        thread_1: thread_1.path,
        thread_2: thread_2.path
    }

    thread_events = get_expected_thread_events_on_date(thread_rts, {start_date_1.date(): [thread_1]})
    assert thread_events == {start_date_1.date(): {thread_1: {
        'path': [
            {'dt_normative': datetime(2017, 3, 20, 13, 0),
             'type': 'departure',
             'rtstation': thread_1.path[0]},
            {'dt_normative': datetime(2017, 3, 20, 13, 10),
             'type': 'arrival',
             'rtstation': thread_1.path[1]},
            {'dt_normative': datetime(2017, 3, 20, 13, 20),
             'type': 'departure',
             'rtstation': thread_1.path[1]},
            {'dt_normative': datetime(2017, 3, 20, 13, 50),
             'type': 'arrival',
             'rtstation': thread_1.path[2]}
        ],
        'pass_count': {
            station_from.id: 1,
            station_to.id: 1,
            station_mid_1.id: 1,
        }
    }}}

    thread_events = get_expected_thread_events_on_date(thread_rts, {start_date_2.date(): [thread_2]})
    assert thread_events == {start_date_2.date(): {thread_2: {
        'path': [
            {'dt_normative': datetime(2017, 3, 25, 15, 0),
             'type': 'departure',
             'rtstation': thread_2.path[0]},
            {'dt_normative': datetime(2017, 3, 25, 15, 35),
             'type': 'arrival',
             'rtstation': thread_2.path[2]},
            {'dt_normative': datetime(2017, 3, 25, 15, 40),
             'type': 'departure',
             'rtstation': thread_2.path[2]},
            {'dt_normative': datetime(2017, 3, 25, 15, 50),
             'type': 'arrival',
             'rtstation': thread_2.path[3]},
            {'dt_normative': datetime(2017, 3, 25, 16),
             'type': 'departure',
             'rtstation': thread_2.path[3]},
            {'dt_normative': datetime(2017, 3, 25, 16, 10),
             'type': 'arrival',
             'rtstation': thread_2.path[4]},
        ],
        'pass_count': {
            station_from.id: 2,
            station_to.id: 1,
            station_mid_2.id: 1
        }
    }}}


def test_precalc_all_thread_events():
    station_from, station_to = create_station(id=121), create_station(id=122)
    station_mid_1, station_mid_2 = create_station(id=123), create_station(id=124)

    start_date_1 = datetime(2017, 3, 20, 13)
    start_date_2 = datetime(2017, 3, 25, 15)
    start_date_3 = datetime(2017, 3, 25)

    thread_1 = create_thread(
        uid=u'uid_1',
        number=u'number_1',
        id=121,
        tz_start_time=time(13),
        modified_at=datetime(2017, 3, 20, 11),
        year_days=[start_date_1],
        schedule_v1=[
            [None, 0, station_from],
            [10, 20, station_mid_1],
            [50, 60, station_to],
            [70, None, station_from],
        ],
    )

    thread_2 = create_thread(
        uid=u'uid_2',
        number=u'number_2',
        id=123,
        tz_start_time=time(15),
        year_days=[start_date_2],
        schedule_v1=[
            [None, 0, station_from],
            [25, 25, station_mid_1],
            [35, 40, station_mid_2],
            [50, None, station_to],
        ],
    )

    # Создаем нитку и ее отмену.
    thread_3 = create_thread(
        uid=u'uid_3',
        number=u'number_3',
        tz_start_time=time(19),
        year_days=[start_date_3],
        schedule_v1=[
            [None, 0, station_from],
            [150, 150, station_mid_1],
            [200, None, station_to],
        ],
    )

    thread_3_cancel = create_thread(
        uid=u'uid_3_cancel',
        type=RThreadType.objects.get(id=RThreadType.CANCEL_ID),
        basic_thread=thread_3,
        tz_start_time=time(19),
        year_days=[start_date_3],
        schedule_v1=[
            [None, 0, station_from],
            [150, 150, station_mid_1],
            [200, None, station_to],
        ],
    )

    SuburbanKey.objects.create(thread=thread_1, key='th_1_key')
    SuburbanKey.objects.create(thread=thread_2, key='th_2_key')

    with replace_now(datetime(2017, 3, 20, 23)):
        precalc_all_expected_thread_events()
        thread_events = list(ThreadEvents.objects.all())
        assert len(thread_events) == 1
        expected_events = thread_events[0].stations_expected_events
        assert len(expected_events) == 6
        assert_that(expected_events[0],
                    has_properties({
                        'dt_normative': datetime(2017, 3, 20, 13, 0),
                        'station_key': u'121',
                        'type': u'departure'
                    }))
        assert_that(expected_events[1],
                    has_properties({
                        'dt_normative': datetime(2017, 3, 20, 13, 10),
                        'station_key': u'123',
                        'type': u'arrival'
                    }))
        assert_that(expected_events[2],
                    has_properties({
                        'dt_normative': datetime(2017, 3, 20, 13, 20),
                        'station_key': u'123',
                        'type': u'departure'
                    }))
        assert_that(expected_events[3],
                    has_properties({
                        'dt_normative': datetime(2017, 3, 20, 13, 50),
                        'station_key': u'122',
                        'type': u'arrival',
                    }))
        assert_that(expected_events[4],
                    has_properties({
                        'dt_normative': datetime(2017, 3, 20, 14),
                        'station_key': u'122',
                        'type': u'departure',
                        'passed_several_times': False
                    }))
        assert_that(expected_events[5],
                    has_properties({
                        'dt_normative': datetime(2017, 3, 20, 14, 10),
                        'station_key': u'121',
                        'type': u'arrival',
                        'passed_several_times': True
                    }))

    precalc_all_expected_thread_events(start_date=datetime(2017, 3, 25, 23))

    thread_events = list(ThreadEvents.objects.all().order_by('key.thread_key'))
    assert len(thread_events) == 2
    expected_events = thread_events[1].stations_expected_events
    assert len(expected_events) == 4
    assert_that(expected_events[0],
                has_properties({
                    'dt_normative': datetime(2017, 3, 25, 15, 0),
                    'station_key': u'121',
                    'type': u'departure'
                }))
    assert_that(expected_events[1],
                has_properties({
                    'dt_normative': datetime(2017, 3, 25, 15, 35),
                    'station_key': u'124',
                    'type': u'arrival'
                }))
    assert_that(expected_events[2],
                has_properties({
                    'dt_normative': datetime(2017, 3, 25, 15, 40),
                    'station_key': u'124',
                    'type': u'departure'
                }))
    assert_that(expected_events[3],
                has_properties({
                    'dt_normative': datetime(2017, 3, 25, 15, 50),
                    'station_key': u'122',
                    'type': u'arrival'
                }))

    # Проверяем, что нитка отмены и отмененная нитка добавлена в UpdatedThread.
    assert UpdatedThread.objects.count() == 2
    UpdatedThread.objects.get(uid=thread_3.uid, start_date=start_date_3)
    UpdatedThread.objects.get(uid=thread_3_cancel.uid, start_date=start_date_3)


def test_cancelled_changes():
    station_from, station_to = create_station(id=121), create_station(id=122)
    station_mid_1, station_mid_2 = create_station(id=123), create_station(id=124)

    start_date_1 = datetime(2017, 3, 20)
    start_date_2 = datetime(2017, 3, 25)

    # Создаем нитку и ее изменение.
    thread_1 = create_thread(
        uid=u'uid_1',
        tz_start_time=time(13),
        year_days=[start_date_1],
        schedule_v1=[
            [None, 0, station_from],
            [10, 20, station_mid_1],
            [50, None, station_to],
        ],
    )

    thread_1_change = create_thread(
        uid=u'uid_1_change',
        type=RThreadType.objects.get(id=RThreadType.CHANGE_ID),
        basic_thread=thread_1,
        tz_start_time=time(15),
        year_days=[start_date_1],
        schedule_v1=[
            [None, 0, station_from],
            [25, 25, station_mid_1],
            [35, 40, station_mid_2],
            [50, None, station_to],
        ],
    )

    # Создаем нитку и ее отмену.
    thread_2 = create_thread(
        uid=u'uid_2',
        tz_start_time=time(19),
        year_days=[start_date_2],
        schedule_v1=[
            [None, 0, station_from],
            [150, 150, station_mid_1],
            [200, None, station_to],
        ],
    )

    thread_2_cancel = create_thread(
        uid=u'uid_2_cancel',
        type=RThreadType.objects.get(id=RThreadType.CANCEL_ID),
        basic_thread=thread_2,
        tz_start_time=time(19),
        year_days=[start_date_2],
        schedule_v1=[
            [None, 0, station_from],
            [150, 150, station_mid_1],
            [200, None, station_to],
        ],
    )

    SuburbanKey.objects.create(thread=thread_1, key='th_1_key')
    SuburbanKey.objects.create(thread=thread_2, key='th_2_key')
    SuburbanKey.objects.create(thread=thread_1_change, key='th_1_change_key')

    # Проверяем, что нитка отмены и отмененная нитка добавлены в UpdatedThread.
    precalc_all_expected_thread_events(start_date=datetime(2017, 3, 25, 23))
    assert UpdatedThread.objects.count() == 2
    UpdatedThread.objects.get(uid=thread_2.uid, start_date=start_date_2)
    UpdatedThread.objects.get(uid=thread_2_cancel.uid, start_date=start_date_2)

    # Проверяем, что после изменения даты нитки отмены,
    # нитка отмены и отмененная были удалены из UpdatedThread.
    thread_2_cancel.year_days = RunMask(days=[start_date_1])
    thread_2_cancel.save()
    precalc_all_expected_thread_events(start_date=datetime(2017, 3, 25, 23))
    assert UpdatedThread.objects.count() == 0

    # Проверяем, что при отмене уже существующего в базе ThreadEvents он будет удален.
    # А нитки будут добалены в UpdatedThread.
    assert ThreadEvents.objects.count() == 1
    thread_2_cancel.year_days = RunMask(days=[start_date_2])
    thread_2_cancel.save()
    precalc_all_expected_thread_events(start_date=datetime(2017, 3, 25, 23))
    assert UpdatedThread.objects.count() == 2
    assert ThreadEvents.objects.count() == 0

    UpdatedThread.objects.delete()

    # Проверяем, что нитка изменение была добавлена в ThreadEvents.
    # А измененная нитка была добалена в UpdatedThread.
    precalc_all_expected_thread_events(start_date=datetime(2017, 3, 20))
    assert UpdatedThread.objects.count() == 1
    UpdatedThread.objects.get(uid=thread_1.uid, start_date=start_date_1)
    assert ThreadEvents.objects.count() == 1
    ThreadEvents.objects.get(key__thread_key='th_1_change_key',
                             key__thread_start_date=datetime.combine(start_date_1, time(15)))

    # Проверяем, что после изменения даты нитки изменения,
    # нитка изменения была удалена из UpdatedThread и ThreadEvents,
    # а основная нитка была добалена в ThreadEvents.
    thread_1_change.year_days = RunMask(days=[start_date_2])
    thread_1_change.save()
    precalc_all_expected_thread_events(start_date=datetime(2017, 3, 20))
    assert UpdatedThread.objects.count() == 0
    assert ThreadEvents.objects.count() == 1
    ThreadEvents.objects.get(key__thread_key='th_1_key',
                             key__thread_start_date=datetime.combine(start_date_1, time(13)))

    # Проверяем, что после изменения даты нитки изменения,
    # основная нитка была удалена из ThreadEvents,
    # а нитка изменение была добалена в ThreadEvents и UpdatedThread.
    thread_1_change.year_days = RunMask(days=[start_date_1])
    thread_1_change.save()
    precalc_all_expected_thread_events(start_date=datetime(2017, 3, 20))
    assert UpdatedThread.objects.count() == 1
    assert ThreadEvents.objects.count() == 1
    ThreadEvents.objects.get(key__thread_key='th_1_change_key',
                             key__thread_start_date=datetime.combine(start_date_1, time(15)))


def test_precalc_without_suburban_key():
    station_from, station_to = create_station(id=121), create_station(id=122)
    start_date = datetime(2017, 3, 20, 13)

    thread_1 = create_thread(
        tz_start_time=time(13),
        year_days=[start_date],
        schedule_v1=[
            [None, 0, station_from],
            [50, None, station_to],
        ],
    )

    thread_2 = create_thread(
        tz_start_time=time(15),
        year_days=[start_date],
        schedule_v1=[
            [None, 0, station_from],
            [50, None, station_to],
        ],
    )

    with replace_now(datetime(2017, 3, 20, 23)):
        SuburbanKey.objects.create(thread=thread_1, key='th_1_key')
        precalc_all_expected_thread_events()
        thread_events = list(ThreadEvents.objects.all())
        assert len(thread_events) == 1

        SuburbanKey.objects.create(thread=thread_2, key='th_2_key')
        precalc_all_expected_thread_events()
        thread_events = list(ThreadEvents.objects.all())
        assert len(thread_events) == 2


class TestThreadRematch(object):
    def test_thread_rematch_from_basic(self):
        station_from, station_to = create_station(id=121), create_station(id=124)
        station_mid_1, station_mid_2 = create_station(id=122), create_station(id=123)
        start_dt = datetime(2017, 3, 20, 10)

        thread = create_thread(
            uid=u'uid_1',
            number='404',
            tz_start_time=start_dt.time(),
            year_days=[start_dt + timedelta(days=1)],
            schedule_v1=[
                [None, 0, station_from],
                [10, 20, station_mid_1],
                [50, None, station_to],
            ],
        )
        thread_change = create_change_thread(
            thread, [start_dt.date()],
            uid=u'uid_1_change',
            schedule_v1=[
                [None, 0, station_from],
                [10, 20, station_mid_1],
                [30, 35, station_mid_2],
                [40, 45, station_mid_1],
                [50, None, station_to],
            ],
        )

        thread_key = get_thread_suburban_key(thread.number, station_from)
        SuburbanKey.objects.create(thread=thread, key=thread_key)
        SuburbanKey.objects.create(thread=thread_change, key=thread_key)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=start_dt,
                thread_key=thread_key,
                thread_type=ThreadEventsTypeCodes.SUBURBAN
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=start_dt,
                    dt_fact=start_dt,
                    passed_several_times=False
                ),
                StationEventFactory(
                    station_key=str(station_mid_1.id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=10),
                    dt_fact=start_dt + timedelta(minutes=15),
                    passed_several_times=False
                ),
            ],
            'need_recalc': True,
        })

        # Проверяем, что основная нитка удалена, а ее события перематчены на новую нитку.
        precalc_all_expected_thread_events(start_date=datetime(2017, 3, 20, 23))
        assert UpdatedThread.objects.count() == 1
        assert UpdatedThread.objects.get().uid == thread.uid
        assert ThreadEvents.objects.count() == 1
        assert_that(ThreadEvents.objects.get(), has_properties({
            'stations_events': contains(
                has_properties({
                    'station_key': str(station_from.id),
                    'dt_fact': start_dt,
                    'type': 'departure',
                    'passed_several_times': False
                }),
                has_properties({
                    'station_key': str(station_mid_1.id),
                    'dt_fact': start_dt + timedelta(minutes=15),
                    'type': 'arrival',
                    'passed_several_times': True
                })
            ),
            'stations_expected_events': contains(
                has_properties({
                    'station_key': str(station_from.id),
                    'dt_normative': start_dt,
                    'type': 'departure',
                    'passed_several_times': False
                }),
                has_properties({
                    'station_key': str(station_mid_1.id),
                    'dt_normative': start_dt + timedelta(minutes=10),
                    'type': 'arrival',
                    'passed_several_times': True
                }),
                has_properties({
                    'station_key': str(station_mid_1.id),
                    'dt_normative': start_dt + timedelta(minutes=20),
                    'type': 'departure',
                    'passed_several_times': True
                }),
                has_properties({
                    'station_key': str(station_mid_2.id),
                    'dt_normative': start_dt + timedelta(minutes=30),
                    'type': 'arrival',
                    'passed_several_times': False
                }),
                has_properties({
                    'station_key': str(station_mid_2.id),
                    'dt_normative': start_dt + timedelta(minutes=35),
                    'type': 'departure',
                    'passed_several_times': False
                }),
                has_properties({
                    'station_key': str(station_mid_1.id),
                    'dt_normative': start_dt + timedelta(minutes=40),
                    'type': 'arrival',
                    'passed_several_times': True
                }),
                has_properties({
                    'station_key': str(station_mid_1.id),
                    'dt_normative': start_dt + timedelta(minutes=45),
                    'type': 'departure',
                    'passed_several_times': True
                }),
                has_properties({
                    'station_key': str(station_to.id),
                    'dt_normative': start_dt + timedelta(minutes=50),
                    'type': 'arrival',
                    'passed_several_times': False
                })
            )
        }))

    def test_thread_rematch_to_basic(self):
        station_from, station_to = create_station(id=121), create_station(id=124)
        station_mid_1, station_mid_2 = create_station(id=122), create_station(id=123)
        start_dt = datetime(2017, 3, 20, 10)

        thread = create_thread(
            uid=u'uid_1',
            number='404',
            tz_start_time=start_dt.time(),
            year_days=[start_dt],
            schedule_v1=[
                [None, 0, station_from],
                [10, 20, station_mid_1],
                [50, None, station_to],
            ],
        )
        thread_change = create_change_thread(
            thread, [start_dt + timedelta(days=1)],
            uid=u'uid_1_change',
            schedule_v1=[
                [None, 0, station_from],
                [10, 20, station_mid_1],
                [30, 35, station_mid_2],
                [40, 45, station_mid_1],
                [50, None, station_to],
            ],
        )
        UpdatedThread.objects.create(uid=thread.uid, start_date=start_dt.date())

        thread_key = get_thread_suburban_key(thread.number, station_from)
        SuburbanKey.objects.create(thread=thread, key=thread_key)
        SuburbanKey.objects.create(thread=thread_change, key=thread_key)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=start_dt,
                thread_key=thread_key
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=start_dt,
                    dt_fact=start_dt
                ),
                StationEventFactory(
                    station_key=str(station_mid_1.id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=10),
                    dt_fact=start_dt + timedelta(minutes=15)
                ),
                StationEventFactory(
                    station_key=str(station_mid_2.id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=30),
                    dt_fact=start_dt + timedelta(minutes=32)
                )
            ],
            'need_recalc': True,
        })

        # Проверяем, что нитка изменения удалена, а ее события перематчены на основную нитку.
        precalc_all_expected_thread_events(start_date=datetime(2017, 3, 20, 23))
        assert UpdatedThread.objects.count() == 0
        assert ThreadEvents.objects.count() == 1
        assert_that(ThreadEvents.objects.get(), has_properties({
            'stations_events': contains(
                has_properties({
                    'station_key': '121',
                    'dt_fact': start_dt,
                    'type': 'departure'
                }),
                has_properties({
                    'station_key': '122',
                    'dt_fact': start_dt + timedelta(minutes=15),
                    'type': 'arrival'
                })
            ),
            'stations_expected_events': contains(
                has_properties({
                    'station_key': '121',
                    'dt_normative': start_dt,
                    'type': 'departure'
                }),
                has_properties({
                    'station_key': '122',
                    'dt_normative': start_dt + timedelta(minutes=10),
                    'type': 'arrival'
                }),
                has_properties({
                    'station_key': '122',
                    'dt_normative': start_dt + timedelta(minutes=20),
                    'type': 'departure'
                }),
                has_properties({
                    'station_key': '124',
                    'dt_normative': start_dt + timedelta(minutes=50),
                    'type': 'arrival'
                })
            )
        }))

    def test_thread_rematch_from_basic_different_start_time(self):
        station_from, station_mid, station_to = create_station(id=121), create_station(id=122), create_station(id=123)
        start_dt = datetime(2017, 3, 20, 10)

        thread = create_thread(
            uid=u'uid_1',
            number='404',
            tz_start_time=start_dt.time(),
            year_days=[start_dt + timedelta(days=1)],
            schedule_v1=[
                [None, 0, station_from],
                [10, 20, station_mid],
                [50, None, station_to]
            ]
        )
        # Создаем новую нитку изменение с другим временем старта.
        change_start_dt = start_dt + timedelta(minutes=15)
        thread_change = create_change_thread(
            basic_thread=thread,
            year_days=[start_dt.date()],
            uid=u'uid_change',
            tz_start_time=change_start_dt.time(),
        )

        thread_key = get_thread_suburban_key(thread.number, station_from)
        SuburbanKey.objects.create(thread=thread, key=thread_key)
        SuburbanKey.objects.create(thread=thread_change, key=thread_key)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=start_dt,
                thread_key=thread_key
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=start_dt,
                    dt_fact=start_dt
                ),
                StationEventFactory(
                    station_key=str(station_mid.id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=10),
                    dt_fact=start_dt + timedelta(minutes=15)
                )
            ],
            'need_recalc': True,
        })

        # Проверяем, что основная нитка удалена, а ее события перематчены на новую нитку.
        precalc_all_expected_thread_events(start_date=datetime(2017, 3, 20, 23))
        assert UpdatedThread.objects.count() == 1
        assert UpdatedThread.objects.get().uid == thread.uid
        assert ThreadEvents.objects.count() == 1
        assert_that(ThreadEvents.objects.get(), has_properties(
            {
                'key': has_properties({
                    'thread_key': thread_key,
                    'thread_start_date': change_start_dt
                }),
                'stations_events': contains(
                    has_properties({
                        'station_key': str(station_from.id),
                        'dt_fact': start_dt,
                        'type': 'departure'
                    }),
                    has_properties({
                        'station_key': str(station_mid.id),
                        'dt_fact': start_dt + timedelta(minutes=15),
                        'type': 'arrival'
                    })
                ),
                'stations_expected_events': contains(
                    has_properties({
                        'station_key': str(station_from.id),
                        'dt_normative': change_start_dt,
                        'type': 'departure'
                    }),
                    has_properties({
                        'station_key': str(station_mid.id),
                        'dt_normative': change_start_dt + timedelta(minutes=10),
                        'type': 'arrival'
                    }),
                    has_properties({
                        'station_key': str(station_mid.id),
                        'dt_normative': change_start_dt + timedelta(minutes=20),
                        'type': 'departure'
                    }),
                    has_properties({
                        'station_key': str(station_to.id),
                        'dt_normative': change_start_dt + timedelta(minutes=50),
                        'type': 'arrival'
                    })
                )
            }
        ))
