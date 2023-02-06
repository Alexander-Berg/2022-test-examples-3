from datetime import timedelta

import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, equal_to

from mail.ipa.ipa.core.entities.collector import Collector
from mail.ipa.ipa.core.entities.import_params import ImportParams
from mail.ipa.ipa.storage.exceptions import CollectorNotFound


@pytest.fixture
def collector_entity(user):
    return Collector(
        user_id=user.user_id,
        params=ImportParams(
            server='example.test',
            port=993,
            src_login='admin@example.test',
            ssl=False,
            imap=True,
            mark_archive_read=True,
            delete_msgs=True,
        )
    )


@pytest.fixture(autouse=True)
def func_now(mocker, make_now):
    now = make_now()
    mocker.patch('mail.ipa.ipa.storage.mappers.collector.mapper.func.now', mocker.Mock(return_value=now))
    return now


@pytest.fixture
async def collector(storage, collector_entity):
    return await storage.collector.create(collector_entity)


@pytest.fixture
async def collectors(storage, collector_entity):
    return [
        await storage.collector.create(collector_entity),
        await storage.collector.create(collector_entity),
    ]


@pytest.mark.asyncio
class TestCollectorMapper:
    async def test_create(self, storage, collector_entity):
        collector = await storage.collector.create(collector_entity)
        collector_entity.collector_id = collector.collector_id
        assert_that(collector, equal_to(collector_entity))

    async def test_get(self, storage, collector):
        actual_collector = await storage.collector.get(collector.collector_id)
        assert_that(actual_collector, equal_to(collector))

    async def test_get_with_user(self, storage, collector, user):
        actual_collector = await storage.collector.get(collector.collector_id, True)
        collector.user = user
        assert_that(actual_collector, equal_to(collector))

    async def test_get_not_found(self, storage):
        with pytest.raises(CollectorNotFound):
            await storage.collector.get(1)

    async def test_save(self, storage, collector, make_now):
        collector.popid = '100001'
        collector.checked_at = make_now()
        collector.enabled = False
        updated_collector = await storage.collector.save(collector)
        assert_that(updated_collector, equal_to(collector))

    async def test_save_updates_modified_at(self, storage, collector, func_now):
        collector.modified_at = func_now - timedelta(minutes=5)
        updated_collector = await storage.collector.save(collector)
        assert_that(updated_collector.modified_at, equal_to(func_now))

    async def test_find_statuses(self, storage, create_collector, user, rands, randn):
        statuses = sorted([rands().lower() for _ in range(randn(min=5, max=10))])
        for status in statuses:
            await create_collector(user.user_id, status=status)

        returned = await alist(storage.collector.find_statuses(user.org_id))
        assert returned == statuses

    @pytest.mark.asyncio
    class TestFind:
        @pytest.fixture
        async def users(self, org_id, other_org_id, randn, create_user):
            return [
                await create_user(org_id)
                for current_org_id in (org_id, other_org_id)
                for _ in range(3)
            ]

        @pytest.fixture
        async def collectors(self, users, create_collector):
            collectors = []
            for user in users:
                for status in ('ok', 'not ok'):
                    collector = await create_collector(user.user_id, status=status)
                    collector.user = user
                    collectors.append(collector)
            return collectors

        @pytest.fixture
        def returned_func(self, storage):
            async def _inner(*args, **kwargs):
                return await alist(storage.collector.find(*args, **kwargs))

            return _inner

        async def test_all(self, collectors, returned_func):
            assert_that(
                await returned_func(),
                contains_inanyorder(*collectors),
            )

        async def test_login_filter(self, randitem, randslice, users, collectors, returned_func):
            user = randitem(users)

            assert_that(
                await returned_func(login=randslice(user.login, min_length=3)),
                contains_inanyorder(*[c for c in collectors if c.user_id == user.user_id])
            )

        async def test_status_filter(self, randitem, collectors, returned_func):
            collector = randitem(collectors)

            assert_that(
                await returned_func(status=collector.status),
                contains_inanyorder(*[c for c in collectors if c.status == collector.status])
            )

        async def test_org_id_filter(self, org_id, collectors, returned_func):
            assert_that(
                await returned_func(org_id=org_id),
                contains_inanyorder(*[c for c in collectors if c.user.org_id == org_id])
            )

        @pytest.mark.parametrize('ok_status', (False, True))
        async def test_ok_status_filter(self, collectors, returned_func, ok_status):
            assert_that(
                await returned_func(ok_status=ok_status),
                contains_inanyorder(*[c for c in collectors if (c.status == 'ok') == ok_status])
            )

        @pytest.mark.parametrize('limit_delta', (-1, 0, 1))
        async def test_limit(self, collectors, returned_func, limit_delta):
            limit = len(collectors) + limit_delta
            assert len(await returned_func(limit=limit)) == min(limit, len(collectors))

        @pytest.mark.parametrize('desc', (True, False))
        async def test_order(self, collectors, returned_func, desc):
            assert await returned_func(order_by='collector_id', desc=desc) == sorted(
                collectors,
                key=lambda c: c.collector_id,
                reverse=desc,
            )

        async def test_user_id_filter(self, returned_func, collectors, users):
            user = users[0]
            assert_that(
                await returned_func(user_id=user.user_id),
                contains_inanyorder(*[c for c in collectors if c.user_id == user.user_id]),
            )

    async def test_remove_user_collectors(self, storage, collectors, user):
        await storage.collector.remove_user_collectors(user.user_id)
        assert_that(
            [collector async for collector in storage.collector.find(user_id=user.user_id)],
            equal_to([]),
        )

    class TestGetMinCheckedAt:
        @pytest.fixture
        def times(self, make_now):
            now = make_now()
            return {
                'active': [now - timedelta(minutes=1), now - timedelta(minutes=10)],
                'no_pop_id': now - timedelta(minutes=20),
                'not_enabled': now - timedelta(minutes=20),
            }

        @pytest.fixture(autouse=True)
        async def create_collectors(self, times, create_collector, user, storage, rands):
            for time in times['active']:
                await create_collector(user.user_id, pop_id=rands(), checked_at=time)
            await create_collector(user.user_id, checked_at=times['no_pop_id'], pop_id=None, enabled=True)
            await create_collector(user.user_id, checked_at=times['no_pop_id'], pop_id=rands(), enabled=False)

        @pytest.mark.asyncio
        async def test_get_min_checked_at(self, storage, times):
            assert_that(
                await storage.collector.get_min_checked_at(),
                equal_to(min(times['active'])),
            )
