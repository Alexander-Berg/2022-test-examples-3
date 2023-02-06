import pytest

from sendr_utils import enum_value

from hamcrest import assert_that, has_entries, has_properties

from mail.payments.payments.core.actions.mixins.callback_task import APICallbackTaskMixin
from mail.payments.payments.core.entities.enums import APICallbackSignMethod, CallbackMessageType
from mail.payments.payments.core.entities.task import Task, TaskType
from mail.payments.payments.utils.helpers import temp_setattr


@pytest.fixture
def action(storage):
    with temp_setattr(APICallbackTaskMixin.context, 'storage', storage):
        yield APICallbackTaskMixin()


@pytest.fixture
def callback_url(rands):
    return rands()


@pytest.fixture
def secret(rands):
    return rands()


@pytest.fixture(params=list(APICallbackSignMethod))
def sign_method(request):
    return request.param


@pytest.fixture
def kwargs():
    return {}


class TestCallbackOnOrderStatusUpdate:
    @pytest.fixture
    def kwargs(self):
        return {}

    @pytest.fixture
    async def returned(self, action, order, kwargs):
        return await action.create_order_status_task(order=order, **kwargs)

    @pytest.fixture
    async def created_task(self, storage, returned):
        return await storage.task.get(returned.task_id)

    class BaseTest:
        @pytest.fixture
        def expected_params(self, callback_url, order, kwargs, sign_method, secret):
            return {
                'callback_url': callback_url,
                'message': {
                    'order_id': order.order_id,
                    'meta': order.data.meta,
                    'new_status': order.pay_status.value,
                    'updated': order.updated.isoformat(),
                },
                'callback_params': has_properties({
                    'sign_method': sign_method,
                    'secret': secret,
                }) if 'merchant' in kwargs else None,
                'callback_message_type': enum_value(CallbackMessageType.ORDER_STATUS_UPDATED),
            }

        def check_returned(self, expected_params, merchant, order, returned):
            assert_that(
                returned,
                has_properties({
                    'task_type': TaskType.API_CALLBACK,
                    'params': has_entries(**expected_params),
                }),
            )

        def check_creates_task(self, returned, created_task):
            assert returned == created_task

    class TestMerchant(BaseTest):
        @pytest.fixture
        def kwargs(self, merchant):
            return {'merchant': merchant}

        @pytest.fixture(autouse=True)
        def setup(self, merchant, sign_method, secret, callback_url, expected_params):
            merchant.api_callback_url = callback_url
            merchant.api_callback_params.sign_method = sign_method
            merchant.api_callback_params.secret = secret
            expected_params['tvm_id'] = None
            expected_params['message']['uid'] = merchant.uid

        def test_merchant__creates_task(self, returned, created_task):
            self.check_creates_task(returned, created_task)

        def test_merchant__returned(self, expected_params, merchant, order, returned):
            self.check_returned(expected_params, merchant, order, returned)

        class TestAPICallbackUrl:
            @pytest.fixture
            def callback_url(self):
                return None

            def test_merchant_api_callback_url__returned(self, returned):
                assert returned is None

    class TestService(BaseTest):
        @pytest.fixture
        def kwargs(self, service_with_related):
            return {'service': service_with_related}

        @pytest.fixture(autouse=True)
        def setup(self, service_client, service_merchant, callback_url, expected_params):
            service_client.api_callback_url = callback_url
            expected_params['tvm_id'] = service_client.tvm_id
            expected_params['message']['service_merchant_id'] = service_merchant.service_merchant_id

        def test_service__creates_task(self, returned, created_task):
            self.check_creates_task(returned, created_task)

        def test_service__returned(self, expected_params, merchant, order, returned):
            self.check_returned(expected_params, merchant, order, returned)

        class TestAPICallbackUrl:
            @pytest.fixture
            def callback_url(self):
                return None

            def test_service_api_callback_url__returned(self, returned):
                assert returned is None


class TestCallbackCreateRefundStatusTask:
    @pytest.fixture
    def returned_func(self, action, refund, kwargs):
        async def _inner():
            return await action.create_refund_status_task(refund, **kwargs)

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.fixture
    async def created_task(self, storage, returned):
        return await storage.task.get(returned.task_id)

    @pytest.mark.asyncio
    async def test_requires_refund(self, action, order):
        with pytest.raises(AssertionError):
            await action.create_refund_status_task(order)

    @pytest.mark.asyncio
    async def test_requires_service_or_merchant(self, returned_func):
        with pytest.raises(RuntimeError):
            await returned_func()

    class BaseTest:
        @pytest.fixture
        def expected_params(self, kwargs, sign_method, secret, refund, callback_url):
            return {
                'callback_message_type': enum_value(CallbackMessageType.REFUND_STATUS_UPDATED),
                'callback_url': callback_url,
                'callback_params': has_properties({
                    'sign_method': sign_method,
                    'secret': secret,
                }) if 'merchant' in kwargs else None,
                'message': {
                    'refund_id': refund.order_id,
                    'meta': refund.data.meta,
                    'order_id': refund.original_order_id,
                    'new_status': refund.refund_status.value,
                    'updated': refund.updated.isoformat(),
                },
            }

        def check_empty_callback_url(self, returned):
            assert returned is None

        def check_returned(self, expected_params, returned):
            assert_that(
                returned,
                has_properties({
                    'task_type': TaskType.API_CALLBACK,
                    'params': has_entries(**expected_params),
                })
            )

        def check_created(self, returned, created_task):
            assert returned == created_task

    class TestMerchant(BaseTest):
        @pytest.fixture
        def kwargs(self, merchant):
            return {'merchant': merchant}

        @pytest.fixture(autouse=True)
        def setup(self, merchant, callback_url, sign_method, secret, expected_params):
            merchant.api_callback_url = callback_url
            merchant.api_callback_params.sign_method = sign_method
            merchant.api_callback_params.secret = secret
            expected_params['tvm_id'] = None
            expected_params['message']['uid'] = merchant.uid

        @pytest.mark.parametrize('callback_url', (None,))
        def test_merchant__empty_callback_url(self, returned):
            self.check_empty_callback_url(returned)

        def test_merchant__returned(self, expected_params, returned):
            self.check_returned(expected_params, returned)

        def test_merchant__created(self, returned, created_task):
            self.check_created(returned, created_task)

    class TestService(BaseTest):
        @pytest.fixture
        def kwargs(self, service_with_related):
            return {'service': service_with_related}

        @pytest.fixture(autouse=True)
        def setup(self, service_client, service_merchant, callback_url, expected_params):
            service_client.api_callback_url = callback_url
            expected_params['tvm_id'] = service_client.tvm_id
            expected_params['message']['service_merchant_id'] = service_merchant.service_merchant_id

        @pytest.mark.parametrize('callback_url', (None,))
        def test_service__empty_callback_url(self, returned):
            self.check_empty_callback_url(returned)

        def test_service__returned(self, expected_params, returned):
            self.check_returned(expected_params, returned)

        def test_service__created(self, returned, created_task):
            self.check_created(returned, created_task)


class TestServiceCallbackOnServiceMerchantRelationUpdate:
    @pytest.fixture
    async def returned(self, action, service_with_related) -> Task:
        return await action.create_service_merchant_updated_task(service=service_with_related)

    @pytest.fixture
    async def created_task(self, storage, returned) -> Task:
        return await storage.task.get(task_id=returned.task_id)

    @pytest.mark.asyncio
    async def test_returned_task_state(self, returned, service_client, service_merchant):
        expected_message = {
            'service_merchant_id': service_merchant.service_merchant_id,
            'enabled': service_merchant.enabled,
            'entity_id': service_merchant.entity_id,
            'updated': service_merchant.updated.isoformat(),
        }
        assert_that(returned, has_properties({
            'task_type': TaskType.API_CALLBACK,
            'params': has_entries(
                message=expected_message,
                callback_url=service_client.api_callback_url,
                tvm_id=service_client.tvm_id,
                callback_message_type=enum_value(CallbackMessageType.SERVICE_MERCHANT_UPDATED)
            )
        }))

    @pytest.mark.asyncio
    async def test_returned_task_equals_created_task(self, returned, created_task):
        assert returned == created_task


class TestServiceCallbackOnCustomerSubscriptionProlongated:
    @pytest.fixture
    async def returned(self, action, customer_subscription, service_with_related) -> Task:
        return await action.create_customer_subscription_prolongated_service_task(
            customer_subscription=customer_subscription,
            service=service_with_related
        )

    @pytest.fixture
    async def created_task(self, storage, returned) -> Task:
        return await storage.task.get(task_id=returned.task_id)

    @pytest.mark.asyncio
    async def test_returned_task_state(self, customer_subscription, service_client, service_merchant, returned):
        assert_that(returned, has_properties({
            'task_type': TaskType.API_CALLBACK,
            'params': has_entries(
                callback_url=service_client.api_callback_url,
                tvm_id=service_client.tvm_id,
                callback_message_type=enum_value(CallbackMessageType.CUSTOMER_SUBSCRIPTION_PROLONGATED),
                message=has_entries(
                    order_id=customer_subscription.order_id,
                    customer_subscription_id=customer_subscription.customer_subscription_id,
                    service_merchant_id=service_merchant.service_merchant_id,
                    updated=customer_subscription.updated.isoformat()
                )
            )
        }))

    @pytest.mark.asyncio
    async def test_returned_task_equals_created_task(self, returned, created_task):
        assert returned == created_task


class TestServiceCallbackOnCustomerSubscriptionStopped:
    @pytest.fixture
    async def returned(self, action, customer_subscription, service_with_related) -> Task:
        return await action.create_customer_subscription_stopped_service_task(
            customer_subscription=customer_subscription,
            service=service_with_related
        )

    @pytest.fixture
    async def created_task(self, storage, returned) -> Task:
        return await storage.task.get(task_id=returned.task_id)

    @pytest.mark.asyncio
    async def test_returned_task_state(self, customer_subscription, service_client, service_merchant, returned):
        assert_that(returned, has_properties({
            'task_type': TaskType.API_CALLBACK,
            'params': has_entries(
                callback_url=service_client.api_callback_url,
                tvm_id=service_client.tvm_id,
                callback_message_type=enum_value(CallbackMessageType.CUSTOMER_SUBSCRIPTION_STOPPED),
                message=has_entries(
                    order_id=customer_subscription.order_id,
                    customer_subscription_id=customer_subscription.customer_subscription_id,
                    service_merchant_id=service_merchant.service_merchant_id,
                    updated=customer_subscription.updated.isoformat()
                )
            )
        }))

    @pytest.mark.asyncio
    async def test_returned_task_equals_created_task(self, returned, created_task):
        assert returned == created_task
