from datetime import timezone

import pytest

from hamcrest import assert_that, greater_than, has_entries

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.tests.base import BaseTestMerchantRoles
from mail.payments.payments.tests.utils import check_order

from .base import BaseTestOrder


@pytest.mark.usefixtures('moderation')
class BaseTestActiveOrder(BaseTestOrder):
    @pytest.fixture(params=(True, False))
    def active(self, request):
        return request.param

    @pytest.fixture
    async def service_merchant_order_test_data(self, storage, active, service_merchant, service_merchant_order):
        suffix = 'activate' if active else 'deactivate'
        service_merchant_id = service_merchant.service_merchant_id
        order_id = service_merchant_order.order_id

        service_merchant_order.active = not active
        await storage.order.save(service_merchant_order)

        return {
            'path': f'/v1/internal/order/{service_merchant_id}/{order_id}/{suffix}',
            'order': service_merchant_order
        }

    @pytest.fixture
    async def uid_order_test_data(self, active, storage, order):
        suffix = 'activate' if active else 'deactivate'

        order.active = not active
        await storage.order.save(order)

        return {
            'path': f'/v1/order/{order.uid}/{order.order_id}/{suffix}',
            'order': order
        }

    @pytest.fixture
    def response_func(self, client, order, test_data, tvm):
        path = test_data['path']

        async def _inner(path=path):
            return await client.post(path)

        return _inner

    @pytest.fixture
    async def order_response(self, response_func):
        r = await response_func()
        assert r.status == 200
        return await r.json()

    def test_order(self, test_data, active, order_response):
        order = test_data['order']
        check_order(
            order,
            order_response['data'],
            {
                'active': active,
                'revision': greater_than(order.revision),
                'updated': greater_than(order.updated.astimezone(timezone.utc).isoformat()),
            }
        )


class TestActiveOrder(BaseTestMerchantRoles, BaseTestActiveOrder):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    @pytest.fixture
    def id_type(self):
        return 'uid'


class TestActiveOrderInternal(BaseTestActiveOrder):
    @pytest.fixture
    def id_type(self):
        return 'service_merchant'


@pytest.mark.usefixtures('moderation')
class BaseTestUpdateOrder(BaseTestOrder):
    @pytest.fixture
    def response_func(self, client, order, test_data, tvm):
        path = test_data['path']
        json_data = {
            'active': False,
            'pay_status': 'paid',
            'refund_status': 'requested',
        }

        async def _inner(path=path, json_data=json_data):
            return await client.put(path, json=json_data)

        return _inner

    @pytest.fixture
    async def order_response(self, response_func):
        r = await response_func()
        assert r.status == 200
        return await r.json()

    def test_order(self, test_data, order_response):
        order = test_data['order']
        check_order(
            order,
            order_response['data'],
            {
                'active': False,
                'revision': greater_than(order.revision),
                'updated': greater_than(order.updated.astimezone(timezone.utc).isoformat()),
            }
        )


class TestUpdateOrder(BaseTestMerchantRoles, BaseTestUpdateOrder):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    @pytest.fixture
    def id_type(self):
        return 'uid'

    @pytest.mark.asyncio
    async def test_order_not_found(self, merchant, bad_order_id, response_func):
        r = await response_func(path=f'/v1/order/{merchant.uid}/{bad_order_id}')
        r_data = await r.json()
        assert r.status == 404 and r_data['data']['message'] == 'ORDER_NOT_FOUND'


class TestUpdateOrderInternal(BaseTestUpdateOrder):
    @pytest.fixture
    def id_type(self):
        return 'service_merchant'


@pytest.mark.usefixtures('moderation')
class TestUpdateBadOrder(BaseTestOrder):
    @pytest.fixture(params=('bad_service_merchant', 'bad_service_merchant_order_id'))
    def test_data(self, request, order, bad_merchant_uid, bad_order_id, bad_service_merchant_id, merchant,
                  service_merchant):
        data = {
            'bad_service_merchant': [
                f'/v1/internal/order/{bad_service_merchant_id}/{order.order_id}',
                'Action access denied',
                403
            ],
            'bad_service_merchant_order_id': [
                f'/v1/internal/order/{service_merchant.service_merchant_id}/{bad_order_id}',
                'ORDER_NOT_FOUND',
                404
            ]
        }
        return data[request.param]

    @pytest.mark.asyncio
    async def test_order_not_found(self, client, test_data, tvm):
        r = await client.put(test_data[0], json={
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
            'mode': 'prod',
        })

        assert_that(
            await r.json(),
            has_entries({
                'code': test_data[2],
                'data': has_entries({
                    'message': test_data[1],
                }),
                'status': 'fail',
            }),
        )

    @pytest.mark.asyncio
    async def test_service_merchant_not_enabled(self, client, service_merchant, storage):
        service_merchant.enabled = False
        service_merchant = await storage.service_merchant.save(service_merchant)
        r = await client.get(f'/v1/internal/order/{service_merchant.service_merchant_id}/')
        assert_that(
            await r.json(),
            has_entries({
                'code': 403,
                'data': has_entries({
                    'message': 'SERVICE_MERCHANT_NOT_ENABLED',
                }),
                'status': 'fail',
            }),
        )
