import pytest

from mail.payments.payments.tests.base import BaseSdkOrderTest, BaseTaskTest
from mail.payments.payments.tests.base import BaseTestClearUnholdOrder as BTestClearUnholdOrder


@pytest.fixture
def params(order, items, operation, validated_items):
    return {
        'uid': order.uid,
        'order_id': order.order_id,
        'operation': operation,
        'items': validated_items
    }


@pytest.fixture
def action_name():
    return 'core_clear_unhold_order_action'


class TestClearUnhold(BTestClearUnholdOrder, BaseSdkOrderTest, BaseTaskTest):
    @pytest.fixture
    async def response(self, sdk_client, order, operation, tvm):
        r = await sdk_client.post(f'/v1/order/{order.order_id}/{operation}')
        return r


class TestClearUnholdResize(BTestClearUnholdOrder, BaseSdkOrderTest, BaseTaskTest):
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
    async def response(self, validated_items, sdk_client, order, operation, tvm):
        r = await sdk_client.post(f'/v1/order/{order.order_id}/{operation}', json=validated_items)
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
        async def test_invalid_data(self, validated_items, sdk_client, order, operation, tvm):
            r = await sdk_client.post(f'/v1/order/{order.order_id}/{operation}', json=validated_items)
            assert r.status == 400
