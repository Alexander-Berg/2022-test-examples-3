import pytest

from sendr_utils import alist

from mail.beagle.beagle.core.entities.mail_list_responsible import MailListResponsible
from mail.beagle.beagle.storage.exceptions import MailListResponsibleAlreadyExists, MailListResponsibleNotFound


@pytest.mark.asyncio
class TestMailListResponsibleMapper:
    @pytest.fixture
    def responsible_entity(self, org, mail_list, user):
        return MailListResponsible(
            org_id=org.org_id,
            mail_list_id=mail_list.mail_list_id,
            uid=user.uid,
        )

    @pytest.fixture
    async def responsible(self, storage, responsible_entity):
        return await storage.mail_list_responsible.create(responsible_entity)

    @pytest.fixture
    def func_now(self, mocker, now):
        mocker.patch('mail.beagle.beagle.storage.mappers.mail_list_responsible.func.now', mocker.Mock(return_value=now))
        return now

    async def test_create(self, storage, responsible_entity, func_now):
        responsible = await storage.mail_list_responsible.create(responsible_entity)
        responsible_entity.created = responsible_entity.updated = func_now
        assert responsible_entity == responsible

    async def test_create_duplicate(self, storage, responsible_entity, func_now):
        await storage.mail_list_responsible.create(responsible_entity)
        with pytest.raises(MailListResponsibleAlreadyExists):
            await storage.mail_list_responsible.create(responsible_entity)

    async def test_get(self, storage, responsible):
        assert responsible == await storage.mail_list_responsible.get(
            org_id=responsible.org_id,
            mail_list_id=responsible.mail_list_id,
            uid=responsible.uid,
        )

    async def test_get_not_found(self, storage, randn):
        with pytest.raises(MailListResponsibleNotFound):
            await storage.mail_list_responsible.get(randn(), randn(), randn())

    async def test_delete(self, storage, responsible):
        await storage.mail_list_responsible.delete(responsible)
        with pytest.raises(MailListResponsibleNotFound):
            await storage.mail_list_responsible.get(
                org_id=responsible.org_id,
                mail_list_id=responsible.mail_list_id,
                uid=responsible.uid,
            )

    async def test_find_basic(self, storage, responsible):
        found_responsible = await alist(storage.mail_list_responsible.find(
            mail_list_id=responsible.mail_list_id, org_id=responsible.org_id
        ))
        assert len(found_responsible) == 1
