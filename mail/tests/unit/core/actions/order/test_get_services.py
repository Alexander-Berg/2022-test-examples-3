import pytest

from hamcrest import assert_that, contains_inanyorder

from mail.ohio.ohio.core.actions.order.get_services import GetServicesOrderAction
from mail.ohio.ohio.core.entities.order_service import OrderService


@pytest.fixture(autouse=True)
def setup(ohio_settings):
    ohio_settings.GET_SERVICES_LIMIT = 1000


@pytest.fixture
async def services(create_service):
    return [await create_service() for _ in range(2)]


@pytest.fixture
async def orders(rands, services, create_order):
    return [
        await create_order(service_id=service.service_id, subservice_id=rands())
        for service in services
        for _ in range(2)
    ]


@pytest.fixture
def returned_func(customer, orders):
    async def _inner(**kwargs):
        kwargs = {'customer_uid': customer.customer_uid, **kwargs}
        return await GetServicesOrderAction(**kwargs).run()

    return _inner


def test_returns_order_services(orders, returned):
    assert_that(
        returned,
        contains_inanyorder(*[OrderService(order.service_id, order.subservice_id) for order in orders]),
    )


@pytest.mark.asyncio
async def test_returns_unique_order_services(create_order, orders, returned_func):
    for order in orders:
        await create_order(service_id=order.service_id, subservice_id=order.subservice_id)
    assert_that(
        await returned_func(),
        contains_inanyorder(*[OrderService(order.service_id, order.subservice_id) for order in orders]),
    )


@pytest.mark.asyncio
async def test_returns_latest_order_services(rands, ohio_settings, create_order, returned_func):
    limit = ohio_settings.GET_SERVICES_LIMIT = 5
    orders = [await create_order(subservice_id=rands()) for _ in range(limit)]
    assert_that(
        await returned_func(),
        contains_inanyorder(*[OrderService(order.service_id, order.subservice_id) for order in orders]),
    )
