import pytest

from mail.ohio.ohio.core.actions.order.get_by_id import GetByIdOrderAction
from mail.ohio.ohio.core.exceptions import OrderNotFoundError


@pytest.fixture
def returned_func(order):
    async def _inner(**kwargs):
        kwargs = {'customer_uid': order.customer_uid, 'order_id': order.order_id, **kwargs}
        return await GetByIdOrderAction(**kwargs).run()

    return _inner


def test_returns_order(order, returned):
    assert order == returned


@pytest.mark.asyncio
async def test_raises_not_found(randn, returned_func):
    with pytest.raises(OrderNotFoundError):
        await returned_func(customer_uid=randn(), order_id=randn())
