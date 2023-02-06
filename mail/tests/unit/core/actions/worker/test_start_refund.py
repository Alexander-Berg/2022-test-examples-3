import pytest

from mail.payments.payments.core.actions.worker.start_refund import StartRefundAction
from mail.payments.payments.core.entities.enums import OrderKind, RefundStatus, TaskType
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.task import Task
from mail.payments.payments.core.exceptions import CoreFailError, CoreInteractionFatalError
from mail.payments.payments.interactions.trust.exceptions import TrustException
from mail.payments.payments.tests.base import BaseOrderAcquirerTest, parametrize_merchant_oauth_mode


class TestStartRefund(BaseOrderAcquirerTest):
    @pytest.fixture(autouse=True)
    def get_acquirer_mock(self, mock_action, acquirer):
        from mail.payments.payments.core.actions.merchant.get_acquirer import GetAcquirerMerchantAction
        return mock_action(GetAcquirerMerchantAction, acquirer)

    @pytest.fixture(autouse=True)
    def refund_start_mock(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'refund_start', None) as mock:
            yield mock

    @pytest.fixture
    async def refund(self, storage, order, rands):
        return await storage.order.create(Order(
            uid=order.uid,
            shop_id=order.shop_id,
            original_order_id=order.order_id,
            kind=OrderKind.REFUND,
            pay_status=None,
            refund_status=RefundStatus.CREATED,
            trust_refund_id=rands(),
            acquirer=order.acquirer,
        ))

    @pytest.fixture
    def params(self, refund):
        return {'uid': refund.uid, 'order_id': refund.order_id}

    @pytest.fixture
    async def task(self, storage, params):
        return await storage.task.create(Task(
            task_type=TaskType.START_REFUND,
            params=params
        ))

    @pytest.fixture
    async def returned(self, params):
        return await StartRefundAction(**params).run()

    @pytest.mark.asyncio
    async def test_fails_on_bad_refund_status(self, storage, refund, params):
        refund.refund_status = RefundStatus.REQUESTED
        await storage.order.save(refund)
        with pytest.raises(CoreFailError):
            await StartRefundAction(**params).run()

    @pytest.mark.asyncio
    async def test_exception(self, shop_type, trust_client_mocker, params):
        with pytest.raises(CoreInteractionFatalError):
            with trust_client_mocker(shop_type, 'refund_start', exc=TrustException(method='POST')):
                await StartRefundAction(**params).run()

    @pytest.mark.asyncio
    async def test_updates_refund(self, storage, refund, returned):
        updated = await storage.order.get(refund.uid, refund.order_id)
        assert all([
            updated.revision > refund.revision,
            updated.refund_status == RefundStatus.REQUESTED,
        ])

    @parametrize_merchant_oauth_mode
    def test_refund_start_call(self, refund_start_mock, refund, returned, order_acquirer):
        refund_start_mock.assert_called_once_with(
            uid=refund.uid,
            acquirer=order_acquirer,
            refund_id=refund.trust_refund_id
        )

    @pytest.mark.asyncio
    async def test_send_to_history_task_created(self, storage, order, returned):
        task = await (storage.task.find()).__anext__()
        assert all((
            task.action_name == 'send_to_history_order_action',
            task.params['action_kwargs'] == {'uid': order.uid, 'order_id': order.order_id},
        ))
