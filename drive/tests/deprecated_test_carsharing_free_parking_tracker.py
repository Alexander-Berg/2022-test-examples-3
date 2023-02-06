import datetime

from rest_framework.test import APITransactionTestCase
from django.utils import timezone

from cars.calculator.core.client import StubCalculatorClient
from cars.orders.core.order_request import OrderItemRequest, OrderRequest
from ..core.carsharing_free_parking_tracker import CarsharingFreeParkingTracker
from ..models.order_item import OrderItem
from .mixins import OrderTestCaseMixin


class CarsharingFreeParkingTrackerTestCase(OrderTestCaseMixin, APITransactionTestCase):

    def setUp(self):
        super().setUp()
        self.reset_parking_tariff(cost_per_minute=10)
        self.tracker = CarsharingFreeParkingTracker(order_manager=self.order_manager)

    def create_reservation_paid_order(self, user=None, car=None):
        order = self.create_base_order(user=user, car=car, request_acceptance=False)
        car = order.get_sorted_items()[-1].get_impl().car

        reservation_paid_request = OrderItemRequest.from_dict(
            user=order.user,
            data={
                'type': OrderItem.Type.CARSHARING_RESERVATION_PAID.value,
                'params': {
                    'car_id': car.id,
                },
            },
            context=None,
        )
        self.order_manager.add_order_item(
            order=order,
            order_item_request=reservation_paid_request,
        )

        return order

    def create_parking_order(self, user=None, car=None):
        order = self.create_base_order(user=user, car=car)
        car = order.get_sorted_items()[-1].get_impl().car

        parking_request = OrderItemRequest.from_dict(
            user=order.user,
            data={
                'type': OrderItem.Type.CARSHARING_PARKING.value,
                'params': {
                    'car_id': car.id,
                },
            },
            context=None,
        )
        self.order_manager.add_order_item(
            order=order,
            order_item_request=parking_request,
        )

        return order

    def create_base_order(self, user=None, car=None, request_acceptance=True):
        if user is None:
            user = self.user
        if car is None:
            car = self.car

        order_request = OrderRequest.from_dict(
            user=self.user,
            data={
                'items': [
                    {
                        'type': OrderItem.Type.CARSHARING_RESERVATION.value,
                        'params': {
                            'car_id': car.id,
                        },
                    },
                ],
            },
            context=None,
        )

        order = self.order_manager.create_order(order_request).order

        if request_acceptance:
            acceptance_request = OrderItemRequest.from_dict(
                user=user,
                data={
                    'type': OrderItem.Type.CARSHARING_ACCEPTANCE.value,
                    'params': {
                        'car_id': car.id,
                    },
                },
                context=None,
            )
            self.order_manager.add_order_item(
                order=order,
                order_item_request=acceptance_request,
            )

        return order

    def reset_parking_tariff(self, cost_per_minute, start_time=None):
        StubCalculatorClient.set_carsharing_base_default_tariff(
            start_time=start_time,
            ride_cost_per_minute=100,
            parking_cost_per_minute=cost_per_minute,
        )

    def test_tariff_not_changed(self):
        order = self.create_parking_order()
        self.tracker.track_all()

        item = order.get_sorted_items()[-1]
        self.assertIsNone(item.finished_at)
        self.assertEqual(item.tariff.per_minute_params.cost_per_minute, 10)

    def test_reservation_paid_tariff_changed_to_free(self):
        order = self.create_reservation_paid_order()
        self.reset_parking_tariff(cost_per_minute=0)
        self.tracker.track_all()

        self.assertIs(
            order.get_sorted_items()[-1].get_type(),
            OrderItem.Type.CARSHARING_RESERVATION_PAID,
        )
        self.assertIs(
            order.get_sorted_items()[-2].get_type(),
            OrderItem.Type.CARSHARING_RESERVATION_PAID,
        )
        self.assertIsNotNone(order.get_sorted_items()[-2].finished_at)

        item = order.get_sorted_items()[-1]
        self.assertIsNone(item.finished_at)
        self.assertEqual(item.tariff.per_minute_params.cost_per_minute, 0)

    def test_reservation_paid_tariff_changed_to_paid(self):
        order = self.create_reservation_paid_order()
        self.reset_parking_tariff(cost_per_minute=0)
        self.tracker.track_all()

        self.assertIs(
            order.get_sorted_items()[-1].get_type(),
            OrderItem.Type.CARSHARING_RESERVATION_PAID,
        )
        self.assertIs(
            order.get_sorted_items()[-2].get_type(),
            OrderItem.Type.CARSHARING_RESERVATION_PAID,
        )
        self.assertIsNotNone(order.get_sorted_items()[-2].finished_at)

        item = order.get_sorted_items()[-1]
        self.assertIsNone(item.finished_at)
        self.assertEqual(item.tariff.per_minute_params.cost_per_minute, 0)

        self.reset_parking_tariff(cost_per_minute=1)
        self.tracker.track_all()

        item = order.get_sorted_items()[-1]
        self.assertIs(item.get_type(), OrderItem.Type.CARSHARING_RESERVATION_PAID)
        expected_tariff = order.get_tariff_snapshot().carsharing_reservation_paid
        expected_cost_per_minute = expected_tariff.per_minute_params.cost_per_minute
        self.assertEqual(item.tariff.per_minute_params.cost_per_minute, expected_cost_per_minute)

    def test_reservation_paid_tariff_changed_to_paid_free_snapshot(self):
        self.reset_parking_tariff(cost_per_minute=0)
        order = self.create_reservation_paid_order()

        self.reset_parking_tariff(cost_per_minute=1)
        self.tracker.track_all()

        item = order.get_sorted_items()[-1]
        self.assertIs(item.get_type(), OrderItem.Type.CARSHARING_RESERVATION_PAID)
        self.assertEqual(item.tariff.per_minute_params.cost_per_minute, 1)

    def test_reservation_paid_tariff_changed_to_paid_no_snapshot(self):
        order = self.create_reservation_paid_order()
        self.reset_parking_tariff(cost_per_minute=0)
        self.tracker.track_all()

        order.tariff_snapshot.delete()

        self.reset_parking_tariff(cost_per_minute=1)
        self.tracker.track_all()

        item = order.get_sorted_items()[-1]
        self.assertIs(item.get_type(), OrderItem.Type.CARSHARING_RESERVATION_PAID)
        self.assertEqual(item.tariff.per_minute_params.cost_per_minute, 1)

    def test_parking_tariff_changed_to_free(self):
        order = self.create_parking_order()
        self.reset_parking_tariff(cost_per_minute=0)
        self.tracker.track_all()

        self.assertIs(order.get_sorted_items()[-1].get_type(), OrderItem.Type.CARSHARING_PARKING)
        self.assertIs(order.get_sorted_items()[-2].get_type(), OrderItem.Type.CARSHARING_PARKING)
        self.assertIsNotNone(order.get_sorted_items()[-2].finished_at)

        item = order.get_sorted_items()[-1]
        self.assertIsNone(item.finished_at)
        self.assertEqual(item.tariff.per_minute_params.cost_per_minute, 0)

    def test_parking_tariff_changed_to_paid(self):
        order = self.create_parking_order()
        self.reset_parking_tariff(cost_per_minute=0)
        self.tracker.track_all()

        item = order.get_sorted_items()[-1]
        self.assertEqual(item.tariff.per_minute_params.cost_per_minute, 0)

        self.reset_parking_tariff(cost_per_minute=1)
        self.tracker.track_all()

        item = order.get_sorted_items()[-1]
        self.assertIs(item.get_type(), OrderItem.Type.CARSHARING_PARKING)
        self.assertIsNone(item.finished_at)
        expected_tariff = order.get_tariff_snapshot().carsharing_parking
        expected_cost_per_minute = expected_tariff.per_minute_params.cost_per_minute
        self.assertEqual(item.tariff.per_minute_params.cost_per_minute, expected_cost_per_minute)

    def test_started_at_before_free_period(self):
        order = self.create_parking_order()
        tariff_active_from = timezone.now().time()
        self.reset_parking_tariff(
            cost_per_minute=0,
            start_time=tariff_active_from,
        )
        self.tracker.track_all()

        item = order.get_sorted_items()[-1]
        prev_item = order.get_sorted_items()[-2]

        self.assertEqual(item.started_at.time(), tariff_active_from)
        self.assertIsNotNone(prev_item.finished_at)
        self.assertEqual(prev_item.finished_at.time(), tariff_active_from)

    def test_started_at_in_future(self):
        order = self.create_parking_order()
        tariff_active_from = datetime.time.max
        self.reset_parking_tariff(
            cost_per_minute=0,
            start_time=tariff_active_from,
        )
        self.tracker.track_all()

        item = order.get_sorted_items()[-1]
        prev_item = order.get_sorted_items()[-2]

        self.assertLess(item.started_at.time(), tariff_active_from)
        self.assertIsNotNone(prev_item.finished_at)
        self.assertLess(prev_item.finished_at.time(), tariff_active_from)
