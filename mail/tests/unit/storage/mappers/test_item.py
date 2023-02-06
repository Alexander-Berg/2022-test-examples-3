from copy import copy
from decimal import Decimal

import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, has_properties

from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.storage.exceptions import ItemNotFound


@pytest.fixture
def product(products):
    return products[0]


@pytest.fixture
def item_entity(storage, order, product):
    return Item(
        uid=order.uid,
        order_id=order.order_id,
        product_id=product.product_id,
        amount=Decimal('3.33'),
    )


@pytest.fixture
async def item(storage, item_entity):
    return await storage.item.create(item_entity)


@pytest.fixture
async def created_item(storage, item):
    return await storage.item.get(
        uid=item.uid,
        order_id=item.order_id,
        product_id=item.product_id,
    )


class TestCreate:
    def test_create_creates(self, item_entity, created_item):
        assert item_entity == created_item

    def test_create_returns(self, item_entity, item):
        assert item_entity == item


class TestCreateOrUpdate:
    @pytest.mark.asyncio
    async def test_create(self, storage, item_entity):
        with pytest.raises(ItemNotFound):
            await storage.item.get(item_entity.uid, item_entity.order_id, item_entity.product_id)
        returned = await storage.item.create_or_update(item_entity)
        assert returned == await storage.item.get(item_entity.uid, item_entity.order_id, item_entity.product_id)

    @pytest.mark.asyncio
    async def test_update(self, storage, created_item, image):
        created_item.amount += 1
        created_item.image_id = image.image_id
        await storage.item.create_or_update(created_item)
        assert created_item == await storage.item.get(created_item.uid, created_item.order_id, created_item.product_id)


class TestGet:
    @pytest.mark.asyncio
    async def test_get_not_found(self, storage, item):
        with pytest.raises(ItemNotFound):
            await storage.item.get(
                uid=item.uid,
                order_id=item.order_id,
                product_id=-1,
            )


class TestFindByImage:
    @pytest.fixture(autouse=True)
    async def relevant(self, create_item, image, order):
        return await create_item(image_id=image.image_id, order=order)

    @pytest.fixture(autouse=True)
    async def irrelevant(self, create_item, order):
        return await create_item(order)

    @pytest.mark.asyncio
    async def test_find_by_image(self, storage, relevant, image):
        assert_that(
            await alist(storage.item.find_by_image(
                uid=image.uid,
                image_id=image.image_id,
            )),
            contains_inanyorder(
                has_properties({
                    'order_id': relevant.order_id,
                    'uid': relevant.uid,
                    'product_id': relevant.product_id,
                })
            )
        )


class TestGetForOrder:
    @pytest.fixture
    async def returned(self, storage, order):
        return [
            item
            async for item in storage.item.get_for_order(order.uid, order.order_id)
        ]

    def test_get_for_order(self, items, returned):
        assert_that(returned, contains_inanyorder(*items))

    @pytest.mark.asyncio
    async def test_get_for_order_empty(self, returned):
        assert returned == []


class TestGetForOrders:
    @pytest.fixture
    async def extra_order(self, storage, order):
        entity = copy(order)
        entity.order_id = None
        return await storage.order.create(entity)

    @pytest.fixture
    async def extra_items(self, storage, items, extra_order):
        extra_order.items = []
        for item in items:
            extra_item = copy(item)
            extra_item.order_id = extra_order.order_id
            extra_item = await storage.item.create(extra_item)
            extra_item.product = item.product
            extra_item.image = item.image
            extra_order.items.append(extra_item)
        return extra_order.items

    @pytest.fixture
    def returned(self, storage, merchant):
        async def _inner(order_ids):
            return [
                item
                async for item in storage.item.get_for_orders(
                    [(merchant.uid, order_id) for order_id in order_ids]
                )
            ]

        return _inner

    @pytest.mark.asyncio
    async def test_get_for_no_orders(self, returned):
        assert await returned([]) == []

    @pytest.mark.asyncio
    async def test_get_for_two_orders_no_items(self, order, extra_order, returned):
        assert await returned([order.order_id, extra_order.order_id]) == []

    @pytest.mark.asyncio
    async def test_get_for_two_orders_with_items(self,
                                                 storage,
                                                 order, items,
                                                 extra_order, extra_items,
                                                 returned,
                                                 ):
        assert_that(
            await returned([order.order_id, extra_order.order_id]),
            contains_inanyorder(*items, *extra_items)
        )


class TestGetProductAmountInRefunds:
    @pytest.fixture
    def returned(self, storage, merchant):
        async def _inner(order_id):
            return [
                (product_id, amount)
                async for product_id, amount in storage.item.get_product_amount_in_refunds(
                    uid=merchant.uid,
                    order_id=order_id,
                )
            ]

        return _inner

    @pytest.mark.asyncio
    async def test_get_product_amount_in_refunds_for_order_without_refunds(self, order, returned):
        assert await returned(order.order_id) == []

    @pytest.mark.asyncio
    async def test_get_product_amount_in_refunds(self, existing_refunds, returned):
        assert await returned(existing_refunds[0].original_order_id) == [(1, Decimal('1.00')), (2, Decimal('2.06'))]


class TestDelete:
    @pytest.mark.asyncio
    async def test_delete(self, storage, item):
        await storage.item.get(item.uid, item.order_id, item.product_id)
        await storage.item.delete(item)
        with pytest.raises(ItemNotFound):
            await storage.item.get(item.uid, item.order_id, item.product_id)


class TestSave:
    @pytest.mark.asyncio
    async def test_save(self, item, storage):
        item.amount = Decimal("0.1")
        item.new_price = Decimal("42")
        item.image_url = 'http://image_url.test'
        saved_item = await storage.item.save(item)
        assert saved_item == item
