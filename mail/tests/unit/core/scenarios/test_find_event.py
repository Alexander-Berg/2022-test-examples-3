from datetime import datetime, timedelta
from unittest.mock import seal

import pytest
import pytz

from sendr_utils import without_none

from hamcrest import assert_that, contains, has_properties

from mail.ciao.ciao.core.entities.enums import FrameName
from mail.ciao.ciao.core.entities.scenario_result import ScenarioResult
from mail.ciao.ciao.core.scenarios.find_event import FindEventScenario
from mail.ciao.ciao.utils.datetime import UserTime


@pytest.fixture
def event(create_event_entity):
    return create_event_entity()


@pytest.fixture
def slots():
    return None


@pytest.fixture
def events(create_event_entity):
    return [create_event_entity() for _ in range(3)]


@pytest.fixture
def name():
    return None


@pytest.fixture
def time_start():
    return None


@pytest.fixture
def scenario(slots, events, name, time_start):
    return FindEventScenario(**without_none(dict(
        slots=slots,
        events=events,
        name=name,
        time_start=time_start,
    )))


@pytest.mark.parametrize('slots,name,time_start,expected', (
    pytest.param(
        {'find_event_name': 'test1', 'find_event_time_start': UserTime(hour=1, minute=2)},
        None,
        None,
        {'name': 'test1', 'time_start': UserTime(hour=1, minute=2)},
        id='only-slots',
    ),
    pytest.param(
        None,
        'test-name',
        UserTime(hour=1, minute=2),
        {'name': 'test-name', 'time_start': UserTime(hour=1, minute=2)},
        id='no-slots',
    ),
    pytest.param(
        {'find_event_name': 'slot-name', 'find_event_time_start': UserTime(hour=1)},
        'param-name',
        UserTime(hour=2),
        {'name': 'slot-name', 'time_start': UserTime(hour=1)},
        id='slots-over-params',
    ),
))
def test_get_params(events, scenario, expected):
    assert scenario.get_params() == {
        'events': events,
        'name': None,
        'time_start': None,
        **expected,
    }


@pytest.mark.parametrize('name,event_name,expected', (
    pytest.param('dinner', 'dinner', 0, id='equal'),
    pytest.param('dinnER', 'DInner', 0, id='case-ignored'),
    pytest.param('dinner', 'diner', 1, id='deletion'),
    pytest.param('diner', 'dinner', 1, id='insertion'),
    pytest.param('finner', 'dinner', 1, id='substitution'),
    pytest.param('nn', 'dinner', 4, id='complex'),
))
def test_name_key(event, name, event_name, expected):
    event.name = event_name
    assert FindEventScenario._name_key(name, event) == expected


class TestTimeStartKey:
    @pytest.fixture(autouse=True)
    def now(self, mocker):
        dt = mocker.patch('mail.ciao.ciao.core.scenarios.find_event.datetime')
        now = datetime(2020, 4, 16, 14, 30)
        dt.now = lambda tz: tz.localize(now)
        seal(dt)

    @pytest.mark.parametrize('time_start,tz,event_time_start,expected', (
        pytest.param(
            UserTime(hour=10),
            pytz.utc,
            pytz.utc.localize(datetime(2020, 4, 16, 10, 15)),
            timedelta(minutes=15),
            id='absolute-assumed-am'
        ),
        pytest.param(
            UserTime(hour=10),
            pytz.utc,
            pytz.utc.localize(datetime(2020, 4, 16, 22, 15)),
            timedelta(minutes=15),
            id='absolute-assumed-pm'
        ),
        pytest.param(  # depends on "now"
            UserTime(hour=1, relative=True),
            pytz.utc,
            pytz.utc.localize(datetime(2020, 4, 16, 15, 0)),
            timedelta(minutes=30),
            id='relative'
        ),
        pytest.param(
            UserTime(hour=21),
            pytz.FixedOffset(3 * 60),  # 3 hour offset
            pytz.utc.localize(datetime(2020, 4, 16, 21, 0)),
            timedelta(hours=3),
            id='different-timezones',
        ),
    ))
    def test_result(user, event, time_start, tz, event_time_start, expected):
        event.start_ts = event_time_start
        assert FindEventScenario._time_start_key(time_start, tz, event) == expected


class TestHandle:
    @pytest.fixture
    def slots(self):
        return None

    @pytest.fixture
    def name(self):
        return None

    @pytest.fixture
    def time_start(self):
        return None

    @pytest.fixture
    def returned_func(self, setup_user, scenario):
        return scenario.handle

    class TestNoFilters:
        def test_no_filters__requests_filter(self, returned):
            assert_that(
                returned,
                has_properties({
                    'response': has_properties({
                        'text': 'Укажите название или время начала события.',
                        'expected_frames': contains(FrameName.FIND_EVENT),
                        'expects_continuation': True,
                    }),
                }),
            )

    class TestSingleEvent:
        @pytest.fixture
        def events(self, create_event_entity):
            return [create_event_entity()]

        def test_single_event__returned(self, events, returned):
            assert returned == ScenarioResult(value=events[0])

    class TestNameFilter:
        @pytest.fixture
        def events(self, create_event_entity):
            return [
                create_event_entity(name=name)
                for name in ('aaa', 'bbb', 'ccc')
            ]

        @pytest.mark.parametrize('name,expected_event_index', (
            pytest.param('aaa', 0, id='full_match_0'),
            pytest.param('bbb', 1, id='full_match_1'),
            pytest.param('ccc', 2, id='full_match_2'),
            pytest.param('aab', 0, id='partial_match_0'),
            pytest.param('abb', 1, id='partial_match_1'),
            pytest.param('cc', 2, id='partial_match_2'),
        ))
        def test_name_filter__returned(self, events, returned, expected_event_index):
            assert returned == ScenarioResult(value=events[expected_event_index])

    class TestTimeStartFilter:
        @pytest.fixture(autouse=True)
        def now(self, mocker):
            dt = mocker.patch('mail.ciao.ciao.core.scenarios.find_event.datetime')
            now = datetime(2020, 4, 16, 8, 30)
            dt.now.side_effect = lambda tz: tz.localize(now)
            seal(dt)
            return now

        @pytest.fixture
        def events(self, user, create_event_entity):
            return [
                create_event_entity(start=user.timezone.localize(start))
                for start in (
                    datetime(2020, 4, 16, 10),
                    datetime(2020, 4, 16, 12, 30),
                    datetime(2020, 4, 16, 19),
                )
            ]

        @pytest.mark.parametrize('time_start,expected_event_index', (
            pytest.param(UserTime(10), 0, id='absolute_match_0'),
            pytest.param(UserTime(12, 30), 1, id='absolute_match_1'),
            pytest.param(UserTime(19), 2, id='absolute_match_2'),
            pytest.param(UserTime(hour=2, relative=True), 0, id='relative_match_0'),
            pytest.param(UserTime(hour=4, relative=True), 1, id='relative_match_1'),
            pytest.param(UserTime(hour=11, relative=True), 2, id='relative_match_2'),
            pytest.param(UserTime(12), 1, id='absolute_approximate_match_1'),
            pytest.param(UserTime(20), 2, id='approximate_match_2'),
            pytest.param(UserTime(20, 1), None, id='no_match_in_1_hour_distance'),
        ))
        def test_time_start_filter__returned(self, events, returned, expected_event_index):
            expected_event = None if expected_event_index is None else events[expected_event_index]
            assert returned == ScenarioResult(value=expected_event)
