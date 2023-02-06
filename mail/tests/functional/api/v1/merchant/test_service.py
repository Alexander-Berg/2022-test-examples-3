from datetime import timezone
from itertools import chain

import pytest

from hamcrest import assert_that, empty, greater_than_or_equal_to, has_entries

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.tests.base import BaseTestMerchantRoles


class TestGetMerchantService(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    def expected(self, service, service_merchant):
        return {
            'service_merchant_id': service_merchant.service_merchant_id,
            'uid': service_merchant.uid,
            'service_id': service_merchant.service_id,
            'entity_id': service_merchant.entity_id,
            'description': service_merchant.description,
            'enabled': service_merchant.enabled,
            'created': service_merchant.created.astimezone(timezone.utc).isoformat(),
            'deleted': service_merchant.deleted,
            'revision': greater_than_or_equal_to(service_merchant.revision),
            'service': {
                'service_id': service.service_id,
                'name': service.name,
            },
        }

    @pytest.fixture
    def response_func(self, client, service_merchant, tvm):
        async def _inner(status=200):
            r = await client.get(f'/v1/merchant/{service_merchant.uid}/service/{service_merchant.service_merchant_id}')
            assert r.status == status
            return await r.json()

        return _inner

    @pytest.mark.asyncio
    async def test_success(self, response_func, expected):
        response = await response_func()
        assert_that(
            response['data'],
            has_entries(expected)
        )

    @pytest.mark.asyncio
    async def test_not_found(self, response_func, service_merchant):
        service_merchant.service_merchant_id = service_merchant.service_merchant_id * 10
        response = await response_func(404)
        assert response['data']['message'] == 'SERVICE_MERCHANT_NOT_FOUND'


class TestUpdateMerchantService(TestGetMerchantService):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
    )

    @pytest.fixture
    def description(self):
        return 'updated test descr'

    @pytest.fixture
    def enabled(self):
        return True

    @pytest.fixture
    def request_json(self, merchant, expected, description, enabled):
        expected['description'] = description
        expected['enabled'] = enabled
        return {
            'description': description,
            'enabled': enabled,
        }

    @pytest.fixture
    def response_func(self, client, service_merchant, request_json, tvm):
        async def _inner(status=200):
            r = await client.post(f'/v1/merchant/{service_merchant.uid}/service/{service_merchant.service_merchant_id}',
                                  json=request_json)
            assert r.status == status
            return await r.json()

        return _inner


class TestDeleteMerchantService(TestGetMerchantService):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
    )

    @pytest.fixture
    def response_func(self, client, service_merchant, tvm):
        async def _inner(status=200):
            r = await \
                client.delete(f'/v1/merchant/{service_merchant.uid}/service/{service_merchant.service_merchant_id}')
            assert r.status == status
            return await r.json()

        return _inner

    @pytest.mark.asyncio
    async def test_success(self, response_func):
        response = await response_func()
        assert_that(response['data'], empty())


class TestGetMerchantServiceList(TestGetMerchantService):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
        MerchantRole.VIEWER,
    )

    @pytest.fixture
    def response_func(self, client, service_merchant, tvm):
        async def _inner(status=200):
            r = await client.get(f'/v1/merchant/{service_merchant.uid}/service')
            assert r.status == status
            return await r.json()

        return _inner

    @pytest.mark.asyncio
    async def test_success(self, response_func, expected):
        response = await response_func()
        assert_that(
            response['data'][0],
            has_entries(expected)
        )

    @pytest.mark.parametrize('role', chain((None,), MerchantRole))
    @pytest.mark.asyncio
    async def test_not_found(self, response_func, service_merchant):
        service_merchant.uid = service_merchant.uid * 10
        await response_func(403)
