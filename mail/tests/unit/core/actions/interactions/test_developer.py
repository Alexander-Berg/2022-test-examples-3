import pytest

from mail.payments.payments.core.actions.interactions.developer import GetUidByKeyAction
from mail.payments.payments.core.exceptions import DeveloperKeyAbsentError, DeveloperKeyAccessDenyError
from mail.payments.payments.interactions.developer import DeveloperKeyAccessDeny


class TestGetUidByKeyAction:
    @pytest.fixture
    def uid(self, randn):
        return randn()

    @pytest.fixture
    def key(self, rands):
        return rands()

    @pytest.fixture
    def user_ip(self, rands):
        return rands()

    @pytest.fixture
    def exc(self):
        return None

    @pytest.fixture(autouse=True)
    def developer_mock(self, developer_client_mocker, uid, exc):
        with developer_client_mocker('check_key', result=uid, exc=exc) as mock:
            yield mock

    @pytest.fixture
    def returned_func(self, key, user_ip):
        async def _inner():
            return await GetUidByKeyAction(key=key, user_ip=user_ip).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    def test_result(self, uid, returned):
        assert returned == uid

    @pytest.mark.parametrize('exc', (DeveloperKeyAccessDeny,))
    @pytest.mark.asyncio
    async def test_access_deny(self, returned_func):
        with pytest.raises(DeveloperKeyAccessDenyError):
            await returned_func()

    @pytest.mark.parametrize('key', (None,))
    @pytest.mark.asyncio
    async def test_absent_key(self, returned_func):
        with pytest.raises(DeveloperKeyAbsentError):
            await returned_func()
