from itertools import chain

import pytest

from hamcrest import assert_that, contains, equal_to, has_entries, has_properties

from mail.beagle.beagle.interactions.directory import InvalidMasterDomainError
from mail.beagle.beagle.interactions.directory.entities import (
    DirectoryDepartment, DirectoryGroup, DirectoryObjectType, DirectoryUser
)
from mail.beagle.beagle.tests.utils import dummy_coro


class BaseTestCollectionGetter:
    @pytest.fixture
    def collection_objects(self):
        return []

    @pytest.fixture(autouse=True)
    def get_collection_mock(self, mocker, clients, collection_objects):
        async def _inner():
            for obj in collection_objects:
                yield obj

        return mocker.patch.object(
            clients.directory,
            '_get_collection',
            mocker.Mock(return_value=_inner()),
        )

    @pytest.fixture
    def method_name(self):
        pass

    @pytest.fixture
    def url(self):
        pass

    @pytest.fixture
    def fields(self):
        pass

    def test_call(self,
                  clients,
                  org_id,
                  get_collection_mock,
                  returned,
                  method_name,
                  url,
                  fields,
                  ):
        get_collection_mock.assert_called_once_with(
            method_name,
            url,
            org_id=org_id,
            params={
                'per_page': clients.directory._max_per_page,
                'fields': fields,
            }
        )

    def test_returned(self, returned, parsed_objects):
        assert returned == parsed_objects


class TestGetCollection:
    @pytest.fixture
    def urls(self):
        return [f'/dummy_url/{i}' for i in range(5)]

    @pytest.fixture
    def collection_objects(self, urls):
        return [
            [f'dummy-object-{i}-{j}' for j in range(3)]
            for i in range(len(urls))
        ]

    @pytest.fixture
    def interaction_method(self):
        return 'dummy-interaction-method'

    @pytest.fixture
    def response_pages(self, directory_host, urls, collection_objects):
        return [
            {
                'links': {'next': 'https://' + directory_host + url} if url else {},
                'result': result,
            }
            for url, result in zip(chain(urls[1:], (None,)), collection_objects)
        ]

    @pytest.fixture
    def response_json_generator(self, response_pages):
        return (dummy_coro(page) for page in response_pages)

    @pytest.fixture
    def params(self):
        return {'some_key': 'some_value'}

    @pytest.fixture
    async def returned(self, directory_host, mock_directory, clients, interaction_method, urls, response_pages, params):
        for url, page in zip(urls, response_pages):
            mock_directory(url, page)

        return [
            item
            async for item in clients.directory._get_collection(
                interaction_method,
                'https://' + directory_host + urls[0],
                params=params,
            )
        ]

    def test_requests(self, urls, returned, directory_requests, params):
        assert_that(
            directory_requests,
            contains(*[
                has_properties({
                    'method': 'GET',
                    'path': url,
                    'query': params,
                })
                for url in urls
            ])
        )

    def test_returns_all_objects(self, returned, collection_objects):
        assert returned == list(chain(*collection_objects))


class TestGetDepartments(BaseTestCollectionGetter):
    @pytest.fixture
    def collection_objects(self):
        return [
            {
                'id': 5,
                'parent_id': 4,
                'uid': 113,
                'name': 'some text',
                'label': 'some-email',
            },
        ]

    @pytest.fixture
    def parsed_objects(self):
        return [
            DirectoryDepartment(
                department_id=5,
                name='some text',
                label='some-email',
                uid=113,
                parent_id=4,
            ),
        ]

    @pytest.fixture
    def method_name(self):
        return 'get_departments'

    @pytest.fixture
    def url(self, directory_host):
        return 'https://' + directory_host + '/v11/departments/'

    @pytest.fixture
    def fields(self):
        return 'id,name,label,uid,parent_id'

    @pytest.fixture
    async def returned(self, clients, org_id):
        return [department async for department in clients.directory.get_departments(org_id)]


class TestGetGroupMembers:
    @pytest.fixture
    def group_id(self, randn):
        return randn()

    @pytest.fixture
    def response_json(self, randn):
        return [
            {'type': 'user', 'object': {'id': randn()}},
            {'type': 'group', 'object': {'id': randn()}},
            {'type': 'department', 'object': {'id': randn()}},
        ]

    @pytest.fixture
    async def returned(self, mock_directory, clients, org_id, group_id, response_json):
        mock_directory(f'/v11/groups/{group_id}/members/', response_json)
        return [obj async for obj in clients.directory.get_group_members(org_id, group_id)]

    def test_request(self, clients, org_id, group_id, returned, last_directory_request):
        assert_that(
            last_directory_request(),
            has_properties({
                'method': 'GET',
                'path': f'/v11/groups/{group_id}/members/',
                'headers': has_entries({'X-ORG-ID': str(org_id)}),
                'query': {'per_page': str(clients.directory._max_per_page)},
            }),
        )

    def test_returned(self, response_json, returned):
        assert returned == [
            (DirectoryObjectType(member_json['type']), member_json['object']['id'])
            for member_json in response_json
        ]

    @pytest.mark.parametrize('response_json', ([{'type': 'some-new-type', 'object': {'id': 1234}}],))
    def test_skips_unknown_types(self, returned):
        assert returned == []


class TestGetGroups(BaseTestCollectionGetter):
    @pytest.fixture
    def collection_objects(self):
        return [
            {
                'id': 10,
                'name': 'test team',
                'label': 'test-team-email',
                'uid': 113,
            }
        ]

    @pytest.fixture
    def parsed_objects(self):
        return [
            DirectoryGroup(
                group_id=10,
                name='test team',
                label='test-team-email',
                uid=113,
            ),
        ]

    @pytest.fixture
    def method_name(self):
        return 'get_groups'

    @pytest.fixture
    def url(self, directory_host):
        return 'https://' + directory_host + '/v11/groups/'

    @pytest.fixture
    def fields(self):
        return 'id,name,label,uid'

    @pytest.fixture
    async def returned(self, clients, org_id):
        return [group async for group in clients.directory.get_groups(org_id)]


class TestGetOrganizationRevision:
    @pytest.fixture
    def revision(self, randn):
        return randn()

    @pytest.fixture
    def response_json(self, revision):
        return {'revision': revision}

    @pytest.fixture
    async def returned(self, clients, mock_directory, org_id, response_json):
        mock_directory(f'/v11/organizations/{org_id}/', response_json)
        return await clients.directory.get_organization_revision(org_id)

    def test_call(self, returned, last_directory_request):
        assert_that(
            last_directory_request(),
            has_properties({
                'method': 'GET',
                'query': {'fields': 'revision'},
            })
        )

    def test_returns_revision(self, revision, returned):
        assert returned == revision


class TestGetUsers(BaseTestCollectionGetter):
    @pytest.fixture
    def name(self):
        return {'first': 'a', 'last': 'b'}

    @pytest.fixture
    def collection_objects(self, name):
        return [
            {
                'id': 113,
                'email': 'test@ya.ru',
                'name': name,
                'departments': [{'id': 1}, {'id': 2}],
                'groups': [{'id': 3}],
            },
        ]

    @pytest.fixture
    def parsed_objects(self):
        return [
            DirectoryUser(
                user_id=113,
                local_part='test',
                first_name='a',
                last_name='b',
                departments=[1, 2],
                groups=[3],
            )
        ]

    @pytest.fixture
    def method_name(self):
        return 'get_users'

    @pytest.fixture
    def url(self, directory_host):
        return 'https://' + directory_host + '/v11/users/'

    @pytest.fixture
    def fields(self):
        return 'id,name,email,departments,groups'

    @pytest.fixture
    async def returned(self, clients, org_id):
        return [user async for user in clients.directory.get_users(org_id)]


class TestGetEmptyLastName(TestGetUsers):
    @pytest.fixture
    def name(self):
        return {'first': 'a'}

    @pytest.fixture
    def parsed_objects(self):
        return [
            DirectoryUser(
                user_id=113,
                local_part='test',
                first_name='a',
                last_name='',
                departments=[1, 2],
                groups=[3],
            )
        ]


@pytest.mark.asyncio
class TestGetMasterDomain:
    @pytest.fixture
    def org_id(self, randn):
        return randn()

    @pytest.fixture
    def response(self, rands):
        return [{'master': True, 'name': rands()}]

    @pytest.fixture
    def returned_func(self, org_id, response, clients, mock_directory):
        async def _inner():
            mock_directory('/v11/domains/', response)
            return await clients.directory.get_master_domain(org_id)

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    async def test_params(self, returned, org_id, last_directory_request, response):
        request = last_directory_request()

        assert_that(
            (request, returned),
            contains(
                has_properties({'headers': has_entries({'X-ORG-ID': str(org_id)})}),
                equal_to(response[0]['name'])
            )
        )

    @pytest.mark.parametrize('response', ([],))
    async def test_invalid_master_domain(self, returned_func):
        with pytest.raises(InvalidMasterDomainError):
            await returned_func()
