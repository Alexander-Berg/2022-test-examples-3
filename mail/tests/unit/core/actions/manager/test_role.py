from random import choices

import pytest

from hamcrest import all_of, assert_that, has_properties, instance_of

from mail.payments.payments.core.actions.manager.role import (
    AddRoleManagerAction, GetManagerRoleAction, ListAllManagerRolesAction, RemoveRoleManagerAction
)
from mail.payments.payments.core.entities.enums import Role
from mail.payments.payments.core.entities.manager import Manager, ManagerRole
from mail.payments.payments.core.entities.userinfo import UserInfo
from mail.payments.payments.storage.exceptions import ManagerRoleNotFound


class RoleFixtures:
    @pytest.fixture
    def manager_uid(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    def blackbox_uid_mock(self, manager_uid, blackbox_corp_client_mocker):
        """BlackBoxClient.uid method will return manager_uid"""
        with blackbox_corp_client_mocker('userinfo', result=UserInfo(uid=manager_uid), multiple_calls=True) as mock:
            yield mock

    @pytest.fixture
    def manager_domain_login(self):
        return ''.join(choices(self.__class__.__name__, k=10))

    @pytest.fixture
    def role(self):
        return Role.ADMIN

    @pytest.fixture
    async def manager(self, manager_uid, manager_domain_login, storage):
        return await storage.manager.create(Manager(uid=manager_uid, domain_login=manager_domain_login))

    @pytest.fixture
    async def manager_role(self, manager, role, storage):
        return await storage.manager_role.create(ManagerRole(manager_uid=manager.uid, role=role))

    @pytest.fixture
    async def manager_roles(self, manager, storage):
        return [await storage.manager_role.create(ManagerRole(manager_uid=manager.uid, role=role)) for role in Role]


class TestAddRoleAction(RoleFixtures):
    @pytest.fixture
    def params(self, manager_domain_login, role):
        return {
            'domain_login': manager_domain_login,
            'role': role,
        }

    @pytest.fixture
    async def add_role(self, params):
        return await AddRoleManagerAction(**params).run()

    @pytest.mark.asyncio
    async def test_creates_manager_account(self, add_role, manager_uid, storage):
        manager = await storage.manager.get(uid=manager_uid)
        assert_that(manager, all_of(instance_of(Manager), has_properties({'uid': manager_uid})))

    @pytest.mark.asyncio
    async def test_role_is_created(self, add_role, manager_uid, role, storage):
        manager_role = await storage.manager_role.get(manager_uid=manager_uid, role=role)
        assert_that(manager_role, all_of(instance_of(ManagerRole), has_properties({'manager_uid': manager_uid,
                                                                                   'role': role})))


class TestRemoveRoleAction(RoleFixtures):
    @pytest.fixture
    def params(self, manager, manager_role):
        return {
            'domain_login': manager.domain_login,
            'role': manager_role.role,
        }

    @pytest.fixture
    async def remove_role(self, params):
        return await RemoveRoleManagerAction(**params).run()

    @pytest.mark.asyncio
    async def test_role_is_removed(self, remove_role, manager_role, storage):
        with pytest.raises(ManagerRoleNotFound):
            await storage.manager_role.get(manager_uid=manager_role.manager_uid, role=manager_role.role)

    @pytest.mark.asyncio
    async def test_can_run_action_even_role_is_removed_already(self, remove_role, params):
        await RemoveRoleManagerAction(**params).run()


class TestListRolesAction(RoleFixtures):
    @pytest.fixture
    def params(self):
        return {}

    @pytest.fixture
    async def roles_list(self, params, manager, manager_role):
        return await ListAllManagerRolesAction(**params).run()

    @pytest.mark.parametrize('role', [Role.ADMIN, Role.ASSESSOR])
    @pytest.mark.asyncio
    async def test_returned_format(self, roles_list, manager, manager_role, role):
        expected = [{
            'login': manager.domain_login,
            'roles': [
                {'role': manager_role.role.value}
            ]
        }]
        assert expected == roles_list

    @pytest.mark.asyncio
    async def test_manager_without_roles(self, params, manager):
        result = await ListAllManagerRolesAction(**params).run()
        assert result == [{'login': manager.domain_login, 'roles': []}]


class TestGetManagerRoleAction(RoleFixtures):
    @pytest.fixture
    def params(self, manager):
        return {
            'manager_uid': manager.uid,
            'uid': manager.uid
        }

    @pytest.fixture
    async def returned_list(self, params, manager_roles):
        return await GetManagerRoleAction(**params).run()

    def test_returned_roles(self, manager_roles, returned_list):
        assert set(returned_list) == set([Role.ADMIN, Role.ASSESSOR, Role.ACCOUNTANT])
