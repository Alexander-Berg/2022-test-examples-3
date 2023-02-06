import pytest

from mail.payments.payments.core.actions.customer_subscription.cancel import (
    CancelCustomerSubscriptionAction, CancelCustomerSubscriptionServiceMerchantAction,
    CoreCancelCustomerSubscriptionAction
)
from mail.payments.payments.core.exceptions import (
    CustomerSubscriptionAlreadyCanceledError, CustomerSubscriptionNotEnabledError
)
from mail.payments.payments.tests.base import (
    BaseOrderAcquirerTest, BaseTestOrderWithCustomerSubscriptionAction, parametrize_shop_type
)
from mail.payments.payments.utils.datetime import utcnow


@pytest.fixture
def params(order_with_customer_subscription, items):
    return {
        'uid': order_with_customer_subscription.uid,
        'customer_subscription_id': order_with_customer_subscription.customer_subscription_id,
    }


@pytest.fixture
def customer_subscription_enabled():
    return True


@pytest.fixture
def customer_subscription_data(customer_subscription_data, customer_subscription_enabled):
    customer_subscription_data['enabled'] = customer_subscription_enabled
    return customer_subscription_data


class TestCoreCancelCustomerSubscriptionAction(
    BaseTestOrderWithCustomerSubscriptionAction, BaseOrderAcquirerTest
):
    @pytest.fixture(autouse=True)
    def get_acquire_mock(self, mock_action, acquirer):
        from mail.payments.payments.core.actions.merchant.get_acquirer import GetAcquirerMerchantAction
        return mock_action(GetAcquirerMerchantAction, acquirer)

    @pytest.fixture
    def utcnow(self, mocker):
        now = utcnow()
        mocker.patch(
            'mail.payments.payments.core.actions.customer_subscription.cancel.utcnow', mocker.Mock(return_value=now)
        )
        return now

    @pytest.fixture(autouse=True)
    def subscription_cancel_mock(self, trust_client_mocker, shop_type):
        with trust_client_mocker(shop_type, 'subscription_cancel', None) as mock:
            yield mock

    @pytest.fixture
    def action(self, crypto):
        CoreCancelCustomerSubscriptionAction.context.crypto = crypto
        return CoreCancelCustomerSubscriptionAction

    @pytest.fixture
    def returned_func(self, action, params):
        async def _inner():
            return await action(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    def test_saved(self, utcnow, returned):
        customer_subscription, _ = returned

        assert all((
            customer_subscription.enabled is False,
            customer_subscription.time_finish == utcnow,
        ))

    @parametrize_shop_type
    def test_subscription_cancel_mock_call(self, order_with_customer_subscription, some_hash, utcnow,
                                           subscription_cancel_mock, returned, items, order_acquirer):
        customer_subscription, _ = returned

        order_with_customer_subscription.items = items
        order_with_customer_subscription.order_hash = some_hash
        order_with_customer_subscription.customer_subscription = customer_subscription
        subscription_cancel_mock.assert_called_once_with(
            uid=order_with_customer_subscription.uid,
            acquirer=order_acquirer,
            order=order_with_customer_subscription,
            item=items[0],
            finish_ts=utcnow,
            customer_uid=order_with_customer_subscription.customer_uid
        )

    @pytest.mark.asyncio
    async def test_already_canceled(self, returned, returned_func):
        with pytest.raises(CustomerSubscriptionAlreadyCanceledError):
            await returned_func()

    @pytest.mark.parametrize('customer_subscription_enabled', (False,))
    @pytest.mark.asyncio
    async def test_not_enabled(self, returned_func):
        with pytest.raises(CustomerSubscriptionNotEnabledError):
            await returned_func()


class TestCancelCustomerSubscriptionAction:
    @pytest.fixture
    def mock_core_action(self, mock_action, customer_subscription_entity, order_entity):
        return mock_action(CoreCancelCustomerSubscriptionAction, (customer_subscription_entity, order_entity))

    @pytest.fixture
    async def returned(self, params):
        return await CancelCustomerSubscriptionAction(**params).run()

    def test_cancel_customer_subscription_call(self, mock_core_action, params, returned):
        mock_core_action.assert_called_once_with(**params)

    def test_cancel_customer_subscription_returned(self,
                                                   mock_core_action,
                                                   returned,
                                                   customer_subscription_entity,
                                                   order_entity):
        returned_customer_subscription, returned_order = returned
        assert returned_customer_subscription == customer_subscription_entity
        assert returned_order == order_entity


class TestCancelCustomerSubscriptionServiceMerchantAction:
    @pytest.fixture
    def mock_core_action(self, mock_action, customer_subscription_entity, order_entity):
        return mock_action(CoreCancelCustomerSubscriptionAction, (customer_subscription_entity, order_entity))

    @pytest.fixture
    def action_params(self, service_merchant, service_client, params):
        return {
            'service_tvm_id': service_client.tvm_id,
            'service_merchant_id': service_merchant.service_merchant_id,
            'customer_subscription_id': params['customer_subscription_id'],
        }

    @pytest.fixture
    async def returned(self, action_params):
        return await CancelCustomerSubscriptionServiceMerchantAction(**action_params).run()

    def test_cancel_customer_subscription_service_merchant_call(self, mock_core_action, params, returned):
        mock_core_action.assert_called_once_with(**params)

    def test_cancel_customer_subscription_service_merchant_returned(self,
                                                                    mock_core_action,
                                                                    returned,
                                                                    customer_subscription_entity,
                                                                    order_entity):
        returned_customer_subscription, returned_order = returned
        assert returned_customer_subscription == customer_subscription_entity
        assert returned_order == order_entity
