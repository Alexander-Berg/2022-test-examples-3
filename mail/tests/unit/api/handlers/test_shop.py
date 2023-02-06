from datetime import timezone

import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.actions.shop.create_or_update import CreateOrUpdateShopAction
from mail.payments.payments.core.actions.shop.get_list import GetShopListAction
from mail.payments.payments.core.entities.enums import ShopType


class TestGetShopListHandler:
    @pytest.fixture
    def limit(self, randn):
        return randn(max=100)

    @pytest.fixture(params=(True, False))
    def merchant_oauth(self, request, merchant_oauth):
        return merchant_oauth if request.param else None

    @pytest.fixture
    def offset(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    def action(self, mock_action, merchant_oauth, shop):
        shop.oauth = merchant_oauth
        return mock_action(GetShopListAction, [shop])

    @pytest.fixture
    def params(self, offset, limit):
        return {
            'limit': limit,
            'offset': offset,
        }

    @pytest.fixture
    async def response(self, uid, payments_client, params):
        return await payments_client.get(f'/v1/shop/{uid}', params=params)

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_called(self, response, action, uid, params):
        action.assert_called_once_with(uid=uid, offset=params['offset'], limit=params['limit'],
                                       with_merchant_oauth=True)

    @pytest.mark.parametrize(('limit', 'offset'), (
        (-1, 0),
        (101, 0),
        (1, -1),
    ))
    def test_error(self, response):
        assert response.status == 400

    def test_response(self, response_json, merchant_oauth, shop):
        assert_that(response_json, has_entries({
            'status': 'success',
            'code': 200,
            'data': [
                {
                    'uid': shop.uid,
                    'shop_id': shop.shop_id,
                    'name': shop.name,
                    'shop_type': shop.shop_type.value,
                    'is_default': shop.is_default,
                    'created': shop.created.astimezone(timezone.utc).isoformat(),
                    'updated': shop.updated.astimezone(timezone.utc).isoformat(),
                    'oauth': {
                        'mode': 'prod',
                        'expired': False,
                        'account_id': merchant_oauth.data['account_id']
                    } if merchant_oauth else None,
                }
            ]
        }))


class TestPostShopListHandler:
    @pytest.fixture(autouse=True)
    def action(self, mock_action, shop):
        return mock_action(CreateOrUpdateShopAction, shop)

    @pytest.fixture
    def param_shop_type(self, randitem):
        return randitem(ShopType).value

    @pytest.fixture
    def params(self, rands, randbool, param_shop_type):
        return {
            'name': rands(),
            'is_default': randbool(),
            'shop_type': param_shop_type,
        }

    @pytest.fixture
    def response_func(self, uid, payments_client, params):
        async def _inner():
            return await payments_client.post(f'/v1/shop/{uid}', json=params)

        return _inner

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_called(self, response, shop_type, action, uid, params):
        action.assert_called_once_with(
            uid=uid,
            name=params['name'],
            is_default=params['is_default'],
            shop_type=ShopType(params['shop_type'])
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('pop_key', ['name', 'shop_type'])
    async def test_error(self, pop_key, params, response_func):
        params.pop(pop_key)
        response = await response_func()
        assert response.status == 400

    @pytest.mark.parametrize('param_shop_type', ['shop_type'])
    def test_invalid_shop_type(self, params, response):
        assert response.status == 400

    def test_response(self, response_json, shop):
        assert_that(response_json, has_entries({
            'status': 'success',
            'code': 200,
            'data': {
                'uid': shop.uid,
                'shop_id': shop.shop_id,
                'name': shop.name,
                'shop_type': shop.shop_type.value,
                'is_default': shop.is_default,
                'created': shop.created.astimezone(timezone.utc).isoformat(),
                'updated': shop.updated.astimezone(timezone.utc).isoformat(),
                'oauth': None,
            }
        }))


class TestPutShopHandler:
    @pytest.fixture(autouse=True)
    def action(self, mock_action, shop):
        return mock_action(CreateOrUpdateShopAction, shop)

    @pytest.fixture
    def params(self, rands, randbool):
        return {
            'name': rands(),
            'is_default': randbool(),
        }

    @pytest.fixture
    def response_func(self, uid, shop, payments_client, params):
        async def _inner():
            return await payments_client.put(f'/v1/shop/{uid}/{shop.shop_id}', json=params)

        return _inner

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    @pytest.fixture
    async def response_json(self, response):
        return await response.json()

    def test_called(self, response, action, uid, shop, params):
        action.assert_called_once_with(
            uid=uid, shop_id=shop.shop_id, name=params['name'], is_default=params['is_default']
        )

    @pytest.mark.asyncio
    @pytest.mark.parametrize('pop_key', ['name', 'is_default'])
    async def test_error(self, pop_key, params, response_func):
        params.pop(pop_key)
        response = await response_func()
        assert response.status == 400

    def test_response(self, response_json, shop):
        assert_that(response_json, has_entries({
            'status': 'success',
            'code': 200,
            'data': {
                'uid': shop.uid,
                'shop_id': shop.shop_id,
                'name': shop.name,
                'shop_type': shop.shop_type.value,
                'is_default': shop.is_default,
                'created': shop.created.astimezone(timezone.utc).isoformat(),
                'updated': shop.updated.astimezone(timezone.utc).isoformat(),
                'oauth': None,
            }
        }))
