import pytest


@pytest.fixture(params=(
    pytest.param(False, id='external'),
    pytest.param(True, id='internal'),
))
def internal_api(request):
    return request.param


@pytest.fixture
def hash_():
    return 'test-order-external-hash'


@pytest.fixture
def user_agent():
    return 'test-agent'


@pytest.fixture
def payment_completion_action(rands):
    return rands()


@pytest.fixture
def hash_test_data(hash_, user_agent):
    return {
        'path': f'/ext/{hash_}',
        'action_result': {'payment_url': 'test-order-external-post-payment-url'},
        'required_params': {
            'email': 'test-order-email',
            'return_url': 'test-order-return-url',
            'yandexuid': 'test-order-yandexuid',
        },
        'context': {'hash_': hash_, 'user_agent': user_agent, 'overwrite_return_url': False}
    }


@pytest.fixture
def order_id_test_data(service_merchant, payment_completion_action, order, tvm_client_id):
    return {
        'path': f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/start',
        'action_result': {'payment_url': 'test-order-trust-url'},
        'required_params': {
            'yandexuid': 'test-order-yandexuid',
            'payment_completion_action': payment_completion_action
        },
        'context': {
            'order_id': order.order_id,
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_tvm_id': tvm_client_id,
        }
    }


@pytest.fixture
def request_json_extra():
    return {}


@pytest.fixture
def request_json(request_json_extra, order_id_test_data):
    return {
        **order_id_test_data['required_params'],
        **request_json_extra,
    }


@pytest.fixture
async def response(request_json, response_func):
    return await response_func(request_json)


class TestContext:
    @pytest.fixture
    def action(self, internal_api, mock_action, test_data):
        if internal_api:
            from mail.payments.payments.core.actions.order.pay import StartOrderAction
            return mock_action(StartOrderAction, test_data['action_result'])
        else:
            from mail.payments.payments.core.actions.order.pay import PayOrderByHashAction
            return mock_action(PayOrderByHashAction, test_data['action_result'])

    @pytest.fixture(params=[
        {},
        {'customer_uid': 12345},
        {'description': 'some-description'},
    ])
    def request_json_extra(self, internal_api, request, test_data):
        if internal_api:
            return request.param
        else:
            return {
                **request.param,
                'select_customer_subscription': None
            }

    @pytest.fixture
    def test_data(self, internal_api, hash_test_data, order_id_test_data):
        if internal_api:
            return order_id_test_data
        else:
            return hash_test_data

    @pytest.fixture
    def request_json(self, request_json_extra, test_data):
        return {
            **test_data['required_params'],
            **request_json_extra,
        }

    @pytest.fixture
    def response_func(self, action, payments_client, test_data, user_agent):
        async def _inner(request_json):
            return await payments_client.post(test_data['path'], json=request_json,
                                              headers={'User-Agent': user_agent})

        return _inner

    def test_context(self, request_json, response, action, test_data):
        request_json.setdefault('customer_uid', None)
        action.assert_called_once_with(**request_json, **test_data['context'])


class TestContextHash:
    @pytest.fixture
    def action(self, mock_action, hash_test_data):
        from mail.payments.payments.core.actions.order.pay import PayOrderByHashAction
        return mock_action(PayOrderByHashAction, hash_test_data['action_result'])

    @pytest.fixture
    def request_json_extra(self):
        return {'hash_': 'hash in body does not override hash in path'}

    @pytest.fixture
    def request_json(self, request_json_extra, hash_test_data):
        return {
            **hash_test_data['required_params'],
            **request_json_extra,
            'select_customer_subscription': None,
        }

    @pytest.fixture
    def response_func(self, action, payments_client, hash_test_data, user_agent):
        async def _inner(request_json):
            return await payments_client.post(hash_test_data['path'], json=request_json,
                                              headers={'User-Agent': user_agent})

        return _inner

    def test_context(self, request_json, response, action, hash_test_data):
        request_json.setdefault('customer_uid', None)
        action.assert_called_once_with(**{**request_json, **hash_test_data['context']})

    @pytest.mark.asyncio
    async def test_response(self, hash_test_data, response):
        assert await response.json() == {
            'status': 'success',
            'code': 200,
            'data': {
                'payment_url': hash_test_data['action_result']['payment_url'],
            },
        }


class TestContextOrderID:
    @pytest.fixture
    def action(self, mock_action, order_id_test_data):
        from mail.payments.payments.core.actions.order.pay import StartOrderAction
        return mock_action(StartOrderAction, order_id_test_data['action_result'])

    @pytest.fixture(params=(
        pytest.param({'email': 'user_email'}, id='email'),
        pytest.param({'return_url': 'test-order-return-url'}, id='return_url'),
        pytest.param({'description': 'some-user-description'}, id='description'),
        pytest.param({'service_data': {'custom': 'json'}}, id='service_data'),
    ))
    def request_json_extra(self, request):
        return request.param

    @pytest.fixture
    def response_func(self, action, payments_client, order_id_test_data):
        async def _inner(request_json):
            return await payments_client.post(order_id_test_data['path'], json=request_json)

        return _inner

    def test_context(self, request_json, response, action, order_id_test_data):
        request_json.setdefault('customer_uid', None)
        action.assert_called_once_with(**{**request_json, **order_id_test_data['context']})

    @pytest.mark.asyncio
    async def test_response(self, order_id_test_data, response):
        assert await response.json() == {
            'status': 'success',
            'code': 200,
            'data': {
                'trust_url': order_id_test_data['action_result']['payment_url'],
            },
        }
