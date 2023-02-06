from contextlib import contextmanager

import pytest

from sendr_utils import alist, anext

from hamcrest import (
    all_of, assert_that, contains_inanyorder, has_entries, has_item, has_key, has_properties, instance_of, is_, is_not,
    match_equality
)

from mail.payments.payments.core.actions.order.get_trust_env import GetOrderTrustEnvAction
from mail.payments.payments.core.actions.order.pay import (
    CorePayOrderAction, PayOrderByHashAction, ProcessNewOrderInTrustAction, StartOrderAction,
    StartOrderByPayTokenAction
)
from mail.payments.payments.core.actions.update_transaction import UpdateTransactionAction
from mail.payments.payments.core.entities.enums import (
    PAYMETHOD_ID_OFFLINE, AcquirerType, OperationKind, OrderKind, OrderSource, PayStatus, RefundStatus,
    TransactionStatus, TrustEnv
)
from mail.payments.payments.core.entities.log import OrderPaymentStartedLog
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.service import Service, ServiceMerchant, ServiceOptions
from mail.payments.payments.core.exceptions import (
    CoreActionDenyError, CoreFailError, MerchantNotFoundError, OrderAbandonedError, OrderAlreadyPaidError,
    OrderArchivedError, OrderCancelledError, OrderCannotBePaidWithoutEmailError, OrderCannotBePaidWithoutReturnUrlError,
    OrderNotAllowedByModerationPolicyError, OrderNotFoundError, OrderRefundCannotBePaidError,
    ServiceMerchantNotEnabledError, ServiceMerchantNotFoundError
)
from mail.payments.payments.interactions.trust.entities import PaymentMode
from mail.payments.payments.tests.base import BaseAcquirerTest, BaseTestRequiresModeration, parametrize_shop_type
from mail.payments.payments.tests.utils import dummy_coro


@pytest.fixture
def decrypted(order):
    return {'uid': order.uid, 'order_id': order.order_id}


@pytest.fixture
def crypto_mock(mocker, decrypted):
    @contextmanager
    def dummy(*args, **kwargs):
        yield decrypted

    crypto = mocker.Mock()
    crypto.decrypt_payment = dummy
    return crypto


@pytest.mark.usefixtures('base_merchant_action_data_mock')
class TestCorePayOrderAction(BaseAcquirerTest, BaseTestRequiresModeration):
    @pytest.fixture
    def transaction_data(self):
        return None

    @pytest.fixture
    def email(self):
        return None

    @pytest.fixture
    def order_email(self, rands):
        return rands()

    @pytest.fixture
    def template(self):
        return 'desktop'

    @pytest.fixture
    def trust_env(self):
        return TrustEnv.PROD

    @pytest.fixture
    def return_url(self, rands):
        return rands()

    @pytest.fixture
    def payment_mode(self):
        return PaymentMode.WEB_PAYMENT

    @pytest.fixture
    def order_return_url(self, rands):
        return rands()

    @pytest.fixture
    def overwrite_return_url(self):
        return True

    @pytest.fixture(autouse=True)
    def get_order_trust_env_action_mock(self, mock_action, trust_env):
        return mock_action(GetOrderTrustEnvAction, trust_env)

    @pytest.fixture(autouse=True)
    def update_transaction_action_mock(self, mock_action, transaction):
        return mock_action(UpdateTransactionAction, transaction)

    @pytest.fixture
    def trust_action_result(self, rands):
        return {'purchase_token': rands(), 'payment_url': rands(), 'trust_payment_id': rands()}

    @pytest.fixture(autouse=True)
    def trust_action_mock(self, mock_action, trust_action_result):
        return mock_action(ProcessNewOrderInTrustAction, trust_action_result)

    @pytest.fixture
    def order_data(self, order_email, order_return_url, order_data):
        return {
            **order_data,
            'return_url': order_return_url,
            'user_email': order_email,
        }

    @pytest.fixture
    async def updated_order(self, storage, order, returned):
        return await storage.order.get(order.uid, order.order_id)

    @pytest.fixture
    async def created_transaction(self, storage, updated_order):
        return await storage.transaction.get_last_by_order(updated_order.uid, updated_order.order_id)

    @pytest.fixture
    def params(self, return_url, email, payment_mode, template, overwrite_return_url, randn, rands):
        return {
            'hash_': rands(),
            'overwrite_return_url': overwrite_return_url,
            'return_url': return_url,
            'trust_form_name': rands(),
            'template': template,
            'customer_uid': randn(),
            'yandexuid': rands(),
            'login_id': rands(),
            'paymethod_id': rands(),
            'email': email,
            'payment_mode': payment_mode,
            'payment_completion_action': rands(),
        }

    @pytest.fixture
    def returned_func(self, crypto_mock, params):
        async def _inner(**kwargs):
            kwargs = {**params, **kwargs}
            CorePayOrderAction.context.crypto_mock = crypto_mock
            return await CorePayOrderAction(**kwargs).run()

        return _inner

    @pytest.fixture
    def get_order(self, order, storage):
        async def _inner():
            return await storage.order.get(uid=order.uid, order_id=order.order_id)

        return _inner

    @pytest.fixture
    async def log_service(self, updated_order, storage):
        service = None
        if updated_order.service_client_id is not None and updated_order.service_merchant_id is not None:
            service = await storage.service.get_by_related(
                service_client_id=updated_order.service_client_id,
                service_merchant_id=updated_order.service_merchant_id,
            )
        return service

    def test_returned(self, merchant, trust_env, trust_action_result, returned):
        assert returned == {
            'purchase_token': trust_action_result['purchase_token'],
            'payment_url': trust_action_result['payment_url'],
            'acquirer': merchant.acquirer,
            'environment': trust_env,
            'payment_systems_options': merchant.options.payment_systems,
            'merchant': merchant,
        }

    def test_logged(self, returned, updated_order, log_service, pushers_mock):
        merchant = returned['merchant']
        oauth_mode = None if updated_order.merchant_oauth_mode is None else updated_order.merchant_oauth_mode.value
        service_id = log_service.service_id if log_service else None
        service_name = log_service.name if log_service else None
        assert_that(
            pushers_mock.log.push.call_args[0][0],
            all_of(
                is_(OrderPaymentStartedLog),
                has_properties(
                    merchant_name=merchant.name,
                    merchant_uid=merchant.uid,
                    merchant_acquirer=updated_order.get_acquirer(merchant.acquirer),
                    order_id=updated_order.order_id,
                    customer_uid=updated_order.customer_uid,
                    price=updated_order.log_price,
                    purchase_token=returned['purchase_token'],
                    service_id=service_id,
                    service_name=service_name,
                    sdk_api_created=updated_order.created_by_source == OrderSource.SDK_API,
                    sdk_api_pay=updated_order.pay_by_source == OrderSource.SDK_API,
                    created_by_source=updated_order.created_by_source,
                    pay_by_source=updated_order.pay_by_source,
                    merchant_oauth_mode=oauth_mode,
                )
            )
        )
        assert True

    @pytest.mark.parametrize('acquirer', [AcquirerType.TINKOFF])
    @pytest.mark.parametrize('payment_mode', list(PaymentMode))
    @pytest.mark.parametrize('return_url', (None, 'http://returned-url'))
    @pytest.mark.parametrize('order_return_url', (None, 'http://returned-url'))
    @pytest.mark.asyncio
    async def test_return_url_error(self, payment_mode, return_url, order, returned_func, noop_manager):
        manager = pytest.raises(OrderCannotBePaidWithoutReturnUrlError) if (
            payment_mode in [PaymentMode.WEB_PAYMENT, PaymentMode.EXTERNAL_WEB_PAYMENT]
            and return_url is None
            and order.return_url is None
        ) else noop_manager()

        with manager:
            await returned_func()

    @pytest.mark.parametrize('acquirer', [AcquirerType.TINKOFF])
    @pytest.mark.parametrize('email', (None, 'pay@yandex-team.ru'))
    @pytest.mark.parametrize('order_email', (None, 'pay@yandex-team.ru'))
    @pytest.mark.asyncio
    async def test_email_error(self, email, order_email, returned_func, noop_manager):
        manager = (
            pytest.raises(OrderCannotBePaidWithoutEmailError)
            if (email is None and order_email is None)
            else noop_manager()
        )
        with manager:
            await returned_func()

    @pytest.mark.asyncio
    async def test_masks_exception(self, returned_func, crypto_mock):
        crypto_mock.decrypt_payment = lambda: None
        with pytest.raises(OrderNotFoundError):
            await returned_func()

    @pytest.mark.parametrize('decrypted_update,exception', [
        [{'uid': -1}, MerchantNotFoundError],
        [{'order_id': -1}, OrderNotFoundError],
    ])
    @pytest.mark.asyncio
    async def test_masked_not_found(self, returned_func, decrypted, decrypted_update, exception):
        decrypted.update(decrypted_update)
        with pytest.raises(exception):
            await returned_func()

    @pytest.mark.usefixtures('order_with_customer_subscription')
    @pytest.mark.asyncio
    async def test_customer_subscription(self, returned_func):
        with pytest.raises(OrderNotFoundError):
            await returned_func()

    @pytest.mark.asyncio
    async def test_saves_order_service_merchant_id(self, storage, order, service_merchant, returned_func):
        order.service_merchant_id = None
        await storage.order.save(order)
        await returned_func(order_service_merchant_id=service_merchant.service_merchant_id)
        order = await storage.order.get(order.uid, order.order_id)
        assert order.service_merchant_id == service_merchant.service_merchant_id

    @pytest.mark.asyncio
    async def test_does_not_reset_service_merchant_id(self, storage, order, service_merchant, returned_func):
        order.service_merchant_id = service_merchant.service_merchant_id
        await storage.order.save(order)
        await returned_func(order_service_merchant_id=None)
        order = await storage.order.get(order.uid, order.order_id)
        assert order.service_merchant_id == service_merchant.service_merchant_id

    @pytest.mark.asyncio
    async def test_send_to_history_task_created(self, storage, order, returned):
        task = await anext(storage.task.find())
        assert_that(task, has_properties({
            'action_name': 'send_to_history_order_action',
            'params': has_entries({
                'action_kwargs': {'uid': order.uid, 'order_id': order.order_id}
            })
        }))

    @pytest.mark.asyncio
    async def test_saves_order_acquirer(self, storage, get_order, merchant, returned):
        order = await get_order()
        assert order.acquirer == merchant.acquirer

    @pytest.mark.asyncio
    @pytest.mark.parametrize('acquirer', [None])
    @pytest.mark.parametrize('trust_env', list(TrustEnv))
    async def test_no_acquirer(self, trust_env, returned_func, noop_manager):
        manager = noop_manager() if trust_env == TrustEnv.SANDBOX else pytest.raises(CoreActionDenyError)
        with manager:
            await returned_func()

    @pytest.mark.asyncio
    @pytest.mark.parametrize(('pay_status', 'exception'), (
        (PayStatus.ABANDONED, OrderAbandonedError),
        (PayStatus.CANCELLED, OrderCancelledError),
    ))
    async def test_error_by_pay_status(self, pay_status, exception, storage, order, returned_func):
        order.pay_status = pay_status
        await storage.order.save(order)

        with pytest.raises(exception):
            await returned_func()

    def test_customer_uid(self, returned, updated_order, params):
        assert updated_order.customer_uid == params['customer_uid']

    @pytest.mark.parametrize(('template', 'email'), (('desktop', None), ('mobile', 'a@ya.ru')))
    def test_calls_payment_create(self, merchant, email, items, transaction, trust_action_mock, params, returned,
                                  updated_order):
        updated_order.items = items
        trust_action_mock.assert_called_once_with(**{
            'merchant': merchant,
            'order': updated_order,
            'items': items,
            'return_url': params['return_url'],
            'trust_form_name': params['trust_form_name'],
            'template_tag': 'mobile/form' if params['template'] == 'mobile' else 'desktop/form',
            'yandexuid': params['yandexuid'],
            'login_id': params['login_id'],
            'customer_uid': params['customer_uid'],
            'user_email': email or updated_order.user_email,
            'paymethod_id': params['paymethod_id'],
            'payment_mode': params['payment_mode'],
            'payment_completion_action': params['payment_completion_action'],
        })

    @pytest.mark.asyncio
    async def test_changelog(self, returned, storage, updated_order):
        assert_that(
            await alist(storage.change_log.find()),
            contains_inanyorder(
                has_properties(dict(
                    uid=updated_order.uid,
                    revision=updated_order.revision - 1,
                    operation=OperationKind.UPDATE_ORDER,
                    arguments={
                        'order_id': updated_order.order_id,
                        'pay_status': updated_order.pay_status.value,
                        'user_email': updated_order.user_email,
                    }
                ))
            )
        )

    class TestOrderGetter:
        @pytest.fixture
        def order_getter(self, mocker, order):
            mock = mocker.Mock()
            mock.return_value = dummy_coro(order)
            return mock

        @pytest.fixture
        def params(self, order_getter, params):
            return {
                **params,
                'order_getter': order_getter,
            }

        def test_order_getter__called(self, returned, order_getter):
            order_getter.assert_called_once_with()

    class TestTrustParamsGetter:
        @pytest.fixture
        def trust_params_getter_result(self, rands):
            return {'paymethod_id': rands()}

        @pytest.fixture
        def trust_params_getter(self, mocker, trust_params_getter_result):
            mock = mocker.Mock()
            mock.return_value = dummy_coro(trust_params_getter_result)
            return mock

        @pytest.fixture
        def params(self, trust_params_getter, params):
            return {
                **params,
                'trust_params_getter': trust_params_getter,
            }

        def test_trust_params_getter__called(self, returned, trust_params_getter, updated_order):
            updated_order.items = []
            trust_params_getter.assert_called_once_with(updated_order)

        def test_trust_params_getter__calls_payment_create(self, returned, trust_action_mock,
                                                           trust_params_getter_result):
            assert_that(trust_action_mock.mock_calls[0][2], has_entries(**trust_params_getter_result))

    class TestWithoutHash:
        @pytest.fixture
        def param_uid(self, order):
            return order.uid

        @pytest.fixture
        def param_merchant(self):
            return None

        @pytest.fixture
        def param_order_id(self, order):
            return order.order_id

        @pytest.fixture
        def params(self, param_uid, param_merchant, param_order_id, params):
            return {
                **params,
                'uid': param_uid,
                'merchant': param_merchant,
                'order_id': param_order_id,
                'order_by_hash': False,
            }

        @pytest.mark.asyncio
        @pytest.mark.parametrize('param_uid', [None])
        async def test_without_hash__no_hash_no_uid(self, returned_func):
            with pytest.raises(CoreFailError):
                await returned_func()

        @pytest.mark.asyncio
        @pytest.mark.parametrize('param_order_id', [None])
        async def test_without_hash__no_hash_no_order_id(self, returned_func):
            with pytest.raises(CoreFailError):
                await returned_func()

        def test_without_hash__no_hash(self, returned):
            pass

        class TestMerchant:
            @pytest.fixture
            def param_merchant(self, merchant):
                return merchant

            @pytest.mark.parametrize('param_uid', [None])
            def test_without_hash__use_merchant(self, returned):
                pass

    class TestServiceData:
        @pytest.fixture
        def service_data(self, rands):
            return {rands(): rands()}

        @pytest.fixture
        def params(self, service_data, params):
            return {
                **params,
                'service_data': service_data,
            }

        @pytest.mark.asyncio
        async def test_service_data__saved(self, service_data, storage, order, returned):
            updated_order = await storage.order.get(order.uid, order.order_id)
            assert service_data == updated_order.data.service_data

        @pytest.mark.asyncio
        @pytest.mark.parametrize('service_data', (None,))
        async def test_service_data__none_ignored(self, rands, storage, order, returned_func):
            service_data = order.data.service_data = {rands(): rands()}
            await storage.order.save(order)
            await returned_func()
            updated_order = await storage.order.get(order.uid, order.order_id)
            assert service_data == updated_order.data.service_data

    class TestResetPaymethodIdOffline:
        @pytest.fixture(autouse=True)
        async def setup(self, storage, order):
            order.paymethod_id = PAYMETHOD_ID_OFFLINE
            await storage.order.save(order)

        @pytest.mark.asyncio
        async def test_reset_paymethod_id_offline(self, updated_order):
            assert updated_order.paymethod_id is None

    class TestActionDeny(BaseTestRequiresModeration.TestActionDeny):
        @pytest.fixture(params=TrustEnv)
        def trust_env(self, request):
            return request.param

        @pytest.mark.asyncio
        async def test_action_deny__raises_error(self, trust_env, returned_func, noop_manager):
            manager = pytest.raises(CoreActionDenyError) if trust_env == TrustEnv.PROD else noop_manager()
            with manager:
                await returned_func()

    class TestOrderDataIsUpdated:
        @pytest.fixture
        def params(self, rands, params):
            return {
                **params,
                'turboapp_id': rands(),
                'tsid': rands(),
                'psuid': rands(),
            }

        @pytest.mark.asyncio
        async def test_order_data_is_updated(self, returned, get_order, params):
            order = await get_order()
            assert_that(
                order.data,
                has_properties({
                    'turboapp_id': params['turboapp_id'],
                    'tsid': params['tsid'],
                    'psuid': params['psuid'],
                })
            )

    class TestOrderLogPushersCall:
        @pytest.fixture
        def params(self, rands, params):
            return {
                **params,
                'turboapp_id': rands(),
                'tsid': rands(),
                'psuid': rands(),
            }

        def test_order_log_pushers_call(self, returned, pushers_order_calls, params, payments_settings):
            """Проверяем, что логгер событий был вызван с нужными данными"""
            log_objects = [args[0] for args, kwargs in pushers_order_calls]
            assert_that(
                log_objects,
                has_item(all_of(
                    instance_of(OrderPaymentStartedLog),
                    has_properties({
                        'pay_token': payments_settings.PAY_TOKEN_PREFIX + params['hash_'],
                        'turboapp_id': params['turboapp_id'],
                        'psuid': params['psuid'],
                        'tsid': params['tsid'],
                    })
                )))

    class TestCommission:
        @pytest.fixture
        def order_data(self, order_data):
            order_data['commission'] = 105
            return order_data

        def test_calls_payment_create(self, merchant, email, items, transaction, trust_action_mock, params, returned,
                                      updated_order):
            updated_order.items = items
            trust_action_mock.assert_called_once_with(**{
                'merchant': merchant,
                'order': updated_order,
                'items': items,
                'return_url': params['return_url'],
                'trust_form_name': params['trust_form_name'],
                'template_tag': 'mobile/form' if params['template'] == 'mobile' else 'desktop/form',
                'yandexuid': params['yandexuid'],
                'login_id': params['login_id'],
                'customer_uid': params['customer_uid'],
                'user_email': email or updated_order.user_email,
                'paymethod_id': params['paymethod_id'],
                'payment_mode': params['payment_mode'],
                'payment_completion_action': params['payment_completion_action'],
                'commission': 105,
            })

    class TestOverwriteReturnUrl:
        @pytest.fixture
        def order_return_url(self, rands):
            return rands()

        @pytest.fixture(autouse=True)
        async def order(self, storage, order, order_return_url):
            order.return_url = order_return_url
            return await storage.order.save(order)

        @pytest.mark.parametrize('overwrite_return_url,order_return_url', [(True, '123'), (False, None)])
        def test_overwrite_return_url__overwrite(self, trust_action_mock, return_url, returned):
            assert_that(trust_action_mock.call_args_list[0][1], has_entries({'return_url': return_url}))

        @pytest.mark.parametrize('overwrite_return_url', [False])
        def test_overwrite_return_url__deny(self, trust_action_mock, order_return_url, returned):
            assert_that(trust_action_mock.call_args_list[0][1], has_entries({'return_url': order_return_url}))

    class TestActiveLastTransaction:
        @pytest.fixture
        def transaction_data(self):
            return {
                'status': TransactionStatus.ACTIVE,
                'trust_payment_url': '123-payment_url',
            }

        def test_active_last_transaction__no_new_transaction(self, returned, transaction, created_transaction):
            assert transaction == created_transaction

        def test_active_last_transaction__updates_transaction(self, returned, update_transaction_action_mock):
            update_transaction_action_mock.assert_called_once()

        def test_active_last_transaction__returned(self, transaction, returned):
            assert returned['payment_url'] == transaction.trust_payment_url

    class TestChecksOrder:
        @pytest.fixture
        async def extra_order(self, storage, shop, merchant):
            return await storage.order.create(Order(
                uid=merchant.uid,
                shop_id=shop.shop_id,
                pay_status=PayStatus.PAID,
            ))

        @pytest.fixture(params=[
            [
                {'kind': OrderKind.REFUND, 'pay_status': None, 'refund_status': RefundStatus.REQUESTED},
                OrderRefundCannotBePaidError
            ],
            [{'pay_status': PayStatus.PAID}, OrderAlreadyPaidError],
            [{'pay_status': PayStatus.IN_PROGRESS}, OrderAlreadyPaidError],
            [{'active': False}, OrderArchivedError],
            [{'pay_status': PayStatus.MODERATION_NEGATIVE}, OrderNotAllowedByModerationPolicyError],
            [{'pay_status': PayStatus.IN_MODERATION}, OrderAlreadyPaidError]
        ])
        def test_data(self, request):
            return request.param

        @pytest.fixture
        def order_data(self, order_data, extra_order, test_data):
            if test_data[0].get('kind') == OrderKind.REFUND:
                test_data[0]['original_order_id'] = extra_order.order_id

            return {
                **order_data,
                **test_data[0]
            }

        @pytest.mark.asyncio
        async def test_checks_order__fails(self, test_data, returned_func):
            with pytest.raises(test_data[1]):
                await returned_func()

    class TestBadLastTransaction:
        @pytest.fixture(autouse=True)
        def create_order_status_task_mock(self, mocker):
            return mocker.spy(CorePayOrderAction, 'create_order_status_task')

        @pytest.fixture(params=(None, TransactionStatus.CANCELLED, TransactionStatus.FAILED))
        def transaction_data(self, request):
            if request.param is None:
                return None
            return {'status': request.param}

        @pytest.fixture(params=(None, 'desktop', 'mobile'))
        def template(self, request):
            return request.param

        @pytest.mark.parametrize('email', ('pay@yandex-team.ru',))
        def test_bad_last_transaction__updates_order(self, transaction, params, returned, updated_order):
            assert all([
                updated_order.user_email == params['email'],
                updated_order.user_description == params.get('description'),
            ])

        def test_bad_last_transaction__updates_order_data(self, transaction, params, updated_order):
            assert_that(updated_order.data, has_properties({
                'trust_form_name': params.get('trust_form_name'),
                'trust_template': params.get('template'),
            }))

        def test_bad_last_transaction__creates_transaction(self, transaction, trust_action_result, returned,
                                                           created_transaction):
            assert all([
                transaction is None or created_transaction.tx_id > transaction.tx_id,
                created_transaction.status == TransactionStatus.ACTIVE,
                created_transaction.trust_purchase_token == trust_action_result['purchase_token'],
                created_transaction.trust_payment_url == trust_action_result['payment_url'],
            ])

        @parametrize_shop_type
        def test_bad_last_transaction__calls_payment_create(self, merchant, order, items, updated_order, transaction,
                                                            trust_action_mock,
                                                            payment_mode, params, returned):
            updated_order.items = items
            trust_action_mock.assert_called_once_with(**{
                'merchant': merchant,
                'order': updated_order,
                'items': items,
                'return_url': params['return_url'],
                'trust_form_name': params.get('trust_form_name'),
                'template_tag': f"{params.get('template') or 'mobile'}/form",
                'yandexuid': params['yandexuid'],
                'login_id': params['login_id'],
                'customer_uid': params['customer_uid'],
                'user_email': order.user_email or params['email'],
                'paymethod_id': params['paymethod_id'],
                'payment_mode': payment_mode,
                'payment_completion_action': params['payment_completion_action'],
            })

        class TestPayStatusNew:
            @pytest.fixture
            def order_data(self, order_data):
                return {
                    **order_data,
                    'pay_status': PayStatus.NEW
                }

            def test_pay_status_new__create_callback_not_called(self, create_order_status_task_mock, returned):
                create_order_status_task_mock.assert_not_called()

        class TestPayStatusRejected:
            @pytest.fixture
            def order_data(self, order_data):
                return {
                    **order_data,
                    'pay_status': PayStatus.REJECTED
                }

            def test_pay_status_rejected__create_callback_called(self, merchant, create_order_status_task_mock,
                                                                 updated_order):
                updated_order.items = []
                create_order_status_task_mock.assert_called_once_with(
                    match_equality(instance_of(CorePayOrderAction)),
                    order=updated_order,
                    merchant=merchant
                )

            def test_pay_status_rejected__sets_status_to_new(self, updated_order):
                assert updated_order.pay_status == PayStatus.NEW

            class TestCreateServiceCallbackTaskCalled:
                @pytest.fixture
                def order(self, order_with_service):
                    return order_with_service

                def test_pay_status_rejected__create_service_callback_task_called(self,
                                                                                  updated_order,
                                                                                  merchant,
                                                                                  mocker,
                                                                                  service_with_related,
                                                                                  create_order_status_task_mock,
                                                                                  returned):
                    updated_order.items = []
                    assert create_order_status_task_mock.mock_calls == [
                        mocker.call(
                            match_equality(instance_of(CorePayOrderAction)),
                            order=updated_order,
                            service=service_with_related
                        ),
                        mocker.call(
                            match_equality(instance_of(CorePayOrderAction)),
                            order=updated_order,
                            merchant=merchant
                        )
                    ]

    class TestOrderWithServiceMerchant:
        @pytest.fixture
        def set_service_to_order(self, storage):
            async def _set_service_to_order(service, order):
                service = await storage.service.create(service)
                service_merchant = await storage.service_merchant.create(
                    ServiceMerchant(uid=order.uid, service_id=service.service_id, entity_id='', description='')
                )
                order.service_merchant_id = service_merchant.service_merchant_id
                return await storage.order.save(order)

            return _set_service_to_order

        @pytest.mark.asyncio
        async def test_service_fee_supplied(self, returned_func, order, set_service_to_order, trust_action_mock):
            service = Service(name='the-name', options=ServiceOptions(service_fee=10))
            order = await set_service_to_order(service, order)

            await returned_func(uid=order.uid, order_id=order.order_id)

            assert_that(
                trust_action_mock.call_args.kwargs,
                has_entries({
                    'service_fee': 10,
                })
            )

        @pytest.mark.asyncio
        async def test_slug_supplied_when_antifraud_is_set(
            self, returned_func, order, set_service_to_order, trust_action_mock
        ):
            service = Service(name='the-name', antifraud=True, slug='the-slug')
            order = await set_service_to_order(service, order)

            await returned_func(uid=order.uid, order_id=order.order_id)

            assert_that(
                trust_action_mock.call_args.kwargs,
                has_entries({
                    'payments_service_slug': 'the-slug',
                })
            )

        @pytest.mark.asyncio
        async def test_slug_not_supplied_when_antifraud_is_clear(
            self, returned_func, order, set_service_to_order, trust_action_mock
        ):
            service = Service(name='the-name', antifraud=False, slug='the-slug')
            order = await set_service_to_order(service, order)

            await returned_func(uid=order.uid, order_id=order.order_id)

            assert_that(
                trust_action_mock.call_args.kwargs,
                is_not(has_key('payments_service_slug')),
            )


class TestPayOrderByHashAction:
    @pytest.fixture
    def mock_core_pay_order_action_result(self, rands):
        return {rands(): rands()}

    @pytest.fixture(autouse=True)
    def mock_core_pay_order_action(self, mock_action, mock_core_pay_order_action_result):
        return mock_action(CorePayOrderAction, mock_core_pay_order_action_result)

    @pytest.fixture
    def params(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await PayOrderByHashAction(**params).run()

        return _inner

    def test_call(self, returned, mock_core_pay_order_action, params):
        mock_core_pay_order_action.assert_called_once_with(**params)


class TestStartOrderAction:
    @pytest.fixture
    def mock_core_pay_order_action_result(self, rands):
        return {rands(): rands()}

    @pytest.fixture(autouse=True)
    def mock_core_pay_order_action(self, mock_action, mock_core_pay_order_action_result):
        return mock_action(CorePayOrderAction, mock_core_pay_order_action_result)

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
    def action(self, mocker, storage, params):
        action = StartOrderAction(**params)
        mocker.spy(action, 'authorize_service_merchant')
        return action

    @pytest.fixture
    def returned_func(self, action, params):
        async def _inner():
            return await action.run()

        return _inner

    def test_call(self, service_merchant, action, returned, mock_core_pay_order_action, proxy_kwargs):
        mock_core_pay_order_action.assert_called_once_with(
            uid=service_merchant.uid,
            trust_params_getter=action._trust_params,
            **proxy_kwargs
        )

    def test_call_auth(self, returned, action):
        action.authorize_service_merchant.assert_called_once_with()

    class TestTrustParams:
        @pytest.mark.asyncio
        @pytest.mark.parametrize('paymethod_id', (None, 'x-card'))
        async def test__trust_params(self, paymethod_id, order, action):
            order.paymethod_id = paymethod_id
            expected = {}
            if paymethod_id:
                expected = {'paymethod_id': 'trust_web_page', 'selected_card_id': paymethod_id}
            assert await action._trust_params(order) == expected


class TestStartOrderByPayTokenAction:
    @pytest.fixture
    async def turboapp_service(self, payments_settings, create_service):
        service = await create_service()
        payments_settings.TURBOAPP_SERVICE_ID = service.service_id
        return service

    @pytest.fixture
    async def service_merchant(self, turboapp_service, merchant, create_service_merchant):
        return await create_service_merchant(
            service_id=turboapp_service.service_id,
            uid=merchant.uid,
            enabled=True,
        )

    @pytest.fixture
    def tsid(self, rands):
        return rands()

    @pytest.fixture
    def psuid(self, rands):
        return rands()

    @pytest.fixture
    def default_kwargs(self, payments_settings, rands, service_merchant, tsid, psuid):
        return {
            'pay_token': payments_settings.PAY_TOKEN_PREFIX + rands(),
            'email': rands(),
            'turboapp_id': service_merchant.entity_id,
            'tsid': tsid,
            'psuid': psuid,
        }

    @pytest.fixture
    def mock_core_pay_order_action_result(self, rands):
        return {rands(): rands()}

    @pytest.fixture(autouse=True)
    def mock_core_pay_order_action(self, mock_action, mock_core_pay_order_action_result):
        return mock_action(CorePayOrderAction, mock_core_pay_order_action_result)

    @pytest.fixture
    def returned_func(self, crypto_mock, default_kwargs):
        async def _inner(**kwargs):
            kwargs = {**default_kwargs, **kwargs}
            StartOrderByPayTokenAction.context.crypto = crypto_mock
            return await StartOrderByPayTokenAction(**kwargs).run()

        return _inner

    def test_returns_core_pay_order_action_result(self, mock_core_pay_order_action_result, returned):
        assert mock_core_pay_order_action_result == returned

    @pytest.mark.asyncio
    @pytest.mark.parametrize('kwargs', (
        pytest.param({}, id='default'),
        pytest.param({'customer_uid': 123}, id='with customer_uid'),
        pytest.param({'pay_by_source': OrderSource.SDK_API}, id='with pay_by_source'),
    ))
    async def test_calls_base_action(self, payments_settings, default_kwargs, mock_core_pay_order_action,
                                     service_merchant,
                                     returned_func,
                                     kwargs):
        await returned_func(**kwargs)
        kwargs = {**default_kwargs, **kwargs}
        mock_core_pay_order_action.assert_called_once_with(
            hash_=kwargs['pay_token'][len(payments_settings.PAY_TOKEN_PREFIX):],
            email=kwargs['email'],
            paymethod_id=None,
            payment_mode=None,
            customer_uid=kwargs.get('customer_uid'),
            pay_by_source=kwargs.get('pay_by_source', OrderSource.UI),
            order_service_merchant_id=service_merchant.service_merchant_id,
            turboapp_id=kwargs['turboapp_id'],
            tsid=kwargs['tsid'],
            psuid=kwargs['psuid'],
        )

    @pytest.mark.asyncio
    async def test_turboapp_service_id_not_set(self, payments_settings, mock_core_pay_order_action, returned_func):
        payments_settings.TURBOAPP_SERVICE_ID = None
        await returned_func()
        assert mock_core_pay_order_action.call_args[1]['order_service_merchant_id'] is None

    @pytest.mark.asyncio
    async def test_turboapp_id_is_not_passed(self, mock_core_pay_order_action, returned_func):
        await returned_func(turboapp_id=None)
        assert mock_core_pay_order_action.call_args[1]['order_service_merchant_id'] is None

    @pytest.mark.asyncio
    async def test_service_merchant_not_found(self, rands, returned_func):
        with pytest.raises(ServiceMerchantNotFoundError):
            await returned_func(turboapp_id=rands())

    @pytest.mark.asyncio
    async def test_service_merchant_not_enabled(self, storage, service_merchant, returned_func):
        service_merchant.enabled = False
        await storage.service_merchant.save(service_merchant)
        with pytest.raises(ServiceMerchantNotEnabledError):
            await returned_func()
