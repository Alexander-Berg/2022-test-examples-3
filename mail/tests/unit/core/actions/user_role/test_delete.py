import pytest

from mail.payments.payments.core.actions.user_role.delete import DeleteUserRoleAction
from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.core.entities.user import User
from mail.payments.payments.core.entities.user_role import UserRole
from mail.payments.payments.core.exceptions import (
    CoreFailError, CoreMerchantUserNotAuthorizedError, RoleDeletionNotAllowed, UserRoleNotFoundError
)
from mail.payments.payments.storage.exceptions import UserRoleNotFound


@pytest.fixture
def params(user_role, merchant):
    return {'merchant_id': user_role.merchant_id,
            'user_uid': user_role.uid}


@pytest.fixture
async def returned(user_roles, params, blackbox_mock, info):
    return await DeleteUserRoleAction(**params).run()


@pytest.fixture
async def owner(storage, randmail, merchant):
    user_entity = User(uid=merchant.uid, email=randmail())
    user = await storage.user.create(user_entity)
    entity = UserRole(uid=user.uid, merchant_id=str(merchant.uid), role=MerchantRole.OWNER)
    return await storage.user_role.create(entity)


@pytest.mark.asyncio
async def test_returned(storage, user_role, returned):
    with pytest.raises(UserRoleNotFound):
        await storage.user_role.get(merchant_id=user_role.merchant_id, uid=user_role.uid)


@pytest.mark.asyncio
async def test_empty_params(params):
    params.pop('user_email', None)
    params.pop('user_uid', None)
    with pytest.raises(CoreFailError):
        await DeleteUserRoleAction(**params).run()


@pytest.mark.asyncio
async def test_owner_role_denied(params, owner):
    params.update({'merchant_id': owner.merchant_id,
                   'user_uid': owner.uid})
    with pytest.raises(RoleDeletionNotAllowed):
        await DeleteUserRoleAction(**params).run()


@pytest.mark.asyncio
async def test_delete_not_existing(params, user_role, randn):
    user_uid = randn()
    params.update({'merchant_id': user_role.merchant_id,
                   'user_uid': user_uid})
    with pytest.raises(UserRoleNotFoundError):
        await DeleteUserRoleAction(**params).run()


@pytest.mark.parametrize('acting_role', [MerchantRole.ADMIN, MerchantRole.OWNER])
@pytest.mark.asyncio
async def test_admin_role_is_enough_to_run_action(params, acting_role, create_acting_user):
    acting_user_data = await create_acting_user(role=acting_role)
    DeleteUserRoleAction.context.merchant_user = acting_user_data['merchant_user']
    await DeleteUserRoleAction(**params).run()


@pytest.mark.parametrize('acting_role', [MerchantRole.VIEWER, MerchantRole.OPERATOR])
@pytest.mark.asyncio
async def test_raises_if_role_is_lower_than_admin(params, acting_role, create_acting_user):
    acting_user_data = await create_acting_user(role=acting_role)
    DeleteUserRoleAction.context.merchant_user = acting_user_data['merchant_user']
    with pytest.raises(CoreMerchantUserNotAuthorizedError):
        await DeleteUserRoleAction(**params).run()
