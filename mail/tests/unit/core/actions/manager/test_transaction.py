from datetime import datetime
from random import randint

import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.actions.manager.transaction import GetTransactionListManagerAction
from mail.payments.payments.core.entities.common import SearchStats
from mail.payments.payments.core.entities.enums import PayStatus, TransactionStatus


class TestGetTransactionListManagerAction:
    @pytest.fixture(params=('assessor', 'admin'))
    def acting_manager(self, request, managers):
        return managers[request.param]

    @pytest.fixture(autouse=True)
    async def setup_order_acquirer(self, order, merchant, storage):
        order.acquirer = merchant.acquirer
        await storage.order.save(order)

    @pytest.fixture
    def params(self, merchant, order):
        return {'uid': merchant.uid, 'order_id': order.order_id}

    @pytest.fixture
    def returned_func(self, acting_manager, params, transaction):
        async def _inner():
            return await GetTransactionListManagerAction(
                **params,
                manager_uid=acting_manager.uid,
            ).run()

        return _inner

    @pytest.fixture
    async def returned(self, returned_func):
        return await returned_func()

    @pytest.mark.parametrize('params', (
        {
            'uid': randint(1, 10 ** 9),
            'tx_id': randint(1, 10 ** 9),
            'order_id': randint(1, 10 ** 9),
            'statuses': [TransactionStatus.CANCELLED, TransactionStatus.FAILED],
            'email': 'email',
            'lower_created_dt': datetime.utcnow(),
            'upper_created_dt': datetime.utcnow(),
            'lower_updated_dt': datetime.utcnow(),
            'upper_updated_dt': datetime.utcnow(),
            'limit': randint(1, 100),
            'offset': randint(1, 10),
            'sort_by': 'updated',
            'desc': False,
            'order_pay_statuses': [PayStatus.CANCELLED],
            'customer_uid': randint(1, 10 ** 9),
            'services': [randint(1, 10 ** 9), randint(1, 10 ** 9)],
        },
    ))
    @pytest.mark.asyncio
    async def test_find_call(self, mocker, transaction, params, returned_func):
        from mail.payments.payments.storage.mappers.transaction import TransactionMapper
        find = mocker.spy(TransactionMapper, 'find')
        await returned_func()
        find_kwargs = find.call_args[1]
        assert_that(
            find_kwargs,
            has_entries({
                'uid': params['uid'],
                'order_id': params['order_id'],
                'statuses': params['statuses'],
                'limit': params['limit'],
                'offset': params['offset'],
                'tx_id': params['tx_id'],
                'email_query': params['email'],
                'created_from': params['lower_created_dt'],
                'created_to': params['upper_created_dt'],
                'updated_from': params['lower_updated_dt'],
                'updated_to': params['upper_updated_dt'],
                'order_pay_statuses': params['order_pay_statuses'],
                'sort_by': params['sort_by'],
                'descending': params['desc'],
                'customer_uid': params['customer_uid'],
                'services': params['services'],
            })
        )

    def test_returned(self, transaction, order, returned):
        transaction.set_trust_receipt_urls(order.acquirer)
        assert all((
            returned.transactions == [transaction],
            returned.stats == SearchStats(total=1, found=1)
        ))
