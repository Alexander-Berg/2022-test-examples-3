import pytest
import ujson

from hamcrest import assert_that, equal_to

from mail.payments.payments.core.actions.get_oauth import GetMerchantOAuth
from mail.payments.payments.core.actions.interactions.trust import (
    ClearPaymentInTrustAction, GetPaymentInfoInTrustAction, GetPaymentStatusInTrustAction, GetSubscriptionInTrustAction,
    GetTrustCredentialParamsAction, ProcessNewOrderInTrustAction, UnholdPaymentInTrustAction
)
from mail.payments.payments.core.actions.order.get_trust_env import GetOrderTrustEnvAction
from mail.payments.payments.core.entities.enums import AcquirerType, ReceiptType, TransactionStatus, TrustEnv
from mail.payments.payments.core.exceptions import (
    CoreActionDenyError, CoreDataError, CoreFailError, OAuthAbsentError, SubscriptionRequiresCustomerUid,
    TinkoffInvalidSubmerchantIdError
)
from mail.payments.payments.interactions.trust import TrustPaymentClient
from mail.payments.payments.interactions.trust.base import BaseTrustClient
from mail.payments.payments.interactions.trust.exceptions import TrustUidNotFound
from mail.payments.payments.tests.base import (
    BaseAcquirerTest, BaseOrderAcquirerTest, BaseSubscriptionAcquirerTest, BaseTrustCredentialsErrorTest,
    parametrize_shop_type
)
from mail.payments.payments.utils.helpers import without_none


class TestGetTrustCredentialParamsAction:
    @pytest.fixture
    def trust_env(self):
        return TrustEnv.PROD

    @pytest.fixture(autouse=True)
    def mock_get_order_trust_env_action(self, mock_action, trust_env):
        return mock_action(GetOrderTrustEnvAction, trust_env)

    @pytest.fixture
    def params(self, acquirer, order, merchant):
        return {
            'acquirer': acquirer,
            'order': order,
            'merchant': merchant
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await GetTrustCredentialParamsAction(**params).run()

        return _inner

    def test_env_call(self, returned, order, mock_get_order_trust_env_action):
        mock_get_order_trust_env_action.assert_called_once_with(order)

    @pytest.mark.parametrize('acquirer', {AcquirerType.TINKOFF})
    @pytest.mark.parametrize('submerchant_id', {None, 'test-submerchant-id'})
    class TestTinkoff:
        @pytest.mark.asyncio
        async def test_tinkoff_prod(self, noop_manager, submerchant_id, returned_func):
            manager = noop_manager() if submerchant_id else pytest.raises(TinkoffInvalidSubmerchantIdError)
            with manager:
                returned = await returned_func()
                assert returned == (AcquirerType.TINKOFF, submerchant_id, None)

        @pytest.mark.parametrize('trust_env', {TrustEnv.SANDBOX})
        @pytest.mark.asyncio
        async def test_tinkoff_sandbox(self, submerchant_id, payments_settings, acquirer, returned):
            assert returned == (acquirer, submerchant_id or payments_settings.TRUST_SANDBOX_SUBMERCHANT_ID, None)

    @pytest.mark.parametrize('acquirer', {AcquirerType.KASSA})
    class TestKassa:
        @pytest.fixture(params=(True, False), autouse=True)
        async def delete_oauth(self, request, merchant, storage):
            if request.param:
                for oauth in merchant.oauth:
                    await storage.merchant_oauth.delete(oauth)
                merchant.oauth = []
            return request.param

        @pytest.mark.asyncio
        async def test_kassa_prod(self, noop_manager, merchant, submerchant_id, delete_oauth, returned_func):
            manager = pytest.raises(OAuthAbsentError) if delete_oauth else noop_manager()
            with manager:
                returned = await returned_func()
                assert returned == (AcquirerType.KASSA, submerchant_id, merchant.oauth[0])

        @pytest.mark.parametrize('trust_env', {TrustEnv.SANDBOX})
        @pytest.mark.asyncio
        async def test_kassa_sandbox(self, submerchant_id, delete_oauth, merchant, merchant_oauth_mode, acquirer,
                                     returned):
            assert returned == (
                AcquirerType.TINKOFF if delete_oauth else AcquirerType.KASSA,
                submerchant_id,
                None if delete_oauth else merchant.oauth[0],
            )

    @pytest.mark.parametrize('acquirer', {None})
    class TestNoneAcquirer:
        @pytest.mark.parametrize('trust_env', {TrustEnv.PROD})
        @pytest.mark.asyncio
        async def test_none_acquirer_prod(self, returned_func):
            with pytest.raises(CoreActionDenyError):
                await returned_func()

        @pytest.mark.parametrize('trust_env', {TrustEnv.SANDBOX})
        def test_none_acquirer_sandbox(self, returned, submerchant_id):
            assert returned == (AcquirerType.TINKOFF, submerchant_id, None)


class BaseTestTrust:
    class TestProcessNewOrderInTrustAction(BaseAcquirerTest, BaseTrustCredentialsErrorTest):
        @pytest.fixture
        def user_email(self):
            return 'me@ya.ru'

        @pytest.fixture
        def trust_form_name(self):
            return 'disk'

        @pytest.fixture
        def return_url(self):
            return 'return url'

        @pytest.fixture(params=[
            {},
            {'template_tag': 'xxx'},
            {'yandexuid': 'xxx', 'customer_uid': 'yy', 'service_fee': 2, 'payment_completion_action': 'redirect'},
            {'yandexuid': '111222233333', 'payments_service_slug': 'the-slug', 'login_id': 'loginid'},
        ])
        def kwargs(self, request):
            return request.param

        @pytest.fixture(autouse=True)
        def payment_create(self, shop_type, trust_client_mocker, purchase_token, trust_payment_id):
            result = {
                'status': 'success',
                'status_code': 'payment_created',
                'purchase_token': purchase_token,
                'trust_payment_id': trust_payment_id
            }
            with trust_client_mocker(shop_type, '_payment_create', result) as mock:
                yield mock

        @pytest.fixture
        def payment_start_result(self):
            return {'started_payment': 1}

        @pytest.fixture(autouse=True)
        def payment_start(self, shop_type, trust_client_mocker, payment_start_result):
            with trust_client_mocker(shop_type, 'payment_start', payment_start_result) as mock:
                yield mock

        @pytest.fixture(autouse=True)
        def payment_markup(self, shop_type, trust_client_mocker):
            with trust_client_mocker(shop_type, 'payment_markup', None) as mock:
                yield mock

        @pytest.fixture
        async def params(self, storage, merchant, randn, rands, items, order, return_url, user_email, trust_form_name,
                         kwargs):
            return {
                'merchant': merchant,
                'items': items,
                'order': order,
                'return_url': return_url,
                'user_email': user_email,
                'trust_form_name': trust_form_name,
                **kwargs,
            }

        @pytest.fixture
        def action_params(self, params):
            return params

        @pytest.fixture
        def returned_func(self, action_params):
            async def _inner():
                return await ProcessNewOrderInTrustAction(**action_params).run()

            return _inner

        @pytest.mark.asyncio
        async def test_process_new_order_in_trust_action__returned(self, returned, trust_payment_id,
                                                                   payment_start_result):
            assert_that(returned, equal_to({
                **payment_start_result,
                'trust_payment_id': trust_payment_id
            }))

        @pytest.mark.asyncio
        @parametrize_shop_type
        async def test_payment_start(self, merchant, action_params, order, payment_start, purchase_token, returned):
            payment_start.assert_called_once_with(
                uid=order.uid,
                acquirer=action_params['order'].acquirer,
                purchase_token=purchase_token,
                without_url=order.data.recurrent or order.data.without_3ds,
            )

        @pytest.mark.asyncio
        @parametrize_shop_type
        @pytest.mark.parametrize('order_data_data', [{'receipt_type': receipt_type} for receipt_type in ReceiptType])
        async def test_payment_create_calls(self, acquirer, merchant, action_params, returned, payments_settings,
                                            payment_create, order, items, return_url, user_email,
                                            trust_form_name, params):
            payment_create.assert_called_once()
            trust_payment_data = payment_create.call_args[1]['payment_data']
            headers = BaseTrustClient._make_headers(action_params.get('yandexuid'), action_params.get('customer_uid'))

            developer_payload = {}
            if trust_form_name:
                developer_payload['form_name'] = trust_form_name
            if params.get('payment_completion_action'):
                developer_payload['payment_completion_action'] = params['payment_completion_action']

            order_acquirer = action_params['order'].acquirer  # acquirer заказа или подписки

            oauth = await GetMerchantOAuth(acquirer=order_acquirer, merchant=merchant, shop=order.shop).run()
            trust_payment_data_correct = {
                'paymethod_id': 'trust_web_page',
                'back_url': f'{payments_settings.CALLBACK_ADDRESS}/callback/payment/{order.uid}/{order.order_id}',
                'return_path': return_url,
                'template_tag': action_params.get('template_tag', 'mobile/form'),
                'currency': items[-1].currency,
                'payment_timeout': payments_settings.TRUST_PAYMENT_TIMEOUT,
                'lang': 'ru',
                'payment_mode': 'web_payment',
                'user_email': user_email,
                'pass_params': without_none({
                    'submerchantIdRbs':
                        merchant.get_submerchant_id() if order_acquirer == AcquirerType.TINKOFF else None,
                    'oplata_yakassa_data':
                        {
                            'merchant_oauth': oauth.decrypted_access_token,
                        } if (order_acquirer == AcquirerType.KASSA) else None,
                }),
                'afs_params': without_none({
                    'yandexuid': params.get('yandexuid'),
                    'login_id': params.get('login_id'),
                    'paymentsServiceSlug': params.get('payments_service_slug'),
                }),
                'orders': list(
                    TrustPaymentClient._trust_payment_orders_data(
                        uid=merchant.uid, order=order, items=items, customer_uid=action_params.get('customer_uid'),
                    )
                ),
                'developer_payload': ujson.dumps(developer_payload),
                'fiscal_expects_delivery': order.data.receipt_type == ReceiptType.PREPAID,
            }

            assert trust_payment_data == trust_payment_data_correct
            assert_that(trust_payment_data, equal_to(trust_payment_data_correct))
            payment_create.assert_called_once_with(
                uid=merchant.uid,
                acquirer=order_acquirer,
                payment_data=trust_payment_data,
                headers=headers,
            )

    class TestClearPaymentInTrustAction:
        @pytest.fixture
        def params(self, purchase_token, order, acquirer):
            return {
                'order': order,
                'purchase_token': purchase_token,
                'acquirer': acquirer,
            }

        @pytest.fixture(autouse=True)
        def payment_clear(self, shop_type, trust_client_mocker):
            with trust_client_mocker(shop_type, 'payment_clear', 'cleared_payment') as mock:
                yield mock

        @pytest.fixture
        def action(self, params):
            return ClearPaymentInTrustAction(**params)

        @pytest.fixture
        def returned_func(self, action):
            async def _inner():
                return await action.run()

            return _inner

        @pytest.fixture
        async def returned(self, returned_func):
            return await returned_func()

        @pytest.mark.asyncio
        @parametrize_shop_type
        async def test_trust_clear_calls(self, order, returned, payment_clear, purchase_token, acquirer):
            payment_clear.assert_called_once_with(uid=order.uid, purchase_token=purchase_token, acquirer=acquirer)

        @pytest.mark.asyncio
        async def test_clear_payment_in_trust_action__returned(self, returned):
            assert_that(returned, equal_to('cleared_payment'))

        @pytest.mark.asyncio
        @parametrize_shop_type
        async def test_skip_clear(self, order, payments_settings, payment_clear, returned_func):
            payments_settings.INTERACTION_MERCHANT_SETTINGS[order.uid] = {'trust_skip_clear': True}
            await returned_func()
            payment_clear.assert_not_called()

    class TestUnholdPaymentInTrustAction:
        @pytest.fixture
        def params(self, purchase_token, order, acquirer):
            return {
                'order': order,
                'purchase_token': purchase_token,
                'acquirer': acquirer,
            }

        @pytest.fixture(autouse=True)
        def payment_unhold(self, shop_type, trust_client_mocker):
            with trust_client_mocker(shop_type, 'payment_unhold', 'unheld') as mock:
                yield mock

        @pytest.fixture
        async def returned(self, params):
            return await UnholdPaymentInTrustAction(**params).run()

        def test_unhold_payment_in_trust_action__returned(self, returned):
            assert returned == 'unheld'

        @parametrize_shop_type
        def test_payment_unhold_call(self, order, purchase_token, returned, payment_unhold, acquirer):
            payment_unhold.assert_called_once_with(uid=order.uid, acquirer=acquirer, purchase_token=purchase_token)

    class TestGetPaymentInfoInTrustAction:
        @pytest.fixture
        def params(self, purchase_token, order, acquirer):
            return {
                'order': order,
                'purchase_token': purchase_token,
                'acquirer': acquirer,
            }

        @pytest.fixture(autouse=True)
        def payment_get(self, shop_type, trust_client_mocker):
            with trust_client_mocker(shop_type, 'payment_get', 'payment') as mock:
                yield mock

        @pytest.fixture
        async def returned(self, params):
            return await GetPaymentInfoInTrustAction(**params).run()

        def test_get_payment_info_in_trust_action__returned(self, returned):
            assert returned == 'payment'

        @parametrize_shop_type
        def test_payment_get_calls(self, order, returned, payment_get, purchase_token, acquirer):
            payment_get.assert_called_once_with(
                uid=order.uid,
                acquirer=acquirer,
                purchase_token=purchase_token,
                with_terminal_info=True
            )

    class TestGetPaymentStatusInTrustAction:
        @pytest.fixture
        def params(self, purchase_token, order, acquirer):
            return {
                'order': order,
                'purchase_token': purchase_token,
                'acquirer': acquirer,
            }

        @pytest.fixture(autouse=True)
        def payment_status(self, shop_type, trust_client_mocker):
            with trust_client_mocker(shop_type, 'payment_status', TransactionStatus.HELD) as mock:
                yield mock

        @pytest.fixture
        async def returned(self, params):
            return await GetPaymentStatusInTrustAction(**params).run()

        @pytest.mark.asyncio
        async def test_get_payment_status_in_trust_action__returned(self, returned):
            assert_that(returned, equal_to(TransactionStatus.HELD))

        @pytest.mark.asyncio
        @parametrize_shop_type
        async def test_payment_status_calls(self, order, returned, payment_status, purchase_token, acquirer):
            payment_status.assert_called_once_with(uid=order.uid, acquirer=acquirer, purchase_token=purchase_token)

    class TestGetSubscriptionInTrustAction:
        @pytest.fixture
        async def params(self, storage, items, order, acquirer):
            if order.customer_subscription_id:
                order = await storage.order.get(order.uid,
                                                customer_subscription_id=order.customer_subscription_id,
                                                with_customer_subscription=True)
                order.items = items
            return {'order': order, 'acquirer': acquirer}

        @pytest.fixture
        def trust_response(self, rands):
            return {rands(): rands() for _ in range(5)}

        @pytest.fixture(autouse=True)
        def subscription_get(self, shop_type, trust_response, trust_client_mocker):
            with trust_client_mocker(shop_type, '_subscription_get', trust_response) as mock:
                yield mock

        @pytest.fixture
        def returned_func(self, params):
            async def _inner():
                return await GetSubscriptionInTrustAction(**params).run()

            return _inner

        @pytest.fixture
        async def returned(self, returned_func):
            return await returned_func()

        @pytest.mark.asyncio
        async def test_get_subscription_in_trust_action__returned(self, returned, trust_response):
            assert returned == trust_response

        @pytest.mark.asyncio
        @parametrize_shop_type
        async def test_subscription_get_calls(self, order, returned, subscription_get, acquirer):
            order_id = BaseTrustClient.make_order_id(order.uid, order.order_id, order.items[0].product_id,
                                                     order.customer_uid, order.data.version)
            subscription_get.assert_called_once_with(uid=order.uid, acquirer=acquirer, order_id=order_id, headers={})


@pytest.mark.usefixtures('order')
class TestTrustOrder(BaseTestTrust, BaseOrderAcquirerTest):
    @pytest.fixture
    async def action_params(self, params, order, order_acquirer, storage):
        order.acquirer = order_acquirer
        await storage.order.save(order)
        params['order'] = order
        return params

    @pytest.fixture(autouse=True)
    async def setup_order_acquirer(self, storage, order_acquirer, create_merchant_oauth, merchant, merchant_oauth_mode):
        if order_acquirer == AcquirerType.KASSA:
            uid = merchant.uid if not merchant.parent else merchant.parent.uid
            oauth = [await create_merchant_oauth(uid, mode=merchant_oauth_mode)]
            merchant.oauth = oauth
            await storage.merchant.save(merchant)

    class TestGetSubscriptionInTrustAction(BaseTestTrust.TestGetSubscriptionInTrustAction):
        @pytest.mark.asyncio
        async def test_get_subscription_in_trust_action__returned(self, returned_func):
            with pytest.raises(CoreFailError):
                await returned_func()

        @pytest.mark.asyncio
        async def test_subscription_get_calls(self, returned_func):
            with pytest.raises(CoreFailError):
                await returned_func()

    class TestProcessNewOrderInTrustAction(BaseTestTrust.TestProcessNewOrderInTrustAction):
        @pytest.fixture
        def orders_create_exception(self):
            return None

        @pytest.fixture(autouse=True)
        def orders_create(self, shop_type, trust_client_mocker, orders_create_exception):
            with trust_client_mocker(shop_type, 'orders_create', [], exc=orders_create_exception) as mock:
                yield mock

        @pytest.mark.asyncio
        @parametrize_shop_type
        async def test_orders_create_calls(self, items, order, merchant, action_params, params, returned,
                                           orders_create):
            orders_create.assert_called_once_with(
                merchant=merchant,
                uid=merchant.uid,
                acquirer=action_params['order'].acquirer,
                order=order,
                items=items,
                customer_uid=params.get('customer_uid'),
                service_fee=params.get('service_fee'),
                commission=params.get('commission'),
            )

        class TestOrdersCreateException:
            @pytest.fixture
            def orders_create_exception(self):
                return TrustUidNotFound(method='POST', message='CUSTOMER_UID_INVALID')

            @pytest.mark.asyncio
            async def test_orders_create_exception(self, returned_func, orders_create_exception):
                with pytest.raises(CoreDataError) as exc_info:
                    await returned_func()
                assert orders_create_exception.message == exc_info.value.message

        class TestItemsMarkup:
            @pytest.fixture
            async def items_without_markup(self, items):
                for item in items:
                    item.markup = None
                return items

            @pytest.mark.asyncio
            @parametrize_shop_type
            async def test_payment_markup_not_called(self, payment_markup, items_without_markup, returned):
                payment_markup.assert_not_called()

            @pytest.mark.asyncio
            @parametrize_shop_type
            async def test_payment_markup(self, action_params, order, payment_markup, items, purchase_token, returned):
                payment_markup.assert_called_once_with(
                    uid=order.uid,
                    acquirer=action_params['order'].acquirer,
                    items=items,
                    purchase_token=purchase_token,
                    order=order,
                    customer_uid=action_params.get('customer_uid'),
                )


@pytest.mark.usefixtures('order_with_customer_subscription')
class TestTrustOrderWithCustomerSubscription(BaseTestTrust, BaseSubscriptionAcquirerTest):
    @pytest.fixture(autouse=True)
    async def setup_order_acquirer(self, storage, subscription_acquirer, create_merchant_oauth, merchant,
                                   merchant_oauth_mode):
        if subscription_acquirer == AcquirerType.KASSA:
            merchant_id = merchant.merchant_id if not merchant.parent else merchant.parent.merchant_id
            oauth = [await create_merchant_oauth(merchant_id, mode=merchant_oauth_mode)]
            merchant.oauth = oauth
            await storage.merchant.save(merchant)

    @pytest.fixture
    def items_amount(self):
        return 1

    class TestProcessNewOrderInTrustAction(BaseTestTrust.TestProcessNewOrderInTrustAction):
        @pytest.fixture
        def subscription_create_exception(self):
            return None

        @pytest.fixture
        def customer_uid(self, randn):
            return randn()

        @pytest.fixture(params=(True, False))
        def with_trial(self, request):
            return request.param

        @pytest.fixture
        async def action_params(self, params, with_trial, order, subscription_acquirer, customer_uid, storage):
            params['customer_uid'] = customer_uid
            order = await storage.order.get(order.uid, order.order_id,
                                            with_customer_subscription=True,
                                            select_customer_subscription=True)
            if not with_trial:
                order.customer_subscription.subscription.trial_period_amount = None
                order.customer_subscription.subscription.trial_period_units = None
            order.acquirer = subscription_acquirer
            await storage.order.save(order)
            params['order'] = order
            return params

        @pytest.fixture(autouse=True)
        def subscription_create(self, shop_type, trust_client_mocker, subscription_create_exception):
            with trust_client_mocker(shop_type, 'subscription_create', [],
                                     exc=subscription_create_exception) as mock:
                yield mock

        @pytest.mark.asyncio
        @parametrize_shop_type
        async def test_subscription_create_calls(self, returned, subscription_create):
            subscription_create.assert_called_once()

        class TestNoCustomerIdException:
            @pytest.fixture
            def customer_uid(self):
                return None

            @pytest.mark.asyncio
            async def test_exception(self, returned_func):
                with pytest.raises(SubscriptionRequiresCustomerUid):
                    await returned_func()

        class TestSubscriptionCreateException:
            @pytest.fixture
            def subscription_create_exception(self):
                return TrustUidNotFound(method='POST', message='CUSTOMER_UID_INVALID')

            @pytest.mark.asyncio
            async def test_subscription_exception(self, returned_func, subscription_create_exception):
                with pytest.raises(CoreDataError) as exc_info:
                    await returned_func()
                assert subscription_create_exception.message == exc_info.value.message
