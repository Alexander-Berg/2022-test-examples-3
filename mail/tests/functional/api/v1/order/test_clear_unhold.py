import pytest

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.tests.base import BaseTaskTest, BaseTestClearUnholdOrder, BaseTestMerchantRoles

from .base import BaseTestOrder


class TestClearUnholdOrder(BaseTestMerchantRoles, BaseTestClearUnholdOrder, BaseTestOrder):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    @pytest.fixture
    def action_name(self):
        return 'clear_by_ids_order_action'

    @pytest.fixture
    async def params(self, order, items, storage, operation, validated_items):
        transaction = await storage.transaction.get_last_by_order(
            uid=order.uid,
            order_id=order.order_id,
            raise_=False,
        )
        return {
            'uid': order.uid,
            'order_id': order.order_id,
            'transaction_id': transaction.tx_id,
        }

    @pytest.fixture
    async def response(self, client, order, operation, tvm):
        r = await client.post(f'/v1/order/{order.uid}/{order.order_id}/{operation}')
        return r


class TestClearUnholdOrderInternal(BaseTestClearUnholdOrder, BaseTestOrder, BaseTaskTest):
    @pytest.fixture
    def action_name(self):
        return 'core_clear_unhold_order_action'

    @pytest.fixture
    def params(self, order, items, operation, validated_items):
        return {
            'order_id': order.order_id,
            'operation': operation,
            'items': validated_items,
            'uid': order.uid
        }

    @pytest.fixture
    async def response(self, client, service_merchant, order, operation, tvm):
        r = await client.post(
            f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/{operation}')
        return r


class TestResize(BaseTestClearUnholdOrder, BaseTestOrder, BaseTaskTest):
    @pytest.fixture
    def action_name(self):
        return 'core_clear_unhold_order_action'

    @pytest.fixture
    async def validated_items(self, storage, items, order):
        return [
            {
                "product_id": item.product_id,
                "amount": str(item.amount),
                "price": str(item.price)
            }
            async for item in storage.item.get_for_order(uid=order.uid, order_id=order.order_id)
        ]

    @pytest.fixture
    def params(self, order, items, operation, validated_items):
        return {
            'order_id': order.order_id,
            'operation': operation,
            'items': validated_items,
            'uid': order.uid
        }

    @pytest.fixture
    async def response(self, client, service_merchant, order, operation, tvm, validated_items):
        r = await client.post(
            f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/{operation}',
            json=validated_items)
        return r

    class TestInvalidData:
        @pytest.fixture
        def operation(self):
            return 'clear'

        @pytest.fixture
        async def validated_items(self, storage, randn, items, order):
            return [
                {
                    "product_id": randn(),
                    "amount": str(item.amount),
                    "price": str(item.price)
                }
                async for item in storage.item.get_for_order(uid=order.uid, order_id=order.order_id)
            ]

        @pytest.mark.asyncio
        async def test_invalid_data(self, client, service_merchant, order, operation, tvm, validated_items):
            r = await client.post(
                f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/{operation}',
                json=validated_items)
            assert r.status == 400
