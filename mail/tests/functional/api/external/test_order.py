import pytest

from mail.payments.payments.core.entities.enums import TransactionStatus
from mail.payments.payments.tests.utils import (
    check_merchant_from_person, check_order, check_order_hashes, check_transaction
)

from ..v1.order.test_active import BaseTestUpdateOrder
from ..v1.order.test_create_or_update import BaseTestCreateOrder
from ..v1.order.test_get import BaseTestGetOrder


@pytest.mark.usefixtures('moderation')
class BaseOrderExternal:
    @pytest.fixture(autouse=True)
    def person_get_mock(self, balance_client_mocker, person_entity):
        with balance_client_mocker('get_person', person_entity) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def update_transaction_calls(self, mocker):
        calls = []

        def dummy_init(self, transaction, *args, **kwargs):
            self.tx = transaction

        async def dummy_run(self):
            nonlocal calls
            calls.append(self.tx)
            return self.tx

        mocker.patch(
            'mail.payments.payments.core.actions.update_transaction.UpdateTransactionAction.__init__', dummy_init
        )
        mocker.patch(
            'mail.payments.payments.core.actions.update_transaction.UpdateTransactionAction.run', dummy_run
        )

        return calls


class BaseGetOrderByHashExternal(BaseOrderExternal):
    @pytest.fixture
    async def response(self, client, order_hash, transaction):
        r = await client.get(f'/ext/{order_hash}')
        assert r.status == 200
        return await r.json()


class TestGetOrderByHashExternal(BaseGetOrderByHashExternal):
    def test_merchant_data(self, merchant, stored_person_entity, response):
        check_merchant_from_person(merchant, stored_person_entity, response['data']['merchant'])

    def test_order_data(self, order, response):
        check_order(order, response['data']['order'], items=True)

    def test_order_data_with_customer_subscription(self,
                                                   order_with_customer_subscription,  # add subs to stored order
                                                   response,
                                                   customer_subscription):
        actual = response['data']['order']['customer_subscription']
        assert customer_subscription.customer_subscription_id == actual['customer_subscription_id']
        assert customer_subscription.subscription_id == actual['subscription']['subscription_id']

    def test_order_hashes(self, crypto_mock, response):
        check_order_hashes(crypto_mock, response['data']['order'])

    def test_transaction_data(self, transaction, response, order_hash):
        check_transaction(transaction, response['data']['transaction'])

    class TestUpdatesTransaction:
        @pytest.fixture(params=[
            TransactionStatus.ACTIVE,
            TransactionStatus.HELD,
        ])
        def transaction_status(self, request):
            return request.param

        @pytest.mark.asyncio
        async def test_update_transaction_called(self, update_transaction_calls, response):
            assert len(update_transaction_calls) == 1


class TestGetOrderExternal(BaseTestGetOrder):
    @pytest.fixture
    def uid_order_test_data(self, order):
        return {'path': f'/ext/order/{order.uid}/{order.order_id}', 'order': order}

    @pytest.fixture
    def id_type(self):
        return 'uid'


class TestCreateOrderExternal(BaseTestCreateOrder):
    @pytest.fixture
    def uid_order_test_data(self, merchant):
        return {'path': f'/ext/order/{merchant.uid}'}

    @pytest.fixture
    def id_type(self):
        return 'uid'


class TestUpdateOrderExternal(BaseTestUpdateOrder):
    @pytest.fixture
    def uid_order_test_data(self, merchant, order):
        return {'path': f'/ext/order/{order.uid}/{order.order_id}', 'order': order}

    @pytest.fixture
    def id_type(self):
        return 'uid'
