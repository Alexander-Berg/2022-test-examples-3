from datetime import date, datetime, time, timedelta
from unittest.mock import seal

import pytest
from dateutil.relativedelta import relativedelta

from sendr_utils import without_none

from hamcrest import assert_that, contains, has_entries, has_properties, is_

from mail.ciao.ciao.core.entities.analytcs import Action, Analytics, Intent
from mail.ciao.ciao.core.entities.enums import CreateEventAllDay, FrameName, SysSlotType, YesNo
from mail.ciao.ciao.core.exceptions import CoreIrrelevantScenarioError
from mail.ciao.ciao.core.scenarios.create_event import CreateEventScenario, CreateEventScenarioState, EventEndError
from mail.ciao.ciao.tests.utils import dummy_async_function
from mail.ciao.ciao.utils.datetime import Period, UserDate, UserTime, safe_localize
from mail.ciao.ciao.utils.format import format_datetime

NOW = datetime(2020, 2, 26, 11, 22)


@pytest.fixture
def analytics():
    return Analytics(Intent.CREATE_EVENT)


@pytest.fixture(autouse=True)
def datetime_mock(mocker):
    mock = mocker.patch('mail.ciao.ciao.core.scenarios.create_event.datetime')
    mock.now.side_effect = lambda tz: tz.localize(NOW)
    mock.combine = datetime.combine
    mock.min = datetime.min
    seal(mock)


@pytest.fixture
def frame_name():
    return None


@pytest.fixture
def slots():
    return None


@pytest.fixture
def commit():
    return False


@pytest.fixture
def scenario(frame_name, slots, commit, fields):
    return CreateEventScenario(frame_name, slots=slots, commit=commit, **fields)


@pytest.fixture
def event_start_end(user):
    start = datetime(2020, 2, 25, 12, 36)
    end = start + timedelta(hours=1)
    return user.timezone.localize(start), user.timezone.localize(end)


@pytest.mark.parametrize('contains_sensitive_data', (True, False, None))
def test_build_request(mocker, rands, contains_sensitive_data):
    text = rands()
    requested_slot = rands()
    assert_that(
        CreateEventScenario._build_request(**without_none(dict(
            text=text,
            requested_slot=requested_slot,
            contains_sensitive_data=contains_sensitive_data,
        ))),
        has_properties({
            'response': has_properties({
                'text': text,
                'speech': text,
                'frame_name': FrameName.CREATE_EVENT_SUBSEQUENT,
                'requested_slot': requested_slot,
                'contains_sensitive_data': bool(contains_sensitive_data),
            }),
        })
    )


@pytest.mark.parametrize('fields,slots', (
    pytest.param({}, {}, id='no_fields_and_slots'),
    pytest.param(
        {},
        {
            'create_event_date_start': UserDate(year=2020, month=2, day=25),
            'create_event_time_start': UserTime(11, 25),
            'create_event_date_end': UserDate(year=2020, month=2, day=26),
            'create_event_time_end': UserTime(3, 30),
            'create_event_duration': relativedelta(hours=3),
            'create_event_event_name': 'some name',
            'create_event_all_day': CreateEventAllDay.ALL_DAY,
        },
        id='no_fields_only_slots',
    ),
    pytest.param(
        {  # Every field is different from corresponding slot value.
            'date_start': UserDate(year=2021, month=2, day=25),
            'time_start': UserTime(12, 25),
            'date_end': UserDate(year=2021, month=2, day=26),
            'time_end': UserTime(4, 30),
            'duration': relativedelta(hours=4),
            'event_name': 'Some name 2',
        },
        {
            'create_event_date_start': UserDate(year=2020, month=2, day=25),
            'create_event_time_start': UserTime(11, 25),
            'create_event_date_end': UserDate(year=2020, month=2, day=26),
            'create_event_time_end': UserTime(3, 30),
            'create_event_duration': relativedelta(hours=3),
            'create_event_event_name': 'some name',
        },
        id='favours_fields'
    ),
    pytest.param(
        {'all_day': True},
        {},
    ),
))
def test_update_from_slots(scenario, fields, slots):
    if 'create_event_event_name' not in slots:
        slot_event_name = None
    else:
        slot_event_name = slots['create_event_event_name'].capitalize()

    scenario._update_from_slots()
    assert_that(
        scenario,
        has_properties({
            '_date_start': None or fields.get('date_start') or slots.get('create_event_date_start'),
            '_time_start': None or fields.get('time_start') or slots.get('create_event_time_start'),
            '_date_end': None or fields.get('date_end') or slots.get('create_event_date_end'),
            '_time_end': None or fields.get('time_end') or slots.get('create_event_time_end'),
            '_duration': None or fields.get('duration') or slots.get('create_event_duration'),
            '_event_name': None or fields.get('event_name') or slot_event_name,
            '_all_day': fields.get('all_day', False) or (
                slots.get('create_event_all_day') is CreateEventAllDay.ALL_DAY),
        })
    )


class TestRequestMissingSlot:
    RESULT_MISSING_PARAMETRIZE = pytest.mark.parametrize('fields,text,requested_slot,action', (
        pytest.param(
            {},
            'На какой день создать событие?',
            ('create_event_date_start', SysSlotType.DATE),
            Action.CREATE_EVENT_START_DATE,
            id='date_start_missing',
        ),
        pytest.param(
            {
                'date_start': 'not-missing',
            },
            'На какое время создать событие?',
            ('create_event_time_start', SysSlotType.TIME),
            Action.CREATE_EVENT_START_TIME,
            id='time_start_missing',
        ),
        pytest.param(
            {
                'date_start': 'not-missing',
                'time_start': 'not-missing',
            },
            'Сколько времени займет событие?',
            ('create_event_duration', SysSlotType.DATETIME_RANGE),
            Action.CREATE_EVENT_DURATION,
            id='time_end_and_duration_missing',
        ),
        pytest.param(
            {
                'date_start': 'not-missing',
                'time_start': 'not-missing',
                'time_end': 'not-missing',
            },
            'Как назвать событие?',
            ('create_event_event_name', SysSlotType.STRING),
            Action.CREATE_EVENT_NAME,
            id='event_name_missing_time_end_present',
        ),
        pytest.param(
            {
                'date_start': 'not-missing',
                'time_start': 'not-missing',
                'duration': 'not-missing',
            },
            'Как назвать событие?',
            ('create_event_event_name', SysSlotType.STRING),
            Action.CREATE_EVENT_NAME,
            id='event_name_missing_duration_present',
        ),
    ))

    @RESULT_MISSING_PARAMETRIZE
    def test_result_missing(self, scenario, text, analytics, action, requested_slot):
        assert_that(
            scenario._request_missing_slot(analytics),
            has_properties({
                'response': has_properties({
                    'text': text,
                    'speech': text,
                    'requested_slot': requested_slot,
                    'contains_sensitive_data': False,
                }),
            })
        )

    @RESULT_MISSING_PARAMETRIZE
    def test_analytics(self, requested_slot, text, scenario, analytics, action):
        scenario._request_missing_slot(analytics)
        assert analytics.actions == [action]

    @pytest.mark.parametrize('fields', (
        pytest.param(
            {
                'date_start': 'not-missing',
                'time_start': 'not-missing',
                'time_end': 'not-missing',
                'event_name': 'not-missing',
            },
            id='with_time_end',
        ),
        pytest.param(
            {
                'date_start': 'not-missing',
                'time_start': 'not-missing',
                'duration': 'not-missing',
                'event_name': 'not-missing',
            },
            id='with_duration',
        ),
        pytest.param(
            {
                'time_start': UserTime(relative=True),
                'duration': 'not-missing',
                'event_name': 'not-missing',
            },
            id='date_start_is_missing_but_time_start_is_relative'
        ),
        pytest.param(
            {
                'date_start': 'not-missing',
                'all_day': True,
                'event_name': 'not-missing',
            },
            id='all_day',
        )
    ))
    def test_nothing_is_missing(self, scenario, analytics):
        assert scenario._request_missing_slot(analytics) is None


class TestGetEventStartEnd:
    @pytest.fixture(autouse=True)
    def work_day_start(self, settings):
        settings.WORK_DAY_START = time(hour=8)

    @pytest.mark.parametrize('fields,datetime_start,datetime_end', (
        pytest.param(
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'time_start': UserTime(8, 30, period=Period.PM),
                'date_end': UserDate(year=2020, month=2, day=25),
                'time_end': UserTime(9, 30, period=Period.PM),
            },
            datetime(2020, 2, 25, 20, 30),
            datetime(2020, 2, 25, 21, 30),
            id='full_start_and_end_provided'
        ),
        pytest.param(
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'time_start': UserTime(8, 30, period=Period.PM),
                'duration': relativedelta(hours=1),
            },
            datetime(2020, 2, 25, 20, 30),
            datetime(2020, 2, 25, 21, 30),
            id='full_start_and_duration_provided'
        ),
        pytest.param(
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'time_start': UserTime(7, 30),
                'duration': relativedelta(hours=1),
            },
            datetime(2020, 2, 25, 19, 30),
            datetime(2020, 2, 25, 20, 30),
            id='fits_start_time_to_8_am'
        ),
        pytest.param(
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'time_start': UserTime(13, 30),
                'time_end': UserTime(14, 30),
            },
            datetime(2020, 2, 25, 13, 30),
            datetime(2020, 2, 25, 14, 30),
            id='assumes_today_end_date_if_possible'
        ),
        pytest.param(
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'time_start': UserTime(13, 30),
                'time_end': UserTime(11, 30, period=Period.AM),
            },
            datetime(2020, 2, 25, 13, 30),
            datetime(2020, 2, 26, 11, 30),
            id='assumes_tomorrow_end_date_if_today_is_impossible'
        ),
        pytest.param(
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'time_start': UserTime(13, 30),
                'time_end': UserTime(11, 30),
            },
            datetime(2020, 2, 25, 13, 30),
            datetime(2020, 2, 25, 23, 30),
            id='assumes_end_time_period_to_use_today_end_date',
        ),
        pytest.param(  # This one is questionable. Might change it later.
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'time_start': UserTime(13, 30),
                'date_end': UserDate(year=2020, month=2, day=26),
                'time_end': UserTime(7, 30),
            },
            datetime(2020, 2, 25, 13, 30),
            datetime(2020, 2, 26, 7, 30),
            id='assumes_end_time_out_of_fit_to_minimize_duration',
        ),
        pytest.param(
            {
                'time_start': UserTime(minute=15, relative=True),
                'duration': relativedelta(hours=1),
            },
            NOW + timedelta(minutes=15),
            NOW + timedelta(hours=1, minutes=15),
            id='relative_time_start_and_duration',
        ),
        pytest.param(
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'time_start': UserTime(hour=7),
                'time_end': UserTime(hour=8),
            },
            datetime(2020, 2, 25, 19),
            datetime(2020, 2, 25, 20),
            id='assumes_work_day_evening_without_periods'
        ),
        pytest.param(
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'time_start': UserTime(hour=7, period=Period.AM),
                'time_end': UserTime(hour=8),
            },
            datetime(2020, 2, 25, 7),
            datetime(2020, 2, 25, 8),
            id='assumes_work_day_morning_with_start_period'
        ),
        pytest.param(
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'time_start': UserTime(hour=7),
                'time_end': UserTime(hour=8, period=Period.AM),
            },
            datetime(2020, 2, 25, 7),
            datetime(2020, 2, 25, 8),
            id='assumes_work_day_morning_with_end_period'
        ),
        pytest.param(
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'all_day': True,
            },
            datetime(2020, 2, 25, 0),
            datetime(2020, 2, 26, 0),
            id='all_day'
        ),
    ))
    def test_result(self, user, setup_user, scenario, datetime_start, datetime_end):
        assert scenario._get_event_start_end() == (
            safe_localize(datetime_start, user.timezone),
            safe_localize(datetime_end, user.timezone),
        )

    @pytest.mark.parametrize('fields', (
        pytest.param(
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'time_start': UserTime(10, 30),
                'date_end': UserDate(year=2020, month=2, day=24),
                'time_end': UserTime(11, 30),
            },
            id='end_date_before_start_date',
        ),
        pytest.param(
            {
                'date_start': UserDate(year=2020, month=2, day=25),
                'time_start': UserTime(10, 30),
                'date_end': UserDate(year=2020, month=2, day=25),
                'time_end': UserTime(8, 30, period=Period.AM),
            },
            id='end_time_before_start_with_fixed_dates',
        ),
    ))
    def test_error(self, setup_user, scenario):
        with pytest.raises(EventEndError):
            scenario._get_event_start_end()


class TestHandleFillingSlots:
    @pytest.fixture
    def all_day(self):
        return False

    @pytest.fixture
    def fields(self, rands, all_day):
        return {
            'state': CreateEventScenarioState.FILLING_SLOTS,
            'event_name': rands(),
            'all_day': all_day,
        }

    @pytest.fixture(autouse=True)
    def update_from_slots_mock(self, mocker):
        return mocker.patch.object(CreateEventScenario, '_update_from_slots')

    @pytest.fixture(autouse=True)
    def request_missing_slot_mock(self, mocker, missing_slot_request):
        return mocker.patch.object(
            CreateEventScenario,
            '_request_missing_slot',
            mocker.Mock(return_value=missing_slot_request),
        )

    @pytest.fixture(autouse=True)
    def get_event_start_end_mock(self, mocker, event_start_end):
        return mocker.patch.object(
            CreateEventScenario,
            '_get_event_start_end',
            mocker.Mock(return_value=event_start_end),
        )

    @pytest.fixture
    def all_mocks(self, mocker, update_from_slots_mock, request_missing_slot_mock, get_event_start_end_mock):
        mock = mocker.Mock()
        mock.attach_mock(update_from_slots_mock, 'update_from_slots')
        mock.attach_mock(request_missing_slot_mock, 'request_missing_slot')
        mock.attach_mock(get_event_start_end_mock, 'get_event_start_end')
        return mock

    @pytest.fixture
    def returned_func(self, setup_user, scenario, analytics):
        async def _inner():
            return await scenario._handle_filling_slots(analytics)

        return _inner

    class TestMissingSlotCase:
        @pytest.fixture
        def missing_slot_request(self):
            return 'not-none'

        def test_missing_slot_case__call_order(self, mocker, analytics, all_mocks, returned):
            assert all_mocks.mock_calls == [
                mocker.call.update_from_slots(),
                mocker.call.request_missing_slot(analytics),
            ]

        def test_missing_slot_case__result(self, missing_slot_request, returned):
            assert returned == missing_slot_request

        def test_missing_slot_case__params(self, scenario, event_start_end, returned):
            assert_that(
                scenario.get_params(),
                has_entries({
                    'state': CreateEventScenarioState.FILLING_SLOTS,
                })
            )

    class TestNoMissingSlotCase:
        @pytest.fixture
        def missing_slot_request(self):
            return None

        @pytest.fixture(autouse=True)
        def today(self, mocker):
            today = date(2020, 3, 18)
            mocker.patch(
                'mail.ciao.ciao.core.scenarios.create_event.timezone_today',
                mocker.Mock(return_value=today),
            )
            return today

        def test_no_missing_slot_case__call_order(self, mocker, analytics, all_mocks, returned):
            assert all_mocks.mock_calls == [
                mocker.call.update_from_slots(),
                mocker.call.request_missing_slot(analytics),
                mocker.call.get_event_start_end(),
            ]

        @pytest.mark.parametrize('today,event_start_end,all_day,text', (
            pytest.param(
                date(2020, 3, 18),
                (datetime(2020, 2, 25, 12, 36), datetime(2020, 2, 25, 14, 48)),
                False,
                'Создать событие "%s" 25 февраля 2020 года с 12:36 до 14:48?',
                id='single_day_same_year',
            ),
            pytest.param(
                date(2020, 3, 18),
                (datetime(2007, 2, 25, 12, 36), datetime(2007, 2, 25, 14, 48)),
                False,
                'Создать событие "%s" 25 февраля 2007 года с 12:36 до 14:48?',
                id='single_day_different_year',
            ),
            pytest.param(
                date(2020, 3, 18),
                (datetime(2020, 2, 25, 12, 36), datetime(2020, 2, 26, 14, 48)),
                False,
                'Создать событие "%s" с 25 февраля 2020 года 12:36 до 26 февраля 2020 года 14:48?',
                id='different_days_same_year',
            ),
            pytest.param(
                date(2020, 3, 18),
                (datetime(2007, 2, 25, 12, 36), datetime(2008, 2, 26, 14, 48)),
                False,
                'Создать событие "%s" с 25 февраля 2007 года 12:36 до 26 февраля 2008 года 14:48?',
                id='different_days_different_year',
            ),
            pytest.param(
                date(2020, 4, 7),
                (datetime(2020, 4, 7, 0), datetime(2020, 4, 8, 0)),
                True,
                'Создать событие "%s" на весь день 7 апреля 2020 года?',
                id='all_day_event',
            ),
        ))
        def test_no_missing_slot_case__result(self, today, fields, event_start_end, text, returned):
            event_name = fields['event_name']
            start_str, end_str = map(lambda dt: format_datetime(dt, today), event_start_end)
            assert_that(
                returned,
                has_properties({
                    'response': has_properties({
                        'text': text % event_name,
                        'speech': text % event_name,
                        'frame_name': FrameName.CREATE_EVENT_SUBSEQUENT,
                        'requested_slot': ('create_event_confirmation', YesNo),
                        'contains_sensitive_data': True,
                    })
                }),
            )

        @pytest.mark.asyncio
        async def test_no_missing_slot_case__handles_event_end_error(self, get_event_start_end_mock, returned_func):
            get_event_start_end_mock.side_effect = EventEndError()
            assert_that(
                await returned_func(),
                has_properties({
                    'response': has_properties({
                        'text': 'Упс. Не удалось подобрать время.',
                        'speech': 'Упс. Не удалось подобрать время.',
                        'contains_sensitive_data': False,
                    }),
                    'value': None,
                }),
            )

        def test_no_missing_slot_case__params(self, scenario, event_start_end, returned):
            assert_that(
                scenario.get_params(),
                has_entries({
                    'state': CreateEventScenarioState.AWAITING_CONFIRMATION,
                })
            )


class TestHandleAwaitingConfirmation:
    @pytest.fixture
    def show_event_id(self, randn):
        return randn()

    @pytest.fixture
    def slots(self, confirmation):
        return {'create_event_confirmation': confirmation}

    @pytest.fixture
    def all_day(self):
        return False

    @pytest.fixture
    def fields(self, rands, event_start_end, all_day):
        return {
            'state': CreateEventScenarioState.AWAITING_CONFIRMATION,
            'event_name': rands(),
            'all_day': all_day,
        }

    @pytest.fixture(autouse=True)
    def create_event_mock(self, mocker, show_event_id):
        return mocker.patch(
            'mail.ciao.ciao.interactions.calendar.base.BaseCalendarClient.create_event',
            mocker.Mock(side_effect=dummy_async_function({"showEventId": show_event_id}))
        )

    @pytest.fixture(autouse=True)
    def get_event_start_end_mock(self, mocker, event_start_end):
        return mocker.patch.object(
            CreateEventScenario,
            '_get_event_start_end',
            mocker.Mock(return_value=event_start_end),
        )

    @pytest.fixture
    def returned_func(self, scenario, setup_user, analytics):
        async def _inner():
            return await scenario._handle_awaiting_confirmation(analytics)

        return _inner

    @pytest.mark.asyncio
    @pytest.mark.parametrize('slots', (
        {},
        {'create_event_confirmation': 'wrong-type'},
    ))
    async def test_requires_confirmation(self, returned_func):
        with pytest.raises(CoreIrrelevantScenarioError):
            await returned_func()

    class TestConfirmed:
        @pytest.fixture
        def confirmation(self):
            return YesNo.YES

        @pytest.mark.parametrize('commit', (False,))
        def test_confirmed__event_not_created_without_commit(self, create_event_mock, returned):
            create_event_mock.assert_not_called()

        @pytest.mark.parametrize('commit', (True,))
        @pytest.mark.parametrize('all_day', (False, True))
        def test_confirmed__event_created_with_commit(self, user, event_start_end, fields, create_event_mock, returned):
            create_event_mock.assert_called_once_with(
                uid=user.uid,
                user_ticket=user.user_ticket,
                start_datetime=event_start_end[0],
                end_datetime=event_start_end[1],
                name=fields['event_name'],
                all_day=fields['all_day'],
            )

        @pytest.mark.parametrize('commit', (True,))
        def test_analytics(self, analytics, returned):
            assert analytics.actions == [Action.EVENT_CREATED]

        def test_confirmed__result(self, returned):
            assert_that(
                returned,
                has_properties({
                    'response': has_properties({
                        'text': 'Событие создано.',
                        'speech': 'Событие создано.',
                        'commit': True,
                        'contains_sensitive_data': False,
                        'buttons': contains(has_properties({
                                'title': 'Открыть календарь',
                                'uri': is_(str),
                        })),
                    }),
                    'value': None,
                })
            )

    class TestDenied:
        @pytest.fixture
        def confirmation(self):
            return YesNo.NO

        def test_denied__event_not_created(self, create_event_mock, returned):
            create_event_mock.assert_not_called()

        def test_denied__result(self, returned):
            assert_that(
                returned,
                has_properties({
                    'response': has_properties({
                        'text': 'Хорошо.',
                        'speech': 'Хорошо.',
                        'contains_sensitive_data': False,
                    }),
                    'value': None,
                })
            )


class TestHandle:
    @pytest.fixture
    def create_event_state(self):
        return None

    @pytest.fixture
    def fields(self, create_event_state):
        return without_none({'state': create_event_state})

    @pytest.fixture
    def filling_slots_result(self, mocker):
        return mocker.Mock()

    @pytest.fixture
    def awaiting_confirmation_result(self, mocker):
        return mocker.Mock()

    @pytest.fixture(autouse=True)
    def handle_mocks(self, mocker, filling_slots_result, awaiting_confirmation_result):
        mock = mocker.Mock()
        mock.handle_filling_slots = mocker.patch.object(
            CreateEventScenario,
            '_handle_filling_slots',
            mocker.Mock(side_effect=dummy_async_function(filling_slots_result))
        )
        mock.handle_awaiting_confirmation = mocker.patch.object(
            CreateEventScenario,
            '_handle_awaiting_confirmation',
            mocker.Mock(side_effect=dummy_async_function(awaiting_confirmation_result))
        )
        return mock

    @pytest.fixture
    def returned_func(self, scenario):
        async def _inner():
            return await scenario.handle()

        return _inner

    @pytest.mark.asyncio
    @pytest.mark.parametrize('frame_name,create_event_state', (
        pytest.param(FrameName.CREATE_EVENT, CreateEventScenarioState.FILLING_SLOTS, id='user_restart'),
    ))
    async def test_checks_frame_matches_state(self, returned_func):
        with pytest.raises(CoreIrrelevantScenarioError):
            await returned_func()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('frame_name,create_event_state', (
        (FrameName.CREATE_EVENT_SUBSEQUENT, 'unexpected-value'),
    ))
    async def test_fails_on_unexpected_state(self, analytics, returned_func):
        with pytest.raises(CoreIrrelevantScenarioError) as exc_info:
            await returned_func()
        assert exc_info.value.analytics == analytics

    @pytest.mark.parametrize('frame_name,create_event_state', (
        (FrameName.CREATE_EVENT, None),
        (FrameName.CREATE_EVENT_SUBSEQUENT, CreateEventScenarioState.FILLING_SLOTS),
    ))
    class TestInitialAndFillingSlots:
        def test_initial_and_filling_slots__calls(self, mocker, handle_mocks, analytics, returned):
            assert handle_mocks.mock_calls == [mocker.call.handle_filling_slots(analytics)]

        def test_initial_and_filling_slots__result(self, filling_slots_result, returned):
            assert returned is filling_slots_result

    @pytest.mark.parametrize('frame_name,create_event_state', (
        (FrameName.CREATE_EVENT_SUBSEQUENT, CreateEventScenarioState.AWAITING_CONFIRMATION),
    ))
    class TestAwaitingConfirmation:
        def test_awaiting_confirmation__calls(self, mocker, handle_mocks, analytics, returned):
            assert handle_mocks.mock_calls == [mocker.call.handle_awaiting_confirmation(analytics)]

        def test_awaiting_confirmation__result(self, awaiting_confirmation_result, returned):
            assert returned is awaiting_confirmation_result
