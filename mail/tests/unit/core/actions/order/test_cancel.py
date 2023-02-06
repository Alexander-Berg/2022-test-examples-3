import pytest

from sendr_utils import alist

from hamcrest import all_of, assert_that, contains_inanyorder, has_entries, has_properties, is_

from mail.payments.payments.core.actions.order.cancel import (
    CancelOrderAction, CancelOrderServiceMerchantAction, CoreCancelOrderAction
)
from mail.payments.payments.core.entities.enums import OperationKind, PayStatus, TaskType, TransactionStatus
from mail.payments.payments.core.entities.log import OrderUpdatedLog
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.core.exceptions import (
    OrderAbandonedError, OrderAlreadyPaidError, OrderArchivedError, OrderCancelledError,
    OrderHaveUnfinishedTransactions
)
from mail.payments.payments.tests.base import BaseTestOrderAction


class TestCoreCancelOrderAction(BaseTestOrderAction):
    @pytest.fixture
    def order(self, order):
        assert order.pay_status == PayStatus.NEW
        return order

    @pytest.fixture
    def action(self, crypto, order):
        CoreCancelOrderAction.context.crypto = crypto
        return CoreCancelOrderAction(uid=order.uid, order_id=order.order_id)

    @pytest.fixture
    def returned_func(self, action):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.fixture
    def transaction_status(self):
        return None

    @pytest.fixture(autouse=True)
    async def transaction(self, transaction_status, order, storage):
        if transaction_status is None:
            return None
        return await storage.transaction.create(Transaction(
            uid=order.uid,
            order_id=order.order_id,
            status=transaction_status,
        ))

    @pytest.fixture
    async def order_updated(self, returned, storage):
        return await storage.order.get(uid=returned.uid, order_id=returned.order_id)

    @pytest.mark.parametrize('transaction_status', (None, TransactionStatus.CANCELLED))
    def test_order_is_cancelled(self, order_updated):
        assert all((
            order_updated.pay_status == PayStatus.CANCELLED,
            order_updated.closed is not None
        ))

    @pytest.mark.asyncio
    @pytest.mark.parametrize(
        'transaction_status',
        {TransactionStatus.CLEARED} | set(TransactionStatus) - set(TransactionStatus.FINAL_STATUSES)
    )
    async def test_raises_on_bad_transaction_status_error(self, transaction_status, returned_func):
        exc = (
            OrderAlreadyPaidError
            if transaction_status == TransactionStatus.CLEARED
            else OrderHaveUnfinishedTransactions
        )
        with pytest.raises(exc):
            await returned_func()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('pay_status,exc', [
        *[(pay_status, OrderAlreadyPaidError) for pay_status in PayStatus.ALREADY_PAID_STATUSES],
        (PayStatus.CANCELLED, OrderCancelledError),
        (PayStatus.ABANDONED, OrderAbandonedError)
    ])
    async def test_raises_on_pay_status_error(self, pay_status, exc, order, storage, returned_func):
        order.pay_status = pay_status
        await storage.order.save(order)

        with pytest.raises(exc):
            await returned_func()

    @pytest.mark.asyncio
    async def test_raises_on_active_error(self, order, storage, returned_func):
        order.active = False
        await storage.order.save(order)

        with pytest.raises(OrderArchivedError):
            await returned_func()

    def test_logged(self, returned, pushers_mock):
        assert_that(
            pushers_mock.log.push.call_args[0][0],
            all_of(
                is_(OrderUpdatedLog),
                has_properties(dict(
                    status=PayStatus.CANCELLED.value,
                ))
            )
        )

    @pytest.mark.asyncio
    async def test_create_callback_task(self, order_with_service, storage, returned):
        assert_that(
            await alist(storage.task.find(TaskType.API_CALLBACK)),
            contains_inanyorder(*[
                has_properties({
                    'params': has_entries({
                        'message': has_entries({
                            'order_id': returned.order_id,
                            'new_status': PayStatus.CANCELLED.value
                        })
                    }),
                }) for _ in range(2)
            ])
        )

    @pytest.mark.asyncio
    async def test_changelog(self, storage, returned):
        assert_that(
            await alist(storage.change_log.find(returned.uid)),
            contains_inanyorder(
                has_properties({
                    'uid': returned.uid,
                    'revision': returned.revision,
                    'operation': OperationKind.UPDATE_ORDER,
                    'arguments': {'pay_status': PayStatus.CANCELLED.value, 'order_id': returned.order_id}
                })
            )
        )


class TestCancelOrderAction:
    @pytest.fixture
    def mock_core_cancel_order_action_result(self, rands):
        return {rands(): rands()}

    @pytest.fixture(autouse=True)
    def mock_core_cancel_order_action(self, mock_action, mock_core_cancel_order_action_result):
        return mock_action(CoreCancelOrderAction, mock_core_cancel_order_action_result)

    @pytest.fixture
    def params(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await CancelOrderAction(**params).run()

        return _inner

    def test_call(self, returned, mock_core_cancel_order_action, params):
        mock_core_cancel_order_action.assert_called_once_with(**params)


class TestCancelOrderServiceMerchantAction:
    @pytest.fixture
    def mock_core_cancel_order_action_result(self, rands):
        return {rands(): rands()}

    @pytest.fixture(autouse=True)
    def mock_core_cancel_order_action(self, mock_action, mock_core_cancel_order_action_result):
        return mock_action(CoreCancelOrderAction, mock_core_cancel_order_action_result)

    @pytest.fixture
    def proxy_kwargs(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def params(self, service_merchant, service_client, customer_subscription, proxy_kwargs):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': service_client.tvm_id,
            **proxy_kwargs
        }

    @pytest.fixture
    def action(self, storage, params):
        return CancelOrderServiceMerchantAction(**params)

    @pytest.fixture
    def returned_func(self, action, params):
        async def _inner():
            return await action.run()

        return _inner

    def test_call(self, service_merchant, action, returned, mock_core_cancel_order_action, proxy_kwargs):
        mock_core_cancel_order_action.assert_called_once_with(
            uid=service_merchant.uid,
            **proxy_kwargs
        )
