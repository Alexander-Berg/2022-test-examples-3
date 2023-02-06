import pytest

from mail.payments.payments.core.actions.init_products import InitProductsAction
from mail.payments.payments.core.entities.enums import NDS
from mail.payments.payments.tests.utils import dummy_async_function


class TestInitProducts:
    @pytest.fixture(autouse=True)
    def product_create_calls(self, mocker):
        calls = []
        mocker.patch(
            'mail.payments.payments.interactions.trust.TrustProductionClient.product_create',
            dummy_async_function(calls=calls),
        )
        return calls

    @pytest.fixture
    def client_id(self, rands):
        return rands()

    @pytest.fixture(params=(True, False))
    def service_fee(self, request, randn):
        return randn() if request.param else None

    @pytest.fixture(params=['uid', 'merchant'])
    def params_key(self, request):
        return request.param

    @pytest.fixture
    def params(self, base_merchant_action_data_mock, merchant, service_fee, params_key):
        data = {
            'uid': merchant.uid,
            'merchant': merchant,
        }
        return {params_key: data[params_key], 'service_fee': service_fee}

    @pytest.fixture
    async def returned(self, params):
        return await InitProductsAction(**params).run()

    def test_create_calls(self, merchant, client_id, product_create_calls, service_fee, returned):
        assert [call[1] for call in product_create_calls] == [
            {
                'uid': merchant.uid,
                'acquirer': merchant.acquirer,
                'partner_id': client_id,
                'nds': nds.value,
                'inn': merchant.organization.inn,
                'service_fee': service_fee,
            }
            for nds in NDS
        ]
