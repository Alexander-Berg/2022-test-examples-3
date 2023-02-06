import pytest

from hamcrest import assert_that, equal_to, has_properties

from mail.payments.payments.core.actions.get_oauth import GetMerchantOAuth
from mail.payments.payments.core.entities.enums import AcquirerType, MerchantOAuthMode, ShopType
from mail.payments.payments.core.exceptions import OAuthAbsentError


@pytest.fixture
@pytest.mark.usefixtures('merchant')
def shop(default_merchant_shops):
    return default_merchant_shops[ShopType.PROD]


@pytest.fixture
def returned_func(merchant, shop, acquirer):
    async def _returned_func(**kwargs):
        kwargs.setdefault('shop', shop)
        kwargs.setdefault('merchant', merchant)
        kwargs.setdefault('acquirer', acquirer)
        return await GetMerchantOAuth(**kwargs).run()

    return _returned_func


@pytest.mark.parametrize('acquirer', (AcquirerType.TINKOFF,))
@pytest.mark.asyncio
async def test_tinkoff__returns_none(storage, returned_func):
    assert_that(
        await returned_func(),
        equal_to(None),
    )


class TestKassa:
    @pytest.fixture
    def acquirer(self):
        return AcquirerType.KASSA

    @pytest.mark.asyncio
    async def test_returns_oauth(self, merchant, returned_func, payments_settings):
        assert merchant.oauth[0].shop_id is not None

        assert_that(
            await returned_func(),
            equal_to(merchant.oauth[0]),
        )

    @pytest.mark.asyncio
    async def test_inherits_acquirer_from_merchant(self, merchant, returned_func, payments_settings):
        assert merchant.oauth[0].shop_id is not None

        assert_that(
            await returned_func(acquirer=None),
            equal_to(merchant.oauth[0]),
        )

    @pytest.mark.asyncio
    async def test_no_oauth__raises(self, merchant, storage, returned_func, payments_settings):
        assert merchant.oauth[0].shop_id is not None
        await storage.merchant_oauth.delete(merchant.oauth[0])

        with pytest.raises(OAuthAbsentError):
            await returned_func()

    @pytest.mark.asyncio
    async def test_returns_for_test_shop(self,
                                         merchant,
                                         storage,
                                         returned_func,
                                         default_merchant_shops,
                                         create_merchant_oauth
                                         ):
        test_shop = default_merchant_shops[ShopType.TEST]
        test_oauth = await create_merchant_oauth(
            uid=merchant.uid,
            shop_id=test_shop.shop_id,
            mode=MerchantOAuthMode.TEST,
        )
        merchant.oauth.append(test_oauth)

        assert_that(
            await returned_func(shop=test_shop),
            has_properties({
                'uid': test_oauth.uid,
                'mode': test_oauth.mode,
                'shop_id': test_shop.shop_id,
            }),
        )
