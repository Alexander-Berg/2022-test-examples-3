from itertools import groupby

import pytest

from hamcrest import assert_that, contains_inanyorder

from mail.beagle.beagle.core.actions.sync.sync_unit_members import GetDependentUIDsUnitAction, SyncUnitMembersAction
from mail.beagle.beagle.tests.utils import dummy_coro_generator

from .base import BaseTestSyncAction


class TestSyncUnitMembersAction(BaseTestSyncAction):
    @pytest.fixture
    def dependent_uids(self, randn):
        return {randn() for _ in range(5)}

    @pytest.fixture(autouse=True)
    def get_dependent_uids_mock(self, mock_action, dependent_uids):
        return mock_action(GetDependentUIDsUnitAction, dependent_uids)

    @pytest.fixture(autouse=True)
    def sync_unit_users_mock(self, mocker, generate_set_of_numbers):
        return mocker.patch.object(
            SyncUnitMembersAction,
            'sync_unit_users',
            mocker.Mock(side_effect=generate_set_of_numbers),
        )

    @pytest.fixture(autouse=True)
    def sync_unit_units_mock(self, mocker, generate_set_of_numbers):
        return mocker.patch.object(
            SyncUnitMembersAction,
            'sync_unit_units',
            mocker.Mock(side_effect=generate_set_of_numbers),
        )

    @pytest.fixture
    def returned_func(self, mocker, org_id):
        async def _inner():
            return await SyncUnitMembersAction(
                org_id=org_id,
                external_organization=mocker.Mock(),
                units=[],
                users=[]
            ).run()

        return _inner

    def test_returns_dependent_uids(self, dependent_uids, returned):
        assert returned == dependent_uids

    def test_get_dependent_uids_call(self,
                                     org_id,
                                     returned,
                                     all_generated_numbers,
                                     get_dependent_uids_mock,
                                     ):
        get_dependent_uids_mock.assert_called_once_with(
            org_id=org_id,
            unit_ids=all_generated_numbers,
        )


class TestSyncOneUnitMembers:
    IDS_PARAMS = pytest.mark.parametrize('stored_ids,external_ids', (
        pytest.param(set(), set(), id='empty_stored_external'),
        pytest.param({1}, set(), id='empty_external'),
        pytest.param(set(), {1}, id='empty_stored'),
        pytest.param({1, 2, 3}, {1, 2, 4}, id='not_empty'),
    ))

    @pytest.fixture
    def stored_members(self, stored_ids):
        return [{'id': id_, 'type': 'stored'} for id_ in stored_ids]

    @pytest.fixture
    def external_members(self, external_ids):
        return [{'id': id_, 'type': 'external'} for id_ in external_ids]

    @pytest.fixture
    def stored_to_external(self):
        def _inner(stored):
            return {**stored, 'type': 'external'}

        return _inner

    @pytest.fixture
    def create_func(self, mocker):
        return mocker.Mock(side_effect=dummy_coro_generator())

    @pytest.fixture
    def delete_func(self, mocker):
        return mocker.Mock(side_effect=dummy_coro_generator())

    @pytest.fixture
    def returned_func(self, unit, stored_members, external_members, stored_to_external, create_func, delete_func):
        async def _inner():
            return await SyncUnitMembersAction._sync_one_unit_members(
                unit=unit,
                stored_members=stored_members,
                external_members=external_members,
                stored_to_external=stored_to_external,
                create_func=create_func,
                delete_func=delete_func,
            )

        return _inner

    @IDS_PARAMS
    def test_create_calls(self, mocker, unit, stored_ids, external_ids, create_func, returned):
        assert_that(
            create_func.call_args_list,
            contains_inanyorder(*[
                mocker.call(unit, {'id': id_, 'type': 'external'})
                for id_ in external_ids.difference(stored_ids)
            ])
        )

    @IDS_PARAMS
    def test_delete_calls(self, mocker, unit, stored_ids, external_ids, delete_func, returned):
        assert_that(
            delete_func.call_args_list,
            contains_inanyorder(*[
                mocker.call({'id': id_, 'type': 'stored'})
                for id_ in stored_ids.difference(external_ids)
            ])
        )

    @IDS_PARAMS
    def test_returns_changed(self, stored_ids, external_ids, returned):
        assert returned == (stored_ids != external_ids)


class BaseTestSyncUnitMembersMethod:
    @pytest.fixture
    async def units(self, org, create_unit):
        return [await create_unit(org_id=org.org_id) for _ in range(5)]

    @pytest.fixture
    def changed_units(self, units):
        return [units[1], units[2], units[4]]

    @pytest.fixture
    async def users(self, org, create_user):
        return [await create_user(org_id=org.org_id) for _ in range(5)]

    @pytest.fixture(autouse=True)
    def sync_one_unit_members_mock(self, mocker, changed_units):
        async def dummy_sync(unit, *args, **kwargs):
            return unit in changed_units

        return mocker.patch.object(
            SyncUnitMembersAction,
            '_sync_one_unit_members',
            mocker.Mock(side_effect=dummy_sync)
        )

    @pytest.fixture
    def action(self, storage, org, external_organization, units, users):
        action = SyncUnitMembersAction(
            org_id=org.org_id,
            external_organization=external_organization,
            units=units,
            users=users,
        )
        action.context.storage = storage
        yield action
        action.context.storage = None

    def test_sync_one_unit_members_calls(self, units, returned, sync_one_unit_members_mock, get_expected_call):
        assert_that(
            sync_one_unit_members_mock.call_args_list,
            contains_inanyorder(*[
                get_expected_call(unit)
                for unit in units
            ])
        )

    def test_returns_changed(self, changed_units, returned):
        assert returned == {unit.unit_id for unit in changed_units}


class TestSyncUnitUsers(BaseTestSyncUnitMembersMethod):
    @pytest.fixture(autouse=True)
    async def setup(self, create_unit_user_entity, create_unit_user, units, users):
        external_unit_users = []
        unit_users = []
        all_unit, rest_units = units[0], units[1:]
        for user in users:
            external_unit_users.append((all_unit, user.uid))
            unit_users.append(await create_unit_user(unit=all_unit, user=user))
        for unit, user in zip(rest_units, users):
            external_unit_users.append((unit, user.uid))
            unit_users.append(await create_unit_user(unit=unit, user=user))
        return external_unit_users, unit_users

    @pytest.fixture
    def external_unit_users(self, setup):
        return setup[0]

    @pytest.fixture
    def unit_users(self, setup):
        return setup[1]

    @pytest.fixture
    def external_organization(self, mocker, units, external_unit_users):
        async def dummy_get_unit_users():
            for unit, members in groupby(external_unit_users, key=lambda item: item[0]):
                yield unit.external_key, {uid for _, uid in members}

        mock = mocker.Mock()
        mock.get_unit_users = mocker.Mock(side_effect=dummy_get_unit_users)
        return mock

    @pytest.fixture
    def returned_func(self, action):
        async def _inner():
            return await action.sync_unit_users()

        return _inner

    @pytest.fixture
    def get_expected_call(self, mocker, storage, action, unit_users, external_unit_users):
        def _inner(unit):
            return mocker.call(
                unit=unit,
                stored_members=[
                    unit_user
                    for unit_user in unit_users
                    if unit_user.unit_id == unit.unit_id
                ],
                external_members={
                    uid
                    for external_unit, uid in external_unit_users
                    if external_unit == unit
                },
                stored_to_external=action._unit_user_to_external,
                create_func=action._create_unit_user,
                delete_func=storage.unit_user.delete,
            )

        return _inner


class TestSyncUnitUnits(BaseTestSyncUnitMembersMethod):
    @pytest.fixture(autouse=True)
    async def setup(self, create_unit_unit, units):
        external_unit_units = []
        unit_units = []
        all_unit, rest_units = units[0], units[1:]
        for unit in rest_units:
            external_unit_units.append((all_unit, unit))
            unit_units.append(
                await create_unit_unit(
                    org_id=all_unit.org_id,
                    unit_id=unit.unit_id,
                    parent_unit_id=all_unit.unit_id,
                )
            )
        return external_unit_units, unit_units

    @pytest.fixture
    def external_unit_units(self, setup):
        return setup[0]

    @pytest.fixture
    def unit_units(self, setup):
        return setup[1]

    @pytest.fixture
    def external_organization(self, mocker, external_unit_units):
        async def dummy_get_unit_units():
            for parent, parent_child_pairs in groupby(external_unit_units, key=lambda item: item[0]):
                yield parent.external_key, {child.external_key for _, child in parent_child_pairs}

        mock = mocker.Mock()
        mock.get_unit_units = mocker.Mock(side_effect=dummy_get_unit_units)
        return mock

    @pytest.fixture
    def returned_func(self, action):
        async def _inner():
            return await action.sync_unit_units()

        return _inner

    @pytest.fixture
    def get_expected_call(self, mocker, storage, action, unit_units, external_unit_units):
        def _inner(unit):
            return mocker.call(
                unit=unit,
                stored_members=[
                    unit_unit
                    for unit_unit in unit_units
                    if unit_unit.parent_unit_id == unit.unit_id
                ],
                external_members={
                    child.external_key
                    for parent, child in external_unit_units
                    if parent == unit
                },
                stored_to_external=action._unit_unit_to_external,
                create_func=action._create_unit_unit,
                delete_func=storage.unit_unit.delete,
            )

        return _inner
