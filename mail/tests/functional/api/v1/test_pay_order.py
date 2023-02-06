import pytest

from hamcrest import assert_that, has_properties, is_not

from mail.payments.payments.core.entities.enums import (
    AcquirerType, OrderKind, PayStatus, RefundStatus, TransactionStatus
)
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.transaction import Transaction


@pytest.mark.usefixtures('moderation', 'balance_person_mock')
class BasePayOrderTest:
    @pytest.fixture(params=list(AcquirerType))
    def acquirer(self, request):
        return request.param

    @pytest.fixture
    def payment_status(self):
        return 'started'

    @pytest.fixture(autouse=True)
    def payment_start(self, shop_type, trust_client_mocker, payment_url):
        result = {
            'purchase_token': 'XX-TOKEN-XX',
            'payment_url': payment_url
        }
        with trust_client_mocker(shop_type, 'payment_start', result) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def orders_create(self, shop_type, trust_client_mocker):
        with trust_client_mocker(shop_type, 'orders_create', []) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def payment_create(self, shop_type, trust_client_mocker, payment_create_result):
        with trust_client_mocker(shop_type, 'payment_create', payment_create_result) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def get_payment_status_mock(self, shop_type, trust_client_mocker, payment_status):
        with trust_client_mocker(shop_type, 'payment_status', payment_status) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def update_transaction_calls(self, mocker):
        calls = []

        def dummy_init(self, transaction, *args, **kwargs):
            self.tx = transaction

        async def dummy_run(self):
            nonlocal calls
            calls.append(self.tx)
            return self.tx

        mocker.patch(
            'mail.payments.payments.core.actions.update_transaction.UpdateTransactionAction.__init__', dummy_init
        )
        mocker.patch(
            'mail.payments.payments.core.actions.update_transaction.UpdateTransactionAction.run', dummy_run
        )

        return calls

    @pytest.fixture
    def yandexuid(self, request):
        return 'test-yandexuid'

    @pytest.fixture
    def trust_form_name(self, order):
        return f'{order.data.trust_form_name}something'

    @pytest.fixture
    def template(self):
        return 'mobile'

    @pytest.fixture
    def req_json(self, rands, user_email, randn, user_description, yandexuid, template, trust_form_name):
        return {
            'yandexuid': yandexuid,
            'email': user_email,
            'return_url': 'ya.ru',
            'description': user_description,
            'template': template,
            'trust_form_name': trust_form_name,
            'customer_uid': randn(),
            'service_data': {rands(): rands()},
        }


class TestPostOrderExternalFailsOnBadOrder(BasePayOrderTest):
    @pytest.fixture(params=(
        pytest.param('hash', id='external'),
        pytest.param('order_id', id='internal'),
    ))
    def url(self, request, crypto_mock, service_merchant):
        def _inner(order):
            payment_hash = crypto_mock.encrypt_payment(uid=order.uid, order_id=order.order_id)
            data = {
                'hash': f'/ext/{payment_hash}',
                'order_id': f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/start'
            }

            return data[request.param]

        return _inner

    @pytest.fixture
    def assert_fails(self, url, client, req_json, service_merchant, tvm):
        async def _inner(order):
            r = await client.post(url(order), json=req_json)
            assert (await r.json())['code'] == 400

        return _inner

    @pytest.mark.asyncio
    async def test_fails_on_refund(self, storage, order, service_merchant, assert_fails, shop):
        refund = await storage.order.create(Order(
            uid=order.uid,
            original_order_id=order.order_id,
            kind=OrderKind.REFUND,
            pay_status=None,
            refund_status=RefundStatus.REQUESTED,
            service_merchant_id=service_merchant.service_merchant_id,
            shop_id=shop.shop_id,
        ))
        await assert_fails(refund)

    @pytest.mark.asyncio
    async def test_fails_on_paid(self, storage, order, assert_fails):
        order.pay_status = PayStatus.PAID
        order = await storage.order.save(order)
        await assert_fails(order)

    @pytest.mark.asyncio
    async def test_fails_on_inactive(self, storage, order, assert_fails):
        order.active = False
        order = await storage.order.save(order)
        await assert_fails(order)


class BaseGoodOrderTest(BasePayOrderTest):
    @pytest.fixture
    def transaction_status(self):
        return TransactionStatus.CLEARED

    @pytest.fixture
    async def transaction(self, storage, order, transaction_status):
        if transaction_status is None:
            return None
        return await storage.transaction.create(Transaction(
            uid=order.uid,
            order_id=order.order_id,
            status=transaction_status,
            trust_purchase_token='xxx_purchase_token',
            trust_payment_url='yyy_payment_url',
        ))

    @pytest.fixture(params=(
        pytest.param(True, id='internal'),
        pytest.param(False, id='external'),
    ))
    def internal_api(self, request):
        return request.param

    @pytest.fixture
    def path(self, internal_api, payment_hash, service_merchant, order):
        if internal_api:
            return f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/start'
        else:
            return f'/ext/{payment_hash}'

    @pytest.fixture
    async def response(self, client, transaction, req_json, path, tvm):
        r = await client.post(
            path,
            json=req_json,
        )
        return await r.json()


class TestNewTransaction(BaseGoodOrderTest):
    @pytest.fixture(params=[
        None,
        TransactionStatus.FAILED,
        TransactionStatus.CANCELLED,
    ])
    def transaction_status(self, request):
        return request.param

    @pytest.fixture
    async def new_transaction(self, storage, order, response):
        return await storage.transaction.get_last_by_order(order.uid, order.order_id)

    @pytest.mark.asyncio
    async def test_creates_transaction(self, response, payment_start):
        payment_start.assert_called_once()

    def test_new_transaction__response_payment_url(self, response, payment_url, path, payment_status):
        if 'ext' in path:
            assert response['data']['payment_url'] == payment_url
        else:
            assert response['data']['trust_url'] == payment_url

    def test_transaction_payment_url(self, payment_url, new_transaction):
        assert new_transaction.trust_payment_url == payment_url

    def test_creates_new_transaction(self, transaction, new_transaction):
        assert transaction is None or transaction.tx_id != new_transaction.tx_id

    @pytest.mark.asyncio
    async def test_updates_order(self, storage, order, response, user_email, user_description, template,
                                 trust_form_name, internal_api, req_json):
        order = await storage.order.get(order.uid, order.order_id, select_customer_subscription=None)

        order_data = order.data
        order_data.trust_template = template
        order_data.trust_form_name = trust_form_name
        if internal_api:
            order_data.service_data = req_json['service_data']

        assert_that(
            order,
            has_properties({
                'user_email': user_email,
                'user_description': user_description,
                'data': order_data
            })
        )


class TestExistingTransaction(BaseGoodOrderTest):
    @pytest.fixture
    def transaction_status(self):
        return TransactionStatus.ACTIVE

    def test_existing_transation__response_payment_url(self, response, transaction, path, payment_status):
        if 'ext' in path:
            assert response['data']['payment_url'] == transaction.trust_payment_url
        else:
            assert response['data']['trust_url'] == transaction.trust_payment_url

    @pytest.mark.asyncio
    async def test_does_not_update_order(self, storage, order, response, user_email, user_description):
        order = await storage.order.get(order.uid, order.order_id, select_customer_subscription=None)
        assert_that(
            order,
            has_properties({
                'user_email': is_not(user_email),
                'user_description': is_not(user_description),
            })
        )


class TestUpdatesTransaction(BaseGoodOrderTest):
    @pytest.fixture(params=[
        TransactionStatus.ACTIVE,
        TransactionStatus.HELD,
    ])
    def transaction_status(self, request):
        return request.param

    def test_update_transaction_called(self, update_transaction_calls, response):
        assert len(update_transaction_calls) == 1


class TestEmptyReqParams(BaseGoodOrderTest):
    @pytest.fixture
    def transaction_status(self, request):
        return None

    @pytest.fixture
    def req_json(self):
        return {}

    @pytest.fixture
    def path(self, service_merchant, order):
        return f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/start'

    @pytest.fixture
    async def response(self, client, path, transaction, req_json, tvm, order, storage):
        order.user_email = 'email'
        order.return_url = 'ddd'
        order.paymethod_id = 'yandex_money_web_page'
        await storage.order.save(order)
        r = await client.post(path, json=req_json)
        return await r.json()

    def test_response_status(self, response, payment_status, payment_url):
        assert response['data']['trust_url'] == payment_url


class TestMissingTrustParams(BaseGoodOrderTest):
    @pytest.fixture
    def req_json(self):
        return {
            'yandexuid': 'xx_yandexuid',
            'email': 'email',
            'return_url': 'return_url',
        }

    @pytest.fixture
    def path(self, order, service_merchant):
        return f'/v1/internal/order/{service_merchant.service_merchant_id}/{order.order_id}/start'

    @pytest.mark.parametrize('param', ['email', 'return_url'])
    @pytest.mark.asyncio
    async def test_fail_on_skip_params(self, param, client, path, req_json, order):
        req_json.pop(param)
        r = await client.post(path, json=req_json)
        assert (await r.json())['code'] == 400
