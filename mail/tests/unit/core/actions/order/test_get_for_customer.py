import pytest

from mail.ohio.ohio.core.actions.order.get_for_customer import GetForCustomerOrderAction


@pytest.fixture
async def orders(create_order):
    return [await create_order() for _ in range(3)][::-1]


@pytest.fixture
def returned_func():
    async def _inner(*args, **kwargs):
        return await GetForCustomerOrderAction(*args, **kwargs).run()

    return _inner


@pytest.mark.asyncio
async def test_find_for_customer_call(mocker, storage, customer, order, returned_func):
    async def dummy_find_for_customer(*args, **kwargs):
        for _ in []:
            yield
    mock = mocker.patch.object(
        type(storage.order),
        'find_for_customer',
        mocker.Mock(side_effect=dummy_find_for_customer),
    )
    kwargs = dict(
        service_ids=[1],
        subservice_ids=[2],
        created_keyset=3,
        order_id_keyset=4,
        limit=5,
    )
    await returned_func(customer.customer_uid, **kwargs)
    mock.assert_called_once_with(customer_uid=customer.customer_uid, **kwargs)


@pytest.mark.asyncio
async def test_returned(customer, orders, returned_func):
    next_keyset = {
        'created_keyset': orders[-1].created,
        'order_id_keyset': orders[-1].order_id,
    }
    returned_orders, returned_next_keyset = await returned_func(customer.customer_uid)
    assert (
        orders == returned_orders
        and next_keyset == returned_next_keyset
    )
