from django.urls import reverse

from cars.carsharing.models import CarHardwareBeacon, CarHardwareVega
from cars.carsharing.models.car_document import CarDocument, CarDocumentAssignment
from cars.service_app.core.assembly_manager import AssemblyManager
from ..base import ServiceAppAPITestCase


class AssemblyTestCase(ServiceAppAPITestCase):

    def setUp(self):
        super().setUp()
        self.car = self.make_servicing_car()
        self.other_car = self.make_servicing_car()
        self.other_car.imei = None
        self.car.imei = None
        self.car.save()
        self.other_car.save()

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

    def simulate_attach_device_request(self, car_id, device_code, user=None, force=False):
        if not user:
            user = self.supervisor_user
        payload = {
            'device_code': device_code,
        }
        url = reverse('service_app:car-attach-device', kwargs={'car_id': car_id})
        if force:
            payload['force'] = True
        with self.as_user(user):
            response = self.client.post(url, payload, **{'HTTP_IMEI': '1'})
        return response

    def simulate_get_attached_devices_request(self, car_id, user=None):
        if not user:
            user = self.supervisor_user
        url = reverse('service_app:car-attached-devices', kwargs={'car_id': car_id})
        with self.as_user(user):
            response = self.client.get(url, **{'HTTP_IMEI': '1'})
        return response

    def test_assign_hardware_scenario(self):
        self._preupload_hardware()
        # attach beacon
        response = self.simulate_attach_device_request(str(self.car.id), 'SN1267867,86000000000000')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['beacon']['imei'], 86000000000000)
        self.assertIsNone(self.car.imei)

        # attach new beacon, which is not present in the base, but will be created
        response = self.simulate_attach_device_request(str(self.car.id), 'SN1267867,86000000000001')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['beacon']['imei'], 86000000000001)
        self.assertIsNone(self.car.imei)
        self.assertIsNotNone(CarHardwareBeacon.objects.filter(imei=86000000000001))

        # attach vega
        response = self.simulate_attach_device_request(str(self.car.id), '86000000000001')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['beacon']['imei'], 86000000000001)
        self.assertEqual(response.json()['car']['vega']['imei'], 86000000000001)
        self.car.refresh_from_db()
        self.assertEqual(self.car.imei, 86000000000001)

        # attach non existent vega, ensure it's created and attached
        response = self.simulate_attach_device_request(str(self.car.id), '86000000000009')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['beacon']['imei'], 86000000000001)
        self.assertEqual(response.json()['car']['vega']['imei'], 86000000000009)

        # attach modem
        response = self.simulate_attach_device_request(str(self.car.id), '897019940000000000')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['beacon']['imei'], 86000000000001)
        self.assertEqual(response.json()['car']['vega']['imei'], 86000000000009)
        self.assertEqual(response.json()['car']['modem']['sim']['icc'], 897019940000000000)

        # attach non existent modem
        response = self.simulate_attach_device_request(str(self.car.id), '897019910000000000')
        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.json()['car']['beacon']['imei'], 86000000000001)
        self.assertEqual(response.json()['car']['vega']['imei'], 86000000000009)
        self.assertEqual(response.json()['car']['modem']['sim']['icc'], 897019940000000000)

        # attach head
        response = self.simulate_attach_device_request(str(self.car.id), '123456789ABC')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['beacon']['imei'], 86000000000001)
        self.assertEqual(response.json()['car']['vega']['imei'], 86000000000009)
        self.assertEqual(response.json()['car']['modem']['sim']['icc'], 897019940000000000)
        self.assertEqual(response.json()['car']['head']['head_id'], '123456789ABC')

        response = self.simulate_attach_device_request(str(self.car.id), '64300042100020016058425000001010965')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(CarDocumentAssignment.objects.filter(document__type='car_transponder').count(), 1)

        response = self.simulate_attach_device_request(str(self.car.id), '5M-1131200')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(CarDocumentAssignment.objects.filter(document__type='car_airport_pass').count(), 1)

        response = self.simulate_attach_device_request(str(self.car.id), '6362875000007654321')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(CarDocumentAssignment.objects.filter(document__type='car_transponder_spb').count(), 1)

        response = self.simulate_attach_device_request(
            str(self.car.id),
            'MT-32K LTE;861108034442932;897010269200757903;8970199171227765984'
        )
        print(response.json())
        self.assertEqual(response.status_code, 200)
        self.assertEqual(CarHardwareVega.objects.get(imei=861108034442932).primary_sim.icc, 89701026920075790)
        self.assertEqual(CarHardwareVega.objects.get(imei=861108034442932).secondary_sim.icc, 897019917122776598)

        response = self.simulate_attach_device_request(str(self.car.id), 'ABCDEF')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(CarDocumentAssignment.objects.filter(document__type='car_airport_pass_spb').count(), 1)

    def test_attached_devices(self):
        self._preupload_hardware()
        # attach beacon
        response = self.simulate_attach_device_request(str(self.car.id), 'SN1267867,86000000000000')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['beacon']['imei'], 86000000000000)

        response = self.simulate_get_attached_devices_request(
            str(self.car.id),
            user=self.technician_user
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['beacon']['imei'], 86000000000000)
        self.assertIsNone(response.json()['vega'])
        self.assertIsNone(response.json()['modem'])
        self.assertIsNone(response.json()['head'])

    def test_attach_several_vega(self):
        self._preupload_hardware()
        # attach vega
        response = self.simulate_attach_device_request(str(self.car.id), '86000000000001')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['vega']['imei'], 86000000000001)
        self.car.refresh_from_db()
        self.assertEqual(self.car.imei, 86000000000001)

        # attach another vega
        response = self.simulate_attach_device_request(str(self.car.id), '86000000000002')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['vega']['imei'], 86000000000002)
        self.car.refresh_from_db()
        self.assertEqual(self.car.imei, 86000000000002)

        # revert to older vega
        response = self.simulate_attach_device_request(str(self.car.id), '86000000000001')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['vega']['imei'], 86000000000001)
        self.car.refresh_from_db()
        self.assertEqual(self.car.imei, 86000000000001)

    def test_attach_one_device_to_different_cars(self):
        self._preupload_hardware()

        # attach vega
        response = self.simulate_attach_device_request(str(self.car.id), '86000000000001')
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['vega']['imei'], 86000000000001)
        self.car.refresh_from_db()
        self.assertEqual(self.car.imei, 86000000000001)

        # attach same vega to different car
        response = self.simulate_attach_device_request(str(self.other_car.id), '86000000000001')
        self.assertEqual(response.status_code, 400)
        self.assertEqual(response.json()['conflict_car']['vega']['imei'], 86000000000001)
        self.assertEqual(response.json()['conflict_car']['id'], str(self.car.id))
        print('conflict case response json:', response.json())
        self.car.refresh_from_db()
        self.other_car.refresh_from_db()
        self.assertEqual(self.car.imei, 86000000000001)
        self.assertIsNone(self.other_car.imei)

        # attach same vega to different car again, force
        response = self.simulate_attach_device_request(str(self.other_car.id), '86000000000001', force=True)
        self.assertEqual(response.status_code, 200)
        self.car.refresh_from_db()
        self.other_car.refresh_from_db()
        self.assertEqual(self.other_car.imei, 86000000000001)
        self.assertIsNone(self.car.imei)

    """
    def test_attach_vega_in_new_format(self):
        self._preupload_hardware()
        # attach vega
        response = self.simulate_attach_device_request(
            str(self.car.id),
            'MT-32K LTE;861108036392073;897010269200742995;8970199170667053027'
        )
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['car']['vega']['imei'], 861108036392073)
        self.car.refresh_from_db()
        self.assertEqual(self.car.imei, 861108036392073)
    """

    def _preupload_hardware(self):
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
            {
                'phone_number': '+70001112236',
                'icc': '897019940000000000',
            },
            {
                'phone_number': '+70001112237',
                'icc': '897019950000000000',
            },
            {
                'phone_number': '+70001112238',
                'icc': '897019960000000000',
            },
        ]

        manager = AssemblyManager(self.supervisor_user)
        manager.bulk_add_sim_cards(sim_cards)

        self.assertEqual(CarDocument.objects.count(), 6)

        manager.add_or_update_beacon(
            imei='SN123456,86000000000000',
            sim_icc='897019910000000000',
        )

        manager.add_or_update_vega(
            imei='86000000000001',
            primary_sim_icc='897019920000000000',
            secondary_sim_icc='897019930000000000',
        )

        manager.add_or_update_vega(
            imei='86000000000002',
            primary_sim_icc='897019950000000000',
            secondary_sim_icc='897019960000000000',
        )
