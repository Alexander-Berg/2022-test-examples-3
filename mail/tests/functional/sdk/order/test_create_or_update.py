from datetime import timezone

import pytest

from sendr_utils import alist, enum_value

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.payments.payments.tests.base import BaseSdkOrderTest


@pytest.mark.usefixtures('moderation')
class BaseTestCreateOrUpdateOrder(BaseSdkOrderTest):
    @pytest.fixture
    async def response(self, moderation, sdk_client, http_method, path, order_data):
        r = await getattr(sdk_client, http_method)(path, json=order_data)
        assert r.status == 200
        return await r.json()

    @pytest.fixture
    async def created_order(self, merchant, storage, response):
        data = response['data']
        order = await storage.order.get(uid=merchant.uid, order_id=data['order_id'])
        order.items = await alist(storage.item.get_for_order(uid=order.uid, order_id=order.order_id))
        return order

    def test_order(self, created_order, order_data, response):
        assert_that(
            response['data'],
            has_entries({
                'meta': created_order.data.meta,
                'pay_status': enum_value(created_order.pay_status),
                'price': float(round(created_order.price, 2)),
                'updated': created_order.updated.astimezone(timezone.utc).isoformat(),
                'revision': created_order.revision,
                'active': created_order.active,
                'order_id': created_order.order_id,
                'description': created_order.description,
                'currency': created_order.currency,
                'mode': enum_value(created_order.shop.shop_type),
                'created': created_order.created.astimezone(timezone.utc).isoformat(),
                'caption': created_order.caption,
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
                            'stored': None,
                        } if item.image is not None else None,
                        'markup': item.markup,
                    }
                    for item in created_order.items
                ])
            })
        )

    def test_hashes(self, crypto_mock, response, merchant, payments_settings):
        data = response['data']
        pay_token = data['pay_token'][len(payments_settings.PAY_TOKEN_PREFIX):]

        with crypto_mock.decrypt_payment(pay_token) as order:
            assert_that(order, has_entries({
                'uid': merchant.uid,
                'order_id': data['order_id'],
            }))

    @pytest.mark.asyncio
    async def test_duplicate_item(self, sdk_client, path, http_method, order_data):
        r = await getattr(sdk_client, http_method)(path, json={
            **order_data,
            'items': [order_data['items'][0]] * 2,
        })
        assert_that(
            await r.json(),
            has_entries({
                'code': 400,
                'data': has_entries({
                    'message': 'ORDER_HAS_DUPLICATE_ITEM_ENTRIES',
                }),
                'status': 'fail',
            })
        )

    @pytest.mark.asyncio
    async def test_100k_price(self, sdk_client, path, http_method, order_data):
        r = await getattr(sdk_client, http_method)(path, json={
            **order_data,
            'items': [{
                **order_data['items'][0],
                'amount': 100 * 100,  # Should it be 10^7 instead of 10^5?
                'price': 1000.01,
            }],
        })
        assert_that(
            await r.json(),
            has_entries({
                'code': 400,
                'data': has_entries({
                    'message': 'ORDER_PRICE_EXCEEDS_100K_RUB',
                }),
                'status': 'fail',
            })
        )


class TestCreateOrder(BaseTestCreateOrUpdateOrder):
    @pytest.fixture
    def path(self):
        return '/v1/order'

    @pytest.fixture
    def http_method(self):
        return 'post'


class TestUpdateOrder(BaseTestCreateOrUpdateOrder):
    @pytest.fixture
    def path(self, order):
        return f'/v1/order/{order.order_id}'

    @pytest.fixture
    def http_method(self):
        return 'put'
