import pytest

from hamcrest import assert_that, has_properties, only_contains

from mail.payments.payments.core.entities.enums import Role
from mail.payments.payments.core.entities.manager import Manager, ManagerRole


@pytest.fixture
async def manager(storage, unique_rand, randn):
    return await storage.manager.create(Manager(
        uid=unique_rand(randn, basket='uid'),
        domain_login='test-manager-domain-login',
    ))


@pytest.fixture
async def manager_roles(manager, storage):
    manager_roles = []
    for role in Role:
        mr = await storage.manager_role.create(ManagerRole(manager_uid=manager.uid, role=role))
        manager_roles.append(mr)
    return manager_roles


@pytest.mark.asyncio
async def test_find_roles_for_manager(manager_roles, manager, storage):
    fetched = [role async for role in storage.manager_role.find(manager_uid=manager.uid)]
    assert {role.role: role for role in manager_roles} == {role.role: role for role in fetched}


@pytest.mark.asyncio
async def test_find_joined_with_manager(manager_roles, manager, storage):
    roles = [role async for role in storage.manager_role.find(manager_uid=manager.uid, with_manager=True)]
    assert_that(roles, only_contains(has_properties({'manager': manager})))


@pytest.mark.asyncio
async def test_delete_role(manager_roles, manager, storage):
    for mr in manager_roles:
        await storage.manager_role.delete(mr)
    roles = [role async for role in storage.manager_role.find(manager_uid=manager.uid)]
    assert len(roles) == 0


@pytest.mark.asyncio
async def test_delete_for_manager(manager_roles, manager, storage):
    for mr in manager_roles:
        await storage.manager_role.delete_for_manager(manager_uid=manager.uid, role=mr.role)
    roles = [role async for role in storage.manager_role.find(manager_uid=manager.uid)]
    assert len(roles) == 0


@pytest.mark.parametrize('manager_roles', [[]])
@pytest.mark.asyncio
async def test_silent_delete_role_for_manager_if_not_exists(manager, manager_roles, storage):
    await storage.manager_role.delete_for_manager(manager_uid=manager.uid, role=Role.ADMIN)


@pytest.mark.asyncio
async def test_silent_delete_for_non_existing_manager(manager, storage):
    await storage.manager_role.delete_for_manager(manager_uid=manager.uid * 100, role=Role.ADMIN)
