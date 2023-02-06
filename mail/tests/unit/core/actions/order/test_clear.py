import pytest

from mail.payments.payments.core.actions.order.clear import ClearByIdsOrderAction, ClearOrderAction
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.exceptions import OrdersAmountExceed
from mail.payments.payments.tests.base import BaseAcquirerTest, BaseOrderAcquirerTest, parametrize_shop_type
from mail.payments.payments.utils.helpers import without_none


class TestClearOrderAction(BaseAcquirerTest, BaseOrderAcquirerTest):
    @pytest.fixture(autouse=True)
    def get_acquirer_mock(self, mock_action, acquirer):
        from mail.payments.payments.core.actions.merchant.get_acquirer import GetAcquirerMerchantAction
        return mock_action(GetAcquirerMerchantAction, acquirer)

    @pytest.fixture
    def multi_max_amount(self):
        return None

    @pytest.fixture
    def order_data_data(self, multi_max_amount):
        return without_none({
            'trust_template': 'desktop',
            'multi_max_amount': multi_max_amount,
        })

    @pytest.fixture(autouse=True)
    def trust_clear_mock(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'payment_clear') as mock:
            yield mock

    @pytest.fixture
    def params(self, transaction, order):
        return {
            'transaction': transaction,
            'order': order,
        }

    @pytest.fixture
    def action(self):
        return ClearOrderAction

    @pytest.fixture
    def returned_func(self, action, params):
        async def _inner():
            return await action(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @parametrize_shop_type
    def test_clear(self, trust_clear_mock, transaction, returned, order_acquirer):
        trust_clear_mock.assert_called_once_with(
            uid=transaction.uid,
            acquirer=order_acquirer,
            purchase_token=transaction.trust_purchase_token,
        )

    @pytest.mark.parametrize('multi_max_amount', (0,))
    @pytest.mark.asyncio
    async def test_amount_exceeded(self, multi_order, returned_func):
        with pytest.raises(OrdersAmountExceed):
            await returned_func()

    @pytest.mark.asyncio
    async def test_issued_inc(self, storage, multi_order, returned_func):
        multi_order: Order = await storage.order.get(multi_order.uid, multi_order.order_id)
        multi_issued_before = multi_order.data.multi_issued

        await returned_func()

        multi_order: Order = await storage.order.get(multi_order.uid, multi_order.order_id)
        multi_issued_after = multi_order.data.multi_issued

        assert multi_issued_before == 0 and multi_issued_after == 1


class TestClearByIdsOrderAction:
    @pytest.fixture
    def params(self, transaction, order):
        return {
            'uid': order.uid,
            'order_id': order.order_id,
            'tx_id': transaction.tx_id,
        }

    @pytest.fixture
    def action_clear_mock(self, mock_action):
        return mock_action(ClearOrderAction)

    @pytest.fixture
    async def returned(self, params):
        return await ClearByIdsOrderAction(**params).run()

    def test_clear_call(self, action_clear_mock, order, transaction, returned):
        action_clear_mock.assert_called_once_with(
            order=order,
            transaction=transaction
        )
