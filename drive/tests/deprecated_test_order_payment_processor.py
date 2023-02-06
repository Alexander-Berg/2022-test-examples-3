import datetime
import decimal
from unittest.mock import MagicMock

from django.test import TransactionTestCase
from django.utils import timezone

from cars.billing.core.payment_processor import CardPaymentProcessor
from cars.billing.iface.payment import IPayment
from cars.billing.models.card_payment import CardPayment
from ..core.order_payment_processor import OrderPaymentProcessor
from ..models.order import Order
from ..models.order_item_payment import OrderItemPayment
from ..models.order_item_tariff import OrderItemTariff, PerMinuteOrderItemTariffParams
from .mixins import OrderTestCaseMixin


class BaseOrderPaymentProcessorTestCase(OrderTestCaseMixin, TransactionTestCase):
    pass


class OrderPaymentProcessorTestCase(BaseOrderPaymentProcessorTestCase):

    def test_fix(self):
        order = self.create_fix_order(item_cost=1)
        self.order_payment_processor.make_payments(order)
        self.assertTrue(OrderItemPayment.objects.filter(order_item__order=order).exists())

        item_payment = OrderItemPayment.objects.get(order_item__order=order)
        self.assertEqual(item_payment.amount, 1)

        payment = item_payment.card_payment
        self.assertIsNotNone(payment)
        self.assertEqual(payment.user, self.user)
        self.assertEqual(payment.amount, 1)
        self.assertEqual(payment.get_status(), CardPayment.Status.DRAFT)
        self.assertEqual(payment.paymethod_id, 'card-1234')

    def test_fix_greater_than_chunk_size(self):
        order = self.create_fix_order(item_cost=2.5)
        self.order_payment_processor.make_payments(order)

        payments = [
            x.card_payment for x in OrderItemPayment.objects.filter(order_item__order=order)
        ]
        self.assertEqual(len(payments), 1)
        self.assertEqual(payments[0].amount, 2.5)

    def test_fractional_chunk_size(self):
        processor = OrderPaymentProcessor(
            payment_processor=self.payment_processor,
            chunked_payment_amount=decimal.Decimal(1.25),
            push_client=MagicMock(),
        )

        tariff = OrderItemTariff(
            type=OrderItemTariff.Type.PER_MINUTE.value,
            per_minute_params=PerMinuteOrderItemTariffParams.objects.create(
                cost_per_minute=1,
            ),
        )
        order = self.create_single_item_order(
            tariff=tariff,
            started_at=timezone.now() - datetime.timedelta(minutes=10),
        )

        processor.make_payments(order)

        payments = [
            x.card_payment for x in OrderItemPayment.objects.filter(order_item__order=order)
        ]
        self.assertEqual(len(payments), 1)
        self.assertGreaterEqual(payments[0].amount, 10)

    def test_generate_payments_for_multiple_orders(self):
        order1 = self.create_fix_order(item_cost=1)
        order2 = self.create_fix_order(item_cost=2)
        self.order_payment_processor.make_payments_for_unpaid_orders()
        self.assert_all_payments_made(order1)
        self.assert_all_payments_made(order2)

    def test_payments_are_merged(self):
        req1 = self.create_reservation_order_item_request(tariff=self.create_fix_tariff(cost=0.5))
        req2 = self.create_reservation_order_item_request(tariff=self.create_fix_tariff(cost=0.5))
        order = self.create_multiple_items_order(item_requests=[req1, req2], complete=True)
        self.order_payment_processor.make_payments_for_unpaid_orders()

        payments = list(CardPayment.objects.all())
        self.assertEqual(len(payments), 1)

        payment = payments[0]
        self.assertEqual(payment.amount, 1)

        for item_payment in OrderItemPayment.objects.filter(order_item__order=order):
            self.assertEqual(item_payment.card_payment, payment)
            self.assertEqual(item_payment.amount, 0.5)

    def test_payment_equal_to_bonus_account(self):
        cost = decimal.Decimal(100)
        bonus = decimal.Decimal(100)

        self.bonus_account_manager.update_registration_taxi_cashback_earned(bonus)
        self.user.bonus_account.refresh_from_db()

        order = self.create_fix_order(item_cost=cost)
        self.order_payment_processor.make_payments(order)

        self.user.bonus_account.refresh_from_db()
        self.assertEqual(self.user.bonus_account.balance, 0)

        payments = list(OrderItemPayment.objects.filter(order_item__order=order))
        self.assertEqual(len(payments), 1)

        payment = payments[0]
        self.assertIs(payment.get_payment_method(), OrderItemPayment.PaymentMethod.BONUS)
        self.assertEqual(payment.amount, cost)

    def test_payment_less_than_bonus_account(self):
        cost = decimal.Decimal(100)
        bonus = decimal.Decimal(200)

        self.bonus_account_manager.update_registration_taxi_cashback_earned(bonus)
        self.user.bonus_account.refresh_from_db()

        order = self.create_fix_order(item_cost=cost)
        self.order_payment_processor.make_payments(order)

        self.user.bonus_account.refresh_from_db()
        self.assertEqual(self.user.bonus_account.balance, bonus - cost)

        payments = list(OrderItemPayment.objects.filter(order_item__order=order))
        self.assertEqual(len(payments), 1)

        payment = payments[0]
        self.assertIs(payment.get_payment_method(), OrderItemPayment.PaymentMethod.BONUS)
        self.assertEqual(payment.amount, cost)

    def test_payment_greater_than_bonus_account(self):
        cost = decimal.Decimal(200)
        bonus = decimal.Decimal(100)

        self.bonus_account_manager.update_registration_taxi_cashback_earned(bonus)
        self.user.bonus_account.refresh_from_db()

        order = self.create_fix_order(item_cost=cost)
        self.order_payment_processor.make_payments(order)

        self.user.bonus_account.refresh_from_db()
        self.assertEqual(self.user.bonus_account.balance, 0)

        self.assertEqual(OrderItemPayment.objects.filter(order_item__order=order).count(), 2)

        bonus_payment = OrderItemPayment.objects.get(
            order_item__order=order,
            payment_method=OrderItemPayment.PaymentMethod.BONUS.value,
        )
        self.assertEqual(bonus_payment.amount, bonus)

        card_payment = OrderItemPayment.objects.get(
            order_item__order=order,
            payment_method=OrderItemPayment.PaymentMethod.CARD.value,
        )
        self.assertEqual(card_payment.amount, cost - bonus)

    def test_multiple_payments_greater_than_bonus_account(self):
        bonus = 150
        cost1 = 130
        cost2 = 130

        self.bonus_account_manager.update_registration_taxi_cashback_earned(bonus)
        self.user.bonus_account.refresh_from_db()

        req1 = self.create_reservation_order_item_request(tariff=self.create_fix_tariff(cost=cost1))
        req2 = self.create_reservation_order_item_request(tariff=self.create_fix_tariff(cost=cost2))
        order = self.create_multiple_items_order(item_requests=[req1, req2], complete=True)
        self.order_payment_processor.make_payments_for_unpaid_orders()

        self.assertEqual(OrderItemPayment.objects.filter(order_item__order=order).count(), 3)

        bonus_payments = list(OrderItemPayment.objects.filter(
            order_item__order=order,
            payment_method=OrderItemPayment.PaymentMethod.BONUS.value,
        ))
        self.assertEqual(bonus_payments[0].bonus_payment, bonus_payments[1].bonus_payment)
        self.assertEqual(
            sum([bp.amount for bp in bonus_payments]),
            bonus,
        )

        card_payment = OrderItemPayment.objects.get(
            order_item__order=order,
            payment_method=OrderItemPayment.PaymentMethod.CARD.value,
        )
        self.assertEqual(card_payment.amount, cost1 + cost2 - bonus)

    def test_round_down(self):
        order = self.create_per_minute_order(minutes=1.333333, cost_per_minute=1)
        self.order_payment_processor.make_payments(order)

        payment = OrderItemPayment.objects.get(order_item__order=order).card_payment
        self.assertEqual(payment.amount, round(decimal.Decimal(1.333333), 2))

        self.order_payment_processor.make_payments(order)
        self.assertEqual(OrderItemPayment.objects.filter(order_item__order=order).count(), 1)

    def test_round_up(self):
        order = self.create_per_minute_order(minutes=1.666666, cost_per_minute=1)
        self.order_payment_processor.make_payments(order)

        payment = OrderItemPayment.objects.get(order_item__order=order).card_payment
        self.assertEqual(payment.amount, round(decimal.Decimal(1.666666), 2))

        self.order_payment_processor.make_payments(order)
        self.assertEqual(OrderItemPayment.objects.filter(order_item__order=order).count(), 1)


class OrderPaymentProcessorUpdatePaymentStatusTestCase(BaseOrderPaymentProcessorTestCase):

    def test_no_payments(self):
        order = self.create_fix_order(item_cost=1)
        self.order_payment_processor.finalize_all_orders_payment_statuses()
        self.assert_order_payment_status_equal(order, Order.PaymentStatus.NEW)

    def test_ok(self):
        order = self.create_fix_order(item_cost=1)
        self.order_payment_processor.make_payments(order)

        payment = OrderItemPayment.objects.get(order_item__order=order).card_payment
        self.card_payment_processor.initialize_payment(payment)
        self.card_payment_processor.start_payment(payment)
        self.trust_client.authorize_payment(
            uid=payment.user.uid,
            purchase_token=payment.purchase_token,
        )
        self.card_payment_processor.update_started_payment(payment)

        self.order_payment_processor.finalize_all_orders_payment_statuses()
        self.assert_order_payment_status_equal(order, Order.PaymentStatus.SUCCESS)

    def test_error(self):
        order = self.create_fix_order(item_cost=1)
        self.order_payment_processor.make_payments(order)

        payment = OrderItemPayment.objects.get(order_item__order=order).card_payment
        self.card_payment_processor.initialize_payment(payment)
        self.card_payment_processor.start_payment(payment)
        self.trust_client.unauthorize_payment(
            uid=payment.user.uid,
            purchase_token=payment.purchase_token,
            resp_code='test',
            resp_desc='test',
        )
        self.card_payment_processor.update_started_payment(payment)

        self.order_payment_processor.finalize_all_orders_payment_statuses()
        self.assert_order_payment_status_equal(order, Order.PaymentStatus.ERROR)


class RefundOrderManagerTestCase(BaseOrderPaymentProcessorTestCase):

    def setUp(self):
        super().setUp()
        self.order = self.create_single_item_order(
            tariff=self.create_fix_tariff(cost=100),
        )
        self.zero_clear_delay_card_payment_processor = CardPaymentProcessor(
            trust_client=self.trust_client,
            fiscal_title='[test] Yandex.Drive',
            fiscal_nds='nds_18',
            clear_delay=datetime.timedelta(seconds=0),
        )

    def add_fix_cost_item(self, cost, order=None):
        if order is None:
            order = self.order

        tariff = self.create_fix_tariff(cost=cost)
        request = self.create_reservation_paid_order_item_request(tariff=tariff)
        self.order_manager.add_order_item(order=order, order_item_request=request)

    def test_refund_new_order(self):
        with self.assertRaises(OrderPaymentProcessor.Error):
            self.order_payment_processor.refund_order(self.order)

    # def test_refund_bonus_single_item_single_payment(self):
    #     self.bonus_account_manager.debit_generic(
    #         amount=100,
    #         operator=self.user,
    #         comment='',
    #         nonce='',
    #     )

    #     self.assert_order_payment_status_equal(self.order, Order.PaymentStatus.NEW)

    #     self.complete_order(self.order)
    #     self.assert_order_payment_status_equal(self.order, Order.PaymentStatus.SUCCESS)

    #     self.order_payment_processor.refund_order(self.order)
    #     self.assert_order_payment_status_equal(self.order, Order.PaymentStatus.REFUNDED)

    #     payments = list(OrderItemPayment.objects.filter(order_item__order=self.order))
    #     self.assertEqual(len(payments), 1)

    #     payment = payments[0]
    #     self.assertIs(payment.get_payment_method(), OrderItemPayment.PaymentMethod.BONUS)

    def test_refund_card_authorized_single_item_single_payment(self):
        self.trust_client.set_post_start_action(self.trust_client.PostStartAction.AUTHORIZE)

        self.assert_order_payment_status_equal(self.order, Order.PaymentStatus.NEW)

        self.complete_order(self.order)
        self.payment_processor.process_all()
        self.payment_processor.process_all()
        self.order_payment_processor.finalize_order_payment_status(self.order)
        self.assert_order_payment_status_equal(self.order, Order.PaymentStatus.SUCCESS)

        self.order_payment_processor.refund_order(self.order)
        self.assert_order_payment_status_equal(self.order, Order.PaymentStatus.REFUNDED)

        payments = list(OrderItemPayment.objects.filter(order_item__order=self.order))
        self.assertEqual(len(payments), 1)

        payment = payments[0]
        self.assertIs(payment.get_payment_method(), OrderItemPayment.PaymentMethod.CARD)
        self.assertIs(payment.card_payment.get_generic_status(), IPayment.GenericStatus.REFUNDED)

    def test_refund_card_cleared_single_item_single_payment(self):
        self.trust_client.set_post_start_action(self.trust_client.PostStartAction.AUTHORIZE)

        self.assert_order_payment_status_equal(self.order, Order.PaymentStatus.NEW)

        self.complete_order(self.order)
        self.payment_processor.process_all()
        self.payment_processor.process_all()

        card_payment = OrderItemPayment.objects.get(order_item__order=self.order).get_impl()
        self.zero_clear_delay_card_payment_processor.try_complete_authorized_payment(card_payment)
        self.assertIs(card_payment.get_status(), CardPayment.Status.CLEARED)

        self.order_payment_processor.finalize_order_payment_status(self.order)
        self.assert_order_payment_status_equal(self.order, Order.PaymentStatus.SUCCESS)

        self.order_payment_processor.refund_order(self.order)
        self.assert_order_payment_status_equal(self.order, Order.PaymentStatus.REFUNDED)

        payments = list(OrderItemPayment.objects.filter(order_item__order=self.order))
        self.assertEqual(len(payments), 1)

        payment = payments[0]
        self.assertIs(payment.get_payment_method(), OrderItemPayment.PaymentMethod.CARD)
        self.assertIs(payment.card_payment.get_generic_status(), IPayment.GenericStatus.REFUNDED)
