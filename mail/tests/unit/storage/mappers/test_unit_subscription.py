import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder

from mail.beagle.beagle.core.entities.enums import SubscriptionType
from mail.beagle.beagle.core.entities.unit_subscription import UnitSubscription
from mail.beagle.beagle.storage.exceptions import UnitSubscriptionAlreadyExists, UnitSubscriptionNotFound


@pytest.mark.asyncio
class TestUnitSubscriptionMapper:
    @pytest.fixture
    def func_now(self, mocker, now):
        mocker.patch('mail.beagle.beagle.storage.mappers.unit_subscription.func.now', mocker.Mock(return_value=now))
        return now

    async def test_create(self, storage, unit_subscription_entity, func_now):
        unit_subscription = await storage.unit_subscription.create(unit_subscription_entity)
        unit_subscription_entity.created = unit_subscription_entity.updated = func_now
        assert unit_subscription_entity == unit_subscription

    async def test_create_duplicate(self, storage, unit_subscription_entity, func_now):
        await storage.unit_subscription.create(unit_subscription_entity)
        with pytest.raises(UnitSubscriptionAlreadyExists):
            await storage.unit_subscription.create(unit_subscription_entity)

    async def test_get(self, storage, unit_subscription):
        assert unit_subscription == await storage.unit_subscription.get(
            org_id=unit_subscription.org_id,
            mail_list_id=unit_subscription.mail_list_id,
            unit_id=unit_subscription.unit_id,
        )

    async def test_get_not_found(self, storage, randn):
        with pytest.raises(UnitSubscriptionNotFound):
            await storage.unit_subscription.get(randn(), randn(), randn())

    async def test_delete(self, storage, unit_subscription):
        await storage.unit_subscription.delete(unit_subscription)
        with pytest.raises(UnitSubscriptionNotFound):
            await storage.unit_subscription.get(
                org_id=unit_subscription.org_id,
                mail_list_id=unit_subscription.mail_list_id,
                unit_id=unit_subscription.unit_id,
            )

    async def test_save(self, storage, unit_subscription, func_now):
        unit_subscription.subscription_type = SubscriptionType.YORK
        updated = await storage.unit_subscription.save(unit_subscription)
        unit_subscription.updated = func_now
        assert unit_subscription == updated \
            and unit_subscription == await storage.unit_subscription.get(
                org_id=unit_subscription.org_id,
                mail_list_id=unit_subscription.mail_list_id,
                unit_id=unit_subscription.unit_id,
            )

    @pytest.mark.asyncio
    class TestFind:
        @pytest.fixture
        async def unit_subscriptions(self, storage, units_with_other_org, mail_lists):
            unit_subscriptions = []
            for unit in units_with_other_org:
                for mail_list in mail_lists:
                    if unit.org_id != mail_list.org_id:
                        continue
                    us = await storage.unit_subscription.create(UnitSubscription(
                        org_id=unit.org_id,
                        mail_list_id=mail_list.mail_list_id,
                        unit_id=unit.unit_id,
                    ))
                    us.mail_list = mail_list
                    unit_subscriptions.append(us)
            return unit_subscriptions

        async def test_unit_id_filter(self, storage, units_with_other_org, unit_subscriptions):
            unit = units_with_other_org[0]
            assert_that(
                await alist(storage.unit_subscription.find(org_id=unit.org_id, unit_id=unit.unit_id)),
                contains_inanyorder(*[
                    us
                    for us in unit_subscriptions
                    if us.org_id == unit.org_id and us.unit_id == unit.unit_id
                ])
            )

        async def test_unit_ids_filter(self, storage, org_id, units_with_other_org, unit_subscriptions):
            unit_ids = [unit.unit_id for unit in units_with_other_org if unit.org_id == org_id]
            assert_that(
                await alist(storage.unit_subscription.find(org_id=org_id, unit_ids=unit_ids)),
                contains_inanyorder(*[
                    us
                    for us in unit_subscriptions
                    if us.org_id == org_id and us.unit_id in unit_ids
                ])
            )

        async def test_filters_by_mail_list_is_deleted(self, storage, unit_subscriptions):
            unit = unit_subscriptions[0]
            mail_list = unit.mail_list
            mail_list.is_deleted = True
            await storage.mail_list.save(mail_list)
            assert_that(
                await alist(storage.unit_subscription.find(org_id=unit.org_id)),
                contains_inanyorder(*[
                    us
                    for us in unit_subscriptions
                    if us.org_id == unit.org_id and us.mail_list_id != mail_list.mail_list_id
                ])
            )
