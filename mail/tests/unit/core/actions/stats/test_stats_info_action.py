from datetime import datetime, timedelta, timezone
from itertools import chain

import pytest

from mail.ipa.ipa.core.actions.stats.info import GetImportInfoAction, UserImportError


class TestGetImportInfoAction:
    @pytest.fixture(autouse=True)
    def func_now(self, mocker):
        now = datetime(2019, 12, 23, 18, 45, 33, tzinfo=timezone.utc)
        delta = timedelta()

        def _dummy_now():
            nonlocal delta
            delta += timedelta(seconds=1)
            return now + delta  # making sure modified_at is different for each object

        mocker.patch('mail.ipa.ipa.storage.mappers.user.mapper.func.now', _dummy_now)

    @pytest.fixture
    def limit(self):
        return 1000

    @pytest.fixture(autouse=True)
    def setup_limit(self, limit):
        before = GetImportInfoAction.LIMIT
        GetImportInfoAction.LIMIT = limit
        yield
        GetImportInfoAction.LIMIT = before

    @pytest.fixture
    async def users(self, org_id, create_user):
        return [
            await create_user(org_id=org_id, error=error)
            for error in (None, 'some error')
            for _ in range(2)
        ]

    @pytest.fixture
    async def collectors(self, users, create_collector):
        collectors = []
        for user in users:
            if user.error is not None:
                continue
            for status in ('ok', 'not ok'):
                c = await create_collector(user_id=user.user_id, status=status)
                c.user = user
                collectors.append(c)
        return collectors

    @pytest.fixture
    def returned_func(self, org_id, users, collectors, only_errors):
        async def _inner(org_id=org_id, only_errors=only_errors):
            return await GetImportInfoAction(org_id=org_id, only_errors=only_errors).run()

        return _inner

    @pytest.fixture
    def expected(self, test_logger, users, collectors, only_errors):
        return sorted(
            chain(
                [  # All users with error
                    (u, None, UserImportError.get_error(user_error=u.error))
                    for u in users
                    if u.error is not None
                ],
                [  # All pairs of user, collector. Only error ones if only_errors is set.
                    (c.user, c, UserImportError.get_error(user_error=c.user.error, collector_status=c.status))
                    for c in collectors
                    if not only_errors or c.status != 'ok'
                ]
            ),
            key=lambda row: row[0].modified_at if row[1] is None else row[1].modified_at,
            reverse=True,
        )

    @pytest.mark.parametrize('only_errors', (True, False))
    def test_returned(self, returned, expected):
        assert returned[0] == expected

    @pytest.mark.parametrize('only_errors', (True, False))
    @pytest.mark.parametrize('limit', (0, 3, 10 ** 6))
    def test_has_more(self, returned, expected, limit):
        assert returned[1] == (limit < len(expected))
