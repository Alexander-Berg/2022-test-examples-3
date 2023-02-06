import pytest

from mail.payments.payments.core.actions.shop.get_or_ensure_default import GetOrEnsureDefaultShopAction
from mail.payments.payments.core.entities.enums import ShopType


class TestGetOrEnsureDefaultShopAction:
    @pytest.fixture(params=ShopType)
    def shop_type(self, request):
        return request.param

    @pytest.mark.parametrize('shop_is_default', [False])
    @pytest.mark.asyncio
    async def test_creates_default_shop(self, storage, merchant, shop_is_default, shop_type):
        shop = await GetOrEnsureDefaultShopAction(uid=merchant.uid, default_shop_type=shop_type).run()
        shop_fetched = await storage.shop.get_default_for_merchant(uid=merchant.uid, shop_type=shop_type)
        assert shop == shop_fetched

    @pytest.mark.parametrize('shop_is_default', [True])
    @pytest.mark.parametrize('shop_type', list(ShopType))
    @pytest.mark.asyncio
    async def test_gets_default_shop(self, shop, shop_type):
        shop = await GetOrEnsureDefaultShopAction(uid=shop.uid, default_shop_type=shop_type).run()
        assert shop == shop

    @pytest.mark.asyncio
    async def test_gets_shop_by_shop_id(self, shop):
        shop_returned = await GetOrEnsureDefaultShopAction(uid=shop.uid, shop_id=shop.shop_id).run()
        assert shop == shop_returned
