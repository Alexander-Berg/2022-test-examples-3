import pytest

from mail.payments.payments.core.actions.shop.create_or_update import (
    CoreCreateOrUpdateShopAction, CreateOrUpdateShopAction
)
from mail.payments.payments.core.entities.enums import ShopType
from mail.payments.payments.core.exceptions import ShopDefaultMustBePresent, ShopNotFoundError


class TestCoreCreateOrUpdateShopAction:
    @pytest.fixture
    def shop_type(self):
        return ShopType.PROD

    @pytest.fixture
    def is_default(self):
        return True

    @pytest.fixture
    def shop_id(self):
        return None

    @pytest.fixture
    def params(self, shop_id, shop_type, rands, is_default, merchant):
        return {
            'is_default': is_default,
            'shop_type': shop_type,
            'name': rands(),
            'shop_id': shop_id,
            'uid': merchant.uid,
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await CoreCreateOrUpdateShopAction(**params).run()

        return _inner

    @pytest.mark.asyncio
    async def test_returned(self, returned, storage):
        assert returned == await storage.shop.get(returned.uid, returned.shop_id)

    @pytest.mark.asyncio
    @pytest.mark.parametrize('shop_id', [-1])
    async def test_shop_not_found(self, returned_func):
        with pytest.raises(ShopNotFoundError):
            await returned_func()

    class TestForceDefault:
        """
        Проверяем, что если у мерчанта нету дефолтного шопа, то при попытке
        создания шопа этот шоп будет создан как дефолтный.
        На практике таких мерчантов быть не должно.
        """
        @pytest.fixture
        async def merchant_without_shops(self, create_merchant):
            return await create_merchant(create_shops=False)

        @pytest.fixture
        def merchant(self, merchant_without_shops):
            return merchant_without_shops

        @pytest.mark.parametrize('is_default', [False])
        def test_force_default(self, returned, storage):
            assert returned.is_default

    @pytest.mark.asyncio
    @pytest.mark.parametrize('shop_is_default', [True])
    async def test_reassign_default(self, shop, returned, storage):
        prev_default_shop = await storage.shop.get(shop.uid, shop.shop_id)
        assert returned.is_default and shop.is_default and not prev_default_shop.is_default

    class TestUpdate:
        @pytest.fixture
        def shop_id(self, shop):
            return shop.shop_id

        @pytest.mark.asyncio
        @pytest.mark.parametrize(['shop_is_default', 'is_default'], [(True, False)])
        async def test_default_must_be_present(self, returned_func):
            with pytest.raises(ShopDefaultMustBePresent):
                await returned_func()

        @pytest.mark.asyncio
        async def test_update__returned(self, shop, params, returned, storage):
            assert all((
                returned.name == params['name'],
                returned.is_default == params['is_default'],
                returned.shop_type == shop.shop_type,
            ))


class TestCreateOrUpdateShopAction:
    @pytest.fixture
    def result(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def core_action_mock(self, mock_action, result):
        return mock_action(CoreCreateOrUpdateShopAction, result)

    @pytest.fixture
    def params(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await CreateOrUpdateShopAction(**params).run()

        return _inner

    def test_call(self, params, core_action_mock, returned):
        core_action_mock.assert_called_once_with(**params)

    def test_result(self, result, returned):
        assert returned == result
