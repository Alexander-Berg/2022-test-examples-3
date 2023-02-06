from datetime import datetime

import pytest

from hamcrest import assert_that, contains_inanyorder, has_entries

from mail.payments.payments.core.entities.common import SearchStats
from mail.payments.payments.core.entities.enums import PAY_METHOD_OFFLINE, OrderKind
from mail.payments.payments.core.entities.order import OrdersAdminData


@pytest.fixture
def run_action_result(order):
    return OrdersAdminData(orders=[order], stats=SearchStats(total=1, found=1))


@pytest.fixture(autouse=True)
def action(mock_action, run_action_result):
    from mail.payments.payments.core.actions.manager.order import GetOrderListManagerAction
    return mock_action(GetOrderListManagerAction, run_action_result)


@pytest.fixture(params=('assessor', 'admin'))
def acting_manager(request, managers):
    return managers[request.param]


@pytest.fixture
def req_params():
    return {}


@pytest.fixture
async def response(admin_client, url, req_params, tvm):
    return await admin_client.get(url, params=req_params)


class TestTicketExceptionsV1AndV2:
    @pytest.fixture(params=('/admin/api/v1/order', '/admin/api/v2/order'))
    def url(self, request):
        return request.param

    @pytest.mark.asyncio
    async def test_tvm_service_ticket_exception(self, url, payments_settings, admin_client, req_params, tvm):
        payments_settings.TVM_CHECK_SERVICE_TICKET = True
        payments_settings.TVM_ADMIN_API_ALLOWED_CLIENTS = ()
        response = await admin_client.get(url, params=req_params)
        resp_json = await response.json()
        assert all((
            resp_json['code'] == 403,
            resp_json['data']['message'] == 'Service not authorized'
        ))

    @pytest.mark.asyncio
    async def test_missing_user_ticket(self, url, admin_client, req_params, tvm):
        tvm.default_uid = None
        response = await admin_client.get(url, params=req_params)
        resp_json = await response.json()
        assert all((resp_json['code'] == 403,
                    resp_json['data']['message'] == 'Missing User-Ticket'))


class TestOrderHandlerV1:
    @pytest.fixture
    def url(self):
        return '/admin/api/v1/order'

    @pytest.fixture(params=({},
                            {'limit': 5,
                             'offset': 10},
                            {'sort_by': 'created',
                             'descending': 'False',
                             'created_from': str(datetime.utcnow()),
                             'created_to': str(datetime.utcnow()),
                             'updated_from': str(datetime.utcnow()),
                             'updated_to': str(datetime.utcnow()),
                             'merchant_uid': 123,
                             'order_id': 345,
                             'original_order_id': 12,
                             'email_query': 'trs',
                             'kinds[]': OrderKind.PAY.value},
                            {'order_hash': 'some_hash'},
                            {'parent_order_id': 1},
                            {'is_active': 'true'},
                            {'is_active': 'false'},
                            {'pay_method': PAY_METHOD_OFFLINE},
                            {'subscription': 'false'},
                            {'subscription': 'true'},
                            {'subscription': 'null'},
                            ))
    def req_params(self, request):
        return request.param

    @pytest.fixture
    def expected_action_params(self, req_params):
        params = dict(req_params)
        if params.get('merchant_uid') is not None:
            params['uid'] = params.pop('merchant_uid')
        if params.get('descending') is not None:
            params['descending'] = (params['descending'] == 'True')
        if params.get('created_from') is not None:
            params['created_from'] = datetime.strptime(params['created_from'], '%Y-%m-%d %H:%M:%S.%f')
        if params.get('created_to') is not None:
            params['created_to'] = datetime.strptime(params['created_to'], '%Y-%m-%d %H:%M:%S.%f')
        if params.get('updated_from') is not None:
            params['updated_from'] = datetime.strptime(params['updated_from'], '%Y-%m-%d %H:%M:%S.%f')
        if params.get('updated_to') is not None:
            params['updated_to'] = datetime.strptime(params['updated_to'], '%Y-%m-%d %H:%M:%S.%f')
        if params.get('kinds[]') is not None:
            params['kinds'] = [OrderKind(params.pop('kinds[]'))]
        if params.get('is_active') is not None:
            params['is_active'] = params['is_active'] == 'true'
        if params.get('subscription') is not None:
            value = params['subscription']
            params['subscription'] = False if value == 'false' else True if value == 'true' else None
        else:
            params['subscription'] = False
        return params

    @pytest.mark.asyncio
    async def test_response(self, response, run_action_result):
        resp_json = await response.json()
        some_order = run_action_result.orders[0]
        assert_that(
            resp_json['data'],
            contains_inanyorder(has_entries(uid=some_order.uid, order_id=some_order.order_id)),
        )

    def test_params(self, response, action, expected_action_params, acting_manager):
        expected_action_params.setdefault('limit', 100)
        expected_action_params.setdefault('offset', 0)
        expected_action_params['manager_uid'] = acting_manager.uid
        action.assert_called_once_with(**expected_action_params)


class TestOrderHandlerV2:
    @pytest.fixture
    def url(self):
        return '/admin/api/v2/order'

    @pytest.fixture(params=(
        {},
        {'limit': 5,
         'offset': 10},
        {'sort_by': 'created',
         'descending': 'False',
         'created_from': str(datetime.utcnow()),
         'created_to': str(datetime.utcnow()),
         'updated_from': str(datetime.utcnow()),
         'updated_to': str(datetime.utcnow()),
         'merchant_uid': 123,
         'order_id': 345,
         'original_order_id': 12,
         'email_query': 'trs',
         'kinds[]': OrderKind.PAY.value},
        {'order_hash': 'some_hash'},
        {'parent_order_id': 1},
        {'is_active': 'true'},
        {'is_active': 'false'},
        {'pay_method': PAY_METHOD_OFFLINE},
        {'subscription': 'false'},
        {'subscription': 'true'},
        {'subscription': 'null'},
    ))
    def req_params(self, request):
        return request.param

    @pytest.fixture
    def expected_action_params(self, req_params):
        params = dict(req_params)
        if params.get('merchant_uid') is not None:
            params['uid'] = params.pop('merchant_uid')
        if params.get('descending') is not None:
            params['descending'] = (params['descending'] == 'True')
        if params.get('created_from') is not None:
            params['created_from'] = datetime.strptime(params['created_from'], '%Y-%m-%d %H:%M:%S.%f')
        if params.get('created_to') is not None:
            params['created_to'] = datetime.strptime(params['created_to'], '%Y-%m-%d %H:%M:%S.%f')
        if params.get('updated_from') is not None:
            params['updated_from'] = datetime.strptime(params['updated_from'], '%Y-%m-%d %H:%M:%S.%f')
        if params.get('updated_to') is not None:
            params['updated_to'] = datetime.strptime(params['updated_to'], '%Y-%m-%d %H:%M:%S.%f')
        if params.get('kinds[]') is not None:
            params['kinds'] = [OrderKind(params.pop('kinds[]'))]
        if params.get('is_active') is not None:
            params['is_active'] = params['is_active'] == 'true'
        if params.get('subscription') is not None:
            value = params['subscription']
            params['subscription'] = False if value == 'false' else True if value == 'true' else None
        else:
            params['subscription'] = False
        return params

    @pytest.mark.asyncio
    async def test_response(self, response, run_action_result):
        resp_json = await response.json()
        some_order = run_action_result.orders[0]
        assert_that(resp_json['data'], has_entries(dict(
            orders=contains_inanyorder(
                has_entries(
                    uid=some_order.uid,
                    order_id=some_order.order_id,
                    trust_refund_id=some_order.trust_refund_id,
                )
            ),
            total=run_action_result.stats.total,
            found=run_action_result.stats.found,
        )))

    def test_params(self, response, action, expected_action_params, acting_manager):
        expected_action_params.setdefault('limit', 100)
        expected_action_params.setdefault('offset', 0)
        expected_action_params['manager_uid'] = acting_manager.uid
        action.assert_called_once_with(**expected_action_params)


class TestOrderHandlerWithBadParamsV1AndV2:
    @pytest.fixture(params=('/admin/api/v1/order', '/admin/api/v2/order'))
    def url(self, request):
        return request.param

    @pytest.fixture(params=({'limit': 1000},))
    def req_params(self, request):
        return request.param

    def test_error(self, response):
        assert response.status == 400
