from decimal import Decimal

import pytest

from hamcrest import assert_that, has_properties

from mail.payments.payments.core.entities.enums import (
    PAYMETHOD_ID_OFFLINE, OrderKind, PaymentsTestCase, PayStatus, ShopType
)
from mail.payments.payments.core.entities.item import Item
from mail.payments.payments.tests.base import parametrize_shop_type
from mail.payments.payments.utils.datetime import utcnow


class TestAddCrypto:
    @pytest.fixture
    def crypto_prefix(self):
        return 'prefix/'

    @pytest.fixture
    def order_hash(self):
        return 'test-add-crypto-order-hash'

    @pytest.fixture
    def payment_hash(self):
        return 'test-add-crypto-payment-hash'

    @pytest.fixture
    def crypto_mock(self, mocker, order_hash, payment_hash):
        mock = mocker.Mock()
        mock.encrypt_order.return_value = order_hash
        mock.encrypt_payment.return_value = payment_hash
        return mock

    def test_crypto_call_order(self, order, crypto_prefix, crypto_mock):
        order.add_crypto(crypto_prefix, crypto_mock)
        crypto_mock.encrypt_order.assert_called_once_with(uid=order.uid, order_id=order.order_id)

    def test_crypto_call_payment(self, order, crypto_prefix, crypto_mock):
        order.add_crypto(crypto_prefix, crypto_mock)
        crypto_mock.encrypt_payment.assert_called_once_with(uid=order.uid, order_id=order.order_id)

    def test_order_hashes(self, order, crypto_prefix, order_hash, payment_hash, crypto_mock):
        order.add_crypto(crypto_prefix, crypto_mock)
        assert_that(
            order,
            has_properties({
                'order_hash': order_hash,
                'order_url': crypto_prefix + order_hash,
                'payment_hash': payment_hash,
                'payment_url': crypto_prefix + payment_hash,
            })
        )


class TestIsSubscription:
    @pytest.fixture(params=(True, False))
    def is_subscription(self, request):
        return request.param

    @pytest.fixture
    def customer_subscription_id(self, customer_subscription, is_subscription):
        return customer_subscription.customer_subscription_id if is_subscription else None

    def test_is_subscription(self, order, is_subscription, customer_subscription_id):
        order.customer_subscription_id = customer_subscription_id
        assert order.is_subscription == is_subscription


class TestSingleItem:
    @pytest.mark.parametrize('items_amount', (0, 2))
    def test_single_item_fail(self, order, items):
        order.items = items
        with pytest.raises(AssertionError):
            order.single_item

    @pytest.mark.parametrize('items_amount', (1,))
    def test_single_item(self, order, items):
        order.items = items
        assert order.single_item == items[0]


class TestInModeration:
    @pytest.mark.parametrize('pay_status', list(PayStatus))
    def test_in_moderation(self, order, pay_status):
        order.pay_status = pay_status
        assert order.in_moderation == (pay_status == PayStatus.IN_MODERATION)


class TestMultiAmountExceed:
    def test_non_multi(self, order):
        with pytest.raises(AssertionError):
            order.multi_amount_exceed

    @pytest.mark.parametrize('field', ('multi_max_amount', 'multi_issued'))
    def test_none(self, order, field):
        order.kind = OrderKind.MULTI
        setattr(order.data, field, None)
        assert order.multi_amount_exceed is False

    def test_exceed(self, order):
        order.kind = OrderKind.MULTI
        order.data.multi_max_amount = 1
        order.data.multi_issued = 1
        assert order.multi_amount_exceed is True

    def test_not_exceed(self, order):
        order.kind = OrderKind.MULTI
        order.data.multi_max_amount = 2
        order.data.multi_issued = 1
        assert order.multi_amount_exceed is False


class TestIsAlreadyPaid:
    @pytest.mark.parametrize('pay_status', list(PayStatus))
    def test_is_already_paid(self, order, pay_status):
        order.pay_status = pay_status
        assert order.is_already_paid == (pay_status in (PayStatus.PAID, PayStatus.IN_PROGRESS, PayStatus.IN_MODERATION))


class TestPaymentMethod:
    @pytest.mark.parametrize('pay_status', [PayStatus.PAID, PayStatus.NEW])
    def test_null(self, pay_status, order):
        order.pay_status = pay_status
        assert order.pay_method is not None if order.is_already_paid else order.pay_method is None

    def test_offline(self, order):
        order.pay_status = PayStatus.PAID
        order.paymethod_id = PAYMETHOD_ID_OFFLINE
        assert order.pay_method == 'offline'

    def test_yandex(self, rands, order):
        order.pay_status = PayStatus.PAID
        order.paymethod_id = rands()
        assert order.pay_method == 'yandex'


class TestIsTest:
    @parametrize_shop_type
    @pytest.mark.parametrize('test', [None, PaymentsTestCase.TEST_OK_CLEAR])
    def test_is_test(self, order, shop, test):
        order.shop = shop
        order.test = test
        assert order.is_test == (order.shop.shop_type == ShopType.TEST or order.test is not None)


class TestIsCreateArbitrageAvailable:
    @pytest.mark.parametrize('dialogs_org_id', (None, '123'))
    def test_is_create_arbitrage_available(self, order, merchant, dialogs_org_id):
        merchant.dialogs_org_id = dialogs_org_id
        order.merchant = merchant

        assert order.is_create_arbitrage_available == (dialogs_org_id is not None)


class TestPrice:
    def test_none_items(self, order):
        assert order.price is None and order.log_price == 0.0

    def test_empty_items(self, order):
        order.items = []
        assert order.price == Decimal(0) and order.log_price == 0.0

    def test_price_calc(self, order):
        order.items = [
            Item(uid=order.uid, amount=1, new_price=Decimal(1.0)),
            Item(uid=order.uid, amount=2, new_price=Decimal(2.0))
        ]
        assert order.price == Decimal(5.0) and order.log_price == 5.0


class TestChangePayStatus:
    def test_default_pay_updated_at_is_empty(self, order):
        assert order.pay_status_updated_at is None

    def test_change_pay_updated_at(self, order):
        start = utcnow()
        order.pay_status = PayStatus.IN_PROGRESS
        assert start <= order.pay_status_updated_at <= utcnow()

    def test_not_change_pay_updated_at(self, order):
        order.pay_status = PayStatus.PAID
        start = order.pay_status_updated_at

        order.pay_status = PayStatus.PAID
        assert start == order.pay_status_updated_at
