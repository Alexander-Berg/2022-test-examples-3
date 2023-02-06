from random import choice

import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.utils.helpers import without_none

from ..base import BaseCustomerSubscriptionCancelHandlerPostTest, BaseCustomerSubscriptionHandlerPostTest
from .base import BaseInternalHandlerTvmTest


class TestInternalCustomerSubscriptionHandlerGet(BaseInternalHandlerTvmTest):
    @pytest.fixture(autouse=True)
    def action(self, mock_action, customer_subscription, order):
        from mail.payments.payments.core.actions.customer_subscription.get_list import (
            GetCustomerSubscriptionListServiceMerchantAction
        )
        return mock_action(GetCustomerSubscriptionListServiceMerchantAction, [[customer_subscription, order]])

    @pytest.fixture
    def offset(self):
        return None

    @pytest.fixture
    def limit(self):
        return None

    @pytest.fixture
    def params(self, limit, offset):
        return without_none({'limit': limit, 'offset': offset})

    @pytest.fixture
    async def response(self, payments_client, service_merchant, params):
        return await payments_client.get(
            f'/v1/internal/customer_subscription/{service_merchant.service_merchant_id}',
            params=params
        )

    class TestSuccessRequest:
        def test_200(self, response):
            assert response.status == 200

        @pytest.mark.parametrize('limit', (1, 10, None))
        @pytest.mark.parametrize('offset', (0, 10, None))
        def test_params(self, service_merchant, service_client, limit, offset, response, action):
            assert_that(
                action.call_args[1],
                has_entries({
                    'service_merchant_id': service_merchant.service_merchant_id,
                    'service_tvm_id': service_client.tvm_id,
                    'offset': 0 if offset is None else offset,
                    'limit': 100 if limit is None else limit
                })
            )


class TestInternalCustomerSubscriptionHandlerPost(BaseCustomerSubscriptionHandlerPostTest,
                                                  BaseInternalHandlerTvmTest):
    @pytest.fixture
    def action_cls(self):
        from mail.payments.payments.core.actions.customer_subscription.create import (
            CreateCustomerSubscriptionServiceMerchantAction
        )
        return CreateCustomerSubscriptionServiceMerchantAction

    @pytest.fixture
    def request_path(self, service_merchant):
        return f'/v1/internal/customer_subscription/{service_merchant.service_merchant_id}'

    @pytest.fixture
    def extra_call_args(self, service_merchant, service_client):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': service_client.tvm_id,
        }


class TestInternalCustomerSubscriptionIdHandlerGet(BaseInternalHandlerTvmTest):
    @pytest.fixture
    def action(self, mock_action):
        from mail.payments.payments.core.actions.customer_subscription.get import (
            GetCustomerSubscriptionServiceMerchantAction
        )
        return mock_action(GetCustomerSubscriptionServiceMerchantAction, (None, None))

    @pytest.fixture
    async def response(self, action, payments_client, customer_subscription, service_merchant):
        service_merchant_id = service_merchant.service_merchant_id
        customer_subscription_id = customer_subscription.customer_subscription_id

        return await payments_client.get(
            f'/v1/internal/customer_subscription/{service_merchant_id}/{customer_subscription_id}',
        )

    class TestSuccessRequest:
        @pytest.mark.asyncio
        async def test_200(self, response):
            assert response.status == 200

        def test_params(self, service_merchant, service_client, response, customer_subscription, action):
            assert_that(
                action.call_args[1],
                has_entries({
                    'service_merchant_id': service_merchant.service_merchant_id,
                    'service_tvm_id': service_client.tvm_id,
                    'customer_subscription_id': customer_subscription.customer_subscription_id,
                })
            )


class TestInternalStartCustomerSubscriptionHandlerPost(BaseInternalHandlerTvmTest):
    @pytest.fixture
    def action(self, mock_action):
        from mail.payments.payments.core.actions.customer_subscription.pay import (
            StartCustomerSubscriptionServiceMerchantAction
        )
        return mock_action(StartCustomerSubscriptionServiceMerchantAction)

    @pytest.fixture
    def request_json(self, randmail, rands, randn):
        return {
            'yandexuid': rands(),
            'customer_uid': randn(),
            'email': randmail(),
            'return_url': rands(),
            'description': rands(),
            'template': choice(('mobile', 'desktop')),
        }

    @pytest.fixture
    async def response(self, action, payments_client, customer_subscription, service_merchant, request_json):
        service_merchant_id = service_merchant.service_merchant_id
        customer_subscription_id = customer_subscription.customer_subscription_id

        return await payments_client.post(
            f'/v1/internal/customer_subscription/{service_merchant_id}/{customer_subscription_id}/start',
            json=request_json
        )

    class TestSuccessRequest:
        def test_200(self, response):
            assert response.status == 200

        def test_params(self,
                        request_json,
                        customer_subscription,
                        service_merchant,
                        service_client,
                        response,
                        action,
                        ):
            assert_that(
                action.call_args[1],
                has_entries({
                    'customer_subscription_id': customer_subscription.customer_subscription_id,
                    'yandexuid': request_json['yandexuid'],
                    'customer_uid': request_json['customer_uid'],
                    'template': request_json['template'],
                    'return_url': request_json['return_url'],
                    'email': request_json['email'],
                    'description': request_json['description'],
                    'service_merchant_id': service_merchant.service_merchant_id,
                    'service_tvm_id': service_client.tvm_id,
                })
            )


class TestInternalCustomerSubscriptionCancelHandlerPost(BaseCustomerSubscriptionCancelHandlerPostTest,
                                                        BaseInternalHandlerTvmTest):
    @pytest.fixture
    def action_cls(self):
        from mail.payments.payments.core.actions.customer_subscription.cancel import (
            CancelCustomerSubscriptionServiceMerchantAction
        )
        return CancelCustomerSubscriptionServiceMerchantAction

    @pytest.fixture
    def request_path(self, service_merchant, customer_subscription):
        service_merchant_id = service_merchant.service_merchant_id
        customer_subscription_id = customer_subscription.customer_subscription_id

        return f'/v1/internal/customer_subscription/{service_merchant_id}/{customer_subscription_id}/cancel'

    @pytest.fixture
    def extra_call_args(self, service_merchant, service_client):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': service_client.tvm_id,
        }
