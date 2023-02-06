import pytest

from hamcrest import assert_that, has_entries, has_properties

from mail.beagle.beagle.conf import settings
from mail.beagle.beagle.core.actions.smtp_cache import GenerateSMTPCacheAction
from mail.beagle.beagle.core.actions.user_subscription.create import CreateUserSubscriptionAction
from mail.beagle.beagle.core.entities.enums import SubscriptionType, TaskType
from mail.beagle.beagle.core.exceptions import UserNotFoundError, UserSubscriptionAlreadyExistsError
from mail.beagle.beagle.interactions.blackbox import UserInfo
from mail.beagle.beagle.tests.unit.core.actions.base import *  # noqa


class TestCreateUserSubscription(BaseTestNotFound):  # noqa
    @pytest.fixture
    def action(self, params, mocker):
        return CreateUserSubscriptionAction

    @pytest.fixture
    def subscription_type(self):
        return SubscriptionType.INBOX

    @pytest.fixture
    def params(self, org, mail_list, user, subscription_type):
        return {
            'org_id': org.org_id,
            'mail_list_id': mail_list.mail_list_id,
            'uid': user.uid,
            'subscription_type': subscription_type
        }

    @pytest.fixture
    def default_email(self):
        return "qq@yandex.ru"

    @pytest.fixture
    def info(self, user, default_email):
        return [UserInfo(uid=user.uid, default_email=default_email)]

    @pytest.fixture(autouse=True)
    def blackbox_mock(self, blackbox_client_mocker, info):
        with blackbox_client_mocker('userinfo_by_uids', info) as mock:
            yield mock

    @pytest.fixture
    def returned_func(self, action, params, blackbox_mock, info):
        async def _inner():
            return await action(**params).run()
        return _inner

    @pytest.fixture
    async def returned(self, blackbox_mock, returned_func):
        return await returned_func()

    @pytest.fixture(autouse=True)
    def action_generate_smtp_cache(self, mock_action):
        return mock_action(GenerateSMTPCacheAction)

    @pytest.mark.asyncio
    async def test_cache(self, returned, params, action_generate_smtp_cache):
        action_generate_smtp_cache.assert_called_once_with(org_id=params['org_id'], mail_list_id=params['mail_list_id'])

    class TestNotFound:
        @pytest.mark.asyncio
        async def test_user_not_found(self, params, randn, action):
            params.update({'uid': randn()})
            with pytest.raises(UserNotFoundError):
                await action(**params).run()

    class TestAlreadyExists:
        @pytest.mark.asyncio
        async def test_subscription_already_exists(self, params, user_subscription, action):
            params.update({'uid': user_subscription.uid})
            with pytest.raises(UserSubscriptionAlreadyExistsError):
                await action(**params).run()

    class TestTaskParams:
        @pytest.fixture
        async def tasks(self, storage):
            return [t async for t in storage.task.find()]

        def test_task_creation(self, blackbox_mock, returned, tasks):
            assert len(tasks) == 1

        def test_task_params(self, returned, tasks, mail_list, blackbox_mock, default_email):
            task = tasks[0]
            assert_that(task, has_properties({
                'params': has_entries({
                    'action_kwargs': {
                        'to_email': default_email,
                        'mailing_id': settings.SENDER_MAILING_USER_SUBSCRIBED,
                        'render_context': {'mail_list_name': mail_list.username},
                        'org_id': mail_list.org_id,
                    }
                }),
                'action_name': 'transact_email_action',
                'task_type': TaskType.RUN_ACTION
            }))
