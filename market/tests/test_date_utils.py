import os
import datetime
import dateutil
import time
import pytest
from hamcrest import assert_that, raises, calling, equal_to
from market.idx.cron.availability_metrics.date_utils import date_range, ensure_msk_datetime, to_timestamp


@pytest.fixture(scope="function", params=["MSK", "UTC", None])
def timezone(request):
    """System timezone should not affect time_utils
    In order to validate in we run tests with different values of TZ env variable"""
    original_tz = os.environ.get('TZ')
    try:
        if request.param:
            os.environ['TZ'] = request.param
        else:
            try:
                del os.environ['TZ']
            except KeyError:
                pass
        time.tzset()
        yield
    finally:
        if original_tz:
            os.environ['TZ'] = original_tz
        else:
            try:
                del os.environ['TZ']
            except KeyError:
                pass
        time.tzset()


def test_date_range(timezone):
    result_dates = list(date_range(datetime.date(2022, 1, 1), datetime.date(2022, 1, 3)))
    assert_that(
        result_dates, equal_to([datetime.date(2022, 1, 1), datetime.date(2022, 1, 2), datetime.date(2022, 1, 3)])
    )


def test_date_range_one_day(timezone):
    result_dates = list(date_range(datetime.date(2022, 1, 1), datetime.date(2022, 1, 1)))
    assert_that(result_dates, equal_to([datetime.date(2022, 1, 1)]))


def test_date_range_illegal_dates(timezone):
    # no exception here becase dates_range only creates generator
    date_range_gen = date_range(datetime.date(2022, 1, 3), datetime.date(2022, 1, 1))
    assert_that(calling(next).with_args(date_range_gen), raises(ValueError))


def test_ensure_msk_datetime_parser(timezone):
    result = ensure_msk_datetime("2022-01-05")
    assert_that(
        result,
        equal_to(
            datetime.datetime(year=2022, month=1, day=5, hour=0, minute=0, tzinfo=dateutil.tz.gettz('Europe/Moscow'))
        ),
    )


def test_ensure_msk_datetime_parser_error(timezone):
    assert_that(calling(ensure_msk_datetime).with_args("2022-01-05-07"), raises(ValueError))


def test_ensure_msk_datetime_fix_type(timezone):
    result = ensure_msk_datetime(datetime.date(year=2022, month=1, day=5))
    assert_that(
        result,
        equal_to(
            datetime.datetime(year=2022, month=1, day=5, hour=0, minute=0, tzinfo=dateutil.tz.gettz('Europe/Moscow'))
        ),
    )


def test_ensure_msk_datetime_fix_tz(timezone):
    result = ensure_msk_datetime(
        datetime.datetime(year=2022, month=1, day=5, hour=0, minute=0, tzinfo=dateutil.tz.gettz('Europe/London'))
    )
    assert_that(
        result,
        equal_to(
            datetime.datetime(year=2022, month=1, day=5, hour=3, minute=0, tzinfo=dateutil.tz.gettz('Europe/Moscow'))
        ),
    )


def test_to_timestamp(timezone):
    """Way to get timestamp:
    $ date --date "2022-01-05 00:00:00 MSK" +%s
    1641330000
    """
    timestamp = to_timestamp(datetime.datetime(year=2022, month=1, day=5, hour=0, tzinfo=dateutil.tz.gettz('Europe/Moscow')))
    assert_that(timestamp, equal_to(1641330000))
