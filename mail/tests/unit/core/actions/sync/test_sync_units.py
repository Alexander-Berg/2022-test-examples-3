from itertools import chain

import pytest

from hamcrest import assert_that, contains_inanyorder

from mail.beagle.beagle.core.actions.sync.sync_units import (
    CreateUnitAction, DeleteUnitAction, SyncUnitsAction, UpdateUnitAction
)

from .base import BaseTestSyncAction


class TestSyncUnitsAction(BaseTestSyncAction):
    @pytest.fixture
    def external_id_range(self):
        ids_per_range = 5
        return (map(str, range(i * ids_per_range, (i + 1) * ids_per_range)) for i in range(3))

    @pytest.fixture(autouse=True)
    async def units_for_delete(self, org, create_unit, external_id_range):
        return [
            (None, await create_unit(org_id=org.org_id, external_id=external_id))
            for external_id in next(external_id_range)
        ]

    @pytest.fixture(autouse=True)
    def units_for_create(self, org, create_unit_entity, external_id_range):
        return [
            (create_unit_entity(org_id=org.org_id, external_id=external_id), None)
            for external_id in next(external_id_range)
        ]

    @pytest.fixture(autouse=True)
    async def units_for_update(self, org, create_unit, external_id_range):
        result = []
        for external_id in next(external_id_range):
            unit = await create_unit(org_id=org.org_id, external_id=external_id)
            result.append((unit, unit))
        return result

    @pytest.fixture
    def external_units(self, units_for_create, units_for_update):
        return [
            external_unit
            for external_unit, _ in chain(units_for_create, units_for_update)
        ]

    @pytest.fixture
    def external_organization(self, mocker, external_units):
        async def dummy_get_units():
            for external_unit in external_units:
                yield external_unit

        mock = mocker.Mock()
        mock.get_units = mocker.Mock(side_effect=dummy_get_units)
        return mock

    @pytest.fixture(autouse=True)
    def delete_mock(self, mock_action, generate_set_of_numbers):
        return mock_action(DeleteUnitAction, action_func=generate_set_of_numbers)

    @pytest.fixture(autouse=True)
    def update_mock(self, mock_action, generate_set_of_numbers):
        async def dummy_update(self):
            return self._init_kwargs['unit_entity'], await generate_set_of_numbers()

        return mock_action(UpdateUnitAction, action_func=dummy_update)

    @pytest.fixture(autouse=True)
    def create_mock(self, mock_action, generate_set_of_numbers):
        async def dummy_create(self):
            return self._init_kwargs['unit_entity'], await generate_set_of_numbers()

        return mock_action(CreateUnitAction, action_func=dummy_create)

    @pytest.fixture
    def returned_func(self, org, external_organization):
        async def _inner():
            return await SyncUnitsAction(
                org_id=org.org_id,
                external_organization=external_organization,
            ).run()

        return _inner

    def test_returned_units(self, units_for_create, units_for_update, returned):
        # Mocks always return unit_entity unlike actual functions for the purpose of testing.
        assert returned[0] == [
            external_unit
            for external_unit, _ in chain(units_for_update, units_for_create)
        ]

    def test_returned_affected_uids(self, all_generated_numbers, returned):
        assert returned[1] == all_generated_numbers

    def test_delete_calls(self, mocker, units_for_delete, returned, delete_mock):
        assert_that(
            delete_mock.call_args_list,
            contains_inanyorder(*[
                mocker.call(unit=unit)
                for _, unit in units_for_delete
            ])
        )

    def test_update_calls(self, mocker, units_for_update, returned, update_mock):
        assert_that(
            update_mock.call_args_list,
            contains_inanyorder(*[
                mocker.call(unit=unit, unit_entity=external_unit)
                for external_unit, unit in units_for_update
            ])
        )

    def test_create_calls(self, mocker, units_for_create, returned, create_mock):
        assert_that(
            create_mock.call_args_list,
            contains_inanyorder(*[
                mocker.call(unit_entity=external_unit)
                for external_unit, _ in units_for_create
            ])
        )

    @pytest.fixture
    def all_mocks(self, mocker, delete_mock, create_mock, update_mock):
        mock = mocker.Mock()
        mock.attach_mock(delete_mock, 'delete_mock')
        mock.attach_mock(update_mock, 'update_mock')
        mock.attach_mock(create_mock, 'create_mock')
        return mock

    @pytest.fixture
    def expected_calls_order(self, mocker, units_for_delete, units_for_update, units_for_create):
        calls = [mocker.call.delete_mock(unit=unit) for _, unit in units_for_delete]
        calls.extend([
            mocker.call.update_mock(unit=unit, unit_entity=external_unit) for external_unit, unit in units_for_update
        ])
        calls.extend([mocker.call.create_mock(unit_entity=external_unit) for external_unit, _ in units_for_create])
        return calls

    def test_calls_order(self, mocker, all_mocks, units_for_delete, units_for_update, units_for_create, returned,
                         expected_calls_order):
        assert all_mocks.mock_calls == expected_calls_order
