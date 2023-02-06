import pytest

from mail.payments.payments.core.actions.customer_subscription.pay import StartCustomerSubscriptionServiceMerchantAction
from mail.payments.payments.core.actions.order.pay import CorePayOrderAction
from mail.payments.payments.core.entities.enums import AcquirerType
from mail.payments.payments.core.exceptions import CustomerSubscriptionNotFoundError
from mail.payments.payments.tests.base import BaseSubscriptionAcquirerTest


class TestStartCustomerSubscriptionServiceMerchantAction(BaseSubscriptionAcquirerTest):
    @pytest.fixture
    def mock_core_pay_order_action_result(self, rands):
        return {rands(): rands()}

    @pytest.fixture(autouse=True)
    async def setup_order_acquirer(self, storage, subscription_acquirer, create_merchant_oauth, merchant,
                                   merchant_oauth_mode):
        if subscription_acquirer == AcquirerType.KASSA:
            uid = merchant.uid
            oauth = [await create_merchant_oauth(uid, mode=merchant_oauth_mode)]
            merchant.oauth = oauth
            await storage.merchant.save(merchant)

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
            'customer_subscription_id': customer_subscription.customer_subscription_id,
            **proxy_kwargs
        }

    @pytest.fixture
    def action(self, storage, params):
        StartCustomerSubscriptionServiceMerchantAction.context.storage = storage
        yield StartCustomerSubscriptionServiceMerchantAction(**params)
        StartCustomerSubscriptionServiceMerchantAction.context.storage = None

    @pytest.fixture
    def returned_func(self, action, params):
        async def _inner():
            return await action.run()

        return _inner

    def test_call(self, service_merchant, action, returned, mock_core_pay_order_action, proxy_kwargs):
        mock_core_pay_order_action.assert_called_once_with(
            uid=service_merchant.uid,
            order_getter=action._get_order,
            order_by_hash=False,
            **proxy_kwargs
        )

    class TestGetOrder:
        @pytest.fixture(autouse=True)
        async def setup(self, action):
            await action.pre_handle()

        @pytest.mark.asyncio
        async def test_get_order__returned(self, order_with_customer_subscription, action):
            returned = await action._get_order()
            assert returned == order_with_customer_subscription

        @pytest.mark.asyncio
        async def test_get_order__not_found(self, order, action):
            with pytest.raises(CustomerSubscriptionNotFoundError):
                await action._get_order()
