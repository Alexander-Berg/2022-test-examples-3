import pytest

from sendr_utils import alist

from mail.beagle.beagle.core.entities.user_subscription import SubscriptionType
from mail.beagle.beagle.storage.exceptions import UserSubscriptionAlreadyExists, UserSubscriptionNotFound


@pytest.mark.asyncio
class TestUserSubscriptionMapper:
    @pytest.fixture
    def func_now(self, mocker, now):
        mocker.patch('mail.beagle.beagle.storage.mappers.user_subscription.func.now', mocker.Mock(return_value=now))
        return now

    async def test_create(self, storage, user_subscription_entity, func_now):
        user_subscription = await storage.user_subscription.create(user_subscription_entity)
        user_subscription_entity.created = user_subscription_entity.updated = func_now
        assert user_subscription_entity == user_subscription

    async def test_create_duplicate(self, storage, user_subscription_entity, func_now):
        await storage.user_subscription.create(user_subscription_entity)
        with pytest.raises(UserSubscriptionAlreadyExists):
            await storage.user_subscription.create(user_subscription_entity)

    async def test_get(self, storage, user_subscription):
        assert user_subscription == await storage.user_subscription.get(
            org_id=user_subscription.org_id,
            mail_list_id=user_subscription.mail_list_id,
            uid=user_subscription.uid,
        )

    @pytest.mark.asyncio
    class TestFind:
        @pytest.fixture
        def user_subscription(self, mail_list, user_subscription):
            user_subscription.mail_list = mail_list
            return user_subscription

        @pytest.fixture
        def user_subscriptions(self, mail_list, user_subscriptions):
            for user_subscription in user_subscriptions:
                user_subscription.mail_list = mail_list
            return user_subscriptions

        async def test_find(self, storage, user_subscription):
            returned = await alist(storage.user_subscription.find(user_subscription.org_id))
            assert returned == [user_subscription]

        async def test_find_subscription_type(self, storage, user_subscription):
            returned = await alist(storage.user_subscription.find(
                user_subscription.org_id,
                subscription_type=user_subscription.subscription_type,
            ))
            assert returned == [user_subscription]

        async def test_find_subscription_type_not_found(self, storage, user_subscription):
            returned = await alist(storage.user_subscription.find(user_subscription.org_id,
                                                                  subscription_type=SubscriptionType.YORK))
            assert returned == []

        async def test_find_by_mail_list_id(self, storage, user_subscription):
            returned = await alist(storage.user_subscription.find(
                user_subscription.org_id,
                user_subscription.mail_list_id,
            ))
            assert returned == [user_subscription]

        async def test_find_order_by(self, storage, user_subscriptions, org):
            returned = await alist(storage.user_subscription.find(org.org_id, order_by='uid'))
            assert returned == sorted(user_subscriptions, key=lambda x: x.uid)

        async def test_get_not_found(self, storage, randn):
            with pytest.raises(UserSubscriptionNotFound):
                await storage.user_subscription.get(randn(), randn(), randn())

        async def test_find_basic(self, storage, user_subscription):
            found_subscription = await alist(storage.user_subscription.find(
                org_id=user_subscription.org_id, mail_list_id=user_subscription.mail_list_id)
            )
            assert len(found_subscription) == 1

    async def test_delete(self, storage, user_subscription):
        await storage.user_subscription.delete(user_subscription)
        with pytest.raises(UserSubscriptionNotFound):
            await storage.user_subscription.get(
                org_id=user_subscription.org_id,
                mail_list_id=user_subscription.mail_list_id,
                uid=user_subscription.uid,
            )

    async def test_save(self, storage, user_subscription, func_now):
        user_subscription.subscription_type = SubscriptionType.YORK
        updated = await storage.user_subscription.save(user_subscription)
        user_subscription.updated = func_now
        assert user_subscription == updated
