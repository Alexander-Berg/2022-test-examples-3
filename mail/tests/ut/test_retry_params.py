import pytest
from dataclasses import dataclass
from datetime import timedelta

from mail.callmeback.callmeback.detail.retry_params import RetryParams, ConstantRetryStrategy, RetriesLimit, QuickRetries

EMPTY_PARAMS = {}

DEFAULT_QUICK_RETRIES_COUNT = 5
DEFAULT_STOP_AFTER_DELAY = timedelta(days=5)
DEFAULT_RETRY_DELAY = timedelta(seconds=60)


@dataclass
class Config:
    quick_retries = QuickRetries(DEFAULT_QUICK_RETRIES_COUNT)
    retries_limit = RetriesLimit(DEFAULT_STOP_AFTER_DELAY)
    retry_strategy = ConstantRetryStrategy(DEFAULT_RETRY_DELAY)


def test_get_quick_retries_count_for_empty_params():
    retry_params = RetryParams(Config())
    assert retry_params.get_quick_retries_count(EMPTY_PARAMS) == DEFAULT_QUICK_RETRIES_COUNT


def test_get_stop_after_delay_for_empty_params():
    retry_params = RetryParams(Config())
    assert retry_params.get_stop_after_delay(EMPTY_PARAMS) == DEFAULT_STOP_AFTER_DELAY


@pytest.mark.parametrize(
    "retry_number",
    [1, 2, 3],
)
def test_get_retry_delay_for_empty_params(retry_number):
    retry_params = RetryParams(Config())
    assert retry_params.get_retry_delay(EMPTY_PARAMS, retry_number) == DEFAULT_RETRY_DELAY


def test_get_quick_retries_count_for_given_params():
    retry_params = RetryParams(Config())
    assert retry_params.get_quick_retries_count({'quick_retries_count': 10}) == 10


def test_get_stop_after_delay_for_given_params():
    retry_params = RetryParams(Config())
    assert retry_params.get_stop_after_delay({'stop_after_delay': 3600}) == timedelta(seconds=3600)


@pytest.mark.parametrize(
    "retry_number, base_delay, retry_delay",
    [
        (1, 120, timedelta(seconds=120)),
        (2, 120, timedelta(seconds=120)),
        (5, 120, timedelta(seconds=120)),
    ],
)
def test_get_retry_delay_for_given_constant_retry_strategy(retry_number, base_delay, retry_delay):
    retry_params = RetryParams(Config())
    assert retry_params.get_retry_delay({'strategy': 'constant', 'base_delay': base_delay}, retry_number) == retry_delay


@pytest.mark.parametrize(
    "retry_number, base_delay, delay_growth_rate, retry_delay",
    [
        (1, 100, 2, timedelta(seconds=100)),
        (2, 100, 2, timedelta(seconds=100*2)),
        (3, 100, 2, timedelta(seconds=100*2*2)),
        (5, 100, 2, timedelta(seconds=100*2*2*2*2)),
        (2, 100, 3, timedelta(seconds=100*3)),
    ],
)
def test_get_retry_delay_for_given_exponential_retry_strategy(retry_number, base_delay, delay_growth_rate, retry_delay):
    retry_params = RetryParams(Config())
    assert retry_params.get_retry_delay({'strategy': 'exponential', 'base_delay': base_delay, 'delay_growth_rate': delay_growth_rate}, retry_number) == retry_delay


def test_get_quick_retries_count_for_bad_params():
    retry_params = RetryParams(Config())
    assert retry_params.get_quick_retries_count({'quick_retries_count': '11asd'}) == DEFAULT_QUICK_RETRIES_COUNT


def test_get_stop_after_delay_for_bad_params():
    retry_params = RetryParams(Config())
    assert retry_params.get_stop_after_delay({'stop_after_delay': '3600sadfg'}) == DEFAULT_STOP_AFTER_DELAY


def test_get_retry_delay_for_bad_params():
    retry_params = RetryParams(Config())
    assert retry_params.get_retry_delay({'strategy': 'exponential', 'base_delay': 130, 'delay_growth_rate': 'saf'}, 3) == DEFAULT_RETRY_DELAY
    assert retry_params.get_retry_delay({'strategy': 'exponential', 'base_delay': 130}, 3) == DEFAULT_RETRY_DELAY
    assert retry_params.get_retry_delay({'strategy': 'exponential', 'delay_growth_rate': 2}, 3) == DEFAULT_RETRY_DELAY
    assert retry_params.get_retry_delay({'strategy': 'constant', 'base_delay': 'asd'}, 3) == DEFAULT_RETRY_DELAY
    assert retry_params.get_retry_delay({'base_delay': 130, 'delay_growth_rate': 2}, 3) == DEFAULT_RETRY_DELAY


@pytest.mark.parametrize(
    "retry_params",
    [
        {'stop_after_delay': '3600sadfg'},
        {'quick_retries_count': '11asd'},
        {'strategy': 'exponential', 'base_delay': 130, 'delay_growth_rate': 'saf'},
        {'strategy': 'exponential', 'base_delay': 130},
        {'strategy': 'constant', 'base_delay': 'asd'},
        {'strategy': 'constant'},
        {'strategy': 'unknown_strategy'},
    ],
)
def test_check_params_for_bad_params(retry_params):
    with pytest.raises(Exception):
        RetryParams.check_params(retry_params)


def test_check_params_for_empty_params():
    RetryParams.check_params({})
