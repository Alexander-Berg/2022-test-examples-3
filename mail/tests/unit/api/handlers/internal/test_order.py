from decimal import Decimal

import pytest

from sendr_utils import enum_value

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.enums import NDS, OrderKind, ShopType
from mail.payments.payments.core.exceptions import TrustError
from mail.payments.payments.utils.helpers import without_none

from ..test_order import TestRefundHandler as BaseTestRefundHandler
from ..test_order import TestScheduleOrderClearUnholdHandler as BaseTestOrderClearUnholdHandler
from .base import BaseInternalHandlerTvmTest


class TestCreateInternalOrderWithTrustParams(BaseInternalHandlerTvmTest):
    @pytest.fixture
    def action(self, mock_action, order, items):
        from mail.payments.payments.core.actions.order.create_or_update import CreateOrUpdateOrderServiceMerchantAction
        return mock_action(CreateOrUpdateOrderServiceMerchantAction, order)

    @pytest.fixture(params=(None, OrderKind.MULTI, OrderKind.PAY))
    def kind(self, request):
        return request.param

    @pytest.fixture
    def fast_moderation_param(self, randbool):
        return randbool()

    @pytest.fixture
    def request_json(self, rands, kind, randitem, test_param, fast_moderation_param):
        return without_none({
            'kind': enum_value(kind),
            'autoclear': False,
            'uid': -1,
            'caption': ' abc ',
            'description': 'def',
            'user_email': ' test_user_email ',
            'customer_uid': 23456,
            'user_description': 'test_user_description',
            'return_url': 'test_return_url',
            'paymethod_id': '1234',
            'items': [
                {
                    'amount': 22.33,
                    'currency': 'RUB',
                    'name': ' roses ',
                    'nds': 'nds_none',
                    'price': 99.99,
                },
                {
                    'amount': 100,
                    'currency': 'RUB',
                    'name': ' smth ',
                    'nds': 'nds_10',
                    'price': 1000,
                }
            ],
            'mode': enum_value(randitem(ShopType)),
            'test': enum_value(test_param),
            'service_data': {rands(): rands()},
            'fast_moderation': fast_moderation_param,
            'commission': 100,
        })

    @pytest.fixture
    def request_url(self, service_merchant):
        return f'/v1/internal/order/{service_merchant.service_merchant_id}/'

    @pytest.fixture
    async def response(self, action, payments_client, request_url, request_json):
        return await payments_client.post(request_url, json=request_json, allow_redirects=False)

    def test_context(self, response, kind, action, request_json, service_merchant, tvm_client_id, test_param,
                     fast_moderation_param):
        exp_context = {
            'autoclear': request_json['autoclear'],
            'caption': request_json['caption'].strip(),
            'description': request_json['description'],
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': tvm_client_id,
            'user_email': request_json['user_email'].strip(),
            'customer_uid': request_json['customer_uid'],
            'user_description': request_json['user_description'],
            'return_url': request_json['return_url'],
            'paymethod_id': request_json['paymethod_id'],
            'items': [
                {
                    'amount': Decimal(str(item['amount'])),
                    'price': Decimal(str(item['price'])),
                    'currency': item['currency'],
                    'nds': NDS(item['nds']),
                    'name': item['name'].strip(),
                }
                for item in request_json['items']
            ],
            'test': test_param,
            'kind': kind if kind else OrderKind.PAY,
            'default_shop_type': ShopType(request_json['mode']),
            'service_data': request_json['service_data'],
            'fast_moderation': fast_moderation_param,
            'commission': 100,
        }

        if kind == OrderKind.MULTI:
            exp_context['max_amount'] = None

        action.assert_called_once_with(**exp_context)

    class TestUrlMatch:
        """/ в конце url не влияет на матчинг"""

        @pytest.fixture
        def kind(self):
            return None

        @pytest.fixture(params=['', '/'])
        def request_url(self, request, service_merchant):
            return f'/v1/internal/order/{service_merchant.service_merchant_id}{request.param}'

        @pytest.mark.asyncio
        async def test_url_match__called(self, response, action):
            action.assert_called_once()


class TestInternalRefundHandler(BaseTestRefundHandler):
    @pytest.fixture
    def action(self, mock_action):
        from mail.payments.payments.core.actions.order.refund import CreateServiceMerchantRefundAction
        return mock_action(CreateServiceMerchantRefundAction)

    @pytest.fixture(autouse=True)
    def setup(self, crypto_mock, service_client, service_merchant, expected_context):
        expected_context.pop('uid')
        expected_context.update({
            'service_tvm_id': service_client.tvm_id,
            'service_merchant_id': service_merchant.service_merchant_id,
        })

    @pytest.fixture
    async def response(self, payments_client, service_merchant, refund, request_json):
        return await payments_client.post(
            f'/v1/internal/order/{service_merchant.service_merchant_id}/{refund.original_order_id}/refund',
            json=request_json,
        )


class TestInternalOrderClearUnholdHandler(BaseTestOrderClearUnholdHandler):
    @pytest.fixture
    def action(self, mock_action):
        from mail.payments.payments.core.actions.order.clear_unhold import ScheduleClearUnholdServiceMerchantOrderAction
        return mock_action(ScheduleClearUnholdServiceMerchantOrderAction)

    @pytest.fixture
    async def response(self, action, payments_client, service_merchant, order, operation):
        return await payments_client.post(
            f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/{operation}')

    @pytest.fixture
    def expected_context(self, service_merchant, tvm_client_id, order, operation, crypto_mock):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': tvm_client_id,
            'order_id': order.order_id,
            'operation': operation,
        }


class TestClearUnholdWithResizeParams(BaseTestOrderClearUnholdHandler):
    @pytest.fixture
    def action(self, mock_action):
        from mail.payments.payments.core.actions.order.clear_unhold import ScheduleClearUnholdServiceMerchantOrderAction
        return mock_action(ScheduleClearUnholdServiceMerchantOrderAction)

    @pytest.fixture
    def request_json(self, items):
        return {
            'items': [
                {
                    'product_id': item.product_id,
                    'amount': str(round(item.amount, 2)),  # type: ignore
                    'price': str(round(item.price, 2)),  # type: ignore
                }
                for item in items
            ]
        }

    @pytest.fixture
    async def response(self, action, payments_client, service_merchant, order, operation, request_json):
        return await payments_client.post(
            f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/{operation}',
            json=request_json
        )

    @pytest.fixture
    def expected_context(self, service_merchant, tvm_client_id, order, operation, items, crypto_mock):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': tvm_client_id,
            'order_id': order.order_id,
            'operation': operation,
            'items': [
                {
                    'product_id': item.product_id,
                    'amount': item.amount,
                    'price': item.price
                }
                for item in items
            ],
        }


class TestInternalUpdateServiceDataOrderHandler:
    @pytest.fixture(autouse=True)
    def action(self, mock_action, order):
        from mail.payments.payments.core.actions.order.update_service_data import UpdateServiceDataServiceMerchantAction
        return mock_action(UpdateServiceDataServiceMerchantAction, order)

    @pytest.fixture
    def service_merchant_id(self, randn):
        return randn()

    @pytest.fixture
    def service_data(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    async def response(self, payments_client, service_merchant_id, order, service_data):
        return await payments_client.post(
            f'/v1/internal/order/{service_merchant_id}/{order.order_id}/service_data',
            json={'service_data': service_data},
        )

    def test_success(self, response):
        assert response.status == 200

    def test_context(self, tvm_client_id, order, action, service_merchant_id, service_data, response):
        action.assert_called_once_with(
            service_merchant_id=service_merchant_id,
            service_tvm_id=tvm_client_id,
            order_id=order.order_id,
            service_data=service_data,
        )


class TestInternalReceiptCloseHandler:
    @pytest.fixture
    def result(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def service_merchant_id(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    def action(self, mock_action, result):
        from mail.payments.payments.core.actions.order.receipt_close import OrderReceiptCloseServiceMerchantAction
        return mock_action(OrderReceiptCloseServiceMerchantAction, result)

    @pytest.fixture
    def items(self, randn, randitem):
        return [
            {'product_id': randn(), 'nds': randitem(NDS).value}
            for _ in range(randn(min=1, max=10))
        ]

    @pytest.fixture
    async def response(self, payments_client, service_merchant_id, order, items):
        return await payments_client.post(
            f'/v1/internal/order/{service_merchant_id}/{order.order_id}/receipt/close',
            json={'items': items},
        )

    def test_context(self, tvm_client_id, order, action, service_merchant_id, items, response):
        action.assert_called_once_with(
            service_merchant_id=service_merchant_id,
            service_tvm_id=tvm_client_id,
            order_id=order.order_id,
            items=[{**item, 'nds': NDS(item['nds'])} for item in items],
        )

    @pytest.mark.parametrize('items', [None])
    def test_items_none(self, tvm_client_id, order, action, service_merchant_id, items, response):
        action.assert_called_once_with(
            service_merchant_id=service_merchant_id,
            service_tvm_id=tvm_client_id,
            order_id=order.order_id,
            items=None,
        )

    @pytest.mark.parametrize('result', [TrustError(params={'a': 'b'})])
    @pytest.mark.asyncio
    async def test_error(self, response):
        assert_that(await response.json(), has_entries({
            'data': {
                'message': 'TINKOFF_ERROR',
                'params': {'a': 'b'}
            }
        }))


class TestInternalOrderCancelHandler:
    @pytest.fixture
    def service_merchant_id(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    def action(self, mock_action, order):
        from mail.payments.payments.core.actions.order.cancel import CancelOrderServiceMerchantAction
        return mock_action(CancelOrderServiceMerchantAction, order)

    @pytest.fixture
    async def response(self, payments_client, service_merchant_id, order, items):
        return await payments_client.post(
            f'/v1/internal/order/{service_merchant_id}/{order.order_id}/cancel',
        )

    def test_success(self, response):
        assert response.status == 200

    def test_context(self, tvm_client_id, order, action, service_merchant_id, items, response):
        action.assert_called_once_with(
            service_merchant_id=service_merchant_id,
            service_tvm_id=tvm_client_id,
            order_id=order.order_id
        )


class TestInternalOrderPayoutInfoHandler:
    @pytest.fixture
    def service_merchant_id(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    def action(self, mock_action, order, payouts_data):
        from mail.payments.payments.core.actions.order.get import GetInternalOrderPayoutInfoAction
        return mock_action(GetInternalOrderPayoutInfoAction, payouts_data['payouts'])

    @pytest.fixture
    async def response(self, payments_client, service_merchant_id, order):
        return await payments_client.get(
            f'/v1/internal/order/{service_merchant_id}/{order.order_id}/payout_info',
        )

    def test_success(self, response):
        assert response.status == 200

    @pytest.mark.asyncio
    async def test_response(self, response, payouts_data):
        assert_that((await response.json()), has_entries({'data': payouts_data['payouts']}))

    def test_context(self, tvm_client_id, order, action, service_merchant_id, response):
        action.assert_called_once_with(
            service_merchant_id=service_merchant_id,
            service_tvm_id=tvm_client_id,
            order_id=order.order_id
        )
