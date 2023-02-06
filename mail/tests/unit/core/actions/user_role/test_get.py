import pytest

from hamcrest import assert_that, contains_inanyorder

from mail.payments.payments.core.actions.user_role.get import GetUserRolesForMerchantAction, GetUserRolesForUserAction
from mail.payments.payments.core.entities.enums import MerchantRole
from mail.payments.payments.core.entities.user import User
from mail.payments.payments.core.entities.user_role import UserRole
from mail.payments.payments.core.exceptions import CoreMerchantUserNotAuthorizedError
from mail.payments.payments.tests.utils import dummy_coro_generator


@pytest.mark.usefixtures('user_roles')
class TestGetUserRolesForMerchantAction:
    @pytest.fixture
    def params(self, merchant):
        return {'merchant_id': merchant.merchant_id}

    @pytest.fixture
    def run_action(self, params):
        async def run(cls=GetUserRolesForMerchantAction):
            return await cls(**params).run()

        return run

    @pytest.mark.asyncio
    async def test_returns_user_roles(self, run_action, user_roles):
        """Экшен возвращает корректный список ролей по заданному Мерчанту."""

        returned_user_roles = await run_action()

        assert_that(
            [
                {
                    'uid': ret_user_role.uid,
                    'merchant_id': ret_user_role.merchant_id,
                    'role': ret_user_role.role
                }
                for ret_user_role in returned_user_roles
            ],
            contains_inanyorder(*[
                {
                    'uid': user_role.uid,
                    'merchant_id': user_role.merchant_id,
                    'role': user_role.role,
                }
                for user_role in user_roles
            ])
        )

    @pytest.mark.parametrize('acting_role', [MerchantRole.VIEWER])
    @pytest.mark.asyncio
    async def test_raises_if_weak_merchant_user_role(
        self,
        run_action,
        create_acting_user,
        acting_role: MerchantRole,
    ):
        """
        Если в контексте есть MerchantUser со слишком слабой ролью,
        экшен должен бросить ошибку доступа.
        """
        GetUserRolesForMerchantAction.required_merchant_roles = (MerchantRole.ADMIN,)
        acting_user_data = await create_acting_user(role=acting_role)
        GetUserRolesForMerchantAction.context.merchant_user = acting_user_data['merchant_user']
        with pytest.raises(CoreMerchantUserNotAuthorizedError):
            await run_action()

    @pytest.mark.parametrize('acting_role', list(MerchantRole))
    @pytest.mark.asyncio
    async def test_enough_merchant_user_role(
        self,
        run_action,
        create_acting_user,
        acting_role: MerchantRole,
    ):
        """
        Если в контексте есть MerchantUser с достаточной ролью,
        то экшен отработает нормально.
        Достаточная роль по-умолчанию: VIEWER, т. е. любая явная роль
        должна позволить пользователю увидеть список ролей.
        """
        GetUserRolesForMerchantAction.required_merchant_roles = (MerchantRole.VIEWER,)
        acting_user_data = await create_acting_user(role=acting_role)
        GetUserRolesForMerchantAction.context.merchant_user = acting_user_data['merchant_user']
        await run_action()


class TestGetUserRolesForUserAction:
    @pytest.fixture
    def uid(self, randn):
        return randn()

    @pytest.fixture
    def params(self, uid):
        return {'uid': uid}

    @pytest.fixture(autouse=True)
    def balance_person_mock(self, mocker, person_entity):
        yield mocker.patch(
            'mail.payments.payments.interactions.balance.BalanceClient.get_person',
            mocker.Mock(side_effect=dummy_coro_generator(person_entity)),
        )

    @pytest.fixture
    async def merchants(self, storage, create_merchant):
        for _ in range(3):
            await create_merchant()
        return [m async for m in storage.merchant.find()]

    @pytest.fixture
    async def user_roles(self, merchants, uid, randmail, storage):
        user_roles = []
        user = await storage.user.create(User(uid=uid, email=randmail()))
        for merchant in merchants:
            user_role = await storage.user_role.create(UserRole(
                uid=user.uid,
                merchant_id=merchant.merchant_id,
                role=MerchantRole.VIEWER,
            ))
            user_roles.append(user_role)
        return user_roles

    @pytest.fixture
    async def returned(self, user_roles, params):
        return await GetUserRolesForUserAction(**params).run()

    def test_returned(self, balance_person_mock, returned, user_roles):
        assert_that(
            [
                {
                    'uid': ret_user_role.uid,
                    'merchant_id': ret_user_role.merchant_id,
                    'role': ret_user_role.role
                }
                for ret_user_role in returned
            ],
            contains_inanyorder(*[
                {
                    'uid': user_role.uid,
                    'merchant_id': user_role.merchant_id,
                    'role': user_role.role,
                }
                for user_role in user_roles
            ])
        )
