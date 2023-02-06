import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, has_entries

from .base import BaseTestNotAuthorized


class TestService(BaseTestNotAuthorized):
    @pytest.fixture
    def tvm_uid(self, manager_admin):
        return manager_admin.uid

    @pytest.fixture
    def request_url(self, manager):
        return '/admin/api/v1/service'

    @pytest.fixture
    def response_status(self):
        return 200

    @pytest.fixture
    async def response(self,
                       request_url,
                       admin_client,
                       tvm,
                       service,
                       response_status):
        return await admin_client.get(request_url)

    @pytest.fixture
    async def response_data(self, response):
        return (await response.json())['data']

    @pytest.mark.asyncio
    async def test_response(self, storage, response_data):
        expected = await alist(storage.service.find())
        assert_that(
            response_data,
            contains_inanyorder(*[
                has_entries({
                    'name': service.name,
                    'service_id': service.service_id,
                })
                for service in expected
            ])
        )
