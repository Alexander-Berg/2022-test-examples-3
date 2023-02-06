import pytest

from mail.payments.payments.core.actions.manager.role import GetManagerRoleAction
from mail.payments.payments.core.entities.enums import Role
from mail.payments.payments.core.entities.manager import Manager, ManagerRole


class TestManagerRoleHandler:
    @pytest.fixture
    async def manager(self, randn, rands, storage):
        return await storage.manager.create(Manager(uid=randn(), domain_login=rands()))

    @pytest.fixture
    async def manager_role(self, manager, storage):
        return await storage.manager_role.create(ManagerRole(manager_uid=manager.uid, role=Role.ASSESSOR))

    @pytest.fixture
    def run_action_result(self, manager_role):
        return [Role.ASSESSOR]

    @pytest.fixture(autouse=True)
    def action(self, mock_action, run_action_result):
        return mock_action(GetManagerRoleAction, run_action_result)

    @pytest.fixture
    def request_url(self, manager_admin, manager):
        return f'/admin/api/v1/manager_role/{manager.uid}'

    @pytest.fixture
    async def response(self, action, admin_client, request_url, tvm):
        return await admin_client.get(request_url)

    def test_params(self, response, action, manager, manager_admin):
        action.assert_called_once_with(
            uid=manager.uid,
            manager_uid=manager_admin.uid,
        )
