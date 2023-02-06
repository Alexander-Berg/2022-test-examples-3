from datetime import datetime
from random import choice

import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains, contains_inanyorder, instance_of

from mail.payments.payments.core.entities.enums import TransactionStatus
from mail.payments.payments.storage.exceptions import CustomerSubscriptionTransactionNotFound


class TestMapperCustomerSubscriptionTransaction:
    class TestCreate:
        @pytest.fixture
        async def created_customer_subscription_transaction(self, storage, customer_subscription_transaction):
            return await storage.customer_subscription_transaction.get(
                customer_subscription_transaction.uid,
                customer_subscription_transaction.customer_subscription_id,
                customer_subscription_transaction.purchase_token
            )

        def test_created_updated(self, customer_subscription_transaction):
            assert_that(
                (customer_subscription_transaction.created, customer_subscription_transaction.updated),
                contains(instance_of(datetime), instance_of(datetime))
            )

        def test_create__returned(self, customer_subscription_transaction_entity, customer_subscription_transaction):
            customer_subscription_transaction_entity.created = customer_subscription_transaction.created
            customer_subscription_transaction_entity.updated = customer_subscription_transaction.updated
            assert customer_subscription_transaction_entity == customer_subscription_transaction

        def test_created(self, customer_subscription_transaction_entity, created_customer_subscription_transaction):
            customer_subscription_transaction_entity.created = created_customer_subscription_transaction.created
            customer_subscription_transaction_entity.updated = created_customer_subscription_transaction.updated
            assert customer_subscription_transaction_entity == created_customer_subscription_transaction

        @pytest.mark.asyncio
        async def test_create(self, storage, customer_subscription_transaction_entity):
            _, created = await storage.customer_subscription_transaction.create_or_update(
                customer_subscription_transaction_entity
            )
            assert created is True

        @pytest.mark.asyncio
        async def test_update(self, storage, customer_subscription_transaction_entity):
            customer_subscription_transaction, _ = await storage.customer_subscription_transaction.create_or_update(
                customer_subscription_transaction_entity
            )
            _, created = await storage.customer_subscription_transaction.create_or_update(
                customer_subscription_transaction
            )

            assert created is False

    class TestFind:
        @pytest.fixture
        def returned_func(self, storage):
            async def _inner(**kwargs):
                return await alist(storage.customer_subscription_transaction.find(**kwargs))

            return _inner

        @pytest.mark.asyncio
        async def test_empty(self, returned_func):
            assert await returned_func() == []

        @pytest.mark.asyncio
        async def test_get_all(self, customer_subscription_transaction, returned_func):
            assert_that(await returned_func(), contains_inanyorder(*[customer_subscription_transaction]))

        class TestUid:
            @pytest.mark.asyncio
            async def test_empty_uid(self, customer_subscription_transaction, returned_func):
                assert await returned_func(uid=customer_subscription_transaction.uid + 1) == []

            @pytest.mark.asyncio
            async def test_uid(self, customer_subscription_transaction, returned_func):
                returned = await returned_func(uid=customer_subscription_transaction.uid)
                assert returned == [customer_subscription_transaction]

        class TestCustomerSubscriptionId:
            @pytest.mark.asyncio
            async def test_empty_customer_subscription_id(self, customer_subscription_transaction, returned_func):
                customer_subscription_id = customer_subscription_transaction.customer_subscription_id + 1
                assert await returned_func(customer_subscription_id=customer_subscription_id + 1) == []

            @pytest.mark.asyncio
            async def test_customer_subscription_id(self, customer_subscription_transaction, returned_func):
                customer_subscription_id = customer_subscription_transaction.customer_subscription_id
                returned = await returned_func(customer_subscription_id=customer_subscription_id)
                assert returned == [customer_subscription_transaction]

        class TestFinal:
            @pytest.fixture(params=list(TransactionStatus))
            def payment_status(self, request):
                return request.param

            @pytest.fixture(autouse=True)
            async def setup(self, storage, payment_status, customer_subscription_transaction):
                customer_subscription_transaction.payment_status = payment_status
                returned = await storage.customer_subscription_transaction.save(customer_subscription_transaction)
                customer_subscription_transaction.updated = returned.updated

            @pytest.mark.asyncio
            async def test_empty_final(self, payment_status, returned_func):
                assert await returned_func(final=payment_status not in TransactionStatus.FINAL_STATUSES) == []

            @pytest.mark.asyncio
            async def test_final(self, storage, customer_subscription_transaction, payment_status, returned_func):
                returned = await returned_func(final=payment_status in TransactionStatus.FINAL_STATUSES)
                assert returned == [customer_subscription_transaction]

    class TestFindFinalPurchaseTokens:
        @pytest.fixture(params=list(TransactionStatus))
        def payment_status(self, request):
            return request.param

        @pytest.fixture(autouse=True)
        async def setup(self, storage, payment_status, customer_subscription_transaction):
            customer_subscription_transaction.payment_status = payment_status
            returned = await storage.customer_subscription_transaction.save(customer_subscription_transaction)
            customer_subscription_transaction.updated = returned.updated

        @pytest.fixture
        def returned_func(self, storage):
            async def _inner(*args, **kwargs):
                return await alist(
                    storage.customer_subscription_transaction.find_final_purchase_tokens(*args, **kwargs)
                )

            return _inner

        @pytest.mark.asyncio
        async def test_find_final_purchase_tokens__returned(self,
                                                            customer_subscription_transaction,
                                                            payment_status,
                                                            returned_func):
            expected = []
            if payment_status in TransactionStatus.FINAL_STATUSES:
                expected = [customer_subscription_transaction.purchase_token]

            assert await returned_func(customer_subscription_transaction.uid,
                                       customer_subscription_transaction.customer_subscription_id) == expected

    class TestSave:
        @pytest.fixture
        def returned_func(self, storage):
            async def _inner(*args, **kwargs):
                return await storage.customer_subscription_transaction.save(*args, **kwargs)

            return _inner

        @pytest.mark.asyncio
        async def test_save(self, rands, customer_subscription_transaction, storage):
            uid = customer_subscription_transaction.uid
            customer_subscription_id = customer_subscription_transaction.customer_subscription_id
            purchase_token = customer_subscription_transaction.purchase_token

            customer_subscription_transaction.payment_status = choice(list(TransactionStatus))
            customer_subscription_transaction.data = {rands(): rands() for _ in range(10)}

            await storage.customer_subscription_transaction.save(customer_subscription_transaction)

            returned = await storage.customer_subscription_transaction.get(uid,
                                                                           customer_subscription_id,
                                                                           purchase_token)
            customer_subscription_transaction.updated = returned.updated
            assert customer_subscription_transaction == returned

        @pytest.mark.parametrize('field', ('uid', 'customer_subscription_id', 'purchase_token'))
        @pytest.mark.asyncio
        async def test_save_not_found(self, storage, customer_subscription_transaction, field):
            setattr(customer_subscription_transaction, field, None)
            with pytest.raises(CustomerSubscriptionTransactionNotFound):
                await storage.customer_subscription_transaction.save(customer_subscription_transaction)

        @pytest.mark.parametrize('field', ('created',))
        @pytest.mark.asyncio
        async def test_not_save(self, storage, customer_subscription_transaction, field):
            uid = customer_subscription_transaction.uid
            customer_subscription_id = customer_subscription_transaction.customer_subscription_id
            purchase_token = customer_subscription_transaction.purchase_token
            setattr(customer_subscription_transaction, field, None)

            await storage.customer_subscription_transaction.save(customer_subscription_transaction)
            returned = await storage.customer_subscription_transaction.get(uid,
                                                                           customer_subscription_id,
                                                                           purchase_token)

            assert all((
                getattr(customer_subscription_transaction, field) is None,
                getattr(customer_subscription_transaction, field) != getattr(returned, field)
            ))
