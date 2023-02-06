# coding: utf-8
from decimal import Decimal

import pytest

from sendr_utils import alist, anext

from hamcrest import (
    all_of, assert_that, contains_inanyorder, has_entries, has_item, has_properties, is_, match_equality
)

from mail.payments.payments.core.actions.get_oauth import GetMerchantOAuth
from mail.payments.payments.core.actions.order.get_trust_env import GetOrderTrustEnvAction
from mail.payments.payments.core.actions.order.refund import (
    CreateCustomerSubscriptionTransactionRefundAction, CreateRefundAction, CreateServiceMerchantRefundAction
)
from mail.payments.payments.core.entities.enums import (
    NDS, PAYMETHOD_ID_OFFLINE, AcquirerType, MerchantOAuthMode, OrderKind, OrderSource, PayStatus, RefundStatus,
    ShopType, TaskState, TaskType, TransactionStatus, TrustEnv
)
from mail.payments.payments.core.entities.log import RefundCreatedLog
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.exceptions import (
    CoreActionDenyError, CoreFailError, CoreNotFoundError, CustomerSubscriptionTransactionHasOpenRefundsError,
    CustomerSubscriptionTransactionNotClearedError, CustomerSubscriptionTransactionNotFoundError, OAuthAbsentError,
    OrderItemNotPresentInOriginalOrderError, OrderNotFoundError, OrderOriginalOrderMustBePaidError,
    TransactionNotFoundError
)
from mail.payments.payments.tests.base import (
    BaseAcquirerTest, BaseOrderAcquirerTest, BaseTestOrderAction, BaseTestOrderWithCustomerSubscriptionAction,
    BaseTestParent, BaseTestRequiresModeration, BaseTrustCredentialsErrorTest, parametrize_shop_type
)


class BaseCreateRefundAction(BaseTestRequiresModeration, BaseTestOrderAction, BaseTestParent):
    @pytest.fixture
    def trust_env(self):
        return TrustEnv.PROD

    @pytest.fixture(autouse=True)
    def get_order_trust_env_action_mock(self, mock_action, trust_env):
        return mock_action(GetOrderTrustEnvAction, trust_env)

    @pytest.fixture
    def trust_refund_id(self):
        return 'xxx-trust-refund-id'

    @pytest.fixture
    def customer_uid(self):
        return 'test-create-refund-customer-uid'

    @pytest.fixture(autouse=True)
    def payment_get_mock(self, shop_type, trust_client_mocker, customer_uid):
        with trust_client_mocker(shop_type, 'payment_get', {'uid': customer_uid}) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def refund_create_mock(self, shop_type, trust_client_mocker, trust_refund_id):
        with trust_client_mocker(shop_type, 'refund_create', trust_refund_id) as mock:
            yield mock

    @pytest.fixture
    def paymethod_id(self):
        raise NotImplementedError

    @pytest.fixture
    def order_data(self, paymethod_id):
        return {
            'pay_status': PayStatus.PAID,
            'paymethod_id': paymethod_id
        }

    @pytest.fixture
    def transaction_data(self):
        return {
            'trust_purchase_token': 'xxx_trust_purchase_token',
        }

    @pytest.fixture
    def param_caption(self):
        return 'My refund caption'

    @pytest.fixture
    def param_pop_key(self):
        return None

    @pytest.fixture
    def params(self, merchant, merchant_oauth_mode, moderations, order, items, param_caption, param_pop_key):
        result = {
            'uid': order.uid,
            'order_id': order.order_id,
            'caption': param_caption,
            'items': [
                {
                    'name': item.name,
                    'nds': item.nds,
                    'price': item.price,
                    'amount': item.amount,
                    'currency': item.currency,
                }
                for item in items
            ],
            'description': 'My refund description',
        }
        if param_pop_key is not None:
            result.pop(param_pop_key)
        return result

    @pytest.fixture
    def action(self):
        return CreateRefundAction

    @pytest.fixture
    def returned_func(self, transaction, params, action):
        async def _inner():
            return await action(**params).run()

        return _inner

    @pytest.fixture
    async def created_refund(self, storage, returned):
        return await storage.order.get(returned.uid, returned.order_id)

    @pytest.fixture
    async def refund_items(self, storage, created_refund):
        return [
            item
            async for item in storage.item.get_for_order(created_refund.uid, created_refund.order_id)
        ]

    @pytest.mark.asyncio
    async def test_not_matching_items(self, action, transaction, items, params):
        params['items'] = [
            {
                'name': 'nonexistent',
                'nds': NDS.NDS_0,
                'price': Decimal(1),
                'amount': Decimal(1),
                'currency': 'RUB',
            }
        ]
        with pytest.raises(OrderItemNotPresentInOriginalOrderError):
            await action(**params).run()

    @pytest.mark.asyncio
    async def test_exceeding_items_amount(self,
                                          action,
                                          storage,
                                          order,
                                          existing_refunds,
                                          existing_refunds_data,
                                          params,
                                          ):
        refund = existing_refunds[0]
        params['order_id'] = refund.original_order_id
        if order.service_merchant_id:
            original_order = await storage.order.get(refund.uid, refund.original_order_id)
            original_order.service_merchant_id = order.service_merchant_id
            await storage.order.save(original_order)
        params['items'] = [
            {
                'name': item_data['name'],
                'nds': NDS(item_data['nds']),
                'price': Decimal(item_data['price']),
                'amount': Decimal(item_data['amount']),
                'currency': item_data['currency'],
            }
            for item_data in existing_refunds_data[0]['items']
        ]
        with pytest.raises(TransactionNotFoundError):
            await action(**params).run()

    def test_returned_refund(self, returned, order, items, paymethod_id, products, trust_refund_id):
        assert_that(
            returned,
            has_properties({
                'uid': order.uid,
                'original_order_id': order.order_id,
                'kind': OrderKind.REFUND,
                'refund_status': (
                    RefundStatus.COMPLETED
                    if paymethod_id == PAYMETHOD_ID_OFFLINE
                    else RefundStatus.CREATED
                ),
                'merchant_oauth_mode': order.merchant_oauth_mode,
                'trust_refund_id': None if paymethod_id == PAYMETHOD_ID_OFFLINE else trust_refund_id,
                'items': contains_inanyorder(*[
                    has_properties({
                        'uid': item.uid,
                        'product_id': item.product_id,
                        'order_id': returned.order_id,
                    })
                    for item in items
                ])
            })
        )

    def test_created_refund(self, created_refund, paymethod_id, order, trust_refund_id):
        assert_that(
            created_refund,
            has_properties({
                'uid': order.uid,
                'original_order_id': order.order_id,
                'kind': OrderKind.REFUND,
                'refund_status': (
                    RefundStatus.COMPLETED
                    if paymethod_id == PAYMETHOD_ID_OFFLINE
                    else RefundStatus.CREATED
                ),
                'trust_refund_id': None if paymethod_id == PAYMETHOD_ID_OFFLINE else trust_refund_id,
                'service_client_id': None,
                'service_merchant_id': None,
                'created_by_source': OrderSource.UI,
            })
        )

    @pytest.mark.parametrize('param_caption', ['abc', '', None])
    def test_created_refund_captions(self, created_refund, paymethod_id, order, trust_refund_id, param_caption):
        caption = param_caption or f'Возврат по заказу №{order.order_id}'
        assert_that(
            created_refund,
            has_properties({
                'uid': order.uid,
                'caption': caption,
                'original_order_id': order.order_id,
                'kind': OrderKind.REFUND
            })
        )

    @pytest.mark.parametrize('param_pop_key', [None, 'caption'])
    def test_created_refund_absent_captions(self, created_refund, paymethod_id, order, trust_refund_id, param_caption,
                                            param_pop_key):
        assert_that(
            created_refund,
            has_properties({
                'uid': order.uid,
                'caption': param_caption if param_pop_key is None else f'Возврат по заказу №{order.order_id}',
                'original_order_id': order.order_id,
                'kind': OrderKind.REFUND
            })
        )

    def test_logged(self, created_refund, refund_items, order, merchant, pushers_mock):
        created_refund.items = refund_items
        assert_that(
            pushers_mock.log.push.call_args[0][0],
            all_of(
                is_(RefundCreatedLog),
                has_properties(dict(
                    merchant_name=merchant.name,
                    merchant_uid=merchant.uid,
                    merchant_acquirer=created_refund.get_acquirer(merchant.acquirer),
                    refund_id=created_refund.order_id,
                    order_id=created_refund.original_order_id,
                    status=None if created_refund.refund_status is None else created_refund.refund_status.value,
                    price=created_refund.log_price,
                    items=[item.dump() for item in created_refund.items],
                    paymethod_id=order.paymethod_id,
                )),
            )
        )

    @pytest.mark.asyncio
    async def test_merchant_callback_scheduled(self, storage, merchant, returned):
        tasks = await alist(storage.task.find())
        assert_that(
            tasks,
            has_item(has_properties({
                'task_type': TaskType.API_CALLBACK,
                'params': has_entries(callback_url=merchant.api_callback_url),
            }))
        )

    @pytest.mark.asyncio
    async def test_send_to_history_task_created(self, storage, order, returned):
        tasks = await alist(storage.task.find())
        task = next((t for t in tasks if t.action_name == 'send_to_history_order_action'), None)
        assert all((
            task is not None,
            task.params['action_kwargs'] == {'uid': order.uid, 'order_id': order.order_id},
        ))

    @pytest.mark.asyncio
    async def test_transaction_not_paid(self, action, storage, order, transaction, params):
        order.pay_status = PayStatus.REJECTED
        await storage.order.save(order)
        with pytest.raises(OrderOriginalOrderMustBePaidError):
            await action(**params).run()

    class TestActionDeny(BaseTestRequiresModeration.TestActionDeny):
        @pytest.fixture(params=TrustEnv)
        def trust_env(self, request):
            return request.param

        @pytest.mark.asyncio
        async def test_action_deny__raises_error(self, trust_env, returned_func, noop_manager):
            manager = pytest.raises(CoreActionDenyError) if trust_env == TrustEnv.PROD else noop_manager()
            with manager:
                await returned_func()

    class TestNotFound:
        @pytest.mark.asyncio
        async def test_unknown_order(self, action, params):
            params.update({'order_id': -1})
            with pytest.raises(CoreNotFoundError):
                await action(**params).run()

        @pytest.mark.usefixtures('order_with_customer_subscription')
        @pytest.mark.asyncio
        async def test_customer_subscription(self, action, params):
            with pytest.raises(CoreNotFoundError):
                await action(**params).run()

    class TestOldNDSMapping:
        @pytest.fixture
        def products_data(self):
            return [
                {
                    'name': 'product 1',
                    'price': Decimal('10.01'),
                    'nds': NDS.NDS_20,
                },
                {
                    'name': 'product 2',
                    'price': Decimal('99.97'),
                    'nds': NDS.NDS_20_120,
                },
                {
                    'name': 'product 3',
                    'price': Decimal('99.97'),
                    'nds': NDS.NDS_10,
                }
            ]

        @pytest.fixture
        def items_data(self):
            return [
                {
                    'product_index': 0,
                    'amount': Decimal('10'),
                },
                {
                    'product_index': 1,
                    'amount': Decimal('11'),
                },
                {
                    'product_index': 2,
                    'amount': Decimal('12'),
                },
            ]

        @pytest.mark.asyncio
        async def test_fixes_nds(self, action, transaction, params):
            for item in params['items']:
                if item['nds'] == NDS.NDS_20:
                    item['nds'] = NDS.NDS_18
                if item['nds'] == NDS.NDS_20_120:
                    item['nds'] = NDS.NDS_18_118
            await action(**params).run()


class BaseTestCreateRefundActionOnline(BaseCreateRefundAction, BaseAcquirerTest, BaseOrderAcquirerTest,
                                       BaseTrustCredentialsErrorTest):
    @pytest.fixture(autouse=True)
    async def setup_merchant_oauth(self, storage, order_acquirer, create_merchant_oauth, merchant, merchant_oauth_mode):
        if order_acquirer == AcquirerType.KASSA:
            oauth = [await create_merchant_oauth(merchant.uid, mode=merchant_oauth_mode)]
            merchant.oauth = oauth
            await storage.merchant.save(merchant)

    @pytest.fixture(autouse=True)
    async def setup_order_acquirer(self, order, order_acquirer, storage):
        order.acquirer = order_acquirer
        await storage.order.save(order)

    @pytest.fixture
    def action_params(self, params):
        return params

    @pytest.fixture
    def paymethod_id(self, rands):
        return rands()

    @pytest.mark.parametrize('acquirer', [AcquirerType.KASSA])
    @pytest.mark.parametrize('order_acquirer', [AcquirerType.KASSA])
    @pytest.mark.parametrize(
        ('shop_type', 'merchant_oauth_mode'), [
            (ShopType.PROD, MerchantOAuthMode.TEST),
            (ShopType.TEST, MerchantOAuthMode.PROD),
        ])
    @pytest.mark.asyncio
    async def test_oauth_absent(self, returned_func):
        with pytest.raises(OAuthAbsentError):
            await returned_func()

    @pytest.mark.asyncio
    async def test_not_found_transaction(self, action, params):
        with pytest.raises(CoreNotFoundError):
            await action(**params).run()

    @parametrize_shop_type
    def test_payment_get_call(self, order_acquirer, transaction, payment_get_mock, returned, merchant):
        payment_get_mock.assert_called_once_with(
            uid=transaction.uid,
            acquirer=order_acquirer,
            purchase_token=transaction.trust_purchase_token,
            with_terminal_info=True
        )

    @parametrize_shop_type
    @pytest.mark.asyncio
    async def test_refund_create_call(self,
                                      merchant,
                                      order_acquirer,
                                      order,
                                      shop,
                                      transaction,
                                      customer_uid,
                                      refund_create_mock,
                                      returned,
                                      refund_items,
                                      ):
        refund_create_mock.assert_called_once_with(
            uid=merchant.uid,
            acquirer=order_acquirer,
            original_order_id=order.order_id,
            customer_uid=customer_uid,
            caption=returned.caption,
            purchase_token=transaction.trust_purchase_token,
            items=match_equality(contains_inanyorder(*refund_items)),
            submerchant_id=merchant.submerchant_id,
            oauth=await GetMerchantOAuth(acquirer=order_acquirer, merchant=merchant, shop=shop).run(),
            version=order.data.version,
        )

    @pytest.mark.asyncio
    async def test_start_refund_task_created(self, storage, returned):
        task = await anext((t async for t in storage.task.find() if t.task_type == TaskType.START_REFUND))
        assert_that(
            task,
            has_properties({
                'state': TaskState.PENDING,
                'params': has_entries(uid=returned.uid, order_id=returned.order_id)
            })
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('order_acquirer', [None])
    @pytest.mark.parametrize('acquirer', [None])
    async def test_deny_with_no_acquirer(self, order_acquirer, returned_func):
        with pytest.raises(CoreActionDenyError):
            await returned_func()


class BaseTestCreateRefundActionOffline(BaseCreateRefundAction):
    @pytest.fixture
    def paymethod_id(self):
        return PAYMETHOD_ID_OFFLINE

    @pytest.fixture(params=([None, *list(AcquirerType)]))
    def acquirer(self, request):
        return request.param

    @pytest.fixture
    def order_acquirer(self, acquirer):
        return acquirer

    def test_status(self, returned):
        assert all((
            returned.refund_status == RefundStatus.COMPLETED,
            returned.trust_refund_id is None
        ))

    @parametrize_shop_type
    def test_refund_create_not_call(self, returned, refund_create_mock):
        refund_create_mock.assert_not_called()

    @parametrize_shop_type
    def test_payment_get_not_call(self, payment_get_mock, returned):
        payment_get_mock.assert_not_called()

    @pytest.mark.asyncio
    async def test_start_refund_task_no_created(self, storage, returned):
        assert len(await alist(storage.task.find(task_type=TaskType.START_REFUND))) == 0


class BaseTestOrderWithServiceMerchant:
    class TestOrderWithServiceMerchant:
        @pytest.fixture(autouse=True)
        async def setup(self, storage, order, params, service_client, service_merchant):
            order.service_client_id = service_client.service_client_id
            order.service_merchant_id = service_merchant.service_merchant_id
            await storage.order.save(order)

        @pytest.mark.asyncio
        async def test_service_callback_scheduled(self, storage, service_client, returned):
            tasks = [t async for t in storage.task.find()]
            assert_that(
                tasks,
                has_item(has_properties({
                    'task_type': TaskType.API_CALLBACK,
                    'params': has_entries(callback_url=service_client.api_callback_url),
                }))
            )

        @pytest.mark.asyncio
        async def test_set_service_merchant_id(self, created_refund, service_merchant):
            assert created_refund.service_merchant_id == service_merchant.service_merchant_id

        @pytest.mark.asyncio
        async def test_set_service_client_id(self, created_refund, service_client):
            assert created_refund.service_client_id == service_client.service_client_id


# Uid


class TestCreateRefundActionOnline(BaseTestCreateRefundActionOnline, BaseTestOrderWithServiceMerchant):
    pass


class TestCreateRefundActionOffline(BaseTestCreateRefundActionOffline, BaseTestOrderWithServiceMerchant):
    pass


# Service Merchant

class BaseCreateServiceMerchantRefund:
    @pytest.fixture(autouse=True)
    async def setup(self, storage, order, params, service_client, service_merchant):
        order.service_client_id = service_client.service_client_id
        order.service_merchant_id = service_merchant.service_merchant_id
        await storage.order.save(order)
        params.update({
            'service_tvm_id': service_client.tvm_id,
            'service_merchant_id': service_merchant.service_merchant_id,
        })

    @pytest.fixture
    def action(self):
        return CreateServiceMerchantRefundAction

    @pytest.mark.asyncio
    async def test_not_found_without_service_merchant(self, storage, order, returned_func):
        order.service_client_id = order.service_merchant_id = None
        await storage.order.save(order)
        with pytest.raises(OrderNotFoundError):
            await returned_func()

    def test_created_refund(self, paymethod_id, created_refund, order, trust_refund_id, service_client,
                            service_merchant):
        assert_that(
            created_refund,
            has_properties({
                'uid': order.uid,
                'original_order_id': order.order_id,
                'kind': OrderKind.REFUND,
                'refund_status': (
                    RefundStatus.COMPLETED
                    if paymethod_id == PAYMETHOD_ID_OFFLINE
                    else RefundStatus.CREATED
                ),
                'trust_refund_id': None if paymethod_id == PAYMETHOD_ID_OFFLINE else trust_refund_id,
                'service_client_id': service_client.service_client_id,
                'service_merchant_id': service_merchant.service_merchant_id,
                'created_by_source': OrderSource.SERVICE,
            })
        )

    @pytest.mark.asyncio
    async def test_service_callback_scheduled(self, storage, service_client, returned):
        tasks = [t async for t in storage.task.find()]
        assert_that(
            tasks,
            has_item(has_properties({
                'task_type': TaskType.API_CALLBACK,
                'params': has_entries(callback_url=service_client.api_callback_url),
            }))
        )


class TestCreateServiceMerchantRefundOnline(BaseCreateServiceMerchantRefund, BaseTestCreateRefundActionOnline):
    pass


class TestCreateServiceMerchantRefundOffline(BaseCreateServiceMerchantRefund, BaseTestCreateRefundActionOffline):
    pass


class TestCreateCustomerSubscriptionTransactionRefundAction(BaseTestOrderWithCustomerSubscriptionAction):
    @pytest.fixture
    def trust_env(self):
        return TrustEnv.PROD

    @pytest.fixture(autouse=True)
    def get_order_trust_env_action_mock(self, mock_action, trust_env):
        return mock_action(GetOrderTrustEnvAction, trust_env)

    @pytest.fixture
    def trust_refund_id(self):
        return 'xxx-trust-refund-id'

    @pytest.fixture
    def customer_uid(self):
        return 'test-create-refund-customer-uid'

    @pytest.fixture(autouse=True)
    def payment_get_mock(self, shop_type, trust_client_mocker, customer_uid):
        with trust_client_mocker(shop_type, 'payment_get', {'uid': customer_uid}) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def refund_create_mock(self, shop_type, trust_client_mocker, trust_refund_id):
        with trust_client_mocker(shop_type, 'refund_create_single', trust_refund_id) as mock:
            yield mock

    @pytest.fixture
    def paymethod_id(self, rands):
        return rands()

    @pytest.fixture
    def trust_order_id(self, rands):
        return rands()

    @pytest.fixture
    def order_data(self, paymethod_id):
        return {
            'pay_status': PayStatus.PAID,
            'paymethod_id': paymethod_id
        }

    @pytest.fixture
    def customer_subscription_transaction_data(self, rands, trust_order_id):
        data = {rands(): rands() for _ in range(10)}
        return {
            'payment_status': TransactionStatus.CLEARED,
            'trust_order_id': trust_order_id,
            'data': {
                **data,
                'amount': '183.48',
            },
        }

    @pytest.fixture
    def action(self):
        return CreateCustomerSubscriptionTransactionRefundAction

    @pytest.fixture
    def returned_func(self, params, action):
        async def _inner():
            return await action(**params).run()

        return _inner

    @pytest.fixture
    async def created_refund(self, storage, returned):
        return await storage.order.get(uid=returned.uid, order_id=returned.order_id, select_customer_subscription=None)

    @pytest.fixture
    def param_pop_key(self):
        return None

    @pytest.fixture
    def param_caption(self):
        return 'My refund caption'

    @pytest.fixture
    def params(self, merchant, merchant_oauth_mode, moderations, order_with_customer_subscription,
               customer_subscription, customer_subscription_transaction, param_caption, param_pop_key):
        result = {
            'uid': merchant.uid,
            'customer_subscription_id': customer_subscription.customer_subscription_id,
            'purchase_token': customer_subscription_transaction.purchase_token,
            'merchant': merchant,
            'caption': param_caption,
            'description': 'My refund description',
        }
        if param_pop_key is not None:
            result[param_pop_key] = None
        return result

    @pytest.fixture
    def refund_status(self):
        return RefundStatus.CREATED

    @pytest.fixture
    def transaction_status(self):
        return TransactionStatus.CLEARED

    @pytest.fixture
    def pay_status(self):
        return PayStatus.PAID

    @pytest.mark.asyncio
    @pytest.mark.parametrize('param_pop_key', ['uid', 'customer_subscription_id', 'purchase_token'])
    async def test_bad_params(self, action, params):
        with pytest.raises(CoreFailError):
            await action(**params).run()

    @pytest.mark.asyncio
    async def test_tx_absent(self, action, params, db_conn, customer_subscription_transaction):
        await db_conn.execute(
            f"""delete from payments.customer_subscription_transactions
                where
                    uid = {customer_subscription_transaction.uid} and
                    customer_subscription_id = {customer_subscription_transaction.customer_subscription_id}
            """
        )

        with pytest.raises(CustomerSubscriptionTransactionNotFoundError):
            await action(**params).run()

    @pytest.mark.asyncio
    async def test_order_id_absent(self, action, params, storage, customer_subscription):
        customer_subscription.order_id = None
        await storage.customer_subscription.save(customer_subscription)

        with pytest.raises(CoreFailError):
            await action(**params).run()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('pay_status', [PayStatus.NEW, PayStatus.IN_MODERATION, PayStatus.HELD,
                                            PayStatus.IN_PROGRESS, PayStatus.REJECTED, PayStatus.MODERATION_NEGATIVE,
                                            PayStatus.ABANDONED, PayStatus.IN_CANCEL, PayStatus.CANCELLED])
    async def test_order_not_paid(self, action, params, storage, order_with_customer_subscription, pay_status):
        order_with_customer_subscription.pay_status = pay_status
        await storage.order.save(order_with_customer_subscription)

        with pytest.raises(OrderOriginalOrderMustBePaidError):
            await action(**params).run()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('transaction_status', [TransactionStatus.ACTIVE, TransactionStatus.FAILED,
                                                    TransactionStatus.CANCELLED, TransactionStatus.HELD])
    async def test_tx_not_cleared(self, action, params, storage, customer_subscription_transaction, transaction_status):
        customer_subscription_transaction.payment_status = transaction_status
        await storage.customer_subscription_transaction.save(customer_subscription_transaction)

        with pytest.raises(CustomerSubscriptionTransactionNotClearedError):
            await action(**params).run()

    @pytest.mark.asyncio
    @pytest.mark.parametrize('refund_status', [RefundStatus.CREATED, RefundStatus.COMPLETED, RefundStatus.REQUESTED])
    async def test_not_failed_refunds(self, action, params, storage, order_with_customer_subscription, merchant,
                                      customer_subscription, customer_subscription_transaction, refund_status):
        open_refund = Order(
            uid=merchant.uid,
            original_order_id=order_with_customer_subscription.order_id,
            shop_id=order_with_customer_subscription.shop_id,
            caption=f'Возврат по заказу №{order_with_customer_subscription.order_id}',
            kind=OrderKind.REFUND,
            pay_status=None,
            refund_status=refund_status,
            acquirer=order_with_customer_subscription.acquirer,
            customer_subscription_id=customer_subscription.customer_subscription_id,
            customer_subscription_tx_purchase_token=customer_subscription_transaction.purchase_token
        )
        await storage.order.create(open_refund)

        with pytest.raises(CustomerSubscriptionTransactionHasOpenRefundsError):
            await action(**params).run()

    def test_returned_refund(self, returned, order_with_customer_subscription, items, paymethod_id, trust_refund_id):
        assert_that(
            returned,
            has_properties({
                'uid': order_with_customer_subscription.uid,
                'original_order_id': order_with_customer_subscription.order_id,
                'kind': OrderKind.REFUND,
                'refund_status': (
                    RefundStatus.COMPLETED
                    if paymethod_id == PAYMETHOD_ID_OFFLINE
                    else RefundStatus.CREATED
                ),
                'merchant_oauth_mode': order_with_customer_subscription.merchant_oauth_mode,
                'trust_refund_id': trust_refund_id,
                'items': contains_inanyorder(*[
                    has_properties({
                        'uid': item.uid,
                        'product_id': item.product_id,
                        'order_id': returned.order_id,
                    })
                    for item in items
                ])
            })
        )

    def test_created_refund(self, created_refund, paymethod_id, order_with_customer_subscription, trust_refund_id,
                            customer_subscription, customer_subscription_transaction):
        assert_that(
            created_refund,
            has_properties({
                'uid': order_with_customer_subscription.uid,
                'original_order_id': order_with_customer_subscription.order_id,
                'kind': OrderKind.REFUND,
                'refund_status': (
                    RefundStatus.COMPLETED
                    if paymethod_id == PAYMETHOD_ID_OFFLINE
                    else RefundStatus.CREATED
                ),
                'trust_refund_id': None if paymethod_id == PAYMETHOD_ID_OFFLINE else trust_refund_id,
                'service_client_id': None,
                'service_merchant_id': None,
                'created_by_source': OrderSource.UI,
                'customer_subscription_id': customer_subscription.customer_subscription_id,
                'customer_subscription_tx_purchase_token': customer_subscription_transaction.purchase_token,
            })
        )

    def test_logged(self, created_refund, items, order_with_customer_subscription, merchant, pushers_mock):
        created_refund.items = items
        assert_that(
            pushers_mock.log.push.call_args[0][0],
            all_of(
                is_(RefundCreatedLog),
                has_properties(dict(
                    merchant_name=merchant.name,
                    merchant_uid=merchant.uid,
                    merchant_acquirer=created_refund.get_acquirer(merchant.acquirer),
                    refund_id=created_refund.order_id,
                    order_id=created_refund.original_order_id,
                    status=None if created_refund.refund_status is None else created_refund.refund_status.value,
                    price=created_refund.log_price,
                    items=[item.dump() for item in created_refund.items],
                    paymethod_id=order_with_customer_subscription.paymethod_id,
                )),
            )
        )

    @pytest.mark.asyncio
    async def test_merchant_callback_scheduled(self, storage, merchant, returned):
        tasks = await alist(storage.task.find())
        assert_that(
            tasks,
            has_item(has_properties({
                'task_type': TaskType.API_CALLBACK,
                'params': has_entries(callback_url=merchant.api_callback_url),
            }))
        )

    @pytest.mark.asyncio
    async def test_send_to_history_task_created(self, storage, order_with_customer_subscription, returned):
        tasks = await alist(storage.task.find())
        task = next((t for t in tasks if t.action_name == 'send_to_history_order_action'), None)
        assert all((
            task is not None,
            task.params['action_kwargs'] == {'uid': order_with_customer_subscription.uid,
                                             'order_id': order_with_customer_subscription.order_id},
        ))

    @pytest.mark.asyncio
    async def test_start_refund_task_created(self, storage, returned):
        task = await anext((t async for t in storage.task.find() if t.task_type == TaskType.START_REFUND))
        assert_that(
            task,
            has_properties({
                'state': TaskState.PENDING,
                'params': has_entries(uid=returned.uid, order_id=returned.order_id)
            })
        )

    @parametrize_shop_type
    def test_payment_get_call(self, acquirer, customer_subscription_transaction, payment_get_mock, returned,
                              merchant):
        payment_get_mock.assert_called_once_with(
            uid=customer_subscription_transaction.uid,
            acquirer=acquirer,
            purchase_token=customer_subscription_transaction.purchase_token,
            with_terminal_info=True
        )

    @parametrize_shop_type
    @pytest.mark.asyncio
    async def test_refund_create_call(self,
                                      merchant,
                                      acquirer,
                                      shop,
                                      trust_order_id,
                                      customer_subscription,
                                      customer_subscription_transaction,
                                      customer_uid,
                                      refund_create_mock,
                                      returned,
                                      ):
        refund_create_mock.assert_called_once_with(
            uid=merchant.uid,
            acquirer=acquirer,
            customer_uid=customer_uid,
            caption=returned.caption,
            purchase_token=customer_subscription_transaction.purchase_token,
            quantity=customer_subscription.quantity,
            trust_order_id=trust_order_id,
            submerchant_id=merchant.submerchant_id,
            oauth=await GetMerchantOAuth(acquirer=acquirer, merchant=merchant, shop=shop).run(),
        )
