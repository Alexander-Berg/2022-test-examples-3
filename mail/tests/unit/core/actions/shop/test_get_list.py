import pytest

from hamcrest import assert_that, contains_inanyorder

from mail.payments.payments.core.actions.shop.get_list import CoreGetShopListAction, GetShopListAction
from mail.payments.payments.core.entities.merchant_oauth import MerchantOAuth


class TestCoreGetShopListAction:
    @pytest.fixture
    def limit(self):
        """
        В этом тесте три магазина: два дефолтных (тест и прод) и один не основной (прод)
        """
        return 3

    @pytest.fixture
    def offset(self):
        return 0

    @pytest.fixture(params=(True, False))
    def with_merchant_oauth(self, request):
        return request.param

    @pytest.fixture
    def params(self, offset, limit, with_merchant_oauth, shop):
        return {
            'uid': shop.uid,
            'limit': limit,
            'offset': offset,
            'with_merchant_oauth': with_merchant_oauth,
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await CoreGetShopListAction(**params).run()

        return _inner

    @pytest.mark.asyncio
    async def test_returned(self, storage, merchant_oauth, with_merchant_oauth, shop, default_merchant_shops, returned):
        shops = [*default_merchant_shops.values(), shop]

        if with_merchant_oauth:
            for i in range(len(shops)):
                try:
                    shops[i].oauth = await storage.merchant_oauth.get_by_shop_id(shops[i].uid, shops[i].shop_id)
                except MerchantOAuth.DoesNotExist:
                    shops[i].oauth = None

        assert_that(returned, contains_inanyorder(*shops))

    @pytest.mark.parametrize(['limit', 'offset'], ((0, 0), (0, 1), (1, 3)))
    def test_limit_offset(self, shop, returned):
        assert returned == []


class TestGetShopListAction:
    @pytest.fixture
    def result(self, rands):
        return rands()

    @pytest.fixture(autouse=True)
    def core_action_mock(self, mock_action, result):
        return mock_action(CoreGetShopListAction, result)

    @pytest.fixture
    def params(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await GetShopListAction(**params).run()

        return _inner

    def test_call(self, params, core_action_mock, returned):
        core_action_mock.assert_called_once_with(**params)

    def test_result(self, result, returned):
        assert returned == result
