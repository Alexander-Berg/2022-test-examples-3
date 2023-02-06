import pytest

from hamcrest import assert_that, contains, has_entries, has_properties

from mail.payments.payments.core.actions.order.update_service_data import UpdateServiceDataServiceMerchantAction
from mail.payments.payments.core.exceptions import OrderNotFoundError


@pytest.fixture
async def order(service_merchant, create_order):
    return await create_order(service_merchant_id=service_merchant.service_merchant_id)


@pytest.fixture
def service_data(rands):
    return {rands(): rands()}


@pytest.fixture
def returned_func(service_merchant, order, service_data):
    async def _inner(**kwargs):
        kwargs = {
            'service_merchant_id': service_merchant.service_merchant_id,
            'order_id': order.order_id,
            'service_data': service_data,
            **kwargs
        }
        return await UpdateServiceDataServiceMerchantAction(**kwargs).run()

    return _inner


@pytest.mark.asyncio
async def test_updates_service_data(storage, service_data, order, returned):
    updated_order = await storage.order.get(order.uid, order.order_id)
    order.data.service_data = service_data
    order.revision = updated_order.revision
    order.updated = updated_order.updated
    assert all((
        order == updated_order,
        order == returned,
    ))


@pytest.mark.asyncio
async def test_order_without_service_merchant_not_found(storage, order, returned_func):
    order.service_merchant_id = None
    await storage.order.save(order)
    with pytest.raises(OrderNotFoundError):
        await returned_func()


@pytest.mark.asyncio
async def test_creates_send_to_order_task(get_tasks, order, returned):
    assert_that(
        await get_tasks(),
        contains(has_properties({
            'action_name': 'send_to_history_order_action',
            'params': has_entries({
                'action_kwargs': {'uid': order.uid, 'order_id': order.order_id},
            }),
        }))
    )
