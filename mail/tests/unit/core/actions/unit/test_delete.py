import pytest

from mail.beagle.beagle.core.actions.unit.delete import DeleteUnitAction
from mail.beagle.beagle.storage.exceptions import MailListNotFound, UnitNotFound


class TestDeleteUnitAction:
    @pytest.fixture(params=(True, False))
    def unit_has_mail_list(self, request):
        return request.param

    @pytest.fixture
    async def unit(self, storage, randn, unit, unit_has_mail_list):
        if unit_has_mail_list:
            unit.uid = randn()
        else:
            unit.uid = None
        await storage.unit.save(unit)
        return unit

    @pytest.fixture
    async def unit_mail_list(self, storage, unit, unit_has_mail_list, create_mail_list, create_unit_subscription):
        if not unit_has_mail_list:
            return None
        mail_list = await create_mail_list(unit.org_id)
        mail_list.uid = unit.uid
        await storage.mail_list.save(mail_list)
        await create_unit_subscription(
            org_id=unit.org_id,
            mail_list_id=mail_list.mail_list_id,
            unit_id=unit.unit_id,
        )
        return mail_list

    @pytest.fixture
    async def other_mail_list(self, unit, create_mail_list, create_unit_subscription):
        mail_list = await create_mail_list(unit.org_id)
        await create_unit_subscription(org_id=unit.org_id, mail_list_id=mail_list.mail_list_id, unit_id=unit.unit_id)
        return mail_list

    @pytest.fixture
    def returned_func(self, unit):
        async def _inner(unit=unit):
            return await DeleteUnitAction(unit).run()

        return _inner

    def test_affected_uids(self, unit_mail_list, other_mail_list, returned):
        assert returned == {other_mail_list.uid}

    @pytest.mark.asyncio
    class TestStorage:
        async def test_deletes_unit(self, storage, unit, returned):
            with pytest.raises(UnitNotFound):
                await storage.unit.get(unit.org_id, unit.unit_id)

        @pytest.mark.parametrize('unit_has_mail_list', (True,))
        async def test_deletes_unit_mail_list(self, storage, unit_mail_list, returned):
            with pytest.raises(MailListNotFound):
                await storage.mail_list.get(unit_mail_list.org_id, unit_mail_list.mail_list_id)

        async def test_does_not_delete_other_mail_list(self, storage, other_mail_list, returned):
            assert await storage.mail_list.get(other_mail_list.org_id, other_mail_list.mail_list_id) == other_mail_list
