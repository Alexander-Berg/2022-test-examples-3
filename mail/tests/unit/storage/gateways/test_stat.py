import pytest

from mail.ipa.ipa.core.entities.stat import CollectorStat, UserStat


@pytest.fixture
async def users(rands, org_id, other_org_id, create_user):
    users = []
    for current_org_id in (org_id, other_org_id):
        for _ in range(2):  # ok users
            users.append(await create_user(org_id=current_org_id))
        for _ in range(2):  # error users
            users.append(await create_user(org_id=current_org_id, error=rands()))
    return users


@pytest.fixture
def org_users(org_id, users):
    return [u for u in users if u.org_id == org_id]


class TestGetUserStat:
    @pytest.fixture
    async def returned(self, storage, org_id):
        return await storage.stat.get_user_stat(org_id)

    def test_empty(self, org_id, returned):
        assert returned == UserStat(errors=0)

    def test_returned(self, org_id, org_users, returned):
        assert returned == UserStat(
            errors=len([
                user
                for user in org_users
                if user.error is None
            ])
        )


class TestGetCollectorStat:
    ERROR_STATUS = 'not ok'

    @pytest.fixture
    async def collectors(self, users, create_collector):
        collectors = []
        for user in users:
            collectors.append(await create_collector(user.user_id))
            collectors.append(await create_collector(user.user_id, enabled=False))
            collectors.append(await create_collector(user.user_id, status=self.ERROR_STATUS))
        return collectors

    @pytest.fixture
    async def error_collectors(self, collectors):
        return [c for c in collectors if c.status == self.ERROR_STATUS]

    @pytest.fixture
    async def finished_collectors(self, randn, users, create_collector):
        collectors = []
        for user in users:
            collected = randn()
            errors = randn()
            collectors.append(await create_collector(
                user_id=user.user_id, collected=collected, errors=errors, total=collected + errors))
        return collectors

    @pytest.fixture
    def get_org_collectors(self, org_users):
        def _inner(collectors):
            org_user_ids = {u.user_id for u in org_users}
            return [c for c in collectors if c.user_id in org_user_ids]

        return _inner

    @pytest.fixture
    def returned_func(self, storage, org_id):
        async def _inner():
            return await storage.stat.get_collector_stat(org_id)

        return _inner

    def test_empty(self, org_id, returned):
        assert returned == CollectorStat(
            total=0,
            errors=0,
            finished=0,
        )

    def test_total(self, org_id, collectors, get_org_collectors, returned):
        assert returned.total == len(get_org_collectors(collectors))

    def test_errors(self, storage, rands, error_collectors, get_org_collectors, returned):
        assert returned.errors == len(get_org_collectors(error_collectors))

    def test_finished(self, storage, collectors, finished_collectors, get_org_collectors, returned):
        assert returned.finished == len(get_org_collectors(finished_collectors))

    @pytest.mark.asyncio
    async def test_error_not_finished(self, randn, storage, user, create_collector, returned_func):
        collected = randn()
        errors = randn()
        await create_collector(
            user_id=user.user_id,
            collected=collected,
            errors=errors,
            total=collected + errors,
            status=self.ERROR_STATUS,
        )
        assert await returned_func() == CollectorStat(
            total=1,
            errors=1,
            finished=0,
        )
