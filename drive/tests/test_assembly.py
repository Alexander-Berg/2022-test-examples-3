import logging
import time

from cars.carsharing.models import CarHardwareSim, CarHardwareBeacon, CarHardwareVega
from cars.users.factories import UserFactory
from django.test import TransactionTestCase
from django.urls import reverse

from cars.carsharing.models.car_document import CarDocument
from cars.service_app.core.assembly_manager import AssemblyManager
from .base import ServiceAppAPITestCase


LOGGER = logging.getLogger(__name__)


class AssemblyTestCase(ServiceAppAPITestCase):

    def simulate_upload_sim_request(self, user=None, payload=None):
        if not user:
            user = self.supervisor_user
        payload = payload or {}
        url = reverse('service_app:car-assembly-sim-upload')
        with self.as_user(user):
            response = self.client.post(url, payload, **{'HTTP_IMEI': '1'})
        return response

    def simulate_upload_beacon_request(self, user=None, payload=None):
        if not user:
            user = self.supervisor_user
        payload = payload or {}
        url = reverse('service_app:car-assembly-beacon-upload')
        with self.as_user(user):
            response = self.client.post(url, payload, **{'HTTP_IMEI': '1'})
        return response

    def simulate_upload_vega_request(self, user=None, payload=None):
        if not user:
            user = self.supervisor_user
        payload = payload or {}
        url = reverse('service_app:car-assembly-vega-upload')
        with self.as_user(user):
            response = self.client.post(url, payload, **{'HTTP_IMEI': '1'})
        return response

    def test_sim_upload_then_get_list(self):
        self.assertEqual(CarHardwareSim.objects.count(), 0)

        response = self.simulate_upload_sim_request(
            payload={
                'phone_number': '+71234567890',
                'icc': 89701998970199897019989701998970199,
            }
        )
        self.assertEqual(response.status_code, 200)

        sim = CarHardwareSim.objects.first()
        self.assertEqual(CarHardwareSim.objects.count(), 1)
        self.assertEqual(sim.phone_number, '+71234567890')
        self.assertEqual(sim.icc, 897019989701998970)
        self.assertEqual(CarDocument.objects.count(), 1)

    def test_sim_upload_then_get_list_then_upload_beacon(self):
        self.assertEqual(CarHardwareSim.objects.count(), 0)

        response = self.simulate_upload_sim_request(
            payload={
                'phone_number': '+71234567890',
                'icc': 89701998970199897019989701998970199,
            }
        )
        self.assertEqual(response.status_code, 200)

        sim = CarHardwareSim.objects.first()
        self.assertEqual(CarHardwareSim.objects.count(), 1)
        self.assertEqual(sim.phone_number, '+71234567890')
        self.assertEqual(sim.icc, 897019989701998970)
        self.assertEqual(CarDocument.objects.count(), 1)

        self.assertEqual(CarHardwareBeacon.objects.count(), 0)

        response = self.simulate_upload_beacon_request(
            payload={
                'imei': 'SN123456,123',
            }
        )

        self.assertEqual(CarHardwareBeacon.objects.count(), 1)
        beacon = CarHardwareBeacon.objects.first()
        self.assertEqual(beacon.imei, 123)
        self.assertIsNone(beacon.sim)

        response = self.simulate_upload_beacon_request(
            payload={
                'imei': 'SN123456,123',
                'sim_icc': 89701998970199897019989701998970199,
            }
        )
        self.assertEqual(response.status_code, 200)

        self.assertEqual(CarHardwareBeacon.objects.count(), 1)
        beacon = CarHardwareBeacon.objects.first()
        self.assertEqual(beacon.imei, 123)
        self.assertEqual(beacon.sim.phone_number, '+71234567890')
        self.assertEqual(beacon.sim.icc, 897019989701998970)
        self.assertEqual(beacon.serial_number, 'SN123456')
        self.assertEqual(CarDocument.objects.count(), 2)

    def test_sim_upload_then_get_list_then_upload_vega(self):
        self.assertEqual(CarHardwareSim.objects.count(), 0)

        response = self.simulate_upload_sim_request(
            payload={
                'phone_number': '+71234567890',
                'icc': 897019989701998970,
            }
        )
        self.assertEqual(response.status_code, 200)

        self.assertEqual(CarHardwareSim.objects.count(), 1)
        sim = CarHardwareSim.objects.first()
        self.assertEqual(sim.phone_number, '+71234567890')
        self.assertEqual(sim.icc, 897019989701998970)

        self.assertEqual(CarHardwareVega.objects.count(), 0)
        response = self.simulate_upload_vega_request(
            payload={
                'imei': 123,
            }
        )
        self.assertEqual(response.status_code, 200)

        self.assertEqual(CarHardwareVega.objects.count(), 1)
        vega = CarHardwareVega.objects.first()
        self.assertEqual(vega.imei, 123)
        self.assertIsNone(vega.primary_sim)
        self.assertIsNone(vega.secondary_sim)

        response = self.simulate_upload_vega_request(
            payload={
                'imei': 123,
                'primary_sim_icc': '89701998970199897019989701998970199',
            }
        )
        vega_document = CarDocument.objects.get(type='car_hardware_vega', car_hardware_vega__imei=123)
        LOGGER.info(vega_document.blob)
        self.assertEqual(response.status_code, 200)

        self.assertEqual(CarHardwareVega.objects.count(), 1)
        vega = CarHardwareVega.objects.first()

        vega_document = CarDocument.objects.get(car_hardware_vega=vega)
        LOGGER.info('Vega blob: %s', vega_document.blob)

        self.assertIsNotNone(vega_document.blob)

        self.assertEqual(vega.primary_sim.phone_number, '+71234567890')
        self.assertEqual(vega.primary_sim.icc, 897019989701998970)
        self.assertIsNone(vega.secondary_sim)
        self.assertEqual(CarDocument.objects.count(), 2)

class CarPickerAssignmentTestCase(ServiceAppAPITestCase):

    def simulate_assign_picker_request(self, car_id, picker_info):
        url = reverse('service_app:car-set-responsible-picker', kwargs={'car_id': car_id})
        with self.as_user(self.technician_user):
            response = self.client.post(url, {'picker': picker_info}, **{'HTTP_IMEI': '1'})
        return response

    def test_picker_assignment_ok(self):
        car = self.make_servicing_car()

        response = self.simulate_assign_picker_request(str(car.id), 'Test Picker')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['responsible_picker'], 'Test Picker')

        car.refresh_from_db()
        self.assertEqual(car.responsible_picker, 'Test Picker')


class CarVINChangeTestCase(ServiceAppAPITestCase):

    def simulate_change_vin_request(self, car_id, new_vin):
        url = reverse('service_app:car-change-vin', kwargs={'car_id': car_id})
        with self.as_user(self.technician_user):
            response = self.client.post(url, {'vin': new_vin}, **{'HTTP_IMEI': '1'})
        return response

    def test_vin_change_ok(self):
        car = self.make_servicing_car()

        new_vin = 'NEW VIN'
        self.assertNotEqual(car.vin, new_vin)
        response = self.simulate_change_vin_request(str(car.id), new_vin)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['vin'], new_vin)
        car.refresh_from_db()
        self.assertTrue(abs(time.time() - car.update_timestamp) < 10)

        car.refresh_from_db()
        self.assertEqual(car.vin, new_vin)


class SimBulkUploadTestCase(TransactionTestCase):

    def setUp(self):
        self.supervisor_user = UserFactory.create(uid=1)

    def test_bulk_upload_sim_cards(self):
        sim_cards = [
            {
                'phone_number': '+70001112233',
                'icc': '897019910000000000',
            },
            {
                'phone_number': '+70001112234',
                'icc': '897019920000000000',
            },
            {
                'phone_number': '+70001112235',
                'icc': '897019930000000000',
            },
        ]

        manager = AssemblyManager(self.supervisor_user)
        manager.bulk_add_sim_cards(sim_cards)

        self.assertEqual(CarDocument.objects.count(), 3)
