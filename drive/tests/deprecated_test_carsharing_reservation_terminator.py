from unittest.mock import MagicMock

from rest_framework.test import APITransactionTestCase
from django.utils import timezone

from cars.calculator.tests.helper import CalculatorTestHelper
from cars.carsharing.factories.reservation import CarsharingReservationFactory
from cars.carsharing.models.car import Car
from cars.core.pusher import BasePusher
from cars.orders.core.order_request import OrderItemRequest, OrderRequest
from ..core.carsharing_reservation_terminator import CarsharingReservationTerminator
from ..core.order_item_requests.carsharing import CarsharingReservationOrderItemRequest
from ..factories.order_item import OrderItemFactory
from ..models.order_item import OrderItem
from .mixins import OrderTestCaseMixin


class CarsharingReservationTerminatorTestCase(OrderTestCaseMixin, APITransactionTestCase):

    def setUp(self):
        super().setUp()

        self.ch = CalculatorTestHelper(tc=self)
        self.ch.setUp()
        self.ch.set_carsharing_base_default_tariff(
            ride_cost_per_minute=100,
            parking_cost_per_minute=10,
        )

        self.xiva_mock = MagicMock()
        self.terminator = CarsharingReservationTerminator(
            order_manager=self.order_manager,
            pusher=BasePusher(
                xiva_client=self.xiva_mock,
                android_app_name='android.drive',
                ios_app_name='ios.drive',
            ),
        )

    def create_order(self, user=None):
        if user is None:
            user = self.user

        order_request = OrderRequest(
            user=user,
            payment_method=None,
            items=[
                OrderItemRequest(
                    user=user,
                    item_type=OrderItem.Type.CARSHARING_RESERVATION,
                    impl=CarsharingReservationOrderItemRequest(
                        user=self.user,
                        car_id=self.car.id,
                        max_duration_seconds=0,
                    ),
                    tariff=None,
                ),
            ],
        )

        order = self.order_manager.create_order(order_request).order

        return order

    def reset_parking_tariff(self, cost_per_minute):
        self.ch.set_carsharing_base_default_tariff(
            ride_cost_per_minute=100,
            parking_cost_per_minute=cost_per_minute,
        )

    def test_new_reservation(self):
        item = OrderItemFactory.create(
            started_at=timezone.now(),
            finished_at=None,
            type=OrderItem.Type.CARSHARING_RESERVATION.value,
            carsharing_reservation=CarsharingReservationFactory.create(),
        )
        self.terminator.process_ongoing_reservations()
        self.assertIsNone(OrderItem.objects.get(id=item.id).finished_at)

    def test_expired_reservation(self):
        order_request = OrderRequest(
            user=self.user,
            payment_method=None,
            items=[
                OrderItemRequest(
                    user=self.user,
                    item_type=OrderItem.Type.CARSHARING_RESERVATION,
                    impl=CarsharingReservationOrderItemRequest(
                        user=self.user,
                        car_id=self.car.id,
                        max_duration_seconds=0,
                    ),
                    tariff=None,
                ),
            ],
        )
        order = self.order_manager.create_order(order_request).order

        self.terminator.process_ongoing_reservations()

        reservation_item = (
            OrderItem.objects
            .get(order=order, type=OrderItem.Type.CARSHARING_RESERVATION.value)
        )
        self.assertIsNotNone(reservation_item.finished_at)

        reservation_paid_item = (
            OrderItem.objects
            .filter(
                order=order,
                type=OrderItem.Type.CARSHARING_RESERVATION_PAID.value,
            )
            .first()
        )
        self.assertIsNotNone(reservation_paid_item)
        self.assertIsNone(reservation_paid_item.finished_at)
        self.assertEqual(reservation_paid_item.tariff.per_minute_params.cost_per_minute, 10)

        self.assertEqual(
            Car.objects.get(id=self.car.id).status,
            Car.Status.RESERVATION_PAID.value,
        )

    def test_dynamic_tariff_fixed(self):
        self.reset_parking_tariff(cost_per_minute=1)

        order = self.create_order()

        self.reset_parking_tariff(cost_per_minute=2)

        self.terminator.process_ongoing_reservations()

        reservation_paid_item = (
            OrderItem.objects
            .filter(
                order=order,
                type=OrderItem.Type.CARSHARING_RESERVATION_PAID.value,
            )
            .first()
        )
        self.assertEqual(reservation_paid_item.tariff.per_minute_params.cost_per_minute, 1)

    def test_free_reservation_paid_after_fixed_cost(self):
        self.reset_parking_tariff(cost_per_minute=1)

        order = self.create_order()

        self.reset_parking_tariff(cost_per_minute=0)

        self.terminator.process_ongoing_reservations()

        reservation_paid_item = (
            OrderItem.objects
            .filter(
                order=order,
                type=OrderItem.Type.CARSHARING_RESERVATION_PAID.value,
            )
            .first()
        )
        self.assertEqual(reservation_paid_item.tariff.per_minute_params.cost_per_minute, 0)
