from datetime import timezone

import pytest

from hamcrest import assert_that, contains, greater_than, has_entries

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.tests.base import BaseTestMerchantRoles


@pytest.mark.usefixtures('moderation', 'balance_person_mock')
class TestPayOfflineOrder(BaseTestMerchantRoles):
    ALLOWED_ROLES = (
        MerchantRole.OWNER,
        MerchantRole.ADMIN,
        MerchantRole.OPERATOR,
    )

    @pytest.fixture
    async def returned(self, client, order, service_merchant, tvm):
        return await client.post(f'/v1/order/{order.uid}/{order.order_id}/pay_offline')

    @pytest.fixture
    async def returned_json(self, returned):
        return await returned.json()

    def test_pay_offline(self, order, returned, returned_json):
        assert_that(
            (returned.status, returned_json['data']),
            contains(200, has_entries({
                'pay_method': 'offline',
                'pay_status': 'paid',
                'revision': greater_than(order.revision),
                'updated': greater_than(order.updated.astimezone(timezone.utc).isoformat()),
            }))
        )
