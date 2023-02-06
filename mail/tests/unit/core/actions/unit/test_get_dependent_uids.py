import pytest

from mail.beagle.beagle.core.actions.unit.get_dependent_uids import GetDependentUIDsUnitAction


class TestGetDependentUIDsUnitAction:
    @pytest.fixture
    async def parent_unit(self, unit, create_unit, create_unit_unit):
        parent_unit = await create_unit(unit.org_id)
        await create_unit_unit(org_id=unit.org_id, unit_id=unit.unit_id, parent_unit_id=parent_unit.unit_id)
        return parent_unit

    @pytest.fixture
    async def dependent_mail_lists(self, org_id, unit, parent_unit, create_mail_list, create_unit_subscription):
        mail_lists = []
        for _ in range(2):
            for unit_id in (unit.unit_id, parent_unit.unit_id):
                mail_list = await create_mail_list(org_id)
                await create_unit_subscription(org_id=org_id, mail_list_id=mail_list.mail_list_id, unit_id=unit_id)
                mail_lists.append(mail_list)
        return mail_lists

    @pytest.fixture(autouse=True)
    async def not_dependent_mail_lists(self, org, create_mail_list):
        return [await create_mail_list(org.org_id) for _ in range(3)]

    @pytest.fixture
    def returned_func(self, unit):
        async def _inner(org_id=unit.org_id, unit_id=unit.unit_id, unit_ids=None):
            return await GetDependentUIDsUnitAction(org_id=org_id, unit_id=unit_id, unit_ids=unit_ids).run()

        return _inner

    def test_returns_dependent_uids_for_unit_id(self, dependent_mail_lists, returned):
        assert returned == {mail_list.uid for mail_list in dependent_mail_lists}

    @pytest.mark.asyncio
    async def test_returns_dependent_uids_for_unit_ids(self, unit, parent_unit, dependent_mail_lists, returned_func):
        assert await returned_func(unit_id=None, unit_ids=(unit.unit_id, parent_unit.unit_id)) \
            == {mail_list.uid for mail_list in dependent_mail_lists}
