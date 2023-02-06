# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import os
from datetime import datetime, time, timedelta

import mock
import pytest
from hamcrest import assert_that, contains_inanyorder, has_properties

from common.apps.suburban_events.dynamic_params import set_param
from common.apps.suburban_events.factories import (
    CompanyCrashFactory, StationEventFactory, StationCancelFactory, StationUpdateInfoFactory, ThreadEventsFactory,
    ThreadStationKeyFactory
)
from common.apps.suburban_events.forecast import forecast
from common.apps.suburban_events.forecast.forecast import (
    Forecaster, filter_events_by_prev_max_next_min, compare_tsses_cancels_first, filter_events_by_weights,
    find_rts_for_events, get_closest_rts, recalc_forecasts, save_forecast_parallel
)
from common.apps.suburban_events.models import (
    EventState, ForecastRepresentation, StationEvent, StationExpectedEvent, SuburbanKey, ThreadEvents, ThreadKey,
    ThreadStationKey, ThreadStationState
)
from common.apps.suburban_events.utils import (
    ClockDirection, EventStateSubType, EventStateType, ThreadEventsTypeCodes, get_thread_suburban_key
)
from common.models.schedule import TrainTurnover
from common.tester.factories import create_thread, create_station, create_train_schedule_plan, create_company
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_dynamic_setting
from common.utils.date import RunMask


create_thread = create_thread.mutate(t_type="suburban")
create_station = create_station.mutate(t_type="suburban")

pytestmark = [pytest.mark.mongouser('module'), pytest.mark.dbuser('module')]


def create_station_event(rts=None, **kwargs):
    kwargs_with_defaults = {
        'type': 'arrival',
        'station_key': '123',
        'dt_normative': datetime.now(),
        'dt_fact': datetime.now(),
        'time': 10,
        'twin_key': '456',
        'weight': 1,
        'passed_several_times': False,
    }

    kwargs_with_defaults.update(kwargs)
    event = StationEvent(**kwargs_with_defaults)
    if rts:
        event.rts = rts

    return event


def test_filter_events_by_weights():
    station_1, station_2 = create_station(id=21), create_station(id=22)
    thread = create_thread(
        schedule_v1=[
            [None, 0, station_2],
            [30, 31, station_1],
            [60, 61, station_1],
            [90, None, station_2],
        ],
    )
    rtstations = thread.path
    e1 = create_station_event(rts=rtstations[2], type='arrival', weight=3)
    e2 = create_station_event(rts=rtstations[2], type='departure', weight=3)
    e3 = create_station_event(rts=rtstations[1], type='arrival', weight=2)
    e4 = create_station_event(rts=rtstations[3], type='arrival', weight=2)

    events = [
        e2,
        create_station_event(rts=rtstations[1], type='arrival', weight=1),
        create_station_event(rts=rtstations[2], type='arrival', weight=2),
        e3,
        e4,
        e1,
        create_station_event(rts=rtstations[2], type='arrival', weight=1),
        create_station_event(rts=rtstations[2], type='departure', weight=1),
    ]

    result = filter_events_by_weights(events)
    assert isinstance(result, list)
    assert sorted(result) == sorted([e1, e2, e3, e4])


class TestRecalcForecasts(object):
    def test_recalc_forecasts(self):
        thread_key_1 = 'thread_key_1'
        thread_key_2 = 'thread_key_2'
        thread_start_dt_1 = datetime(2017, 3, 20, 13)
        dt_normative_1_a = datetime(2017, 3, 20, 13, 15)
        dt_fact_1_a = datetime(2017, 3, 20, 13, 19)
        dt_normative_1_d = datetime(2017, 3, 20, 13, 20)
        dt_fact_1_d = datetime(2017, 3, 20, 13, 35)

        thread_start_dt_2 = datetime(2017, 3, 20, 15)
        dt_normative_2_a = datetime(2017, 3, 20, 15, 20)
        dt_fact_2_a = datetime(2017, 3, 20, 15, 22)
        dt_normative_2_d = datetime(2017, 3, 20, 15, 25)
        dt_fact_2_d = datetime(2017, 3, 20, 15, 29)

        station_from, station_to = create_station(), create_station()
        station_mid_1, station_mid_2 = create_station(), create_station()

        thread_1 = create_thread(
            uid='uid_1',
            tz_start_time=thread_start_dt_1.time(),
            schedule_v1=[
                [None, 0, station_from],
                [10, 15, station_mid_1],
                [50, None, station_to],
            ],
        )
        thread_2 = create_thread(
            uid='uid_2',
            tz_start_time=thread_start_dt_2.time(),
            schedule_v1=[
                [None, 0, station_from],
                [20, 25, station_mid_1],
                [35, 40, station_mid_2],
                [50, None, station_to],
            ],
        )

        SuburbanKey.objects.create(thread=thread_1, key=thread_key_1)
        SuburbanKey.objects.create(thread=thread_2, key=thread_key_2)

        ThreadEventsFactory(**{
            'key': ThreadKey(thread_start_date=thread_start_dt_1, thread_key=thread_key_1),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_mid_1.id), type='arrival',
                    dt_normative=dt_normative_1_a, dt_fact=dt_fact_1_a,
                ),
                StationEventFactory(
                    station_key=str(station_mid_1.id), type='departure',
                    dt_normative=dt_normative_1_d, dt_fact=dt_fact_1_d,
                )
            ],
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_from.id), type='arrival',
                    dt_normative=thread_start_dt_1, time=0,
                ),
            ],
            'need_recalc': True
        })

        thread_2_events = ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_start_dt_2,
                thread_key=thread_key_2
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_mid_1.id), type='arrival',
                    dt_normative=dt_normative_2_a, dt_fact=dt_fact_2_a,
                ),
                StationEventFactory(
                    station_key=str(station_mid_1.id), type='departure',
                    dt_normative=dt_normative_2_d, dt_fact=dt_fact_2_d,
                )
            ],
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_from.id),
                    type='arrival',
                    dt_normative=thread_start_dt_2,
                    time=0,
                ),
            ],
            'need_recalc': True
        })

        assert all(event.need_recalc is True for event in ThreadEvents.objects.all())
        recalc_forecasts()
        assert all(event.need_recalc is False for event in ThreadEvents.objects.all())

        assert ThreadStationState.objects.count() == 5

        thread_states_1_mid = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                             key__station_key=str(station_mid_1.id))
        thread_states_1_to = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                            key__station_key=str(station_to.id))

        # События первой нитки.
        assert_that(thread_states_1_mid.arrival_state,
                    has_properties({
                        'dt': datetime(2017, 3, 20, 13, 19),
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_1',
                        # разница с нормативным временем из базы, а не из события
                        'minutes_from': 9,
                        'minutes_to': 9,
                    }))

        assert_that(thread_states_1_mid.departure_state,
                    has_properties({
                        'dt': datetime(2017, 3, 20, 13, 35),
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_1',
                        # разница с нормативным временем из базы, а не из события
                        'minutes_from': 20,
                        'minutes_to': 20,
                    }))

        assert_that(thread_states_1_to.arrival_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'thread_uid': 'uid_1',
                    }))

        thread_states_2_mid_1 = ThreadStationState.objects.get(key__thread_key=thread_key_2,
                                                               key__station_key=str(station_mid_1.id))
        thread_states_2_mid_2 = ThreadStationState.objects.get(key__thread_key=thread_key_2,
                                                               key__station_key=str(station_mid_2.id))
        thread_states_2_to = ThreadStationState.objects.get(key__thread_key=thread_key_2,
                                                            key__station_key=str(station_to.id))

        # события второй нитки
        assert_that(thread_states_2_mid_1.arrival_state,
                    has_properties({
                        'dt': datetime(2017, 3, 20, 15, 22),
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_2',
                        'minutes_from': 2,
                        'minutes_to': 2
                    }))
        assert_that(thread_states_2_mid_1.departure_state,
                    has_properties({
                        'dt': datetime(2017, 3, 20, 15, 29),
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_2',
                        'minutes_from': 4,
                        'minutes_to': 4
                    }))
        assert_that(thread_states_2_mid_2.arrival_state,
                    has_properties({'type': EventStateType.POSSIBLE_OK, 'thread_uid': 'uid_2'}))
        assert_that(thread_states_2_mid_2.departure_state,
                    has_properties({'type': EventStateType.POSSIBLE_OK, 'thread_uid': 'uid_2'}))
        assert_that(thread_states_2_to.arrival_state,
                    has_properties({'type': EventStateType.POSSIBLE_OK, 'thread_uid': 'uid_2'}))
        assert thread_states_2_to.departure_state is None

        dt_normative_2_a = datetime(2017, 3, 20, 15, 35)
        dt_fact_2_a = datetime(2017, 3, 20, 15, 36)
        dt_normative_2_d = datetime(2017, 3, 20, 15, 40)
        dt_fact_2_d = datetime(2017, 3, 20, 15, 56)

        thread_2_events.update(push_all__stations_events=[
            StationEventFactory(
                station_key=str(station_mid_2.id), type='arrival',
                dt_normative=dt_normative_2_a, dt_fact=dt_fact_2_a,
            ),
            StationEventFactory(
                station_key=str(station_mid_2.id), type='departure',
                dt_normative=dt_normative_2_d, dt_fact=dt_fact_2_d,
            )],
            set__need_recalc=True)

        # Проверяем прогноз после добавления новых событий.
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 5

        thread_states_2_mid_1 = ThreadStationState.objects.get(key__thread_key=thread_key_2,
                                                               key__station_key=str(station_mid_1.id))
        thread_states_2_mid_2 = ThreadStationState.objects.get(key__thread_key=thread_key_2,
                                                               key__station_key=str(station_mid_2.id))
        thread_states_2_to = ThreadStationState.objects.get(key__thread_key=thread_key_2,
                                                            key__station_key=str(station_to.id))

        assert_that(thread_states_2_mid_1.arrival_state,
                    has_properties({
                        'dt': datetime(2017, 3, 20, 15, 22),
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_2',
                        'minutes_from': 2,
                        'minutes_to': 2
                    }))

        assert_that(thread_states_2_mid_2.departure_state,
                    has_properties({
                        'dt': datetime(2017, 3, 20, 15, 56),
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_2',
                        'minutes_from': 16,
                        'minutes_to': 16
                    }))

        assert_that(thread_states_2_to.arrival_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'thread_uid': 'uid_2',
                    }))

        # Проверяем, что события первой нитки не изменились.
        thread_states_1_mid = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                             key__station_key=str(station_mid_1.id))
        thread_states_1_to = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                            key__station_key=str(station_to.id))
        assert_that(thread_states_1_mid.arrival_state,
                    has_properties({
                        'dt': datetime(2017, 3, 20, 13, 19),
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_1',
                        # разница с нормативным временем из базы, а не из события
                        'minutes_from': 9,
                        'minutes_to': 9,
                    }))

        assert_that(thread_states_1_to.arrival_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'thread_uid': 'uid_1',
                    }))

    def test_recalc_forecasts_del_old_possible_delay(self):
        thread_key_1 = 'thread_key_1'
        thread_start_dt = datetime(2017, 3, 20, 13)
        station_1, station_2, station_3, station_4, station_5 = [create_station() for _ in range(5)]

        thread_1 = create_thread(
            uid='uid_1',
            tz_start_time=thread_start_dt.time(),
            schedule_v1=[
                [None, 0, station_1],
                [60, 62, station_2],
                [120, 130, station_3],
                [220, 230, station_4],
                [240, None, station_5],
            ],
        )
        SuburbanKey.objects.create(thread=thread_1, key=thread_key_1)

        thread_1_events = ThreadEventsFactory(**{
            'key': ThreadKey(thread_start_date=thread_start_dt, thread_key=thread_key_1),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_2.id),
                    type='departure',
                    dt_normative=datetime(2017, 3, 20, 14, 10),
                    dt_fact=datetime(2017, 3, 20, 14, 28),
                )
            ],
            'need_recalc': True
        })

        # Есть опоздание по станции station_2, поэтому для нее будет факт,
        # а для 3х следующих станций появятся прогнозы
        recalc_forecasts()

        assert ThreadStationState.objects.count() == 4
        assert ThreadStationState.objects.filter(outdated__ne=True).count() == 4

        thread_states_1_2 = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                           key__station_key=str(station_2.id))
        thread_states_1_3 = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                           key__station_key=str(station_3.id))
        thread_states_1_4 = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                           key__station_key=str(station_4.id))
        thread_states_1_5 = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                           key__station_key=str(station_5.id))

        assert thread_states_1_2.arrival_state is None
        assert_that(thread_states_1_2.departure_state, has_properties({
            'dt': datetime(2017, 3, 20, 14, 28),
            'type': EventStateType.FACT,
            'thread_uid': 'uid_1',
            # разница с нормативным временем из базы, а не из события
            'minutes_from': 26,
            'minutes_to': 26,
        }))

        assert_that(thread_states_1_3.arrival_state, has_properties({
            'thread_uid': 'uid_1',
            'type': EventStateType.POSSIBLE_DELAY,
            'forecast_dt': datetime(2017, 3, 20, 15, 26),
        }))
        assert_that(thread_states_1_3.departure_state, has_properties({
            'thread_uid': 'uid_1',
            'type': EventStateType.POSSIBLE_DELAY,
            'forecast_dt': datetime(2017, 3, 20, 15, 27),
        }))

        assert_that(thread_states_1_4.arrival_state, has_properties({
            'thread_uid': 'uid_1',
            'type': EventStateType.POSSIBLE_DELAY,
            'forecast_dt': datetime(2017, 3, 20, 16, 57),
        }))
        assert_that(thread_states_1_4.departure_state, has_properties({
            'thread_uid': 'uid_1',
            'type': EventStateType.POSSIBLE_DELAY,
            'forecast_dt': datetime(2017, 3, 20, 16, 58),
        }))

        assert_that(thread_states_1_5.arrival_state, has_properties({
            'thread_uid': 'uid_1',
            'type': EventStateType.POSSIBLE_DELAY,
            'forecast_dt': datetime(2017, 3, 20, 17, 8),
        }))
        assert thread_states_1_5.departure_state is None

        thread_1_events.update(
            set__need_recalc=True,
            push_all__stations_events=[
                StationEventFactory(
                    station_key=str(station_4.id),
                    type='arrival',
                    dt_normative=datetime(2017, 3, 20, 16, 40),
                    # Опоздание от нормативного вермени на 4 минуты,
                    # но попадает в SUBURBAN_MIN_DELAY_FOR_FACT_DT (который выставляем ниже),
                    # поэтому считается, что идем по расписанию.
                    dt_fact=datetime(2017, 3, 20, 16, 44),
                )
            ],
        )

        # Прибываем на station_4 вовремя, поэтому прогноз про опоздание на неё заменяется фактом.
        # Старый факт про опоздание на station_2 остается.
        # Опоздание на station_3 заменяется на интерполяцию, т.к. есть факты на соседних станциях.
        # Для отправления со station_4 и для обоих событий station_5 получаем possible_ok.
        with replace_dynamic_setting('SUBURBAN_MIN_DELAY_FOR_FACT_DT', 5):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 4
        assert ThreadStationState.objects.filter(outdated=True).count() == 0

        thread_states_1_2 = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                           key__station_key=str(station_2.id))
        thread_states_1_3 = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                           key__station_key=str(station_3.id))
        thread_states_1_4 = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                           key__station_key=str(station_4.id))
        thread_states_1_5 = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                           key__station_key=str(station_5.id))

        assert_that(thread_states_1_2.departure_state,
                    has_properties({
                        'dt': datetime(2017, 3, 20, 14, 28),
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_1',
                        # разница с нормативным временем из базы, а не из события
                        'minutes_from': 26,
                        'minutes_to': 26,
                    }))

        assert_that(thread_states_1_3.departure_state,
                    has_properties({
                        'dt': None,
                        'type': EventStateType.FACT_INTERPOLATED,
                        'thread_uid': 'uid_1',
                        # разница с нормативным временем из базы, а не из события
                        'minutes_from': 1,
                        'minutes_to': 26,
                    }))
        assert_that(thread_states_1_3.arrival_state,
                    has_properties({
                        'dt': None,
                        'type': EventStateType.FACT_INTERPOLATED,
                        'thread_uid': 'uid_1',
                        # разница с нормативным временем из базы, а не из события
                        'minutes_from': 1,
                        'minutes_to': 26,
                    }))

        assert_that(thread_states_1_4.arrival_state,
                    has_properties({
                        'dt': datetime(2017, 3, 20, 16, 40),
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_1',
                        'minutes_from': 0,
                        'minutes_to': 0
                    }))
        assert_that(thread_states_1_4.departure_state,
                    has_properties({'type': EventStateType.POSSIBLE_OK, 'thread_uid': 'uid_1'}))

        assert_that(thread_states_1_5.arrival_state,
                    has_properties({'type': EventStateType.POSSIBLE_OK, 'thread_uid': 'uid_1'}))
        assert thread_states_1_5.departure_state is None

    @replace_now('2017-03-20 15:31:00')
    def test_no_next_data_forecst(self):
        set_param('last_successful_query_time', datetime(2017, 3, 20, 15, 18))
        thread_key_1 = '7thread_key_1'
        thread_start_dt = datetime(2017, 3, 20, 13)

        dt_normative_1_a = datetime(2017, 3, 20, 14)
        dt_fact_1_a = datetime(2017, 3, 20, 14)
        dt_normative_1_d = datetime(2017, 3, 20, 14, 2)
        dt_fact_1_d = datetime(2017, 3, 20, 14, 7)

        station_from, station_to = create_station(), create_station()
        station_mid_1, station_mid_2 = create_station(), create_station()
        station_mid_2.use_in_forecast = True
        station_mid_2.save()

        thread_1 = create_thread(
            uid='uid_1',
            tz_start_time=thread_start_dt.time(),
            schedule_v1=[
                [None, 0, station_from],
                [60, 62, station_mid_1],
                [120, 133, station_mid_2],
                [240, None, station_to],
            ],
        )

        SuburbanKey.objects.create(thread=thread_1, key=thread_key_1)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_start_dt,
                thread_key=thread_key_1
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_mid_1.id),
                    type='arrival',
                    dt_normative=dt_normative_1_a,
                    dt_fact=dt_fact_1_a,
                    time=60,
                ),
                StationEventFactory(
                    station_key=str(station_mid_1.id),
                    type='departure',
                    dt_normative=dt_normative_1_d,
                    dt_fact=dt_fact_1_d,
                    time=62,
                )
            ],
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=thread_start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_1.id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=60),
                    time=60,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_1.id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=62),
                    time=62,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_2.id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=120),
                    time=120,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_2.id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=130),
                    time=133,
                ),
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=240),
                    time=240,
                ),
            ],
            'last_station_event': StationEventFactory(
                station_key=str(station_mid_1.id),
                type='departure',
                dt_normative=dt_normative_1_d,
                dt_fact=dt_fact_1_d,
                time=62,
            ),
            'need_recalc': True
        })

        StationUpdateInfoFactory(**{
            'station_key': str(station_mid_2.id),
            'timestamp': datetime(2017, 3, 20, 12)
        })

        with replace_dynamic_setting('SUBURBAN_ENABLE_NO_NEXT_DATA', False):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 3
        thread_states_mid_2 = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                             key__station_key=str(station_mid_2.id))
        thread_states_to = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                          key__station_key=str(station_to.id))

        assert_that(thread_states_mid_2.arrival_state,
                    has_properties({'type': EventStateType.POSSIBLE_OK, 'thread_uid': 'uid_1'}))
        assert_that(thread_states_mid_2.departure_state,
                    has_properties({'type': EventStateType.POSSIBLE_OK, 'thread_uid': 'uid_1'}))
        assert_that(thread_states_to.arrival_state,
                    has_properties({'type': EventStateType.POSSIBLE_OK, 'thread_uid': 'uid_1'}))
        assert thread_states_to.departure_state is None

        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        thread_states_mid_1 = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                             key__station_key=str(station_mid_1.id))
        thread_states_mid_2 = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                             key__station_key=str(station_mid_2.id))
        thread_states_to = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                          key__station_key=str(station_to.id))

        assert_that(thread_states_mid_1.arrival_state,
                    has_properties({
                        'dt': datetime(2017, 3, 20, 14),
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_1',
                        'minutes_from': 0,
                        'minutes_to': 0
                    }))

        assert_that(thread_states_mid_1.departure_state,
                    has_properties({
                        'dt': datetime(2017, 3, 20, 14, 7),
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_1',
                        'minutes_from': 5,
                        'minutes_to': 5
                    }))

        assert_that(thread_states_mid_2.departure_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'thread_uid': 'uid_1',
                    }))

        assert_that(thread_states_mid_2.arrival_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'thread_uid': 'uid_1',
                    }))

        assert_that(thread_states_to.arrival_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'thread_uid': 'uid_1',
                    }))

        set_param('last_successful_query_time', datetime(2017, 3, 20, 15, 12))
        recalc_forecasts()
        # Опоздание составляет 12 минут.
        # Опоздание не распространится на последнюю станцию.
        # Время второй остановки - 13 минут.
        # Всего поезд может нагнать 12 минут. Итоговое время опоздания для последней станции 0 минут.
        assert ThreadStationState.objects.count() == 3
        assert ThreadStationState.objects.filter(outdated=True).count() == 0

        ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                       key__station_key=str(station_mid_1.id))
        ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                       key__station_key=str(station_mid_2.id))

    @replace_now('2018-03-20 16:00:00')
    def test_no_next_data_only_arrival(self):
        start_dt = datetime(2018, 3, 20, 13)
        stations = [create_station(use_in_forecast=True) for _ in range(3)]

        thread = create_thread(
            uid='uid_1',
            number='666',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [30, 40, stations[1]],
                [120, None, stations[2]],
            ]
        )

        last_event = StationEventFactory(
            station_key=str(stations[1].id),
            type='arrival',
            dt_normative=start_dt + timedelta(minutes=30),
            dt_fact=start_dt + timedelta(minutes=30),
            time=30
        )

        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_events': [last_event],
            'last_station_event': last_event,
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=30),
                    time=30,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=40),
                    time=40,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=120),
                    time=120,
                ),
            ],
            'need_recalc': False
        })

        StationUpdateInfoFactory(**{
            'station_key': str(stations[1].id),
            'timestamp': datetime(2018, 3, 20, 12)
        })
        set_param('last_successful_query_time', datetime(2018, 3, 20, 13, 50))

        recalc_forecasts()
        assert ThreadStationState.objects.count() == 2

        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=th_event.key.thread_key, key__station_key=str(stations[1].id)
        )
        thread_states_2 = ThreadStationState.objects.get(
            key__thread_key=th_event.key.thread_key, key__station_key=str(stations[2].id)
        )

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.FACT
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY
            })
        }))

        assert_that(thread_states_2, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY
            })
        }))

    @replace_now('2017-03-20 15:31:00')
    def test_no_data(self):
        set_param('last_successful_query_time', datetime(2017, 3, 20, 15, 30))
        thread_key_1 = '7thread_key_1'
        thread_start_dt = datetime(2017, 3, 20, 13)

        station_from, station_mid_1, station_to = create_station(), create_station(), create_station()
        station_mid_1.use_in_departure_forecast = True
        station_mid_1.save()

        thread_1 = create_thread(
            uid='uid_1',
            tz_start_time=thread_start_dt.time(),
            schedule_v1=[
                [None, 0, station_from],
                [60, 62, station_mid_1],
                [200, None, station_to],
            ],
        )

        SuburbanKey.objects.create(thread=thread_1, key=thread_key_1)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_start_dt,
                thread_key=thread_key_1
            ),
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=thread_start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_1.id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=60),
                    time=60,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_1.id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=62),
                    time=62,
                ),
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=200),
                    time=200,
                ),
            ],
            'need_recalc': True
        })

        StationUpdateInfoFactory(**{
            'station_key': str(station_mid_1.id),
            'timestamp': datetime(2017, 3, 20, 12)
        })

        with replace_dynamic_setting('SUBURBAN_ENABLE_NO_DATA', False):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 0

        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        thread_states_from = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                            key__station_key=str(station_from.id))
        thread_states_mid_1 = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                             key__station_key=str(station_mid_1.id))
        thread_states_to = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                          key__station_key=str(station_to.id))

        assert_that(thread_states_from.departure_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'thread_uid': 'uid_1',
                    }))

        assert_that(thread_states_mid_1.arrival_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'thread_uid': 'uid_1',
                    }))

        assert_that(thread_states_mid_1.departure_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'thread_uid': 'uid_1',
                    }))

        assert_that(thread_states_to.arrival_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'thread_uid': 'uid_1',
                    }))

    def test_two_same_events(self):
        """
        Баг при получении одинаковых событий.
        https://st.yandex-team.ru/RASPEXPORT-295
        """

        start_date = datetime(2017, 9, 10, 13)
        station_from, station_to, station_mid = create_station(), create_station(), create_station()
        thread = create_thread(
            uid='uid_1',
            tz_start_time=start_date.time(),
            schedule_v1=[
                [None, 0, station_from],
                [10, 15, station_mid],
                [50, None, station_to],
            ],
        )
        thread_key = get_thread_suburban_key(thread.number, station_from)
        SuburbanKey.objects.create(thread=thread, key=thread_key)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=start_date,
                thread_key=thread_key
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_mid.id),
                    type='arrival',
                    dt_normative=start_date,
                    dt_fact=start_date + timedelta(minutes=20),
                ),
                StationEventFactory(
                    station_key=str(station_mid.id),
                    type='arrival',
                    dt_normative=start_date,
                    dt_fact=start_date + timedelta(minutes=21),
                ),

            ],
            'need_recalc': True
        })

        recalc_forecasts()

        # Проверяем, что опоздание распространилось.
        assert ThreadStationState.objects.count() == 2
        thread_states_mid = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(station_mid.id))
        thread_states_to = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(station_to.id))

        assert_that(thread_states_mid.arrival_state,
                    has_properties({
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_1',
                    }))
        assert_that(thread_states_mid.departure_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'thread_uid': 'uid_1',
                    }))
        assert_that(thread_states_to.arrival_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'thread_uid': 'uid_1',
                    }))

    def test_no_stop_station_forecast(self):
        """
        Для безостановочной станции в качестве нормативного времени используем время РЖД.
        https://st.yandex-team.ru/RASPEXPORT-291
        """
        thread_key_1 = 'thread_key_1'
        start_date = datetime(2017, 9, 10, 13)
        station_from, station_to, station_mid = create_station(), create_station(), create_station()
        thread_1 = create_thread(
            uid='uid_1',
            tz_start_time=start_date.time(),
            schedule_v1=[
                [None, 0, station_from],
                [1, 1, station_mid],
                [50, None, station_to],
            ],
        )
        SuburbanKey.objects.create(thread=thread_1, key=thread_key_1)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=start_date,
                thread_key=thread_key_1
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_mid.id),
                    type='departure',
                    dt_normative=start_date + timedelta(minutes=10),
                    dt_fact=start_date + timedelta(minutes=10),
                ),
            ],

            'need_recalc': True
        })

        recalc_forecasts()

        # При использовании времени РЖД прогноз не должен построиться.
        assert ThreadStationState.objects.count() == 2
        thread_states_mid = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                           key__station_key=str(station_mid.id))
        thread_station_to = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                           key__station_key=str(station_to.id))
        assert_that(thread_states_mid.departure_state,
                    has_properties({
                        'type': EventStateType.FACT,
                        'thread_uid': 'uid_1',
                    }))
        assert_that(thread_station_to.arrival_state,
                    has_properties({'type': EventStateType.POSSIBLE_OK, 'thread_uid': 'uid_1'}))
        assert thread_station_to.departure_state is None

    @replace_now(datetime(2017, 3, 20, 13))
    def test_turnover_forecasts(self):
        thread_start_date = datetime(2017, 3, 20)
        tz_start_time_1 = time(13, 5)
        tz_start_time_2 = time(14, 10)

        station_from, station_mid, station_to = create_station(), create_station(), create_station()
        schedule_plan = create_train_schedule_plan()

        thread_1 = create_thread(
            uid='uid_1',
            number='number_1',
            tz_start_time=tz_start_time_1,
            schedule_plan=schedule_plan,
            schedule_v1=[
                [None, 0, station_from],
                [10, 15, station_mid],
                [50, None, station_to],
            ],
        )
        thread_2 = create_thread(
            uid='uid_2',
            number='number_2',
            tz_start_time=tz_start_time_2,
            schedule_v1=[
                [None, 0, station_to],
                [30, None, station_mid],
            ],
        )

        thread_key_1 = '{}__{}'.format(thread_1.number, station_from.id)
        thread_key_2 = '{}__{}'.format(thread_2.number, station_to.id)

        SuburbanKey.objects.create(thread=thread_1, key=thread_key_1)
        SuburbanKey.objects.create(thread=thread_2, key=thread_key_2)

        thread_1_events = ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=datetime.combine(thread_start_date, tz_start_time_1),
                thread_key=thread_key_1
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_mid.id),
                    type='arrival',
                    dt_normative=datetime(2017, 3, 20, 13, 15),
                    dt_fact=datetime(2017, 3, 20, 13, 19),
                ),
                StationEventFactory(
                    station_key=str(station_mid.id),
                    type='departure',
                    dt_normative=datetime(2017, 3, 20, 13, 20),
                    dt_fact=datetime(2017, 3, 20, 13, 35),
                )
            ],
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=datetime.combine(thread_start_date.date(), tz_start_time_1),
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid.id),
                    type='arrival',
                    dt_normative=datetime.combine(thread_start_date.date(), tz_start_time_1) + timedelta(
                        minutes=10),
                    time=10,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid.id),
                    type='departure',
                    dt_normative=datetime.combine(thread_start_date.date(), tz_start_time_1) + timedelta(
                        minutes=15),
                    time=15,
                ),
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='arrival',
                    dt_normative=datetime.combine(thread_start_date.date(), tz_start_time_1) + timedelta(
                        minutes=50),
                    time=50,
                ),
            ],
            'need_recalc': True
        })

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=datetime.combine(thread_start_date, tz_start_time_2),
                thread_key=thread_key_2
            ),
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='departure',
                    dt_normative=datetime.combine(thread_start_date.date(), tz_start_time_2),
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid.id),
                    type='arrival',
                    dt_normative=datetime.combine(thread_start_date.date(), tz_start_time_2) + timedelta(minutes=30),
                    time=30,
                ),
            ],
            'need_recalc': False
        })

        TrainTurnover.objects.create(
            number_before='number_1',
            number_after='number_2',
            year_days=str(RunMask(days=[thread_start_date])),
            graph=schedule_plan,
            station=station_to
        )

        # Оборот выключен.
        with replace_dynamic_setting('SUBURBAN_TRAIN_TURNOVER_ENABLED', False):
            recalc_forecasts()

        assert ThreadStationState.objects.count() == 2
        thread_states_1_mid = ThreadStationState.objects.get(key__thread_key=thread_key_1,  # noqa
                                                             key__station_key=str(station_mid.id))
        thread_states_1_to = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                            key__station_key=str(station_to.id))
        # Последнее событие 1 нитки.
        assert_that(thread_states_1_to.arrival_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'forecast_dt': datetime(2017, 3, 20, 14, 10),
                        'thread_uid': 'uid_1',
                    }))

        # Оборот включен.
        # Должны получить два стейта для второй нитки.
        thread_1_events.update(set__need_recalc=True)
        with replace_dynamic_setting('SUBURBAN_TRAIN_TURNOVER_ENABLED', True):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 4
        thread_states_2_from = ThreadStationState.objects.get(key__thread_key=thread_key_2,
                                                              key__station_key=str(station_to.id))
        thread_states_2_to = ThreadStationState.objects.get(key__thread_key=thread_key_2,
                                                            key__station_key=str(station_mid.id))
        assert_that(thread_states_2_from.departure_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'sub_type': EventStateSubType.TRAIN_TURNOVER,
                        'thread_uid': 'uid_2',
                        'trigger': thread_key_1
                    }))
        assert_that(thread_states_2_to.arrival_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'sub_type': EventStateSubType.TRAIN_TURNOVER,
                        'thread_uid': 'uid_2',
                        'trigger': thread_key_1
                    }))

    @replace_now(datetime(2017, 3, 20, 23))
    def test_turnover_next_day_forecasts(self):
        thread_start_date_1 = datetime(2017, 3, 20, 23)
        thread_start_date_2 = datetime(2017, 3, 21, 0, 5)

        station_from, station_mid, station_to = create_station(), create_station(), create_station()
        schedule_plan = create_train_schedule_plan()

        thread_1 = create_thread(
            number='number_1',
            tz_start_time=thread_start_date_1.time(),
            schedule_plan=schedule_plan,
            schedule_v1=[
                [None, 0, station_from],
                [10, 15, station_mid],
                [58, None, station_to],
            ],
        )
        thread_2 = create_thread(
            uid='uid_2',
            number='number_2',
            tz_start_time=thread_start_date_2.time(),
            schedule_v1=[
                [None, 0, station_to],
                [30, None, station_mid],
            ],
        )

        thread_key_1 = '{}__{}'.format(thread_1.number, station_from.id)
        thread_key_2 = '{}__{}'.format(thread_2.number, station_to.id)

        SuburbanKey.objects.create(thread=thread_1, key=thread_key_1)
        SuburbanKey.objects.create(thread=thread_2, key=thread_key_2)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_start_date_1,
                thread_key=thread_key_1
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_mid.id),
                    type='departure',
                    dt_normative=datetime(2017, 3, 20, 23, 20),
                    dt_fact=datetime(2017, 3, 20, 23, 35),
                )
            ],
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='arrival',
                    dt_normative=thread_start_date_1 + timedelta(minutes=58),
                    time=58,
                ),
            ],
            'need_recalc': True
        })

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_start_date_2,
                thread_key=thread_key_2
            ),
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='departure',
                    dt_normative=thread_start_date_2,
                    time=0,
                ),
            ],
            'need_recalc': False
        })

        TrainTurnover.objects.create(
            number_before='number_1',
            number_after='number_2',
            year_days=str(RunMask(days=[thread_start_date_1])),
            graph=schedule_plan,
            station=station_to
        )

        # Должны получить два стейта для второй нитки.
        with replace_dynamic_setting('SUBURBAN_TRAIN_TURNOVER_ENABLED', True):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 4
        thread_states_2_from = ThreadStationState.objects.get(key__thread_key=thread_key_2,
                                                              key__station_key=str(station_to.id))
        thread_states_2_to = ThreadStationState.objects.get(key__thread_key=thread_key_2,
                                                            key__station_key=str(station_mid.id))
        assert_that(thread_states_2_from.departure_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'sub_type': EventStateSubType.TRAIN_TURNOVER,
                        'thread_uid': 'uid_2',
                        'trigger': thread_key_1
                    }))
        assert_that(thread_states_2_to.arrival_state,
                    has_properties({
                        'type': EventStateType.POSSIBLE_DELAY,
                        'sub_type': EventStateSubType.TRAIN_TURNOVER,
                        'thread_uid': 'uid_2',
                        'trigger': thread_key_1
                    }))

    @replace_now('2017-03-20 17:00:00')
    def test_no_data_crash(self):
        last_query_time = datetime(2017, 3, 20, 15)
        set_param('last_successful_query_time', last_query_time)
        thread_key_1 = '7thread_key_1'
        thread_start_dt = datetime(2017, 3, 20, 14)

        station_from, station_mid, station_to = create_station(), create_station(), create_station()
        station_from.use_in_departure_forecast = True
        station_from.save()
        company = create_company(id=42)

        thread_1 = create_thread(
            uid='uid_1',
            tz_start_time=thread_start_dt.time(),
            schedule_v1=[
                [None, 0, station_from],
                [180, 185, station_mid],
                [210, None, station_to],
            ],
            company=company
        )

        SuburbanKey.objects.create(thread=thread_1, key=thread_key_1)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_start_dt,
                thread_key=thread_key_1
            ),
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=thread_start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid.id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=180),
                    time=180,
                ),
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=210),
                    time=210,
                ),
            ],
            'thread_company': company.id,
            'need_recalc': False
        })

        StationUpdateInfoFactory(**{
            'station_key': str(station_from.id),
            'timestamp': datetime(2017, 3, 20, 12)
        })

        StationUpdateInfoFactory(**{
            'station_key': str(station_mid.id),
            'timestamp': datetime(2017, 3, 20, 12)
        })

        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        crash = CompanyCrashFactory(
            company=company.id,
            first_dt=last_query_time,
        )

        ThreadStationState.drop_collection()
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        crash.first_dt = thread_start_dt + timedelta(minutes=1)
        crash.save()
        ThreadStationState.drop_collection()
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        crash.first_dt = thread_start_dt
        crash.save()
        ThreadStationState.drop_collection()
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 0

        set_param('last_successful_query_time', datetime(2017, 3, 20, 17, 15))

        crash.last_dt = datetime(2017, 3, 20, 16, 59)
        crash.save()
        ThreadStationState.drop_collection()
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        crash.last_dt = datetime(2017, 3, 20, 17)
        crash.save()
        ThreadStationState.drop_collection()
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 0

    @replace_now('2017-03-20 15:31:00')
    def test_no_next_data_crash(self):
        last_query_time = datetime(2017, 3, 20, 15, 30)
        set_param('last_successful_query_time', last_query_time)
        thread_key_1 = '7thread_key_1'
        thread_start_dt = datetime(2017, 3, 20, 13)

        station_from, station_to = create_station(), create_station()
        station_mid_1, station_mid_2 = create_station(use_in_forecast=True), create_station(use_in_forecast=True)
        company = create_company(id=42)

        thread_1 = create_thread(
            uid='uid_1',
            tz_start_time=thread_start_dt.time(),
            schedule_v1=[
                [None, 0, station_from],
                [60, 62, station_mid_1],
                [80, 90, station_mid_2],
                [240, None, station_to],
            ],
            company=company
        )

        SuburbanKey.objects.create(thread=thread_1, key=thread_key_1)

        thread_events = ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_start_dt,
                thread_key=thread_key_1
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=thread_start_dt,
                    dt_fact=thread_start_dt,
                    time=0,
                )
            ],
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=thread_start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_1.id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=60),
                    time=60,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_1.id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=62),
                    time=62,
                ),
                StationExpectedEvent(
                    station_key=str(station_mid_2.id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=90),
                    time=90,
                ),
                StationExpectedEvent(
                    station_key=str(station_to.id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=240),
                    time=240,
                ),
            ],
            'last_station_event': StationEventFactory(
                station_key=str(station_from.id),
                type='departure',
                dt_normative=thread_start_dt,
                dt_fact=thread_start_dt,
                time=0,
            ),
            'need_recalc': False
        })

        StationUpdateInfoFactory(**{
            'station_key': str(station_mid_1.id),
            'timestamp': datetime(2017, 3, 20, 12)
        })
        StationUpdateInfoFactory(**{
            'station_key': str(station_mid_2.id),
            'timestamp': datetime(2017, 3, 20, 12)
        })

        recalc_forecasts()
        assert ThreadStationState.objects.count() == 4

        crash = CompanyCrashFactory(
            company=company.id,
            first_dt=thread_start_dt - timedelta(minutes=20),
        )

        ThreadStationState.drop_collection()
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 0

        crash.last_dt = thread_start_dt
        crash.save()
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 0

        crash.last_dt = thread_start_dt - timedelta(minutes=1)
        crash.save()
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 4

        ThreadStationState.drop_collection()
        crash.last_dt = thread_start_dt + timedelta(minutes=61)
        crash.save()
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 0

        station_mid_event = StationEventFactory(
            station_key=str(station_mid_1.id),
            type='departure',
            dt_normative=thread_start_dt + timedelta(minutes=62),
            dt_fact=thread_start_dt + timedelta(minutes=62),
            time=62
        )
        thread_events.update(
            push_all__stations_events=[station_mid_event],
            set__last_station_event=station_mid_event
        )
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 4

    @replace_now('2018-03-20 17:00:00')
    def test_recalc_filter(self):
        start_dt = datetime(2018, 3, 20, 13)
        stations = [create_station() for _ in range(4)]

        thread = create_thread(
            uid='uid_1',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [60, 65, stations[1]],
                [90, 95, stations[2]],
                [120, None, stations[3]],
            ]
        )
        thread_key = get_thread_suburban_key(thread.number, stations[0])
        SuburbanKey.objects.create(thread=thread, key=thread_key)

        pack_1_dt = datetime(2018, 2, 20, 13, 20)
        pack_2_dt = datetime(2018, 2, 20, 14, 40)
        pack_3_dt = datetime(2018, 2, 20, 15, 5)
        pack_4_dt = datetime(2018, 2, 20, 15, 10)

        th_events = ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=start_dt,
                thread_key=thread_key
            ),
            'stations_events': [
                # 1 пакет
                StationEventFactory(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=start_dt,
                    dt_fact=start_dt + timedelta(minutes=5),
                    dt_save=pack_1_dt
                ),
                StationEventFactory(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=start_dt,
                    dt_fact=start_dt + timedelta(minutes=7),
                    dt_save=pack_1_dt
                ),
                # невалидное событие
                StationEventFactory(
                    station_key=str(stations[1].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=60),
                    dt_fact=start_dt + timedelta(minutes=7),
                    dt_save=pack_1_dt,
                    time=60
                ),
                # 2 пакет
                StationEventFactory(
                    station_key=str(stations[1].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=60),
                    dt_fact=start_dt + timedelta(minutes=60),
                    dt_save=pack_2_dt,
                    time=60
                ),
                StationEventFactory(
                    station_key=str(stations[2].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=90),
                    dt_fact=start_dt + timedelta(minutes=92),
                    dt_save=pack_2_dt,
                    time=90
                ),
                StationEventFactory(
                    station_key=str(stations[2].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=95),
                    dt_fact=start_dt + timedelta(minutes=98),
                    dt_save=pack_2_dt,
                    time=95
                ),
                # 3 пакет
                # # невалидное событие
                StationEventFactory(
                    station_key=str(stations[1].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=65),
                    dt_fact=start_dt + timedelta(minutes=7),
                    dt_save=pack_3_dt,
                    time=65
                ),
                StationEventFactory(
                    station_key=str(stations[3].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=120),
                    dt_fact=start_dt + timedelta(minutes=125),
                    dt_save=pack_3_dt,
                    time=120
                ),
                StationEventFactory(
                    station_key=str(stations[1].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=65),
                    dt_fact=start_dt + timedelta(minutes=68),
                    dt_save=pack_3_dt,
                    time=65
                ),
                # 4 пакет
                # невалидное событие
                StationEventFactory(
                    station_key=str(stations[2].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=95),
                    dt_fact=start_dt + timedelta(minutes=130),
                    dt_save=pack_4_dt
                ),
                StationEventFactory(
                    station_key=str(stations[3].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=120),
                    dt_fact=start_dt + timedelta(minutes=124),
                    dt_save=pack_4_dt
                ),

            ],
            'need_recalc': True
        })

        recalc_forecasts()

        assert ThreadStationState.objects.count() == 4
        thread_states_0 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[0].id)
        )
        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[1].id)
        )
        thread_states_2 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[2].id)
        )
        thread_states_3 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[3].id)
        )

        assert_that(thread_states_0, has_properties({
            'departure_state': has_properties({
                'dt': th_events.stations_events[1].dt_fact,
                'type': EventStateType.FACT,
                'thread_uid': 'uid_1',
            })
        }))

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'dt': th_events.stations_events[3].dt_fact,
                'type': EventStateType.FACT,
                'thread_uid': 'uid_1',
            }),
            'departure_state': has_properties({
                'dt': th_events.stations_events[8].dt_fact,
                'type': EventStateType.FACT,
                'thread_uid': 'uid_1',
            })
        }))

        assert_that(thread_states_2, has_properties({
            'arrival_state': has_properties({
                'dt': th_events.stations_events[4].dt_fact,
                'type': EventStateType.FACT,
                'thread_uid': 'uid_1',
            }),
            'departure_state': has_properties({
                'dt': th_events.stations_events[5].dt_fact,
                'type': EventStateType.FACT,
                'thread_uid': 'uid_1',
            })
        }))

        assert_that(thread_states_3, has_properties({
            'arrival_state': has_properties({
                'dt': th_events.stations_events[-1].dt_fact,
                'type': EventStateType.FACT,
                'thread_uid': 'uid_1',
            })
        }))

    @replace_now('2018-03-20 17:00:00')
    def test_recalc_filter_one_bad_event(self):
        start_dt = datetime(2018, 3, 20, 13)
        stations = [create_station() for _ in range(3)]

        thread = create_thread(
            uid='uid_1',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [60, 65, stations[1]],
                [120, None, stations[2]],
            ]
        )
        thread_key = get_thread_suburban_key(thread.number, stations[0])
        SuburbanKey.objects.create(thread=thread, key=thread_key)

        th_event = ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=start_dt,
                thread_key=thread_key
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=start_dt,
                    dt_fact=start_dt + timedelta(minutes=5),
                )
            ],
            'need_recalc': True
        })

        with replace_dynamic_setting('SUBURBAN_MAX_EVENT_DELAY', 4):
            recalc_forecasts()
            assert ThreadStationState.objects.count() == 0

        with replace_dynamic_setting('SUBURBAN_MAX_EVENT_DELAY', 10):
            th_event.update(set__need_recalc=True)
            recalc_forecasts()
            assert ThreadStationState.objects.count() == 3

    @replace_now('2018-03-20 16:00:00')
    def test_numeric_forecast(self):
        start_dt = datetime(2018, 3, 20, 13)
        stations = [create_station() for _ in range(3)]

        thread = create_thread(
            uid='uid_1',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [60, 65, stations[1]],
                [120, None, stations[2]],
            ]
        )

        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_events': [
                StationEventFactory(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=start_dt,
                    dt_fact=start_dt + timedelta(minutes=10),
                )
            ],
            'need_recalc': True
        })

        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        thread_key = th_event.key.thread_key
        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[1].id)
        )
        thread_states_2 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[2].id)
        )

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'minutes_from': None,
                'minutes_to': None
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'minutes_from': None,
                'minutes_to': None
            })
        }))

        assert_that(thread_states_2, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'minutes_from': None,
                'minutes_to': None
            })
        }))

        ForecastRepresentation.objects.create(
            type=EventStateSubType.FACT,
            delay_from=1, delay_to=10,
            deep_from=10, deep_to=60,
            minutes_from=1, minutes_to=5
        )

        ForecastRepresentation.objects.create(
            type=EventStateSubType.FACT,
            delay_from=1, delay_to=5,
            deep_from=50, deep_to=100,
            minutes_from=3, minutes_to=4
        )

        ForecastRepresentation.objects.create(
            type=EventStateSubType.FACT,
            delay_from=5, delay_to=6,
            deep_from=50, deep_to=100,
            minutes_from=5, minutes_to=7
        )

        ForecastRepresentation.objects.create(
            type=EventStateSubType.FACT,
            delay_from=1, delay_to=6,
            deep_from=110, deep_to=150,
            minutes_from=10
        )

        th_event.update(set__need_recalc=True)
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[1].id)
        )
        thread_states_2 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[2].id)
        )

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'minutes_from': 1,
                'minutes_to': 5
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'minutes_from': 5,
                'minutes_to': 7
            })
        }))

        assert_that(thread_states_2, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'minutes_from': 10,
                'minutes_to': None
            })
        }))

    @replace_now('2018-03-20 16:00:00')
    def test_no_next_data_numeric_forecast(self):
        start_dt = datetime(2018, 3, 20, 13)
        stations = [create_station(use_in_forecast=True) for _ in range(3)]

        thread = create_thread(
            uid='uid_1',
            number='666',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [30, 40, stations[1]],
                [119, None, stations[2]],
            ]
        )

        last_event = StationEventFactory(
            station_key=str(stations[0].id),
            type='departure',
            dt_normative=start_dt,
            dt_fact=start_dt + timedelta(minutes=2),
            time=0
        )

        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_events': [last_event],
            'last_station_event': last_event,
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=30),
                    time=30,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=40),
                    time=40,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=119),
                    time=119,
                )
            ],
            'need_recalc': False
        })

        ForecastRepresentation.objects.create(
            type=EventStateSubType.NO_NEXT_DATA,
            delay_from=10, delay_to=15,
            deep_from=20, deep_to=30,
            minutes_from=5, minutes_to=7
        )

        ForecastRepresentation.objects.create(
            type=EventStateSubType.NO_NEXT_DATA,
            delay_from=1, delay_to=2,
            deep_from=110, deep_to=110,
            minutes_from=10, minutes_to=15
        )

        StationUpdateInfoFactory(**{
            'station_key': str(stations[1].id),
            'timestamp': datetime(2018, 3, 20, 12)
        })
        set_param('last_successful_query_time', datetime(2018, 3, 20, 13, 45))

        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=th_event.key.thread_key, key__station_key=str(stations[1].id)
        )
        thread_states_2 = ThreadStationState.objects.get(
            key__thread_key=th_event.key.thread_key, key__station_key=str(stations[2].id)
        )

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'minutes_from': 5,
                'minutes_to': 7
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'minutes_from': None,
                'minutes_to': None
            })
        }))

        assert_that(thread_states_2, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'minutes_from': None,
                'minutes_to': None
            })
        }))

        with replace_dynamic_setting('SUBURBAN_REDUCTION_TIME_BY_HOUR', 2):
            # опоздание на последней станции 6 минут
            # время от факта до последней станции с сокращением остановки - 110 минут
            # 110 / 60 * 2 = 3.66 - нагон
            # время опоздания должно быть округлено до 2 минут
            recalc_forecasts()

        thread_states_2 = ThreadStationState.objects.get(
            key__thread_key=th_event.key.thread_key, key__station_key=str(stations[2].id)
        )

        assert_that(thread_states_2, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'minutes_from': 10,
                'minutes_to': 15
            })
        }))

    @replace_now('2018-03-20 16:00:00')
    def test_no_next_data_fact_compare(self):
        start_dt = datetime(2018, 3, 20, 13)
        stations = [create_station(use_in_forecast=True) for _ in range(3)]

        thread = create_thread(
            uid='uid_1',
            number='666',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [30, 40, stations[1]],
                [120, None, stations[2]],
            ]
        )

        last_event = StationEventFactory(
            station_key=str(stations[0].id),
            type='departure',
            dt_normative=start_dt,
            dt_fact=start_dt + timedelta(minutes=20),
            time=0
        )

        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_events': [last_event],
            'last_station_event': last_event,
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=30),
                    time=30,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=40),
                    time=40,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=120),
                    time=120,
                )
            ],
            'need_recalc': True
        })

        StationUpdateInfoFactory(**{
            'station_key': str(stations[1].id),
            'timestamp': datetime(2018, 3, 20, 12)
        })
        set_param('last_successful_query_time', datetime(2018, 3, 20, 13, 20))

        # должно быть использовано опоздание, которое было построено при обработке нового факта
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        thread_key = th_event.key.thread_key
        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[1].id)
        )

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'forecast_delay': 20,
                'sub_type': EventStateSubType.FACT,
                'forecast_dt': datetime(2018, 3, 20, 13, 50)
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'forecast_delay': 11,
                'sub_type': EventStateSubType.FACT,
                'forecast_dt': datetime(2018, 3, 20, 13, 51)
            })
        }))

        # опоздание "застрял в пути" не будет построено (недостаточное время опоздания)
        ThreadStationState.drop_collection()
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 0

        # должно быть использовано опоздание, которое будет построено по факту
        set_param('last_successful_query_time', datetime(2018, 3, 20, 13, 50))
        with replace_dynamic_setting('SUBURBAN_MIN_DELAY_FOR_NO_NEXT_DATA_EVENT', 0):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[1].id)
        )

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'forecast_delay': 20,
                'sub_type': EventStateSubType.FACT,
                'forecast_dt': datetime(2018, 3, 20, 13, 50)
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'forecast_delay': 11,
                'sub_type': EventStateSubType.FACT,
                'forecast_dt': datetime(2018, 3, 20, 13, 51)
            })
        }))

        # должно быть использовано опоздание "застрял в пути"
        set_param('last_successful_query_time', datetime(2018, 3, 20, 14, 10))
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[1].id)
        )

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'forecast_delay': 40,
                'sub_type': EventStateSubType.NO_NEXT_DATA,
                'forecast_dt': datetime(2018, 3, 20, 14, 10)
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'forecast_delay': 31,
                'sub_type': EventStateSubType.NO_NEXT_DATA,
                'forecast_dt': datetime(2018, 3, 20, 14, 11)
            })
        }))

    @replace_now('2018-03-20 15:00:00')
    def test_no_next_data_fact_compare_with_arrival(self):
        start_dt = datetime(2018, 3, 20, 13)
        stations = [create_station(use_in_forecast=True) for _ in range(4)]

        thread = create_thread(
            uid='uid_1',
            number='666',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [30, 40, stations[1]],
                [90, 100, stations[2]],
                [120, None, stations[3]],
            ]
        )

        last_event = StationEventFactory(
            station_key=str(stations[1].id),
            type='arrival',
            dt_normative=start_dt,
            dt_fact=start_dt + timedelta(minutes=40),
            time=30
        )

        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_events': [last_event],
            'last_station_event': last_event,
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=90),
                    time=90
                ),
                StationExpectedEvent(
                    station_key=str(stations[3].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=120),
                    time=120
                )
            ],
            'need_recalc': False
        })

        StationUpdateInfoFactory(**{
            'station_key': str(stations[2].id),
            'timestamp': datetime(2018, 3, 20, 12)
        })

        # должно быть использовано опоздание, которое будет построено по факту
        set_param('last_successful_query_time', datetime(2018, 3, 20, 14, 50))
        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        thread_key = th_event.key.thread_key
        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[1].id)
        )
        thread_states_2 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[2].id)
        )
        thread_states_3 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[3].id)
        )

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.FACT,
                'thread_uid': 'uid_1',
                'dt': datetime(2018, 3, 20, 13, 40)
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'forecast_delay': 1,
                'sub_type': EventStateSubType.FACT,
                'forecast_dt': datetime(2018, 3, 20, 13, 41)
            })
        }))

        assert_that(thread_states_2, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'forecast_delay': 11,
                'sub_type': EventStateSubType.NO_NEXT_DATA,
                'forecast_dt': datetime(2018, 3, 20, 14, 41)
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'forecast_delay': 2,
                'sub_type': EventStateSubType.NO_NEXT_DATA,
                'forecast_dt': datetime(2018, 3, 20, 14, 42)
            })
        }))

        assert_that(thread_states_3, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': 'uid_1',
                'forecast_delay': 2,
                'sub_type': EventStateSubType.NO_NEXT_DATA,
                'forecast_dt': datetime(2018, 3, 20, 15, 2)
            })
        }))

    @replace_now('2018-03-20 16:00:00')
    def test_calc_rts_event_delay_params(self):
        start_dt = datetime(2018, 3, 20, 13)
        stations = [create_station() for _ in range(3)]

        thread = create_thread(
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [60, 65, stations[1]],
                [125, None, stations[2]],
            ]
        )

        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_events': [
                StationEventFactory(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=start_dt,
                    dt_fact=start_dt + timedelta(minutes=10),
                )
            ],
            'need_recalc': True
        })

        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        thread_key = th_event.key.thread_key
        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[1].id)
        )
        thread_states_2 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[2].id)
        )

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 10,
                'forecast_dt': start_dt + timedelta(minutes=70)
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 6,
                'forecast_dt': start_dt + timedelta(minutes=71)
            })
        }))

        assert_that(thread_states_2, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 6,
                'forecast_dt': start_dt + timedelta(minutes=131)
            })
        }))

        with replace_dynamic_setting('SUBURBAN_REDUCTION_TIME_BY_HOUR', 3):
            th_event.update(set__need_recalc=True)
            recalc_forecasts()

        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[1].id)
        )
        thread_states_2 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[2].id)
        )

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 7,
                'forecast_dt': start_dt + timedelta(minutes=67)
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 2.95,
                'forecast_dt': start_dt + timedelta(minutes=67, seconds=57)
            })
        }))

        assert_that(thread_states_2, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_OK
            })
        }))

    @replace_now('2018-03-20 16:00:00')
    def test_no_next_data_test_calc_rts_event_delay_params(self):
        start_dt = datetime(2018, 3, 20, 13)
        stations = [create_station(use_in_forecast=True) for _ in range(4)]

        thread = create_thread(
            number='666',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [30, 36, stations[1]],
                [60, 65, stations[2]],
                [130, None, stations[3]],
            ]
        )

        last_event = StationEventFactory(
            station_key=str(stations[0].id),
            type='departure',
            dt_normative=start_dt,
            dt_fact=start_dt + timedelta(minutes=2),
            time=0
        )

        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_events': [last_event],
            'last_station_event': last_event,
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=60),
                    time=60,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=119),
                    time=130,
                )
            ],
            'need_recalc': False
        })

        StationUpdateInfoFactory(**{
            'station_key': str(stations[2].id),
            'timestamp': datetime(2018, 3, 20, 12)
        })
        set_param('last_successful_query_time', datetime(2018, 3, 20, 14, 15))

        recalc_forecasts()
        assert ThreadStationState.objects.count() == 4

        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=th_event.key.thread_key, key__station_key=str(stations[1].id)
        )
        thread_states_2 = ThreadStationState.objects.get(
            key__thread_key=th_event.key.thread_key, key__station_key=str(stations[2].id)
        )
        thread_states_3 = ThreadStationState.objects.get(
            key__thread_key=th_event.key.thread_key, key__station_key=str(stations[3].id)
        )

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 15,
                'forecast_dt': start_dt + timedelta(minutes=45)
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 10,
                'forecast_dt': start_dt + timedelta(minutes=46)
            })
        }))
        assert_that(thread_states_2, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 10,
                'forecast_dt': start_dt + timedelta(minutes=70)
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 6,
                'forecast_dt': start_dt + timedelta(minutes=71)
            })
        }))
        assert_that(thread_states_3, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 6,
                'forecast_dt': start_dt + timedelta(minutes=136)
            })
        }))

        with replace_dynamic_setting('SUBURBAN_REDUCTION_TIME_BY_HOUR', 3):
            recalc_forecasts()

        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=th_event.key.thread_key, key__station_key=str(stations[1].id)
        )
        thread_states_2 = ThreadStationState.objects.get(
            key__thread_key=th_event.key.thread_key, key__station_key=str(stations[2].id)
        )
        thread_states_3 = ThreadStationState.objects.get(
            key__thread_key=th_event.key.thread_key, key__station_key=str(stations[3].id)
        )

        assert_that(thread_states_1, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 13.5,
                'forecast_dt': start_dt + timedelta(minutes=43, seconds=30)
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 8.45,
                'forecast_dt': start_dt + timedelta(minutes=44, seconds=27)
            })
        }))
        assert_that(thread_states_2, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 7.25,
                'forecast_dt': start_dt + timedelta(minutes=67, seconds=15)
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'forecast_delay': 3.2,
                'forecast_dt': start_dt + timedelta(minutes=68, seconds=12)
            })
        }))
        assert_that(thread_states_3, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_OK
            })
        }))

    @replace_now('2018-03-20 16:00:00')
    def test_forecast_station_double(self):
        start_dt = datetime(2018, 3, 20, 13)
        stations = [create_station() for _ in range(2)]

        thread = create_thread(
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [60, 62, stations[1]],
                [125, None, stations[0]],
            ]
        )

        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_events': [
                StationEventFactory(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=start_dt,
                    dt_fact=start_dt + timedelta(minutes=10),
                )
            ],
            'need_recalc': True
        })

        recalc_forecasts()
        assert ThreadStationState.objects.count() == 3

        thread_key = th_event.key.thread_key
        thread_states_1 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[0].id), key__arrival=None
        )
        thread_states_2 = ThreadStationState.objects.get(
            key__thread_key=thread_key, key__station_key=str(stations[0].id), key__departure=None
        )

        assert_that(thread_states_1, has_properties({
            'departure_state': has_properties({
                'type': EventStateType.FACT,
                'minutes_from': 10,
                'minutes_to': 10
            })
        }))

        assert_that(thread_states_2, has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY
            })
        }))


class TestForecastWithCancels(object):
    @replace_now('2021-04-19 16:00:00')
    def test_forecast_with_cancels(self):
        start_dt = datetime(2021, 4, 19, 10, 0)
        stations = [create_station() for _ in range(3)]
        thread = create_thread(
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [10, 12, stations[1]],
                [25, None, stations[2]],
            ]
        )

        last_event = StationEventFactory(
            station_key=str(stations[0].id),
            type='departure',
            dt_normative=start_dt,
            dt_fact=start_dt + timedelta(minutes=10),
        )
        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_events': [last_event],
            'last_station_event': last_event,
            'stations_cancels': [
                StationCancelFactory.create_from_rtstations(
                    rtstations=list(thread.path)[1:],
                    dt_save=datetime(2021, 4, 19, 8, 0)
                )
            ],
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=10),
                    time=10,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=12),
                    time=12,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=25),
                    time=25,
                )
            ],
            'need_recalc': True
        })
        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()

        thread_key = th_event.key.thread_key

        assert ThreadStationState.objects.count() == 3
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id), outdated=False)
            for station in stations
        ]
        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({'type': EventStateType.FACT})
        }))
        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({'type': EventStateType.POSSIBLE_DELAY}),
            'departure_state': has_properties({'type': EventStateType.CANCELLED})
        }))
        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({'type': EventStateType.CANCELLED})
        }))

        # после второго пересчёта того же треда без изменений старые стейты не должны быть перезаписаны
        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()

        thread_key = th_event.key.thread_key

        assert ThreadStationState.objects.count() == 3
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id), outdated=False)
            for station in stations
        ]
        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({'type': EventStateType.FACT})
        }))
        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({'type': EventStateType.POSSIBLE_DELAY}),
            'departure_state': has_properties({'type': EventStateType.CANCELLED})
        }))
        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({'type': EventStateType.CANCELLED})
        }))

    @replace_now('2021-04-19 16:00:00')
    def test_forecast_with_cancels_after_arrival(self):
        start_dt = datetime(2021, 4, 19, 10, 0)
        stations = [create_station() for _ in range(3)]
        thread = create_thread(
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [10, 12, stations[1]],
                [25, None, stations[2]],
            ]
        )

        last_event = StationEventFactory(
            station_key=str(stations[2].id),
            type='arrival',
            dt_normative=start_dt + timedelta(minutes=25),
            dt_fact=start_dt + timedelta(minutes=30),
        )
        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_events': [last_event],
            'last_station_event': last_event,
            'stations_cancels': [
                StationCancelFactory.create_from_rtstations(
                    rtstations=list(thread.path)[:2],
                    dt_save=datetime(2021, 4, 19, 8, 0)
                )
            ],
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=10),
                    time=10,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=12),
                    time=12,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='arrival',
                    dt_normative=start_dt+timedelta(minutes=25),
                    time=25,
                )
            ],
            'need_recalc': True
        })
        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()

        thread_key = th_event.key.thread_key

        assert ThreadStationState.objects.count() == 3
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id), outdated=False)
            for station in stations
        ]
        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({'type': EventStateType.CANCELLED})
        }))
        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({'type': EventStateType.CANCELLED}),
            'departure_state': None
        }))
        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({'type': EventStateType.FACT})
        }))

        # после второго пересчёта того же треда без изменений старые стейты не должны быть перезаписаны
        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()

        thread_key = th_event.key.thread_key

        assert ThreadStationState.objects.count() == 3
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id), outdated=False)
            for station in stations
        ]
        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({'type': EventStateType.CANCELLED})
        }))
        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({'type': EventStateType.CANCELLED}),
            'departure_state': None
        }))
        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({'type': EventStateType.FACT})
        }))

    @replace_now('2021-04-19 10:00:00')
    def test_fully_cancelled(self):
        start_dt = datetime(2021, 4, 19, 16, 0)
        stations = [create_station() for _ in range(3)]
        thread = create_thread(
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [10, 12, stations[1]],
                [25, None, stations[2]],
            ]
        )

        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_cancels': [
                StationCancelFactory.create_from_rtstations(
                    rtstations=list(thread.path),
                    dt_save=datetime(2021, 4, 19, 8, 0)
                )
            ],
            'need_recalc': True
        })
        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()

        thread_key = th_event.key.thread_key

        assert ThreadStationState.objects.count() == 3
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id), outdated=False)
            for station in stations
        ]
        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({'type': EventStateType.CANCELLED})
        }))
        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({'type': EventStateType.CANCELLED}),
            'departure_state': has_properties({'type': EventStateType.CANCELLED})
        }))
        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({'type': EventStateType.CANCELLED})
        }))

        # после второго пересчёта того же треда без изменений старые стейты не должны быть перезаписаны
        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()

        thread_key = th_event.key.thread_key

        assert ThreadStationState.objects.count() == 3
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id), outdated=False)
            for station in stations
        ]
        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({'type': EventStateType.CANCELLED})
        }))
        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({'type': EventStateType.CANCELLED}),
            'departure_state': has_properties({'type': EventStateType.CANCELLED})
        }))
        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({'type': EventStateType.CANCELLED})
        }))

    @replace_now('2021-04-19 10:00:00')
    def test_partially_cancelled(self):
        start_dt = datetime(2021, 4, 19, 16, 0)
        stations = [create_station() for _ in range(3)]
        thread = create_thread(
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [10, 12, stations[1]],
                [25, None, stations[2]],
            ]
        )

        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_cancels': [
                StationCancelFactory.create_from_rtstations(
                    rtstations=list(thread.path)[1:],
                    dt_save=datetime(2021, 4, 19, 8, 0)
                )
            ],
            'need_recalc': True
        })
        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()

        thread_key = th_event.key.thread_key

        assert ThreadStationState.objects.count() == 2
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id), outdated=False)
            for station in stations[1:]
        ]
        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({'type': EventStateType.CANCELLED})
        }))
        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({'type': EventStateType.CANCELLED})
        }))

        # после второго пересчёта того же треда без изменений старые стейты не должны быть перезаписаны
        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()

        thread_key = th_event.key.thread_key

        assert ThreadStationState.objects.count() == 2
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id), outdated=False)
            for station in stations[1:]
        ]
        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({'type': EventStateType.CANCELLED})
        }))
        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({'type': EventStateType.CANCELLED})
        }))

    @replace_now('2021-04-19 16:00:00')
    def test_no_next_data_with_cancels(self):
        set_param('last_successful_query_time', datetime(2021, 4, 19, 15, 47))
        thread_key = '7thread_key'
        thread_uid = 'uid'
        thread_start_dt = datetime(2021, 4, 19, 13)

        dt_normative_1_a = thread_start_dt + timedelta(minutes=50)
        dt_fact_1_a = thread_start_dt + timedelta(minutes=55)

        stations = [create_station() for _ in range(4)]
        stations[2].use_in_forecast = True
        stations[2].save()

        thread = create_thread(
            uid=thread_uid,
            tz_start_time=thread_start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [50, 62, stations[1]],
                [120, 133, stations[2]],
                [240, None, stations[3]],
            ],
        )

        SuburbanKey.objects.create(thread=thread, key=thread_key)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_start_dt,
                thread_key=thread_key
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=thread_start_dt,
                    dt_fact=thread_start_dt,
                    time=0,
                ),
                StationEventFactory(
                    station_key=str(stations[1].id),
                    type='arrival',
                    dt_normative=dt_normative_1_a,
                    dt_fact=dt_fact_1_a,
                    time=50,
                )
            ],
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=thread_start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=50),
                    time=50,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=62),
                    time=62,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=120),
                    time=120,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=133),
                    time=133,
                ),
                StationExpectedEvent(
                    station_key=str(stations[3].id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=240),
                    time=240,
                ),
            ],
            'last_station_event': StationEventFactory(
                station_key=str(stations[1].id),
                type='arrival',
                dt_normative=dt_normative_1_a,
                dt_fact=dt_fact_1_a,
                time=50,
            ),
            'stations_cancels': [
                StationCancelFactory.create_from_rtstations(
                    rtstations=list(thread.path)[-2:],
                    dt_save=datetime(2021, 4, 19, 10, 0)
                )
            ],
            'need_recalc': False
        })

        StationUpdateInfoFactory(**{
            'station_key': str(stations[2].id),
            'timestamp': datetime(2021, 4, 19, 13)
        })

        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 4

        station_tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(st.id), outdated=False)
            for st in stations
        ]

        assert_that(station_tsses[0], has_properties({
            'departure_state': has_properties({
                'dt': thread_start_dt,
                'type': EventStateType.FACT,
                'thread_uid': thread_uid,
                'minutes_from': 0,
                'minutes_to': 0
            })
        }))

        assert_that(station_tsses[1], has_properties({
            'arrival_state': has_properties({
                'dt': dt_fact_1_a,
                'type': EventStateType.FACT,
                'thread_uid': thread_uid,
                'minutes_from': 5,
                'minutes_to': 5
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        assert_that(station_tsses[2], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            }),
            'departure_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            })
        }))

        assert_that(station_tsses[3], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            })
        }))

        # после второго пересчёта того же треда без изменений старые стейты не должны быть перезаписаны
        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 4

        station_tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(st.id), outdated=False)
            for st in stations
        ]

        assert_that(station_tsses[0], has_properties({
            'departure_state': has_properties({
                'dt': thread_start_dt,
                'type': EventStateType.FACT,
                'thread_uid': thread_uid,
                'minutes_from': 0,
                'minutes_to': 0
            })
        }))

        assert_that(station_tsses[1], has_properties({
            'arrival_state': has_properties({
                'dt': dt_fact_1_a,
                'type': EventStateType.FACT,
                'thread_uid': thread_uid,
                'minutes_from': 5,
                'minutes_to': 5
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        assert_that(station_tsses[2], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            }),
            'departure_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            })
        }))

        assert_that(station_tsses[3], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            })
        }))

    @replace_now('2021-04-19 14:00:00')
    def test_no_data_before_head_cancels(self):
        set_param('last_successful_query_time', datetime(2021, 4, 19, 13, 55))
        thread_key = '7thread_key'
        thread_start_dt = datetime(2021, 4, 19, 15, 0)

        stations = [create_station() for _ in range(4)]
        stations[0].use_in_departure_forecast = True
        stations[0].save()

        thread_uid = 'uid'
        thread = create_thread(
            uid=thread_uid,
            tz_start_time=thread_start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [60, 62, stations[1]],
                [100, 102, stations[2]],
                [200, None, stations[3]]
            ],
        )

        SuburbanKey.objects.create(thread=thread, key=thread_key)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_start_dt,
                thread_key=thread_key
            ),
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=thread_start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=60),
                    time=60,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=62),
                    time=62,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=100),
                    time=100,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=102),
                    time=102,
                ),
                StationExpectedEvent(
                    station_key=str(stations[3].id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=200),
                    time=200,
                )
            ],
            'stations_cancels': [
                StationCancelFactory.create_from_rtstations(
                    rtstations=list(thread.path)[:2],
                    dt_save=datetime(2021, 4, 19, 10, 0)
                )
            ],
            'need_recalc': True
        })

        StationUpdateInfoFactory(**{
            'station_key': str(stations[0].id),
            'timestamp': datetime(2021, 4, 19, 13, 55)
        })

        ThreadStationState.drop_collection()
        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 2

        # после второго пересчёта того же треда без изменений старые стейты не должны быть перезаписаны
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id), outdated=False)
            for station in stations[:2]
        ]

        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            }),
            'departure_state': None
        }))

        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 2

        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id), outdated=False)
            for station in stations[:2]
        ]

        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            }),
            'departure_state': None
        }))

    @replace_now('2021-04-19 15:25:00')
    def test_no_data_in_head_cancels(self):
        set_param('last_successful_query_time', datetime(2021, 4, 19, 15, 20))
        thread_key = '7thread_key'
        thread_start_dt = datetime(2021, 4, 19, 15, 0)

        stations = [create_station() for _ in range(4)]
        stations[0].use_in_departure_forecast = True
        stations[0].save()

        thread_uid = 'uid'
        thread = create_thread(
            uid=thread_uid,
            tz_start_time=thread_start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [30, 32, stations[1]],
                [100, 102, stations[2]],
                [200, None, stations[3]]
            ],
        )

        SuburbanKey.objects.create(thread=thread, key=thread_key)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_start_dt,
                thread_key=thread_key
            ),
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=thread_start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=30),
                    time=30,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=32),
                    time=32,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=100),
                    time=100,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=102),
                    time=102,
                ),
                StationExpectedEvent(
                    station_key=str(stations[3].id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=200),
                    time=200,
                )
            ],
            'stations_cancels': [
                StationCancelFactory.create_from_rtstations(
                    rtstations=list(thread.path)[:2],
                    dt_save=datetime(2021, 4, 19, 10, 0)
                )
            ],
            'need_recalc': False
        })

        StationUpdateInfoFactory(**{
            'station_key': str(stations[0].id),
            'timestamp': datetime(2021, 4, 19, 15, 20)
        })

        recalc_forecasts()  # отмены не учитываются
        assert ThreadStationState.objects.count() == 4

        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id))
            for station in stations
        ]

        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[3], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        ThreadStationState.drop_collection()

        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 2

        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id), outdated=False)
            for station in stations[:2]
        ]

        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            }),
            'departure_state': None
        }))

        # после второго пересчёта того же треда без изменений старые стейты не должны быть перезаписаны
        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 2

        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id), outdated=False)
            for station in stations[:2]
        ]

        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            }),
            'departure_state': None
        }))

    @replace_now('2021-04-19 16:00:00')
    def test_no_data_after_head_cancels(self):
        set_param('last_successful_query_time', datetime(2021, 4, 19, 15, 55))
        thread_key = '7thread_key'
        thread_start_dt = datetime(2021, 4, 19, 15, 0)

        stations = [create_station() for _ in range(4)]
        stations[0].use_in_departure_forecast = True
        stations[0].save()

        thread_uid = 'uid'
        thread = create_thread(
            uid=thread_uid,
            tz_start_time=thread_start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [30, 32, stations[1]],
                [100, 102, stations[2]],
                [200, None, stations[3]]
            ],
        )

        SuburbanKey.objects.create(thread=thread, key=thread_key)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=thread_start_dt,
                thread_key=thread_key
            ),
            'stations_expected_events': [
                StationExpectedEvent(
                    station_key=str(stations[0].id),
                    type='departure',
                    dt_normative=thread_start_dt,
                    time=0,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=30),
                    time=30,
                ),
                StationExpectedEvent(
                    station_key=str(stations[1].id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=32),
                    time=32,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=100),
                    time=100,
                ),
                StationExpectedEvent(
                    station_key=str(stations[2].id),
                    type='departure',
                    dt_normative=thread_start_dt + timedelta(minutes=102),
                    time=102,
                ),
                StationExpectedEvent(
                    station_key=str(stations[3].id),
                    type='arrival',
                    dt_normative=thread_start_dt + timedelta(minutes=200),
                    time=200,
                )
            ],
            'stations_cancels': [
                StationCancelFactory.create_from_rtstations(
                    rtstations=list(thread.path)[:2],
                    dt_save=datetime(2021, 4, 19, 10, 0)
                )
            ],
            'need_recalc': False
        })

        StationUpdateInfoFactory(**{
            'station_key': str(stations[0].id),
            'timestamp': datetime(2021, 4, 19, 15, 55)
        })

        recalc_forecasts()  # отмены не учитываются
        assert ThreadStationState.objects.count() == 4

        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id))
            for station in stations
        ]

        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[3], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        ThreadStationState.drop_collection()

        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 4

        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id))
            for station in stations
        ]

        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[3], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        # после второго пересчёта того же треда без изменений старые стейты не должны быть перезаписаны
        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()
        assert ThreadStationState.objects.count() == 4

        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id))
            for station in stations
        ]

        assert_that(tsses[0], has_properties({
            'departure_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.CANCELLED,
                'thread_uid': thread_uid
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            }),
            'departure_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

        assert_that(tsses[3], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.POSSIBLE_DELAY,
                'thread_uid': thread_uid
            })
        }))

    @replace_now('2021-04-19 16:00:00')
    def test_interpolation_with_head_cancels(self):
        start_dt = datetime(2021, 4, 19, 10, 0)
        stations = [create_station() for _ in range(4)]
        thread = create_thread(
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [10, 12, stations[1]],
                [25, 30, stations[2]],
                [35, None, stations[3]]
            ]
        )

        last_event = StationEventFactory(
            station_key=str(stations[1].id),
            type='arrival',
            dt_normative=start_dt + timedelta(minutes=10),
            dt_fact=start_dt + timedelta(minutes=12),
        )
        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_events': [
                last_event,
                StationEventFactory(
                    station_key=str(stations[2].id),
                    type='arrival',
                    dt_normative=start_dt + timedelta(minutes=25),
                    dt_fact=start_dt + timedelta(minutes=30)
                )
            ],
            'last_station_event': last_event,
            'stations_cancels': [
                StationCancelFactory.create_from_rtstations(
                    rtstations=list(thread.path)[:2],
                    dt_save=datetime(2021, 4, 19, 8, 0)
                )
            ],
            'need_recalc': True
        })
        thread_key = th_event.key.thread_key

        recalc_forecasts()  # без отмен

        assert ThreadStationState.objects.count() == 3
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id))
            for station in stations[1:]
        ]
        assert_that(tsses[0], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.FACT,
                'minutes_from': 2,
                'minutes_to': 2
            }),
            'departure_state': has_properties({
                'type': EventStateType.FACT_INTERPOLATED,
                'minutes_from': 2,
                'minutes_to': 5
            })
        }))
        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.FACT,
                'minutes_from': 5,
                'minutes_to': 5
            }),
            'departure_state': has_properties({'type': EventStateType.POSSIBLE_OK})
        }))
        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({'type': EventStateType.POSSIBLE_OK}),
            'departure_state': None
        }))

        ThreadStationState.drop_collection()
        th_event.update(set__need_recalc=True)

        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()

        assert ThreadStationState.objects.count() == 4
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id))
            for station in stations
        ]
        assert_that(tsses[0], has_properties({
            'arrival_state': None,
            'departure_state': has_properties({'type': EventStateType.CANCELLED})
        }))
        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({'type': EventStateType.CANCELLED}),
            'departure_state': None
        }))
        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.FACT,
                'minutes_from': 5,
                'minutes_to': 5
            }),
            'departure_state': has_properties({'type': EventStateType.POSSIBLE_OK})
        }))
        assert_that(tsses[3], has_properties({
            'arrival_state': has_properties({'type': EventStateType.POSSIBLE_OK}),
            'departure_state': None
        }))

    @replace_now('2021-04-19 16:00:00')
    def test_interpolation_with_tail_cancels(self):
        start_dt = datetime(2021, 4, 19, 10, 0)
        stations = [create_station() for _ in range(4)]
        thread = create_thread(
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [10, 12, stations[1]],
                [25, 30, stations[2]],
                [35, None, stations[3]]
            ]
        )

        last_event = StationEventFactory(
            station_key=str(stations[0].id),
            type='departure',
            dt_normative=start_dt,
            dt_fact=start_dt,
        )
        th_event = ThreadEventsFactory(**{
            'thread': thread,
            'thread_start_date': start_dt,
            'stations_events': [
                last_event,
                StationEventFactory(
                    station_key=str(stations[1].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=12),
                    dt_fact=start_dt + timedelta(minutes=15)
                ),
                StationEventFactory(
                    station_key=str(stations[2].id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=30),
                    dt_fact=start_dt + timedelta(minutes=30)
                )
            ],
            'last_station_event': last_event,
            'stations_cancels': [
                StationCancelFactory.create_from_rtstations(
                    rtstations=list(thread.path)[2:],
                    dt_save=datetime(2021, 4, 19, 8, 0)
                )
            ],
            'need_recalc': True
        })
        thread_key = th_event.key.thread_key

        recalc_forecasts()  # без отмен

        assert ThreadStationState.objects.count() == 4
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id))
            for station in stations
        ]
        assert_that(tsses[0], has_properties({
            'arrival_state': None,
            'departure_state': has_properties({
                'type': EventStateType.FACT,
                'minutes_from': 0,
                'minutes_to': 0
            })
        }))
        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.FACT_INTERPOLATED,
                'minutes_from': 1,
                'minutes_to': 3
            }),
            'departure_state': has_properties({
                'type': EventStateType.FACT,
                'minutes_from': 3,
                'minutes_to': 3
            })
        }))
        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.FACT_INTERPOLATED,
                'minutes_from': 1,
                'minutes_to': 3
            }),
            'departure_state': has_properties({
                'type': EventStateType.FACT,
                'minutes_from': 0,
                'minutes_to': 0
            })
        }))
        assert_that(tsses[3], has_properties({
            'arrival_state': has_properties({'type': EventStateType.POSSIBLE_OK}),
            'departure_state': None
        }))

        ThreadStationState.drop_collection()
        th_event.update(set__need_recalc=True)

        with replace_dynamic_setting('SUBURBAN_ENABLE_CANCELS', True):
            recalc_forecasts()

        assert ThreadStationState.objects.count() == 4
        tsses = [
            ThreadStationState.objects.get(key__thread_key=thread_key, key__station_key=str(station.id))
            for station in stations
        ]
        assert_that(tsses[0], has_properties({
            'arrival_state': None,
            'departure_state': has_properties({
                'type': EventStateType.FACT,
                'minutes_from': 0,
                'minutes_to': 0
            })
        }))
        assert_that(tsses[1], has_properties({
            'arrival_state': has_properties({
                'type': EventStateType.FACT_INTERPOLATED,
                'minutes_from': 1,
                'minutes_to': 3
            }),
            'departure_state': has_properties({
                'type': EventStateType.FACT,
                'minutes_from': 3,
                'minutes_to': 3
            })
        }))
        assert_that(tsses[2], has_properties({
            'arrival_state': has_properties({'type': EventStateType.POSSIBLE_OK}),
            'departure_state': has_properties({'type': EventStateType.CANCELLED})
        }))
        assert_that(tsses[3], has_properties({
            'arrival_state': has_properties({'type': EventStateType.CANCELLED}),
            'departure_state': None
        }))


class TestMCZKForecast(object):
    def test_mczk_forecast(self):
        thread_key_1 = 'thread_key_1'
        start_dt = datetime(2017, 9, 10, 13)
        station_from, station_to, station_mid = create_station(), create_station(), create_station()
        thread_1 = create_thread(
            number='МЦК',
            uid='MCZK_1',
            title='по часовой стрелке',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, station_from],
                [10, 20, station_mid],
                [50, None, station_to],
            ],
        )
        SuburbanKey.objects.create(thread=thread_1, key=thread_key_1)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=start_dt,
                thread_key=thread_key_1,
                thread_type=ThreadEventsTypeCodes.MCZK,
                clock_direction=ClockDirection.CLOCK_WISE
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_mid.id),
                    type='departure',
                    dt_normative=start_dt + timedelta(minutes=20),
                    dt_fact=start_dt + timedelta(minutes=35)
                ),
            ],
            'need_recalc': True,
        })

        with replace_dynamic_setting('SUBURBAN_ENABLE_MCZK', True):
            recalc_forecasts()

        # Для МЦК прогноз не должен строиться.
        assert ThreadStationState.objects.count() == 1
        thread_states_mid = ThreadStationState.objects.get(key__thread_key=thread_key_1,
                                                           key__station_key=str(station_mid.id))
        assert_that(thread_states_mid.departure_state,
                    has_properties({
                        'type': EventStateType.FACT,
                        'thread_uid': 'MCZK_1',
                    }))

    def test_clock_direction(self):
        thread_key = 'key'
        start_dt = datetime(2017, 9, 10, 13)
        station_from, station_to = create_station(), create_station()
        thread_1 = create_thread(
            number='МЦК',
            uid='MCZK_1',
            title='по часовой стрелке',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, station_from],
                [50, None, station_to],
            ],
        )

        # Нитка отличается от первой только направлением движения.
        thread_2 = create_thread(
            number='МЦК',
            uid='MCZK_2',
            title='против часовой стрелки',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, station_from],
                [50, None, station_to],
            ],
        )

        SuburbanKey.objects.create(thread=thread_1, key=thread_key)
        SuburbanKey.objects.create(thread=thread_2, key=thread_key)

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=start_dt,
                thread_key=thread_key,
                thread_type=ThreadEventsTypeCodes.MCZK,
                clock_direction=ClockDirection.CLOCK_WISE
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=start_dt,
                    dt_fact=start_dt + timedelta(minutes=5)
                ),
            ],
            'need_recalc': True,
        })

        ThreadEventsFactory(**{
            'key': ThreadKey(
                thread_start_date=start_dt,
                thread_key=thread_key,
                thread_type=ThreadEventsTypeCodes.MCZK,
                clock_direction=ClockDirection.C_CLOCK_WISE
            ),
            'stations_events': [
                StationEventFactory(
                    station_key=str(station_from.id),
                    type='departure',
                    dt_normative=start_dt,
                    dt_fact=start_dt + timedelta(minutes=10)
                ),
            ],
            'need_recalc': True,
        })

        with replace_dynamic_setting('SUBURBAN_ENABLE_MCZK', True):
            recalc_forecasts()

        # Для МЦК прогноз не должен строиться.
        assert ThreadStationState.objects.count() == 2
        thread_states_mid = ThreadStationState.objects.get(key__thread_key=thread_key,
                                                           key__station_key=str(station_from.id),
                                                           key__clock_direction=ClockDirection.CLOCK_WISE)
        assert_that(thread_states_mid.departure_state,
                    has_properties({
                        'type': EventStateType.FACT,
                        'thread_uid': 'MCZK_1',
                        'dt': start_dt + timedelta(minutes=5)
                    }))

        thread_states_mid = ThreadStationState.objects.get(key__thread_key=thread_key,
                                                           key__station_key=str(station_from.id),
                                                           key__clock_direction=ClockDirection.C_CLOCK_WISE)
        assert_that(thread_states_mid.departure_state,
                    has_properties({
                        'type': EventStateType.FACT,
                        'thread_uid': 'MCZK_2',
                        'dt': start_dt + timedelta(minutes=10)
                    }))

    def test_save_thread_events_no_need_recalc(self):
        now = datetime.now()
        forecaster = Forecaster()
        forecaster.save_thread_events_no_need_recalc()  # проверяем, что всё ок без объектов

        thread_events = forecaster.thread_events_no_need_recalc = []
        for i in range(3):
            thread_event = ThreadEvents.objects.create(
                key={'thread_key': str(i), 'thread_start_date': now},
                need_recalc=False
            )
            thread_events.append(thread_event)

        for i in range(3, 6):
            thread_event = ThreadEvents.objects.create(
                key={'thread_key': str(i), 'thread_start_date': now},
                need_recalc=True
            )
            thread_events.append(thread_event)

        assert ThreadEvents.objects.count() == 6
        assert ThreadEvents.objects.filter(need_recalc=False).count() == 3
        forecaster.save_thread_events_no_need_recalc()
        assert ThreadEvents.objects.count() == 6
        assert ThreadEvents.objects.filter(need_recalc=False).count() == 6


class TestForecaster(object):
    def test_save_parallel(self):
        forecaster = Forecaster()
        forecaster.thread_station_states = [ThreadStationState() for i in range(13)]

        with mock.patch.object(forecast, 'run_parallel') as m_run_parallel, \
                mock.patch.dict(os.environ, {'QLOUD_PROJECT': 'rasp', 'QLOUD_CPU_GUARANTEE': '3.0'}):

            forecaster.save_forecast()
            assert m_run_parallel.call_args_list[0][0] == (save_forecast_parallel, [(0, 5), (5, 10), (10, 15)], 3)

    def test_setup_thread_events(self):
        start_dt = datetime(2017, 3, 20, 11, 12, 13)
        thread = create_thread(
            year_days=[start_dt],
            tz_start_time=start_dt.time(),
            schedule_v1=[[None, 11], [350, None]],
        )
        th_event = ThreadEventsFactory(thread=thread, thread_start_date=start_dt)
        th_event.thread = None  # фабрика выставляет thread; сносим, чтобы проверить setup

        th_events = Forecaster().setup_thread_events([th_event])
        assert th_events == [th_event]
        assert th_event.thread == thread
        assert th_event.rts_path == list(thread.path)

        # проверяем, что эвент отбрасывается, если thread не нашелся
        th_event.thread = None
        thread.delete()
        th_events = Forecaster().setup_thread_events([th_event])
        assert not th_events


def test_save_forecast_parallel():
    now = datetime.now()

    thread = create_thread(schedule_v1=[[None, 11], [350, None]])
    th_event = ThreadEventsFactory(thread=thread)

    def create_tss(**kwargs):
        tss = ThreadStationState(**kwargs)
        tss.th_event = th_event
        tss.rts = th_event.rts_path[0]
        return tss

    forecaster = Forecaster()
    Forecaster.instance = forecaster
    forecaster.thread_station_states = {
        ThreadKey(thread_key='1', thread_start_date=now, thread_type='mczk', clock_direction=1): [
            create_tss(
                key=ThreadStationKeyFactory(
                    thread_key='1', thread_start_date=now, thread_type='mczk', clock_direction=1,
                    station_key=str(i),
                )
            ) for i in range(3)
        ],
        ThreadKey(thread_key='2'): [
            create_tss(
                key=ThreadStationKeyFactory(
                    thread_key='2', thread_start_date=now,
                    station_key=str(i),
                )
            ) for i in range(3)
        ],
    }

    save_forecast_parallel((0, 1))
    assert ThreadStationState.objects.count() == 3
    save_forecast_parallel((0, 1))
    assert ThreadStationState.objects.count() == 3
    save_forecast_parallel((1, 2))
    assert ThreadStationState.objects.count() == 6

    # уменьшаем количество эвентов, часть должна стать outdated=True
    forecaster.thread_station_states = {
        ThreadKey(thread_key='1', thread_start_date=now, thread_type='mczk', clock_direction=1): [
            create_tss(
                key=ThreadStationKey(
                    thread_key='1', thread_start_date=now, thread_type='mczk', clock_direction=1,
                    station_key=str(i),
                )
            ) for i in range(2)
        ],
        ThreadKey(thread_key='2'): [
            create_tss(
                key=ThreadStationKey(thread_key='2', station_key=str(i), thread_start_date=now,)
            ) for i in range(2)
        ],
    }
    save_forecast_parallel((0, 2))
    assert ThreadStationState.objects.count() == 6
    assert ThreadStationState.objects.filter(outdated=True).count() == 2


class TestForecastsFuncs(object):
    def test_filter_events_by_prev_max_next_min(self):
        start_dt = datetime(2018, 2, 20, 13)
        stations = [create_station() for _ in range(3)]
        thread_1 = create_thread(
            uid='uid_1',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [20, 15, stations[1]],
                [50, None, stations[2]],
            ],
        )
        th_event = ThreadEventsFactory(thread=thread_1, thread_start_date=start_dt)

        pack_1_dt = datetime(2018, 2, 20, 13, 16)
        pack_2_dt = datetime(2018, 2, 20, 13, 22)
        pack_3_dt = datetime(2018, 2, 20, 13, 30)
        pack_4_dt = datetime(2018, 2, 20, 13, 55)
        pack_5_dt = datetime(2018, 2, 20, 13, 58)

        pack_1 = [
            StationEventFactory(
                station_key=str(stations[0].id),
                type='departure',
                dt_normative=start_dt,
                dt_fact=start_dt + timedelta(minutes=10),
                dt_save=pack_1_dt
            ),
            StationEventFactory(
                station_key=str(stations[0].id),
                type='departure',
                dt_normative=start_dt,
                dt_fact=start_dt + timedelta(minutes=15),
                dt_save=pack_1_dt
            ),
            # невалидное событие
            StationEventFactory(
                station_key=str(stations[1].id),
                type='arrival',
                dt_normative=start_dt,
                dt_fact=start_dt + timedelta(minutes=15),
                dt_save=pack_1_dt
            ),
        ]
        pack_2 = [StationEventFactory(
            station_key=str(stations[1].id),
            type='arrival',
            dt_normative=start_dt,
            dt_fact=start_dt + timedelta(minutes=25),
            dt_save=pack_2_dt
        )]
        pack_3 = [
            # невалидное событие
            StationEventFactory(
                station_key=str(stations[2].id),
                type='arrival',
                dt_normative=start_dt,
                dt_fact=start_dt + timedelta(minutes=25),
                dt_save=pack_3_dt
            ),
            StationEventFactory(
                station_key=str(stations[2].id),
                type='arrival',
                dt_normative=start_dt,
                dt_fact=start_dt + timedelta(minutes=30),
                dt_save=pack_3_dt
            )
        ]
        pack_4 = [
            # невалидное событие
            StationEventFactory(
                station_key=str(stations[1].id),
                type='arrival',
                dt_normative=start_dt,
                dt_fact=start_dt + timedelta(minutes=30),
                dt_save=pack_4_dt
            ),
            StationEventFactory(
                station_key=str(stations[2].id),
                type='arrival',
                dt_normative=start_dt,
                dt_fact=start_dt + timedelta(minutes=51),
                dt_save=pack_4_dt
            )
        ]
        pack_5 = [
            StationEventFactory(
                station_key=str(stations[1].id),
                type='departure',
                dt_normative=start_dt,
                dt_fact=start_dt + timedelta(minutes=26),
                dt_save=pack_5_dt
            ),
        ]

        th_event.stations_events = pack_1
        stations_events = find_rts_for_events(th_event)
        filtered_events = filter_events_by_prev_max_next_min(stations_events)
        assert_that(filtered_events, contains_inanyorder(
            has_properties({
                'station_key': str(stations[0].id),
                'type': 'departure',
                'dt_fact': start_dt + timedelta(minutes=10)
            }),
            has_properties({
                'station_key': str(stations[0].id),
                'type': 'departure',
                'dt_fact': start_dt + timedelta(minutes=15)
            })
        ))

        th_event.stations_events = pack_1 + pack_2
        stations_events = find_rts_for_events(th_event)
        filtered_events = filter_events_by_prev_max_next_min(stations_events)
        assert_that(filtered_events, contains_inanyorder(
            has_properties({
                'station_key': str(stations[0].id),
                'type': 'departure',
                'dt_fact': start_dt + timedelta(minutes=10)
            }),
            has_properties({
                'station_key': str(stations[0].id),
                'type': 'departure',
                'dt_fact': start_dt + timedelta(minutes=15)
            }),
            has_properties({
                'station_key': str(stations[1].id),
                'type': 'arrival',
                'dt_fact': start_dt + timedelta(minutes=25)
            })
        ))

        th_event.stations_events = pack_1 + pack_2 + pack_3
        stations_events = find_rts_for_events(th_event)
        filtered_events = filter_events_by_prev_max_next_min(stations_events)
        assert_that(filtered_events, contains_inanyorder(
            has_properties({
                'station_key': str(stations[0].id),
                'type': 'departure',
                'dt_fact': start_dt + timedelta(minutes=10)
            }),
            has_properties({
                'station_key': str(stations[0].id),
                'type': 'departure',
                'dt_fact': start_dt + timedelta(minutes=15)
            }),
            has_properties({
                'station_key': str(stations[1].id),
                'type': 'arrival',
                'dt_fact': start_dt + timedelta(minutes=25)
            }),
            has_properties({
                'station_key': str(stations[2].id),
                'type': 'arrival',
                'dt_fact': start_dt + timedelta(minutes=30)
            })
        ))

        th_event.stations_events = pack_1 + pack_2 + pack_3 + pack_4
        stations_events = find_rts_for_events(th_event)
        filtered_events = filter_events_by_prev_max_next_min(stations_events)
        assert_that(filtered_events, contains_inanyorder(
            has_properties({
                'station_key': str(stations[0].id),
                'type': 'departure',
                'dt_fact': start_dt + timedelta(minutes=10)
            }),
            has_properties({
                'station_key': str(stations[0].id),
                'type': 'departure',
                'dt_fact': start_dt + timedelta(minutes=15)
            }),
            has_properties({
                'station_key': str(stations[1].id),
                'type': 'arrival',
                'dt_fact': start_dt + timedelta(minutes=25)
            }),
            has_properties({
                'station_key': str(stations[2].id),
                'type': 'arrival',
                'dt_fact': start_dt + timedelta(minutes=30)
            }),
            has_properties({
                'station_key': str(stations[2].id),
                'type': 'arrival',
                'dt_fact': start_dt + timedelta(minutes=51)
            })
        ))

        th_event.stations_events = pack_1 + pack_2 + pack_3 + pack_4 + pack_5
        stations_events = find_rts_for_events(th_event)
        filtered_events = filter_events_by_prev_max_next_min(stations_events)
        assert_that(filtered_events, contains_inanyorder(
            has_properties({
                'station_key': str(stations[0].id),
                'type': 'departure',
                'dt_fact': start_dt + timedelta(minutes=10)
            }),
            has_properties({
                'station_key': str(stations[0].id),
                'type': 'departure',
                'dt_fact': start_dt + timedelta(minutes=15)
            }),
            has_properties({
                'station_key': str(stations[1].id),
                'type': 'arrival',
                'dt_fact': start_dt + timedelta(minutes=25)
            }),
            has_properties({
                'station_key': str(stations[2].id),
                'type': 'arrival',
                'dt_fact': start_dt + timedelta(minutes=30)
            }),
            has_properties({
                'station_key': str(stations[2].id),
                'type': 'arrival',
                'dt_fact': start_dt + timedelta(minutes=51)
            }),
            has_properties({
                'station_key': str(stations[1].id),
                'type': 'departure',
                'dt_fact': start_dt + timedelta(minutes=26)
            }),
        ))

    def test_get_closest_rts(self):
        start_dt = datetime(2018, 2, 20, 13)
        stations = [create_station() for _ in range(2)]
        thread = create_thread(
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [95, 96, stations[1]],
                [100, 101, stations[1]],
                [111, None, stations[1]],
            ],
        )

        rtstations = thread.path[1:]

        event = StationEventFactory(
            station_key=str(stations[1].id),
            type='arrival',
            time=100,
            dt_fact=start_dt + timedelta(minutes=104)
        )

        rts = get_closest_rts(event, rtstations, start_dt)
        assert rts == rtstations[1]
        assert event.minutes_diff == 4

        with replace_dynamic_setting('SUBURBAN_MAX_EVENT_DELAY', 3):
            rts = get_closest_rts(event, rtstations, start_dt)
            assert rts == rtstations[2]
            assert event.minutes_diff == -7

            with replace_dynamic_setting('SUBURBAN_MAX_EVENT_OVERTAKING', 6):
                rts = get_closest_rts(event, rtstations, start_dt)
                assert rts is None

    def test_filter_events_by_rts(self):
        start_dt = datetime(2018, 2, 20, 13)
        stations = [create_station() for _ in range(4)]
        thread = create_thread(
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [30, 31, stations[1]],
                [60, 61, stations[2]],
                [90, 91, stations[1]],
                [120, 121, stations[2]],
                [150, None, stations[3]],
            ],
        )

        events = [
            StationEventFactory(
                station_key=str(stations[1].id),
                type='departure',
                dt_fact=start_dt + timedelta(minutes=86),
                time=91
            ),
            StationEventFactory(
                station_key=str(stations[2].id),
                type='arrival',
                dt_fact=start_dt + timedelta(minutes=62),
                time=60
            ),
            StationEventFactory(
                station_key=str(stations[2].id),
                type='arrival',
                dt_fact=start_dt + timedelta(minutes=114),
                time=120
            ),
            StationEventFactory(
                station_key=str(stations[3].id),
                type='arrival',
                dt_fact=start_dt + timedelta(minutes=153),
                time=150
            ),
        ]

        th_event = ThreadEventsFactory(thread=thread, thread_start_date=start_dt, stations_events=events)
        filtered_events = find_rts_for_events(th_event)

        assert_that(filtered_events, contains_inanyorder(
            has_properties({
                'station_key': str(stations[1].id),
                'type': 'departure',
                'dt_fact': start_dt + timedelta(minutes=86),
                'station_idx': 3,
                'minutes_diff': -5
            }),
            has_properties({
                'station_key': str(stations[2].id),
                'type': 'arrival',
                'dt_fact': start_dt + timedelta(minutes=62),
                'station_idx': 2,
                'minutes_diff': 2
            }),
            has_properties({
                'station_key': str(stations[2].id),
                'type': 'arrival',
                'dt_fact': start_dt + timedelta(minutes=114),
                'station_idx': 4,
                'minutes_diff': -6
            }),
            has_properties({
                'station_key': str(stations[3].id),
                'type': 'arrival',
                'dt_fact': start_dt + timedelta(minutes=153),
                'station_idx': 5,
                'minutes_diff': 3
            })
        ))

    def test_compare_tsses_cancels_first(self):
        start_dt = datetime(2021, 4, 19, 13)
        stations = [create_station() for _ in range(4)]
        thread = create_thread(
            number='thread',
            tz_start_time=start_dt.time(),
            schedule_v1=[
                [None, 0, stations[0]],
                [30, 31, stations[1]],
                [60, 61, stations[2]],
                [90, None, stations[3]],
            ],
        )
        th_keys = [
            ThreadStationKeyFactory(
                thread_key='thread_1_key',
                thread_start_date=start_dt,
                station_key=str(rts.id),
                arrival=None,
                departure=0
            ) for rts in thread.path
        ]

        cancelled_tsses = [
            ThreadStationState(
                key=th_keys[0],
                departure_state=EventState(type=EventStateType.CANCELLED)
            ),
            ThreadStationState(
                key=th_keys[1],
                arrival_state=EventState(type=EventStateType.CANCELLED)
            )
        ]
        for tss, rts in zip(cancelled_tsses, thread.path):
            tss.rts = rts

        fact_tsses = [
            ThreadStationState(
                key=th_keys[0],
                departure_state=EventState(type=EventStateType.FACT)
            ),
            ThreadStationState(
                key=th_keys[1],
                arrival_state=EventState(type=EventStateType.POSSIBLE_DELAY),
                departure_state=EventState(type=EventStateType.POSSIBLE_DELAY)
            ),
            ThreadStationState(
                key=th_keys[2],
                arrival_state=EventState(type=EventStateType.POSSIBLE_DELAY),
                departure_state=EventState(type=EventStateType.POSSIBLE_DELAY)
            ),
            ThreadStationState(
                key=th_keys[3],
                arrival_state=EventState(type=EventStateType.POSSIBLE_DELAY)
            )
        ]
        for tss, rts in zip(fact_tsses, thread.path):
            tss.rts = rts

        filtered_tsses = {tss.key:tss for tss in compare_tsses_cancels_first(fact_tsses, cancelled_tsses)}

        expected_tsses = {
            th_keys[0]: ThreadStationState(
                key=th_keys[0],
                departure_state=cancelled_tsses[0].departure_state
            ),
            th_keys[1]: ThreadStationState(
                key=th_keys[1],
                arrival_state=cancelled_tsses[1].arrival_state,
                departure_state=fact_tsses[1].departure_state
            ),
            th_keys[2]: ThreadStationState(
                key=th_keys[2],
                arrival_state=fact_tsses[2].arrival_state,
                departure_state=fact_tsses[2].departure_state
            ),
            th_keys[3]: ThreadStationState(
                key=th_keys[3],
                arrival_state=fact_tsses[3].arrival_state,
            )
        }

        for th_key in th_keys:
            filtered_tss, expected_tss = filtered_tsses[th_key], expected_tsses[th_key]
            assert filtered_tss.arrival_state == expected_tss.arrival_state
            assert filtered_tss.departure_state == expected_tss.departure_state
