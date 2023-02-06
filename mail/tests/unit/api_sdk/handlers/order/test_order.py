from datetime import timezone
from decimal import Decimal

import pytest

from sendr_utils import enum_value

from hamcrest import assert_that, contains, contains_inanyorder, has_entries

from mail.payments.payments.core.entities.enums import NDS, PAY_METHOD_OFFLINE, PAYMETHOD_ID_OFFLINE, OrderSource
from mail.payments.payments.utils.helpers import without_none


@pytest.mark.asyncio
async def template_case__order_response(self, payments_settings, response, order, items):
    """
    Пробуем сделать тесты плоскими (без наследования) - общие тестовые случаи можно выносить в обычные функции,
    присваивая их в соответствующих местах как тест-кейсы.
    """
    assert order.user_email is not None
    response_json = await response.json()
    assert_that(
        (response.status, response_json),
        contains(
            200,
            has_entries({
                'status': 'success',
                'data': has_entries({
                    'pay_status': order.pay_status.value,
                    'price': float(round(order.price, 2)),
                    'updated': order.updated.astimezone(timezone.utc).isoformat(),
                    'revision': order.revision,
                    'active': order.active,
                    'autoclear': order.autoclear,
                    'order_id': order.order_id,
                    'description': order.description,
                    'currency': order.currency,
                    'created': order.created.astimezone(timezone.utc).isoformat(),
                    'caption': order.caption,
                    'payments_url': order.payments_url,
                    'pay_token': f'{payments_settings.PAY_TOKEN_PREFIX}None',
                    'pay_method': order.pay_method,
                    'items': contains_inanyorder(*[
                        {
                            'amount': float(round(item.amount, 2)),  # type: ignore
                            'currency': item.currency,
                            'name': item.name,
                            'nds': item.nds.value,
                            'price': float(round(item.price, 2)),  # type: ignore
                            'product_id': item.product_id,
                            'image': {
                                'url': item.image.url,
                                'stored': {
                                    'original': item.image.stored.orig,
                                } if item.image.stored else None,
                            } if item.image else None,
                            'markup': item.markup,
                        }
                        for item in items
                    ]),
                    'user_email': order.user_email,
                })
            })
        )
    )


@pytest.mark.asyncio
async def template_case__user_email_in_order_response(order, response):
    response_json = await response.json()
    assert response_json['data']['user_email'] == order.user_email


class TestOrderGet:
    @pytest.fixture
    def action(self, mock_action, order, items):
        from mail.payments.payments.core.actions.order.get import CoreGetOrderAction
        return mock_action(CoreGetOrderAction, order)

    @pytest.fixture
    async def response(self, action, sdk_client, order):
        return await sdk_client.get(f'/v1/order/{order.order_id}')

    def test_context(self, order, crypto_mock, response, merchant, action):
        action.assert_called_once_with(uid=merchant.uid, order_id=order.order_id)

    test_response = template_case__order_response


class TestOrderPost:
    @pytest.fixture
    def action(self, mock_action, order, items):
        order.items = items
        from mail.payments.payments.core.actions.order.create_or_update import CoreCreateOrUpdateOrderAction
        return mock_action(CoreCreateOrUpdateOrderAction, order)

    @pytest.fixture
    def extra_context(self):
        return {}

    @pytest.fixture
    def pay_method(self):
        return None

    @pytest.fixture
    def paymethod_id(self):
        return None

    @pytest.fixture
    def request_json(self, rands, pay_method, paymethod_id, randbool, randn, randitem, randdecimal,
                     merchant_oauth_mode):
        return without_none({
            'caption': rands(),
            'description': rands(),
            'mode': enum_value(merchant_oauth_mode),
            'meta': rands(),
            'autoclear': randbool(),
            'return_url': rands(),
            'pay_method': pay_method,
            'paymethod_id': paymethod_id,
            'items': [
                {
                    'amount': str(randdecimal(max=10)),
                    'currency': 'RUB',
                    'name': rands(),
                    'nds': enum_value(randitem(NDS)),
                    'price': str(randdecimal(max=100)),
                    'image': {
                        'url': rands(),
                    }
                } for _ in range(randn(min=2, max=10))
            ],
        })

    @pytest.fixture
    def response_func(self, action, sdk_client):
        async def _inner(request_json):
            return await sdk_client.post('/v1/order', json=request_json)

        return _inner

    @pytest.fixture
    async def response(self, request_json, response_func):
        return await response_func(request_json)

    test_response = template_case__order_response

    @pytest.mark.parametrize('pop_key', ['items'])
    @pytest.mark.asyncio
    async def test_bad_request(self, request_json, response_func, pop_key):
        request_json.pop(pop_key)
        response = await response_func(request_json)
        assert_that(
            (response.status, await response.json()),
            contains(400, has_entries({'status': 'fail', 'code': 400}))
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

    @pytest.mark.parametrize('paymethod_id', ['123'])
    @pytest.mark.asyncio
    async def test_paymethod_id(self, request_json, response):
        assert_that(
            (response.status, await response.json()),
            contains(400, has_entries({'status': 'fail', 'code': 400}))
        )

    @pytest.mark.parametrize('pop_key', ['amount', 'currency', 'name', 'nds', 'price'])
    @pytest.mark.asyncio
    async def test_bad_request_items(self, request_json, response_func, pop_key):
        request_json['items'][0].pop(pop_key)
        response = await response_func(request_json)

        assert_that(
            (response.status, await response.json()),
            contains(400, has_entries({'status': 'fail', 'code': 400}))
        )

    @pytest.mark.parametrize('pay_method', (None, 'offline'))
    def test_context(self, action, merchant, shop_type, pay_method, request_json, response, extra_context):
        if pay_method == PAY_METHOD_OFFLINE:
            extra_context['paymethod_id'] = PAYMETHOD_ID_OFFLINE

        action.assert_called_once_with(
            source=OrderSource.SDK_API,
            uid=merchant.uid,
            default_shop_type=shop_type,
            caption=request_json['caption'],
            description=request_json['description'],
            meta=request_json['meta'],
            return_url=request_json['return_url'],
            autoclear=request_json['autoclear'],
            items=[
                {
                    'amount': Decimal(str(item['amount'])),
                    'price': Decimal(str(item['price'])),
                    'currency': item['currency'],
                    'nds': NDS(item['nds']),
                    'name': item['name'],
                    'image': {
                        'url': item['image']['url'],
                    }
                }
                for item in request_json['items']
            ],
            **extra_context,
        )


class TestOrderPut(TestOrderPost):
    @pytest.fixture
    def response_func(self, action, order, sdk_client):
        async def _inner(request_json):
            return await sdk_client.put(f'/v1/order/{order.order_id}', json=request_json)

        return _inner

    @pytest.fixture
    def extra_context(self, order):
        return {
            'order_id': order.order_id
        }

    @pytest.mark.parametrize('pay_method', (None, 'offline'))
    def test_context(self, action, merchant, pay_method, request_json, response, extra_context):
        if pay_method == PAY_METHOD_OFFLINE:
            extra_context['paymethod_id'] = PAYMETHOD_ID_OFFLINE

        action.assert_called_once()
        actual_kwargs = action.call_args[1]
        expected_kwargs = dict(
            uid=merchant.uid,
            caption=request_json['caption'],
            description=request_json['description'],
            meta=request_json['meta'],
            autoclear=request_json['autoclear'],
            items=[
                {
                    'amount': Decimal(str(item['amount'])),
                    'price': Decimal(str(item['price'])),
                    'currency': item['currency'],
                    'nds': NDS(item['nds']),
                    'name': item['name'],
                    'image': {
                        'url': item['image']['url'],
                    }
                }
                for item in request_json['items']
            ],
            return_url=request_json['return_url'],
            source=OrderSource.SDK_API,
            **extra_context,
        )
        # allows more granular diffs
        assert actual_kwargs == expected_kwargs


class TestOrderDeactivate:
    @pytest.fixture
    def action(self, mock_action, order, items):
        from mail.payments.payments.core.actions.order.activate import CoreActivateOrderAction
        return mock_action(CoreActivateOrderAction, order)

    @pytest.fixture
    async def response(self, action, sdk_client, order):
        return await sdk_client.post(f'/v1/order/{order.order_id}/deactivate')

    test_response = template_case__order_response

    def test_context(self, order, crypto_mock, response, merchant, action):
        action.assert_called_once_with(uid=merchant.uid, order_id=order.order_id, active=False)


class TestPayOfflineOrder:
    @pytest.fixture
    def action(self, mock_action, order, items):
        from mail.payments.payments.core.actions.order.pay_offline import CorePayOfflineOrderAction
        return mock_action(CorePayOfflineOrderAction, order)

    @pytest.fixture
    async def response(self, action, sdk_client, order):
        return await sdk_client.post(f'/v1/order/{order.order_id}/pay_offline')

    test_response = template_case__order_response

    def test_context(self, order, crypto_mock, response, merchant, action):
        action.assert_called_once_with(uid=merchant.uid, order_id=order.order_id)


class TestCancelOrder:
    @pytest.fixture
    def action(self, mock_action, order, items):
        from mail.payments.payments.core.actions.order.cancel import CoreCancelOrderAction
        return mock_action(CoreCancelOrderAction, order)

    @pytest.fixture
    async def response(self, action, sdk_client, order):
        return await sdk_client.post(f'/v1/order/{order.order_id}/cancel')

    test_response = template_case__order_response

    def test_context(self, order, crypto_mock, response, merchant, action):
        action.assert_called_once_with(uid=merchant.uid, order_id=order.order_id)


class TestClearUnholdOrderHandler:
    @pytest.fixture(params=('clear', 'unhold'))
    def operation(self, request):
        return request.param

    @pytest.fixture
    def action(self, mock_action, order, items):
        from mail.payments.payments.core.actions.order.clear_unhold import CoreScheduleClearUnholdOrderAction
        return mock_action(CoreScheduleClearUnholdOrderAction)

    @pytest.fixture
    async def response(self, action, sdk_client, order, operation):
        return await sdk_client.post(f'/v1/order/{order.order_id}/{operation}')

    def test_context(self, response, merchant, order, action, operation):
        action.assert_called_once_with(uid=merchant.uid, order_id=order.order_id, operation=operation)

    @pytest.mark.asyncio
    async def test_response(self, response):
        assert_that(
            (response.status, await response.json()),
            contains(
                200,
                has_entries({
                    'code': 200,
                    'status': 'success',
                    'data': {}
                })
            )
        )

    class TestResizeParams:
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
        async def response(self, action, sdk_client, order, operation, request_json):
            return await sdk_client.post(f'/v1/order/{order.order_id}/{operation}', json=request_json)

        def test_context_with_items(self, response, merchant, order, action, operation, items):
            action.assert_called_once_with(
                uid=merchant.uid,
                order_id=order.order_id,
                operation=operation,
                items=[
                    {
                        'product_id': item.product_id,
                        'amount': item.amount,
                        'price': item.price,
                    }
                    for item in items
                ]
            )
