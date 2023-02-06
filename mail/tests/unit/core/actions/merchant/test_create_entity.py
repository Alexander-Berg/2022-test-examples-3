import pytest

from hamcrest import assert_that, has_properties

from mail.payments.payments.core.actions.merchant.create_entity import CreateMerchantEntityAction
from mail.payments.payments.core.actions.user_role.create import CreateOwnerUserRoleAction
from mail.payments.payments.core.entities.enums import ShopType


@pytest.mark.asyncio
async def test_merchant_created(storage, create_entity, merchant_entity):
    await create_entity()

    assert_that(
        await storage.merchant.get(merchant_entity.uid),
        has_properties({
            'name': merchant_entity.name,
        })
    )


@pytest.mark.asyncio
async def test_serial_created(storage, create_entity, merchant_entity):
    await create_entity()

    assert_that(
        await storage.serial.get(merchant_entity.uid),
        has_properties({
            'uid': merchant_entity.uid,
        })
    )


@pytest.mark.asyncio
async def test_create_owner_user_role_called(storage, create_entity, merchant_entity, mock_create_owner_user_role):
    await create_entity()

    mock_create_owner_user_role.assert_called_once_with(
        merchant_id=merchant_entity.merchant_id,
        user_uid=merchant_entity.uid,
    )


@pytest.mark.parametrize('shop_type', ShopType)
@pytest.mark.asyncio
async def test_default_shop_is_created(storage, create_entity, merchant_entity, shop_type):
    await create_entity()

    # Should not raise NotFoundError
    await storage.shop.get_default_for_merchant(uid=merchant_entity.uid, shop_type=shop_type)


@pytest.fixture
def create_entity(merchant_entity):
    async def _create_entity():
        await CreateMerchantEntityAction(merchant_entity).run()
    return _create_entity


@pytest.fixture(autouse=True)
def mock_create_owner_user_role(mock_action):
    return mock_action(CreateOwnerUserRoleAction)
