import pytest

from hamcrest import assert_that, has_properties

from mail.payments.payments.commands.create_default_shops import CreateDefaultShopForAllMerchantAction
from mail.payments.payments.core.entities.enums import ShopType
from mail.payments.payments.storage.exceptions import ShopNotFound


@pytest.fixture(autouse=True)
def shop_is_default():
    return False


@pytest.fixture
def run_action():
    async def run():
        await CreateDefaultShopForAllMerchantAction().run()

    return run


class TestCreatesDefaultShops:
    # TODO: PAYBACK-670 удалить тест
    @pytest.fixture
    async def merchant_without_shops(self, create_merchant):
        return await create_merchant(create_shops=False)

    @pytest.fixture
    def merchant(self, merchant_without_shops):
        return merchant_without_shops

    @pytest.fixture(autouse=True)
    async def ensure_no_default_shop_for_merchant(self, merchant, shop, storage):
        with pytest.raises(ShopNotFound):
            await storage.shop.get_default_for_merchant(uid=merchant.uid)

    @pytest.fixture(autouse=True)
    async def ensure_no_default_test_shop_for_merchant(self, merchant, shop, storage):
        with pytest.raises(ShopNotFound):
            await storage.shop.get_default_for_merchant(uid=merchant.uid, shop_type=ShopType.TEST)

    @pytest.mark.parametrize('expected_shop_type', [ShopType.PROD, ShopType.TEST])
    @pytest.mark.asyncio
    async def test_creates_default_shop(self, merchant, run_action, storage, expected_shop_type):
        await run_action()
        shop = await storage.shop.get_default_for_merchant(uid=merchant.uid, shop_type=expected_shop_type)
        assert_that(shop, has_properties(dict(is_default=True, shop_type=expected_shop_type)))
