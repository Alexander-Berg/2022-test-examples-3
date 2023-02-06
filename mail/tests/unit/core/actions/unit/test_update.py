from copy import copy

import pytest

from mail.beagle.beagle.core.actions.unit.update import (
    CreateAutoMailListUnitAction, DeleteMailListAction, GetDependentUIDsUnitAction, UnitUpdateError, UpdateUnitAction
)
from mail.beagle.beagle.tests.utils import dummy_coro_generator


class TestUpdateUnitAction:
    @pytest.fixture
    def old_unit_uid(self, randn):
        return randn()

    @pytest.fixture(autouse=True)
    async def old_unit_mail_list(self, storage, create_mail_list, unit, old_unit_uid):
        if old_unit_uid is None:
            return None
        mail_list = await create_mail_list(unit.org_id)
        mail_list.uid = old_unit_uid
        return await storage.mail_list.save(mail_list)

    @pytest.fixture
    async def unit(self, storage, unit, old_unit_uid, rands):
        unit.uid = old_unit_uid
        unit.username = rands()
        return await storage.unit.save(unit)

    @pytest.fixture
    def dependent_uids(self, randn):
        return {randn() for _ in range(5)}

    @pytest.fixture(autouse=True)
    def delete_mail_list_mock(self, mock_action):
        return mock_action(DeleteMailListAction)

    @pytest.fixture(autouse=True)
    def create_auto_mail_list_mock(self, mock_action):
        return mock_action(CreateAutoMailListUnitAction)

    @pytest.fixture(autouse=True)
    def get_dependent_uids_unit_mock(self, mock_action, dependent_uids):
        return mock_action(GetDependentUIDsUnitAction, dependent_uids)

    @pytest.fixture
    def unit_entity(self, unit):
        return copy(unit)

    @pytest.fixture
    def update_mail_list_affected_uids(self, randn):
        return {randn(), randn()}

    @pytest.fixture
    def update_mail_list_mock(self, mocker, update_mail_list_affected_uids):
        return mocker.patch.object(
            UpdateUnitAction,
            'update_mail_list',
            mocker.Mock(side_effect=dummy_coro_generator(update_mail_list_affected_uids))
        )

    @pytest.fixture
    def soft_update(self, unit_entity, rands):
        unit_entity.name = rands()

    @pytest.fixture
    def hard_update(self, unit_entity, randn, rands):
        unit_entity.uid = randn()
        unit_entity.username = rands()

    @pytest.fixture
    def returned_func(self, unit, unit_entity, update_mail_list_mock):
        async def _inner(unit=unit, unit_entity=unit_entity):
            return await UpdateUnitAction(unit=unit, unit_entity=unit_entity).run()

        return _inner

    @pytest.mark.asyncio
    async def test_fails_for_different_external_key(self, mocker, unit, returned_func):
        unit_entity = mocker.Mock()
        unit_entity.external_key = 'other'
        with pytest.raises(UnitUpdateError):
            await returned_func(unit=unit, unit_entity=unit_entity)

    @pytest.mark.asyncio
    async def test_updates_unit(self, storage, unit, unit_entity, soft_update, hard_update, returned):
        returned_unit, _ = returned
        unit_entity.updated = returned_unit.updated
        unit_from_db = await storage.unit.get(unit.org_id, unit.unit_id)
        assert returned_unit == unit_from_db == unit_entity

    def test_no_affected_uids_for_soft_update(self, soft_update, returned):
        assert returned[1] == set()

    def test_update_mail_list_not_called_for_soft_update(self, soft_update, returned, update_mail_list_mock):
        update_mail_list_mock.assert_not_called()

    def test_returns_dependent_uids_for_hard_update(self,
                                                    hard_update,
                                                    dependent_uids,
                                                    update_mail_list_affected_uids,
                                                    returned,
                                                    ):
        assert returned[1] == set.union(dependent_uids, update_mail_list_affected_uids)

    def test_update_mail_list_called_for_hard_update(self, old_unit_uid, hard_update, returned, update_mail_list_mock):
        update_mail_list_mock.assert_called_once_with(old_unit_uid)

    class TestUpdateMailList:
        @pytest.fixture
        def new_unit_uid(self, randn):
            return randn()

        @pytest.fixture(autouse=True)
        def apply_new_unit_uid(self, unit, new_unit_uid):
            unit.uid = new_unit_uid

        @pytest.fixture
        def returned_func(self, mocker, storage, unit, old_unit_uid):
            async def _inner(unit=unit, old_unit_uid=old_unit_uid):
                # unit_entity argument doesn't affect update_mail_list method
                action = UpdateUnitAction(unit=unit, unit_entity=mocker.Mock())
                action.context.storage = storage
                return await action.update_mail_list(old_unit_uid)
            return _inner

        @pytest.mark.parametrize('old_unit_uid,new_unit_uid', (
            (1, None),
            (1, 2),
        ))
        def test_deletes_old_mail_list(self, old_unit_mail_list, returned, delete_mail_list_mock):
            delete_mail_list_mock.assert_called_once_with(mail_list=old_unit_mail_list)

        @pytest.mark.asyncio
        @pytest.mark.parametrize('old_unit_uid,new_unit_uid', ((1, 2),))
        async def test_ignores_old_mail_list_not_found(self, storage, old_unit_mail_list, returned_func):
            await storage.mail_list.delete(old_unit_mail_list)
            await returned_func()

        @pytest.mark.parametrize('old_unit_uid,new_unit_uid', (
            (None, 1),
            (1, 1),
        ))
        def test_does_not_delete_old_mail_list(self, returned, delete_mail_list_mock):
            delete_mail_list_mock.assert_not_called()

        @pytest.mark.parametrize('old_unit_uid,new_unit_uid', (
            (1, 2),
            (None, 2),
        ))
        def test_creates_mail_list(self, unit, returned, create_auto_mail_list_mock):
            create_auto_mail_list_mock.assert_called_once_with(unit)

        @pytest.mark.asyncio
        @pytest.mark.parametrize('old_unit_uid,new_unit_uid', ((1, 2),))
        async def test_fails_if_mail_list_exists(self, storage, create_mail_list, unit, new_unit_uid, returned_func):
            mail_list = await create_mail_list(unit.org_id)
            mail_list.uid = new_unit_uid
            await storage.mail_list.save(mail_list)
            with pytest.raises(UnitUpdateError):
                await returned_func()
