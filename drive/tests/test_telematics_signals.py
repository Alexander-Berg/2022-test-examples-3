import time

from django.urls import reverse

from cars.carsharing.core.wialon_parameters import WialonParameters
from cars.carsharing.factories.car import CarFactory
from cars.carsharing.models.car_location import CarLocation
from cars.carsharing.models.car_telematics_state import CarTelematicsState
from .base import AdminAPITestCase


class TelematicsSignalsTestCase(AdminAPITestCase):

    @property
    def url(self):
        return reverse('cars-admin:telematics-signals')

    def setUp(self):
        super().setUp()
        self.car = CarFactory.create()

    def make_payload(self, imei, custom_parameters=None, position_data=None):
        data = {
            'imei': imei,
            'packet': {
                'records': [
                    {
                        'subrecords': [],
                        'timestamp': time.time(),
                        'type': 'record',
                    }
                ],
            },
        }

        if custom_parameters:
            data['packet']['records'][0]['subrecords'].append({
                'params': custom_parameters,
                'type': 'custom_parameters',
            })

        if position_data:
            data['packet']['records'][0]['subrecords'].append(position_data)

        return data

    def test_nonexistent_car(self):
        data = self.make_payload(
            imei=123,
            custom_parameters={
                WialonParameters.MILEAGE.value.code: 100,
            },
        )
        response = self.client.post(self.url, data=data)
        self.assertEqual(response.status_code, 404)

    """
    def test_custom_parameters(self):
        data = self.make_payload(
            imei=self.car.imei,
            custom_parameters={
                WialonParameters.MILEAGE.value.code: 100,
            },
        )
        response = self.client.post(self.url, data=data)
        self.assertEqual(response.status_code, 200)

        state = CarTelematicsState.objects.filter(car=self.car).first()
        self.assertIsNotNone(state)
        self.assertEqual(state.mileage, 100)
        self.assertAlmostEqual(
            state.mileage_updated_at.timestamp(),
            data['packet']['records'][0]['timestamp'],
            delta=1e-3,
        )
    def test_custom_parameters_with_unknown_code(self):
        data = self.make_payload(
            imei=self.car.imei,
            position_data={
                'course': 70,
                'hdop': 0,
                'height': 139,
                'lat': 12.345,
                'lon': 54.321,
                'sats': 15,
                'speed': 0,
                'type': 'position_data'
            },
        )
        response = self.client.post(self.url, data=data)
        self.assertEqual(response.status_code, 200)

        location = CarLocation.objects.filter(car=self.car).first()
        self.assertIsNotNone(location)
        self.assertAlmostEqual(location.lat, 12.345)
        self.assertAlmostEqual(location.lon, 54.321)
        self.assertEqual(location.course, 70)

    def test_location_ok(self):
        data = self.make_payload(
            imei=self.car.imei,
            position_data={
                'course': 70,
                'hdop': 0,
                'height': 139,
                'lat': 12.345,
                'lon': 54.321,
                'sats': 15,
                'speed': 0,
                'type': 'position_data'
            },
        )
        response = self.client.post(self.url, data=data)
        self.assertEqual(response.status_code, 200)

        location = CarLocation.objects.filter(car=self.car).first()
        self.assertIsNotNone(location)
        self.assertAlmostEqual(location.lat, 12.345)
        self.assertAlmostEqual(location.lon, 54.321)
        self.assertEqual(location.course, 70)
    """
