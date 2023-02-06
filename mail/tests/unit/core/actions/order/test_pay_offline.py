import pytest

from hamcrest import all_of, assert_that, has_properties, is_

from mail.payments.payments.core.actions.order.pay_offline import CorePayOfflineOrderAction
from mail.payments.payments.core.entities.enums import OrderKind, OrderSource, PayStatus
from mail.payments.payments.core.entities.log import OrderStatusUpdatedLog
from mail.payments.payments.core.exceptions import (
    OrderAbandonedError, OrderAlreadyHaveTransactions, OrderAlreadyPaidError, OrderArchivedError,
    OrderRefundCannotBePaidError
)
from mail.payments.payments.tests.base import BaseAcquirerTest


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestPayOffline(BaseAcquirerTest):
    @pytest.fixture
    def extra_params(self):
        return {}

    @pytest.fixture
    def params(self, merchant, randn, rands, order, extra_params):
        return {
            'order_id': order.order_id,
            'uid': merchant.uid,
            'description': rands(),
            'customer_uid': randn(),
            **extra_params
        }

    @pytest.fixture
    def action(self, params):
        return CorePayOfflineOrderAction(**params)

    @pytest.fixture
    def returned_func(self, action):
        async def _inner():
            return await action.run()

        return _inner

    def test_result(self, params, returned):
        assert all((
            returned.pay_status == PayStatus.PAID,
            returned.paymethod_id == 'offline',
            returned.user_description == params['description'],
            returned.customer_uid == params['customer_uid'],
            returned.closed is not None
        ))

    def test_logged(self, returned, merchant, pushers_mock):
        assert_that(
            pushers_mock.log.push.call_args[0][0],
            all_of(
                is_(OrderStatusUpdatedLog),
                has_properties(dict(
                    merchant_name=merchant.name,
                    merchant_uid=merchant.uid,
                    merchant_acquirer=merchant.acquirer,
                    order_id=returned.order_id,
                    status=PayStatus.PAID.value,
                    customer_uid=returned.customer_uid,
                    service_id=None,
                    service_name=None,
                    sdk_api_created=returned.created_by_source == OrderSource.SDK_API,
                    sdk_api_pay=returned.pay_by_source == OrderSource.SDK_API,
                    created_by_source=returned.created_by_source,
                    pay_by_source=returned.pay_by_source,
                    price=returned.log_price
                )),
            ),
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('pay_status', (PayStatus.PAID, PayStatus.IN_MODERATION, PayStatus.IN_PROGRESS))
    async def test_pay_status(self, storage, returned_func, pay_status, order):
        order.pay_status = pay_status
        await storage.order.save(order)
        with pytest.raises(OrderAlreadyPaidError):
            await returned_func()

    @pytest.mark.asyncio
    async def test_pay_status_abandoned(self, storage, returned_func, order):
        order.pay_status = PayStatus.ABANDONED
        await storage.order.save(order)
        with pytest.raises(OrderAbandonedError):
            await returned_func()

    @pytest.mark.asyncio
    async def test_already_have_transactions(self, storage, transaction, returned_func, order):
        with pytest.raises(OrderAlreadyHaveTransactions):
            await returned_func()

    @pytest.mark.asyncio
    async def test_archive(self, storage, returned_func, order):
        order.active = False
        await storage.order.save(order)

        with pytest.raises(OrderArchivedError):
            await returned_func()

    @pytest.mark.asyncio
    async def test_send_to_history_task_created(self, storage, order, returned):
        task = await (storage.task.find()).__anext__()
        assert all((
            task.action_name == 'send_to_history_order_action',
            task.params['action_kwargs'] == {'uid': order.uid, 'order_id': order.order_id},
        ))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('extra_params', ({'service_data': {'custom': 'json'}},))
    async def test_service_data_saved(self, storage, order, params, returned):
        order = await storage.order.get(order.uid, order.order_id)
        assert params['service_data'] == order.data.service_data \
            and params['service_data'] == returned.data.service_data

    @pytest.mark.asyncio
    @pytest.mark.parametrize('extra_params', ({'service_data': None},))
    async def test_service_data_none_ignored(self, rands, storage, order, returned_func):
        service_data = order.data.service_data = {rands(): rands()}
        await storage.order.save(order)
        returned = await returned_func()
        order = await storage.order.get(order.uid, order.order_id)
        assert service_data == order.data.service_data and service_data == returned.data.service_data

    class TestKindMulti:
        @pytest.fixture(params=(OrderKind.MULTI, OrderKind.REFUND))
        def extra_params(self, request, refund, multi_order):
            if request.param == OrderKind.MULTI:
                return {'order_id': multi_order.order_id}
            elif request.param == OrderKind.REFUND:
                return {'order_id': refund.order_id}

        @pytest.mark.asyncio
        async def test_kind(self, storage, returned_func, order):
            with pytest.raises(OrderRefundCannotBePaidError):
                await returned_func()
