from datetime import timezone

import pytest

from hamcrest import assert_that, contains, greater_than, has_entries

from mail.payments.payments.tests.base import BaseSdkOrderTest


@pytest.mark.usefixtures('moderation', 'balance_person_mock')
class TestPayOfflineOrder(BaseSdkOrderTest):
    @pytest.fixture
    def returned_func(self, sdk_client, order):
        async def _inner():
            return await sdk_client.post(f'/v1/order/{order.order_id}/pay_offline')

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

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
