from datetime import date, datetime
from unittest.mock import seal

import pytest

from hamcrest import assert_that, has_properties

from mail.ciao.ciao.core.entities.enums import FrameName, SysSlotType, YesNo
from mail.ciao.ciao.core.scenarios.reschedule_event import RescheduleEventScenario, RescheduleEventScenarioState
from mail.ciao.ciao.tests.utils import dummy_async_function
from mail.ciao.ciao.utils.datetime import UserDate, UserTime
from mail.ciao.ciao.utils.format import format_date, format_datetime, format_time


@pytest.fixture
def event_start():
    return None


@pytest.fixture
def all_day():
    return False


@pytest.fixture
def event(user, create_event_entity, event_start, all_day):
    return create_event_entity(
        start=user.timezone.localize(event_start) if event_start else None,
        all_day=all_day,
    )


@pytest.mark.parametrize('kwargs,expected', (
    pytest.param(
        {},
        {
            'state': RescheduleEventScenarioState.INITIAL,
            'new_date_start': None,
            'new_time_start': None,
        },
        id='default',
    ),
    pytest.param(
        {
            'slots': {
                'reschedule_event_new_date_start': UserDate(day=3),
                'reschedule_event_new_time_start': UserTime(hour=3),
            },
        },
        {
            'state': RescheduleEventScenarioState.INITIAL,
            'new_date_start': UserDate(day=3),
            'new_time_start': UserTime(hour=3),
        },
        id='parses_slots',
    ),
    pytest.param(
        {
            'state': RescheduleEventScenarioState.FILLING_SLOTS,
            'new_date_start': UserDate(day=3),
            'new_time_start': UserTime(hour=3),
        },
        {
            'state': RescheduleEventScenarioState.INITIAL,
            'new_date_start': UserDate(day=3),
            'new_time_start': UserTime(hour=3),
        },
        id='uses_args',
    ),
))
def test_params(kwargs, event, expected):
    RescheduleEventScenario(event=event, **kwargs).get_params() == {'event': event, **expected}


class TestGetNewDatetimeStart:
    @pytest.fixture(autouse=True)
    def datetime_mock(self, mocker, now):
        dt = mocker.patch('mail.ciao.ciao.core.scenarios.reschedule_event.datetime')
        dt.now.side_effect = lambda tz: tz.localize(now)
        dt.combine = datetime.combine
        dt.min = datetime.min
        mocker.patch(
            'mail.ciao.ciao.core.scenarios.reschedule_event.timezone_today',
            mocker.Mock(return_value=now.date()),
        )
        seal(dt)

    @pytest.fixture
    def returned(self, setup_user, event, new_date_start, new_time_start):
        return RescheduleEventScenario(
            event=event,
            new_date_start=new_date_start,
            new_time_start=new_time_start,
        )._get_new_datetime_start()

    @pytest.mark.parametrize('now,new_date_start,new_time_start,all_day,expected', (
        pytest.param(
            datetime(2020, 3, 21),
            UserDate(day=22),
            UserTime(hour=10),
            False,
            datetime(2020, 3, 22, 10),
            id='absolute',
        ),
        pytest.param(
            datetime(2020, 3, 21),
            UserDate(day=-1, relative=True),
            UserTime(hour=10),
            False,
            datetime(2020, 3, 20, 10),
            id='relative_date',
        ),
        pytest.param(
            datetime(2020, 3, 21, 10),
            UserDate(year=2020, month=3, day=21),
            UserTime(relative=True, hour=1),
            False,
            datetime(2020, 3, 21, 11),
            id='relative_time'
        ),
        pytest.param(
            datetime(2020, 3, 21, 10),
            UserDate(year=2020, month=3, day=21),
            None,
            True,
            datetime(2020, 3, 21, 0),
            id='all_day'
        ),
    ))
    def test_result(self, user, expected, returned):
        assert returned == user.timezone.localize(expected)


@pytest.mark.asyncio
class TestHandleFillingSlots:
    async def test_new_date_start_request(self, event):
        assert_that(
            await RescheduleEventScenario(event=event)._handle_filling_slots(),
            has_properties({
                'response': has_properties({
                    'text': 'На какой день перенести событие?',
                    'requested_slot': ('reschedule_event_new_date_start', SysSlotType.DATE),
                    'frame_name': FrameName.RESCHEDULE_EVENT,
                    'expected_frames': [FrameName.RESCHEDULE_EVENT],
                }),
            }),
        )

    async def test_new_time_start_request(self, event):
        assert_that(
            await RescheduleEventScenario(
                event=event,
                new_date_start=object(),
            )._handle_filling_slots(),
            has_properties({
                'response': has_properties({
                    'text': 'На какое время перенести событие?',
                    'requested_slot': ('reschedule_event_new_time_start', SysSlotType.TIME),
                    'frame_name': FrameName.RESCHEDULE_EVENT,
                    'expected_frames': [FrameName.RESCHEDULE_EVENT],
                }),
            }),
        )

    class TestConfirmation:
        @pytest.fixture
        def today(self):
            return date(2020, 4, 20)

        @pytest.fixture(autouse=True)
        def timezone_today_mock(self, mocker, today):
            mocker.patch(
                'mail.ciao.ciao.core.scenarios.reschedule_event.timezone_today',
                mocker.Mock(return_value=today),
            )

        @pytest.fixture
        def new_datetime_start(self, user):
            return user.timezone.localize(datetime(2020, 4, 15, 23, 18))

        @pytest.fixture(autouse=True)
        def get_new_datetime_start_mock(self, mocker, new_datetime_start):
            return mocker.patch.object(
                RescheduleEventScenario,
                '_get_new_datetime_start',
                mocker.Mock(return_value=new_datetime_start),
            )

        @pytest.fixture
        def scenario(self, event):
            return RescheduleEventScenario(
                event=event,
                new_date_start=object(),
                new_time_start=object(),
            )

        @pytest.fixture
        def returned_func(self, setup_user, scenario):
            return scenario._handle_filling_slots

        @pytest.mark.parametrize('today,event_start,new_datetime_start,all_day,from_func,to_text', (
            pytest.param(
                date(2020, 4, 20),
                datetime(2020, 4, 20, 23),
                datetime(2020, 4, 20, 19),
                False,
                lambda dt: format_time(dt),
                '19:00',
                id='same_day',
            ),
            pytest.param(
                date(2020, 4, 20),
                datetime(2020, 4, 20, 23),
                datetime(2020, 4, 21, 19),
                False,
                lambda dt: format_datetime(dt, today=date(2020, 4, 20)),
                '21 апреля 19:00',
                id='different_day',
            ),
            pytest.param(
                date(2020, 4, 20),
                datetime(2020, 4, 20, 23),
                datetime(2021, 4, 20, 19),
                False,
                lambda dt: format_datetime(dt, today=date(2020, 4, 20)),
                '20 апреля 2021 года 19:00',
                id='different_year',
            ),
            pytest.param(
                date(2020, 4, 20),
                datetime(2020, 4, 20, 0),
                datetime(2020, 4, 21, 0),
                True,
                lambda dt: format_date(dt, today=date(2020, 4, 20)),
                '21 апреля',
                id='all_day_same_year',
            ),
            pytest.param(
                date(2020, 4, 20),
                datetime(2020, 4, 20, 0),
                datetime(2021, 4, 21, 0),
                True,
                lambda dt: format_date(dt, today=date(2020, 4, 20)),
                '21 апреля 2021 года',
                id='all_day_different_year',
            ),
        ))
        def test_confirmation__result(self, user, event, returned, from_func, to_text):
            from_text = from_func(event.start_ts.astimezone(user.timezone))
            assert_that(
                returned,
                has_properties({
                    'response': has_properties({
                        'text': f'Перенести событие "{event.name}" с {from_text} на {to_text}?',
                        'requested_slot': ('reschedule_event_confirmation', YesNo),
                        'frame_name': FrameName.RESCHEDULE_EVENT,
                        'expected_frames': [FrameName.RESCHEDULE_EVENT],
                    }),
                })
            )

        def test_confirmation__state(self, scenario, returned):
            assert scenario.get_params()['state'] == RescheduleEventScenarioState.AWAITING_CONFIRMATION


class TestHandleAwaitingConfirmation:
    @pytest.fixture
    def new_datetime_start(self, user):
        return user.timezone.localize(datetime(2020, 4, 20, 23, 38))

    @pytest.fixture(autouse=True)
    def get_new_datetime_start_mock(self, mocker, new_datetime_start):
        return mocker.patch.object(
            RescheduleEventScenario,
            '_get_new_datetime_start',
            mocker.Mock(return_value=new_datetime_start),
        )

    @pytest.fixture(autouse=True)
    def update_event_mock(self, mocker):
        return mocker.patch(
            'mail.ciao.ciao.interactions.calendar.base.BaseCalendarClient.update_event',
            mocker.Mock(side_effect=dummy_async_function())
        )

    @pytest.fixture
    def commit(self):
        return False

    @pytest.fixture
    async def returned_func(self, setup_user, event, confirmation, commit):
        return RescheduleEventScenario(
            slots={
                'reschedule_event_confirmation': confirmation,
            },
            event=event,
            commit=commit,
        )._handle_awaiting_confirmation

    class TestNo:
        @pytest.fixture
        def confirmation(self):
            return YesNo.NO

        def test_no__result(self, returned):
            assert_that(
                returned,
                has_properties({
                    'response': has_properties({
                        'text': 'Хорошо.',
                    }),
                    'value': None,
                }),
            )

        def test_no__calendar_not_called(self, returned, update_event_mock):
            update_event_mock.assert_not_called()

    class TestYes:
        @pytest.fixture
        def confirmation(self):
            return YesNo.YES

        @pytest.mark.parametrize('commit', (False, True))
        def test_yes__result(self, returned):
            assert_that(
                returned,
                has_properties({
                    'response': has_properties({
                        'text': 'Событие перенесено.',
                        'commit': True,
                    }),
                    'value': None,
                })
            )

        @pytest.mark.parametrize('commit', (False,))
        def test_yes__calendar_not_called_without_commit(self, returned, update_event_mock):
            update_event_mock.assert_not_called()

        @pytest.mark.parametrize('commit', (True,))
        @pytest.mark.parametrize('all_day', (True, False))
        def test_yes__calendar_called_with_commit(self, user, event, new_datetime_start, all_day, returned,
                                                  update_event_mock):
            update_event_mock.assert_called_once_with(
                uid=user.uid,
                user_ticket=user.user_ticket,
                event_id=event.event_id,
                start_ts=new_datetime_start,
                end_ts=new_datetime_start + (event.end_ts - event.start_ts),
                all_day=all_day,
            )


class TestHandle:
    pass
