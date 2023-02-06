from datetime import datetime
from random import choice

import pytest

from hamcrest import assert_that, contains_inanyorder

from mail.payments.payments.core.entities.enums import NDS
from mail.payments.payments.core.entities.subscription import SubscriptionPrice
from mail.payments.payments.storage.exceptions import SubscriptionNotFound


class TestMapperSubscription:
    class TestCreate:
        @pytest.fixture
        async def created_subscription(self, storage, subscription):
            return await storage.subscription.get(subscription.uid, subscription.subscription_id)

        def test_created_updated(self, subscription):
            assert isinstance(subscription.created, datetime) and isinstance(subscription.updated, datetime)

        def test_returned(self, subscription_entity, subscription):
            subscription_entity.created = subscription.created
            subscription_entity.updated = subscription.updated
            assert subscription_entity == subscription

        def test_created(self, subscription_entity, created_subscription):
            subscription_entity.created = created_subscription.created
            subscription_entity.updated = created_subscription.updated
            assert subscription_entity == created_subscription

    class TestFind:
        @pytest.fixture
        def returned_func(self, storage):
            async def _inner(**kwargs):
                return [
                    subscription
                    async for subscription in storage.subscription.find(**kwargs)
                ]

            return _inner

        @pytest.mark.asyncio
        async def test_empty(self, returned_func):
            assert await returned_func() == []

        @pytest.mark.asyncio
        async def test_empty_uid(self, subscription, returned_func):
            assert await returned_func(uid=subscription.uid + 1) == []

        @pytest.mark.asyncio
        async def test_get_all(self, subscription, returned_func):
            assert_that(await returned_func(), contains_inanyorder(*[subscription]))

        @pytest.mark.asyncio
        async def test_uid_filter(self, subscription, returned_func):
            assert_that(await returned_func(uid=subscription.uid), contains_inanyorder(*[subscription]))

        class TestNotEnabled:
            @pytest.fixture(autouse=True)
            async def disabled_subscription(self, subscription, storage):
                subscription.enabled = False
                return await storage.subscription.save(subscription)

            @pytest.mark.asyncio
            async def test_not_enabled(self, returned_func):
                assert await returned_func() == []

            @pytest.mark.asyncio
            async def test_with_enabled(self, storage, disabled_subscription, returned_func):
                assert_that(await returned_func(enabled=False), contains_inanyorder(*[disabled_subscription]))

        class TestDeleted:
            @pytest.fixture(autouse=True)
            async def deleted_subscription(self, subscription, storage):
                subscription.deleted = True
                return await storage.subscription.save(subscription)

            @pytest.mark.asyncio
            async def test_deleted(self, returned_func):
                assert await returned_func() == []

    class TestGet:
        @pytest.fixture
        def returned_func(self, storage):
            async def _inner(**kwargs):
                return await storage.subscription.get(**kwargs)

            return _inner

        @pytest.mark.asyncio
        async def test_subscription_id_uid(self, subscription, returned_func):
            returned = await returned_func(uid=subscription.uid, subscription_id=subscription.subscription_id)
            assert returned == subscription

        @pytest.mark.asyncio
        async def test_subscription_id_uid_service_merchant_id(self, subscription, returned_func):
            returned = await returned_func(uid=subscription.uid,
                                           subscription_id=subscription.subscription_id,
                                           service_merchant_id=subscription.service_merchant_id)
            assert returned == subscription

        @pytest.mark.asyncio
        async def test_not_found(self, subscription, returned_func):
            with pytest.raises(SubscriptionNotFound):
                await returned_func(uid=subscription.uid + 1, subscription_id=subscription.subscription_id + 1)

        @pytest.mark.asyncio
        async def test_not_found_service_merchant_id(self, subscription, returned_func):
            with pytest.raises(SubscriptionNotFound):
                await returned_func(uid=subscription.uid,
                                    subscription_id=subscription.subscription_id,
                                    service_merchant_id=subscription.service_merchant_id + 1)

        class TestDeleted:
            @pytest.fixture(autouse=True)
            async def deleted_subscription(self, subscription, storage):
                subscription.deleted = True
                return await storage.subscription.save(subscription)

            @pytest.mark.asyncio
            async def test_deleted_not_found(self, subscription, returned_func):
                with pytest.raises(SubscriptionNotFound):
                    await returned_func(uid=subscription.uid,
                                        subscription_id=subscription.subscription_id,
                                        service_merchant_id=subscription.service_merchant_id)

    class TestSave:
        @pytest.fixture
        def returned_func(self, storage):
            async def _inner(*args, **kwargs):
                return await storage.subscription.save(*args, **kwargs)

            return _inner

        @pytest.mark.asyncio
        async def test_save(self, rands, subscription, randn, randdecimal, storage):
            subscription.title = rands()
            subscription.fiscal_title = rands()
            subscription.nds = choice(list(NDS))
            subscription.prices = [
                SubscriptionPrice(currency='RUB', price=randdecimal(), region_id=randn(max=500))
                for _ in range(3)
            ]

            await storage.subscription.save(subscription)

            returned = await storage.subscription.get(subscription.uid, subscription.subscription_id)
            subscription.updated = returned.updated
            assert subscription == returned

        @pytest.mark.parametrize('field', ('uid', 'subscription_id'))
        @pytest.mark.asyncio
        async def test_save_not_found(self, storage, subscription, field):
            setattr(subscription, field, None)
            with pytest.raises(SubscriptionNotFound):
                await storage.subscription.save(subscription)

        @pytest.mark.parametrize('field', (
            'period_amount', 'period_units', 'trial_period_amount', 'trial_period_units'
        ))
        @pytest.mark.asyncio
        async def test_not_save(self, storage, subscription, field):
            setattr(subscription, field, None)

            await storage.subscription.save(subscription)
            returned = await storage.subscription.get(subscription.uid, subscription.subscription_id)

            assert all((
                getattr(subscription, field) is None,
                getattr(subscription, field) != getattr(returned, field)
            ))
