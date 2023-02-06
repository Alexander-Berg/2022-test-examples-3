from datetime import date, datetime, time, timedelta, timezone

import pytest
from dateutil.relativedelta import relativedelta

from mail.ciao.ciao.utils.datetime import Period, UserDate, UserTime, time_between, time_distance

TODAY = date(2020, 3, 16)


@pytest.mark.parametrize('time_,start,end,between', (
    # start is lower than end
    pytest.param(time(12), time(11), time(13), True, id='start_lower_than_end_between'),
    pytest.param(time(14), time(11), time(13), False, id='start_lower_than_end_not_between'),
    pytest.param(time(11), time(11), time(13), True, id='start_lower_than_end_start_edge'),
    pytest.param(time(13), time(11), time(13), False, id='start_lower_than_end_end_edge'),
    # start is greater than end
    pytest.param(time(10), time(13), time(11), True, id='start_greater_than_end_between_lower'),
    pytest.param(time(14), time(13), time(11), True, id='start_greater_than_end_between_greater'),
    pytest.param(time(12), time(13), time(11), False, id='start_greater_than_end_not_between'),
    pytest.param(time(13), time(13), time(11), True, id='start_greater_than_end_start_edge'),
    pytest.param(time(11), time(13), time(11), False, id='start_greater_than_end_end_edge'),
))
def test_time_between(time_, start, end, between):
    assert time_between(time_, start, end) == between


@pytest.mark.parametrize('t1,t2,expected', (
    (time(10, 30), time(8), timedelta(hours=2, minutes=30)),
    (time(8), time(10, 30), timedelta(hours=2, minutes=30)),
    (time(0), time(21), timedelta(hours=3)),
    (time(21), time(0), timedelta(hours=3)),
))
def test_time_distance(t1, t2, expected):
    assert time_distance(t1, t2) == expected


class TestUserTime:
    @pytest.mark.parametrize('hour,expected', (
        (1, True),
        (0, False),
        (12, True),
        (13, False),
    ))
    def test_is_12_hour(self, hour, expected):
        assert UserTime.is_12_hour(hour) == expected

    @pytest.mark.parametrize('hour,period,expected', (
        (1, Period.AM, 1),
        (1, Period.PM, 13),
        (11, Period.AM, 11),
        (11, Period.PM, 23),
        (12, Period.PM, 12),
        (12, Period.AM, 0),
    ))
    def test_convert_hour_12_to_24(self, hour, period, expected):
        assert UserTime.convert_hour_12_to_24(hour, period) == expected

    @pytest.mark.parametrize('user_time,fixed', (
        (UserTime(), False),
        (UserTime(hour=7), False),
        (UserTime(hour=7, period=Period.AM), True),
        (UserTime(hour=19), True),
        (UserTime(minute=15, relative=True), True),
    ))
    def test_fixed(self, user_time, fixed):
        assert user_time.fixed == fixed

    @pytest.mark.parametrize('kwargs,assumed_period,expected', (
        pytest.param(
            dict(hour=10, minute=20, second=30, period=Period.AM),
            Period.PM,
            time(10, 20, 30),
            id='ignores_period_if_already_specified',
        ),
        pytest.param(
            dict(hour=23, minute=20, second=30),
            Period.AM,
            time(23, 20, 30),
            id='ignores_period_if_hour_in_24_format',
        ),
        pytest.param(
            dict(hour=10, minute=20, second=30),
            Period.AM,
            time(10, 20, 30),
            id='assumes_am',
        ),
        pytest.param(
            dict(hour=10, minute=20, second=30),
            Period.PM,
            time(22, 20, 30),
            id='assumes_pm',
        ),
        pytest.param(
            dict(hour=12, minute=20, second=30),
            Period.AM,
            time(0, 20, 30),
            id='assumes_am_12',
        ),
        pytest.param(
            dict(hour=12, minute=20, second=30),
            Period.PM,
            time(12, 20, 30),
            id='assumes_pm_12',
        ),
        pytest.param(
            dict(),
            Period.PM,
            time(0, 0, 0),
            id='fills_missing_fields_with_0',
        ),
    ))
    def test_get_assumed_time(self, kwargs, assumed_period, expected):
        assert UserTime(**kwargs).get_assumed_time(assumed_period) == expected

    def test_get_assumed_time_asserts_absolute(self):
        with pytest.raises(AssertionError):
            UserTime(relative=True).get_assumed_time(Period.AM)

    @pytest.mark.parametrize('kwargs,fit_period_start,expected', (
        pytest.param(
            dict(hour=12, minute=58, second=13, period=Period.AM),
            time(hour=9),
            time(0, 58, 13),
            id='ignores_if_period_provied'
        ),
        pytest.param(
            dict(hour=12, minute=58, second=13),  # Either 12:58 or 00:58
            time(hour=9),  # Half of the day starting with 09:00 and ending with 21:00
            time(12, 58, 13),  # Fits to 12:58 which fits into desired half
            id='fits_using_pm'
        ),
        pytest.param(
            dict(hour=12, minute=58, second=13),  # Either 12:58 or 00:58
            time(hour=21),  # 21:00 - 09:00
            time(0, 58, 13),  # Fits 00:58
            id='fits_using_am'
        ),
    ))
    def test_get_fit_time(self, kwargs, fit_period_start, expected):
        assert UserTime(**kwargs).get_fit_time(fit_period_start) == expected

    def test_get_fit_time_asserts_absolute(self):
        with pytest.raises(AssertionError):
            UserTime(relative=True).get_fit_time(time())

    @pytest.mark.parametrize('kwargs,delta', (
        ({'hour': 10}, timedelta(hours=10)),
        ({'minute': 10}, timedelta(minutes=10)),
        ({'second': 10}, timedelta(seconds=10)),
        ({'hour': 3, 'minute': 4, 'second': 5}, timedelta(hours=3, minutes=4, seconds=5)),
    ))
    def test_get_absolute(self, kwargs, delta):
        now = datetime(2020, 2, 26, 11, 10, tzinfo=timezone.utc)
        assert UserTime(relative=True, **kwargs).get_absolute(now) == now + delta

    def test_get_absolute_asserts_relative(self):
        with pytest.raises(AssertionError):
            UserTime(relative=False).get_absolute(datetime.now())


class TestUserDate:
    @pytest.mark.parametrize('kwargs,delta', (
        pytest.param({}, relativedelta(), id='empty'),
        pytest.param({'day': 1}, relativedelta(days=1), id='day'),
        pytest.param({'month': 1}, relativedelta(months=1), id='month'),
        pytest.param({'year': 1}, relativedelta(years=1), id='year'),
        pytest.param({'weeks': 1}, relativedelta(weeks=1), id='weeks'),
    ))
    def test_get_absolute(self, kwargs, delta):
        kwargs['relative'] = True
        assert UserDate(**kwargs).get_absolute(TODAY) == TODAY + delta

    @pytest.mark.parametrize('kwargs,result', (
        pytest.param({'day': 17, 'month': 3, 'year': 2020}, date(2020, 3, 17), id='filled'),
        pytest.param({'day': TODAY.day, 'month': TODAY.month}, TODAY, id='fills_year'),
        pytest.param({'day': TODAY.day}, TODAY, id='fills_year_and_month'),
        pytest.param({'weekday': TODAY.weekday()}, TODAY, id='weekday_today'),
        pytest.param({'weekday': (TODAY.weekday() + 1) % 7 + 1}, TODAY + timedelta(days=1), id='weekday_tomorrow'),
        pytest.param({'weekday': (TODAY.weekday() + 6) % 7 + 1}, TODAY + timedelta(days=6), id='weekday_6_days_later'),
        pytest.param({'day': TODAY.day - 1}, TODAY + relativedelta(days=-1, months=1), id='fills_not_past'),
    ))
    def test_get_assumed_date(self, kwargs, result):
        assert UserDate(**kwargs).get_assumed_date(TODAY) == result

    class TestGetDate:
        @pytest.fixture(autouse=True)
        def get_absolute_mock(self, mocker):
            return mocker.patch.object(UserDate, 'get_absolute')

        @pytest.fixture(autouse=True)
        def get_assumed_date_mock(self, mocker):
            return mocker.patch.object(UserDate, 'get_assumed_date')

        def test_get_date__relative(self, mocker, get_absolute_mock, get_assumed_date_mock):
            result = UserDate(relative=True).get_date(TODAY)
            assert all((
                result is get_absolute_mock.return_value,
                get_absolute_mock.mock_calls == [mocker.call(TODAY)],
                get_assumed_date_mock.mock_calls == [],
            ))

        def test_get_date__absolue(self, mocker, get_absolute_mock, get_assumed_date_mock):
            result = UserDate(relative=False).get_date(TODAY)
            assert all((
                result is get_assumed_date_mock.return_value,
                get_absolute_mock.mock_calls == [],
                get_assumed_date_mock.mock_calls == [mocker.call(TODAY)],
            ))
