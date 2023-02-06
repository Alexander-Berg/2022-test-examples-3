from datetime import datetime, timedelta

import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder

from mail.payments.payments.core.entities.customer_subscription_transaction import CustomerSubscriptionTransaction
from mail.payments.payments.core.entities.enums import PayStatus, TransactionStatus
from mail.payments.payments.storage.exceptions import CustomerSubscriptionNotFound
from mail.payments.payments.utils.datetime import utcnow


class TestMapperCustomerSubscription:
    class TestCreate:
        @pytest.fixture
        async def created_customer_subscription(self, storage, customer_subscription):
            return await storage.customer_subscription.get(customer_subscription.uid,
                                                           customer_subscription.customer_subscription_id)

        def test_created_updated(self, customer_subscription):
            assert (isinstance(customer_subscription.created, datetime)
                    and isinstance(customer_subscription.updated, datetime))

        def test_returned(self, customer_subscription_entity, customer_subscription):
            customer_subscription_entity.subscription = customer_subscription.subscription
            customer_subscription_entity.created = customer_subscription.created
            customer_subscription_entity.updated = customer_subscription.updated
            assert customer_subscription_entity == customer_subscription

        def test_created(self, customer_subscription_entity, created_customer_subscription):
            customer_subscription_entity.subscription = created_customer_subscription.subscription
            customer_subscription_entity.created = created_customer_subscription.created
            customer_subscription_entity.updated = created_customer_subscription.updated
            assert customer_subscription_entity == created_customer_subscription

    class TestFind:
        @pytest.fixture
        def returned_func(self, storage):
            async def _inner(**kwargs):
                return await alist(storage.customer_subscription.find(**kwargs))

            return _inner

        @pytest.mark.asyncio
        async def test_empty(self, returned_func):
            assert await returned_func() == []

        @pytest.mark.asyncio
        async def test_empty_uid(self, customer_subscription, returned_func):
            assert await returned_func(uid=customer_subscription.uid + 1) == []

        @pytest.mark.asyncio
        async def test_get_all(self, customer_subscription, returned_func):
            customer_subscription.subscription = None
            assert_that(await returned_func(), contains_inanyorder(*[customer_subscription]))

        @pytest.mark.asyncio
        async def test_uid_filter(self, customer_subscription, returned_func):
            customer_subscription.subscription = None
            assert_that(await returned_func(uid=customer_subscription.uid),
                        contains_inanyorder(*[customer_subscription]))

    class TestGet:
        @pytest.fixture
        def returned_func(self, storage):
            async def _inner(**kwargs):
                return await storage.customer_subscription.get(**kwargs)

            return _inner

        @pytest.mark.asyncio
        async def test_customer_subscription_id_uid(self, customer_subscription, returned_func):
            returned = await returned_func(uid=customer_subscription.uid,
                                           customer_subscription_id=customer_subscription.customer_subscription_id)
            returned.subscription = customer_subscription.subscription
            assert returned == customer_subscription

        @pytest.mark.asyncio
        async def test_customer_subscription_id_uid_service_merchant_id(self, customer_subscription, returned_func):
            returned = await returned_func(uid=customer_subscription.uid,
                                           customer_subscription_id=customer_subscription.customer_subscription_id,
                                           service_merchant_id=customer_subscription.service_merchant_id)
            returned.subscription = customer_subscription.subscription
            assert returned == customer_subscription

        @pytest.mark.asyncio
        async def test_get__not_found(self, customer_subscription, returned_func):
            with pytest.raises(CustomerSubscriptionNotFound):
                await returned_func(uid=customer_subscription.uid + 1,
                                    customer_subscription_id=customer_subscription.customer_subscription_id + 1)

        @pytest.mark.asyncio
        async def test_not_found_service_merchant_id(self, customer_subscription, returned_func):
            with pytest.raises(CustomerSubscriptionNotFound):
                await returned_func(uid=customer_subscription.uid,
                                    customer_subscription_id=customer_subscription.customer_subscription_id,
                                    service_merchant_id=customer_subscription.service_merchant_id + 1)

    class TestSave:
        @pytest.fixture
        def returned_func(self, storage):
            async def _inner(*args, **kwargs):
                return await storage.customer_subscription.save(*args, **kwargs)

            return _inner

        @pytest.mark.asyncio
        async def test_save(self, rands, customer_subscription, randn, storage):
            now = utcnow()

            customer_subscription.quantity = randn()
            customer_subscription.user_ip = rands()
            customer_subscription.region_id = randn()
            customer_subscription.enabled = not customer_subscription.enabled
            customer_subscription.time_until = now
            customer_subscription.time_finish = now

            await storage.customer_subscription.save(customer_subscription)

            returned = await storage.customer_subscription.get(customer_subscription.uid,
                                                               customer_subscription.customer_subscription_id)
            customer_subscription.updated = returned.updated
            customer_subscription.subscription = returned.subscription
            assert customer_subscription == returned

        @pytest.mark.parametrize('field', ('uid', 'customer_subscription_id'))
        @pytest.mark.asyncio
        async def test_save_not_found(self, storage, customer_subscription, field):
            setattr(customer_subscription, field, None)
            with pytest.raises(CustomerSubscriptionNotFound):
                await storage.customer_subscription.save(customer_subscription)

        @pytest.mark.parametrize('field', ('created',))
        @pytest.mark.asyncio
        async def test_not_save(self, storage, customer_subscription, field):
            setattr(customer_subscription, field, None)

            await storage.customer_subscription.save(customer_subscription)
            returned = await storage.customer_subscription.get(customer_subscription.uid,
                                                               customer_subscription.customer_subscription_id)

            assert all((
                getattr(customer_subscription, field) is None,
                getattr(customer_subscription, field) != getattr(returned, field)
            ))

    class TestGetForCheck:
        @pytest.fixture
        def pay_status(self):
            return PayStatus.PAID

        @pytest.fixture(autouse=True)
        async def setup(self, storage, order_with_customer_subscription, pay_status):
            order_with_customer_subscription.pay_status = pay_status
            await storage.order.save(order_with_customer_subscription)

        @pytest.fixture(params=(60, 600))
        def lag(self, request, payments_settings):
            payments_settings.CUSTOMER_SUBSCRIPTION_CALLBACK_LAG = request.param
            return request.param

        @pytest.fixture(autouse=True)
        def pause(self, payments_settings):
            payments_settings.CUSTOMER_SUBSCRIPTION_UPDATER_PAUSE = 0
            return payments_settings.CUSTOMER_SUBSCRIPTION_UPDATER_PAUSE

        @pytest.fixture
        def returned_func(self, storage):
            async def _inner(*args, **kwargs):
                return await storage.customer_subscription.get_for_check(*args, **kwargs)

            return _inner

        @pytest.mark.asyncio
        async def test_get_for_check__not_found(self, returned_func):
            with pytest.raises(CustomerSubscriptionNotFound):
                await returned_func()

        @pytest.mark.parametrize('pay_status', (PayStatus.NEW,))
        @pytest.mark.asyncio
        async def test_not_found_pay_status(self, payments_settings, storage, returned_func, customer_subscription):
            lag = payments_settings.CUSTOMER_SUBSCRIPTION_CALLBACK_LAG
            customer_subscription.time_until = utcnow() - timedelta(minutes=lag + 1)
            customer_subscription.time_finish = None
            await storage.customer_subscription.save(customer_subscription)

            with pytest.raises(CustomerSubscriptionNotFound):
                await returned_func()

        @pytest.mark.parametrize(
            'lag_shift',
            (
                pytest.param(1, id='time_until_in_past'),
                pytest.param(-1, id='time_until_in_future')
            )
        )
        @pytest.mark.asyncio
        async def test_get_by_time(self, customer_subscription, lag, lag_shift, returned_func, storage, noop_manager):
            customer_subscription.time_until = utcnow() - timedelta(seconds=lag + lag_shift * 60)
            customer_subscription.time_finish = None
            customer_subscription = await storage.customer_subscription.save(customer_subscription)

            ctx_manager = noop_manager() if lag_shift > 0 else pytest.raises(CustomerSubscriptionNotFound)
            with ctx_manager:
                assert await returned_func() == customer_subscription

        @pytest.mark.asyncio
        async def test_not_found_finished(self, storage, returned_func, customer_subscription):
            customer_subscription.time_finish = utcnow()
            await storage.customer_subscription.save(customer_subscription)

            with pytest.raises(CustomerSubscriptionNotFound):
                await returned_func()

        @pytest.mark.parametrize('time_finish', (
            pytest.param(lambda: None, id='None'),
            pytest.param(lambda: utcnow(), id='now')
        ))
        @pytest.mark.parametrize('payment_status', (
            pytest.param(status, id=status.value) for status in list(TransactionStatus)
        ))
        @pytest.mark.asyncio
        async def test_time_finish_with_transactions(self, payment_status, rands, storage, time_finish, returned_func,
                                                     customer_subscription, noop_manager):
            customer_subscription_id = customer_subscription.customer_subscription_id
            customer_subscription.time_finish = time_finish()
            customer_subscription = await storage.customer_subscription.save(customer_subscription)

            tx = CustomerSubscriptionTransaction(uid=customer_subscription.uid,
                                                 customer_subscription_id=customer_subscription_id,
                                                 purchase_token=rands(),
                                                 payment_status=payment_status)
            await storage.customer_subscription_transaction.create_or_update(tx)

            ctx_manager = (
                noop_manager()
                if payment_status not in TransactionStatus.FINAL_STATUSES
                else pytest.raises(CustomerSubscriptionNotFound)
            )
            with ctx_manager:
                assert customer_subscription == await returned_func()

        class TestPause:
            @pytest.fixture
            def lag(self, payments_settings):
                payments_settings.CUSTOMER_SUBSCRIPTION_CALLBACK_LAG = 60
                return payments_settings.CUSTOMER_SUBSCRIPTION_CALLBACK_LAG

            @pytest.fixture(autouse=True)
            async def setup_customer_subscription(self, customer_subscription, storage, lag):
                customer_subscription.time_until = utcnow() - timedelta(seconds=lag * 2)
                customer_subscription.time_finish = None
                await storage.customer_subscription.save(customer_subscription)

            @pytest.fixture(autouse=True)
            def pause(self, payments_settings):
                payments_settings.CUSTOMER_SUBSCRIPTION_UPDATER_PAUSE = 0.1
                return payments_settings.CUSTOMER_SUBSCRIPTION_UPDATER_PAUSE

            @pytest.mark.asyncio
            async def test_pause__not_found(self, returned_func):
                with pytest.raises(CustomerSubscriptionNotFound):
                    await returned_func()

    class TestCountEnabled:
        @pytest.fixture(params=(False, True))
        def enabled(self, request):
            return request.param

        @pytest.fixture
        async def customer_subscription(self, storage, customer_subscription, enabled):
            customer_subscription.enabled = enabled
            return await storage.customer_subscription.save(customer_subscription)

        @pytest.fixture
        def returned_func(self, storage):
            async def _inner(**kwargs):
                return await storage.customer_subscription.count_enabled(**kwargs)

            return _inner

        @pytest.mark.asyncio
        async def test_count_enabled(self, customer_subscription, returned_func, enabled):
            expected_count = 1 if enabled else 0
            assert expected_count == await returned_func(uid=customer_subscription.uid,
                                                         subscription_id=customer_subscription.subscription_id)

        @pytest.mark.asyncio
        async def test_count_enabled_no_customer_subscriptions(self, subscription, returned_func):
            assert 0 == await returned_func(uid=subscription.uid + 1, subscription_id=subscription.subscription_id + 1)
