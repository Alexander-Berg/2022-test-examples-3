import pytest

from mail.payments.payments.core.entities.enums import Role
from mail.payments.payments.core.entities.manager import Manager, ManagerRole


@pytest.fixture
def manager_uid(unique_rand, randn):
    return unique_rand(randn, basket='manager_uid')


@pytest.fixture
def manager_login():
    return 'vasya'


@pytest.fixture
async def manager_entity(storage, manager_uid, manager_login):
    return Manager(uid=manager_uid, domain_login=manager_login)


@pytest.fixture
async def manager(storage, manager_entity):
    return await storage.manager.create(manager_entity)


@pytest.fixture
async def manager_roles(manager, storage):
    manager_roles = []
    for role in Role:
        mr = await storage.manager_role.create(ManagerRole(manager_uid=manager.uid, role=role))
        manager_roles.append(mr)
    return manager_roles


@pytest.mark.asyncio
async def test_manager_created(manager_entity, manager):
    assert manager_entity == manager


@pytest.mark.asyncio
async def test_get_manager(manager, storage):
    assert manager == await storage.manager.get(uid=manager.uid)


@pytest.mark.asyncio
async def test_find_manager_basic(manager, storage):
    assert [m async for m in storage.manager.find()] == [manager]
