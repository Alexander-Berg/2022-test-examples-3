from datetime import datetime, timezone
from decimal import Decimal

import pytest

from hamcrest import assert_that, has_properties

from mail.ohio.ohio.core.actions.order.create_or_update import CreateOrUpdateFromPaymentsOrderAction
from mail.ohio.ohio.core.entities.order import NDS, Item, OrderData, OrderStatus, Refund, RefundStatus
from mail.ohio.ohio.storage.exceptions import OrderNotFoundStorageError


class TestCreateOrUpdateFromPaymentsOrderAction:
    @staticmethod
    def _check_order(kwargs, order):
        kwargs.pop('payments_service_id')
        kwargs['order_revision'] = kwargs['service_revision'] = kwargs.pop('revision')
        assert_that(order, has_properties(kwargs))

    @pytest.fixture
    def create_item(self, randn, rands):
        def _inner():
            return Item(
                amount=Decimal(randn()),
                price=Decimal(randn()),
                currency=rands(),
                nds=NDS.NDS_10,
                name=rands(),
            )

        return _inner

    @pytest.fixture
    def trust_purchase_token(self, rands):
        return rands()

    @pytest.fixture
    def get_kwargs(self, randn, rands, create_item, customer, service, trust_purchase_token):
        def _inner():
            return {
                'customer_uid': customer.customer_uid,
                'payments_service_id': service.payments_service_id,
                'trust_purchase_token': trust_purchase_token,
                'merchant_uid': randn(),
                'service_merchant_id': randn(),
                'payments_order_id': randn(),
                'status': OrderStatus.PAID,
                'order_data': OrderData(
                    total=Decimal(randn()),
                    currency=rands(),
                    description=rands(),
                    items=[create_item() for _ in range(2)],
                    refunds=[
                        Refund(
                            trust_refund_id=rands(),
                            refund_status=RefundStatus.COMPLETED,
                            total=Decimal(randn()),
                            currency=rands(),
                            items=[create_item() for _ in range(2)]
                        )
                        for _ in range(2)
                    ]
                ),
                'service_data': {rands(): rands()},
                'revision': randn(),
                'created': datetime(2019, 3, 21, 10, 45, tzinfo=timezone.utc),
                'subservice_id': rands(),
            }

        return _inner

    @pytest.fixture
    def default_kwargs(self, get_kwargs):
        return get_kwargs()

    @pytest.fixture
    def get_order(self, storage, customer, trust_purchase_token):
        async def _inner():
            return await storage.order.get_by_trust_purchase_token(
                customer_uid=customer.customer_uid,
                trust_purchase_token=trust_purchase_token,
            )
        return _inner

    @pytest.fixture
    def returned_func(self, default_kwargs):
        async def _inner(**kwargs):
            kwargs = {**default_kwargs, **kwargs}
            return await CreateOrUpdateFromPaymentsOrderAction(**kwargs).run()

        return _inner

    @pytest.mark.asyncio
    async def test_creates_customer(self, storage, customer, returned_func):
        customer_uid = customer.customer_uid + 1
        await returned_func(customer_uid=customer_uid)
        assert await storage.customer.get(customer_uid=customer_uid)

    @pytest.mark.asyncio
    async def test_nonexistent_service(self, service, get_order, returned_func):
        await returned_func(payments_service_id=service.payments_service_id + 1)
        with pytest.raises(OrderNotFoundStorageError):
            await get_order()

    @pytest.mark.asyncio
    async def test_not_enabled_service(self, storage, service, get_order, returned_func):
        service.enabled = False
        await storage.service.save(service)
        with pytest.raises(OrderNotFoundStorageError):
            await get_order()

    @pytest.mark.asyncio
    async def test_returns_created_order(self, get_order, returned):
        assert returned == await get_order()

    def test_created_order(self, default_kwargs, returned):
        self._check_order(default_kwargs, returned)

    @pytest.mark.asyncio
    class TestUpdate:
        @pytest.fixture
        def following_kwargs(self, get_kwargs, default_kwargs):
            kwargs = get_kwargs()
            kwargs['revision'] = default_kwargs['revision'] + 1
            return kwargs

        async def test_update__returns_updated_order(self, get_order, following_kwargs, returned_func):
            await returned_func()
            returned = await returned_func(**following_kwargs)
            assert returned == await get_order()

        async def test_update__updated_order(self, following_kwargs, returned_func):
            await returned_func()
            returned = await returned_func(**following_kwargs)
            TestCreateOrUpdateFromPaymentsOrderAction._check_order(following_kwargs, returned)

        async def test_update__respects_revision(self, get_order, following_kwargs, returned_func):
            order = await returned_func()
            following_kwargs['revision'] = order.order_revision
            new_order = returned_func(**following_kwargs)
            await_new_order = await new_order
            assert order == await_new_order
