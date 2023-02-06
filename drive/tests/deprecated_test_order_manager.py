from django.test import TransactionTestCase

from ..core.order_item_requests.carsharing import CarsharingReservationOrderItemRequest
from ..core.order_request import OrderItemRequest, OrderPaymentMethodRequest, OrderRequest
from ..models.order_item import OrderItem
from ..models.order_item_payment import OrderItemPayment
from ..models.order_item_tariff import FixOrderItemTariffParams, OrderItemTariff
from ..models.order_payment_method import OrderPaymentMethod
from .mixins import OrderTestCaseMixin


class OrderManagerTestCase(OrderTestCaseMixin, TransactionTestCase):

    def create_order(self, tariff=None, payment_method_present=True):
        if tariff is None:
            tariff = OrderItemTariff(
                type=OrderItemTariff.Type.FIX.value,
                fix_params=FixOrderItemTariffParams.objects.create(
                    cost=1,
                ),
            )

        if payment_method_present:
            payment_method = OrderPaymentMethodRequest(
                type_=OrderPaymentMethod.Type.CARD,
                card_paymethod_id='card-1234',
            )
        else:
            payment_method = None

        order_request = OrderRequest(
            user=self.user,
            payment_method=payment_method,
            items=[
                OrderItemRequest(
                    user=self.user,
                    item_type=OrderItem.Type.CARSHARING_RESERVATION,
                    impl=CarsharingReservationOrderItemRequest(
                        user=self.user,
                        car_id=self.car.id,
                        max_duration_seconds=100,
                    ),
                    tariff=tariff,
                ),
            ],
        )
        order = self.order_manager.create_order(order_request).order
        return order

    def test_payment_method_set(self):
        order = self.create_order(payment_method_present=False)
        payment_method = order.payment_method
        self.assertIsNotNone(payment_method)
        self.assertEqual(payment_method.get_type(), OrderPaymentMethod.Type.CARD)
        self.assertEqual(payment_method.card_paymethod_id, 'card-1234')

    def test_payments_created_on_completion(self):
        order = self.create_order()
        self.order_manager.force_complete_order(order)

        payments = OrderItemPayment.objects.filter(order_item__order=order)
        self.assertEqual(payments.count(), 1)

        payment = payments.get()
        self.assertIs(payment.get_payment_method(), OrderItemPayment.PaymentMethod.CARD)
        self.assertEqual(payment.card_payment.paymethod_id, 'card-1234')
        self.assertEqual(payment.amount, 1)
