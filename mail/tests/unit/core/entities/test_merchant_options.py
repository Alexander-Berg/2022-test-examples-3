import pytest

from mail.payments.payments.core.entities.enums import OrderSource


class TestIsCreatingOrderAllowed:
    @pytest.fixture(params=[None, [], [OrderSource.SDK_API]])
    def allowed_order_sources(self, request, merchant_options):
        merchant_options.allowed_order_sources = request.param
        return request.param

    @pytest.mark.parametrize('source', [OrderSource.SDK_API, OrderSource.UI])
    def test_is_creating_order_allowed(self, allowed_order_sources, merchant_options, source):
        assert merchant_options.is_creating_order_allowed(source) == (
            allowed_order_sources is None
            or source in allowed_order_sources
        )
