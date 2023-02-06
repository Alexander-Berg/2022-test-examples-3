from unittest.mock import Mock

import pytest

from sendr_taskqueue.worker.storage import TaskState
from sendr_utils import alist

from hamcrest import (
    all_of, assert_that, contains_inanyorder, has_entries, has_items, has_length, has_properties, instance_of,
    match_equality
)

from mail.beagle.beagle.conf import settings
from mail.beagle.beagle.core.actions.smtp_cache import GenerateSMTPCacheAction
from mail.beagle.beagle.core.actions.sync.sync_organization import (
    QueueSyncCurrentOrganizationAction, SyncCurrentOrganizationAction, SyncDirectoryOrganizationAction,
    SyncOrganizationAction
)
from mail.beagle.beagle.core.actions.sync.sync_unit_members import SyncUnitMembersAction
from mail.beagle.beagle.core.actions.sync.sync_units import SyncUnitsAction
from mail.beagle.beagle.core.actions.sync.sync_users import SyncUsersAction
from mail.beagle.beagle.core.entities.directory_organization import DirectoryOrganization
from mail.beagle.beagle.core.entities.enums import TaskType
from mail.beagle.beagle.core.entities.external_organization import BaseExternalOrganization
from mail.beagle.beagle.core.entities.organization import Organization
from mail.beagle.beagle.core.entities.unit import Unit
from mail.beagle.beagle.core.entities.user import User
from mail.beagle.beagle.core.exceptions import DirectoryOrgNotFoundError
from mail.beagle.beagle.interactions.directory.exceptions import OrganizationDeletedError, UnknownOrganizationError
from mail.beagle.beagle.storage.exceptions import OrganizationNotFound
from mail.beagle.beagle.tests.utils import dummy_async_function


@pytest.fixture
def external_revision(org):
    return org.revision + 1


@pytest.fixture
def external_organization(external_revision):
    mock = Mock(spec=BaseExternalOrganization)
    mock.get_revision = dummy_async_function(external_revision)
    return mock


@pytest.mark.asyncio
class TestSyncOrganizationAction:
    @pytest.fixture
    def users(self, org, randn, rands):
        return [
            User(org_id=org.org_id, uid=randn(), username=rands(), first_name=rands(), last_name=rands())
            for _ in range(randn(max=10))
        ]

    @pytest.fixture
    def units(self, org, randn, rands):
        return [
            Unit(org_id=org.org_id, external_id=rands(), external_type=rands(), name=rands(), uid=randn())
            for _ in range(randn(max=10))
        ]

    @pytest.fixture
    def user_affected_uids(self, users, randslice):
        return set(randslice([user.uid for user in users]))

    @pytest.fixture
    def unit_affected_uids(self, units, randslice):
        return set(randslice([unit.uid for unit in units]))

    @pytest.fixture
    def unit_member_affected_uids(self, randn):
        return set(randn() for _ in range(randn(max=10)))

    @pytest.fixture(autouse=True)
    def action_sync_units(self, mock_action, units, unit_affected_uids):
        return mock_action(SyncUnitsAction, (units, unit_affected_uids))

    @pytest.fixture(autouse=True)
    def action_sync_users(self, mock_action, users, user_affected_uids):
        return mock_action(SyncUsersAction, (users, user_affected_uids))

    @pytest.fixture(autouse=True)
    def action_sync_unit_members(self, mock_action, unit_member_affected_uids):
        return mock_action(SyncUnitMembersAction, unit_member_affected_uids)

    @pytest.fixture(autouse=True)
    def action_generate_smtp_cache(self, mock_action):
        return mock_action(GenerateSMTPCacheAction)

    @pytest.fixture
    def force(self):
        return False

    @pytest.fixture
    def returned_func(self, org, external_organization, force):
        async def _inner():
            return await SyncOrganizationAction(org.org_id, external_organization, force).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.asyncio
    class TestSyncActionsCall:
        @pytest.fixture(params=(True, False), ids=('force', 'no-force'))
        def force(self, request):
            return request.param

        @pytest.fixture(params=(True, False))
        def external_revision(self, request, force, org):
            if not force or not request.param:
                return org.revision + 1
            else:
                return 0

        async def test_sync_units(self, returned, org, external_organization, action_sync_units):
            action_sync_units.assert_called_once_with(org_id=org.org_id, external_organization=external_organization)

        async def test_sync_users(self, returned, org, external_organization, action_sync_users):
            action_sync_users.assert_called_once_with(org_id=org.org_id, external_organization=external_organization)

        async def test_sync_unit_members(self, returned, org, external_organization, action_sync_unit_members, users,
                                         units):
            action_sync_unit_members.assert_called_once_with(org_id=org.org_id,
                                                             external_organization=external_organization,
                                                             users=users,
                                                             units=units)

        async def test_cache(self, returned, action_generate_smtp_cache, org, user_affected_uids, unit_affected_uids,
                             unit_member_affected_uids):
            uids = list(set.union(user_affected_uids, unit_affected_uids, unit_member_affected_uids))
            action_generate_smtp_cache.assert_called_once_with(org_id=org.org_id,
                                                               uids=match_equality(contains_inanyorder(*uids)))

    @pytest.mark.asyncio
    class TestOrgCreation:
        @pytest.fixture
        def org(self, randn):
            return Organization(org_id=randn())

        async def test_org_creating(self, storage, org, returned_func):
            with pytest.raises(OrganizationNotFound):
                await storage.organization.get(org_id=org.org_id)
            await returned_func()
            created = await storage.organization.get(org_id=org.org_id)
            assert created.org_id == org.org_id

    @pytest.mark.parametrize('external_revision', (0,))
    async def test_older_external_revision(self, returned, action_sync_units, action_sync_users,
                                           action_sync_unit_members):
        for action in (action_sync_units, action_sync_users, action_sync_unit_members):
            action.assert_not_called()

    async def test_update_revision(self, org, storage, returned):
        updated = await storage.organization.get(org_id=org.org_id)
        assert org.revision + 1 == updated.revision


@pytest.mark.asyncio
class TestSyncCurrentOrganizationAction:
    @pytest.fixture(autouse=True)
    def action_sync_organization(self, mock_action, randn):
        return mock_action(SyncDirectoryOrganizationAction)

    @pytest.fixture(params=(True, False))
    def force(self, request):
        return request.param

    @pytest.fixture
    def returned_func(self, org, force):
        async def _inner():
            return await SyncCurrentOrganizationAction(org.org_id, force).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    async def test_call(self, org, force, returned, external_organization, action_sync_organization):
        action_sync_organization.assert_called_once_with(
            org_id=org.org_id,
            force=force,
            external_organization=match_equality(instance_of(DirectoryOrganization))
        )


@pytest.mark.asyncio
class TestSyncDirectoryAction:
    @pytest.fixture(autouse=True)
    def action_sync_organization(self, external_organization, mock_action, randn):
        return mock_action(SyncOrganizationAction)

    @pytest.fixture(params=(True, False))
    def force(self, request):
        return request.param

    @pytest.fixture
    def returned_func(self, external_organization, org, force):
        async def _inner():
            return await SyncDirectoryOrganizationAction(org.org_id, external_organization, force).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    async def test_call(self, org, force, returned, external_organization, action_sync_organization):
        action_sync_organization.assert_called_once_with(
            org_id=org.org_id,
            force=force,
            external_organization=external_organization
        )

    @pytest.mark.asyncio
    class TestUnknownOrg:
        @pytest.fixture
        def external_organization(self, org):
            mock = Mock(spec=BaseExternalOrganization)
            mock.get_revision = Mock(
                side_effect=UnknownOrganizationError(method='get', message='message'))
            return mock

        async def test_unknown_directory_org(self, org, external_organization, returned_func, storage):
            with pytest.raises(DirectoryOrgNotFoundError):
                await returned_func()

    @pytest.mark.asyncio
    class TestDeletedOrg:
        @pytest.fixture
        def external_organization(self, org):
            mock = Mock(spec=BaseExternalOrganization)
            mock.get_revision = Mock(
                side_effect=OrganizationDeletedError(method='get', message=f'Organization {org.org_id} was deleted'))
            return mock

        async def test_org_deleted(self, org, external_organization, returned, storage):
            with pytest.raises(OrganizationNotFound):
                await storage.organization.get(org.org_id)


@pytest.mark.asyncio
class TestQueueSyncCurrentOrganizationAction:
    @pytest.fixture(params=(True, False))
    def force(self, request):
        return request.param

    @pytest.fixture
    def returned_func(self, org_id, force):
        async def _inner():
            return await QueueSyncCurrentOrganizationAction(org_id=org_id, force=force).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    async def test_task_create(self, org, returned, force, storage):
        assert_that(
            await alist(storage.task.find()),
            all_of(
                has_length(1),
                has_items(
                    has_properties({
                        'task_type': TaskType.SYNC_ORGANIZATION,
                        'state': TaskState.PENDING,
                        'action_name': None,
                        'org_id': org.org_id,
                        'params': has_entries({
                            'org_id': org.org_id,
                            'force': force,
                        })
                    }))
            )
        )

    async def test_unknown_org(self, org_id, storage, returned_func):
        with pytest.raises(OrganizationNotFound):
            await storage.organization.get(org_id)
        await returned_func()
        await storage.organization.get(org_id)

    @pytest.mark.asyncio
    class TestIgnoredOrganizations:
        @pytest.fixture(autouse=True)
        def setup(self, org):
            settings.DIRECTORY_IGNORED_ORGANIZATIONS = (org.org_id,)

        async def test_ignored(self, org, storage, returned):
            tasks = await alist(storage.task.find())
            assert len(tasks) == 0
