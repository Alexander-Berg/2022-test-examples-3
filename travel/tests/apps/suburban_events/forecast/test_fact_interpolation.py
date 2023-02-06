# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import assert_that, has_properties

from common.apps.suburban_events.factories import ThreadStationStateFactory, EventStateFactory
from common.apps.suburban_events.forecast import fact_interpolation
from common.apps.suburban_events.forecast.fact_interpolation import interpolate_tss_path, interpolate_tss_states
from common.apps.suburban_events.utils import EventStateType
from common.tester.factories import create_rtstation, create_thread, create_station

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def create_tss(arrival=None, departure=None):
    tss = ThreadStationStateFactory()
    tss.rts = create_rtstation(thread=create_thread(), station=create_station())

    if arrival is not None:
        tss.set_arrival_state(EventStateFactory(minutes_from=arrival))

    if departure is not None:
        tss.set_departure_state(EventStateFactory(minutes_from=departure))

    return tss


class TestInterpolateTss(object):
    @pytest.mark.parametrize(
        'prev_late, next_late, exp_minutes_from, exp_minutes_to, arrival, departure',
        [
            [0, 0, 0, 0, False, False],
            [-10, -20, 0, 0, False, False],
            [11, 10, 11, 11, False, False],
            [8, 9, 9, 9, True, False],
            [1000, 100, 100, 1000, False, False],
            [12, 0, 1, 12, False, False],
            [1, 0, 1, 1, False, False],
            [8, 9, None, None, True, True],
        ]
    )
    def test_valid(self, prev_late, next_late, exp_minutes_from, exp_minutes_to, arrival, departure):
        prev_fact = EventStateFactory(type=EventStateType.FACT, minutes_from=prev_late, minutes_to=prev_late)
        next_fact = EventStateFactory(type=EventStateType.FACT, minutes_from=next_late, minutes_to=next_late)

        tss = create_tss()
        if arrival:
            tss.set_arrival_state(EventStateFactory(type=EventStateType.FACT, minutes_from=666))

        if departure:
            tss.set_departure_state(EventStateFactory(type=EventStateType.FACT, minutes_from=777))

        interpolate_tss_states(prev_fact, next_fact, tss)

        if not arrival:
            assert_that(tss.arrival_state, has_properties(
                type=EventStateType.FACT_INTERPOLATED,
                minutes_from=exp_minutes_from,
                minutes_to=exp_minutes_to,
                thread_uid=tss.rts.thread.uid
            ))
        else:
            assert_that(tss.arrival_state, has_properties(
                type=EventStateType.FACT,
                minutes_from=666,
            ))

        if not departure:
            assert_that(tss.departure_state, has_properties(
                type=EventStateType.FACT_INTERPOLATED,
                minutes_from=exp_minutes_from,
                minutes_to=exp_minutes_to,
                thread_uid=tss.rts.thread.uid
            ))
        else:
            assert_that(tss.departure_state, has_properties(
                type=EventStateType.FACT,
                minutes_from=777,
            ))


class TestInterpolateTssPath(object):
    def test_valid(self):
        tss0 = create_tss()
        tss1 = create_tss(arrival=None, departure=0)
        tss2 = create_tss()
        tss3 = create_tss(arrival=8, departure=10)
        tss4 = create_tss()
        tss5 = create_tss(arrival=18)
        tss6 = create_tss()
        tss7 = create_tss()
        tss8 = create_tss(departure=20)
        tss9 = create_tss(arrival=20)
        tss10 = create_tss()

        tss_path = [tss0, tss1, tss2, tss3, tss4, tss5, tss6, tss7, tss8, tss9, tss10]

        expected = [
            [0, 8, tss2],
            [10, 18, tss4],
            [18, 20, tss5],
            [18, 20, tss6],
            [18, 20, tss7],
            [18, 20, tss8],
        ]

        with mock.patch.object(fact_interpolation, 'interpolate_tss_states') as m_interpolate_tss_states:
            interpolate_tss_path(tss_path)

            assert len(m_interpolate_tss_states.call_args_list) == len(expected)

            for i, (exp_prev, exp_next, exp_tss) in enumerate(expected):
                call = m_interpolate_tss_states.call_args_list[i][0]
                assert_that(call[0], has_properties(minutes_from=exp_prev))
                assert_that(call[1], has_properties(minutes_from=exp_next))
                assert_that(call[2], exp_tss)
