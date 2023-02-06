from datetime import timezone

import pytest

from hamcrest import assert_that, contains, greater_than, has_entries

from mail.payments.payments.core.entities.enums import PayStatus
from mail.payments.payments.tests.base import BaseSdkOrderTest


@pytest.mark.usefixtures('moderation')
class TestDeactivateOrder(BaseSdkOrderTest):
    @pytest.fixture
    def returned_func(self, sdk_client, order):
        async def _inner():
            return await sdk_client.post(f'/v1/order/{order.order_id}/deactivate', json={'active': False})

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.fixture
    async def returned_json(self, returned):
        return await returned.json()

    def test_deactivate(self, order, returned, returned_json):
        assert_that(
            (returned.status, returned_json['data']),
            contains(200, has_entries({
                'active': False,
                'revision': greater_than(order.revision),
                'updated': greater_than(order.updated.astimezone(timezone.utc).isoformat()),
            }))
        )

    def test_pay_token_in_response(self, crypto_mock, returned_json, merchant, payments_settings):
        data = returned_json['data']
        pay_token = data['pay_token'][len(payments_settings.PAY_TOKEN_PREFIX):]

        with crypto_mock.decrypt_payment(pay_token) as order:
            assert_that(order, has_entries({
                'uid': merchant.uid,
                'order_id': data['order_id'],
            }))

    @pytest.mark.parametrize('pay_status', PayStatus.ALREADY_PAID_STATUSES)
    @pytest.mark.asyncio
    async def test_fail_if_paid(self, order, pay_status, storage, returned_func):
        order.pay_status = pay_status
        await storage.order.save(order)
        r = await returned_func()
        data = (await r.json())['data']
        assert_that(
            (r.status, data),
            contains(400, has_entries({
                'message': 'ORDER_ALREADY_PAID'
            }))
        )
