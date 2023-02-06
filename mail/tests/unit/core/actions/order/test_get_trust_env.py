import pytest

from mail.payments.payments.core.actions.order.get_trust_env import GetOrderTrustEnvAction
from mail.payments.payments.core.entities.enums import ShopType, TrustEnv
from mail.payments.payments.tests.base import parametrize_shop_type


class TestGetTrustEnv:
    @pytest.fixture
    def returned_func(self, order):
        async def _inner():
            return await GetOrderTrustEnvAction(order=order).run()

        return _inner

    @parametrize_shop_type
    def test_result(self, returned, shop_type):
        expected = {
            ShopType.PROD: TrustEnv.PROD,
            ShopType.TEST: TrustEnv.SANDBOX,
        }[shop_type]

        assert returned == expected
