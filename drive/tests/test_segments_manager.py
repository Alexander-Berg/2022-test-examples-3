from django.test import TestCase

from ..core.segments_prototype import CarsharingSegmentsManagerPrototype
from ..factories.car import CarFactory
from ..models import Car, CarModel


class CarSegmentsManagerTestCase(TestCase):

    def setUp(self):
        self.kia_model = CarModel(
            code='kia_rio',
            name='KIA Rio',
            manufacturer='KIA',
        )
        self.kia_model.save()

        self.kia_car = CarFactory.create(
            status=Car.Status.AVAILABLE.value,
            model=self.kia_model,
        )
        self.kia_car.save()

        self.kia_manager = CarsharingSegmentsManagerPrototype(self.kia_car)

    def test_night_waiting_query_inside_interval(self):
        is_night_waiting = self.kia_manager.is_night_waiting_active(0, is_plus_user=False)
        self.assertTrue(is_night_waiting)

        is_night_waiting = self.kia_manager.is_night_waiting_active(0, is_plus_user=True)
        self.assertTrue(is_night_waiting)

        start, end = self.kia_manager.get_night_waiting_interval(0, is_plus_user=False)
        self.assertEqual(start, 0 - 3 * 3600 - 1800)
        self.assertEqual(end, 3600 * 2 + 1800)

        start, end = self.kia_manager.get_night_waiting_interval(0, is_plus_user=True)
        self.assertEqual(start, 0 - 3 * 3600 - 1800)
        self.assertEqual(end, 3600 * 3)

    def test_night_waiting_outside_interval(self):
        is_night_waiting = self.kia_manager.is_night_waiting_active(3600 * 10, is_plus_user=False)
        self.assertFalse(is_night_waiting)

        is_night_waiting = self.kia_manager.is_night_waiting_active(3600 * 10, is_plus_user=True)
        self.assertFalse(is_night_waiting)

        start, end = self.kia_manager.get_night_waiting_interval(3600 * 10, is_plus_user=False)
        self.assertEqual(start, 86400 - 3600 * 3 - 1800)
        self.assertEqual(end, 86400 + 3600 * 2 + 1800)

        start, end = self.kia_manager.get_night_waiting_interval(3600 * 10, is_plus_user=True)
        self.assertEqual(start, 86400 - 3600 * 3 - 1800)
        self.assertEqual(end, 86400 + 3600 * 3)

    def test_night_waiting_active_before_midnight(self):
        is_night_waiting = self.kia_manager.is_night_waiting_active(86400 - 3600, is_plus_user=False)
        self.assertTrue(is_night_waiting)

        is_night_waiting = self.kia_manager.is_night_waiting_active(86400 - 3600, is_plus_user=True)
        self.assertTrue(is_night_waiting)

        start, end = self.kia_manager.get_night_waiting_interval(86400 - 3600, is_plus_user=False)
        self.assertEqual(start, 86400 - 3600 * 3 - 1800)
        self.assertEqual(end, 86400 + 3600 * 2 + 1800)

        start, end = self.kia_manager.get_night_waiting_interval(86400 - 3600, is_plus_user=True)
        self.assertEqual(start, 86400 - 3600 * 3 - 1800)
        self.assertEqual(end, 86400 + 3600 * 3)

    def test_night_waiting_right_after_non_plus_interval(self):
        is_night_waiting = self.kia_manager.is_night_waiting_active(2 * 3600 + 1800, is_plus_user=False)
        self.assertFalse(is_night_waiting)

        is_night_waiting = self.kia_manager.is_night_waiting_active(2 * 3600 + 1800, is_plus_user=True)
        self.assertTrue(is_night_waiting)

        start, end = self.kia_manager.get_night_waiting_interval(2 * 3600 + 1800, is_plus_user=False)
        self.assertEqual(start, 86400 - 3600 * 3 - 1800)
        self.assertEqual(end, 86400 + 3600 * 2 + 1800)

        start, end = self.kia_manager.get_night_waiting_interval(2 * 3600 + 1800, is_plus_user=True)
        self.assertEqual(start, 0 - 3600 * 3 - 1800)
        self.assertEqual(end, 3600 * 3)
