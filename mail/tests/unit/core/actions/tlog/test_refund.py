from datetime import timezone
from decimal import Decimal

import pytest

from mail.payments.payments.core.actions.base.action import BaseAction
from mail.payments.payments.core.actions.tlog.refund import ExportRefundToTLogAction
from mail.payments.payments.core.entities.enums import OrderKind, RefundStatus, ShopType
from mail.payments.payments.interactions.trust.base import BaseTrustClient
from mail.payments.payments.tests.utils import dummy_async_context_manager, dummy_coro
from mail.payments.payments.utils.helpers import temp_setattr


@pytest.fixture
def producer_mock(mocker):
    mock = mocker.Mock()
    mock.write_dict.return_value = dummy_coro()
    yield mock
    mock.write_dict.return_value.close()


@pytest.fixture(autouse=True)
def mock_lb_factory(lb_factory_mock):
    with temp_setattr(BaseAction.context, 'lb_factory', lb_factory_mock):
        yield


@pytest.fixture(autouse=True)
def producer_cls_mock(mocker, producer_mock):
    yield mocker.patch(
        'mail.payments.payments.core.actions.tlog.write.TLogLogbrokerProducer',
        mocker.Mock(return_value=dummy_async_context_manager(producer_mock)),
    )


class TestExportRefundToTLogAction:
    @pytest.fixture
    def refund_status(self):
        return RefundStatus.COMPLETED

    @pytest.fixture
    def refund_data(self, service_merchant, service_client, refund_status):
        return {
            'refund_status': refund_status,
            'service_merchant_id': service_merchant.service_merchant_id,
            'service_client_id': service_client.service_client_id
        }

    @pytest.fixture
    def returned_func(self, refund, order_with_service, items, transaction):
        async def _inner():
            return await ExportRefundToTLogAction(refund.uid, refund.order_id).run()

        return _inner

    @pytest.mark.parametrize('refund_status', [_ for _ in RefundStatus if _ != RefundStatus.COMPLETED])
    def test_no_write_by_pay_status(self, returned, producer_mock):
        producer_mock.write_dict.assert_not_called()

    @pytest.mark.parametrize('shop_type', [ShopType.TEST])
    def test_no_write_by_is_test(self, returned, producer_mock):
        producer_mock.write_dict.assert_not_called()

    @pytest.mark.asyncio
    async def test_written_data(self, returned, service, service_merchant, merchant, order, refund, items, data,
                                transaction, producer_mock, storage):
        order = await storage.order.get(order.uid, order.order_id, select_customer_subscription=None)
        order.items = items
        refund.items = items

        producer_mock.write_dict.assert_called_once_with(
            data={
                'original_order_transaction': {
                    'revision': transaction.revision,
                    'tx_id': transaction.tx_id,
                    'updated': transaction.updated.astimezone(timezone.utc).isoformat(),
                    'created': transaction.created.astimezone(timezone.utc).isoformat(),
                    'trust_terminal_id': transaction.trust_terminal_id,
                    'trust_payment_id': transaction.trust_payment_id,
                    'trust_resp_code': transaction.trust_resp_code,
                    'trust_purchase_token': transaction.trust_purchase_token,
                },
                'refund': {
                    'shop_id': refund.shop.shop_id,
                    'trust_refund_id': refund.trust_refund_id,
                    'closed': refund.closed.astimezone(timezone.utc).isoformat() if refund.closed else None,
                    'order_id': refund.order_id,
                    'original_order_id': refund.original_order_id,
                    'items': [{
                        'currency': item.currency,
                        'payment_method': "card",
                        'trust_order_id': BaseTrustClient.make_order_id(
                            refund.uid, refund.order_id, item.product_id, order.customer_uid, order.data.version
                        ),
                        'total_price': item.total_price.quantize(Decimal((0, (1,), -2))),
                        'nds': item.nds.value,
                        'product_id': item.product_id,
                        'amount': item.amount,
                        'name': item.name,
                        'markup': item.markup,
                    } for item in items],
                    'parent_order_id': refund.parent_order_id,
                    'created': refund.created.astimezone(timezone.utc).isoformat(),
                    'acquirer': refund.get_acquirer(merchant.acquirer).value,
                    'pay_status_updated_at': (
                        refund.pay_status_updated_at.astimezone(timezone.utc).isoformat()
                        if refund.pay_status_updated_at
                        else None
                    ),
                    'service_client_id': refund.service_client_id,
                    'description': refund.description,
                    'customer_uid': refund.customer_uid,
                    'price': refund.price.quantize(Decimal((0, (1,), -2))),
                    'service_merchant': {
                        'service': {'service_fee': service.options.service_fee},
                        'service_merchant_id': service_merchant.service_merchant_id,
                        'service_id': service.service_id,
                    },
                    'kind': OrderKind.REFUND.value,
                    'uid': refund.uid,
                    'autoclear': refund.autoclear,
                    'caption': refund.caption,
                    'currency': refund.currency,
                    'commission': refund.commission,
                    'revision': refund.revision,
                    'updated': refund.updated.astimezone(timezone.utc).isoformat(),
                    'held_at': refund.held_at.astimezone(timezone.utc).isoformat() if refund.held_at else None
                },
                'original_order': {
                    'shop_id': order.shop.shop_id,
                    'closed': order.closed.astimezone(timezone.utc).isoformat() if order.closed else None,
                    'order_id': order.order_id,
                    'original_order_id': order.original_order_id,
                    'items': [{
                        'currency': item.currency,
                        'payment_method': "card",
                        'trust_order_id': BaseTrustClient.make_order_id(
                            order.uid, order.order_id, item.product_id, order.customer_uid, order.data.version
                        ),
                        'total_price': item.total_price.quantize(Decimal((0, (1,), -2))),
                        'nds': item.nds.value,
                        'product_id': item.product_id,
                        'amount': item.amount,
                        'name': item.name,
                        'markup': item.markup,
                    } for item in items],
                    'parent_order_id': order.parent_order_id,
                    'created': order.created.astimezone(timezone.utc).isoformat(),
                    'acquirer': order.get_acquirer(merchant.acquirer).value,
                    'pay_status_updated_at': (
                        order.pay_status_updated_at.astimezone(timezone.utc).isoformat()
                        if order.pay_status_updated_at
                        else None
                    ),
                    'service_client_id': order.service_client_id,
                    'description': order.description,
                    'customer_uid': order.customer_uid,
                    'price': order.price.quantize(Decimal((0, (1,), -2))),
                    'service_merchant': {
                        'service': {'service_fee': service.options.service_fee},
                        'service_merchant_id': service_merchant.service_merchant_id,
                        'service_id': service.service_id,
                    },
                    'kind': OrderKind.PAY.value,
                    'uid': order.uid,
                    'autoclear': order.autoclear,
                    'caption': order.caption,
                    'currency': order.currency,
                    'commission': order.commission,
                    'revision': order.revision,
                    'updated': order.updated.astimezone(timezone.utc).isoformat(),
                    'held_at': order.held_at.astimezone(timezone.utc).isoformat() if order.held_at else None
                },
                'merchant': {
                    'uid': merchant.uid,
                    'parent_uid': merchant.parent_uid,
                    'person_id': merchant.person_id,
                    'client_id': merchant.client_id,
                    'revision': merchant.revision,
                    'submerchant_id': merchant.submerchant_id,
                    'contract_id': merchant.contract_id
                },
                'type': 'refund'
            }
        )
