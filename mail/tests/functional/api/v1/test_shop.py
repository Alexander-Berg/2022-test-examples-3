from datetime import timezone

import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.payments.payments.core.entities.enums import ShopType


@pytest.fixture
def shop_data(rands):
    return {
        'name': rands(),
        'is_default': False,
        'shop_type': ShopType.PROD.value
    }


@pytest.fixture
async def shop(client, shop_data, merchant):
    r = await client.post(f'/v1/shop/{merchant.uid}', json=shop_data)
    assert r.status == 200
    return (await r.json())['data']


class TestGetShopList:
    @pytest.fixture
    async def response_json(self, client, merchant):
        r = await client.get(f'/v1/shop/{merchant.uid}')
        assert r.status == 200
        return await r.json()

    def test_response(self, shop, response_json, default_merchant_shops):
        default_shops = [
            {
                'shop_type': shop.shop_type.value,
                'updated': shop.updated.astimezone(timezone.utc).isoformat(),
                'created': shop.updated.astimezone(timezone.utc).isoformat(),
                'is_default': shop.is_default,
                'name': shop.name,
                'shop_id': shop.shop_id,
                'uid': shop.uid,
                'oauth': None,
            }
            for shop in default_merchant_shops.values()
        ]
        assert_that(response_json, has_entries({
            'data': contains_inanyorder(shop, *default_shops)
        }))


class TestPutShop:
    @pytest.fixture
    def put_shop_data(self, rands):
        return {
            'name': rands(),
            'is_default': True,
        }

    @pytest.fixture
    async def response_json(self, put_shop_data, shop, client, merchant):
        r = await client.put(f'/v1/shop/{merchant.uid}/{shop["shop_id"]}', json=put_shop_data)
        assert r.status == 200
        return await r.json()

    def test_response(self, put_shop_data, response_json):
        assert_that(response_json, has_entries({
            'data': has_entries(**put_shop_data)
        }))
