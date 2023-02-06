import pytest

from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.core.entities.not_fetched import NOT_FETCHED
from mail.payments.payments.core.entities.user_role import UserRole


@pytest.fixture
def merchant_id(merchant_uid):
    return str(merchant_uid)


@pytest.fixture
def role():
    return MerchantRole.VIEWER


@pytest.fixture
async def user_role_entity(storage, merchant_uid, merchant_id, role, created_now):
    return UserRole(uid=merchant_uid, merchant_id=merchant_id, role=role, created=created_now, updated=created_now)


@pytest.fixture
async def user_role(storage, user, merchant, user_role_entity):
    return await storage.user_role.create(user_role_entity)


@pytest.mark.asyncio
async def test_user_role_created(user_role_entity, user_role):
    assert user_role_entity == user_role


@pytest.mark.asyncio
async def test_get_user_role(user_role, storage):
    assert user_role == await storage.user_role.get(uid=user_role.uid, merchant_id=user_role.merchant_id)


@pytest.mark.asyncio
async def test_get_user_role_with_user(storage, merchant_id, user, user_role):
    returned = await storage.user_role.get(uid=user.uid, merchant_id=merchant_id, with_user=True)
    assert returned == user_role and returned.user == user


@pytest.mark.asyncio
async def test_save_user(user_role, storage):
    user_role.role = MerchantRole.OPERATOR
    await storage.user_role.save(user_role)
    assert MerchantRole.OPERATOR == (await storage.user_role.get(uid=user_role.uid,
                                                                 merchant_id=user_role.merchant_id)).role


@pytest.mark.asyncio
async def test_find_roles_basic(user_role, storage):
    assert [r async for r in storage.user_role.find(merchant_id=user_role.merchant_id)] == [user_role]


@pytest.mark.asyncio
async def test_delete_role(user_roles, merchant, storage):
    for user_role in user_roles:
        await storage.user_role.delete(user_role)
    roles = [role async for role in storage.user_role.find(merchant_id=merchant.merchant_id)]
    assert len(roles) == 0


@pytest.mark.asyncio
async def test_with_merchant(user_roles, merchant, storage):
    roles = [role async for role in storage.user_role.find(merchant_id=merchant.merchant_id, with_merchant=True)]
    merchant.oauth = NOT_FETCHED
    merchant.functionalities = NOT_FETCHED
    assert roles[0].merchant == merchant


@pytest.mark.asyncio
async def test_role_filter(user_roles, storage):
    roles = [role async for role in storage.user_role.find(role=MerchantRole.OWNER)]
    assert len(roles) == 0


@pytest.mark.asyncio
async def test_uid_filter(user_roles, storage):
    uids = [user_role.uid for user_role in user_roles]
    roles = [role async for role in storage.user_role.find(uid=uids[0])]
    assert len(roles) == 1
