import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder

from mail.beagle.beagle.core.entities.unit_user import UnitUser
from mail.beagle.beagle.storage.exceptions import UnitUserNotFound


@pytest.mark.asyncio
class TestUnitUserMapper:
    @pytest.fixture(autouse=True)
    def func_now(self, mocker, now):
        mocker.patch('mail.beagle.beagle.storage.mappers.unit_user.func.now', mocker.Mock(return_value=now))
        return now

    async def test_create(self, storage, unit_user_entity, func_now):
        unit_user = await storage.unit_user.create(unit_user_entity)
        unit_user_entity.created = unit_user_entity.updated = func_now
        assert unit_user_entity == unit_user

    async def test_get(self, storage, unit_user):
        assert unit_user == await storage.unit_user.get(unit_user.org_id, unit_user.unit_id, unit_user.uid)

    async def test_find(self, storage, unit_user):
        assert [unit_user] == await alist(storage.unit_user.find(unit_user.org_id))

    async def test_find_mail_list_id(self, storage, unit_user, unit_subscription):
        assert [unit_user] == await alist(
            storage.unit_user.find(unit_user.org_id, unit_subscription_mail_list_id=unit_subscription.mail_list_id)
        )

    async def test_get_not_found(self, storage, randn):
        with pytest.raises(UnitUserNotFound):
            await storage.unit_user.get(randn(), randn(), randn())

    async def test_delete(self, storage, unit_user):
        await storage.unit_user.delete(unit_user)
        with pytest.raises(UnitUserNotFound):
            await storage.unit_user.get(unit_user.org_id, unit_user.unit_id, unit_user.uid)

    @pytest.mark.asyncio
    class TestFind:
        @pytest.fixture
        async def unit_users(self, storage, units_with_other_org, users_with_other_org):
            return [
                await storage.unit_user.create(UnitUser(
                    org_id=unit.org_id,
                    unit_id=unit.unit_id,
                    uid=user.uid,
                ))
                for unit in units_with_other_org
                for user in users_with_other_org
                if unit.org_id == user.org_id
            ]

        async def test_org_id_filter(self, storage, org_id, unit_users):
            assert_that(
                await alist(storage.unit_user.find(org_id=org_id)),
                contains_inanyorder(*[unit_user for unit_user in unit_users if unit_user.org_id == org_id])
            )

        async def test_uid_filter(self, storage, unit_users):
            org_id = unit_users[0].org_id
            uid = unit_users[0].uid
            assert_that(
                await alist(storage.unit_user.find(org_id=org_id, uid=uid)),
                contains_inanyorder(*[
                    unit_user
                    for unit_user in unit_users
                    if unit_user.org_id == org_id and unit_user.uid == uid
                ])
            )

        @pytest.mark.parametrize('field', ('unit_id', 'uid'))
        async def test_order(self, storage, org_id, unit_users, field):
            found = await alist(storage.unit_user.find(org_id=org_id, order_by=field))
            assert found == sorted(found, key=lambda unit_user: getattr(unit_user, field))
