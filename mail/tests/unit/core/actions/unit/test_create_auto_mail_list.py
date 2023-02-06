import pytest

from mail.beagle.beagle.core.actions.unit.create_auto_mail_list import (
    CreateAutoMailListUnitAction, CreateMailListAction
)


class TestCreateAutoMailListUnitAction:
    @pytest.fixture
    async def unit(self, storage, unit, randn, rands):
        unit.uid = randn()
        unit.username = rands()
        return await storage.unit.save(unit)

    @pytest.fixture(autouse=True)
    def create_mail_list_mock(self, mail_list, mock_action):
        return mock_action(CreateMailListAction, mail_list)

    @pytest.fixture
    def returned_func(self, unit):
        async def _inner(unit=unit):
            return await CreateAutoMailListUnitAction(unit).run()

        return _inner

    def test_calls_create_mail_list(self, unit, create_mail_list_mock, returned):
        create_mail_list_mock.assert_called_once_with(
            org_id=unit.org_id,
            uid=unit.uid,
            username=unit.username,
            generate_cache=False,
        )

    def test_returns_mail_list(self, mail_list, returned):
        assert returned == mail_list

    @pytest.mark.asyncio
    async def test_creates_unit_subscription(self, storage, unit, mail_list, returned):
        await storage.unit_subscription.get(
            org_id=unit.org_id,
            mail_list_id=mail_list.mail_list_id,
            unit_id=unit.unit_id,
        )
