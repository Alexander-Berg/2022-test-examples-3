from datetime import timezone

import pytest

from sendr_utils import enum_value

from hamcrest import assert_that, contains, contains_inanyorder, has_entries, is_

from mail.payments.payments.tests.base import BaseSdkOrderTest


class TestGetOrder(BaseSdkOrderTest):
    @pytest.fixture
    def order_id(self, order):
        return order.order_id

    @pytest.fixture
    async def response(self, sdk_client, order_id):
        return await sdk_client.get(f'/v1/order/{order_id}')

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    @pytest.mark.asyncio
    async def test_response_format(self, response):
        assert_that(
            (response.status, await response.json()),
            contains(200, has_entries({'code': 200, 'status': 'success', 'data': is_(dict)}))
        )

    @pytest.mark.asyncio
    async def test_response_data(self, order, order_data, response_json):
        assert_that(
            response_json['data'],
            has_entries({
                'pay_status': order.pay_status.value,
                'price': float(round(order.price, 2)),
                'updated': order.updated.astimezone(timezone.utc).isoformat(),
                'revision': order.revision,
                'active': order.active,
                'order_id': order.order_id,
                'description': order.description,
                'currency': order.currency,
                'created': order.created.astimezone(timezone.utc).isoformat(),
                'caption': order.caption,
                'mode': enum_value(order.shop.shop_type),
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
                            } if item.image.stored is not None else None,
                        } if item.image is not None else None,
                        'markup': item.markup,
                    }
                    for item in order.items
                ])
            })
        )

    def test_hashes(self, crypto_mock, order_response, merchant, payments_settings):
        data = order_response['data']
        pay_token = data['pay_token'][len(payments_settings.PAY_TOKEN_PREFIX):]

        with crypto_mock.decrypt_payment(pay_token) as order:
            assert_that(order, has_entries({
                'uid': merchant.uid,
                'order_id': data['order_id'],
            }))

    @pytest.mark.parametrize('order_id', (0,))
    @pytest.mark.asyncio
    async def test_not_found(self, response):
        assert_that(
            (response.status, await response.json()),
            contains(404, has_entries({'code': 404, 'status': 'fail'}))
        )
