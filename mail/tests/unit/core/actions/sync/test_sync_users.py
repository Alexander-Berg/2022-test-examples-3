from itertools import chain

import pytest

from hamcrest import assert_that, contains_inanyorder

from mail.beagle.beagle.core.actions.sync.sync_users import (
    CreateUserAction, DeleteUserAction, SyncUsersAction, UpdateUserAction
)

from .base import BaseTestSyncAction


class TestSyncUsersAction(BaseTestSyncAction):
    @pytest.fixture
    def uid_ranges(self):
        uids_per_range = 5
        return (range(i * uids_per_range, (i + 1) * uids_per_range) for i in range(3))

    @pytest.fixture(autouse=True)
    async def users_for_create(self, storage, org, create_user_entity, uid_ranges):
        return [
            (create_user_entity(org_id=org.org_id, uid=uid), None)
            for uid in next(uid_ranges)
        ]

    @pytest.fixture(autouse=True)
    async def users_for_update(self, org, create_user, uid_ranges):
        users_for_update = []
        for uid in next(uid_ranges):
            user = await create_user(org_id=org.org_id, uid=uid)
            users_for_update.append((user, user))
        return users_for_update

    @pytest.fixture(autouse=True)
    async def users_for_delete(self, org, create_user, uid_ranges):
        return [
            (None, await create_user(org_id=org.org_id, uid=uid))
            for uid in next(uid_ranges)
        ]

    @pytest.fixture
    def external_users(self, users_for_create, users_for_update):
        return [
            external_user
            for external_user, _ in chain(users_for_create, users_for_update)
        ]

    @pytest.fixture(autouse=True)
    def delete_mock(self, mock_action, generate_set_of_numbers):
        return mock_action(DeleteUserAction, action_func=generate_set_of_numbers)

    @pytest.fixture(autouse=True)
    def create_mock(self, mock_action):
        async def dummy_create(self):
            return self._init_kwargs['user_entity']

        return mock_action(CreateUserAction, action_func=dummy_create)

    @pytest.fixture(autouse=True)
    def update_mock(self, mock_action):
        async def dummy_update(self):
            return self._init_kwargs['user_entity']

        return mock_action(UpdateUserAction, action_func=dummy_update)

    @pytest.fixture
    def external_organization(self, mocker, external_users):
        async def dummy_get_users():
            for user in external_users:
                yield user

        mock = mocker.Mock()
        mock.get_users = mocker.Mock(side_effect=dummy_get_users)
        return mock

    @pytest.fixture
    def returned_func(self, org, external_organization):
        async def _inner():
            return await SyncUsersAction(
                org_id=org.org_id,
                external_organization=external_organization,
            ).run()

        return _inner

    def test_returned_users(self, users_for_create, users_for_update, returned):
        # Mocks always return user entity unlike actual functions which return user from database.
        # This test checks that all returned values are put in the list.
        assert returned[0] == [
            external_user
            for external_user, _ in chain(users_for_update, users_for_create)
        ]

    def test_affected_uids(self, returned, all_generated_numbers):
        assert returned[1] == all_generated_numbers

    def test_delete_calls(self, mocker, users_for_delete, returned, delete_mock):
        assert_that(
            delete_mock.call_args_list,
            contains_inanyorder(*[
                mocker.call(user=user)
                for _, user in users_for_delete
            ])
        )

    def test_update_calls(self, mocker, users_for_update, returned, update_mock):
        assert_that(
            update_mock.call_args_list,
            contains_inanyorder(*[
                mocker.call(user=user, user_entity=external_user)
                for external_user, user in users_for_update
            ])
        )

    def test_create_calls(self, mocker, users_for_create, returned, create_mock):
        assert_that(
            create_mock.call_args_list,
            contains_inanyorder(*[
                mocker.call(user_entity=external_user)
                for external_user, _ in users_for_create
            ])
        )
