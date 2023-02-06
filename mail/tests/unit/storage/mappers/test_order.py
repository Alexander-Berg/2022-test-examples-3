from copy import deepcopy
from datetime import datetime, timedelta, timezone
from decimal import Decimal

import pytest

from sendr_utils import alist

from hamcrest import assert_that, contains_inanyorder, equal_to

from mail.ohio.ohio.core.entities.order import NDS, Item, Order, OrderData, OrderStatus, Refund, RefundStatus
from mail.ohio.ohio.core.entities.order_service import OrderService
from mail.ohio.ohio.storage.exceptions import OrderNotFoundStorageError


@pytest.fixture(autouse=True)
def utcnow_mock(mocker):
    now = datetime(2020, 6, 24, 15, 36, tzinfo=timezone.utc)
    return mocker.patch(
        'mail.ohio.ohio.storage.mappers.order.utcnow',
        mocker.Mock(return_value=now),
    )


@pytest.fixture
def order_entity(randn, rands, customer, service):
    return Order(
        customer_uid=customer.customer_uid,
        service_id=service.service_id,
        subservice_id=str(randn()),
        merchant_uid=randn(),
        service_merchant_id=randn(),
        payments_order_id=randn(),
        trust_payment_id=rands(),
        trust_purchase_token=rands(),
        status=OrderStatus.PAID,
        order_data=OrderData(
            total=Decimal('123.45'),
            currency=rands(),
            description=rands(),
            items=[
                Item(
                    amount=Decimal('123.45'),
                    price=Decimal('67.89'),
                    currency=rands(),
                    nds=NDS.NDS_NONE,
                    name=rands(),
                    image_url=rands(),
                    image_path=rands(),
                ),
            ],
            refunds=[
                Refund(
                    trust_refund_id=rands(),
                    refund_status=RefundStatus.CREATED,
                    total=Decimal('123.45'),
                    currency=rands(),
                    items=[
                        Item(
                            amount=Decimal('123.45'),
                            price=Decimal('67.89'),
                            currency=rands(),
                            nds=NDS.NDS_NONE,
                            name=rands(),
                        ),
                    ],
                )
            ],
        ),
        order_revision=randn(),
        service_data={'custom': 'id'},
        service_revision=randn(),
        created=datetime(2020, 7, 6, 12, 17, 30, tzinfo=timezone.utc),
    )


@pytest.fixture
async def order(storage, order_entity):
    return await storage.order.create(deepcopy(order_entity))


@pytest.mark.asyncio
async def test_create(storage, utcnow_mock, customer, order_entity):
    order = await storage.order.create(deepcopy(order_entity))
    order_entity.order_id = customer.next_order_id
    order_entity.updated = utcnow_mock.return_value
    assert_that(order, equal_to(order_entity))


@pytest.mark.asyncio
async def test_get_not_found(storage, randn):
    with pytest.raises(OrderNotFoundStorageError):
        await storage.order.get(customer_uid=randn(), order_id=randn())


@pytest.mark.asyncio
async def test_get(storage, order):
    assert order == await storage.order.get(customer_uid=order.customer_uid, order_id=order.order_id)


@pytest.mark.asyncio
async def test_get_by_trust_purchase_token_not_found(storage, randn, rands):
    with pytest.raises(OrderNotFoundStorageError):
        await storage.order.get_by_trust_purchase_token(customer_uid=randn(), trust_purchase_token=rands())


@pytest.mark.asyncio
async def test_get_by_trust_purchase_token(storage, order):
    assert order == await storage.order.get_by_trust_purchase_token(
        customer_uid=order.customer_uid,
        trust_purchase_token=order.trust_purchase_token,
    )


@pytest.mark.asyncio
class TestGetServices:
    def _get_order_services(self, orders):
        return [OrderService(order.service_id, order.subservice_id) for order in orders]

    @pytest.fixture
    async def services(self, create_service):
        return [await create_service() for _ in range(3)]

    @pytest.fixture
    async def orders(self, randn, create_order, services):
        return [
            await create_order(
                service_id=service.service_id,
                subservice_id=str(service.service_id),
            )
            for service in services
        ]

    @pytest.fixture
    def returned_func(self, storage, customer, orders):
        async def _inner(**kwargs):
            kwargs = {'customer_uid': customer.customer_uid, **kwargs}
            return await alist(storage.order.get_services(**kwargs))

        return _inner

    async def test_filters_by_customer_uid(self, create_customer, create_order, customer, orders, returned):
        customer2 = await create_customer()
        await create_order(customer_uid=customer2.customer_uid)
        assert_that(returned, contains_inanyorder(*self._get_order_services(orders)))

    async def test_returns_distinct_among_last_limit(self, randn, create_order, services, returned_func):
        # Each pair of service_id, subservice_id is used twice in a row, hence [::2] below.
        new_orders = [
            await create_order(service_id=service.service_id, subservice_id=subservice_id)
            for service in services
            for subservice_id in (None, str(randn()), str(randn()))
            for _ in range(2)
        ]
        assert_that(
            await returned_func(limit=len(new_orders)),
            contains_inanyorder(*self._get_order_services(new_orders[::2]))
        )


@pytest.mark.asyncio
async def test_save_not_found(storage, order):
    order.order_id += 1
    with pytest.raises(OrderNotFoundStorageError):
        await storage.order.save(order)


@pytest.mark.asyncio
async def test_save(randn, rands, storage, utcnow_mock, order):
    # updatable fields
    subservice_id_num = randn()
    order.subservice_id = str(subservice_id_num if subservice_id_num != 1 else subservice_id_num + 1)
    order.merchant_uid = randn()
    order.service_merchant_id = randn()
    order.payments_order_id = randn()
    order.trust_payment_id = rands()
    order.trust_purchase_token = rands()
    order.status = OrderStatus.REFUNDED
    order.order_data = OrderData(
        total=Decimal('11.22'),
        currency=rands(),
        items=[],
        description=rands(),
        refunds=[],
    )
    order.order_revision += 1
    order.service_data = {'new': 'data'}
    order.service_revision += 1
    order.created -= timedelta(hours=1)

    updated_order = deepcopy(order)
    # non-updatable fields
    updated_order.service_id += 1
    updated_order.updated -= timedelta(hours=1)

    utcnow_mock.return_value += timedelta(minutes=1)
    order.updated = utcnow_mock.return_value

    updated_order = await storage.order.save(updated_order)
    assert order == updated_order


@pytest.mark.asyncio
class TestFindForCustomer:
    @pytest.fixture
    def created_list(self):
        return [
            datetime(2020, 6, 29, 12, tzinfo=timezone.utc),
            datetime(2020, 6, 29, 15, tzinfo=timezone.utc),
            datetime(2020, 6, 29, 15, tzinfo=timezone.utc),
            datetime(2020, 6, 29, 10, tzinfo=timezone.utc),
        ]

    @pytest.fixture
    async def orders(self, utcnow_mock, create_order, created_list):
        orders = []
        for created in created_list:
            utcnow_mock.return_value = created
            orders.append(await create_order())
        orders.sort(key=lambda order: (order.created, order.order_id), reverse=True)
        return orders

    @pytest.fixture
    def returned_func(self, storage):
        async def _inner(*args, **kwargs):
            return await alist(storage.order.find_for_customer(*args, **kwargs))

        return _inner

    async def test_filters_by_customer_uid__found(self, order, returned_func):
        assert [order] == await returned_func(order.customer_uid)

    async def test_filters_by_customer_uid__not_found(self, order, returned_func):
        assert [] == await returned_func(order.customer_uid + 1)

    async def test_filters_by_service_id__found(self, order, returned_func):
        assert [order] == await returned_func(order.customer_uid, service_ids=[order.service_id])

    async def test_filters_by_service_id__not_found(self, order, returned_func):
        assert [] == await returned_func(order.customer_uid, service_ids=[order.service_id + 1])

    # TODO fix this
    # async def test_filters_by_subservice_id__found(self, order, returned_func):
    #     assert [order] == await returned_func(order.customer_uid, subservice_ids=[order.subservice_id])

    async def test_filters_by_subservice_id__not_found(self, order, returned_func):
        assert [] == await returned_func(order.customer_uid, subservice_ids=[order.subservice_id + '2'])

    async def test_filters_out_orders_without_created(self, create_order, returned_func):
        order = await create_order(created=None)
        assert [] == await returned_func(order.customer_uid)

    async def test_limit(self, customer, create_order, returned_func):
        for _ in range(5):
            await create_order()
        assert 3 == len(await returned_func(customer.customer_uid, limit=3))

    async def test_order(self, customer, returned_func, orders):
        assert orders == await returned_func(customer.customer_uid)

    async def test_keyset(self, orders, returned_func):
        assert all([
            orders[i + 1:] == await returned_func(
                order.customer_uid,
                created_keyset=order.created,
                order_id_keyset=order.order_id,
            )
            for i, order in enumerate(orders)
        ])
