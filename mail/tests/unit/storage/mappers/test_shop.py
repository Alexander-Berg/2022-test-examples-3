import datetime
from dataclasses import asdict

import pytest
from psycopg2.errors import IntegrityError

from sendr_utils import alist, utcnow

from hamcrest import assert_that, contains, has_properties, instance_of

from mail.payments.payments.core.entities.enums import ShopType
from mail.payments.payments.storage.exceptions import ShopNotFound
from mail.payments.payments.storage.mappers.shop import ShopDataDumper, ShopDataMapper


@pytest.fixture
async def merchant(create_merchant):
    return await create_merchant(create_shops=False)


@pytest.fixture
def shop_dict(shop):
    result = {
        attr: getattr(shop, attr)
        for attr in [
            'uid',
            'name',
            'is_default',
            'shop_type',
            'shop_id',
            'created',
            'updated',
        ]
    }
    result['settings'] = asdict(shop.settings)
    return result


class TestShopDataMapper:
    def test_mapper(self, shop, shop_dict):
        row = {
            type(shop).__name__ + '__' + key: value
            for key, value in shop_dict.items()
        }
        mapped = ShopDataMapper()(row)
        assert mapped == shop


class TestDataDumper:
    def test_dump(self, shop, shop_dict):
        assert ShopDataDumper()(shop) == shop_dict


class TestShopMapper:
    @pytest.fixture
    def now(self):
        return utcnow()

    @pytest.fixture(autouse=True)
    def now_mock(self, now, mocker):
        now_mock = mocker.Mock(return_value=now)
        mocker.patch('sqlalchemy.func.now', now_mock)
        return now_mock

    @pytest.mark.asyncio
    @pytest.mark.parametrize('shop_is_default', (True,))
    @pytest.mark.parametrize('with_merchant_oauth', (True, False))
    async def test_get(self, storage, shop, merchant_oauth, with_merchant_oauth):
        if with_merchant_oauth:
            shop.oauth = merchant_oauth
        result = await storage.shop.get(uid=shop.uid, shop_id=shop.shop_id, with_merchant_oauth=with_merchant_oauth)
        assert result == shop

    @pytest.mark.asyncio
    @pytest.mark.parametrize('shop_is_default', (True,))
    @pytest.mark.parametrize('with_merchant_oauth', (True, False))
    async def test_find(self, storage, shop, merchant_oauth, with_merchant_oauth):
        if with_merchant_oauth:
            shop.oauth = merchant_oauth

        assert_that(
            await alist(storage.shop.find(with_merchant_oauth=with_merchant_oauth)),
            contains(shop)
        )

    @pytest.mark.asyncio
    async def test_create(self, storage, shop_entity, now):
        assert shop_entity.shop_id is None
        shop_created = await storage.shop.create(shop_entity)
        assert_that(shop_created, has_properties({
            'uid': shop_entity.uid,
            'shop_id': instance_of(int),
            'shop_type': shop_entity.shop_type,
            'name': shop_entity.name,
            'is_default': shop_entity.is_default,
            'settings': shop_entity.settings,
            'updated': now,
            'created': now,
        }))

    @pytest.mark.parametrize('shop_type', ShopType)
    @pytest.mark.parametrize('shop_is_default', [True])
    @pytest.mark.asyncio
    async def test_get_or_create_default_shop_gets(self, merchant, shop, storage):
        shop_fetched, created = await storage.shop.get_or_create(
            shop, lookup_fields=('uid', 'shop_type', 'is_default'),
        )
        assert shop == shop_fetched and not created

    @pytest.mark.parametrize('shop_type', ShopType)
    @pytest.mark.parametrize('shop_is_default', [True])
    @pytest.mark.asyncio
    async def test_get_or_create_default_shop_creates(self, merchant, shop_entity, storage):
        shop, is_created = await storage.shop.get_or_create(
            shop_entity,
            lookup_fields=('uid', 'shop_type', 'is_default'),
        )
        assert shop.is_default and is_created

    @pytest.mark.parametrize('shop_type', ShopType)
    @pytest.mark.parametrize('shop_is_default', [True])
    @pytest.mark.asyncio
    async def test_default_shop_for_merchant_is_unique(self, storage, shop_entity):
        await storage.shop.create(shop_entity)
        with pytest.raises(IntegrityError):
            await storage.shop.create(shop_entity)

    @pytest.mark.parametrize('shop_type', ShopType)
    @pytest.mark.parametrize('shop_is_default', [False])
    @pytest.mark.asyncio
    async def test_non_default_shop_for_merchant_is_not_unique(self, storage, shop_entity):
        shop_a = await storage.shop.create(shop_entity)
        shop_b = await storage.shop.create(shop_entity)
        assert shop_a.shop_id != shop_b.shop_id

    @pytest.mark.parametrize(('field', 'new_value'), (
        ('name', 'Новое название'),
        ('shop_type', ShopType.TEST),
    ))
    @pytest.mark.asyncio
    async def test_update_updates_fields(self, storage, shop, field, new_value):
        assert getattr(shop, field) != new_value
        setattr(shop, field, new_value)
        shop_updated = await storage.shop.save(shop)
        assert getattr(shop_updated, field) == new_value

    @pytest.mark.asyncio
    async def test_update_updates_updated(self, storage, shop, now, now_mock):
        new_now = now - datetime.timedelta(minutes=1)
        now_mock.return_value = new_now
        shop_updated = await storage.shop.save(shop)
        assert shop_updated.updated.astimezone(datetime.timezone.utc) == new_now.astimezone(datetime.timezone.utc)

    @pytest.mark.asyncio
    async def test_delete(self, storage, shop):
        await storage.shop.delete(shop)
        with pytest.raises(ShopNotFound):
            await storage.shop.get(uid=shop.uid, shop_id=shop.shop_id)
