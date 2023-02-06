from dataclasses import asdict

import pytest

from hamcrest import assert_that, equal_to, has_length

from mail.ipa.ipa.core.actions.import_.org import InitOrgImportAction
from mail.ipa.ipa.core.entities.password import Password
from mail.ipa.ipa.core.entities.user import User
from mail.ipa.ipa.core.entities.user_info import UserInfo
from mail.ipa.ipa.interactions.directory import DirectoryClient
from mail.ipa.ipa.tests.contracts import ActionTestContract


async def check_users_created(storage, users):
    created_users = [user async for user in storage.user.find()]
    assert_that(created_users, has_length(len(users)))


async def check_tasks_created(storage, users):
    created_tasks = [task async for task in storage.task.find()]
    created_users = [user async for user in storage.user.find()]
    assert_that(
        sorted(task.entity_id for task in created_tasks),
        equal_to(sorted(user.user_id for user in created_users)),
    )


@pytest.fixture
def users():
    return [
        UserInfo(login='foo', src_login='foomail@example.test', password=Password.from_plain('foopasswd')),
        UserInfo(login='bar', src_login='barmail@example.test', password=Password.from_plain('barpasswd')),
        UserInfo(login='baz', src_login='bazmail@example.test', password=Password.from_plain('bazpasswd')),
    ]


class TestInitOrganizationImportAction(ActionTestContract):
    @pytest.fixture
    def action_class(self):
        return InitOrgImportAction

    @pytest.fixture
    def params(self, general_init_import_params, users):
        return {
            'params': general_init_import_params,
            'users': users,
        }

    @pytest.fixture(autouse=True)
    def mock_get_organization(self, mocker, coromock):
        return mocker.patch.object(DirectoryClient, 'get_organization', coromock(mocker.Mock()))

    @pytest.fixture(autouse=True)
    def mock_strip_login(self, mocker, action):
        return mocker.patch.object(action, '_strip_domain', mocker.Mock(side_effect=lambda login, domains: login))

    @pytest.mark.asyncio
    async def test_users_created(self, returned, storage, users):
        await check_users_created(storage, users)

    @pytest.mark.asyncio
    async def test_tasks_created(self, returned, storage, users):
        await check_tasks_created(storage, users)

    def test_calls_get_organization(self, returned, mock_get_organization, org_id):
        mock_get_organization.assert_called_once_with(org_id)

    @pytest.mark.asyncio
    async def test_calls_strip_domain(self, mocker, returned, mock_get_organization, mock_strip_login, users):
        all_domains = (await mock_get_organization()).domains.all
        mock_strip_login.assert_has_calls([mocker.call(user.login, all_domains) for user in users])

    @pytest.mark.asyncio
    async def test_created_tasks_import_params(self, returned, storage, general_init_import_params, users):
        created_tasks = [task async for task in storage.task.find()]
        assert_that(
            [task.params['import_params'] for task in created_tasks],
            equal_to([asdict(general_init_import_params)] * len(users))
        )

    @pytest.mark.asyncio
    async def test_created_tasks_src_logins(self, returned, storage, users):
        created_tasks = [task async for task in storage.task.find()]
        assert_that(
            sorted(task.params['user_info']['src_login'] for task in created_tasks),
            equal_to(sorted(user.src_login for user in users))
        )

    class TestSomeUsersAlreadyExist:
        @pytest.fixture
        async def create_predefined_users(self, storage, org_id, users):
            await storage.user.create(User(org_id=org_id, login=users[0].login))

        @pytest.fixture
        def before_action_hook(self, create_predefined_users):
            pass

        @pytest.mark.asyncio
        async def test_all_users_created(self, returned, storage, users):
            await check_users_created(storage, users)

        @pytest.mark.asyncio
        async def test_tasks_created_for_all_users(self, returned, storage, users):
            await check_tasks_created(storage, users)


@pytest.mark.parametrize('login, domains, expected', (
    ('login', ['who', 'cares'], 'login'),
    ('login@domain', ['the', 'domain'], 'login'),
    ('login@alien', ['the', 'domain'], 'login@alien'),
))
def test_strip_domain(login, domains, expected):
    assert_that(
        InitOrgImportAction._strip_domain(login, domains),
        equal_to(expected),
    )
