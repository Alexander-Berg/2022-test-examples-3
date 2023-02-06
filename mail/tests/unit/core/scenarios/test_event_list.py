from datetime import date, datetime, time, timedelta, timezone
from enum import Enum

import pytest
from dateutil.relativedelta import relativedelta

from hamcrest import all_of, assert_that, contains, has_properties, is_

from mail.ciao.ciao.core.entities.analytcs import Analytics, Intent
from mail.ciao.ciao.core.entities.button import TypeTextButton
from mail.ciao.ciao.core.entities.enums import DateEnum, EventListSingleEvent, FrameName, YesNo
from mail.ciao.ciao.core.entities.missing import MissingType
from mail.ciao.ciao.core.entities.scenario_response import ScenarioResponse
from mail.ciao.ciao.core.entities.scenario_result import ScenarioResult
from mail.ciao.ciao.core.exceptions import CoreIrrelevantScenarioError
from mail.ciao.ciao.core.scenarios.event_list import (
    DeleteEventScenario, Event, EventListScenario, EventListScenarioState, FindEventScenario, RescheduleEventScenario
)
from mail.ciao.ciao.tests.utils import dummy_async_function
from mail.ciao.ciao.utils.datetime import UserDate
from mail.ciao.ciao.utils.format import format_month


@pytest.fixture
def user_timezone(user):
    return user.timezone


@pytest.fixture
def analytics():
    return Analytics(Intent.EVENT_LIST)


def test_get_params():
    scenario = EventListScenario()
    state_value = scenario._state = 'state-value'
    events_value = scenario._events = 'events-value'
    day = scenario._day = 'day-value'
    found_event = scenario._found_event = 'found-event-value'
    assert scenario.get_params() == {
        'state': state_value,
        'events': events_value,
        'day': day,
        'found_event': found_event,
    }


class TestFormatTimeTz:
    @pytest.fixture
    def utc_datetime(self):
        return datetime(2020, 2, 17, 15, 14, 30, tzinfo=timezone.utc)

    def test_result(self, utc_datetime, user_timezone):
        user_datetime = utc_datetime.astimezone(user_timezone)
        assert EventListScenario.format_time_tz(utc_datetime, user_timezone) == user_datetime.strftime('%H:%M')


@pytest.mark.parametrize('start,end,day,result', (
    pytest.param(
        datetime(2020, 4, 7, 10),
        datetime(2020, 4, 7, 12),
        date(2020, 4, 7),
        ('10:00', '12:00'),
        id='event_inside_day',
    ),
    pytest.param(
        datetime(2020, 4, 6, 10),
        datetime(2020, 4, 7, 12),
        date(2020, 4, 7),
        ('начала дня', '12:00'),
        id='event_starts_before_day',
    ),
    pytest.param(
        datetime(2020, 4, 7, 10),
        datetime(2020, 4, 8, 12),
        date(2020, 4, 7),
        ('10:00', 'конца дня'),
        id='event_ends_after_day',
    ),
    pytest.param(
        datetime(2020, 4, 6, 10),
        datetime(2020, 4, 8, 12),
        date(2020, 4, 7),
        ('начала', 'конца дня'),
        id='event_starts_and_ends_outside_of_day',
    ),
))
def test_format_start_end(user_timezone, start, end, day, result):
    assert EventListScenario.format_start_end(
        start=user_timezone.localize(start),
        end=user_timezone.localize(end),
        day=day,
        tz=user_timezone,
    ) == result


class TestFormatEvent:
    @pytest.fixture
    def event(self, randn, rands):
        return Event(
            event_id=randn(),
            external_id=rands(),
            name='event-name',
            description=rands(),
            start_ts=datetime(2020, 2, 17, 15, 29, 30),
            end_ts=datetime(2020, 3, 18, 16, 30, 31),
            others_can_view=False,
            sequence=randn(),
            all_day=False,
        )

    @pytest.fixture
    def day(self):
        return date(2020, 2, 18)

    @pytest.fixture(autouse=True)
    def format_start_end_mock(self, mocker):
        return mocker.patch.object(
            EventListScenario,
            'format_start_end',
            mocker.Mock(side_effect=lambda start, end, day, tz: ('start', 'end'))
        )

    def test_calls(self, mocker, user_timezone, day, event, format_start_end_mock):
        EventListScenario.format_event(event, day, user_timezone)
        format_start_end_mock.assert_called_once_with(event.start_ts, event.end_ts, day, user_timezone)

    def test_result(self, event, day, user_timezone):
        assert EventListScenario.format_event(event, day, user_timezone) == f'С start до end "{event.name}".'


def test_events_list_result(setup_user, user_timezone, create_event_entity):
    day = date(2020, 4, 1)
    events = [
        create_event_entity(user_timezone.localize(datetime(2020, 3, 29, 10, 0))),
        create_event_entity(user_timezone.localize(datetime(2020, 3, 29, 17, 30))),
    ]
    scenario = EventListScenario(day=day, events=events)
    assert_that(
        scenario._events_list_result(),
        has_properties({
            'response': has_properties({
                'text': '\n'.join([
                    EventListScenario.format_event(event, day, user_timezone)
                    for event in events
                ]),
                'buttons': contains(has_properties({
                    'title': 'Открыть календарь',
                    'uri': is_(str),
                })),
                'expected_frames': [FrameName.DELETE_EVENT, FrameName.RESCHEDULE_EVENT],
                'contains_sensitive_data': True,
                'suggests': contains(
                    all_of(
                        is_(TypeTextButton),
                        has_properties({
                            'title': 'отмени',
                            'text': 'отмени',
                        }),
                    ),
                    all_of(
                        is_(TypeTextButton),
                        has_properties({
                            'title': 'перенеси',
                            'text': 'перенеси',
                        }),
                    ),
                ),
            }),
        })
    )


class TestFetchEvents:
    @pytest.fixture
    def day(self):
        return date(2020, 4, 1)

    @pytest.fixture
    def event_count(self):
        return 0

    @pytest.fixture
    def events(self, create_event_entity, day, event_count):
        return [
            create_event_entity(datetime(day.year, day.month, day.day, i))
            for i in range(event_count)
        ]

    @pytest.fixture
    def single_event(self):
        return None

    @pytest.fixture(autouse=True)
    def get_day_mock(self, mocker, day):
        mocker.patch.object(EventListScenario, '_get_day', mocker.Mock(return_value=day))

    @pytest.fixture(autouse=True)
    def get_events_mock(self, mocker, events):
        async def dummy_get_events(*args, **kwargs):
            for event in events:
                yield event

        mock = mocker.patch('mail.ciao.ciao.interactions.calendar.base.BaseCalendarClient.get_events')
        mock.side_effect = dummy_get_events
        return mock

    @pytest.fixture
    def returned_func(self, setup_user, single_event):
        async def _inner():
            slots = {} if single_event is None else {
                'event_list_single_event': single_event,
            }
            scenario = EventListScenario(slots=slots)
            await scenario._fetch_events()
            return scenario

        return _inner

    def test_get_events_call(self, user, day, get_events_mock, returned):
        from_ = user.timezone.localize(datetime.combine(day, datetime.min.time()))
        to_ = from_ + timedelta(days=1)
        get_events_mock.assert_called_once_with(
            uid=user.uid,
            user_ticket=user.user_ticket,
            from_datetime=from_,
            to_datetime=to_,
        )

    def test_sets_day(self, day, returned):
        assert returned._day == day

    @pytest.mark.parametrize('single_event,event_count', (
        (None, 2),
    ))
    def test_sets_events_all(self, events, returned):
        assert returned._events == events

    @pytest.mark.parametrize('single_event,event_count', (
        (EventListSingleEvent.FIRST, 0),
        (EventListSingleEvent.FIRST, 1),
        (EventListSingleEvent.FIRST, 2),
    ))
    def test_sets_events_first(self, events, returned):
        assert returned._events == events[:1]

    class TestNextEvent:
        @pytest.fixture
        def _now(self):
            return datetime(2020, 4, 1, 10)

        @pytest.fixture(autouse=True)
        def now(self, mocker, user_timezone, _now):
            now = user_timezone.localize(_now)
            mock = mocker.patch('mail.ciao.ciao.core.scenarios.event_list.datetime')
            mock.now.side_effect = lambda tz: now.astimezone(tz)
            mock.combine = datetime.combine
            mock.min = datetime.min
            return now

        @pytest.fixture
        def is_today(self):
            return True

        @pytest.fixture(autouse=True)
        def is_today_mock(self, mocker, is_today):
            mocker.patch(
                'mail.ciao.ciao.core.scenarios.event_list.is_today',
                mocker.Mock(return_value=is_today),
            )

        @pytest.fixture
        def single_event(self):
            return EventListSingleEvent.NEXT

        @pytest.fixture
        def events(self, user_timezone, day, create_event_entity):
            return [
                create_event_entity(user_timezone.localize(datetime.combine(day, time_)))
                for time_ in (
                    time(10, 30),
                    time(12, 0),
                    time(17, 30),
                )
            ]

        @pytest.mark.asyncio
        @pytest.mark.parametrize('is_today', (False,))
        async def test_next_event_fails_for_not_today(self, returned_func):
            with pytest.raises(CoreIrrelevantScenarioError):
                await returned_func()

        @pytest.mark.parametrize('day', (date(2020, 4, 1),))
        @pytest.mark.parametrize('_now,event_index', (
            (datetime(2020, 4, 1, 10, 0), 0),
            (datetime(2020, 4, 1, 13, 0), 2),
            (datetime(2020, 4, 1, 20, 0), None),
        ))
        def test_next_event_sets_events_next(self, now, events, returned, event_index):
            expected_events = []
            if event_index is not None:
                expected_events = [events[event_index]]
            assert returned._events == expected_events


class DayMode(Enum):
    TODAY = 'today'
    TODAY_ENUM = 'today_enum'
    TOMORROW = 'tomorrow'
    TOMORROW_ENUM = 'tomorrow_enum'
    OTHER = 'other'
    OTHER_DIFFERENT_YEAR = 'other_different_year'


@pytest.mark.parametrize('day_mode', (
    pytest.param(None, id='today_filled'),
    pytest.param(DayMode.TODAY, id='today_passed'),
    pytest.param(DayMode.TODAY_ENUM, id='today_enum_passed'),
    pytest.param(DayMode.TOMORROW, id='tomorrow_passed'),
    pytest.param(DayMode.TOMORROW_ENUM, id='tomorrow_enum_passed'),
    pytest.param(DayMode.OTHER, id='other_passed'),
    pytest.param(DayMode.OTHER_DIFFERENT_YEAR, id='other_different_year_passed'),
))
class TestHandleOverview:
    @pytest.fixture(autouse=True)
    def now(self, mocker, user_timezone):
        now = user_timezone.localize(datetime(2020, 2, 17, 15, 14, 30))
        mock = mocker.Mock(return_value=now.date())
        mocker.patch('mail.ciao.ciao.utils.datetime.timezone_today', mock)
        mocker.patch('mail.ciao.ciao.core.scenarios.event_list.timezone_today', mock)
        return now

    @pytest.fixture
    def events(self):
        return []

    @pytest.fixture
    def event_date(self, day_mode, now):
        dates = {
            None: now,
            DayMode.TODAY: now,
            DayMode.TODAY_ENUM: now,
            DayMode.TOMORROW: now + timedelta(days=1),
            DayMode.TOMORROW_ENUM: now + timedelta(days=1),
            DayMode.OTHER: now - timedelta(days=3),
            DayMode.OTHER_DIFFERENT_YEAR: now - relativedelta(years=3),
        }
        return dates[day_mode]

    @pytest.fixture
    def day_str(self, day, day_mode):
        return {
            None: 'Сегодня',
            DayMode.TODAY: 'Сегодня',
            DayMode.TODAY_ENUM: 'Сегодня',
            DayMode.TOMORROW: 'Завтра',
            DayMode.TOMORROW_ENUM: 'Завтра',
            DayMode.OTHER: f'{day.day} {format_month(day.month, genitive=True)}',
            DayMode.OTHER_DIFFERENT_YEAR: f'{day.day} {format_month(day.month, genitive=True)} {day.year} года',
        }[day_mode]

    @pytest.fixture(autouse=True)
    def fetch_events_mock(self, mocker, day, events):
        async def dummy_fetch_events(self, *args, **kwargs):
            self._day = day
            self._events = events

        return mocker.patch.object(
            EventListScenario,
            '_fetch_events',
            dummy_fetch_events,
        )

    @pytest.fixture
    def day(self, event_date):
        return event_date.date()

    @pytest.fixture
    def slots(self, day, day_mode):
        if day_mode is None:
            return {}
        elif day_mode == DayMode.TODAY_ENUM:
            return {'event_list_day': DateEnum.TODAY}
        elif day_mode == DayMode.TOMORROW_ENUM:
            return {'event_list_day': DateEnum.TOMORROW}
        return {'event_list_day': UserDate(day=day.day, month=day.month, year=day.year)}

    @pytest.fixture
    def returned_func(self, slots, setup_user):
        async def _inner():
            scenario = EventListScenario(slots=slots)
            return scenario, await scenario._handle_overview()

        return _inner

    @pytest.fixture
    def returned_scenario(self, returned):
        return returned[0]

    @pytest.fixture
    def returned_result(self, returned):
        return returned[1]

    class TestNoEvents:
        @pytest.fixture
        def events(self):
            return []

        def test_no_events__result(self, day_str, returned_result):
            assert_that(
                returned_result,
                has_properties({
                    'response': has_properties({
                        'text': f'{day_str} событий нет.',
                        'speech': f'{day_str} событий нет.',
                        'contains_sensitive_data': True,
                        'buttons': contains(has_properties({
                            'title': 'Открыть календарь',
                            'uri': is_(str),
                        })),
                    }),
                    'value': None,
                })
            )

    class TestSingleEvent:
        @pytest.fixture
        def events(self, event_date, create_event_entity):
            return [create_event_entity(event_date)]

        def test_single_event__result(self, day_str, user, events, returned_result):
            event = events[0]
            start_time_str = event.start_ts.astimezone(user.timezone).strftime('%H:%M')
            end_time_str = event.end_ts.astimezone(user.timezone).strftime('%H:%M')

            assert_that(
                returned_result,
                has_properties({
                    'response': has_properties({
                        'text': f'С {start_time_str} до {end_time_str} "{event.name}".',
                        'speech': f'С {start_time_str} до {end_time_str} "{event.name}".',
                        'requested_slot': None,
                        'contains_sensitive_data': True,
                        'buttons': contains(has_properties({
                            'title': 'Открыть календарь',
                            'uri': is_(str),
                        })),
                        'expected_frames': [FrameName.DELETE_EVENT, FrameName.RESCHEDULE_EVENT],
                    }),
                }),
            )

        def test_single_event__params(self, day, events, returned_scenario):
            assert returned_scenario.get_params() == {
                'state': EventListScenarioState.AWAITING_ACTION,
                'events': events,
                'day': day,
                'found_event': None,
            }

    class TestMultipleEvents:
        @pytest.fixture
        def event_count(self):
            return 3

        @pytest.fixture
        def events(self, user, create_event_entity, event_date, event_count):
            return [
                create_event_entity(
                    start=user.timezone.localize(
                        datetime.combine(event_date, datetime.min.time()) + timedelta(hours=i + 1)
                    )
                )
                for i in range(event_count)
            ][::-1]

        @pytest.mark.parametrize('event_count,event_word', (
            (3, 'события'),
            (5, 'событий'),
        ))
        def test_multiple_events__result(self, user, day_str, events, returned_result, event_word):
            start_time_str = min(events, key=lambda e: e.start_ts).start_ts.astimezone(user.timezone).strftime('%H:%M')
            end_time_str = max(events, key=lambda e: e.end_ts).end_ts.astimezone(user.timezone).strftime('%H:%M')
            assert_that(
                returned_result,
                has_properties({
                    'response': has_properties({
                        'text': (
                            f'{day_str} у вас {len(events)} {event_word} с '
                            f'{start_time_str} до {end_time_str}. Прочитать подробнее?'
                        ),
                        'speech': (
                            f'{day_str} у вас #neu {len(events)} {event_word} с '
                            f'{start_time_str} до {end_time_str}. Прочитать подробнее?'
                        ),
                        'requested_slot': ('event_list_yes_no_answer', YesNo),
                        'contains_sensitive_data': True,
                    }),
                    'value': MissingType.MISSING,
                })
            )

        def test_multiple_events__params(self, day, events, returned_scenario):
            assert returned_scenario.get_params() == {
                'state': EventListScenarioState.AWAITING_YES_NO,
                'events': events,
                'day': day,
                'found_event': None,
            }


class TestHandleYesNoAnswer:
    @pytest.fixture
    def day(self):
        return date(2020, 3, 27)

    @pytest.fixture
    def events(self, create_event_entity):
        return [create_event_entity() for _ in range(3)]

    @pytest.fixture
    def yes_no_answer(self):
        return YesNo.YES

    @pytest.fixture
    def slots(self, yes_no_answer):
        return {'event_list_yes_no_answer': yes_no_answer}

    @pytest.fixture
    def scenario(self, day, events, slots):
        return EventListScenario(
            events=events,
            slots=slots,
            day=day,
        )

    @pytest.fixture
    def returned_func(self, setup_user, scenario):
        return scenario._handle_yes_no_answer

    @pytest.mark.asyncio
    @pytest.mark.parametrize('slots', (None, {}))
    async def test_requires_yes_no_answer(self, returned_func, analytics):
        with pytest.raises(CoreIrrelevantScenarioError):
            await returned_func()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('events', ([],))
    async def test_asserts_events_present(self, returned_func):
        with pytest.raises(AssertionError):
            await returned_func()

    @pytest.mark.parametrize('yes_no_answer', (YesNo.YES,))
    def test_yes_result(self, user, day, events, returned):
        formatted_events = '\n'.join([
            EventListScenario.format_event(event, day, user.timezone)
            for event in events
        ])
        assert_that(
            returned,
            has_properties({
                'response': has_properties({
                    'text': formatted_events,
                    'speech': formatted_events,
                    'contains_sensitive_data': True,
                    'buttons': contains(has_properties({
                        'title': 'Открыть календарь',
                        'uri': is_(str),
                    })),
                    'expected_frames': [FrameName.DELETE_EVENT, FrameName.RESCHEDULE_EVENT],
                }),
            })
        )

    @pytest.mark.parametrize('yes_no_answer', (YesNo.YES,))
    def test_yes_state(self, scenario, returned):
        assert scenario.get_params()['state'] == EventListScenarioState.AWAITING_ACTION

    @pytest.mark.parametrize('yes_no_answer', (YesNo.NO,))
    def test_no_result(self, returned):
        assert_that(
            returned,
            has_properties({
                'response': has_properties({'text': None, 'speech': None, 'contains_sensitive_data': False}),
                'value': None,
            })
        )


class TestHandleAwaitingAction:
    @pytest.fixture
    def events(self, create_event_entity):
        return [create_event_entity()]

    @pytest.fixture
    def slots(self, mocker):
        return mocker.Mock()

    @pytest.fixture
    def scenario(self, slots, frame_name, events):
        return EventListScenario(slots=slots, frame_name=frame_name, events=events)

    @pytest.fixture
    def returned_func(self, scenario):
        return scenario._handle_awaiting_action

    @pytest.mark.asyncio
    @pytest.mark.parametrize('frame_name', (object(),))
    async def test_unexpected_frame(self, returned_func):
        with pytest.raises(CoreIrrelevantScenarioError):
            await returned_func()

    class TestDelete:
        @pytest.fixture
        def frame_name(self):
            return FrameName.DELETE_EVENT

        def test_delete__returned(self, slots, events, returned):
            assert_that(
                returned,
                has_properties({
                    'call': contains(
                        all_of(
                            is_(FindEventScenario),
                            has_properties({
                                '_slots': slots,
                                '_events': events,
                            }),
                        ),
                        'find_result',
                    ),
                })
            )

        def test_delete__state(self, scenario, returned):
            assert scenario.get_params()['state'] == EventListScenarioState.DELETION

    class TestReschedule:
        @pytest.fixture
        def frame_name(self):
            return FrameName.RESCHEDULE_EVENT

        def test_reschedule__returned(self, slots, events, returned):
            assert_that(
                returned,
                has_properties({
                    'call': contains(
                        all_of(
                            is_(FindEventScenario),
                            has_properties({
                                '_slots': slots,
                                '_events': events,
                            })
                        ),
                        'find_result',
                    ),
                })
            )

        def test_reschedule__state(self, scenario, returned):
            assert scenario.get_params()['state'] == EventListScenarioState.RESCHEDULING


@pytest.mark.asyncio
class TestHandleDeletion:
    async def test_event_not_found_result(self):
        assert_that(
            await EventListScenario(find_result=ScenarioResult(value=None))._handle_deletion(),
            has_properties({
                'response': has_properties({
                    'text': 'Не получилось найти событие.',
                }),
                'value': None
            })
        )

    async def test_delete_call(self, create_event_entity):
        frame_name = object()
        slots = object()
        event = create_event_entity()
        assert_that(
            await EventListScenario(
                frame_name=frame_name,
                slots=slots,
                find_result=ScenarioResult(value=event),
            )._handle_deletion(),
            has_properties({
                'call': contains(
                    all_of(
                        is_(DeleteEventScenario),
                        has_properties({
                            '_frame_name': frame_name,
                            '_slots': slots,
                            '_event': event,
                        }),
                    ),
                    'delete_result',
                ),
            }),
        )

    async def test_delete_result(self):
        response = object()
        assert_that(
            await EventListScenario(delete_result=ScenarioResult(response=response))._handle_deletion(),
            has_properties({
                'response': response,
                'value': None,
            }),

        )


@pytest.mark.asyncio
class TestHandleRescheduling:
    async def test_not_found(self):
        assert_that(
            await EventListScenario(found_event=None)._handle_rescheduling(),
            has_properties({
                'response': has_properties({
                    'text': 'Не получилось найти событие.',
                }),
                'value': None,
            })
        )

    async def test_reschedule_call(self):
        event = object()
        assert_that(
            await EventListScenario(found_event=event)._handle_rescheduling(),
            has_properties({
                'call': contains(
                    all_of(
                        is_(RescheduleEventScenario),
                        has_properties({'_event': event})
                    ),
                    'reschedule_result',
                )
            })
        )

    async def test_reschedule_result_proxy(self, mocker):
        reschedule_result = mocker.Mock()
        assert_that(
            await EventListScenario(reschedule_result=reschedule_result)._handle_rescheduling(),
            has_properties({
                'response': reschedule_result.response,
                'value': None,
            })
        )


@pytest.mark.asyncio
class TestHandle:
    @pytest.fixture(autouse=True)
    def overview_result(self, mocker):
        result = ScenarioResult(response=ScenarioResponse())
        mocker.patch.object(
            EventListScenario,
            '_handle_overview',
            mocker.Mock(side_effect=dummy_async_function(result)),
        )
        return result

    @pytest.fixture(autouse=True)
    def yes_no_answer_result(self, mocker):
        result = ScenarioResult(response=ScenarioResponse())
        mocker.patch.object(
            EventListScenario,
            '_handle_yes_no_answer',
            mocker.Mock(side_effect=dummy_async_function(result)),
        )
        return result

    @pytest.fixture(autouse=True)
    def awaiting_action_result(self, mocker):
        result = ScenarioResult(response=ScenarioResponse())
        mocker.patch.object(
            EventListScenario,
            '_handle_awaiting_action',
            mocker.Mock(side_effect=dummy_async_function(result)),
        )
        return result

    @pytest.fixture(autouse=True)
    def deletion_result(self, mocker):
        result = ScenarioResult(response=ScenarioResponse())
        mocker.patch.object(
            EventListScenario,
            '_handle_deletion',
            mocker.Mock(side_effect=dummy_async_function(result)),
        )
        return result

    @pytest.fixture(autouse=True)
    def rescheduling_result(self, mocker):
        result = ScenarioResult(response=ScenarioResponse())
        mocker.patch.object(
            EventListScenario,
            '_handle_rescheduling',
            mocker.Mock(side_effect=dummy_async_function(result)),
        )
        return result

    async def test_overview_call(self):
        await EventListScenario(state=EventListScenarioState.OVERVIEW).handle()
        EventListScenario._handle_overview.assert_called_once()

    async def test_overview_result(self, overview_result):
        assert await EventListScenario(state=EventListScenarioState.OVERVIEW).handle() == overview_result

    async def test_yes_no_answer_call(self):
        await EventListScenario(state=EventListScenarioState.AWAITING_YES_NO).handle()
        EventListScenario._handle_yes_no_answer.assert_called_once()

    async def test_yes_no_answer_result(self, yes_no_answer_result):
        assert await EventListScenario(state=EventListScenarioState.AWAITING_YES_NO).handle() == yes_no_answer_result

    async def test_awaiting_action(self, mocker, awaiting_action_result):
        result = await EventListScenario(state=EventListScenarioState.AWAITING_ACTION).handle()
        assert all((
            EventListScenario._handle_awaiting_action.mock_calls == [mocker.call()],
            result == awaiting_action_result,
        ))

    async def test_handle_deletion(self, mocker, deletion_result):
        result = await EventListScenario(state=EventListScenarioState.DELETION).handle()
        assert all((
            EventListScenario._handle_deletion.mock_calls == [mocker.call()],
            result == deletion_result,
        ))

    async def test_handle_rescheduling(self, mocker, rescheduling_result):
        result = await EventListScenario(state=EventListScenarioState.RESCHEDULING).handle()
        assert all((
            EventListScenario._handle_rescheduling.mock_calls == [mocker.call()],
            result == rescheduling_result,
        ))

    async def test_returns_irrelevant_response_for_unknown_state(self, analytics):
        with pytest.raises(CoreIrrelevantScenarioError) as exc_info:
            await EventListScenario(state=object()).handle()
        assert exc_info.value.analytics == analytics
