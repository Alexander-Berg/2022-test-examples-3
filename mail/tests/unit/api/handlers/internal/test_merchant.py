from copy import deepcopy

import pytest

from mail.payments.payments.tests.utils import MERCHANT_DATA_TEST_CASES

from ..test_draft import TestMerchantDraftPost as BaseTestMerchantDraftPost


@pytest.mark.usefixtures('tvm_client_id')
class TestServiceMerchantDraftPost(BaseTestMerchantDraftPost):
    @pytest.fixture(autouse=True)
    def action(self, mock_action, service_merchant, merchant_draft):
        from mail.payments.payments.core.actions.merchant.draft import CreateServiceMerchantDraftAction
        return mock_action(CreateServiceMerchantDraftAction, (service_merchant, merchant_draft))

    @pytest.fixture
    def tvm_default_uid(self, test_uid):
        return test_uid

    @pytest.fixture
    def tvm(self, base_tvm, tvm_default_uid, tvm_client_id):
        base_tvm.src = tvm_client_id
        base_tvm.default_uid = tvm_default_uid
        return base_tvm

    @pytest.fixture
    def autoenable(self):
        return False

    @pytest.fixture(params=MERCHANT_DATA_TEST_CASES)
    def request_json(self, request, autoenable):
        result = deepcopy(request.param)
        result.update({"description": "Service-Merchant Link",
                       "entity_id": "42",
                       "autoenable": autoenable})
        return result

    @pytest.fixture
    async def response(self, payments_client, request_json, test_uid):
        return await payments_client.post(f'/v1/internal/merchant/{test_uid}/draft', json=request_json)

    class TestAutoEnableSuccess:
        @pytest.fixture
        def autoenable(self):
            return True

        def test_auto_enable_success__ok(self, response):
            assert response.status == 200

    class TestAutoEnableFail:
        @pytest.fixture
        def autoenable(self):
            return True

        def test_fail(self, response):
            assert response.status == 403

        @pytest.fixture
        def tvm_default_uid(self, randn):
            return randn()
