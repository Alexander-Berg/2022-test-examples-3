import pytest

from mail.payments.payments.tests.base import BaseTestOrderList

from ..test_order import TestOrderActive as BaseTestOrderActive
from ..test_order import TestOrderGet as BaseTestOrderGet
from ..test_order import TestOrderListPost as BaseTestOrderListPost
from ..test_order import TestOrderPut as BaseTestOrderPut


class TestOrderByHashExternal:
    @pytest.fixture
    def hash_(self):
        return 'test-order-external-hash'

    @pytest.fixture
    def user_agent(self):
        return 'test-external-agent'

    class TestGet:
        @pytest.fixture
        def action(self, mock_action):
            from mail.payments.payments.core.actions.order.get_by_hash import GetOrderByHashAction
            return mock_action(GetOrderByHashAction)

        @pytest.fixture
        async def response(self, action, payments_client, hash_):
            return await payments_client.get(f'/ext/{hash_}')

        def test_get__params(self, crypto_mock, hash_, response, action):
            action.assert_called_once_with(hash_=hash_)

    class TestPost:
        @pytest.fixture
        def action(self, mock_action):
            from mail.payments.payments.core.actions.order.pay import PayOrderByHashAction
            return mock_action(PayOrderByHashAction)

        @pytest.fixture
        def run_action_result(self, rands):
            return {'payment_url': rands()}

        @pytest.fixture
        def request_json(self, rands, randurl, randmail, randn):
            return {
                "yandexuid": rands(),
                "customer_uid": randn(),
                "email": f' {randmail()} ',
                "return_url": randurl(),
                "description": rands()
            }

        @pytest.fixture
        async def response(self, payments_client, request_json, hash_, user_agent):
            return await payments_client.post(f'/ext/{hash_}', json=request_json, headers={'User-Agent': user_agent})

        def test_post__params(self, action, request_json, hash_, user_agent, response):
            action.assert_called_once_with(**{
                'hash_': hash_,
                'yandexuid': request_json['yandexuid'],
                'customer_uid': request_json['customer_uid'],
                'description': request_json['description'],
                'email': request_json['email'].strip(),
                'return_url': request_json['return_url'],
                'user_agent': user_agent,
                'overwrite_return_url': False,
                'select_customer_subscription': None,
            })


class TestOrderGetExternal(BaseTestOrderGet):
    @pytest.fixture
    def internal_api(self):
        return False

    @pytest.fixture
    def uid_path(self, order):
        return f'/ext/order/{order.uid}/{order.order_id}'


class TestOrderPutExternal(BaseTestOrderPut):
    @pytest.fixture
    def internal_api(self):
        return False

    @pytest.fixture
    def uid_path(self, order):
        return f'/ext/order/{order.uid}/{order.order_id}'


class TestOrderActiveExternal(BaseTestOrderActive):
    @pytest.fixture
    def internal_api(self):
        return False

    @pytest.fixture
    def uid_path(self, active, order):
        suffix = 'activate' if active else 'deactivate'
        return f'/ext/order/{order.uid}/{order.order_id}/{suffix}'


class TestOrderListPostExternal(BaseTestOrderListPost):
    @pytest.fixture
    def internal_api(self):
        return False

    @pytest.fixture
    def uid_path(self, order):
        return f'/ext/order/{order.uid}'

    class TestUrlMatch:
        pass


class TestOrderListGetExternal(BaseTestOrderList):
    @pytest.fixture
    def internal_api(self):
        return False

    @pytest.fixture
    def uid_path(self, order):
        return f'/ext/order/{order.uid}'

    @pytest.fixture
    async def response(self, payments_client, uid_path, order, test_data, tvm):
        return await payments_client.get(uid_path)

    def test_deny(self, response):
        assert response.status == 403
