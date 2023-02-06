from datetime import timezone
from decimal import Decimal

import pytest

from mail.payments.payments.core.actions.base.action import BaseAction
from mail.payments.payments.core.actions.tlog.customer_subscription import ExportCustomerSubscriptionToTLogAction
from mail.payments.payments.core.entities.enums import OrderKind, PayStatus, ShopType, TransactionStatus
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


class TestExportCustomerSubscriptionToTLogAction:
    @pytest.fixture
    def transaction_status(self):
        return TransactionStatus.CLEARED

    @pytest.fixture
    def pay_status(self):
        return PayStatus.PAID

    @pytest.fixture
    def order_data(self, pay_status, order_data):
        return {
            'pay_status': pay_status,
            **order_data
        }

    @pytest.fixture
    def customer_subscription_transaction_data(self, transaction_status, rands, randdecimal):
        return {
            'payment_status': transaction_status,
            'data': {
                **{rands(): rands() for _ in range(10)},
                'amount': f'{randdecimal()}',
            },
        }

    @pytest.fixture
    def returned_func(self, customer_subscription, items, order_with_service_and_customer_subscription,
                      customer_subscription_transaction):
        async def _inner():
            return await ExportCustomerSubscriptionToTLogAction(
                customer_subscription.uid,
                customer_subscription.customer_subscription_id,
                customer_subscription_transaction.purchase_token
            ).run()

        return _inner

    @pytest.mark.parametrize('pay_status', [_ for _ in PayStatus if _ != PayStatus.PAID])
    def test_no_write_by_pay_status(self, returned, producer_mock):
        producer_mock.write_dict.assert_not_called()

    @pytest.mark.parametrize('shop_type', [ShopType.TEST])
    def test_no_write_by_is_test(self, returned, producer_mock):
        producer_mock.write_dict.assert_not_called()

    @pytest.mark.parametrize('transaction_status', [_ for _ in TransactionStatus if _ != TransactionStatus.CLEARED])
    def test_no_write_by_transaction_status(self, returned, producer_mock):
        producer_mock.write_dict.assert_not_called()

    @pytest.mark.asyncio
    async def test_written_data(self, returned, service, service_merchant, merchant, order, items, data,
                                customer_subscription_transaction, producer_mock, storage):
        order = await storage.order.get(order.uid, order.order_id, select_customer_subscription=None)
        order.items = items

        producer_mock.write_dict.assert_called_once_with(
            data={
                'customer_subscription_transaction': {
                    'updated': customer_subscription_transaction.updated.astimezone(timezone.utc).isoformat(),
                    'trust_purchase_token': customer_subscription_transaction.purchase_token,
                    'created': customer_subscription_transaction.created.astimezone(timezone.utc).isoformat()
                },
                'order': {
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
                'type': 'subscription'
            }
        )
