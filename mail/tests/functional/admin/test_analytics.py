import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.userinfo import UserInfo
from mail.payments.payments.storage.mappers.merchant import MerchantMapper

from .base import BaseTestNotAuthorized


@pytest.mark.usefixtures('balance_person_mock')
class TestGetMerchantsAnalyticsAdmin(BaseTestNotAuthorized):

    @pytest.fixture
    def sort_by(self):
        return 'uid'

    @pytest.fixture
    def desc(self):
        return False

    @pytest.fixture(autouse=True)
    def mock_blackbox_user_info(self, blackbox_client_mocker, merchant, randmail):
        with blackbox_client_mocker('userinfo', UserInfo(uid=merchant.uid, default_email=randmail())) as mock:
            yield mock

    @pytest.fixture
    async def merchant_with_service_merchant(self, create_merchant, create_service_merchant):
        merchant = await create_merchant()
        await create_service_merchant(uid=merchant.uid)
        return merchant

    @pytest.fixture(params=('merchant', 'merchant_preregistered', 'merchant_with_service', 'merchant_with_parent'))
    def target_merchant(
        self,
        request,
        merchant,
        merchant_preregistered,
        merchant_with_service_merchant,
        merchant_with_parent,
    ):
        return {
            'merchant': merchant,
            'merchant_preregistered': merchant_preregistered,
            'merchant_with_service': merchant_with_service_merchant,
            'merchant_with_parent': merchant_with_parent,
        }[request.param]

    @pytest.fixture(params=('assessor', 'admin', 'accountant'))
    def acting_manager_type(self, request):
        return request.param

    @pytest.fixture(autouse=True)
    def acting_manager(self, acting_manager_type, managers):
        return managers[acting_manager_type]

    @pytest.fixture
    def tvm_uid(self, acting_manager):
        return acting_manager.uid

    @pytest.fixture(autouse=True)
    def setup_spy(self, mocker):
        mocker.spy(MerchantMapper, 'get_analytics')
        mocker.spy(MerchantMapper, 'get_analytics_found')

    @pytest.fixture
    def request_url(self):
        return '/admin/api/v1/analytics'

    @pytest.fixture
    def request_params(self, sort_by, target_merchant, desc):
        return {
            'merchant_uid': target_merchant.uid,
            'sort_by': sort_by,
            'desc': str(desc).lower(),
        }

    @pytest.fixture
    def response_func(self, admin_client, tvm, request_url, request_params):
        async def _inner(params=None):
            if params is None:
                params = request_params
            return await admin_client.get(request_url, params=params)

        return _inner

    @pytest.fixture
    def response_merchants(self, response_data):
        return response_data['merchants']

    @pytest.mark.asyncio
    async def test_keyset_reusable(self, response_func):
        response = await response_func(params={})
        keyset = (await response.json())['data']['next']['keyset']
        response = await response_func({'keyset': keyset})
        assert response.status == 200

    class TestGetByUID:
        @pytest.fixture
        def request_params(self, target_merchant):
            return {'merchant_uid': target_merchant.uid}

        def test_get_by_uid__single_merchant(self, response, response_merchants):
            assert len(response_merchants) == 1

        @pytest.mark.parametrize('acting_manager_type', ('assessor',))
        def test_get_by_uid__merchant_data_for_assessor(self, target_merchant, response_merchants):
            assert_that(
                response_merchants[0],
                has_entries({
                    'uid': target_merchant.uid,
                    'payments_success': 0,
                    'money_success': None,
                    'money_refund': None,
                    'money_aov': '0',
                })
            )

        @pytest.mark.parametrize('acting_manager_type', ('accountant', 'admin'))
        def test_get_by_uid__merchant_data_for_accountant(self, target_merchant, person_entity, response_merchants):
            assert_that(
                response_merchants[0],
                has_entries({
                    'uid': target_merchant.uid,
                    'payments_success': 0,
                    'money_success': '0.0000000000',
                    'money_refund': '0.0000000000',
                    'money_aov': '0',
                })
            )

    class TestGetEmpty:
        @pytest.fixture
        def request_params(self, unique_rand, randn):
            return {
                'merchant_uid': unique_rand(randn, basket='uid'),
                'client_id': unique_rand(randn, basket='client_id')
            }

        def test_get_empty__response_should_be_empty(self, response_merchants):
            assert response_merchants == []

    class TestInvalidSortBy:
        @pytest.fixture
        def sort_by(self, rands):
            return rands()

        def test_invalid_sort_by__bad_request(self, response):
            assert response.status == 400

    @pytest.mark.parametrize('sort_by', ('created', 'payments_success', 'money_success', 'uid'))
    @pytest.mark.parametrize('desc', (False, True))
    def test_sort_mapper_params(self, sort_by, desc, response):
        assert_that(
            MerchantMapper.get_analytics.call_args[1],
            has_entries({
                'sort_by': sort_by,
                'descending': desc,
            })
        )
