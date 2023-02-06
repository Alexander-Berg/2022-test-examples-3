import pytest

from mail.beagle.beagle.core.actions.unit.create import CreateAutoMailListUnitAction, CreateUnitAction
from mail.beagle.beagle.core.entities.unit import Unit


class TestCreateUnitAction:
    PARAMETRIZE_MISSING_UID_USERNAME = pytest.mark.parametrize('unit_uid,unit_username', (
        (None, None),
        (1, None),
        (None, '1'),
    ))

    @pytest.fixture(autouse=True)
    def create_auto_mail_list_mock(self, mock_action):
        return mock_action(CreateAutoMailListUnitAction)

    @pytest.fixture
    def unit_uid(self, randn):
        return randn()

    @pytest.fixture
    def unit_username(self, rands):
        return rands()

    @pytest.fixture
    def unit_entity(self, org, rands, unit_uid, unit_username):
        return Unit(
            org_id=org.org_id,
            external_id=rands(),
            external_type=rands(),
            name=rands(),
            uid=unit_uid,
            username=unit_username,
        )

    @pytest.fixture
    def returned_func(self, unit_entity):
        async def _inner():
            return await CreateUnitAction(unit_entity).run()
        return _inner

    @pytest.fixture
    def returned_unit(self, returned):
        return returned[0]

    @pytest.fixture
    def returned_affected_uids(self, returned):
        return returned[1]

    @pytest.mark.asyncio
    async def test_creates_unit(self, storage, unit_entity, returned_unit):
        unit_entity.created = returned_unit.created
        unit_entity.updated = returned_unit.updated
        unit_entity.unit_id = returned_unit.unit_id
        unit_from_db = await storage.unit.get(unit_entity.org_id, unit_entity.unit_id)
        assert returned_unit == unit_entity == unit_from_db

    def test_returns_affected_uids(self, unit_uid, returned_affected_uids):
        assert returned_affected_uids == {unit_uid}

    def test_creates_mail_list_with_uid_and_username_present(self, create_auto_mail_list_mock, returned_unit):
        create_auto_mail_list_mock.assert_called_once_with(returned_unit)

    @PARAMETRIZE_MISSING_UID_USERNAME
    def test_does_not_create_mail_list_with_no_uid_or_username_present(self, create_auto_mail_list_mock, returned):
        create_auto_mail_list_mock.assert_not_called()

    @PARAMETRIZE_MISSING_UID_USERNAME
    def test_returns_empty_affected_uids(self, returned_affected_uids):
        assert returned_affected_uids == set()
