from datetime import timezone
from decimal import Decimal

import pytest

from hamcrest import assert_that, contains, contains_inanyorder, has_entries

from mail.payments.payments.core.entities.enums import NDS


class BaseRefundTest:
    @pytest.mark.asyncio
    async def test_response(self, response, response_json, refund, items):
        assert_that(
            (response.status, response_json),
            contains(
                200,
                has_entries({
                    'status': 'success',
                    'data': has_entries({
                        'refund_status': refund.refund_status.value,
                        'price': float(round(refund.price, 2)),
                        'updated': refund.updated.astimezone(timezone.utc).isoformat(),
                        'revision': refund.revision,
                        'active': refund.active,
                        'refund_id': refund.order_id,
                        'order_id': refund.original_order_id,
                        'description': refund.description,
                        'mode': refund.merchant_oauth_mode.value,
                        'currency': refund.currency,
                        'created': refund.created.astimezone(timezone.utc).isoformat(),
                        'caption': refund.caption,
                        'items': contains_inanyorder(*[
                            {
                                'amount': float(round(item.amount, 2)),  # type: ignore
                                'currency': item.currency,
                                'name': item.name,
                                'nds': item.nds.value,
                                'price': float(round(item.price, 2)),  # type: ignore
                                'product_id': item.product_id,
                                'markup': item.markup,
                            }
                            for item in items
                        ])
                    })
                })
            )
        )


class TestRefundGet(BaseRefundTest):
    @pytest.fixture
    def action(self, mock_action, refund, items):
        refund.items = items
        from mail.payments.payments.core.actions.order.get import CoreGetOrderAction
        return mock_action(CoreGetOrderAction, refund)

    @pytest.fixture
    async def response(self, action, sdk_client, order, refund):
        return await sdk_client.get(f'/v1/order/{order.order_id}/refund/{refund.order_id}')

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_context(self, refund, order, crypto_mock, response, merchant, action):
        action.assert_called_once_with(uid=merchant.uid, order_id=refund.order_id, original_order_id=order.order_id)


class TestRefundPost(BaseRefundTest):
    @pytest.fixture
    def action(self, mock_action, refund, items):
        refund.items = items
        from mail.payments.payments.core.actions.order.refund import CoreCreateRefundAction
        return mock_action(CoreCreateRefundAction, refund)

    @pytest.fixture
    def request_json(self, rands):
        return {
            'meta': rands(),
            'caption': 'abc',
            'description': 'def',
            'items': [
                {
                    'amount': 22.33,
                    'currency': 'RUB',
                    'name': 'roses',
                    'nds': 'nds_none',
                    'price': 99.99,
                },
                {
                    'amount': 100,
                    'currency': 'RUB',
                    'name': 'smth',
                    'nds': 'nds_10',
                    'price': 1000,
                }
            ],
        }

    @pytest.fixture
    def response_func(self, action, order, sdk_client):
        async def _inner(request_json):
            return await sdk_client.post(f'/v1/order/{order.order_id}/refund', json=request_json)

        return _inner

    @pytest.fixture
    async def response(self, request_json, response_func):
        return await response_func(request_json)

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

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

    @pytest.mark.parametrize('pop_key', ['amount', 'currency', 'name', 'nds', 'price'])
    @pytest.mark.asyncio
    async def test_bad_request_items(self, request_json, response_func, pop_key):
        request_json['items'][0].pop(pop_key)
        response = await response_func(request_json)

        assert_that(
            (response.status, await response.json()),
            contains(400, has_entries({'status': 'fail', 'code': 400}))
        )

    def test_context(self, action, merchant, order, request_json, response):
        action.assert_called_once_with(
            uid=merchant.uid,
            order_id=order.order_id,
            meta=request_json['meta'],
            caption=request_json['caption'],
            description=request_json['description'],
            items=[
                {
                    'amount': Decimal(str(item['amount'])),
                    'price': Decimal(str(item['price'])),
                    'currency': item['currency'],
                    'nds': NDS(item['nds']),
                    'name': item['name'],
                }
                for item in request_json['items']
            ]
        )
