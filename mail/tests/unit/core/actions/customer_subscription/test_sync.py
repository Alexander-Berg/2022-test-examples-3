from datetime import timedelta

import pytest

from sendr_utils import alist, enum_value, without_none

from hamcrest import (
    assert_that, contains, contains_inanyorder, equal_to, has_entries, has_item, has_items, has_properties
)

from mail.payments.payments.core.actions.customer_subscription.sync import SyncCustomerSubscriptionAction
from mail.payments.payments.core.entities.customer_subscription_transaction import CustomerSubscriptionTransaction
from mail.payments.payments.core.entities.enums import CallbackMessageType, TransactionStatus
from mail.payments.payments.core.exceptions import OrderNotFoundError
from mail.payments.payments.tests.base import (
    BaseOrderAcquirerTest, parametrize_merchant_oauth_mode, parametrize_shop_type
)
from mail.payments.payments.tests.utils import callback_tasks
from mail.payments.payments.utils.datetime import utcnow


class TestSyncCustomerSubscriptionAction(BaseOrderAcquirerTest):
    @pytest.fixture(autouse=True)
    def get_acquirer_mock(self, mock_action, acquirer):
        from mail.payments.payments.core.actions.merchant.get_acquirer import GetAcquirerMerchantAction
        return mock_action(GetAcquirerMerchantAction, acquirer)

    @pytest.fixture(autouse=True)
    def payment_clear_mock(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'payment_clear', {}, multiple_calls=True) as mock:
            yield mock

    @pytest.fixture
    def get_customer_subscription(self, storage, customer_subscription):
        async def _inner():
            return await storage.customer_subscription.get(
                customer_subscription.uid,
                customer_subscription.customer_subscription_id
            )

        return _inner

    @pytest.fixture
    def items_amount(self):
        return 1

    @pytest.fixture
    def order_id(self, order_with_customer_subscription):
        return order_with_customer_subscription.order_id

    @pytest.fixture
    def purchase_tokens(self):
        return []

    @pytest.fixture(params=(True, False))
    def enabled(self, request):
        return request.param

    @pytest.fixture
    def subs_until_ts(self, enabled):
        return (utcnow() + timedelta(minutes=1 if enabled else -1)).timestamp()

    @pytest.fixture
    def finish_ts(self):
        return None

    @pytest.fixture(autouse=True)
    def subscription_get_mock(self, shop_type, trust_client_mocker, purchase_tokens, subs_until_ts,
                              finish_ts):
        response = {'payments': purchase_tokens,
                    'subs_until_ts': subs_until_ts,
                    'finish_ts': finish_ts}
        with trust_client_mocker(shop_type, 'subscription_get', without_none(response)) as mock:
            yield mock

    @pytest.fixture
    def params(self, merchant, order_id, items):
        return {
            'uid': merchant.uid,
            'order_id': order_id,
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await SyncCustomerSubscriptionAction(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    async def test_update(self, get_customer_subscription, storage, enabled, subs_until_ts, returned):
        customer_subscription = await get_customer_subscription()
        assert customer_subscription.time_until.timestamp() == subs_until_ts

    class TestNotFound:
        @pytest.fixture
        def order_id(self, order_with_customer_subscription):
            return order_with_customer_subscription.order_id + 1

        @pytest.mark.asyncio
        async def test_not_found(self, returned_func):
            with pytest.raises(OrderNotFoundError):
                await returned_func()

    class TestProlongationApiCallbackTask:
        @pytest.fixture
        def prolongate(self):
            return None

        @pytest.fixture
        async def subs_until_ts(self, prolongate, storage, get_customer_subscription):
            assert prolongate is not None

            customer_subscription = await get_customer_subscription()
            customer_subscription.enabled = True
            customer_subscription.time_until = utcnow() + timedelta(minutes=1)
            customer_subscription.time_finish = None
            customer_subscription = await storage.customer_subscription.save(customer_subscription)
            return (customer_subscription.time_until + timedelta(minutes=1 if prolongate else 0)).timestamp()

        @pytest.mark.parametrize('prolongate', (True,))
        @pytest.mark.asyncio
        async def test_prolongation_api_callback_task__created(self, customer_subscription, returned, storage,
                                                               service_client, service_merchant):
            params = {
                'tvm_id': service_client.tvm_id,
                'callback_url': service_client.api_callback_url,
                'message': {
                    'order_id': customer_subscription.order_id,
                    'customer_subscription_id': customer_subscription.customer_subscription_id,
                    'service_merchant_id': service_merchant.service_merchant_id,
                    'updated': customer_subscription.updated.isoformat()
                },
                'callback_message_type': enum_value(CallbackMessageType.CUSTOMER_SUBSCRIPTION_PROLONGATED)
            }

            tasks = await callback_tasks(storage)
            assert_that(tasks, has_item(
                has_properties({
                    'params': has_entries(params)
                })
            ))

        @pytest.mark.parametrize('prolongate', (False,))
        @pytest.mark.asyncio
        async def test_prolongation_api_callback_task__not_created(self, returned, storage):
            assert len(await callback_tasks(storage)) == 0

    class TestFinishTs:
        @pytest.fixture(params=(True, False))
        def enabled(self, request):
            return request.param

        @pytest.fixture
        def finish_ts(self, enabled):
            return (utcnow() + timedelta(minutes=1 if enabled else -1)).timestamp()

        @pytest.mark.asyncio
        async def test_finish_ts__update(self, storage, subs_until_ts, enabled, finish_ts, order, returned):
            customer_subscription = await storage.customer_subscription.get(
                uid=order.uid,
                customer_subscription_id=order.customer_subscription_id
            )

            assert all((
                customer_subscription.enabled == enabled,
                customer_subscription.time_until.timestamp() == subs_until_ts,
                customer_subscription.time_finish.timestamp() == finish_ts,
            ))

        class TestCreateStoppedApiCallbackTask:
            @pytest.fixture(autouse=True)
            async def setup(self, customer_subscription, storage):
                customer_subscription = await storage.customer_subscription.get(
                    customer_subscription.uid,
                    customer_subscription.customer_subscription_id
                )
                customer_subscription.enabled = True
                customer_subscription.time_until = utcnow() + timedelta(minutes=1)
                customer_subscription.time_finish = None
                await storage.customer_subscription.save(customer_subscription)

            @pytest.mark.parametrize('enabled', (False,))
            @pytest.mark.asyncio
            async def test_create_stopped_api_callback_task__created(self, customer_subscription, returned, storage,
                                                                     service_client, service_merchant):
                params = {
                    'tvm_id': service_client.tvm_id,
                    'callback_url': service_client.api_callback_url,
                    'message': {
                        'order_id': customer_subscription.order_id,
                        'customer_subscription_id': customer_subscription.customer_subscription_id,
                        'service_merchant_id': service_merchant.service_merchant_id,
                        'updated': customer_subscription.updated.isoformat()
                    },
                    'callback_message_type': enum_value(CallbackMessageType.CUSTOMER_SUBSCRIPTION_STOPPED)
                }

                tasks = await callback_tasks(storage)
                assert_that(tasks, has_item(
                    has_properties({
                        'params': has_entries(**params)
                    })
                ))

            @pytest.mark.parametrize('enabled', (True,))
            @pytest.mark.asyncio
            async def test_create_stopped_api_callback_task__not_created(self, returned, storage):
                assert len(await callback_tasks(storage)) == 0

    class TestSyncTransactions:
        @pytest.fixture
        def payment_status(self):
            return TransactionStatus.ACTIVE

        @pytest.fixture(autouse=True)
        def payment_get_mock(self, shop_type, trust_client_mocker, payment_status, purchase_tokens):
            response = {'payment_status': TransactionStatus.to_trust(payment_status)}
            with trust_client_mocker(shop_type, 'payment_get', response, multiple_calls=True) as mock:
                yield mock

        @pytest.fixture
        def purchase_tokens(self, randn, rands):
            return [rands() for _ in range(randn(min=2, max=10))]

        @pytest.mark.asyncio
        @parametrize_merchant_oauth_mode
        async def test_payment_get_mock_call_args(self, storage, payment_get_mock, purchase_tokens, order,
                                                  order_acquirer, acquirer, returned):
            assert_that(
                [call[1] for call in payment_get_mock.call_args_list],
                has_items(*[
                    has_entries({'uid': order.uid, 'purchase_token': purchase_token, 'acquirer': order_acquirer})
                    for purchase_token in purchase_tokens
                ])
            )

        @pytest.mark.asyncio
        @parametrize_shop_type
        async def test_transactions(self, storage, payment_get_mock, purchase_tokens, order, returned):
            customer_subscription_transaction = await alist(
                storage.customer_subscription_transaction.find(
                    uid=order.uid,
                    customer_subscription_id=order.customer_subscription_id
                )
            )
            assert all((
                len(payment_get_mock.call_args_list) == len(purchase_tokens),
                len(customer_subscription_transaction) == len(purchase_tokens),
            ))

        class TestTransactionContent:
            @pytest.fixture
            def get_payment_response(self, payment_status, rands):
                return {
                    'payment_status': TransactionStatus.to_trust(payment_status),
                    'orders': [{'order_id': rands()}]
                }

            @pytest.fixture(autouse=True)
            def payment_get_mock(self, shop_type, trust_client_mocker, get_payment_response):
                with trust_client_mocker(shop_type, 'payment_get', get_payment_response, multiple_calls=True) as mock:
                    yield mock

            @pytest.fixture
            def purchase_tokens(self, rands):
                return [rands()]

            @pytest.mark.asyncio
            @parametrize_shop_type
            async def test_transactions_content(self, storage, get_payment_response, purchase_tokens, order, returned):
                customer_subscription_transactions = await alist(
                    storage.customer_subscription_transaction.find(
                        uid=order.uid,
                        customer_subscription_id=order.customer_subscription_id
                    )
                )
                response_order_id = get_payment_response['orders'][0]['order_id']
                assert all((
                    len(customer_subscription_transactions) == len(purchase_tokens),
                    customer_subscription_transactions[0].purchase_token == purchase_tokens[0],
                    customer_subscription_transactions[0].trust_order_id == response_order_id,
                ))

        class TestAlreadyExistTransactionInFinalStatus:
            @pytest.fixture(autouse=True)
            async def setup(self, purchase_tokens, order_with_customer_subscription, storage):
                await storage.customer_subscription_transaction.create_or_update(
                    CustomerSubscriptionTransaction(
                        uid=order_with_customer_subscription.uid,
                        customer_subscription_id=order_with_customer_subscription.customer_subscription_id,
                        purchase_token=purchase_tokens[0],
                        payment_status=list(TransactionStatus.FINAL_STATUSES)[0],
                        data={}
                    )
                )

            @pytest.mark.asyncio
            async def test_already_exists_transaction_in_final_status__transactions(self, storage, payment_get_mock,
                                                                                    purchase_tokens, order, returned):
                customer_subscription_id = order.customer_subscription_id

                customer_subscription_transaction = await alist(
                    storage.customer_subscription_transaction.find(uid=order.uid,
                                                                   customer_subscription_id=customer_subscription_id)
                )

                assert_that(
                    (len(payment_get_mock.call_args_list), len(customer_subscription_transaction)),
                    contains(equal_to(len(purchase_tokens) - 1), equal_to(len(purchase_tokens)))
                )

        class TestHeldTransaction:
            @pytest.fixture
            def payment_status(self):
                return TransactionStatus.HELD

            @pytest.mark.asyncio
            @parametrize_merchant_oauth_mode
            async def test_payment_clear_mock_call_args(self, storage, merchant, payment_get_mock, purchase_tokens,
                                                        order, order_acquirer, returned):
                assert_that(
                    [call[1] for call in payment_get_mock.call_args_list],
                    has_items(*[
                        has_entries({'uid': order.uid, 'purchase_token': purchase_token, 'acquirer': order_acquirer})
                        for purchase_token in purchase_tokens
                    ])
                )

            @pytest.mark.asyncio
            @parametrize_shop_type
            async def test_held_transaction__transactions(self, storage, payment_clear_mock, purchase_tokens, order,
                                                          returned):
                customer_subscription_id = order.customer_subscription_id
                customer_subscription_transaction = await alist(
                    storage.customer_subscription_transaction.find(
                        uid=order.uid,
                        customer_subscription_id=customer_subscription_id
                    )
                )

                assert_that(
                    (
                        len(payment_clear_mock.call_args_list),
                        len(customer_subscription_transaction),
                        [call[1]['purchase_token'] for call in payment_clear_mock.call_args_list]
                    ),
                    contains(
                        equal_to(len(purchase_tokens)),
                        equal_to(len(purchase_tokens)),
                        contains_inanyorder(*[tx.purchase_token for tx in customer_subscription_transaction])
                    )
                )

        class TestClearTransaction:
            @pytest.fixture
            def payment_status(self):
                return TransactionStatus.CLEARED

            def test_no_clear_twice(self, returned, payment_clear_mock):
                payment_clear_mock.assert_not_called()

            @pytest.fixture
            def purchase_tokens(self, randn, rands):
                return [rands() for _ in range(randn(min=2, max=10))]

            @pytest.mark.asyncio
            async def test_order_send_to_tlog(self, returned, get_customer_subscription, purchase_tokens, storage,
                                              transaction, order):
                customer_subscription = await get_customer_subscription()
                tasks = await alist(storage.task.find())

                params = [
                    t.params['action_kwargs']
                    for t in tasks
                    if t.action_name == 'export_customer_subscription_transaction_to_tlog'
                ]

                assert_that(params, contains_inanyorder(*[
                    {
                        'uid': order.uid,
                        'purchase_token': purchase_token,
                        'customer_subscription_id': customer_subscription.customer_subscription_id
                    } for purchase_token in purchase_tokens
                ]))
