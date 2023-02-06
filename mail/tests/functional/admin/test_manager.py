import pytest

from mail.payments.payments.core.entities.enums import Role
from mail.payments.payments.core.entities.manager import Manager, ManagerRole


class TestManagerRole:
    @pytest.fixture
    def tvm_uid(self, manager_admin):
        return manager_admin.uid

    @pytest.fixture
    async def manager(self, randn, rands, storage):
        return await storage.manager.create(Manager(uid=randn(), domain_login=rands()))

    @pytest.fixture
    async def manager_role(self, manager, storage):
        return await storage.manager_role.create(ManagerRole(manager_uid=manager.uid, role=Role.ASSESSOR))

    @pytest.fixture
    def request_url(self, manager, manager_role):
        return f'/admin/api/v1/manager_role/{manager.uid}'

    @pytest.fixture
    def response_status(self):
        return 200

    @pytest.fixture
    async def response(self,
                       request_url,
                       admin_client,
                       tvm,
                       response_status):
        return await admin_client.get(request_url)

    @pytest.fixture
    async def response_data(self, response):
        return (await response.json())['data']

    @pytest.mark.asyncio
    async def test_response_format(self, response_data, manager_role):
        assert response_data == [Role.ASSESSOR.value]
