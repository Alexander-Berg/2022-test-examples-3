import datetime

from django.test import TransactionTestCase

from cars.users.models.user import User
from ..core.order_debt_manager import DebtRetryPolicy, OrderDebtManager
from ..models.order import Order
from ..models.order_item_payment import OrderItemPayment
from .mixins import OrderTestCaseMixin


class OrderDebtManagerTestCase(OrderTestCaseMixin, TransactionTestCase):

    def setUp(self):
        super().setUp()
        self.manager = OrderDebtManager(
            order_payment_processor=self.order_payment_processor,
            debt_order_termination_threshold=0,
            debt_order_termination_delay=0,
        )

    def create_debt_order(self, complete=True):
        order = self.create_fix_order(item_cost=1, n_items=2, complete=complete)
        self.order_payment_processor.make_payments(order)

        for order_item_payment in OrderItemPayment.objects.filter(order_item__order=order):
            payment = order_item_payment.card_payment

            if payment.is_error():
                # The same payment may cover both items.
                # Then it shouldn't be unauthorized twice.
                continue

            payment = self.card_payment_processor.initialize_payment(payment)
            payment = self.card_payment_processor.process(payment)

            self.trust_client.unauthorize_payment(
                uid=payment.user.uid,
                purchase_token=payment.purchase_token,
                resp_code='test',
                resp_desc='test',
            )

            payment = self.card_payment_processor.process(payment)

        if complete:
            self.order_payment_processor.finalize_all_orders_payment_statuses()
            self.assert_order_payment_status_equal(order, Order.PaymentStatus.ERROR)

        return order

    def test_debt_for_ongoing_ok_order(self):
        order = self.create_fix_order(item_cost=1, n_items=2, complete=False)
        self.manager.mark_debtors()
        self.assert_user_status(user=order.user, status=User.Status.ACTIVE)

    def test_mark_debtors(self):
        order = self.create_debt_order()

        self.assert_user_status(user=order.user, status=User.Status.ACTIVE)
        self.manager.mark_debtors()
        self.assert_user_status(user=order.user, status=User.Status.DEBT)

    def test_unmark_debtors_for_ongoing_debt_order(self):
        order = self.create_debt_order(complete=False)

        self.manager.mark_debtors()
        self.assert_user_status(user=order.user, status=User.Status.DEBT)

        self.manager.unmark_debtors()
        self.assert_user_status(user=order.user, status=User.Status.DEBT)

    def test_repay_debt(self):
        order = self.create_debt_order()

        self.manager.mark_debtors()
        user = User.objects.get(id=order.user.id)
        self.assert_user_status(user=user, status=User.Status.DEBT)

        self.trust_client.set_post_start_action(self.trust_client.PostStartAction.AUTHORIZE)
        self.manager.repay_debt(user, timeout=5)

        self.assert_user_status(user=user, status=User.Status.ACTIVE)

    def test_repay_debt_error_generic(self):
        order = self.create_debt_order()

        self.manager.mark_debtors()
        user = User.objects.get(id=order.user.id)
        self.assert_user_status(user=user, status=User.Status.DEBT)

        self.trust_client.set_post_start_action(
            self.trust_client.PostStartAction.UNAUTHORIZE_NO_REASON
        )

        with self.assertRaises(self.manager.Error):
            self.manager.repay_debt(user, timeout=5)

        self.assert_user_status(user=user, status=User.Status.DEBT)

    def test_repay_debt_error_insufficient_funds(self):
        order = self.create_debt_order()

        self.manager.mark_debtors()
        user = User.objects.get(id=order.user.id)
        self.assert_user_status(user=user, status=User.Status.DEBT)

        self.trust_client.set_post_start_action(
            self.trust_client.PostStartAction.UNAUTHORIZE_INSUFFICIENT_FUNDS
        )

        with self.assertRaises(self.manager.InsufficientFundsError):
            self.manager.repay_debt(user, timeout=5)

        self.assert_user_status(user=user, status=User.Status.DEBT)

    def test_retry_all_debts_before_retry_interval(self):
        order = self.create_debt_order()

        self.trust_client.set_post_start_action(
            self.trust_client.PostStartAction.AUTHORIZE,
        )

        n_payments_before = OrderItemPayment.objects.filter(order_item__order=order).count()

        policy = DebtRetryPolicy(retry_interval=datetime.timedelta(days=1))
        self.manager.retry_all_debts(policy=policy)

        n_payments_after = OrderItemPayment.objects.filter(order_item__order=order).count()
        self.assertEqual(n_payments_before, n_payments_after)

    def test_retry_all_debts_after_retry_interval(self):
        order = self.create_debt_order()
        self.manager.mark_debtors()
        self.user.refresh_from_db()

        self.trust_client.set_post_start_action(
            self.trust_client.PostStartAction.AUTHORIZE,
        )

        n_payments_before = OrderItemPayment.objects.filter(order_item__order=order).count()

        policy = DebtRetryPolicy(retry_interval=datetime.timedelta(days=0))
        self.manager.retry_all_debts(policy=policy)

        n_payments_after = OrderItemPayment.objects.filter(order_item__order=order).count()
        self.assertLess(n_payments_before, n_payments_after)

        new_payment = (
            OrderItemPayment.objects
            .filter(order_item__order=order)
            .order_by('-created_at')
            .first()
        )
        self.card_payment_processor.initialize_payment(new_payment.card_payment)
        self.card_payment_processor.process(new_payment.card_payment)

        self.order_payment_processor.finalize_all_orders_payment_statuses()
        self.manager.unmark_debtors()

        self.assert_user_status(User.Status.ACTIVE)
