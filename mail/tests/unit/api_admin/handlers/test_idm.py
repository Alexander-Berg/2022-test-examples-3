from random import choices
from typing import Dict

import pytest
import ujson

from mail.payments.payments.core.entities.enums import Role


class BaseTest:
    @pytest.fixture
    def login(self) -> str:
        return ''.join(choices('abcdefg', k=10))

    @pytest.fixture
    def role(self) -> Role:
        return Role.ADMIN

    @pytest.fixture
    def request_post_params(self, login: str, role: Role) -> Dict[str, str]:
        return {
            'login': login,
            'role': ujson.dumps({'role': role.value}),
        }


class TestAddRoleHandler(BaseTest):
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.manager.role import AddRoleManagerAction
        return mock_action(AddRoleManagerAction)

    @pytest.fixture
    async def response(self, admin_client, request_post_params):
        return await admin_client.post('/admin/idm/add-role/', data=request_post_params)

    @pytest.mark.asyncio
    async def test_params(self, response, action, login, role):
        action.assert_called_once_with(domain_login=login, role=role)


class TestRemoveRoleHandler(BaseTest):
    @pytest.fixture(autouse=True)
    def action(self, mock_action):
        from mail.payments.payments.core.actions.manager.role import RemoveRoleManagerAction
        return mock_action(RemoveRoleManagerAction)

    @pytest.fixture
    async def response(self, admin_client, request_post_params):
        return await admin_client.post('/admin/idm/remove-role/', data=request_post_params)

    @pytest.mark.asyncio
    async def test_params(self, response, action, login, role):
        action.assert_called_once_with(domain_login=login, role=role)
