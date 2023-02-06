import pytest

from hamcrest import assert_that, has_properties

from mail.payments.payments.core.actions.base.action import BaseAction
from mail.payments.payments.core.actions.base.db import BaseDBAction
from mail.payments.payments.core.entities.enums import MerchantRole, TaskType
from mail.payments.payments.core.entities.merchant_user import MerchantUser, UserRole
from mail.payments.payments.core.exceptions import CoreMerchantUserNotAuthorizedError
from mail.payments.payments.tests.utils import dummy_coro_ctx
from mail.payments.payments.utils.helpers import copy_context, temp_setattr


@pytest.fixture
def action_name():
    return 'some_action'


@pytest.fixture
def max_retries(randn):
    return randn()


@pytest.fixture
def action_async_params():
    return ['a']


@pytest.fixture
def get_action_class(max_retries):
    def _inner(action_name):
        """Helper to create action class"""

        class SomeAction(BaseDBAction):
            def __init__(self, *args, **kwargs):
                super().__init__()

            async def handle(self):
                pass

        SomeAction.action_name = action_name
        SomeAction.max_retries = max_retries
        return SomeAction

    return _inner


@pytest.fixture
def action_class(action_name, action_async_params, get_action_class):
    cls = get_action_class(action_name)
    yield cls


class TestActionScheduling:
    @pytest.fixture
    def action_kwargs(self):
        return {
            'a': 1,
            'b': 'b',
        }

    @pytest.fixture
    async def scheduled(self, storage, action_class, action_kwargs):
        with temp_setattr(action_class.context, 'storage', storage):
            return await action_class(**action_kwargs).run_async()

    @pytest.mark.parametrize('action_kwargs', [
        {'a': 1},
        {'a': 1, 'b': 'b'}
    ])
    @pytest.mark.asyncio
    async def test_created_task_properties(self, scheduled, action_kwargs, max_retries, action_class):
        """Basic test case for creation Task"""
        self._assert_task_properties(scheduled, action_kwargs, action_class, max_retries)

    @pytest.mark.asyncio
    async def test_can_fetch_task_from_db(self, scheduled, action_class, max_retries, action_kwargs, storage):
        fetched = await storage.task.get(task_id=scheduled.task_id)
        self._assert_task_properties(fetched, action_kwargs, action_class, max_retries)

    def _assert_task_properties(self, task, action_kwargs, action_class, max_retries):
        assert_that(task, has_properties({
            'task_type': TaskType.RUN_ACTION,
            'action_name': action_class.action_name,
            'params': dict(
                action_kwargs=action_kwargs,
                max_retries=max_retries,
            )
        }))


class TestMerchantUserRoles:
    @pytest.fixture
    def context(self):
        1 / 0

    @pytest.fixture
    def action_cls(self, required_merchant_roles):
        _required_merchant_roles = required_merchant_roles

        class DummyAction(BaseAction):
            required_merchant_roles = _required_merchant_roles

            async def handle(self):
                pass

        return DummyAction

    @pytest.mark.parametrize('required_merchant_roles', (
        None,
        (MerchantRole.ADMIN, MerchantRole.OWNER),
    ))
    @pytest.mark.asyncio
    async def test_calls_check_merchant_user_roles_on_run(self, mocker, action_cls):
        with dummy_coro_ctx() as coro:
            mock = mocker.Mock(return_value=coro)
            mocker.patch.object(action_cls, 'check_merchant_user_roles', mock)
            await action_cls().run()
            mock.assert_called_once()

    @pytest.mark.parametrize('required_merchant_roles', (None,))
    @pytest.mark.asyncio
    async def test_passes_with_no_required_merchant_roles(self, action_cls):
        await action_cls().check_merchant_user_roles()

    @pytest.mark.parametrize('required_merchant_roles', ((MerchantRole.ADMIN, MerchantRole.OWNER),))
    @pytest.mark.asyncio
    async def test_fails_with_no_load_override(self, action_cls):
        with pytest.raises(NotImplementedError):
            await action_cls().check_merchant_user_roles()

    class TestWithRequiredRoles:
        @pytest.fixture
        def required_merchant_roles(self):
            return (MerchantRole.OWNER,)

        @pytest.fixture
        def merchant_role(self):
            return MerchantRole.ADMIN

        @pytest.fixture
        def action_cls(self, action_cls, merchant_role):
            @copy_context
            async def dummy_load(self, *args, **kwargs):
                nonlocal merchant_role
                if self.context.merchant_user is not None:
                    self.context.merchant_user.user_role = UserRole(
                        uid=123,
                        merchant_id='456',
                        role=merchant_role,
                    )

            action_cls.load_merchant_user = dummy_load
            return action_cls

        @pytest.fixture
        def merchant_user(self, randn, rands):
            return MerchantUser(user_uid=randn(), merchant_id=rands())

        @pytest.fixture
        def setup_context(self, action_cls, merchant_user):
            with temp_setattr(action_cls.context, 'merchant_user', merchant_user):
                yield

        @pytest.mark.parametrize('merchant_role', (None,))
        @pytest.mark.asyncio
        async def test_raises_for_empty_role(self, action_cls, setup_context):
            with pytest.raises(CoreMerchantUserNotAuthorizedError):
                await action_cls().run()

        @pytest.mark.asyncio
        async def test_raises_for_not_allowed_role(self, mocker, action_cls, setup_context):
            mocker.patch.object(action_cls, 'is_allowed_role', mocker.Mock(return_value=False))
            with pytest.raises(CoreMerchantUserNotAuthorizedError):
                await action_cls().run()

    class TestIsAllowedRole:
        @pytest.mark.parametrize('required_merchant_roles,user_merchant_role', (
            (None, None),
            (None, MerchantRole.ADMIN),
        ))
        def test_no_required(self, action_cls, user_merchant_role):
            assert action_cls().is_allowed_role(user_merchant_role)

        @pytest.mark.parametrize('required_merchant_roles,user_merchant_role', (
            ((MerchantRole.OPERATOR, MerchantRole.VIEWER), None),
        ))
        def test_no_user_merchant_role(self, action_cls, user_merchant_role):
            assert not action_cls().is_allowed_role(user_merchant_role)

        @pytest.mark.parametrize('required_merchant_roles,user_merchant_role', (
            ((MerchantRole.OPERATOR,), MerchantRole.OPERATOR),
            ((MerchantRole.VIEWER, MerchantRole.OPERATOR), MerchantRole.OPERATOR),
            ((MerchantRole.ADMIN,), MerchantRole.ADMIN),
        ))
        def test_exact_role(self, action_cls, user_merchant_role):
            assert action_cls().is_allowed_role(user_merchant_role)

        @pytest.mark.parametrize('required_merchant_roles,user_merchant_role', (
            ((MerchantRole.ADMIN, MerchantRole.VIEWER), MerchantRole.OWNER),
            ((MerchantRole.OPERATOR,), MerchantRole.ADMIN),
            ((MerchantRole.VIEWER,), MerchantRole.OPERATOR),
        ))
        def test_superrole(self, action_cls, user_merchant_role):
            assert action_cls().is_allowed_role(user_merchant_role)
