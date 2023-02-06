import pytest

from mail.payments.payments.core.entities.enums import MerchantRole, PayStatus
from mail.payments.payments.tests.base import BaseTestMerchantRoles


class TestCancelOrder(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    @pytest.fixture
    async def response(self, client, order, tvm):
        r = await client.post(f'/v1/order/{order.uid}/{order.order_id}/cancel')
        assert r.status == 200
        return r

    async def test_response(self, response, response_data):
        assert response_data == {}

    async def test_order_is_cancelled(self, storage, order, response):
        order = await storage.order.get(uid=order.uid, order_id=order.order_id)
        assert all((
            order.pay_status == PayStatus.CANCELLED,
            order.closed is not None
        ))
