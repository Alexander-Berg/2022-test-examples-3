from copy import copy
from datetime import datetime
from decimal import Decimal

import pytest

from mail.payments.payments.core.entities.enums import NDS, ProductStatus
from mail.payments.payments.core.entities.product import Product
from mail.payments.payments.storage.exceptions import ProductNotFound


@pytest.fixture
def product_entity(merchant):
    return Product(
        uid=merchant.uid,
        name='test product',
        price=Decimal('11.22'),
        nds=NDS.NDS_0,
    )


@pytest.fixture
async def product(storage, product_entity):
    return await storage.product.create(product_entity)


@pytest.fixture
async def created_product(storage, product):
    return await storage.product.get(product.uid, product.product_id)


class TestCreate:
    def test_created_datetime(self, product):
        assert isinstance(product.created, datetime)

    def test_returned(self, product_entity, product):
        product_entity.created = product.created = None
        assert product_entity == product

    def test_created(self, product_entity, created_product):
        product_entity.created = created_product.created = None
        assert product_entity == created_product


class TestGet:
    @pytest.mark.asyncio
    async def test_not_found(self, storage, merchant):
        with pytest.raises(ProductNotFound):
            await storage.product.get(merchant.uid, -1)


class TestGetMany:
    @pytest.fixture
    def returned(self, storage, merchant):
        async def _inner(product_ids):
            return [
                product
                async for product in storage.product.get_many(merchant.uid, product_ids)
            ]

        return _inner

    @pytest.mark.asyncio
    async def test_get_none(self, returned):
        assert await returned([]) == []

    @pytest.mark.asyncio
    async def test_get_empty(self, returned):
        assert await returned([1, 2, 3]) == []

    @pytest.mark.asyncio
    async def test_get_one(self, products, returned):
        assert await returned([products[0].product_id]) == [products[0]]

    @pytest.mark.asyncio
    async def test_get_all(self, products, returned):
        assert await returned([p.product_id for p in products]) == products


class TestGetOrCreate:
    @pytest.mark.parametrize('attr,value', [
        ('name', 'new name'),
        ('nds', NDS.NDS_NONE),
        ('price', Decimal('33.44')),
        ('status', ProductStatus.INACTIVE),
    ])
    @pytest.mark.asyncio
    async def test_creates(self, storage, product, attr, value):
        new_product = copy(product)
        setattr(new_product, attr, value)
        product_created, _ = await storage.product.get_or_create(new_product)
        assert product_created.uid == product.uid and product_created.product_id != product.product_id

    @pytest.mark.asyncio
    async def test_gets(self, storage, product):
        got, _ = await storage.product.get_or_create(copy(product))
        assert got == product
