import pytest

from sendr_utils import alist, json_value

from hamcrest import assert_that, contains_inanyorder, has_properties

from mail.payments.payments.core.actions.order.get import CoreGetOrderAction
from mail.payments.payments.core.actions.order.receipt_close import (
    CoreOrderReceiptCloseAction, OrderReceiptCloseServiceMerchantAction
)
from mail.payments.payments.core.entities.enums import PAYMETHOD_ID_OFFLINE, OperationKind, PayStatus, ReceiptType
from mail.payments.payments.core.entities.order import OrderData
from mail.payments.payments.core.exceptions import (
    ItemsInvalidDataError, OrderInvalidPayMethod, OrderMustBePaidError, OrderReceiptTypeMustPrepaid,
    TransactionNotFoundError, TrustError
)
from mail.payments.payments.interactions.trust.exceptions import TrustException


class TestCoreOrderReceiptCloseAction:
    @pytest.fixture
    def paymethod_id(self):
        return None

    @pytest.fixture
    def order_data(self, paymethod_id, order_data_data):
        return {
            'data': OrderData(receipt_type=ReceiptType.PREPAID),
            'pay_status': PayStatus.PAID,
            'paymethod_id': paymethod_id
        }

    @pytest.fixture
    def items_param(self, items):
        return [{'product_id': items[0].product_id, 'nds': items[0].nds}]

    @pytest.fixture
    def trust_exception(self):
        return None

    @pytest.fixture(autouse=True)
    def trust_payment_deliver_mock(self, shop_type, trust_client_mocker, trust_exception):
        result = {'status': 'success'}
        with trust_client_mocker(shop_type, 'payment_deliver', result=result, exc=trust_exception) as mock:
            yield mock

    @pytest.fixture(autouse=True)
    def get_order_action_mock(self, mock_action, order):
        return mock_action(CoreGetOrderAction, order)

    @pytest.fixture
    def returned_func(self, items_param, order):
        async def _inner():
            return await CoreOrderReceiptCloseAction(uid=order.uid, order_id=order.order_id, items=items_param).run()

        return _inner

    def test_trust_call(self, trust_payment_deliver_mock, items, transaction, order, acquirer, returned):
        trust_payment_deliver_mock.assert_called_once_with(
            uid=order.uid,
            acquirer=acquirer,
            purchase_token=transaction.trust_purchase_token,
            order=order,
            items=[items[0]]
        )

    @pytest.mark.parametrize('items_param', [None])
    def test_trust_call_none(self, trust_payment_deliver_mock, items, transaction, order, acquirer, returned):
        trust_payment_deliver_mock.assert_called_once_with(
            uid=order.uid,
            acquirer=acquirer,
            purchase_token=transaction.trust_purchase_token,
            order=order,
            items=items
        )

    @pytest.mark.asyncio
    async def test_changelog(self, items_param, storage, transaction, order, returned):
        assert_that(
            await alist(storage.change_log.find(order.uid)),
            contains_inanyorder(
                has_properties({
                    'uid': order.uid,
                    'revision': order.revision,
                    'operation': OperationKind.RECEIPT_CLOSE,
                    'arguments': {'order_id': order.order_id, 'items': json_value(items_param)}
                })
            )
        )

    @pytest.mark.parametrize('order_data', [{}])
    @pytest.mark.asyncio
    async def test_pay_status(self, trust_payment_deliver_mock, transaction, returned_func):
        with pytest.raises(OrderMustBePaidError):
            await returned_func()

    @pytest.mark.parametrize('order_data', [{'pay_status': PayStatus.PAID}])
    @pytest.mark.asyncio
    async def test_receipt_type(self, trust_payment_deliver_mock, transaction, returned_func):
        with pytest.raises(OrderReceiptTypeMustPrepaid):
            await returned_func()

    @pytest.mark.parametrize('paymethod_id', [PAYMETHOD_ID_OFFLINE])
    @pytest.mark.asyncio
    async def test_pay_method(self, trust_payment_deliver_mock, transaction, returned_func):
        with pytest.raises(OrderInvalidPayMethod):
            await returned_func()

    @pytest.mark.asyncio
    async def test_tx_absent(self, trust_payment_deliver_mock, items, order, acquirer, returned_func):
        with pytest.raises(TransactionNotFoundError):
            await returned_func()

    @pytest.mark.parametrize('items_param', [[{'product_id': -1}]])
    @pytest.mark.asyncio
    async def test_invalid_items(self, trust_payment_deliver_mock, transaction, items, order, acquirer, returned_func):
        with pytest.raises(ItemsInvalidDataError):
            await returned_func()

    @pytest.mark.parametrize('trust_exception', [TrustException(method='POST')])
    @pytest.mark.asyncio
    async def test_trust_error(self, trust_payment_deliver_mock, transaction, items, order, acquirer, returned_func):
        with pytest.raises(TrustError):
            await returned_func()


class TestOrderReceiptCloseServiceMerchantAction:
    @pytest.fixture
    def result(self, rands):
        return {rands(): rands()}

    @pytest.fixture
    def items_param(self, rands):
        return {rands(): rands()}

    @pytest.fixture(autouse=True)
    def core_action_mock(self, mock_action, result):
        return mock_action(CoreOrderReceiptCloseAction, result)

    @pytest.fixture
    def returned_func(self, service_merchant, service_client, items_param, order):
        async def _inner():
            return await OrderReceiptCloseServiceMerchantAction(
                service_merchant_id=service_merchant.service_merchant_id,
                service_tvm_id=service_client.tvm_id,
                order_id=order.order_id,
                items=items_param
            ).run()

        return _inner

    def test_result(self, result, returned):
        assert returned == result

    def test_call(self, order, items_param, returned, core_action_mock):
        core_action_mock.assert_called_once_with(uid=order.uid, order_id=order.order_id, items=items_param)
