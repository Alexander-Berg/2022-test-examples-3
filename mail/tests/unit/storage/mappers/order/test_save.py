from datetime import datetime, timezone

import pytest

from hamcrest import all_of, any_of, assert_that, greater_than, has_properties, has_property, is_not, less_than

from mail.payments.payments.core.entities.enums import OrderKind, OrderSource, PayStatus
from mail.payments.payments.storage.exceptions import OrderNotFound


class TestSave:
    @pytest.fixture
    def returned(self, storage):
        async def _inner(order):
            return await storage.order.save(order)

        return _inner

    @pytest.mark.asyncio
    async def test_updatable(self, order, returned):
        changes = {
            'active': False,
            'autoclear': False,
            'caption': 'test-updatable-caption',
            'closed': datetime(year=1990, month=1, day=1, tzinfo=timezone.utc),
            'description': 'test-updatable-description',
            'pay_status': PayStatus.REJECTED,
            'trust_refund_id': 'test-updatable-trust-refund-id',
            'user_description': 'test-updatable-user-description',
            'user_email': 'test-updatable-user-email',
            'verified': True,
            'service_client_id': None,
            'service_merchant_id': None,
            'customer_uid': 42,
            'return_url': 'test-return-url',
            'pay_by_source': OrderSource.UI if order.pay_by_source == OrderSource.SDK_API else OrderSource.SDK_API
        }
        for key, value in changes.items():
            setattr(order, key, value)

        before_update_time = datetime.now(tz=timezone.utc)
        returned_order = await returned(order)
        after_update_time = datetime.now(tz=timezone.utc)

        changes['updated'] = all_of(greater_than(before_update_time), less_than(after_update_time))

        assert_that(returned_order, has_properties(changes))

    @pytest.mark.asyncio
    async def test_not_updatable(self, order, returned):
        created_by_source = OrderSource.UI if order.created_by_source == OrderSource.SDK_API else OrderSource.SDK_API
        changes = {
            'revision': order.revision + 100,
            'kind': OrderKind.REFUND,
            'created': datetime(year=1990, month=1, day=1, tzinfo=timezone.utc),
            'updated': datetime(year=1990, month=1, day=1, tzinfo=timezone.utc),
            'order_hash': 'test-not-updatable-order-hash',
            'order_url': 'test-not-updatable-order-url',
            'payment_hash': 'test-not-updatable-payment-hash',
            'payment_url': 'test-not-updatable-payment-url',
            'items': [1, 2, 3],
            'created_by_source': created_by_source,
        }
        for key, value in changes.items():
            setattr(order, key, value)
        assert_that(
            await returned(order),
            is_not(any_of(*[
                has_property(key, value)
                for key, value in changes.items()
            ]))
        )

    @pytest.mark.parametrize('id_key', ['uid', 'order_id'])
    @pytest.mark.asyncio
    async def test_not_found(self, order, returned, id_key):
        setattr(order, id_key, getattr(order, id_key) + 1)
        with pytest.raises(OrderNotFound):
            await returned(order)
