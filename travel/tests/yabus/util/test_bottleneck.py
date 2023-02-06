# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import freezegun
import gevent
import pytest
import ratelimit

from yabus.util.bottleneck import BottleneckBehavior, bottleneck

try:
    from unittest import mock
except ImportError:
    import mock


@pytest.fixture
def frozen_time():
    with freezegun.freeze_time() as frozen_time:
        yield frozen_time


def test_sleep_behavior(frozen_time):
    m_func = mock.Mock()

    @bottleneck(calls=10, behavior=BottleneckBehavior.SLEEP)
    def wrapped():
        m_func()

    with mock.patch.object(wrapped.bottleneck, 'clock', freezegun.api.fake_time), \
            mock.patch.object(gevent, 'sleep', side_effect=frozen_time.tick) as m_sleep:
        for _ in range(10):
            wrapped()

        assert m_func.call_count == 10
        assert not m_sleep.called

        wrapped()

        assert m_func.call_count == 11
        assert m_sleep.call_count == 1


def test_raise_behavior(frozen_time):
    m_func = mock.Mock()

    @bottleneck(calls=10, behavior=BottleneckBehavior.RAISE)
    def wrapped():
        m_func()

    with mock.patch.object(wrapped.bottleneck, 'clock', freezegun.api.fake_time):
        for _ in range(10):
            wrapped()

        assert m_func.call_count == 10

        with pytest.raises(ratelimit.RateLimitException):
            wrapped()

        assert m_func.call_count == 10


def test_return_none_behavior(frozen_time):
    m_func = mock.Mock(return_value=True)

    @bottleneck(calls=10, behavior=BottleneckBehavior.RETURN_NONE)
    def wrapped():
        return m_func()

    with mock.patch.object(wrapped.bottleneck, 'clock', freezegun.api.fake_time):
        assert all(wrapped() for _ in range(10))
        assert m_func.call_count == 10

        assert wrapped() is None
        assert m_func.call_count == 10
