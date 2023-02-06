from collections import defaultdict

import pytest

from sendr_utils import alist

from mail.beagle.beagle.core.entities.directory_organization import DirectoryOrganization
from mail.beagle.beagle.core.entities.unit import Unit
from mail.beagle.beagle.core.entities.user import User
from mail.beagle.beagle.interactions.directory.entities import (
    DirectoryDepartment, DirectoryGroup, DirectoryObjectType, DirectoryUser
)
from mail.beagle.beagle.tests.utils import dummy_coro_generator


@pytest.fixture
def create_directory_department(randn, rands):
    def _inner():
        return DirectoryDepartment(
            department_id=randn(),
            name=rands(),
            label=rands(),
            uid=randn(),
            parent_id=randn(),
        )

    return _inner


@pytest.fixture
def create_directory_group(randn, rands):
    def _inner():
        return DirectoryGroup(
            group_id=randn(),
            name=rands(),
            label=rands(),
            uid=randn(),
        )

    return _inner


@pytest.fixture
def create_directory_user(randn, rands):
    def _inner():
        return DirectoryUser(
            user_id=randn(),
            first_name=rands(),
            last_name=rands(),
            local_part=rands(),
            departments=[],
            groups=[],
        )

    return _inner


@pytest.fixture
def directory_client(mocker):
    return mocker.Mock()


@pytest.fixture
def directory_organization(org_id, directory_client):
    return DirectoryOrganization(org_id, directory_client)


def test_group_key(randn):
    group_id = randn()
    assert DirectoryOrganization.group_key(group_id) == Unit.get_external_key('group', str(group_id))


def test_department_key(randn):
    department_id = randn()
    assert DirectoryOrganization.department_key(department_id) \
        == Unit.get_external_key('department', str(department_id))


def test_unit_from_department(org_id, directory_organization, create_directory_department):
    department = create_directory_department()
    assert directory_organization._unit_from_department(department) == Unit(
        org_id=org_id,
        external_id=str(department.department_id),
        external_type='department',
        name=department.name,
        uid=department.uid,
        username=department.label,
    )


def test_unit_from_group(org_id, directory_organization, create_directory_group):
    group = create_directory_group()
    assert directory_organization._unit_from_group(group) == Unit(
        org_id=org_id,
        external_id=str(group.group_id),
        external_type='group',
        name=group.name,
        uid=group.uid,
        username=group.label,
    )


def test_user_from_directory_user(org_id, directory_organization, create_directory_user):
    user = create_directory_user()
    assert directory_organization._user_from_directory_user(user) == User(
        org_id=org_id,
        uid=user.user_id,
        username=user.local_part,
        first_name=user.first_name,
        last_name=user.last_name,
    )


class TestFetchDepartments:
    @pytest.fixture
    def departments(self, create_directory_department):
        departments = [create_directory_department() for _ in range(5)]
        all_department = departments[0]
        for department in departments[1:]:
            department.parent_id = all_department.department_id
        return departments

    @pytest.fixture
    def directory_client(self, mocker, departments):
        async def dummy_get_departments(org_id):
            for department in departments:
                yield department

        mock = mocker.Mock()
        mock.get_departments = mocker.Mock(side_effect=dummy_get_departments)
        return mock

    @pytest.fixture
    async def returned(self, directory_organization):
        return await directory_organization._fetch_departments()

    @pytest.mark.asyncio
    async def test_caches_result(self, org_id, directory_client, directory_organization):
        for _ in range(3):
            await directory_organization._fetch_departments()
        directory_client.get_departments.assert_called_once_with(org_id)

    def test_fetches_department_units(self, departments, directory_organization, returned):
        assert directory_organization._department_units == [
            directory_organization._unit_from_department(department)
            for department in departments
        ]

    def test_fetches_department_graph(self, departments, directory_organization, returned):
        expected = defaultdict(set)
        for department in departments:
            expected[('department', str(department.parent_id))].add(('department', str(department.department_id)))
        assert directory_organization._graph == expected


class TestFetchGroups:
    @pytest.fixture
    def groups(self, create_directory_group):
        return [create_directory_group() for _ in range(5)]

    @pytest.fixture
    def group_members(self, randn, groups):
        return {
            group.group_id: [
                (type_, randn())
                for _ in range(2)
                for type_ in (DirectoryObjectType.DEPARTMENT, DirectoryObjectType.GROUP)
            ]
            for group in groups
        }

    @pytest.fixture
    def directory_client(self, mocker, groups, group_members):
        async def dummy_get_groups(org_id):
            for group in groups:
                yield group

        async def dummy_get_group_members(org_id, group_id):
            for item in group_members.get(group_id, []):
                yield item

        mock = mocker.Mock()
        mock.get_groups = mocker.Mock(side_effect=dummy_get_groups)
        mock.get_group_members = mocker.Mock(side_effect=dummy_get_group_members)
        return mock

    @pytest.fixture
    async def returned(self, directory_organization):
        return await directory_organization._fetch_groups()

    @pytest.mark.asyncio
    async def test_caches_get_groups_result(self, org_id, directory_client, directory_organization):
        for _ in range(3):
            await directory_organization._fetch_groups()
        directory_client.get_groups.assert_called_once_with(org_id)

    @pytest.mark.asyncio
    async def test_caches_get_group_members_result(self,
                                                   mocker,
                                                   org_id,
                                                   groups,
                                                   directory_client,
                                                   directory_organization,
                                                   ):
        for _ in range(3):
            await directory_organization._fetch_groups()
        assert directory_client.get_group_members.call_args_list == [
            mocker.call(org_id, group.group_id)
            for group in groups
        ]

    def test_fetches_group_units(self, groups, directory_organization, returned):
        assert directory_organization._group_units == [
            directory_organization._unit_from_group(group)
            for group in groups
        ]

    def test_fetches_group_graph(self, groups, group_members, directory_organization, returned):
        expected = {}
        for group_id, members in group_members.items():
            graph_members = expected[('group', str(group_id))] = set()
            for member_type, member_id in members:
                graph_members.add((member_type.value, str(member_id)))
        assert directory_organization._graph == expected


class TestFetchGraph:
    @pytest.fixture
    def departments_graph(self):
        return {
            1: {2},
            2: {3},
            3: set(),
        }

    @pytest.fixture
    def groups_graph(self):
        return {
            4: {1},
            5: set(),
        }

    @pytest.fixture
    def expected_graph(self):
        return {
            1: {2, 3},
            2: {3},
            3: set(),
            4: {1, 2, 3},
            5: set(),
        }

    @pytest.fixture
    def directory_organization(self, mocker, directory_organization, departments_graph, groups_graph):
        async def dummy_fetch_departments():
            directory_organization._graph.update(departments_graph)

        async def dummy_fetch_groups():
            directory_organization._graph.update(groups_graph)

        mocker.patch.object(
            directory_organization,
            '_fetch_departments',
            mocker.Mock(side_effect=dummy_fetch_departments)
        )
        mocker.patch.object(
            directory_organization,
            '_fetch_groups',
            mocker.Mock(side_effect=dummy_fetch_groups)
        )
        return directory_organization

    @pytest.fixture
    async def returned(self, directory_organization):
        return await directory_organization._fetch_graph()

    @pytest.fixture
    async def returned_multiple(self, directory_organization):
        return [
            await directory_organization._fetch_graph()
            for _ in range(3)
        ]

    def test_caches_fetch_departments(self, directory_organization, returned_multiple):
        directory_organization._fetch_departments.assert_called_once()

    def test_caches_fetch_grops(self, directory_organization, returned_multiple):
        directory_organization._fetch_groups.assert_called_once()

    def test_fetches_merges_expends_graph(self, expected_graph, directory_organization, returned):
        assert directory_organization._graph == expected_graph


class TestFetchUsers:
    @pytest.fixture
    def users(self, create_directory_user, randn):
        users = [create_directory_user() for _ in range(5)]
        for n, user in enumerate(users):
            user.departments = list(range(n, 6))
            user.groups = list(range(n, 6))
        return users

    @pytest.fixture
    def directory_client(self, mocker, users):
        async def dummy_get_users(org_id):
            for user in users:
                yield user

        mock = mocker.Mock()
        mock.get_users = mocker.Mock(side_effect=dummy_get_users)
        return mock

    @pytest.fixture
    async def returned(self, directory_organization):
        return await directory_organization._fetch_users()

    @pytest.mark.asyncio
    async def test_caches_get_users_result(self, org_id, directory_client, directory_organization):
        for _ in range(3):
            await directory_organization._fetch_users()
        directory_client.get_users.assert_called_once_with(org_id)

    def test_fetches_users(self, users, directory_organization, returned):
        assert directory_organization._users == [
            directory_organization._user_from_directory_user(user)
            for user in users
        ]

    def test_fetches_department_users(self, users, directory_organization, returned):
        expected = defaultdict(set)
        for user in users:
            for department_id in user.departments:
                expected[('department', str(department_id))].add(user.user_id)
        assert directory_organization._department_users == expected

    def test_fetches_group_users(self, users, directory_organization, returned):
        expected = defaultdict(set)
        for user in users:
            for group_id in user.groups:
                expected[('group', str(group_id))].add(user.user_id)
        assert directory_organization._group_users == expected


class TestGetRevision:
    @pytest.fixture
    def revision(self, randn):
        return randn()

    @pytest.fixture
    def directory_client(self, mocker, revision):
        mock = mocker.Mock()
        mock.get_organization_revision = mocker.Mock(side_effect=dummy_coro_generator(revision))
        return mock

    @pytest.fixture
    async def returned(self, directory_organization):
        return await directory_organization.get_revision()

    def test_returned(self, revision, returned):
        assert returned == revision

    def test_directory_client_call(self, directory_client, org_id, returned):
        directory_client.get_organization_revision.assert_called_once_with(org_id)


class TestGetUnits:
    @pytest.fixture
    def department_units(self):
        return [1, 2, 3]

    @pytest.fixture
    def group_units(self):
        return [4, 5, 6]

    @pytest.fixture
    def directory_organization(self, mocker, directory_organization, department_units, group_units):
        async def dummy_fetch_departments():
            directory_organization._department_units = department_units

        async def dummy_fetch_groups():
            directory_organization._group_units = group_units

        mocker.patch.object(
            directory_organization,
            '_fetch_departments',
            mocker.Mock(side_effect=dummy_fetch_departments)
        )
        mocker.patch.object(
            directory_organization,
            '_fetch_groups',
            mocker.Mock(side_effect=dummy_fetch_groups)
        )
        return directory_organization

    @pytest.fixture
    async def returned(self, directory_organization):
        return await alist(directory_organization.get_units())

    def test_returned(self, department_units, group_units, returned):
        assert returned == department_units + group_units


class TestGetUnitUnits:
    @pytest.fixture
    def graph(self):
        return {
            1: {2, 3},
            2: {3},
            3: set(),
        }

    @pytest.fixture
    def directory_organization(self, mocker, directory_organization, graph):
        async def dummy_fetch_graph():
            directory_organization._graph = graph

        mocker.patch.object(
            directory_organization,
            '_fetch_graph',
            mocker.Mock(side_effect=dummy_fetch_graph)
        )
        return directory_organization

    @pytest.fixture
    async def returned(self, directory_organization):
        return await alist(directory_organization.get_unit_units())

    def test_returned(self, graph, returned):
        expected = list(graph.items())
        assert returned == expected \
            and all((
                returned_item[1] is not expected_item[1]
                for returned_item, expected_item in zip(returned, expected)
            ))


class TestGetUsers:
    @pytest.fixture
    def users(self):
        return [1, 2, 3]

    @pytest.fixture
    def directory_organization(self, mocker, directory_organization, users):
        async def dummy_fetch_users():
            directory_organization._users = users

        mocker.patch.object(
            directory_organization,
            '_fetch_users',
            mocker.Mock(side_effect=dummy_fetch_users),
        )
        return directory_organization

    @pytest.fixture
    async def returned(self, directory_organization):
        return await alist(directory_organization.get_users())

    def test_returned(self, users, returned):
        assert returned == users


class TestGetUnitUsers:
    @pytest.fixture
    def department_users(self):
        return {
            'dep1': {1, 2},
            'dep2': {3, 4},
        }

    @pytest.fixture
    def group_users(self):
        return {
            'gr1': {5, 6},
            'gr2': {7, 8},
        }

    @pytest.fixture
    def directory_organization(self, mocker, directory_organization, department_users, group_users):
        async def dummy_fetch_users():
            directory_organization._department_users = department_users
            directory_organization._group_users = group_users

        mocker.patch.object(
            directory_organization,
            '_fetch_users',
            mocker.Mock(side_effect=dummy_fetch_users),
        )
        return directory_organization

    @pytest.fixture
    async def returned(self, directory_organization):
        return await alist(directory_organization.get_unit_users())

    def test_returned(self, department_users, group_users, returned):
        expected = list(department_users.items()) + list(group_users.items())
        assert returned == expected \
            and all((
                returned_item[1] is not expected_item[1]
                for returned_item, expected_item in zip(returned, expected)
            ))
