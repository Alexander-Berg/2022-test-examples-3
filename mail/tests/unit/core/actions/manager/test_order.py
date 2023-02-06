from contextlib import contextmanager
from datetime import datetime

import pytest

from hamcrest import assert_that, has_properties

from mail.payments.payments.core.actions.manager.order import GetOrderListManagerAction
from mail.payments.payments.core.actions.order.get_list import CoreGetOrderListAction, OrderListResult
from mail.payments.payments.core.entities.enums import OrderKind, PayStatus, RefundStatus


class TestGetOrderListManagerAction:
    @pytest.fixture(params=('assessor', 'admin'))
    def acting_manager(self, request, managers):
        return managers[request.param]

    @pytest.fixture
    def decrypted(self, order):
        return {
            'uid': order.uid,
            'order_id': order.order_id,
        }

    @pytest.fixture(autouse=True)
    def crypto(self, mocker, decrypted):
        @contextmanager
        def dummy_decrypt(*args):
            yield decrypted

        mock = mocker.Mock()
        mock.decrypt_order = mock.decrypt_payment = mock.decrypt = dummy_decrypt
        GetOrderListManagerAction.context.crypto = mock

    @pytest.fixture(params=(
        {},
        {
            'limit': 5,
            'offset': 10
        },
        {
            'sort_by': 'created',
            'desc': False,
            'updated_from': datetime.utcnow(),
            'updated_to': datetime.utcnow(),
            'uid': 123,
            'order_id': 345,
            'original_order_id': 12,
            'email_query': 'trs',
            'kinds': [OrderKind.PAY]
        },
        {
            'order_hash': 'some_hash'
        },
        {
            'pay_statuses': [PayStatus.CANCELLED],
            'refund_statuses': [RefundStatus.COMPLETED]
        },
    ))
    def params(self, request, acting_manager):
        return {
            **request.param,
            'manager_uid': acting_manager.uid,
        }

    @pytest.fixture
    def core_order_list_action_result(self, order):
        result = OrderListResult([order])
        result.total_found_count = 1
        return result

    @pytest.fixture(autouse=True)
    def get_order_list_action_calls(self, mock_action, core_order_list_action_result):
        return mock_action(CoreGetOrderListAction, core_order_list_action_result).call_args_list

    @pytest.fixture
    def returned_func(self, params):
        async def _inner():
            return await GetOrderListManagerAction(**params).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    def test_get_order_list_action_calls(self, order, get_order_list_action_calls, returned, params):
        expected_kwargs = {**params, 'count_found': True}
        if params.get('order_hash'):
            expected_kwargs.update({'uid': order.uid, 'order_id': order.order_id})
        expected_kwargs.pop('manager_uid')
        expected_kwargs.pop('order_hash', None)

        assert expected_kwargs == get_order_list_action_calls[0][1]

    @pytest.mark.asyncio
    async def test_returned(self, order, returned, core_order_list_action_result, storage):
        # total = await storage.order.get_found_count()
        assert_that(returned, has_properties(dict(
            orders=core_order_list_action_result,
            stats=has_properties(dict(
                # total=total,
                total=0,  # OPLATASUPPORT-71, PAYBACK-917
                found=core_order_list_action_result.total_found_count
            ))
        )))
