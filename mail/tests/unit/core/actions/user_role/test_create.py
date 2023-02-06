import pytest

from hamcrest import assert_that, has_entries, has_properties

from mail.payments.payments.core.actions.user_role.create import CreateOwnerUserRoleAction, CreateUserRoleAction
from mail.payments.payments.core.entities.enums import MerchantRole, TaskType
from mail.payments.payments.core.entities.user import User
from mail.payments.payments.core.entities.user_role import UserRole
from mail.payments.payments.core.exceptions import (
    CoreFailError, CoreMerchantUserNotAuthorizedError, MerchantNotFoundError, RoleAssignmentNotAllowed
)


class BaseTestCreateUserRoleAction:
    @pytest.fixture(params=('user_uid', 'user_email'))
    def user_key(self, request):
        return request.param

    @pytest.fixture
    def description(self, rands):
        return rands()

    @pytest.fixture
    def notify(self):
        return False

    @pytest.fixture
    def params(self, merchant, role, user_key, user_uid, user_email, description, notify):
        params = {
            'merchant_id': merchant.merchant_id,
            'role': role,
            'description': description,
            'notify': notify,
        }
        params[user_key] = {
            'user_uid': user_uid,
            'user_email': user_email,
        }[user_key]
        return params

    @pytest.fixture
    def action(self, params):
        return CreateUserRoleAction(**params)

    @pytest.fixture
    def returned_func(self, action, blackbox_mock, info):
        async def _inner():
            return await action.run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.fixture
    async def tasks(self, storage):
        return [t async for t in storage.task.find()]

    @pytest.fixture
    async def user_entity(self, storage, user_uid, default_user_email):
        return User(uid=user_uid, email=default_user_email)

    @pytest.fixture
    async def user(self, storage, user_entity):
        return await storage.user.create(user_entity)

    @pytest.fixture
    async def user_role_entity(self, storage, merchant, role, description, user_key, user_uid, user_email,
                               default_user_email):
        return UserRole(
            uid=user_uid,
            merchant_id=merchant.merchant_id,
            role=role,
            description=description,
            email=user_email if user_key == 'user_email' else default_user_email,
        )

    @pytest.fixture
    async def user_role(self, storage, user, merchant, user_role_entity):
        return await storage.user_role.create(user_role_entity)

    def test_returned(self, returned, user_role_entity):
        assert_that(returned, has_properties({
            'uid': user_role_entity.uid,
            'email': user_role_entity.email,
            'merchant_id': user_role_entity.merchant_id,
            'role': user_role_entity.role,
            'description': user_role_entity.description,
        }))

    @pytest.mark.asyncio
    async def test_role_created(self, storage, returned, user_role_entity):
        user_role = await storage.user_role.get(uid=user_role_entity.uid, merchant_id=user_role_entity.merchant_id)
        assert_that(user_role, has_properties({
            'uid': user_role_entity.uid,
            'email': user_role_entity.email,
            'merchant_id': user_role_entity.merchant_id,
            'role': user_role_entity.role,
            'description': user_role_entity.description,
        }))

    @pytest.mark.parametrize('params', [{'merchant_id': 'some_id', 'role': MerchantRole.ADMIN}])
    @pytest.mark.asyncio
    async def test_merchant_not_found(self, returned_func):
        with pytest.raises(MerchantNotFoundError):
            await returned_func()

    class TestEmptyParams:
        @pytest.fixture
        def params(self, merchant, role):
            return {'merchant_id': merchant.merchant_id, 'role': role}

        @pytest.mark.asyncio
        async def test_empty_params(self, returned_func):
            with pytest.raises(CoreFailError):
                await returned_func()


class TestCreateUserRoleAction(BaseTestCreateUserRoleAction):
    @pytest.mark.parametrize('role', (MerchantRole.OWNER,))
    @pytest.mark.asyncio
    async def test_owner_role_creation_is_not_allowed(self, returned_func):
        with pytest.raises(RoleAssignmentNotAllowed):
            await returned_func()

    @pytest.mark.parametrize('acting_role', [MerchantRole.VIEWER, MerchantRole.OPERATOR])
    @pytest.mark.asyncio
    async def test_raises_if_merchant_user_is_not_so_powerful_as_admin(
        self,
        returned_func,
        create_acting_user,
        acting_role: MerchantRole,
    ):
        acting_user_data = await create_acting_user(role=acting_role)
        CreateUserRoleAction.context.merchant_user = acting_user_data['merchant_user']
        with pytest.raises(CoreMerchantUserNotAuthorizedError):
            await returned_func()

    @pytest.mark.parametrize('acting_role', [MerchantRole.ADMIN, MerchantRole.OWNER])
    @pytest.mark.asyncio
    async def test_admin_role_is_enough_to_run_action(
        self,
        returned_func,
        create_acting_user,
        acting_role: MerchantRole,
    ):
        acting_user_data = await create_acting_user(role=acting_role)
        CreateUserRoleAction.context.merchant_user = acting_user_data['merchant_user']
        await returned_func()

    class TestUpdatesRole:
        @pytest.fixture
        def new_role(self):
            return MerchantRole.OPERATOR

        @pytest.fixture
        def new_description(self, rands):
            return rands()

        @pytest.fixture
        def params(self, user_role, new_role, new_description, params):
            return {**params, 'role': new_role, 'description': new_description}

        @pytest.mark.parametrize('role,description,new_role,new_description', (
            pytest.param(MerchantRole.VIEWER, 'aaa', MerchantRole.OPERATOR, 'bbb', id='role-description-update'),
            pytest.param(MerchantRole.OWNER, 'aaa', MerchantRole.OWNER, 'bbb', id='owner-description-update'),
        ))
        @pytest.mark.asyncio
        async def test_role_updated(self, storage, user, user_role, returned_func, new_role, new_description):
            returned = await returned_func()
            storage_role = await storage.user_role.get(uid=returned.uid, merchant_id=returned.merchant_id)
            assert storage_role.role == new_role and storage_role.description == new_description

        @pytest.mark.parametrize('role,new_role', (
            pytest.param(MerchantRole.OWNER, MerchantRole.VIEWER, id='update_from_owner'),
            pytest.param(MerchantRole.VIEWER, MerchantRole.OWNER, id='update_to_owner'),
        ))
        @pytest.mark.asyncio
        async def test_owner_role_update_is_not_allowed(self, returned_func):
            with pytest.raises(RoleAssignmentNotAllowed):
                await returned_func()


class TestTaskCreation(BaseTestCreateUserRoleAction):
    @pytest.mark.parametrize('notify', (True, False))
    def test_task_creation(self, returned, role, notify, tasks):
        assert len(tasks) == notify


class TestTaskParams(BaseTestCreateUserRoleAction):
    @pytest.mark.parametrize('notify', (True,))
    def test_task_params(self, returned, tasks, merchant, payments_settings, role, blackbox_mock):
        task = tasks[0]
        assert_that(task, has_properties({
            'params': has_entries(
                action_kwargs={
                    'to_email': 'qq@yandex.ru',
                    'mailing_id': payments_settings.SENDER_MAILING_USER_ROLE_ASSIGNED,
                    'render_context': {'user_role': role.value, 'merchant_name': merchant.name}
                }
            ),
            'action_name': 'transact_email_action',
            'task_type': TaskType.RUN_ACTION
        }))


class TestCreateOwnerUserRoleAction(BaseTestCreateUserRoleAction):
    @pytest.fixture
    def role(self):
        return MerchantRole.OWNER

    @pytest.fixture
    def description(self):
        return None

    @pytest.fixture
    def action(self, params):
        params = {**params}
        params.pop('role')
        params.pop('description', None)
        return CreateOwnerUserRoleAction(**params)

    @pytest.mark.asyncio
    async def test_owner_role(self, returned_func):
        await returned_func()
