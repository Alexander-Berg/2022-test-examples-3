from datetime import datetime

import pytest

from hamcrest import assert_that, has_entries

from mail.payments.payments.core.entities.enums import AcquirerType, PayStatus, RefundStatus, TransactionStatus
from mail.payments.payments.core.entities.order import Order
from mail.payments.payments.core.entities.transaction import Transaction
from mail.payments.payments.storage.mappers.transaction import TransactionMapper

from .base import BaseTestNotAuthorized


def check_transaction_response(response_transactions, transaction, order):
    assert len(response_transactions) == 1
    transaction.set_trust_receipt_urls(order.acquirer)
    assert_that(response_transactions[0],
                has_entries({
                    'uid': transaction.uid,
                    'order_id': transaction.order_id,
                    'tx_id': transaction.tx_id,
                    'revision': transaction.revision,
                    'trust_purchase_token': transaction.trust_purchase_token,
                    'trust_payment_url': transaction.trust_payment_url,
                    'status': transaction.status.value,
                    'trust_failed_result': transaction.trust_failed_result,
                    'trust_resp_code': transaction.trust_resp_code,
                    'trust_payment_id': transaction.trust_payment_id,
                    'user_email': order.user_email,
                    'order_pay_status': order.pay_status.value,
                    'trust_receipt_download_url': transaction.trust_receipt_download_url,
                    'trust_receipt_view_url': transaction.trust_receipt_view_url,
                    'customer_uid': order.customer_uid,
                }))


@pytest.fixture(params=('assessor', 'admin'))
def acting_manager(request, managers):
    return managers[request.param]


@pytest.fixture
def request_params():
    return {}


@pytest.fixture
async def another_shop(storage, another_merchant, shop_entity):
    shop_entity.uid = another_merchant.uid
    return await storage.shop.create(shop_entity)


@pytest.fixture
async def another_order(storage, another_merchant, another_shop):
    return await storage.order.create(Order(
        uid=another_merchant.uid,
        shop_id=another_shop.shop_id,
    ))


@pytest.fixture
async def another_one_transaction(storage, another_order):
    return await storage.transaction.create(Transaction(
        uid=another_order.uid,
        order_id=another_order.order_id,
        status=TransactionStatus.FAILED,
        trust_purchase_token='xxx_purchase_token',
        trust_payment_url='some_url',
    ))


@pytest.fixture
async def order(storage, merchant, shop, service_merchant):
    return await storage.order.create(
        Order(
            uid=merchant.uid,
            shop_id=shop.shop_id,
            service_merchant_id=service_merchant.service_merchant_id,
            acquirer=merchant.acquirer)
    )


@pytest.fixture
async def response_data(response):
    return (await response.json())['data']


class TestGetTransactionList(BaseTestNotAuthorized):
    @pytest.fixture
    def tvm_uid(self, acting_manager):
        return acting_manager.uid

    @pytest.fixture
    def additional_params(self):
        return {}

    @pytest.fixture
    def request_params(self, merchant, additional_params):
        params = {'merchant_uid': merchant.uid}
        params.update(additional_params)
        return params

    @pytest.fixture
    async def response(self, response_func):
        return await response_func()

    @pytest.fixture
    def response_func(self, admin_client, request_params, tvm, transaction, another_one_transaction):
        async def _response_func(params_override={}):
            request_params.update(**params_override)
            return await admin_client.get('/admin/api/v1/transaction', params=request_params)

        return _response_func

    def test_get_by_uid__response(self, response_data, transaction, order):
        check_transaction_response(response_data, transaction, order)

    class TestGetByTxId:
        @pytest.fixture
        def additional_params(self, transaction):
            return {'tx_id': transaction.tx_id}

        def test_get_by_tx_id__response(self, response_data, transaction, order):
            check_transaction_response(response_data, transaction, order)

    class TestGetByEmail:
        @pytest.fixture
        async def additional_params(self, order, user_email, storage):
            order.user_email = user_email
            order = await storage.order.save(order)
            return {'email': order.user_email}

        def test_get_by_email__response(self, response_data, transaction, order):
            check_transaction_response(response_data, transaction, order)

    class TestGetByCreated:
        @pytest.fixture
        def additional_params(self, transaction):
            return {
                'lower_created_dt': datetime.fromtimestamp(transaction.created.timestamp() - 86400).isoformat(),
                'upper_created_dt': datetime.fromtimestamp(transaction.created.timestamp() + 86400).isoformat()
            }

        def test_get_by_created__response(self, response_data, transaction, order):
            check_transaction_response(response_data, transaction, order)

    class TestGetByUpdated:
        @pytest.fixture
        def additional_params(self, transaction):
            return {'lower_updated_dt': datetime.fromtimestamp(transaction.created.timestamp() - 86400).isoformat(),
                    'upper_updated_dt': datetime.fromtimestamp(transaction.created.timestamp() + 86400).isoformat()}

        def test_get_by_updated__response(self, response_data, transaction, order):
            check_transaction_response(response_data, transaction, order)

    class TestGetByUidAndOrderId:
        @pytest.fixture
        def additional_params(self, order):
            return {'order_id': order.order_id}

        def test_get_by_uid_and_order_id__response(self, response_data, transaction, order):
            check_transaction_response(response_data, transaction, order)

    class TestGetByStatus:
        @pytest.fixture
        def request_params(self, another_merchant):
            return {'merchant_uid': another_merchant.uid, 'statuses[]': TransactionStatus.FAILED.value}

        def test_get_by_status__response(self, response_data, another_one_transaction, another_order):
            check_transaction_response(response_data, another_one_transaction, another_order)

    class TestEmptyResponse:
        @pytest.fixture
        def request_params(self, unique_rand, randn):
            return {'merchant_uid': unique_rand(randn, basket='uid')}

        def test_empty_response(self, response_data):
            assert response_data == []

    class TestMapper:
        @pytest.fixture
        def order_pay_status(self):
            return PayStatus.NEW

        @pytest.fixture
        def sort_by(self):
            return 'tx_id'

        @pytest.fixture
        def desc(self, request):
            return False

        @pytest.fixture
        def customer_uid(self, randn):
            return randn()

        @pytest.fixture
        def services(self, randn):
            return [randn()]

        @pytest.fixture
        def request_params(self, sort_by, desc, order_pay_status, customer_uid, services):
            return {
                'sort_by': sort_by,
                'desc': str(desc).lower(),
                'order_pay_statuses[]': order_pay_status.value,
                'customer_uid': customer_uid,
                'services[]': services[0],
            }

        @pytest.fixture(autouse=True)
        def setup_spy(self, mocker):
            mocker.spy(TransactionMapper, 'find')

        @pytest.mark.parametrize('desc', (True, False))
        @pytest.mark.parametrize('sort_by', ('tx_id', 'updated', 'created', 'status', 'order_pay_status'))
        @pytest.mark.usefixtures('response')
        def test_sort_mapper_params(self, sort_by, desc):
            assert_that(
                TransactionMapper.find.call_args[1],
                has_entries({
                    'sort_by': sort_by,
                    'descending': desc,
                })
            )

        @pytest.mark.parametrize('order_pay_status', list(PayStatus))
        @pytest.mark.usefixtures('response')
        def test_filters(self, order_pay_status, customer_uid, services):
            assert_that(
                TransactionMapper.find.call_args[1],
                has_entries({
                    'order_pay_statuses': [order_pay_status],
                    'customer_uid': customer_uid,
                    'services': services
                })
            )

    @pytest.mark.asyncio
    async def test_invalid_sort_by(self, response_func):
        response = await response_func({'sort_by': 'invalid-field'})
        assert response.status == 400

    @pytest.mark.parametrize('order_pay_status', (RefundStatus.COMPLETED,))
    @pytest.mark.asyncio
    async def test_invalid_order_pay_status(self, response_func, order_pay_status):
        response = await response_func({'order_pay_statuses[]': order_pay_status.value})
        assert response.status == 400


class TestGetTransactionListV2(BaseTestNotAuthorized):
    @pytest.fixture
    def request_params(self, transaction):
        return {'uid': transaction.uid}

    @pytest.fixture
    def tvm_uid(self, acting_manager):
        return acting_manager.uid

    @pytest.fixture
    async def response(self, admin_client, request_params, tvm, transaction, another_one_transaction):
        return await admin_client.get('/admin/api/v2/transaction', params=request_params)

    def test_transactions(self, order, transaction, response_data):
        check_transaction_response(response_data['transactions'], transaction, order)

    def test_service_merchant(self, response_data, transaction, order, service_merchant, service):
        response_transactions = response_data['transactions']
        assert len(response_transactions) == 1
        assert_that(response_transactions[0],
                    has_entries({
                        'service_merchant': has_entries({
                            'service_merchant_id': order.service_merchant_id,
                            'service': has_entries({
                                'name': service.name,
                                'service_id': service.service_id
                            })
                        }),
                    }))

    class TestEmptyService:
        @pytest.fixture
        async def order(self, storage, merchant, shop):
            return await storage.order.create(
                Order(
                    uid=merchant.uid,
                    acquirer=merchant.acquirer,
                    shop_id=shop.shop_id
                )
            )

        def test_empty_service_merchant(self, response_data, transaction, order, service_merchant, service):
            response_transactions = response_data['transactions']
            assert len(response_transactions) == 1
            assert_that(response_transactions[0],
                        has_entries({
                            'service_merchant': None
                        })
                        )

    class TestReceiptUrls:
        @pytest.fixture(autouse=True)
        async def setup(self, storage, order):
            order.acquirer = AcquirerType.TINKOFF
            await storage.order.save(order)

        @pytest.mark.asyncio
        async def test_trust_receipt_urls(self, response_data, transaction, order):
            tx = response_data['transactions'][0]
            assert all((
                transaction.trust_purchase_token in tx['trust_receipt_view_url'],
                transaction.trust_purchase_token in tx['trust_receipt_download_url'],
            ))

    @pytest.mark.asyncio
    async def test_stats(self, storage, response_data, request_params):
        total = await storage.transaction.get_transactions_count()
        found = await storage.transaction.get_found_count(**request_params)
        assert all((
            response_data['found'] == found,
            response_data['total'] == total
        ))
