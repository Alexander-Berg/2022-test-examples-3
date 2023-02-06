from datetime import datetime, timedelta

import pytest

from hamcrest import assert_that, has_properties

from mail.ciao.ciao.core.entities.state import State


class TestState:
    @pytest.fixture
    def hard_ttl(self, mocker):
        return 600

    @pytest.fixture
    def soft_ttl(self, mocker):
        return 60

    @pytest.fixture
    def now(self, mocker):
        return datetime(2020, 1, 30, 18, 10, 30)

    @pytest.fixture(autouse=True)
    def setup(self, mocker, hard_ttl, soft_ttl, now):
        mocker.patch('mail.ciao.ciao.core.entities.state.settings.STATE_HARD_TTL', hard_ttl)
        mocker.patch('mail.ciao.ciao.core.entities.state.settings.STATE_SOFT_TTL', soft_ttl)
        mocker.patch('mail.ciao.ciao.core.entities.state.utcnow', mocker.Mock(return_value=now))

    def test_new_state(self, hard_ttl, soft_ttl, now):
        assert_that(
            State(),
            has_properties({
                'hard_expire': now + timedelta(seconds=hard_ttl),
                'soft_expire': now + timedelta(seconds=soft_ttl),
            }),
        )

    def test_updates_soft_expire(self, soft_ttl, now):
        state = State(soft_expire=now + timedelta(seconds=soft_ttl / 2))
        assert state.soft_expire == now + timedelta(seconds=soft_ttl)

    def test_does_not_update_soft_expire_if_expired(self, mocker, soft_ttl, now):
        mocker.patch.object(State, 'expired', True)
        soft_expire = now + timedelta(seconds=soft_ttl / 2)
        assert State(soft_expire=soft_expire).soft_expire == soft_expire

    @pytest.mark.parametrize('hard_expire_delta,soft_expire_delta,expired', (
        (1, 1, False),
        (0, 1, True),
        (1, 0, True),
    ))
    def test_hard_expired(self, now, hard_expire_delta, soft_expire_delta, expired):
        assert State(
            hard_expire=now + timedelta(seconds=hard_expire_delta),
            soft_expire=now + timedelta(seconds=soft_expire_delta),
        ).expired == expired
