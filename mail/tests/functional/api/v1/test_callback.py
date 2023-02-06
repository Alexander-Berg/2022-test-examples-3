import pytest

from mail.payments.payments.core.entities.enums import TransactionStatus
from mail.payments.payments.tests.base import BaseAcquirerTest


@pytest.mark.usefixtures('moderation')
class TestCallbackPost(BaseAcquirerTest):
    @pytest.fixture
    def transaction_status(self):
        return TransactionStatus.ACTIVE

    @pytest.fixture
    def new_transaction_status(self):
        return TransactionStatus.HELD

    @pytest.fixture(autouse=True)
    def trust_payment_get_mock(self, shop_type, trust_client_mocker, new_transaction_status):
        status = TransactionStatus.to_trust(new_transaction_status)
        with trust_client_mocker(shop_type, 'payment_get', {'payment_status': status}) as mock:
            yield mock

    @pytest.fixture
    def response_func(self, client):
        async def _inner(uid, order_id):
            return await client.post(f'/callback/payment/{uid}/{order_id}')

        return _inner

    @pytest.fixture
    async def response(self, response_func, order):
        return await response_func(order.uid, order.order_id)

    @pytest.mark.asyncio
    async def test_updates(self, storage, transaction, new_transaction_status, response):
        transaction = await storage.transaction.get(transaction.uid, transaction.tx_id)
        assert transaction.status == new_transaction_status

    @pytest.mark.asyncio
    async def test_response(self, transaction, response):
        assert response.status == 200

    @pytest.mark.asyncio
    async def test_response_no_transaction(self, response):
        assert response.status == 200

    @pytest.mark.asyncio
    async def test_no_order(self, merchant, response_func, randn):
        response = await response_func(merchant.uid, randn())
        assert response.status == 200
