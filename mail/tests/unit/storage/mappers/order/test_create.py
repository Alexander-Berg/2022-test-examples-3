from datetime import datetime

import pytest


class TestCreate:
    @pytest.fixture
    async def created_order(self, storage, order):
        return await storage.order.get(order.uid, order.order_id)

    def test_created_updated(self, order):
        assert isinstance(order.created, datetime) and isinstance(order.updated, datetime)

    def test_returned(self, order_entity, order):
        order_entity.shop = order.shop
        order_entity.service_merchant = order.service_merchant
        order_entity.created = order.created
        order_entity.updated = order.updated
        assert order_entity == order

    def test_created(self, order_entity, created_order):
        order_entity.shop = created_order.shop
        order_entity.service_merchant = created_order.service_merchant
        order_entity.created = created_order.created
        order_entity.updated = created_order.updated
        assert order_entity == created_order
