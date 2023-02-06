from cars.carsharing.factories import CarFactory
from django.urls import reverse
from django.utils import timezone

from cars.carsharing.models import CarDocument
from cars.carsharing.models.car_document import CarDocumentAssignment
from cars.carsharing.models.car_hardware import CarHardwareSim, CarHardwareModem
from cars.users.factories import UserFactory
from .base import AdminAPITestCase


class ConstantsTestCase(AdminAPITestCase):

    def setUp(self):
        super().setUp()
        self._user = UserFactory.create()
        self._car = CarFactory.create()

        self._sim = CarHardwareSim(
            phone_number='+70001112233',
            icc=897019910000000000,
        )
        self._sim.save()

        self._modem = CarHardwareModem(
            sim=self._sim,
        )
        self._modem.save()

        self._document = CarDocument(
            type=CarDocument.Type.CAR_HARDWARE_MODEM.value,
            added_at=timezone.now(),
            added_by=self._user,
            car_hardware_modem=self._modem,
        )
        self._document.save()

        self._document_assignment = CarDocumentAssignment(
            document=self._document,
            car=self._car,
            assigned_at=timezone.now(),
            assigned_by=self._user,
        )
        self._document_assignment.save()

    def _get_request_url(self, car_id, document_id):
        return reverse(
            'cars-admin:car-document-detach',
            kwargs={
                'car_id': car_id,
                'document_id': document_id,
            }
        )

    def test_ok(self):
        url = self._get_request_url(
            car_id=str(self._car.id),
            document_id=str(self._document.id),
        )
        with self.as_user(self.user):
            response = self.client.post(url)

        self.assertEqual(response.status_code, 200)
        self._document_assignment.refresh_from_db()
        self.assertIsNotNone(self._document_assignment.unassigned_at)
