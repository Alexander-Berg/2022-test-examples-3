from datetime import datetime
from decimal import Decimal
from itertools import chain

import pytest
from multidict import MultiDict

from sendr_utils import enum_value, json_value, utcnow

from hamcrest import all_of, assert_that, contains_inanyorder, equal_to, has_entries, has_entry, has_key, not_

from mail.payments.payments.api.schemas.base import CURRENCY_RUB
from mail.payments.payments.core.entities.enums import (
    NDS, PAY_METHOD_OFFLINE, PAY_METHODS, PAYMETHOD_ID_OFFLINE, OrderKind, OrderSource, PayStatus, RefundStatus,
    ShopType
)
from mail.payments.payments.tests.base import BaseTestOrder, BaseTestOrderList
from mail.payments.payments.tests.utils import strip_fields
from mail.payments.payments.utils.helpers import without_none


class TestOrderGet(BaseTestOrder):
    @pytest.fixture
    def with_refunds(self, randbool):
        return randbool()

    @pytest.fixture
    def with_timeline(self, randbool):
        return randbool()

    @pytest.fixture
    def params(self, with_refunds, with_timeline):
        return {
            'with_refunds': str(with_refunds).lower(),
            'with_timeline': str(with_timeline).lower(),
        }

    @pytest.fixture
    def action(self, internal_api, mock_action, order, items):
        if internal_api:
            from mail.payments.payments.core.actions.order.get import GetOrderServiceMerchantAction
            return mock_action(GetOrderServiceMerchantAction, order)
        else:
            from mail.payments.payments.core.actions.order.get import GetOrderAction
            return mock_action(GetOrderAction, order)

    @pytest.fixture
    async def response(self, action, params, payments_client, order, test_data):
        return await payments_client.get(test_data['path'], params=params)

    def test_context(self, order, with_refunds, with_timeline, internal_api, crypto_mock, response, action, test_data):
        test_data['context'].update({
            'with_customer_subscription': True,
            'select_customer_subscription': None,
        })
        if internal_api:
            test_data['context'].update({'with_refunds': with_refunds})
        else:
            test_data['context'].update({'with_timeline': with_timeline})
        action.assert_called_once_with(**test_data['context'])


class TestOrderPut(BaseTestOrder):
    @pytest.fixture(params=('active', 'order'))
    def mode(self, request):
        return request.param

    @pytest.fixture(params=(True, False))
    def active(self, request):
        return request.param

    @pytest.fixture(params=(True, False))
    def autoclear(self, request):
        return request.param

    @pytest.fixture(params=(True, False))
    def fast_moderation(self, request):
        return request.param

    @pytest.fixture
    def order_param_pop(self):
        return None

    @pytest.fixture
    def item_amount(self, randdecimal):
        return randdecimal()

    @pytest.fixture
    def item_price(self, randdecimal):
        return randdecimal()

    @pytest.fixture
    def order_properties(self, rands, autoclear, item_amount, item_price, randitem, fast_moderation, order_param_pop):
        properties = {
            'caption': f' {rands()} ',
            'description': rands(),
            'items': [{
                'amount': item_amount,
                'currency': CURRENCY_RUB,
                'image': {
                    'url': rands(),
                },
                'name': f' {rands()} ',
                'nds': randitem(NDS),
                'price': item_price
            }],
            'autoclear': autoclear,
            'offline_abandon_deadline': utcnow(),
            'fast_moderation': fast_moderation,
        }
        if order_param_pop is not None:
            properties.pop(order_param_pop)
        return properties

    @pytest.fixture
    def service_merchant_context(self, mode, active, service_merchant, order, crypto_mock, tvm_client_id,
                                 order_properties):
        if mode == 'active':
            return {
                'service_merchant_id': service_merchant.service_merchant_id,
                'service_tvm_id': tvm_client_id,
                'order_id': order.order_id,
                'active': active,
            }
        elif mode == 'order':
            return {
                'service_merchant_id': service_merchant.service_merchant_id,
                'service_tvm_id': tvm_client_id,
                'order_id': order.order_id,
                **strip_fields(order_properties)
            }

    @pytest.fixture
    def uid_context(self, active, mode, order, order_properties):
        if mode == 'active':
            return {
                'uid': order.uid,
                'order_id': order.order_id,
                'active': active,
            }
        elif mode == 'order':
            return {
                'uid': order.uid,
                'order_id': order.order_id,
                **order_properties
            }

    @pytest.fixture
    def action(self, mode, internal_api, mock_action, order):
        if mode == 'active':
            if internal_api:
                from mail.payments.payments.core.actions.order.activate import ActivateOrderServiceMerchantAction
                return mock_action(ActivateOrderServiceMerchantAction, order)
            else:
                from mail.payments.payments.core.actions.order.activate import ActivateOrderAction
                return mock_action(ActivateOrderAction, order)
        elif mode == 'order':
            if internal_api:
                from mail.payments.payments.core.actions.order.create_or_update import (
                    CreateOrUpdateOrderServiceMerchantAction
                )
                return mock_action(CreateOrUpdateOrderServiceMerchantAction, order)
            else:
                from mail.payments.payments.core.actions.order.create_or_update import CreateOrUpdateOrderAction
                return mock_action(CreateOrUpdateOrderAction, order)

    @pytest.fixture
    def request_json(self, mode, randitem, randdecimal, order_properties, rands, active):
        if mode == 'active':
            return {
                'active': active,
                rands(): rands(),
                'uid': -1,
            }
        elif mode == 'order':
            return json_value(order_properties)

    @pytest.fixture
    def response_func(self, action, payments_client, test_data, request_json):
        async def _inner(**kwargs):
            request_json.update(kwargs)
            return await payments_client.put(test_data['path'], json=request_json)

        return _inner

    def test_params(self, test_data, action, response):
        action.assert_called_once_with(**strip_fields(test_data['context']))

    @pytest.mark.asyncio
    @pytest.mark.parametrize('internal_api,mode', (
        pytest.param(True, 'order', id='internal order'),
    ))
    async def test_service_data(self, rands, test_data, action, response_func):
        service_data = {rands(): rands()}
        await response_func(service_data=service_data)
        action.assert_called_once_with(service_data=service_data, **test_data['context'])

    @pytest.mark.parametrize('order_properties', (
        pytest.param({
            'caption': 'caption',
            'description': 'description',
            'items': [{
                'amount': Decimal('1.0'),
                'currency': CURRENCY_RUB,
                'image': {},
                'name': 'name',
                'nds': NDS.NDS_10,
                'price': Decimal('1.0'),
            }],
            'autoclear': False,
            'offline_abandon_deadline': utcnow(),
        }, id='no-image-url'),
    ))
    @pytest.mark.parametrize('mode', ('order',))
    @pytest.mark.asyncio
    async def test_invalid_order(self, response_func):
        response = await response_func()
        assert response.status == 400

    @pytest.mark.parametrize('item_amount,item_price', (
        pytest.param(Decimal(1), Decimal(0), id='zero-price'),
        pytest.param(Decimal(0), Decimal(1), id='zero-amount'),
    ))
    @pytest.mark.parametrize('mode', ('order',))
    @pytest.mark.asyncio
    async def test_invalid_item(self, response_func):
        response = await response_func()
        assert response.status == 400

    @pytest.mark.parametrize('order_param_pop', [None, 'caption'])
    @pytest.mark.parametrize('mode', ('order',))
    @pytest.mark.asyncio
    async def test_absent_order_caption(self, response_func):
        response = await response_func()
        assert response.status == 200


class TestOrderActive(BaseTestOrder):
    @pytest.fixture(params=(True, False))
    def active(self, request):
        return request.param

    @pytest.fixture
    def service_merchant_path(self, service_merchant, order, active):
        suffix = 'activate' if active else 'deactivate'
        return f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/{suffix}'

    @pytest.fixture
    def uid_path(self, order, active):
        suffix = 'activate' if active else 'deactivate'
        return f'/v1/order/{order.uid}/{order.order_id}/{suffix}'

    @pytest.fixture
    def service_merchant_context(self, active, service_merchant, order, crypto_mock, tvm_client_id):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': tvm_client_id,
            'order_id': order.order_id,
            'active': active,
        }

    @pytest.fixture
    def uid_context(self, active, order):
        return {
            'uid': order.uid,
            'order_id': order.order_id,
            'active': active,
            'with_customer_subscription': True,
            'select_customer_subscription': None,
        }

    @pytest.fixture
    def action(self, internal_api, mock_action, order):
        if internal_api:
            from mail.payments.payments.core.actions.order.activate import ActivateOrderServiceMerchantAction
            return mock_action(ActivateOrderServiceMerchantAction, order)
        else:
            from mail.payments.payments.core.actions.order.activate import ActivateOrderAction
            return mock_action(ActivateOrderAction, order)

    @pytest.fixture
    async def response(self, action, payments_client, test_data):
        return await payments_client.post(test_data['path'])

    def test_params(self, order, response, action, test_data):
        action.assert_called_once_with(**test_data['context'])


class TestMultiOrderPost(BaseTestOrder):
    @pytest.fixture
    def action(self, internal_api, mock_action, order, items):
        if internal_api:
            from mail.payments.payments.core.actions.order.create_from_multi import (
                CreateOrderFromMultiOrderServiceMerchantAction
            )
            return mock_action(CreateOrderFromMultiOrderServiceMerchantAction, order)
        else:
            from mail.payments.payments.core.actions.order.create_from_multi import CreateOrderFromMultiOrderAction
            return mock_action(CreateOrderFromMultiOrderAction, order)

    @pytest.fixture
    def service_merchant_path(self, service_merchant, multi_order):
        return f'/v1/internal/order/{service_merchant.service_merchant_id}/multi/{multi_order.order_id}'

    @pytest.fixture
    def service_merchant_context(self, service_merchant, service_client, multi_order):
        return {'order_id': multi_order.order_id,
                'service_merchant_id': service_merchant.service_merchant_id,
                'service_tvm_id': service_client.tvm_id}

    @pytest.fixture
    def uid_path(self, multi_order):
        return f'/v1/order/{multi_order.uid}/multi/{multi_order.order_id}'

    @pytest.fixture
    def uid_context(self, multi_order):
        return {'order_id': multi_order.order_id, 'uid': multi_order.uid}

    @pytest.fixture
    async def response(self, action, payments_client, order, test_data):
        return await payments_client.post(test_data['path'])

    def test_params(self, order, response, action, test_data):
        action.assert_called_once_with(**test_data['context'])


class TestOrderListGet(BaseTestOrderList):
    @pytest.fixture
    def params(self, randn):
        return {
            'original_order_id': randn(),
            'subscription': 'true',
        }

    @pytest.fixture(autouse=True)
    def action(self, internal_api, mock_action, order, items, shop):
        order.order_hash = 'test-order-list-get-order-hash'
        order.payment_hash = 'test-order-list-get-payment-hash'
        order.shop = shop
        if internal_api:
            from mail.payments.payments.core.actions.order.get_list import GetServiceMerchantOrderListAction
            return mock_action(GetServiceMerchantOrderListAction, [order])
        else:
            from mail.payments.payments.core.actions.order.get_list import GetOrderListAction
            return mock_action(GetOrderListAction, [order])

    @pytest.fixture
    async def response(self, payments_client, params, test_data, tvm):
        resp = await payments_client.get(test_data['path'], params=params)
        return resp

    def test_params(self, response, action, params, test_data):
        test_data['context'].update({
            'limit': 100,
            'offset': 0,
            'original_order_id': params['original_order_id'],
            'subscription': True,
        })
        action.assert_called_once_with(**test_data['context'])

    class TestParentOrderId:
        @pytest.fixture
        def params(self, randn):
            return {'parent_order_id': randn()}

        def test_parent_order_id(self, params, response, action):
            assert_that(
                action.call_args[1]['parent_order_id'],
                params['parent_order_id'],
            )

    class TestPayMethod:
        @pytest.fixture
        def params(self, randitem):
            return {'pay_method': randitem(PAY_METHODS)}

        def test_pay_method(self, params, response, action):
            assert_that(
                action.call_args[1]['pay_method'],
                params['pay_method'],
            )

    class TestPayStatuses:
        @pytest.fixture
        def params(self):
            p = MultiDict()
            p.add('pay_statuses[]', PayStatus.PAID.value)
            p.add('pay_statuses[]', PayStatus.IN_PROGRESS.value)
            return p

        def test_pay_statuses(self, response, action):
            assert_that(
                action.call_args[1]['pay_statuses'],
                contains_inanyorder(PayStatus.PAID, PayStatus.IN_PROGRESS),
            )

    class TestRefundStatuses:
        @pytest.fixture
        def params(self):
            p = MultiDict()
            p.add('refund_statuses[]', RefundStatus.REQUESTED.value)
            p.add('refund_statuses[]', RefundStatus.COMPLETED.value)
            return p

        def test_refund_statuses(self, response, action):
            assert_that(
                action.call_args[1]['refund_statuses'],
                contains_inanyorder(RefundStatus.REQUESTED, RefundStatus.COMPLETED),
            )

    class TestDatetimeParams:
        @pytest.fixture
        def params(self):
            return {
                'created_from': datetime.utcnow().isoformat(),
                'created_to': datetime.utcnow().isoformat(),
                'held_at_from': datetime.utcnow().isoformat(),
                'held_at_to': datetime.utcnow().isoformat(),
            }

        def test_datetime_params(self, response, action, params, test_data):
            assert_that(action.call_args[1], has_entries({
                'created_from': datetime.fromisoformat(params['created_from']),
                'created_to': datetime.fromisoformat(params['created_to']),
                'held_at_from': datetime.fromisoformat(params['held_at_from']),
                'held_at_to': datetime.fromisoformat(params['held_at_to']),
            }))

    class TestPriceParams:
        @pytest.fixture
        def params(self):
            return {
                'price_from': 100,
                'price_to': 200,
            }

        def test_price_params(self, response, action, params, test_data):
            assert_that(action.call_args[1], has_entries({
                'price_from': params['price_from'],
                'price_to': params['price_to'],
            }))

    class TestTextEmailQueryParams:
        @pytest.fixture
        def params(self):
            return {
                'text_query': 'asd',
                'email_query': 'asd',
            }

        def test_text_email_query_params(self, response, action, params, test_data):
            assert_that(action.call_args[1], has_entries({
                'text_query': params['text_query'],
                'email_query': params['email_query'],
            }))

    class TestCreatedBySourcesQueryParams:
        @pytest.fixture
        def params(self):
            p = MultiDict()
            p.add('created_by_sources[]', OrderSource.UI.value)
            p.add('created_by_sources[]', OrderSource.SERVICE.value)
            return p

        def test_created_by_sources_query_params(self, response, action):
            assert_that(
                action.call_args[1]['created_by_sources'],
                contains_inanyorder(OrderSource.UI, OrderSource.SERVICE)
            )

    class TestServiceIdsQueryParams:
        @pytest.fixture
        def params(self):
            p = MultiDict()
            p.add('service_ids[]', 1)
            p.add('service_ids[]', 2)
            return p

        def test_service_ids_query_params(self, response, action):
            assert_that(action.call_args[1]['service_ids'], contains_inanyorder(1, 2))

    class TestSubscriptionQueryParam:
        @pytest.fixture(params=('false', 'true', 'null'))
        def subscription_param(self, request):
            return request.param

        @pytest.fixture
        def params(self, subscription_param):
            return {
                'subscription': subscription_param
            }

        def test_subscription_query_param(self, response, action, subscription_param):
            expected = True if subscription_param == 'true' else False if subscription_param == 'false' else None
            assert action.call_args[1]['subscription'] == expected

        class TestDefault:
            @pytest.fixture
            def params(self):
                return {}

            def test_subscription_query_param_default(self, response, params, action):
                assert action.call_args[1]['subscription'] is False

    class TestWithRefundsParams:
        @pytest.fixture
        def params(self):
            p = MultiDict()
            p.add('with_refunds', 1)
            return p

        def test_with_refunds_params(self, response, action):
            assert_that(
                action.call_args[1]['with_refunds'],
                equal_to(True)
            )


class TestOrderListPost(BaseTestOrderList):
    @pytest.fixture
    def action(self, internal_api, mock_action, order, shop):
        order.shop = shop
        if internal_api:
            from mail.payments.payments.core.actions.order.create_or_update import (
                CreateOrUpdateOrderServiceMerchantAction
            )
            return mock_action(CreateOrUpdateOrderServiceMerchantAction, order)
        else:
            from mail.payments.payments.core.actions.order.create_or_update import CreateOrUpdateOrderAction
            return mock_action(CreateOrUpdateOrderAction, order)

    @pytest.fixture
    def kind(self):
        return None

    @pytest.fixture
    def max_amount(self):
        return None

    @pytest.fixture
    def pay_method(self):
        return None

    @pytest.fixture
    def mode(self):
        return 'prod'

    @pytest.fixture
    def request_json(self, kind, pay_method, max_amount, mode):
        return without_none({
            'offline_abandon_deadline': utcnow().isoformat(),
            'kind': enum_value(kind),
            'autoclear': False,
            'mode': mode,
            'uid': -1,
            'caption': ' abc ',
            'description': 'def',
            'pay_method': pay_method,
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
            'max_amount': max_amount,
            'fast_moderation': True,
        })

    @pytest.fixture
    def request_headers(self):
        return {}

    @pytest.fixture
    def response_func(self, action, payments_client, test_data):
        async def _inner(request_json, headers=None):
            return await payments_client.post(test_data['path'], json=request_json, headers=headers or {})

        return _inner

    @pytest.fixture
    async def response(self, request_json, request_headers, response_func):
        return await response_func(request_json, headers=request_headers)

    class TestShopIdHeader:
        @pytest.mark.asyncio
        async def test_explicit_shop_id_header(self, action, response_func, request_json, request_headers, randn):
            shop_id = str(randn())
            request_headers['X-Shop-Id'] = shop_id
            await response_func(request_json, headers=request_headers)
            assert action.call_args[1]['shop_id'] == int(shop_id)

        @pytest.mark.asyncio
        async def test_no_shop_id_header_and_mode_parameter(
            self, action, mode, response_func, request_json, request_headers
        ):
            """mode json param means default shop type if no x-shop-id header is passed"""
            request_headers.pop('X-Shop-Id', None)
            await response_func(request_json, headers=request_headers)
            expected_shop_type = dict(prod=ShopType.PROD, test=ShopType.TEST)[mode]
            call_kwargs = action.call_args[1]

            assert_that(call_kwargs, all_of(
                has_entry('default_shop_type', expected_shop_type),
                not_(has_key('shop_id')),
            ))

        @pytest.mark.asyncio
        async def test_response_400_because_shop_id_is_not_integer(self, response_func, request_json):
            headers = {'X-Shop-Id': 'not valid shop id'}
            response = await response_func(request_json, headers)
            assert_that(
                await response.json(),
                has_entries({
                    'data': {
                        'params': {'x-shop-id': ['Not a valid integer.']},
                        'message': 'Bad Request'},
                    'status': 'fail',
                    'code': 400
                })
            )

    @pytest.mark.parametrize('pop_key', ['items'])
    @pytest.mark.asyncio
    async def test_bad_request(self, request_json, response_func, pop_key):
        request_json.pop(pop_key)
        response = await response_func(request_json)
        assert_that(
            await response.json(),
            has_entries({
                'status': 'fail',
                'code': 400,
            })
        )

    @pytest.mark.parametrize('caption', ['abc', '', None])
    @pytest.mark.asyncio
    async def test_order_caption(self, request_json, response_func, caption):
        request_json['caption'] = caption
        response = await response_func(request_json)
        assert_that(
            await response.json(),
            has_entries({
                'status': 'success',
                'code': 200,
            })
        )

    @pytest.mark.asyncio
    async def test_order_caption_absent(self, request_json, response_func):
        request_json.pop('caption')
        response = await response_func(request_json)
        assert_that(
            await response.json(),
            has_entries({
                'status': 'success',
                'code': 200,
            })
        )

    @pytest.mark.asyncio
    async def test_product_name_too_long(self, request_json, response_func):
        request_json['items'][0]['name'] = 129 * 'a'
        response = await response_func(request_json)
        response_json = await response.json()
        assert_that(
            response_json,
            has_entries({
                'code': 400,
                'status': 'fail',
                'data': {
                    'params': {
                        'items': {
                            '0': {
                                'name': ['Longer than maximum length 128.']
                            }
                        }
                    },
                    'message': 'Bad Request',
                }
            })
        )

    @pytest.mark.parametrize(
        'kind',
        [
            pytest.param(
                kind, marks=() if kind in (None, OrderKind.PAY, OrderKind.MULTI) else (pytest.mark.xfail(strict=True),)
            )
            for kind in chain((None,), list(OrderKind))
        ]
    )
    @pytest.mark.asyncio
    async def test_kind(self, request_json, response_func):
        response = await response_func(request_json)
        assert_that(
            await response.json(),
            has_entries({
                'status': 'success',
                'code': 200,
            })
        )

    @pytest.mark.parametrize('pop_key', ['amount', 'currency', 'name', 'nds', 'price'])
    @pytest.mark.asyncio
    async def test_bad_request_items(self, request_json, response_func, pop_key):
        request_json['items'][0].pop(pop_key)
        response = await response_func(request_json)
        assert_that(
            await response.json(),
            has_entries({
                'status': 'fail',
                'code': 400,
            })
        )

    @pytest.mark.parametrize('kind', (OrderKind.MULTI, OrderKind.PAY))
    @pytest.mark.parametrize('pay_method', (None, PAY_METHOD_OFFLINE))
    def test_context(self, action, request_json, pay_method, response, test_data, kind):
        exp_context = {
            'autoclear': request_json['autoclear'],
            'caption': request_json['caption'].strip(),
            'description': request_json['description'],
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
            'offline_abandon_deadline': datetime.fromisoformat(request_json['offline_abandon_deadline']),
            'kind': kind,
            'fast_moderation': request_json['fast_moderation'],
        }
        if kind == OrderKind.MULTI:
            exp_context['max_amount'] = None
        if pay_method == PAY_METHOD_OFFLINE:
            exp_context['paymethod_id'] = PAYMETHOD_ID_OFFLINE

        if 'mode' in request_json:
            mode = request_json['mode']
            exp_context['default_shop_type'] = dict(prod=ShopType.PROD, test=ShopType.TEST)[mode]

        exp_context.update(test_data['context'])
        action.assert_called_once_with(**exp_context)

    @pytest.mark.asyncio
    async def test_paymethod_id_pay_method_both(self, request_json, rands, response_func):
        response = await response_func({**request_json, 'pay_method': 'offline', 'paymethod_id': rands()})
        assert_that(
            await response.json(),
            has_entries({
                'status': 'fail',
                'code': 400,
            })
        )

    @pytest.mark.asyncio
    async def test_paymethod_id(self, internal_api, request_json, rands, response_func):
        paymethod_id = 'offline' if internal_api else rands()
        response = await response_func({**request_json, 'paymethod_id': paymethod_id})

        assert_that(
            await response.json(),
            has_entries({
                'status': 'fail',
                'code': 400,
            })
        )

    class TestUrlMatch:
        @pytest.fixture
        def internal_api(self):
            return False

        @pytest.fixture(params=['', '/'])
        def response_func(self, request, action, payments_client, test_data):
            async def _inner(request_json, headers=None):
                return await payments_client.post(f"{test_data['path']}{request.param}", json=request_json,
                                                  allow_redirects=False)

            return _inner

        @pytest.mark.asyncio
        async def test_url_match__called(self, response, action):
            action.assert_called_once()

    class TestMaxAmountKindCheck:
        @pytest.fixture
        def max_amount(self):
            return 10

        @pytest.fixture
        def kind(self):
            return None

        @pytest.mark.asyncio
        async def test_max_amount_kind_check(self, request_json, response_func):
            response = await response_func(request_json)
            assert_that(
                await response.json(),
                has_entries({
                    'status': 'fail',
                    'code': 400,
                })
            )


class TestScheduleOrderClearUnholdHandler(BaseTestOrder):
    @pytest.fixture
    def action(self, mock_action):
        from mail.payments.payments.core.actions.order.clear_unhold import ScheduleClearUnholdOrderAction
        return mock_action(ScheduleClearUnholdOrderAction)

    @pytest.fixture(params=('clear', 'unhold'))
    def operation(self, request):
        return request.param

    @pytest.fixture
    async def response(self, action, payments_client, order, operation):
        return await payments_client.post(f'/v1/order/{order.uid}/{order.order_id}/{operation}')

    @pytest.fixture
    def expected_context(self, order, operation):
        return {
            'uid': order.uid,
            'order_id': order.order_id,
            'operation': operation,
        }

    def test_context(self, expected_context, response, action):
        action.assert_called_once_with(**expected_context)

    @pytest.mark.parametrize('operation', ('aa',))
    def test_bad_operation(self, response):
        assert response.status == 404


class TestRefundHandler(BaseTestOrder):
    @pytest.fixture
    def caption(self):
        return ' test-refund-handler-caption '

    @pytest.fixture
    def description(self):
        return 'test-refund-handler-description'

    @pytest.fixture
    def request_json(self, items, caption, description):
        return {
            'caption': caption,
            'description': description,
            'items': [
                {
                    'amount': float(item.amount),
                    'currency': item.currency,
                    'name': item.name,
                    'nds': item.nds.value,
                    'price': float(item.price),
                }
                for item in items
            ]
        }

    @pytest.fixture
    def action(self, mock_action):
        from mail.payments.payments.core.actions.order.refund import CreateRefundAction
        return mock_action(CreateRefundAction)

    @pytest.fixture
    async def response(self, action, payments_client, refund, request_json):
        return await payments_client.post(f'/v1/order/{refund.uid}/{refund.original_order_id}/refund',
                                          json=request_json)

    @pytest.fixture
    def expected_context(self, items, refund, caption, description):
        return {
            'uid': refund.uid,
            'order_id': refund.original_order_id,
            'caption': caption.strip(),
            'description': description,
            'items': [
                {
                    'amount': item.amount,
                    'currency': item.currency,
                    'name': item.name,
                    'nds': item.nds,
                    'price': item.price,
                }
                for item in items
            ]
        }

    def test_context(self, action, expected_context, response):
        action.assert_called_once_with(**expected_context)


class TestRefundCustomerSubscriptionTransactionHandler(BaseTestOrder):
    @pytest.fixture
    def caption(self):
        return ' test-refund-handler-caption '

    @pytest.fixture
    def description(self):
        return 'test-refund-handler-description'

    @pytest.fixture
    def request_json(self, caption, description):
        return {
            'caption': caption,
            'description': description,
        }

    @pytest.fixture
    def action(self, mock_action):
        from mail.payments.payments.core.actions.order.refund import CreateCustomerSubscriptionTransactionRefundAction
        return mock_action(CreateCustomerSubscriptionTransactionRefundAction)

    @pytest.fixture
    async def response(self, action, payments_client, merchant, customer_subscription,
                       customer_subscription_transaction, request_json):
        uid = merchant.uid
        subs_id = customer_subscription.customer_subscription_id
        tx_id = customer_subscription_transaction.purchase_token
        return await payments_client.post(
            f'/v1/customer_subscription/{uid}/{subs_id}/{tx_id}/refund',
            json=request_json
        )

    @pytest.fixture
    def expected_context(self, refund, caption, description, customer_subscription, customer_subscription_transaction):
        return {
            'uid': refund.uid,
            'customer_subscription_id': customer_subscription.customer_subscription_id,
            'purchase_token': customer_subscription_transaction.purchase_token,
            'caption': caption.strip(),
            'description': description,
        }

    def test_context(self, action, expected_context, response):
        action.assert_called_once_with(**expected_context)


class TestCancelHandler(BaseTestOrder):
    @pytest.fixture
    def request_json(self):
        return {}

    @pytest.fixture(autouse=True)
    def action(self, mock_action, order):
        from mail.payments.payments.core.actions.order.cancel import CancelOrderAction
        return mock_action(CancelOrderAction, order)

    @pytest.fixture
    async def response(self, action, order, payments_client, request_json):
        return await payments_client.post(
            f'/v1/order/{order.uid}/{order.order_id}/cancel',
            json=request_json,
        )

    def test_success(self, response):
        assert response.status == 200

    def test_context(self, action, order, response):
        action.assert_called_once_with(
            uid=order.uid,
            order_id=order.order_id,
            select_customer_subscription=None,
        )


class TestOrderEmailHandler(BaseTestOrder):
    @pytest.fixture
    def action(self, mock_action):
        from mail.payments.payments.core.actions.order.email import OrderEmailAction
        return mock_action(OrderEmailAction)

    @pytest.fixture(params=(True, False))
    def spam_check(self, request):
        return request.param

    @pytest.fixture
    def request_json(self, spam_check, randmail):
        return {
            'to_email': f' {randmail()} ',
            'reply_email': f' {randmail()} ',
            'spam_check': spam_check,
            'select_customer_subscription': None,
        }

    @pytest.fixture
    async def response(self, action, payments_client, order, request_json):
        return await payments_client.post(f'/v1/order/{order.uid}/{order.order_id}/email', json=request_json)

    def test_context(self, request_json, response, action):
        assert_that(action.call_args[1], has_entries(**strip_fields(request_json)))


class TestDownloadMultiOrderEmailListHandler:
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.order.create_from_multi import DownloadMultiOrderEmailListAction
        return mock_action(DownloadMultiOrderEmailListAction)

    @pytest.fixture
    async def response(self, payments_client, multi_order):
        return await payments_client.get(f'/v1/order/{multi_order.uid}/multi/{multi_order.order_id}/download')

    def test_params(self, multi_order, response, action):
        action.assert_called_once_with(uid=multi_order.uid, order_id=multi_order.order_id)


class TestPayOfflineOrderHandler(BaseTestOrder):
    @pytest.fixture(autouse=True)
    def action(self, internal_api, mock_action, order, items):
        if internal_api:
            from mail.payments.payments.core.actions.order.pay_offline import PayOfflineServiceMerchantOrderAction
            return mock_action(PayOfflineServiceMerchantOrderAction, order)
        else:
            from mail.payments.payments.core.actions.order.pay_offline import PayOfflineOrderAction
            return mock_action(PayOfflineOrderAction, order)

    @pytest.fixture
    def uid_path(self, order):
        return f'/v1/order/{order.uid}/{order.order_id}/pay_offline'

    @pytest.fixture
    def service_merchant_path(self, service_merchant, order):
        return f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/pay_offline'

    @pytest.fixture
    def service_merchant_context(self, rands, order, tvm_client_id, service_merchant):
        return {
            'order_id': order.order_id,
            'customer_uid': None,
            'service_tvm_id': tvm_client_id,
            'service_merchant_id': service_merchant.service_merchant_id,
        }

    @pytest.fixture
    def uid_context(self, order):
        return {
            'uid': order.uid,
            'order_id': order.order_id,
            'customer_uid': None,
        }

    @pytest.fixture
    def response_func(self, payments_client, test_data):
        async def _inner(**kwargs):
            return await payments_client.post(test_data['path'], json=kwargs)

        return _inner

    def test_params(self, test_data, response, action):
        action.assert_called_once_with(**test_data['context'])

    @pytest.mark.asyncio
    @pytest.mark.parametrize('internal_api', (True,))
    async def test_service_data(self, rands, action, service_merchant_context, response_func):
        service_data = {rands(): rands()}
        await response_func(service_data=service_data)
        action.assert_called_once_with(**service_merchant_context, service_data=service_data)
