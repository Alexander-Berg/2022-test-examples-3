from django.test import TestCase

from ..core.car_updater import CarUpdater
from ..factories.car import CarFactory
from ..models.car import Car


class CarUpdaterTestCase(TestCase):

    def setUp(self):
        self.car = CarFactory.create(status=Car.Status.AVAILABLE.value)
        self.updater = CarUpdater(self.car)

    def test_update_status_ok(self):
        self.updater.update_status(Car.Status.SERVICE)
        self.assertEqual(Car.objects.get(id=self.car.id).get_status(), Car.Status.SERVICE)

    def test_update_status_bad_transition(self):
        with self.assertRaises(self.updater.BadStatusError):
            self.updater.update_status(Car.Status.NEW)
        self.assertEqual(Car.objects.get(id=self.car.id).get_status(), Car.Status.AVAILABLE)

    def test_update_status_updates_updated_at(self):
        self.assertIsNone(self.car.updated_at)
        self.updater.update_status(Car.Status.SERVICE)
        self.assertIsNotNone(Car.objects.get(id=self.car.id).updated_at)
