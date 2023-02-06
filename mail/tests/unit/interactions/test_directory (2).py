from datetime import date

import pytest

from hamcrest import assert_that, contains_string, equal_to

from mail.ipa.ipa.interactions.directory.entities import DirectoryDomains, DirectoryOrg, DirectoryUser, PasswordMode
from mail.ipa.ipa.interactions.directory.exceptions import (
    DirectoryNotFoundError, DirectoryOrganizationNotFoundError, DirectoryUserExistsError
)


@pytest.fixture
def directory_client(clients):
    return clients.directory


class TestDirectoryGetUserByLogin:
    @pytest.fixture
    def login(self):
        return 'foobar'

    @pytest.fixture
    def request_coro(self, directory_client, org_id, login):
        return directory_client.get_user_by_login(org_id, login)

    @pytest.fixture
    def directory_client(self, directory_client, mock_directory, login, response):
        mock_directory(f'/v11/users/nickname/{login}/', response)
        return directory_client

    class TestSuccess:
        @pytest.fixture
        def response(self, login):
            return {'id': 100, 'email': f'{login}@org.test'}

        @pytest.fixture(autouse=True)
        async def returned(self, request_coro):
            return await request_coro

        def test_returns_user(self, returned, login):
            assert_that(returned, equal_to(DirectoryUser(uid=100, email=f'{login}@org.test')))

        def test_headers(self, get_last_directory_request, org_id):
            headers = get_last_directory_request().headers
            assert_that(headers['X-ORG-ID'], equal_to(str(org_id)))

        def test_params(self, get_last_directory_request):
            assert_that(get_last_directory_request().query, equal_to({'fields': 'id,email'}))

        @pytest.mark.parametrize('login, expected_url', (
            ('foobar', '/users/nickname/foobar/'),
            (' withspace', '/users/nickname/%20withspace/'),
            ('withslash/', '/users/nickname/withslash//'),
        ))
        def test_url(self, get_last_directory_request, expected_url):
            assert_that(get_last_directory_request().url.raw_path, contains_string(expected_url))

    class TestNotFound:
        @pytest.fixture
        def response(self, mock_response_json):
            return mock_response_json(status=400, body={'code': 'not_found'})

        @pytest.mark.asyncio
        async def test_raises_not_found(self, request_coro):
            with pytest.raises(DirectoryNotFoundError):
                await request_coro


class TestDirectoryCreateUser:
    @pytest.fixture
    def login(self):
        return 'foobar'

    @pytest.fixture
    def password(self):
        return 'buzzword'

    @pytest.fixture
    def request_coro(self, directory_client, org_id, admin_uid, user_ip, login, password):
        return directory_client.create_user(login=login,
                                            password=password,
                                            password_mode=PasswordMode.PLAIN,
                                            org_id=org_id,
                                            birthday=date(2000, 1, 1),
                                            gender='male',
                                            language='ru',
                                            admin_uid=admin_uid,
                                            user_ip=user_ip)

    @pytest.fixture
    def directory_client(self, directory_client, mock_directory, response):
        mock_directory('/v11/users/', response)
        return directory_client

    class TestSuccess:
        @pytest.fixture
        def response(self, login):
            return {'id': 100, 'email': f'{login}@org.test'}

        @pytest.fixture(autouse=True)
        async def returned(self, request_coro):
            return await request_coro

        def test_returns_user(self, returned, login):
            assert_that(returned, equal_to(DirectoryUser(uid=100, email=f'{login}@org.test')))

        def test_headers(self, get_last_directory_request, org_id, admin_uid, user_ip):
            headers = get_last_directory_request().headers
            assert_that(headers['X-ORG-ID'], equal_to(str(org_id)))
            assert_that(headers['X-UID'], equal_to(str(admin_uid)))
            assert_that(headers['X-USER-IP'], equal_to(user_ip))

        @pytest.mark.asyncio
        async def test_json(self, get_last_directory_request, login, password):
            assert_that(
                await get_last_directory_request().json(),
                equal_to({
                    'nickname': login,
                    'password': password,
                    'password_mode': PasswordMode.PLAIN.value,
                    'department_id': 1,
                    'birthday': '2000-01-01',
                    'gender': 'male',
                    'language': 'ru',
                })
            )

    class TestAlreadyExists:
        @pytest.fixture
        def response(self, mock_response_json):
            return mock_response_json(status=400, body={'code': 'some_user_has_this_login'})

        @pytest.mark.asyncio
        async def test_raises_not_found(self, request_coro):
            with pytest.raises(DirectoryUserExistsError):
                await request_coro


class TestDirectoryGetOrganization:
    @pytest.fixture
    def request_coro(self, directory_client, org_id):
        return directory_client.get_organization(org_id)

    @pytest.fixture
    def directory_client(self, directory_client, mock_directory, org_id, response):
        mock_directory(f'/v11/organizations/{org_id}/', response)
        return directory_client

    class TestSuccess:
        @pytest.fixture
        def response(self, org_id):
            return {
                'id': org_id,
                'domains': {
                    'all': ['foo.test', 'bar.test', 'baz.test'],
                    'owned': ['foo.test', 'bar.test'],
                    'master': 'foo.test',
                    'display': 'foo.test',
                }
            }

        @pytest.fixture(autouse=True)
        async def returned(self, request_coro):
            return await request_coro

        def test_returns_org(self, returned, org_id):
            expected_org = DirectoryOrg(
                id=org_id,
                domains=DirectoryDomains(
                    all=['foo.test', 'bar.test', 'baz.test'],
                    owned=['foo.test', 'bar.test'],
                    master='foo.test',
                    display='foo.test',
                ),
            )
            assert_that(returned, equal_to(expected_org))

        def test_headers(self, get_last_directory_request, org_id, admin_uid, user_ip):
            headers = get_last_directory_request().headers
            assert_that(headers['X-ORG-ID'], equal_to(str(org_id)))

        def test_params(self, get_last_directory_request):
            assert_that(get_last_directory_request().query, equal_to({'fields': 'id,domains'}))

        def test_url(self, get_last_directory_request, org_id):
            assert_that(get_last_directory_request().url.raw_path, contains_string(f'v11/organizations/{org_id}/'))

    @pytest.mark.parametrize('response_data', (
        {'code': 'organization_deleted'}, {'code': 'unknown_organization'},
    ))
    class TestNotFound:
        @pytest.fixture
        def response(self, mock_response_json, response_data):
            return mock_response_json(status=400, body=response_data)

        @pytest.mark.asyncio
        async def test_raises_not_found(self, request_coro):
            with pytest.raises(DirectoryOrganizationNotFoundError):
                await request_coro
