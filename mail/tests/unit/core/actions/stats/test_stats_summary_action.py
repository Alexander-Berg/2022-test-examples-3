from itertools import chain

import pytest

from mail.ipa.ipa.core.actions.stats.summary import GetImportStatAction
from mail.ipa.ipa.core.entities.stat import ImportStat


class TestGetImportStatAction:
    @pytest.fixture
    async def users(self, rands, org_id, create_user):
        users = []
        users += [await create_user(org_id) for _ in range(3)]
        users += [await create_user(org_id, error=rands()) for _ in range(2)]
        return users

    @pytest.fixture
    def ok_users(self, users):
        return [u for u in users if u.error is None]

    @pytest.fixture
    async def simple_collectors(self, ok_users, create_collector):
        return [await create_collector(user_id=user.user_id) for user in ok_users]

    @pytest.fixture
    async def finished_collectors(self, randn, ok_users, create_collector):
        collected = randn()
        errors = randn()
        return [
            await create_collector(
                user_id=user.user_id,
                collected=collected,
                errors=errors,
                total=collected + errors,
            )
            for user in ok_users
        ]

    @pytest.fixture
    def returned_func(self, org_id, users):
        async def _inner():
            return await GetImportStatAction(org_id=org_id).run()

        return _inner

    @pytest.fixture
    def get_expected(self, users):
        def _inner(collectors):
            error_users = len([u for u in users if u.error is not None])
            return ImportStat(
                total=error_users + len(collectors),
                finished=len([c for c in collectors if c.is_finished]),
                errors=error_users + len([c for c in collectors if not c.is_ok_status]),
            )

        return _inner

    def test_empty(self, returned, get_expected):
        assert returned == get_expected([])

    def test_simple(self, simple_collectors, returned, get_expected):
        assert returned == get_expected(simple_collectors)

    def test_finished(self, finished_collectors, returned, get_expected):
        assert returned == get_expected(finished_collectors)

    def test_all(self, simple_collectors, finished_collectors, returned, get_expected):
        assert returned == get_expected(list(chain(
            simple_collectors,
            finished_collectors,
        )))
