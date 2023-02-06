# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from copy import copy
from datetime import datetime, date, time, timedelta

import mock
import pytest
from hamcrest import assert_that, has_properties, contains

from common.apps.suburban_events import models
from common.apps.suburban_events.factories import ThreadEventsFactory, StationExpectedEvent
from common.apps.suburban_events.forecast.match_rzd import (
    EventsMatcher, RzdEvent, station_to_str, MCZKEventsMatcher, get_new_events
)
from common.apps.suburban_events.models import ThreadKey, LVGD01_TR2PROC_feed
from common.apps.suburban_events.utils import ThreadEventsTypeCodes, ClockDirection
from common.models.geo import CodeSystem
from common.models.schedule import RThreadType
from common.tester.factories import create_thread, create_station, create_station_code
from common.tester.testcase import TestCase
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from travel.rasp.library.python.common23.date import environment

create_thread = create_thread.mutate(t_type="suburban")
create_station = create_station.mutate(t_type="suburban")


def create_rzd_feed_row(i, **kwargs):
    row = {
        'ID_TRAIN': i,
        'IDTR': i,
        'IDRASP': i,
        'STORASP': i,
        'STOEX': i,
        'NAMESTO': '{}_{}'.format(i, i),
        'STNRASP': i,
        'STNEX': i,
        'NAMESTN': '{}_{}'.format(i, i),
        'NOMPEX': '{}_{}'.format(i, i),
        'NAMEP': '{}_{}'.format(i, i),
        'SOURCE': 1,
        'KODOP': i,
        'DOR': i,
        'OTD': i,
        'NOMRP': i,
        'STOPER': i,
        'STOPEREX': i,
        'STNAME': '{}_{}'.format(i, i),
        'TIMEOPER_N': environment.now(),
        'TIMEOPER_F': environment.now(),
        'KM': i,
        'PRSTOP': i,
        'PRIORITY': i,
        'PRIORITY_RATING': i,
    }
    row.update(kwargs)
    lvgd_obj = LVGD01_TR2PROC_feed(**row)
    lvgd_obj.save()

    return lvgd_obj


def create_rzd_event(**kwargs):
    event_data_defaults = {
        'KODOP': 1,
    }
    event_data_defaults.update(kwargs)
    return RzdEvent(event_data=event_data_defaults)


@pytest.mark.dbuser
def test_calc_rts_event_db_date():
    station_b, station_mid, station_e = create_station(), create_station(), create_station()

    # Время прибытия на станцию в базе - 23:59
    # Время РЖД  - 0:0
    # Нужно вернуть дату предыдущего дня относительно РЖД
    thread = create_thread(
        tz_start_time=time(23, 50),
        schedule_v1=[
            [None, 0, station_b],
            [9, 20, station_mid],
            [50, None, station_e],
        ],
    )
    rts = thread.path[1]
    event = create_rzd_event(TIMEOPER_N=datetime(2017, 3, 6, 0, 0))
    assert EventsMatcher.calc_rts_event_db_date(event, rts, thread) == date(2017, 3, 5)

    # Время прибытия на станцию в базе - 0:01
    # Время РЖД  - 23:55
    # Нужно вернуть дату следующего дня относительно РЖД
    thread = create_thread(
        tz_start_time=time(23, 50),
        schedule_v1=[
            [None, 0, station_b],
            [11, 12, station_mid],
            [50, None, station_e],
        ],
    )
    rts = thread.path[1]
    event = create_rzd_event(TIMEOPER_N=datetime(2017, 3, 6, 23, 55))
    assert EventsMatcher.calc_rts_event_db_date(event, rts, thread) == date(2017, 3, 7)

    thread = create_thread(
        tz_start_time=time(23, 50),
        schedule_v1=[
            [None, 0, station_b],
            [5, 6, station_mid],
            [50, None, station_e],
        ],
    )
    rts = thread.path[1]
    event = create_rzd_event(TIMEOPER_N=datetime(2017, 3, 6, 23, 55))
    assert EventsMatcher.calc_rts_event_db_date(event, rts, thread) == date(2017, 3, 6)

    event = create_rzd_event(TIMEOPER_N=datetime(2017, 3, 6, 23, 58))
    assert EventsMatcher.calc_rts_event_db_date(event, rts, thread) == date(2017, 3, 6)

    event = create_rzd_event(TIMEOPER_N=datetime(2017, 3, 6, 23, 53))
    assert EventsMatcher.calc_rts_event_db_date(event, rts, thread) == date(2017, 3, 6)

    thread = create_thread(
        tz_start_time=time(23, 55),
        schedule_v1=[
            [None, 0, station_b],
            [5, 6, station_mid],
            [50, None, station_e],
        ],
    )
    rts = thread.path[1]
    event = create_rzd_event(TIMEOPER_N=datetime(2017, 4, 14, 0, 1))
    assert EventsMatcher.calc_rts_event_db_date(event, rts, thread) == date(2017, 4, 14)


def test_events_matcher_prepare_raw_events():
    event_1 = {
        'STOPEREX': 2044810,
        'TIMEOPER_N': datetime(2017, 3, 10, 17, 49),
        'TIMEOPER_F': datetime(2017, 3, 10, 14, 56),
    }

    event_2 = copy(event_1)
    event_2['STOPEREX'] = 2700740

    matcher = EventsMatcher([event_1, event_2])
    assert matcher.raw_events[0]['TIMEOPER_N'] == datetime(2017, 3, 10, 17, 49)
    # экспресс код станции начинается с 27, время приводится к MSK_TZ
    assert matcher.raw_events[1]['TIMEOPER_N'] == datetime(2017, 3, 10, 14, 49)


class TestEventsMatcher(TestCase):
    def test_match(self):
        log = mock.Mock()

        station_from, station_to = create_station(), create_station()
        station_mid_1, station_mid_2 = create_station(), create_station()

        esr_code_system = CodeSystem.objects.get(code='rzd_esr')
        create_station_code(station=station_from, system=esr_code_system, code='121')
        create_station_code(station=station_to, system=esr_code_system, code='122')
        create_station_code(station=station_mid_1, system=esr_code_system, code='123')
        create_station_code(station=station_mid_2, system=esr_code_system, code='124')

        thread_1 = create_thread(
            number='6963',
            tz_start_time=time(17, 5),
            schedule_v1=[
                [None, 0, station_from],
                [10, 20, station_mid_1],
                [50, None, station_to],
            ],
        )

        thread_2 = create_thread(
            number='7426',
            tz_start_time=time(15, 0),
            schedule_v1=[
                [None, 0, station_from],
                [20, 25, station_mid_1],
                [35, 40, station_mid_2],
                [50, None, station_to],
            ],
        )
        thread_3 = create_thread(
            number='7426/7425/6852',
            tz_start_time=time(15, 0),
            schedule_v1=[
                [None, 0, station_from],
                [20, 25, station_mid_1],
                [35, 40, station_mid_2],
                [50, None, station_to],
            ],
        )

        thread_4 = create_thread(  # noqa
            number='1242/7436',
            tz_start_time=time(15, 0),
            schedule_v1=[
                [None, 0, station_from],
                [5, 10, station_mid_1],
                [180, 190, station_mid_2],
                [240, None, station_to],
            ],
        )
        thread_5 = create_thread(
            number='5213/1242/6852',
            tz_start_time=time(15, 0),
            schedule_v1=[
                [None, 0, station_from],
                [20, 25, station_mid_1],
                [360, 370, station_mid_2],
                [380, None, station_to],
            ],
        )

        thread_6 = create_thread(
            number='1415',
            tz_start_time=time(15, 0),
            schedule_v1=[
                [None, 0, station_from],
                [60, 70, station_mid_1],
                [120, 130, station_mid_2],
                [360, 370, station_mid_1],
                [380, None, station_to],
            ],
        )

        event_1 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 123,
            'STNRASP': 122,
            'STORASP': 121,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'mid',
            'NOMPEX': u'6963',
            'KODOP': 1,
            'TIMEOPER_N': datetime(2017, 3, 10, 17, 15),
            'TIMEOPER_F': datetime(2017, 3, 10, 17, 20),
            'PRIORITY_RATING': 0.2
        }

        event_2 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 124,
            'STNRASP': 122,
            'STORASP': 121,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'mid',
            'NOMPEX': u'7426',
            'KODOP': 3,
            'TIMEOPER_N': datetime(2017, 3, 10, 15, 36),
            'TIMEOPER_F': datetime(2017, 3, 10, 15, 40),
            'PRIORITY_RATING': 0.1
        }

        event_3 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 124,
            'STNRASP': 122,
            'STORASP': 121,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'mid',
            'NOMPEX': u'1242',
            'KODOP': 1,
            'TIMEOPER_N': datetime(2017, 3, 10, 21),
            'TIMEOPER_F': datetime(2017, 3, 10, 21, 20),
            'PRIORITY_RATING': 0.1
        }

        event_4 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 123,
            'STNRASP': 122,
            'STORASP': 121,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'mid',
            'NOMPEX': u'1415',
            'KODOP': 1,
            'TIMEOPER_N': datetime(2017, 3, 10, 21, 2),
            'TIMEOPER_F': datetime(2017, 3, 10, 21, 10),
            'PRIORITY_RATING': 0.1
        }

        event_5 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 123,
            'STNRASP': 122,
            'STORASP': 121,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'mid',
            'NOMPEX': u'1415',
            'KODOP': 3,
            'TIMEOPER_N': datetime(2017, 3, 10, 16, 1),
            'TIMEOPER_F': datetime(2017, 3, 10, 16, 5),
            'PRIORITY_RATING': 0.1
        }

        event_6 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 1000,
            'STNRASP': 1000,
            'STORASP': 1000,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'mid',
            'NOMPEX': u'1415',
            'KODOP': 3,
            'TIMEOPER_N': datetime(2017, 3, 10, 17, 49),
            'TIMEOPER_F': datetime(2017, 3, 10, 14, 56),
        }

        matcher = EventsMatcher([event_1, event_2, event_3, event_4, event_5, event_6], log)
        matched_events, not_matched_events = matcher.match()

        assert len(matched_events) == 5
        assert len(not_matched_events) == 1

        # Событие совпадает с одной ниткой.
        res_events = list(matched_events[0].get_result_events())
        assert len(res_events) == 1
        assert res_events[0].thread.uid == thread_1.uid
        assert res_events[0].thread_start_date == datetime(2017, 3, 10, 17, 5)
        assert res_events[0].passed_several_times is False
        assert res_events[0].dt_normative == datetime(2017, 3, 10, 17, 15)
        assert res_events[0].dt_fact == datetime(2017, 3, 10, 17, 20)
        assert res_events[0].twin_key == u'6963'
        assert res_events[0].type == u'arrival'
        assert res_events[0].rtstation.tz_arrival == 10
        assert res_events[0].rtstation.station == station_mid_1
        assert res_events[0].weight == 0.2

        # Две нитки с пересекающимся номером проходят в одно время по одной станции.
        # Событие соответствует двум ниткам.
        res_events = list(matched_events[1].get_result_events())
        assert len(res_events) == 2
        assert {e.thread.uid for e in res_events} == {thread_2.uid, thread_3.uid}
        assert {e.thread_start_date for e in res_events} == {datetime(2017, 3, 10, 15)}
        assert res_events[1].passed_several_times is False

        # Нитки и времена прохождения разные, но номера и крайние станции пересекаются.
        # Выбираем ближайшую к событию нитку.
        res_events = list(matched_events[2].get_result_events())
        assert len(res_events) == 1
        assert res_events[0].thread.uid == thread_5.uid
        assert res_events[0].thread_start_date == datetime(2017, 3, 10, 15)
        assert res_events[0].passed_several_times is False

        # Нитка проходит станцию 2 раза.
        # В зависимости от времени получаем разные rts.
        res_events = list(matched_events[3].get_result_events())
        assert len(res_events) == 1
        assert res_events[0].thread.uid == thread_6.uid
        assert res_events[0].thread_start_date == datetime(2017, 3, 10, 15)
        assert res_events[0].rtstation.tz_arrival == 360
        assert res_events[0].rtstation.station == station_mid_1
        assert res_events[0].passed_several_times is True

        res_events = list(matched_events[4].get_result_events())
        assert len(res_events) == 1
        assert res_events[0].thread.uid == thread_6.uid
        assert res_events[0].thread_start_date == datetime(2017, 3, 10, 15)
        assert res_events[0].rtstation.tz_arrival == 60
        assert res_events[0].rtstation.station == station_mid_1
        assert res_events[0].passed_several_times is True
        assert res_events[0].dt_normative == datetime(2017, 3, 10, 16, 1)
        assert res_events[0].dt_fact == datetime(2017, 3, 10, 16, 5)
        assert res_events[0].twin_key == u'1415'
        assert res_events[0].type == u'departure'
        assert res_events[0].weight == 0.1

    def test_updated_thread_exclude(self):
        log = mock.Mock()
        station_from, station_to = create_station(), create_station()
        station_mid_1, station_mid_2 = create_station(), create_station()

        esr_code_system = CodeSystem.objects.get(code='rzd_esr')
        create_station_code(station=station_from, system=esr_code_system, code='121')
        create_station_code(station=station_to, system=esr_code_system, code='122')
        create_station_code(station=station_mid_1, system=esr_code_system, code='123')
        create_station_code(station=station_mid_2, system=esr_code_system, code='124')

        thread_4 = create_thread(
            number='1242',
            tz_start_time=time(15, 0),
            schedule_v1=[
                [None, 0, station_from],
                [5, 10, station_mid_1],
                [180, 190, station_mid_2],
                [240, None, station_to],
            ],
        )
        thread_5 = create_thread(
            number='1242',
            tz_start_time=time(15, 0),
            schedule_v1=[
                [None, 0, station_from],
                [20, 25, station_mid_1],
                [360, 370, station_mid_2],
                [380, None, station_to],
            ],
        )

        event = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 124,
            'STNRASP': 122,
            'STORASP': 121,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'mid',
            'NOMPEX': u'1242',
            'KODOP': 1,
            'TIMEOPER_N': datetime(2017, 3, 10, 21),
            'TIMEOPER_F': datetime(2017, 3, 10, 21, 20),
            'PRIORITY_RATING': 0.1
        }

        matcher = EventsMatcher([event], log)
        matched_events, not_matched_events = matcher.match()

        # Номера и станции одинаковые.
        # Выбираем из двух ближайшую к событию нитку.
        res_events = list(matched_events[0].get_result_events())
        assert len(res_events) == 1
        assert res_events[0].thread.uid == thread_5.uid
        assert res_events[0].thread_start_date == datetime(2017, 3, 10, 15)

        models.UpdatedThread.objects.create(
            uid=thread_5.uid,
            start_date=date(2017, 3, 10)
        )

        matcher = EventsMatcher([event], log)
        matched_events, not_matched_events = matcher.match()

        # Исключаем ближайшую нитку.
        # Должны выбрать вторую.
        res_events = list(matched_events[0].get_result_events())
        assert len(res_events) == 1
        assert res_events[0].thread.uid == thread_4.uid
        assert res_events[0].thread_start_date == datetime(2017, 3, 10, 15)

    @replace_now(datetime(2017, 3, 10, 17))
    def test_cancel_match(self):
        log = mock.Mock()

        station_from, station_to = create_station(), create_station()
        cancel_thread_type = RThreadType.objects.get(id=RThreadType.CANCEL_ID)
        esr_code_system = CodeSystem.objects.get(code='rzd_esr')
        create_station_code(station=station_from, system=esr_code_system, code='121')
        create_station_code(station=station_to, system=esr_code_system, code='122')

        thread_1 = create_thread(
            number='6963',
            tz_start_time=time(17, 5),
            schedule_v1=[
                [None, 0, station_from],
                [50, None, station_to],
            ],
        )

        event_1 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 121,
            'STNRASP': 122,
            'STORASP': 121,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'to',
            'NOMPEX': u'6963',
            'KODOP': 3,
            'TIMEOPER_N': datetime(2017, 3, 10, 17, 5),
            'TIMEOPER_F': datetime(2017, 3, 10, 17, 6),
            'PRIORITY_RATING': 0.2
        }

        matcher = EventsMatcher([event_1], log)
        matched_events, not_matched_events = matcher.match()
        assert len(matched_events) == 1
        assert len(not_matched_events) == 0

        thread_1.type = cancel_thread_type
        thread_1.save()
        matcher = EventsMatcher([event_1], log)
        matched_events, not_matched_events = matcher.match()
        assert len(matched_events) == 0
        assert len(not_matched_events) == 1

    def test_match_threads(self):

        def create_event(name, is_valid):
            return {
                'STOPEREX': 1000 if is_valid else 2000,
                'STNAME': name,
                'NOMPEX': u'6963',
            }

        def m_event_is_valid(rzd_event):
            return rzd_event.event_data['STOPEREX'] == 1000

        def compare_events(events, names):
            assert [e.event_data['STNAME'] for e in events] == names

        matcher = EventsMatcher([
            create_event(u'a', False),
            create_event(u'b', True),
            create_event(u'c', True),
            create_event(u'd', False),
        ])

        # только эти события валидны, и для них должны быть вызваны нужные методы при матчинге
        expected_events = [u'b', u'c']

        with mock.patch.object(RzdEvent, 'is_valid_to_match_thread', m_event_is_valid), \
                mock.patch.object(RzdEvent, 'validate_thread') as m_validate_thread, \
                mock.patch.object(matcher, 'match_threads_by_number_and_stations',
                                  wraps=matcher.match_threads_by_number_and_stations) as m_thread_match1, \
                mock.patch.object(matcher, 'match_threads_oper_station_and_date',
                                  wraps=matcher.match_threads_oper_station_and_date) as m_thread_match2:

            matcher.match_threads()

            assert len(m_validate_thread.call_args_list) == 2

            thread_match1_calls = m_thread_match1.call_args_list
            assert len(thread_match1_calls) == 1
            compare_events(thread_match1_calls[0][0][0], expected_events)

            thread_match2_calls = m_thread_match2.call_args_list
            assert len(thread_match2_calls) == 1
            compare_events(thread_match2_calls[0][0][0], expected_events)


@pytest.mark.dbuser
def test_station_to_str():
    station = create_station(title='wow', id=42)
    assert station_to_str(station) == u'wow(42)'
    assert station_to_str(None) == u'None'


@pytest.mark.mongouser
@pytest.mark.dbuser
class TestMCZKEventsMatcher(object):
    @replace_now(datetime(2017, 11, 9))
    def test_match(self):
        log = mock.Mock()

        station_from, station_to = create_station(), create_station()
        station_mid_1, station_mid_2 = create_station(), create_station()

        esr_code_system = CodeSystem.objects.get(code='rzd_esr')
        create_station_code(station=station_from, system=esr_code_system, code='121')
        create_station_code(station=station_to, system=esr_code_system, code='122')
        create_station_code(station=station_mid_1, system=esr_code_system, code='123')
        create_station_code(station=station_mid_2, system=esr_code_system, code='124')

        thread_key_1, thread_key_2, thread_key_3 = 'thread_key_1', 'thread_key_2', 'thread_key_3'
        thread_1_start_dt = datetime(2017, 11, 9, 15)
        thread_2_start_dt = datetime(2017, 11, 9, 15, 15)
        thread_3_start_dt = datetime(2017, 11, 9, 15, 30)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_1_start_dt,
                thread_key=thread_key_1,
                thread_type=ThreadEventsTypeCodes.MCZK,
                clock_direction=ClockDirection.CLOCK_WISE
            ),
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=thread_1_start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_1.id),
                    type='arrival',
                    dt_normative=thread_1_start_dt + timedelta(minutes=10),
                    time=10,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_1.id),
                    type='departure',
                    dt_normative=thread_1_start_dt + timedelta(minutes=15),
                    time=15,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_2.id),
                    type='arrival',
                    dt_normative=thread_1_start_dt + timedelta(minutes=25),
                    time=25,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_2.id),
                    type='departure',
                    dt_normative=thread_1_start_dt + timedelta(minutes=30),
                    time=30,
                ),
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='arrival',
                    dt_normative=thread_1_start_dt + timedelta(minutes=50),
                    time=50,
                ),
            ],
        })

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_2_start_dt,
                thread_key=thread_key_2,
                thread_type=ThreadEventsTypeCodes.MCZK,
                clock_direction=ClockDirection.CLOCK_WISE
            ),
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_mid_1.id),
                    type='departure',
                    dt_normative=thread_2_start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_2.id),
                    type='arrival',
                    dt_normative=thread_2_start_dt + timedelta(minutes=10),
                    time=10,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_2.id),
                    type='departure',
                    dt_normative=thread_2_start_dt + timedelta(minutes=15),
                    time=15,
                ),
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='arrival',
                    dt_normative=thread_2_start_dt + timedelta(minutes=35),
                    time=35,
                ),
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='departure',
                    dt_normative=thread_2_start_dt + timedelta(minutes=40),
                    time=40,
                ),
                StationExpectedEvent(
                    station_key=str(station_from.id),
                    type='arrival',
                    dt_normative=thread_2_start_dt + timedelta(minutes=45),
                    time=45,
                ),
            ],
        })

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_3_start_dt,
                thread_key=thread_key_3,
                thread_type=ThreadEventsTypeCodes.MCZK,
                clock_direction=ClockDirection.C_CLOCK_WISE
            ),
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_mid_2.id),
                    type='departure',
                    dt_normative=thread_3_start_dt,
                    time=0,
                    passed_several_times=True
                ),
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='arrival',
                    dt_normative=thread_3_start_dt + timedelta(minutes=20),
                    time=20,
                ),
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='departure',
                    dt_normative=thread_3_start_dt + timedelta(minutes=25),
                    time=25,
                ),
                StationExpectedEvent(
                    station_key=str(station_from.id),
                    type='arrival',
                    dt_normative=thread_3_start_dt + timedelta(minutes=30),
                    time=30,
                ),
                StationExpectedEvent(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=thread_3_start_dt + timedelta(minutes=35),
                    time=35,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_1.id),
                    type='arrival',
                    dt_normative=thread_3_start_dt + timedelta(minutes=40),
                    time=40,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_1.id),
                    type='departure',
                    dt_normative=thread_3_start_dt + timedelta(minutes=45),
                    time=45,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_2.id),
                    type='arrival',
                    dt_normative=thread_3_start_dt + timedelta(minutes=55),
                    time=55,
                    passed_several_times=True
                ),

            ],
        })

        event_1 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 124,
            'STNRASP': 122,
            'STORASP': 121,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'mid',
            'NOMPEX': u'МКЖД_5021',
            'KODOP': 1,
            'TIMEOPER_N': datetime(2017, 11, 9, 15, 25),
            'TIMEOPER_F': datetime(2017, 11, 9, 15, 28),
            'PRIORITY_RATING': 0.1
        }

        event_2 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 123,
            'STNRASP': 122,
            'STORASP': 121,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'mid',
            'NOMPEX': u'МКЖД_5020',
            'KODOP': 1,
            'TIMEOPER_N': datetime(2017, 11, 9, 16, 11),  # Увеличиваем время на минуту, чтобы проверить логику.
            'TIMEOPER_F': datetime(2017, 11, 9, 16, 21),
            'PRIORITY_RATING': 0.1
        }

        event_3 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 121,
            'STNRASP': 123,
            'STORASP': 124,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'mid',
            'NOMPEX': u'МКЖД_5020',
            'KODOP': 3,
            'TIMEOPER_N': datetime(2017, 11, 9, 16, 5),
            'TIMEOPER_F': datetime(2017, 11, 9, 16, 12),
            'PRIORITY_RATING': 0.1
        }

        event_4 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 124,
            'STNRASP': 122,
            'STORASP': 121,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'mid',
            'NOMPEX': u'1242',
            'KODOP': 1,
            'TIMEOPER_N': datetime(2017, 11, 9, 15, 15),
            'TIMEOPER_F': datetime(2017, 11, 9, 15, 18),
            'PRIORITY_RATING': 0.1
        }

        event_5 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 124,
            'STNRASP': 122,
            'STORASP': 121,
            'NAMESTO': u'from',
            'NAMESTN': u'to',
            'STNAME': u'mid',
            'NOMPEX': u'МКЖД_5020',
            'KODOP': 1,
            'TIMEOPER_N': datetime(2017, 11, 9, 16, 25),
            'TIMEOPER_F': datetime(2017, 11, 9, 16, 28),
            'PRIORITY_RATING': 0.1
        }

        matcher = MCZKEventsMatcher([event_1, event_2, event_3, event_4, event_5], log)
        matched_events, not_matched_events = matcher.match()

        assert len(matched_events) == 4
        assert len(not_matched_events) == 1

        # Событие_1 совпадает с двумя нитками (thread_1, thread_2).
        # Событие_2 совпадает с 1 ниткой (thread_3).
        # Событие_3 совпадает с 1 ниткой (thread_3).
        # Событие_4 не совпадает ни с одной ниткой.
        # Событие_5 совпадает с 1 ниткой (thread_3). Со станцией, которая встречается в пути 2 раза.
        res_events = list(matched_events[0].get_result_events())
        assert len(res_events) == 2
        assert_that(res_events[0],
                    has_properties({
                        'dt_normative': datetime(2017, 11, 9, 15, 25),
                        'dt_fact': datetime(2017, 11, 9, 15, 28),
                        'thread_start_date': datetime(2017, 11, 9, 15),
                        'type': 'arrival',
                        'passed_several_times': False
                    }))

        assert_that(res_events[1],
                    has_properties({
                        'dt_normative': datetime(2017, 11, 9, 15, 25),
                        'dt_fact': datetime(2017, 11, 9, 15, 28),
                        'thread_start_date': datetime(2017, 11, 9, 15, 15),
                        'type': 'arrival',
                        'passed_several_times': False
                    }))

        res_events = list(matched_events[1].get_result_events())
        assert len(res_events) == 1
        assert_that(res_events[0],
                    has_properties({
                        'dt_normative': datetime(2017, 11, 9, 16, 11),
                        'dt_fact': datetime(2017, 11, 9, 16, 21),
                        'thread_start_date': datetime(2017, 11, 9, 15, 30),
                        'type': 'arrival',
                        'passed_several_times': False
                    }))

        res_events = list(matched_events[2].get_result_events())
        assert len(res_events) == 1
        assert_that(res_events[0],
                    has_properties({
                        'dt_normative': datetime(2017, 11, 9, 16, 5),
                        'dt_fact': datetime(2017, 11, 9, 16, 12),
                        'thread_start_date': datetime(2017, 11, 9, 15, 30),
                        'type': 'departure',
                        'passed_several_times': False
                    }))

        res_events = list(matched_events[3].get_result_events())
        assert len(res_events) == 1
        assert_that(res_events[0],
                    has_properties({
                        'dt_normative': datetime(2017, 11, 9, 16, 25),
                        'dt_fact': datetime(2017, 11, 9, 16, 28),
                        'thread_start_date': datetime(2017, 11, 9, 15, 30),
                        'type': 'arrival',
                        'passed_several_times': True
                    }))

        # Событие не проходящее матчинг по STOPER
        event_6 = {
            'STOPEREX': 1000,
            'STNEX': 1000,
            'STOEX': 1000,
            'STOPER': 130,
            'STNRASP': 122,
            'STORASP': 121,
            'NAMESTO': 'from',
            'NAMESTN': 'to',
            'STNAME': 'mid',
            'NOMPEX': '1242',
            'KODOP': 1,
            'TIMEOPER_N': datetime(2017, 11, 9, 15, 15),
            'TIMEOPER_F': datetime(2017, 11, 9, 15, 18),
            'PRIORITY_RATING': 0.1
        }
        matcher = MCZKEventsMatcher([event_6], log)
        matched_events, not_matched_events = matcher.match()


@pytest.mark.mongouser
class TestGetNewEvents(object):
    def test_source_filter(self):
        create_rzd_feed_row(1, SOURCE=1)
        create_rzd_feed_row(2, SOURCE=3)
        create_rzd_feed_row(3, SOURCE=3)
        create_rzd_feed_row(4, SOURCE=66)

        events, _ = get_new_events(None)
        assert len(events) == 2
        assert all(e['SOURCE'] != 3 for e in events)

    def test_max_events_limit(self):
        create_rzd_feed_row(1)
        obj_2 = create_rzd_feed_row(2)
        create_rzd_feed_row(3)
        create_rzd_feed_row(4)
        create_rzd_feed_row(5)

        events, _ = get_new_events(None)
        assert_that([e['ID_TRAIN'] for e in events], contains(1, 2, 3, 4, 5))

        with replace_dynamic_setting('SUBURBAN_MAX_EVENTS_TO_MATCH', 3):
            events, _ = get_new_events(None)
            assert_that([e['ID_TRAIN'] for e in events], contains(1, 2, 3))

        with replace_dynamic_setting('SUBURBAN_MAX_EVENTS_TO_MATCH', 2):
            events, _ = get_new_events(obj_2.id)
            assert_that([e['ID_TRAIN'] for e in events], contains(3, 4))
