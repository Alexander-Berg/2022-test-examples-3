import logging
from typing import List

import pytest
from db import Merchant, StorageContext

from sendr_aiopg.action import BaseDBAction
from sendr_aiopg.engine.lazy import Preset
from sendr_core import BaseAction
from sendr_core.exceptions import CoreFailError
from sendr_qlog import LoggerContext


@pytest.fixture
def logger_mock():
    return LoggerContext(logging.getLogger('mock_logger'), {})


@pytest.fixture(autouse=True)
def setup_context(logger_mock, storage_context):
    BaseAction.context = storage_context
    BaseAction.context.storage = None
    BaseAction.context.logger = logger_mock
    BaseDBAction.storage_context_cls = StorageContext


@pytest.fixture
def random_merchant(randn, rands):
    def _random_merchant_inner() -> Merchant:
        return Merchant(uid=randn(), name=rands())

    return _random_merchant_inner


class TransactAction(BaseDBAction):
    transact = True

    def __init__(self, merchants: List):
        super().__init__()
        self.merchants = merchants

    async def handle(self):
        for merchant in self.merchants:
            await self.storage.merchant.create(merchant)


class ForceCommitTransactAction(BaseDBAction):
    transact = True

    def __init__(self, merchant: Merchant):
        super().__init__()
        self.merchant = merchant

    async def handle(self):
        await self.storage.merchant.create(self.merchant)
        await self.storage.commit()
        raise CoreFailError


class RollbackTransactAction(BaseDBAction):
    transact = True

    def __init__(self, merchant: Merchant):
        super().__init__()
        self.merchant = merchant

    async def handle(self):
        await self.storage.merchant.create(self.merchant)
        await self.storage.rollback()
        raise CoreFailError


class ParentNonTransactAction(BaseDBAction):
    transact = False

    def __init__(self, merchants: List):
        super().__init__()
        self.merchants = merchants

    async def handle(self):
        await TransactAction(self.merchants).run()


class NonTransactAction(TransactAction):
    transact = False


class ParentTransactAction(BaseDBAction):
    transact = True

    def __init__(self, parent_merchant: Merchant, included_merchant: Merchant):
        super().__init__()
        self.parent_merchant = parent_merchant
        self.included_merchant = included_merchant

    async def handle(self):
        await TransactAction([self.included_merchant]).run()
        await self.storage.merchant.create(self.parent_merchant)


class ParentForForceCommitTransactAction(BaseDBAction):
    transact = True

    def __init__(self, parent_merchant: Merchant, included_merchant: Merchant):
        super().__init__()
        self.parent_merchant = parent_merchant
        self.included_merchant = included_merchant

    async def handle(self):
        await self.storage.merchant.create(self.parent_merchant)
        try:
            await ForceCommitTransactAction(self.included_merchant).run()
        except CoreFailError:
            pass
        await self.storage.merchant.create(self.parent_merchant)


class ParentForRollbackTransactAction(BaseDBAction):
    transact = True

    def __init__(self, parent_merchant: Merchant, included_merchant: Merchant):
        super().__init__()
        self.parent_merchant = parent_merchant
        self.included_merchant = included_merchant

    async def handle(self):
        await self.storage.merchant.create(self.parent_merchant)
        try:
            await RollbackTransactAction(self.included_merchant).run()
        except CoreFailError:
            pass


class TransactParentForNonTransactAction(BaseDBAction):
    transact = True

    def __init__(self, parent_merchant: Merchant, included_merchant: Merchant):
        super().__init__()
        self.parent_merchant = parent_merchant
        self.included_merchant = included_merchant

    async def handle(self):
        await self.storage.merchant.create(self.parent_merchant)
        await NonTransactAction([self.included_merchant]).run()
        raise CoreFailError


class AlternateTransactParentAction(BaseDBAction):
    transact = True

    def __init__(self, non_transact_merchants: List, transact_merchants: List):
        super().__init__()
        self.non_transact_merchants = non_transact_merchants
        self.transact_merchants = transact_merchants

    async def handle(self):
        await NonTransactAction([self.non_transact_merchants[0]]).run()
        await TransactAction([self.transact_merchants[0]]).run()
        await NonTransactAction([self.non_transact_merchants[1]]).run()
        await TransactAction([self.transact_merchants[1]]).run()
        raise CoreFailError


class NonTransactParentWithIncludedTransactAction(BaseDBAction):
    transact = False

    def __init__(self, non_transact_merchant: Merchant, transact_merchant: Merchant):
        super().__init__()
        self.non_transact_merchant = non_transact_merchant
        self.transact_merchant = transact_merchant

    async def handle(self):
        await TransactAction([self.transact_merchant]).run()
        await self.storage.merchant.create(self.non_transact_merchant)
        await self.storage.merchant.create(self.non_transact_merchant)


class StorageReplacingAction(BaseDBAction):
    transact = True

    def __init__(self, merchant: Merchant):
        super().__init__()
        self.merchant = merchant

    async def handle(self):
        async with self.storage_setter(transact=True):
            await self.storage.merchant.create(self.merchant)
        raise CoreFailError


async def should_all_be_exist(storage, *merchants):
    result = True
    for merchant in merchants:
        result = result and await is_exist(storage, merchant)

    return result


async def should_all_be_not_exist(storage, *merchants):
    for merchant in merchants:
        if await is_exist(storage, merchant):
            return False

    return True


async def is_exist(storage, merchant):
    try:
        return await storage.merchant.get(merchant.uid) == merchant
    except Merchant.DoesNotExist:
        return False


class TestTransactions:

    @pytest.fixture
    def same_key_merchants(self, random_merchant, rands):
        merchant = random_merchant()
        same_key_merchant = Merchant(uid=merchant.uid, name=rands())
        return [merchant, same_key_merchant]

    @pytest.mark.asyncio
    async def test_non_transaction(self, same_key_merchants, storage):
        try:
            await NonTransactAction(same_key_merchants).run()
        except CoreFailError:
            pass

        assert await should_all_be_exist(storage, same_key_merchants[0])

    @pytest.mark.parametrize(('action',), (
        pytest.param(TransactAction, id='single_transaction'),
        pytest.param(ParentNonTransactAction, id='parent_action_does_not_break_included_transaction'),
    ))
    @pytest.mark.asyncio
    async def test_transaction(self, action, same_key_merchants, storage):
        try:
            await action(same_key_merchants).run()
        except CoreFailError:
            pass

        assert await should_all_be_not_exist(storage, same_key_merchants[0])

    @pytest.mark.asyncio
    async def test_can_commit_manually_and_prevent_rollback_from_exceptions(self, storage, random_merchant):
        merchant = random_merchant()
        try:
            await ForceCommitTransactAction(merchant).run()
        except CoreFailError:
            pass

        assert await should_all_be_exist(storage, merchant)

    @pytest.mark.asyncio
    async def test_included_transaction_force_commit_does_not_commit_parent(self, storage, random_merchant):
        parent_merchant = random_merchant()
        included_merchant = random_merchant()
        try:
            await ParentForForceCommitTransactAction(parent_merchant, included_merchant).run()
        except CoreFailError:
            pass

        assert await should_all_be_not_exist(storage, parent_merchant, included_merchant)

    @pytest.mark.asyncio
    async def test_can_rollback_included_transaction_in_isolation(self, storage, random_merchant):
        parent_merchant = random_merchant()
        included_merchant = random_merchant()
        await ParentForRollbackTransactAction(parent_merchant, included_merchant).run()

        assert (
            await should_all_be_exist(storage, parent_merchant)
            and await should_all_be_not_exist(storage, included_merchant)
        )

    @pytest.mark.asyncio
    async def test_included_transaction_is_not_committed_if_parent_fails(self, storage, random_merchant):
        single_merchant = random_merchant()
        try:
            await ParentTransactAction(single_merchant, single_merchant).run()
        except CoreFailError:
            pass

        assert await should_all_be_not_exist(storage, single_merchant)

    @pytest.mark.asyncio
    async def test_included_non_transaction_action_is_executed_in_parents_transaction(self,
                                                                                      storage,
                                                                                      random_merchant):
        parent_merchant = random_merchant()
        included_merchant = random_merchant()
        try:
            await TransactParentForNonTransactAction(parent_merchant, included_merchant).run()
        except CoreFailError:
            pass

        assert await should_all_be_not_exist(storage, parent_merchant, included_merchant)

    @pytest.mark.asyncio
    async def test_alternate_transactions_chain(self, storage, random_merchant):
        merchants = [random_merchant() for _ in range(4)]
        try:
            await AlternateTransactParentAction(merchants[:2], merchants[2:]).run()
        except CoreFailError:
            pass

        assert await should_all_be_not_exist(storage, *merchants)

    @pytest.mark.asyncio
    async def test_non_transaction_parent_is_not_executed_in_included_transaction(self, storage, random_merchant):
        non_transact_merchant = random_merchant()
        transact_merchant = random_merchant()
        try:
            await NonTransactParentWithIncludedTransactAction(non_transact_merchant, transact_merchant).run()
        except CoreFailError:
            pass

        assert await should_all_be_exist(storage, non_transact_merchant)

    @pytest.mark.asyncio
    async def test_can_run_action_in_different_transaction_by_replacing_storage(self,
                                                                                storage,
                                                                                random_merchant):
        merchant = random_merchant()
        try:
            await StorageReplacingAction(merchant).run()
        except CoreFailError:
            pass

        assert await should_all_be_exist(storage, merchant)


class DummyAction(BaseDBAction):
    async def pre_handle(self):
        if not hasattr(self.storage.conn, 'conn_preset'):
            # mock connection preset
            preset = Preset.ACTUAL_LOCAL if self.allow_replica_read else Preset.MASTER
            self.storage.conn.conn_preset = preset

    async def handle(self):
        return self.storage.conn


class DummyWriteAction(DummyAction):
    allow_replica_read = False


class DummyReadAction(DummyAction):
    allow_replica_read = True


class OuterWriteAction(DummyAction):
    allow_replica_read = False

    async def handle(self):
        inner_read = await DummyReadAction().run()
        inner_write = await DummyWriteAction().run()
        return self.storage.conn, inner_read, inner_write


class OuterReadAction(OuterWriteAction):
    allow_replica_read = True


class TestReuseConnection:
    @pytest.mark.asyncio
    async def test_only_read_connection_reused_from_outer_read_action(self):
        action = OuterReadAction()
        outer_read_conn, inner_read_conn, inner_write_conn = await action.run()

        assert inner_read_conn is not None
        assert inner_read_conn is outer_read_conn
        assert inner_read_conn.conn_preset == Preset.ACTUAL_LOCAL

        assert inner_write_conn is not None
        assert inner_write_conn is not outer_read_conn
        assert inner_write_conn.conn_preset == Preset.MASTER

    @pytest.mark.asyncio
    async def test_connection_always_reused_from_outer_write_action(self):
        action = OuterWriteAction()
        outer_write_conn, inner_read_conn, inner_write_conn = await action.run()

        assert outer_write_conn is not None
        assert inner_read_conn is outer_write_conn
        assert inner_write_conn is outer_write_conn
        assert outer_write_conn.conn_preset == Preset.MASTER

    @pytest.mark.asyncio
    async def test_connection_never_reused_if_flag_is_off(self, mocker):
        mocker.patch.object(DummyAction, 'allow_connection_reuse', False)

        action = OuterWriteAction()
        outer_write_conn, inner_read_conn, inner_write_conn = await action.run()

        assert outer_write_conn is not None
        assert inner_read_conn is not None
        assert inner_read_conn is not outer_write_conn
        assert inner_write_conn is not None
        assert inner_write_conn is not outer_write_conn
