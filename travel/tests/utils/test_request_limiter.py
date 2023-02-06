# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
import pytest

from common.tester.utils.replace_setting import replace_setting
from common.utils.request_limiter import RequestLimiter


@pytest.fixture(autouse=True)
def request_limiter_can_sleep():
    with replace_setting('REQUEST_LIMITER_NEVER_SLEEP', False):
        yield


@pytest.mark.parametrize('rps, expected_sleep_value', [
    (10, 0.1),
    (1, 1),
    (2, 0.5),
    (0.1, 10)
])
def test_request_limiter(rps, expected_sleep_value):
    with mock.patch('common.utils.request_limiter.c_time') as m_c_time:
        request_limiter = RequestLimiter(max_rps=rps)
        m_c_time.time = mock.Mock(return_value=10.0)
        m_c_time.sleep = mock.Mock()

        request_limiter.make_time_delay()
        assert not m_c_time.sleep.call_count

        request_limiter.make_time_delay()
        assert m_c_time.sleep.call_count == 1
        _assert_almost_equal(m_c_time.sleep.call_args[0][0], expected_sleep_value)


@pytest.mark.parametrize('rps, expected_sleep_value', [
    (10, 0.1),
    (1, 1),
    (2, 0.5),
    (0.1, 10)
])
def test_request_limiter_throttle(rps, expected_sleep_value):
    with mock.patch('common.utils.request_limiter.c_time') as m_c_time:
        request_limiter = RequestLimiter(max_rps=rps)

        @request_limiter.throttle
        def get_content(url):
            pass

        m_c_time.time = mock.Mock(return_value=10.0)
        m_c_time.sleep = mock.Mock()

        get_content('url1')
        assert not m_c_time.sleep.call_count

        get_content('url2')
        assert m_c_time.sleep.call_count == 1
        _assert_almost_equal(m_c_time.sleep.call_args[0][0], expected_sleep_value)


def _assert_almost_equal(first, second):
    assert round(abs(first - second), ndigits=7) == 0
