import pytest

from sendr_utils import alist

from mail.beagle.beagle.core.entities.mail_list import MailList, MailListDescription, MailListType
from mail.beagle.beagle.storage.exceptions import MailListNotFound, OrganizationNotFound


@pytest.mark.asyncio
class TestMailListMapper:
    @pytest.fixture
    def mail_list_entity(self, org, randn, rands):
        return MailList(
            org_id=org.org_id,
            mail_list_type=MailListType.MANUAL,
            uid=randn(),
            username=rands(),
        )

    @pytest.fixture
    async def mail_list(self, storage, mail_list_entity):
        return await storage.mail_list.create(mail_list_entity)

    @pytest.fixture
    def func_now(self, mocker, now):
        mocker.patch('mail.beagle.beagle.storage.mappers.mail_list.func.now', mocker.Mock(return_value=now))
        return now

    async def test_find(self, storage, mail_list):
        returned = await alist(storage.mail_list.find(mail_list.org_id))
        assert returned == [mail_list]

    async def test_find_mail_list_id(self, storage, mail_list):
        returned = await alist(storage.mail_list.find(mail_list.org_id, mail_list_id=mail_list.mail_list_id))
        assert returned == [mail_list]

    async def test_find_uids(self, storage, mail_list):
        returned = await alist(storage.mail_list.find(mail_list.org_id, uids=(mail_list.uid,)))
        assert returned == [mail_list]

    async def test_find_is_deleted(self, storage, mail_list):
        returned = await alist(storage.mail_list.find(mail_list.org_id, is_deleted=True))
        assert returned == []

    async def test_find_unknown_org(self, storage, mail_list, randn):
        returned = await alist(storage.mail_list.find(randn()))
        assert returned == []

    @pytest.mark.parametrize('query,username', (('Ест', 'ТеСты'),))
    async def test_find_by_name_query(self, storage, username, query, func_now, mail_list):
        mail_list.username = username
        await storage.mail_list.save(mail_list)
        mail_list.updated = func_now
        returned = await alist(storage.mail_list.find(mail_list.org_id, name_query=query))
        assert returned == [mail_list]

    async def test_create(self, storage, mail_list_entity, randn, func_now):
        serial = await storage.serial.get(org_id=mail_list_entity.org_id)
        expected_id = serial.mail_list_id = randn()
        await storage.serial.save(serial)
        mail_list = await storage.mail_list.create(mail_list_entity)
        mail_list_entity.mail_list_id = expected_id
        mail_list_entity.created = mail_list_entity.updated = func_now
        assert mail_list_entity == mail_list

    async def test_create_not_found(self, storage, mail_list_entity):
        mail_list_entity.org_id = -1
        with pytest.raises(OrganizationNotFound):
            await storage.mail_list.create(mail_list_entity)

    async def test_get(self, storage, mail_list):
        assert mail_list == await storage.mail_list.get(mail_list.org_id, mail_list.mail_list_id)

    async def test_get_not_found(self, storage, randn):
        with pytest.raises(MailListNotFound):
            await storage.mail_list.get(randn(), randn())

    async def test_get_by_uid(self, storage, mail_list):
        assert mail_list == await storage.mail_list.get_by_uid(mail_list.uid)

    async def test_get_by_uid_not_found(self, storage, randn):
        with pytest.raises(MailListNotFound):
            await storage.mail_list.get_by_uid(randn())

    async def test_delete(self, storage, mail_list, func_now):
        deleted = await storage.mail_list.delete(mail_list)
        mail_list.is_deleted = True
        mail_list.updated = func_now
        assert mail_list == deleted \
            and mail_list == await storage.mail_list.get(mail_list.org_id, mail_list.mail_list_id, is_deleted=True)

    async def test_deleted_not_found(self, storage, mail_list):
        await storage.mail_list.delete(mail_list)
        with pytest.raises(MailListNotFound):
            await storage.mail_list.get(mail_list.org_id, mail_list.mail_list_id)

    async def test_save(self, storage, mail_list, randn, rands, func_now):
        mail_list.type = MailListType.AUTO
        mail_list.uid = randn()
        mail_list.username = rands()
        mail_list.description = MailListDescription(ru='123')
        mail_list.settings = {'a': 'b'}
        saved = await storage.mail_list.save(mail_list)
        mail_list.updated = func_now
        assert mail_list == saved \
            and mail_list == await storage.mail_list.get(mail_list.org_id, mail_list.mail_list_id)
