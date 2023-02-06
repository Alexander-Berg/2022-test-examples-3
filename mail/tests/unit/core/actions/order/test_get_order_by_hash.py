import pytest

from mail.payments.payments.core.actions.order.get_by_hash import GetOrderByHashAction
from mail.payments.payments.core.entities.enums import TransactionStatus
from mail.payments.payments.core.entities.moderation import ModerationData
from mail.payments.payments.core.exceptions import OrderNotFoundError
from mail.payments.payments.tests.base import BaseTestOrderHashAction


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestGetOrderByHash(BaseTestOrderHashAction):
    @pytest.fixture
    def merchant_moderation(self):
        return ModerationData(approved=False, has_moderation=False, has_ongoing=False, reasons=[])

    @pytest.fixture
    def params(self, some_hash, crypto):
        GetOrderByHashAction.context.crypto = crypto
        return {
            'hash_': some_hash,
        }

    @pytest.fixture
    async def returned(self, params):
        return await GetOrderByHashAction(**params).run()

    @pytest.mark.asyncio
    async def test_masks_exception(self, crypto, params):
        crypto.decrypt_order = lambda: None
        with pytest.raises(OrderNotFoundError):
            await GetOrderByHashAction(**params).run()

    @pytest.mark.parametrize('decrypted_update', [
        {'uid': -1},
        {'order_id': -1},
    ])
    @pytest.mark.asyncio
    async def test_masked_not_found(self, decrypted, params, decrypted_update):
        decrypted.update(decrypted_update)
        with pytest.raises(OrderNotFoundError):
            await GetOrderByHashAction(**params).run()

    def test_returned_merchant(self, merchant, returned):
        assert returned['merchant'] == merchant

    def test_returned_order(self, order, items, some_hash, returned):
        returned_order = returned['order']
        order.order_hash = some_hash
        assert returned_order == order

    def test_returned_transaction_none(self, returned):
        assert returned['transaction'] is None

    def test_returned_transaction(self, transaction, returned):
        assert returned['transaction'] == transaction

    @pytest.mark.parametrize('tx_status', [
        TransactionStatus.ACTIVE,
        TransactionStatus.HELD,
    ])
    @pytest.mark.asyncio
    async def test_updates_transaction(self, storage, transaction, update_transaction_mock, params, tx_status):
        transaction.status = tx_status
        transaction = await storage.transaction.save(transaction)
        await GetOrderByHashAction(**params).run()
        update_transaction_mock.assert_called_once()

    class TestWithCustomerSubscription:
        def test_returned_subscription(self, order_with_customer_subscription, returned):
            assert order_with_customer_subscription.customer_subscription == returned['order'].customer_subscription
