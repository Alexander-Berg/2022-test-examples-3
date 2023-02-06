import pytest

from mail.payments.payments.core.actions.merchant.get_by_key import GetMerchantAction, GetMerchantByKeyAction
from mail.payments.payments.core.exceptions import DeveloperKeyAccessDenyError
from mail.payments.payments.interactions.developer.exceptions import DeveloperKeyAccessDeny


class TestGetMerchantByKey:
    @pytest.fixture(autouse=True)
    def get_merchant_action_mock(self, mock_action, merchant):
        return mock_action(GetMerchantAction, merchant)

    @pytest.fixture(autouse=True)
    def check_key_mock(self, developer_client_mocker, merchant):
        with developer_client_mocker('check_key', merchant.uid) as mock:
            yield mock

    @pytest.fixture
    def key(self):
        return 'test-get-merchant-by-key-key'

    @pytest.fixture
    def user_ip(self):
        return 'test-get-merchant-by-key-user-ip'

    @pytest.fixture
    def params(self, key, user_ip):
        return {
            'key': key,
            'user_ip': user_ip,
        }

    @pytest.fixture
    async def returned(self, params):
        return await GetMerchantByKeyAction(**params).run()

    def test_returns_merchant(self, merchant, returned):
        assert returned == merchant

    def test_get_merchant_call(self, merchant, get_merchant_action_mock, returned):
        get_merchant_action_mock.assert_called_once_with(uid=merchant.uid)

    class TestInvalidKey:
        @pytest.fixture(autouse=True)
        def check_key_mock(self, developer_client_mocker):
            with developer_client_mocker('check_key', exc=DeveloperKeyAccessDeny) as mock:
                yield mock

        @pytest.mark.asyncio
        async def test_raises_data_error(self, params):
            with pytest.raises(DeveloperKeyAccessDenyError):
                await GetMerchantByKeyAction(**params).run()
