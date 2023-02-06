from datetime import datetime

import pytest
from multidict import MultiDict

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.common import SearchStats
from mail.payments.payments.core.entities.enums import TransactionStatus
from mail.payments.payments.core.entities.transaction import TransactionsAdminData


def transaction_statuses_params():
    params = MultiDict()
    params.add('statuses[]', TransactionStatus.CANCELLED.value)
    params.add('statuses[]', TransactionStatus.FAILED.value)
    return params


class BaseTestTransactionHandler:
    @pytest.fixture
    def run_action_result(self, transaction, order):
        transaction.order = order
        return TransactionsAdminData(transactions=[transaction], stats=SearchStats(total=1, found=1))

    @pytest.fixture(autouse=True)
    def action(self, mock_action, transaction, order, run_action_result):
        from mail.payments.payments.core.actions.manager.transaction import GetTransactionListManagerAction
        return mock_action(GetTransactionListManagerAction, run_action_result)

    @pytest.fixture(params=('assessor', 'admin'))
    def acting_manager(self, request, managers):
        return managers[request.param]

    @pytest.fixture
    def req_params(self):
        return {}

    @pytest.fixture
    async def response(self, admin_client, req_params, tvm):
        return await admin_client.get('/admin/api/v1/transaction', params=req_params)


class TestTransactionHandler(BaseTestTransactionHandler):
    @pytest.fixture(params=({},
                            {'email': 'some@email.ru',
                             'tx_id': 123890,
                             'lower_updated_dt': datetime.utcnow().isoformat(),
                             'upper_updated_dt': datetime.utcnow().isoformat(),
                             'limit': 5,
                             'offset': 10},
                            {'merchant_uid': 123,
                             'order_id': 345,
                             'customer_uid': 1,
                             'services[]': 12345,
                             'lower_created_dt': datetime.utcnow().isoformat(),
                             'upper_created_dt': datetime.utcnow().isoformat(),
                             'statuses[]': TransactionStatus.CLEARED.value},
                            transaction_statuses_params(),
                            ))
    def req_params(self, request):
        return request.param

    @pytest.fixture
    def expected_action_params(self, req_params):
        params = dict(req_params)
        if req_params.get('merchant_uid') is not None:
            params['uid'] = req_params.get('merchant_uid')
            params.pop('merchant_uid')
        if req_params.get('statuses[]') is not None:
            if isinstance(req_params, dict):
                params['statuses'] = [TransactionStatus(req_params.get('statuses[]'))]
            else:
                params['statuses'] = [TransactionStatus(tx_status)
                                      for tx_status in req_params.getall('statuses[]')]
            params.pop('statuses[]')
        if req_params.get('services[]') is not None:
            params['services'] = [req_params.get('services[]')]
            params.pop('services[]')
        for key, value in req_params.items():
            if key in ('lower_created_dt', 'upper_created_dt', 'lower_updated_dt', 'upper_updated_dt'):
                params[key] = datetime.fromisoformat(value)

        return params

    def test_context(self, response, action, acting_manager, expected_action_params):
        assert_that(action.call_args[1], has_entries({
            'limit': 100,
            'offset': 0,
            **expected_action_params,
            'manager_uid': acting_manager.uid,
        }))


class TestTransactionHandlerWithBadParams(BaseTestTransactionHandler):
    @pytest.fixture(params=(
        {'limit': 1000},
    ))
    def req_params(self, request):
        return request.param

    def test_error(self, response):
        assert response.status == 400


class TestTransactionHandlerV2(TestTransactionHandler):
    @pytest.fixture
    async def response(self, admin_client, req_params, tvm):
        return await admin_client.get('/admin/api/v2/transaction', params=req_params)

    @pytest.mark.asyncio
    async def test_response(self, response, run_action_result):
        resp_json = await response.json()
        assert_that(resp_json['data'], has_entries({
            'found': run_action_result.stats.found,
            'total': run_action_result.stats.total
        }))
