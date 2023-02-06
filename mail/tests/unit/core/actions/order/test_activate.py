from typing import Union

import pytest

from mail.payments.payments.core.actions.order.activate import ActivateOrderAction, ActivateOrderServiceMerchantAction
from mail.payments.payments.core.exceptions import OrderNotFoundError
from mail.payments.payments.tests.base import BaseTestOrderAction


def get_action_class(params) -> Union[ActivateOrderServiceMerchantAction, ActivateOrderAction]:
    if params.get('service_merchant_id'):
        return ActivateOrderServiceMerchantAction(**params)
    return ActivateOrderAction(**params)


class TestOrderActiveAction(BaseTestOrderAction):
    @pytest.fixture
    def active_order_data(self, service_merchant):
        return {}

    @pytest.fixture
    def order_data(self, active_order_data, service_merchant):
        active_order_data.update({'service_merchant_id': service_merchant.service_merchant_id})
        return active_order_data

    @pytest.fixture
    def items(self):
        return 'fake items'

    @pytest.fixture(params=(
        pytest.param(True, id='active'),
        pytest.param(False, id='not active'),
    ))
    def active(self, request):
        return request.param

    @pytest.fixture(params=('uid', 'service_merchant'))
    def params(self, request, merchant, service_client, service_merchant, active, order):
        data = {
            'uid': {'uid': merchant.uid},
            'service_merchant': {
                'service_tvm_id': service_client.tvm_id,
                'service_merchant_id': service_merchant.service_merchant_id
            }
        }

        return {
            'order_id': order.order_id,
            'active': active,
            **data[request.param],
        }

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await get_action_class(params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.fixture
    async def updated_order(self, storage, returned):
        return await storage.order.get(returned.uid, returned.order_id)

    @pytest.mark.asyncio
    async def test_not_found(self, params):
        params['order_id'] += 1
        with pytest.raises(OrderNotFoundError):
            await get_action_class(params).run()

    @pytest.mark.asyncio
    async def test_active_none(self, params, order):
        params['active'] = None
        returned = await get_action_class(params).run()
        assert returned.active == order.active

    @pytest.mark.parametrize('active_order_data,active', (
        ({'active': True}, False),
        ({'active': False}, True),
    ))
    def test_updates_order(self, order, updated_order, active):
        assert all([
            updated_order.uid == order.uid,
            updated_order.order_id == order.order_id,
            updated_order.revision > order.revision,
            updated_order.active == active,
        ])

    @pytest.mark.parametrize('active_order_data,active', (
        ({'active': True}, True),
        ({'active': False}, False),
    ))
    def test_does_not_update_order(self, order, updated_order, active):
        assert all([
            updated_order.uid == order.uid,
            updated_order.order_id == order.order_id,
            updated_order.revision == order.revision,
            updated_order.active == active,
        ])

    def test_returns_updated(self, returned, updated_order):
        assert all([
            returned.uid == updated_order.uid,
            returned.order_id == updated_order.order_id,
            returned.revision == updated_order.revision,
            returned.active == updated_order.active,
        ])

    @pytest.mark.asyncio
    @pytest.mark.parametrize('active_order_data,active', (
        ({'active': True}, False),
        ({'active': False}, True),
    ))
    async def test_send_to_history_task_created(self, storage, updated_order):
        task = await (storage.task.find()).__anext__()
        assert all((
            task.action_name == 'send_to_history_order_action',
            task.params['action_kwargs'] == {'uid': updated_order.uid, 'order_id': updated_order.order_id},
        ))
