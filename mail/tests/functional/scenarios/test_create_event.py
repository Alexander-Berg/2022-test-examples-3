import re
from datetime import datetime
from unittest.mock import seal

import pytest
from dateutil.relativedelta import relativedelta

from hamcrest import assert_that, contains, has_entries, has_properties, is_

from mail.ciao.ciao.core.entities.analytcs import Action, Intent
from mail.ciao.ciao.core.entities.enums import FrameName
from mail.ciao.ciao.core.exceptions import CoreFailError

from .utils import assert_requests_slot, create_frame

NOW = datetime(2020, 2, 26, 10, 35, 47)


@pytest.fixture(autouse=True)
def datetime_mock(mocker):
    mock = mocker.patch('mail.ciao.ciao.core.scenarios.create_event.datetime')
    mock.now.side_effect = lambda tz: tz.localize(NOW)
    mock.combine = datetime.combine
    mock.min = datetime.min
    seal(mock)  # Making sure we don't forget to proxy all the rest attributes
    return mock


def date_start_slot(value):
    return ('create_event_date_start', 'sys.date', value)


def time_start_slot(value):
    return ('create_event_time_start', 'sys.time', value)


def date_end_slot(value):
    return ('create_event_date_end', 'sys.date', value)


def time_end_slot(value):
    return ('create_event_time_end', 'sys.time', value)


def duration_slot(value):
    return ('create_event_duration', 'sys.datetime_range', value)


def event_name_slot(value):
    return ('create_event_event_name', 'string', value)


def confirmation_slot(value):
    return ('create_event_confirmation', 'yes_no', value)


DATE_START_SLOT = date_start_slot('{"years":2020,"months":2,"days":25}')
TIME_START_SLOT = time_start_slot('{"hours":18,"minutes":27}')
TIME_START_SLOT_RELATIVE = time_start_slot('{"minutes":30,"minutes_relative":true}')
TIME_END_SLOT = time_end_slot('{"hours":19,"minutes":27}')
DURATION_SLOT = duration_slot(
    '{"start":{"minutes":0,"minutes_relative":true},"end":{"minutes":30,"minutes_relative":true}}',
)
EVENT_NAME_SLOT = event_name_slot('Test event name')
CONFIRMATION_SLOT_CONFIRMED = confirmation_slot('yes')
CONFIRMATION_SLOT_DENIED = confirmation_slot('no')
ALL_DAY_SLOT = ('create_event_all_day', 'custom.create_event_all_day', 'all_day')


def create_starter_frame(slots=None):
    return create_frame(FrameName.CREATE_EVENT.value, slots)


def create_subsequent_frame(slots=None):
    return create_frame(FrameName.CREATE_EVENT_SUBSEQUENT.value, slots)


@pytest.fixture
def show_event_id(randn):
    return randn()


@pytest.fixture(autouse=True)
def create_event_mock(aioresponses_mocker, settings, show_event_id):
    return aioresponses_mocker.post(
        re.compile(rf'^{settings.PUBLIC_CALENDAR_API_URL}/internal/create-event.*$'),
        payload={
            "showEventId": show_event_id
        },
    )


@pytest.fixture
def returned_func(run_scenario, frames):
    async def _inner():
        return await run_scenario(frames)

    return _inner


@pytest.mark.parametrize('frames', (
    pytest.param([create_starter_frame()], id='no_slots'),
))
def test_requests_date_start(returned):
    assert_requests_slot(
        response=returned,
        frame=FrameName.CREATE_EVENT_SUBSEQUENT.value,
        slot_name='create_event_date_start',
        accepted_types=['sys.date'],
        contains_sensitive_data=False,
        analytics_info={
            'Intent': Intent.CREATE_EVENT.value,
            'Actions': contains(
                has_properties({'Id': Action.CREATE_EVENT_START_DATE.value})
            )
        }
    )


@pytest.mark.parametrize('frames', (
    pytest.param([create_starter_frame([DATE_START_SLOT])], id='date_start_slot'),
))
def test_requests_time_start(returned):
    assert_requests_slot(
        response=returned,
        frame=FrameName.CREATE_EVENT_SUBSEQUENT.value,
        slot_name='create_event_time_start',
        accepted_types=['sys.time'],
        contains_sensitive_data=False,
        analytics_info={
            'Intent': Intent.CREATE_EVENT.value,
            'Actions': contains(
                has_properties({'Id': Action.CREATE_EVENT_START_TIME.value})
            )
        }
    )


@pytest.mark.parametrize('frames', (
    pytest.param(
        [create_starter_frame([DATE_START_SLOT, TIME_START_SLOT])],
        id='date_start_time_start_single_frame',
    ),
    pytest.param(
        [
            create_starter_frame([DATE_START_SLOT]),
            create_subsequent_frame([TIME_START_SLOT]),
        ],
        id='date_start_then_time_start',
    ),
    pytest.param(
        [
            create_starter_frame([]),
            create_subsequent_frame([DATE_START_SLOT, TIME_START_SLOT]),
        ],
        id='nothing_then_date_start_and_time_start',
    ),
    pytest.param(
        [
            create_starter_frame([TIME_START_SLOT_RELATIVE]),
        ],
        id='relative_time_allows_to_skip_date_start',
    ),
))
def test_requests_duration(returned):
    assert_requests_slot(
        response=returned,
        frame=FrameName.CREATE_EVENT_SUBSEQUENT.value,
        slot_name='create_event_duration',
        accepted_types=['sys.datetime_range'],
        contains_sensitive_data=False,
        analytics_info={
            'Intent': Intent.CREATE_EVENT.value,
            'Actions': contains(
                has_properties({'Id': Action.CREATE_EVENT_DURATION.value})
            )
        }
    )


@pytest.mark.parametrize('frames', (
    pytest.param(
        [
            # Поставь встречу на завтра на 2 часа дня на 30 минут
            create_starter_frame([DATE_START_SLOT, TIME_START_SLOT, DURATION_SLOT]),
        ],
        id='single_frame_duration',
    ),
    pytest.param(
        [
            # Поставь встречу на завтра с 3 до 5
            create_starter_frame([DATE_START_SLOT, TIME_START_SLOT, TIME_END_SLOT])
        ],
        id='single_frame_time_end',
    ),
    pytest.param(
        [
            create_starter_frame([]),  # Поставь встречу
            create_subsequent_frame([DATE_START_SLOT]),  # Завтра
            create_subsequent_frame([TIME_START_SLOT]),  # 2 часа дня
            create_subsequent_frame([DURATION_SLOT]),  # 30 минут
        ],
        id='mupliple_frames_duration',
    ),
    pytest.param(
        [
            # Поставь встречу с 3 до 5
            create_starter_frame([TIME_START_SLOT, TIME_END_SLOT]),
            create_subsequent_frame([DATE_START_SLOT]),  # Завтра
        ],
        id='mupliple_frames_time_end',
    ),
    pytest.param(
        [
            # Поставь встречу через 15 минут до 5
            create_starter_frame([TIME_START_SLOT_RELATIVE, TIME_END_SLOT]),
        ],
        id='relative_time_start_with_time_end',
    ),
    pytest.param(
        [
            # Поставь встречу через 15 минут на полчаса
            create_starter_frame([TIME_START_SLOT_RELATIVE, DURATION_SLOT]),
        ],
        id='single_frame_relative_time_start_with_duration'
    ),
    pytest.param(
        [
            # Поставь встречу через 15 минут
            create_starter_frame([TIME_START_SLOT_RELATIVE]),
            create_subsequent_frame([DURATION_SLOT]),
        ],
        id='multiple_frames_relative_start_with_duration',
    ),
    pytest.param(
        [
            # Поставь встречу сегодня на 12 на полчаса
            create_starter_frame([DATE_START_SLOT, TIME_START_SLOT, DURATION_SLOT]),
            create_subsequent_frame([EVENT_NAME_SLOT]),  # *вводит название*
            # Поставь встречу сегодня на 12 на полчаса
            create_starter_frame([DATE_START_SLOT, TIME_START_SLOT, DURATION_SLOT]),
        ],
        id='resets_event_name_after_restart'
    ),
))
def test_requests_event_name(returned):
    assert_requests_slot(
        response=returned,
        frame=FrameName.CREATE_EVENT_SUBSEQUENT.value,
        slot_name='create_event_event_name',
        accepted_types=['string'],
        contains_sensitive_data=False,
        analytics_info={
            'Intent': Intent.CREATE_EVENT.value,
            'Actions': contains(
                has_properties({'Id': Action.CREATE_EVENT_NAME.value})
            )
        }
    )


@pytest.mark.parametrize('frames', (
    pytest.param(
        [
            # Создай встречу планирование на завтра на 2 часа дня на 30 минут
            create_starter_frame([
                DATE_START_SLOT,
                TIME_START_SLOT,
                DURATION_SLOT,
                EVENT_NAME_SLOT,
            ])
        ],
        id='single_frame_duration',
    ),
    pytest.param(
        [
            # Создай встречу планирование на завтра с 3 до 5
            create_starter_frame([
                DATE_START_SLOT,
                TIME_START_SLOT,
                TIME_END_SLOT,
                EVENT_NAME_SLOT,
            ])
        ],
        id='single_frame_time_end',
    ),
    pytest.param(
        [
            create_starter_frame(),
            create_subsequent_frame([DATE_START_SLOT]),
            create_subsequent_frame([TIME_START_SLOT]),
            create_subsequent_frame([DURATION_SLOT]),
            create_subsequent_frame([EVENT_NAME_SLOT]),
        ],
        id='multiple_frames_duration',
    ),
))
def test_requests_confirmation(returned):
    assert_requests_slot(
        response=returned,
        frame=FrameName.CREATE_EVENT_SUBSEQUENT.value,
        slot_name='create_event_confirmation',
        accepted_types=['yes_no'],
        contains_sensitive_data=True,
        analytics_info={
            'Intent': Intent.CREATE_EVENT.value,
        }
    )


@pytest.mark.parametrize('all_day', (False, True))
class TestConfirmation:
    @pytest.fixture
    def frames(self, all_day, confirmation_slot):
        starter_frame_slots = [
            DATE_START_SLOT,
            TIME_START_SLOT,
            TIME_END_SLOT,
            EVENT_NAME_SLOT,
        ]
        if all_day:
            starter_frame_slots.append(ALL_DAY_SLOT)
        return [
            create_starter_frame(starter_frame_slots),
            create_subsequent_frame([confirmation_slot]),
        ]

    @pytest.mark.parametrize('confirmation_slot', (CONFIRMATION_SLOT_CONFIRMED,))
    def test_calendar_api_called_on_yes(self, user, create_event_mock, all_day, returned):
        assert_that(
            create_event_mock.call_args[1],
            has_entries({
                'headers': has_entries({
                    'X-Ya-Service-Ticket': is_(str),
                    'X-Ya-User-Ticket': user.user_ticket,
                }),
                'params': has_entries({'uid': user.uid}),
                'json': has_entries({
                    'startTs': is_(str),
                    'endTs': is_(str),
                    'name': EVENT_NAME_SLOT[2],
                    'type': 'user',
                    'isAllDay': all_day,
                }),
            })
        )

    @pytest.mark.parametrize('confirmation_slot', (CONFIRMATION_SLOT_CONFIRMED,))
    def test_analytics(self, returned):
        assert_that(returned.CommitCandidate.ResponseBody.AnalyticsInfo, has_properties({
            'Intent': Intent.CREATE_EVENT.value,
            'Actions': contains(
                has_properties({
                    'Id': Action.EVENT_CREATED.value,
                })
            )
        }))

    @pytest.mark.parametrize('confirmation_slot', (CONFIRMATION_SLOT_DENIED,))
    def test_calendar_api_not_called_on_no(self, create_event_mock, returned):
        create_event_mock.assert_not_called()

    @pytest.mark.parametrize('confirmation_slot', (CONFIRMATION_SLOT_CONFIRMED,))
    class TestErrorHandling:
        @pytest.fixture(autouse=True)
        def exception_side_effect(self, mocker):
            mocker.patch(
                'mail.ciao.ciao.interactions.calendar.base.BaseCalendarClient.create_event',
                mocker.Mock(side_effect=lambda: 1 / 0),
            )

        def test_error_handling__commit_returns_error(self, returned):
            assert_that(
                returned,
                has_properties({
                    'Error': has_properties({
                        'Message': str(CoreFailError),
                    }),
                }),
            )


class TestEventParams:
    @pytest.mark.parametrize('frames,start,end,name', (
        pytest.param(
            [
                create_starter_frame([
                    date_start_slot('{"years":2020,"months":2,"days":26}'),
                    time_start_slot('{"hours":13,"minutes":18}'),
                    duration_slot(
                        '{"start":{"minutes":0,"minutes_relative":true},"end":{"minutes":30,"minutes_relative":true}}'
                    ),
                    event_name_slot('test event'),
                ]),
                create_subsequent_frame([CONFIRMATION_SLOT_CONFIRMED]),
            ],
            datetime(2020, 2, 26, 13, 18),
            datetime(2020, 2, 26, 13, 18) + relativedelta(minutes=30),
            'test event',
            id='single_frame_duration',
        ),
        pytest.param(
            [
                create_starter_frame([
                    date_start_slot('{"years":2020,"months":2,"days":26}'),
                    time_start_slot('{"hours":13,"minutes":18}'),
                    time_end_slot('{"hours":14,"minutes":10}'),
                    event_name_slot('test event'),
                ]),
                create_subsequent_frame([CONFIRMATION_SLOT_CONFIRMED]),
            ],
            datetime(2020, 2, 26, 13, 18),
            datetime(2020, 2, 26, 14, 10),
            'test event',
            id='single_frame_time_end',
        ),
        pytest.param(
            [
                create_starter_frame(),
                create_subsequent_frame([date_start_slot('{"years":2020,"months":2,"days":26}')]),
                create_subsequent_frame([time_start_slot('{"hours":13,"minutes":18}')]),
                create_subsequent_frame([duration_slot(
                    '{"start":{"hours":0,"minutes_relative":true},"end":{"hours":2,"minutes_relative":true}}'
                )]),
                create_subsequent_frame([event_name_slot('test event')]),
                create_subsequent_frame([CONFIRMATION_SLOT_CONFIRMED]),
            ],
            datetime(2020, 2, 26, 13, 18),
            datetime(2020, 2, 26, 13, 18) + relativedelta(hours=2),
            'test event',
            id='multiple_frames_duration',
        ),
        pytest.param(
            [
                # Создай встречу test event 26 февряля 2020 года с 13:18 до 14:10
                create_starter_frame([
                    date_start_slot('{"years":2020,"months":2,"days":26}'),
                    time_start_slot('{"hours":13,"minutes":18}'),
                    time_end_slot('{"hours":14,"minutes":10}'),
                ]),
                create_subsequent_frame([event_name_slot('test event')]),
                create_subsequent_frame([CONFIRMATION_SLOT_CONFIRMED]),
            ],
            datetime(2020, 2, 26, 13, 18),
            datetime(2020, 2, 26, 14, 10),
            'test event',
            id='multiple_frames_time_end',
        ),
        pytest.param(
            [
                # Поставь встречу test event через 15 минут на полчаса
                create_starter_frame([
                    time_start_slot('{"minutes":15,"minutes_relative":true}'),
                    duration_slot(
                        '{"start":{"minutes":0,"minutes_relative":true},"end":{"minutes":30,"minutes_relative":true}}'
                    ),
                    event_name_slot('test event'),
                ]),
                create_subsequent_frame([CONFIRMATION_SLOT_CONFIRMED]),
            ],
            NOW + relativedelta(minutes=15),
            NOW + relativedelta(minutes=15) + relativedelta(minutes=30),
            'test event',
            id='single_frame_relative_time',
        ),
        pytest.param(
            [
                # Поставь встречу test event через 15 минут на полчаса
                create_starter_frame([
                    time_start_slot('{"minutes":15,"minutes_relative":true}'),
                ]),
                create_subsequent_frame([duration_slot(
                    '{"start":{"hours":0,"minutes_relative":true},"end":{"hours":2,"minutes_relative":true}}'
                )]),
                create_subsequent_frame([event_name_slot('test event')]),
                create_subsequent_frame([CONFIRMATION_SLOT_CONFIRMED]),
            ],
            NOW + relativedelta(minutes=15),
            NOW + relativedelta(minutes=15) + relativedelta(hours=2),
            'test event',
            id='multiple_frames_relative_time',
        ),
    ))
    def test_event_params(self, user, create_event_mock, returned, start, end, name):
        json_params = create_event_mock.call_args[1]['json']
        ts_format = '%Y-%m-%dT%H:%M:%S%z'
        start_param = datetime.strptime(json_params['startTs'], ts_format)
        end_param = datetime.strptime(json_params['endTs'], ts_format)
        name_param = json_params['name']
        analytics_info = returned.CommitCandidate.ResponseBody.AnalyticsInfo

        assert all((
            name_param == name.capitalize(),
            start_param == user.timezone.localize(start),
            end_param == user.timezone.localize(end),
            analytics_info.Intent == Intent.CREATE_EVENT.value,
            len(analytics_info.Actions) == 1,
            analytics_info.Actions[0].Id == Action.EVENT_CREATED.value,
        ))
