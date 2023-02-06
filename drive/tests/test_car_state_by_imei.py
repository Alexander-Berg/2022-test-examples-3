import logging
import time
import unittest.mock

from django.urls import reverse

from cars.calculator.tests.helper import CalculatorTestHelper
from cars.carsharing.factories.car import CarFactory
from cars.orders.core.order_manager import OrderManager
from cars.orders.core.order_request import OrderItemRequestContext, OrderRequest
from cars.orders.models import OrderItem
from .base import AdminAPITestCase


LOGGER = logging.getLogger(__name__)


class CarStateByIMEIBaseTestCase(AdminAPITestCase):

    def setUp(self):
        super().setUp()

        self.car = CarFactory.create()

        self.push_client_mock = unittest.mock.MagicMock()
        self.order_manager = OrderManager.from_settings(push_client=self.push_client_mock)

        self.ch = CalculatorTestHelper(tc=self)
        self.ch.setUp()

    def _perform_ride_endpoint_request(self, imei):
        url = reverse('cars-admin:car-ride-by-imei')
        response = self.client.get(url, {'imei': imei})
        return response

    def _perform_all_info_endpoint_request(self, imei, timestamp):
        url = reverse('cars-admin:car-info-by-imei-on-time')
        response = self.client.get(url, {'imei': imei, 'timestamp': timestamp})
        return response

    def _perform_status_endpoint_request(self, imei):
        url = reverse('cars-admin:car-status-by-imei')
        response = self.client.get(url, {'imei': imei})
        return response

    def _get_order_item_list_url(self, order_id):
        return reverse(
            'drive:order-item-list',
            kwargs={
                'order_id': order_id,
            },
        )

    def _get_dispatching_event_url(self, order_id, order_item_id):
        return reverse(
            'drive:order-item-details',
            kwargs={
                'order_id': order_id,
                'order_item_id': order_item_id,
            },
        )

    def _create_active_ride_item(self):
        order_request = OrderRequest.from_dict(
            user=self.user,
            data={
                'items': [
                    {
                        'type': 'carsharing_reservation',
                        'params': {
                            'car_id': str(self.car.id),
                        },
                    },
                ],
            },
            context=OrderItemRequestContext(
                oauth_token=None,
            ),
        )
        order = self.order_manager.create_service_app_order(order_request).order
        order.save()
        url = self._get_order_item_list_url(str(order.id))
        data = {
            'type': 'carsharing_acceptance',
            'params': {
                'car_id': str(self.car.id),
            },
        }

        response = self.client.post(url, data=data)
        self.assertEqual(response.status_code, 200)
        order_item_id = response.json()['order']['items'][-1]['id']

        url = self._get_dispatching_event_url(str(order.id), order_item_id)
        data = {
            'action': 'report_condition',
            'params': {
                'car_condition': 'ok',
                'insurance_present': True,
                'fuel_card_present': True,
                'sts_present': True,
            },
        }
        response = self.client.post(url, data=data)
        self.assertEqual(response.status_code, 200)
        url = self._get_order_item_list_url(str(order.id))
        data = {
            'type': 'carsharing_ride',
            'params': {
                'car_id': str(self.car.id),
            },
        }

        response = self.client.post(url, data=data)
        self.assertEqual(response.status_code, 200)

        return order


class CarRideByIMEITestCase(CarStateByIMEIBaseTestCase):

    def test_ride_with_no_active_order(self):
        response = self._perform_ride_endpoint_request(
            imei=self.car.imei,
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json(),
            {
                'id': None,
                'current_order_id': None,
                'current_ride_id': None,
                'current_user_id': None,
            }
        )

    def test_ride_with_active_ride_order_item(self):
        order = self._create_active_ride_item()
        response = self._perform_ride_endpoint_request(
            imei=self.car.imei,
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json(),
            {
                'id': str(self.car.id),
                'current_order_id': str(order.id),
                'current_ride_id': str(order.items.filter(type='carsharing_ride').first().id),
                'current_user_id': str(self.user.id),
            }
        )

    def test_ride_with_finished_ride_order_item(self):
        order = self._create_active_ride_item()
        self.order_manager.force_complete_order(order)

        response = self._perform_ride_endpoint_request(
            imei=self.car.imei,
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json(),
            {
                'id': str(self.car.id),
                'current_order_id': str(order.id),
                'current_ride_id': (
                    'post-' + str(order.items.filter(type='carsharing_ride').first().id)
                ),
                'current_user_id': str(self.user.id),
            }
        )


class CarStatusByIMEITestCase(CarStateByIMEIBaseTestCase):

    def test_status_with_no_active_order(self):
        response = self._perform_status_endpoint_request(
            imei=self.car.imei,
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json(),
            {
                'id': str(self.car.id),
                'status': 'available',
            }
        )

    def test_status_with_active_ride_order_item(self):
        self._create_active_ride_item()
        response = self._perform_status_endpoint_request(
            imei=self.car.imei,
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json(),
            {
                'id': str(self.car.id),
                'status': 'ride',
            }
        )

class CarAllInfoByIMEITestCase(CarStateByIMEIBaseTestCase):

    def test_within_order(self):
        order = self._create_active_ride_item()
        ride_order_item = OrderItem.objects.filter(
            type=OrderItem.Type.CARSHARING_RIDE.value,
            order=order,
        ).first()
        response = self._perform_all_info_endpoint_request(
            imei=self.car.imei,
            timestamp=time.time()
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car_id'], str(self.car.id))
        self.assertEqual(response.json()['session_id'], str(order.id))
        self.assertEqual(response.json()['action_id'], str(ride_order_item.id))
        self.assertIsNotNone(response.json()['timestamp_start'])
        self.assertIsNone(response.json()['timestamp_finish'])
        self.assertEqual(response.json()['car_status'], 'ride')
        LOGGER.info(response.json())

    def test_within_order_completed(self):
        order = self._create_active_ride_item()
        self.order_manager.force_complete_order(order)

        response = self._perform_all_info_endpoint_request(
            imei=self.car.imei,
            timestamp=time.time()
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car_id'], str(self.car.id))
        self.assertIsNone(response.json()['session_id'])
        self.assertIsNone(response.json()['action_id'])
        self.assertIsNotNone(response.json()['timestamp_start'])
        self.assertIsNone(response.json()['timestamp_finish'])
        self.assertEqual(response.json()['car_status'], 'available')
        LOGGER.info(response.json())

    def test_before_any_order(self):
        order = self._create_active_ride_item()
        self.order_manager.force_complete_order(order)

        response = self._perform_all_info_endpoint_request(
            imei=self.car.imei,
            timestamp=time.time() - 10**8,
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car_id'], str(self.car.id))
        self.assertIsNone(response.json()['session_id'])
        self.assertIsNone(response.json()['action_id'])
        self.assertIsNone(response.json()['timestamp_start'])
        self.assertIsNotNone(response.json()['timestamp_finish'])
        self.assertEqual(response.json()['car_status'], 'available')

    def test_in_reservation(self):
        order = self._create_active_ride_item()
        reservation_order_item = OrderItem.objects.filter(
            type=OrderItem.Type.CARSHARING_RESERVATION.value,
            order=order,
        ).first()
        self.order_manager.force_complete_order(order)

        timestamp = (
            reservation_order_item.started_at +
            (reservation_order_item.finished_at - reservation_order_item.started_at) / 2
        )

        response = self._perform_all_info_endpoint_request(
            imei=self.car.imei,
            timestamp=timestamp.timestamp(),
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car_id'], str(self.car.id))
        self.assertEqual(response.json()['session_id'], str(order.id))
        self.assertEqual(response.json()['action_id'], str(reservation_order_item.id))
        self.assertEqual(
            response.json()['timestamp_start'],
            reservation_order_item.started_at.timestamp(),
        )
        self.assertIsNotNone(
            response.json()['timestamp_finish'],
            reservation_order_item.finished_at.timestamp(),
        )
        self.assertEqual(response.json()['car_status'], 'reservation')
