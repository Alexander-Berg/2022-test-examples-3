from datetime import datetime, timezone

import pytest

from sendr_utils import enum_value, utcnow

from hamcrest import assert_that, contains_inanyorder, has_entries, has_items, has_properties, is_

from mail.payments.payments.core.entities.enums import (
    PAY_METHOD_OFFLINE, PAYMETHOD_ID_OFFLINE, MerchantRole, OrderKind, PaymentsTestCase, ShopType
)
from mail.payments.payments.tests.base import BaseTestMerchantRoles
from mail.payments.payments.tests.utils import check_order
from mail.payments.payments.utils.helpers import without_none

from .base import BaseTestOrder


@pytest.mark.usefixtures('moderation')
class BaseTestCreateOrUpdateOrder(BaseTestOrder):
    @pytest.fixture(params=(None, OrderKind.PAY, OrderKind.MULTI))
    def kind(self, request):
        return request.param

    @pytest.fixture(params=[None, PaymentsTestCase.TEST_OK_CLEAR])
    def test(self, request):
        return request.param

    @pytest.fixture
    def pay_method(self):
        return None

    @pytest.fixture
    def extra_order_data(self):
        return {}

    @pytest.fixture
    def order_data(self, kind, pay_method, test, extra_order_data):
        return without_none({
            'caption': 'Some test order',
            'description': 'Some description',
            'items': [
                {
                    'name': 'first',
                    'amount': 2,
                    'price': 100,
                    'nds': 'nds_10_110',
                    'currency': 'RUB',
                },
                {
                    'name': 'second',
                    'amount': 3.33,
                    'price': 100.77,
                    'nds': 'nds_10',
                    'currency': 'RUB',
                },
            ],
            'offline_abandon_deadline': utcnow().isoformat(),
            'pay_method': pay_method,
            'test': enum_value(test),
            'kind': enum_value(kind),
            'max_amount': 10 if kind == OrderKind.MULTI else None,
            'fast_moderation': True,
            **extra_order_data,
        })

    @pytest.fixture
    def service_merchant_order_test_data(self, service_merchant):
        return {'path': f'/v1/internal/order/{service_merchant.service_merchant_id}/'}

    @pytest.fixture
    def uid_order_test_data(self, merchant):
        return {'path': f'/v1/order/{merchant.uid}/'}

    @pytest.fixture
    async def order_response(self, client, http_method, tvm, test_data, order_data):
        r = await getattr(client, http_method)(test_data['path'], json=order_data)
        assert r.status == 200
        return await r.json()

    @pytest.fixture
    async def created_order(self, storage, order_response):
        data = order_response['data']
        return await storage.order.get(uid=data['uid'], order_id=data['order_id'])

    def test_order(self, kind, created_order, order_data, order_response):
        check_order(
            created_order,
            order_response['data'],
            {
                'price': round(sum([
                    item['price'] * item['amount']
                    for item in order_data['items']
                ]), 2),  # type: ignore
                'currency': 'RUB',
                'caption': order_data['caption'],
                'items': contains_inanyorder(*[
                    has_entries({
                        'name': item['name'],
                        'amount': item['amount'],
                        'price': item['price'],
                        'nds': item['nds'],
                        'currency': item['currency'],
                        'product_id': is_(int),
                    })
                    for item in order_data['items']
                ]),
            },
        )

    @pytest.mark.parametrize('pay_method', [PAY_METHOD_OFFLINE])
    @pytest.mark.parametrize('kind', [OrderKind.PAY])
    def test_offline(self, order_data, created_order):
        assert all((
            created_order.paymethod_id == PAYMETHOD_ID_OFFLINE,
            created_order.offline_abandon_deadline == datetime.fromisoformat(order_data['offline_abandon_deadline'])
        ))

    def test_hashes(self, crypto_mock, order_response, check_hashes):
        check_hashes(crypto_mock, order_response['data'])

    @pytest.mark.asyncio
    async def test_duplicate_item(self, client, http_method, order_data, test_data, tvm):
        r = await getattr(client, http_method)(test_data['path'], json={
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
    async def test_100k_price(self, client, http_method, test_data, order_data, tvm):
        r = await getattr(client, http_method)(test_data['path'], json={
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


class BaseTestCreateOrder(BaseTestCreateOrUpdateOrder):
    @pytest.fixture
    def http_method(self):
        return 'post'

    class TestShopIdHeader:
        @pytest.fixture
        def kind(self):
            return OrderKind.PAY

        @pytest.fixture
        def role(self):
            return MerchantRole.ADMIN

        @pytest.fixture
        def test(self):
            return None

        @pytest.fixture
        def shop_id(self):
            return None

        @pytest.fixture
        def headers(self, shop_id):
            hs = {}
            if shop_id:
                hs['X-Shop-Id'] = str(shop_id)
            return hs

        @pytest.fixture
        def mode(self):
            return 'prod'

        @pytest.fixture
        def extra_order_data(self, extra_order_data, mode):
            return {**extra_order_data, 'mode': mode}

        @pytest.fixture
        async def response(self, client, http_method, tvm, test_data, order_data, headers):
            return await getattr(client, http_method)(test_data['path'], json=order_data, headers=headers)

        @pytest.fixture
        async def order_response(self, response):
            assert response.status == 200
            return await response.json()

        class TestSetShopOfCertainTypeWithoutShopIdHeader:
            @pytest.fixture
            def shop_id(self):
                return None

            @pytest.mark.parametrize('mode', ['prod', 'test'])
            @pytest.mark.asyncio
            async def test_shop_id_is_default_shop_id(self, storage, created_order, headers, mode, order_data):
                assert 'X-Shop-Id' not in headers
                shop = await storage.shop.get(uid=created_order.uid, shop_id=created_order.shop_id)
                expected_shop_type = dict(prod=ShopType.PROD, test=ShopType.TEST)[mode]
                assert_that(shop, has_properties(dict(
                    is_default=True,
                    shop_id=created_order.shop_id,
                    shop_type=expected_shop_type,
                )))

        class TestExplicitShopIdHeader:
            @pytest.fixture
            def shop_id(self, shop):
                return shop.shop_id

            @pytest.mark.asyncio
            async def test_shop_id_is_taken_from_shop_id_header(self, storage, created_order, shop, headers):
                assert headers['X-Shop-Id'] == str(shop.shop_id)
                assert created_order.shop_id == shop.shop_id

        class TestNonExistingShopIdHeaderResponseFormat:
            @pytest.fixture
            def shop_id(self, shop):
                return shop.shop_id + 123

            @pytest.mark.asyncio
            async def test_404_api_response_because_shop_not_found(self, response, shop_id):
                assert response.status == 404
                json = await response.json()
                assert_that(
                    json,
                    has_entries({
                        'data':
                            {
                                'message': 'SHOP_NOT_FOUND',
                                'params': {
                                    'message': 'Shop not found',
                                    'params': {
                                        'shop_id': shop_id,
                                    }
                                }
                            },
                        'code': 404,
                        'status': 'fail',
                    })
                )


class TestCreateOrder(BaseTestMerchantRoles, BaseTestCreateOrder):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    @pytest.fixture
    def id_type(self):
        return 'uid'


class TestCreateOrderInternal(BaseTestCreateOrder):
    @pytest.fixture
    def id_type(self):
        return 'service_merchant'

    class TestServiceData:
        @pytest.fixture
        def extra_order_data(self, rands):
            return {'service_data': {rands(): rands()}}

        def test_service_data__response(self, order_data, order_response):
            assert order_data['service_data'] == order_response['data']['service_data']

        def test_service_data__saved(self, order_data, created_order):
            assert order_data['service_data'] == created_order.data.service_data

    class TestCommission:
        @pytest.fixture
        def extra_order_data(self):
            return {'commission': 215}

        def test_commission__saved(self, order_data, created_order):
            assert order_data['commission'] == created_order.commission


class BaseTestUpdateOrder(BaseTestCreateOrUpdateOrder):
    @pytest.fixture
    def http_method(self):
        return 'put'

    @pytest.fixture(params=(None, OrderKind.PAY))
    def kind(self, request):
        return request.param


class TestUpdateOrder(BaseTestMerchantRoles, BaseTestUpdateOrder):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    @pytest.fixture
    def id_type(self):
        return 'uid'

    @pytest.fixture
    def uid_order_test_data(self, merchant, order):
        return {'path': f'/v1/order/{merchant.uid}/{order.order_id}'}


class TestUpdateOrderInternal(BaseTestUpdateOrder):
    @pytest.fixture
    def id_type(self):
        return 'service_merchant'

    @pytest.fixture
    def service_merchant_order_test_data(self, service_merchant, order_with_service):
        return {'path': f'/v1/internal/order/{service_merchant.service_merchant_id}/{order_with_service.order_id}'}

    class TestServiceData:
        @pytest.fixture
        def extra_order_data(self, rands):
            return {'service_data': {rands(): rands()}}

        def test_service_data__response(self, order_data, order_response):
            assert order_data['service_data'] == order_response['data']['service_data']

        def test_service_data__saved(self, order_data, created_order):
            assert order_data['service_data'] == created_order.data.service_data

    class TestCommission:
        @pytest.fixture
        def extra_order_data(self):
            return {'commission': 215}

        def test_commission__saved(self, order_data, created_order):
            assert order_data['commission'] == created_order.commission


@pytest.mark.usefixtures('moderation')
class BaseTestCreateOrderFromMulti(BaseTestOrder):
    @pytest.fixture
    def service_merchant_order_test_data(self, service_merchant, multi_order):
        return {'path': f'/v1/internal/order/{service_merchant.service_merchant_id}/multi/{multi_order.order_id}'}

    @pytest.fixture
    def uid_order_test_data(self, merchant, multi_order):
        return {'path': f'/v1/order/{merchant.uid}/multi/{multi_order.order_id}'}

    @pytest.fixture
    async def order_response(self, client, tvm, test_data, order_data):
        r = await client.post(test_data['path'], json=order_data)
        assert r.status == 200
        return await r.json()

    def test_order(self, multi_order, order_data, order_response):
        order_response['data']['order_id'] = multi_order.order_id
        order_response['data']['revision'] = multi_order.revision
        order_response['data']['kind'] = enum_value(OrderKind.MULTI)
        order_response['data']['created'] = multi_order.created.astimezone(timezone.utc).isoformat()
        order_response['data']['updated'] = multi_order.updated.astimezone(timezone.utc).isoformat()

        check_order(
            multi_order,
            order_response['data'],
            {
                'price': round(sum([
                    item['price'] * item['amount']
                    for item in order_data['items']
                ]), 2),  # type: ignore
                'currency': 'RUB',
                'caption': order_data['caption'],
                'items': contains_inanyorder(*[
                    has_entries({
                        'name': item['name'],
                        'amount': item['amount'],
                        'price': item['price'],
                        'nds': item['nds'],
                        'currency': item['currency'],
                        'product_id': is_(int),
                    })
                    for item in order_data['items']
                ]),
            },
        )


class TestCreateOrderFromMulti(BaseTestMerchantRoles, BaseTestCreateOrderFromMulti):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    @pytest.fixture
    def id_type(self):
        return 'uid'


class TestCreateOrderInternalFromMulti(BaseTestCreateOrderFromMulti):
    @pytest.fixture
    def id_type(self):
        return 'service_merchant'


@pytest.mark.usefixtures('moderation')
class TestBadOrderCreate(BaseTestOrder):
    @pytest.fixture
    def test_data(self, request, bad_service_merchant_id):
        return f'/v1/internal/order/{bad_service_merchant_id}/'

    @pytest.mark.asyncio
    async def test_bad_merchant_uid(self, client, test_data, order_data):
        r = await client.post(test_data, json=order_data)
        assert_that(
            await r.json(),
            has_entries({
                'code': 403,
                'data': has_items('message'),
                'status': 'fail',
            })
        )
