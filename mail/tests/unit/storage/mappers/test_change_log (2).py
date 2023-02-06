from random import choice

import pytest

from mail.payments.payments.core.entities.change_log import ChangeLog
from mail.payments.payments.core.entities.enums import OperationKind
from mail.payments.payments.storage.db.tables import metadata
from mail.payments.payments.utils.datetime import utcnow


class TestChangeLogMapper:
    @pytest.fixture
    def now(self, mocker):
        now = utcnow()
        mocker.patch('mail.payments.payments.storage.mappers.change_log.utcnow', mocker.Mock(return_value=now))
        return now

    @pytest.fixture
    def change_log(self):
        return ChangeLog(
            uid=123,
            revision=456,
            operation=OperationKind.START_MERCHANT_MODERATION,
            arguments={'x': 'y'},
            info={'z': '0'},
            changed_at=utcnow(),
        )

    @pytest.fixture
    def change_log_dict(self, change_log):
        return {
            attr: getattr(change_log, attr)
            for attr in [
                'uid',
                'revision',
                'operation',
                'arguments',
                'info',
                'changed_at',
            ]
        }

    async def _count_change_log(self, storage):
        result = await storage.conn.execute(f'SELECT COUNT(*) FROM {metadata.schema}.change_log')
        return (await result.first())[0]

    @pytest.mark.asyncio
    async def test_map(self, storage, change_log, change_log_dict):
        assert storage.change_log.map(change_log_dict) == change_log

    @pytest.mark.asyncio
    async def test_unmap(self, storage, change_log, change_log_dict):
        change_log_dict.pop('changed_at')
        assert storage.change_log.unmap(change_log) == change_log_dict

    @pytest.mark.asyncio
    async def test_create_returns(self, storage, change_log, now):
        change_log.changed_at = now
        assert await storage.change_log.create(change_log) == change_log

    @pytest.mark.asyncio
    async def test_create_writes(self, storage, change_log):
        before = await self._count_change_log(storage)
        await storage.change_log.create(change_log)
        after = await self._count_change_log(storage)
        assert after - before == 1

    class TestFind:
        @pytest.fixture
        def change_log_number(self):
            return 5

        @pytest.fixture(autouse=True)
        def sort_change_logs(self, change_logs):
            change_logs.sort(key=lambda cl: cl.changed_at, reverse=True)

        @pytest.fixture
        def find(self, storage):
            async def _inner(**kwargs):
                return [cl async for cl in storage.change_log.find(**kwargs)]

            return _inner

        @pytest.mark.asyncio
        async def test_order_by_changed_at_desc(self, change_logs, find):
            assert await find() == change_logs

        @pytest.mark.asyncio
        async def test_limit(self, change_logs, find):
            assert await find(limit=2) == change_logs[:2]

        @pytest.mark.asyncio
        async def test_offset(self, change_logs, find):
            assert await find(offset=2) == change_logs[2:]

        @pytest.mark.asyncio
        async def test_filter_by_uid(self, change_logs, find):
            uid = choice(change_logs).uid
            assert await find(uid=uid) == [cl for cl in change_logs if cl.uid == uid]

        @pytest.mark.asyncio
        async def test_filter_by_changed_at(self, change_logs, find):
            to_ = change_logs[1].changed_at
            from_ = change_logs[3].changed_at
            assert await find(changed_at_from=from_, changed_at_to=to_) == change_logs[1 + 1:3 + 1]
