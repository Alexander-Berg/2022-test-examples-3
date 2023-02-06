import contextlib

from django.urls import reverse
from rest_framework.test import APITestCase

from cars.calculator.tests.helper import CalculatorTestHelper
from cars.carsharing.factories.car import CarFactory
from cars.users.factories import UserFactory
from cars.carsharing.models import Car


class ServiceAppAPITestCase(APITestCase):

    def setUp(self):
        self.supervisor_user = UserFactory.create(uid=1)
        self.cleaner_user = UserFactory(uid=2)
        self.technician_user = UserFactory(uid=3)

        self.other_cleaner_user = UserFactory(uid=4)
        self.other_technician_user = UserFactory(uid=5)

        self.ch = CalculatorTestHelper(tc=self)
        self.ch.setUp()

    def make_cleaning_car(self, car_status=None):
        if car_status is None:
            car_status = Car.Status.AVAILABLE.value

        car = CarFactory.create(
            status=car_status,
        )
        car.save()
        return car

    def make_servicing_car(self, car_status=None):
        if car_status is None:
            car_status = Car.Status.AVAILABLE.value

        car = CarFactory.create(
            status=car_status,
        )
        car.save()
        return car

    @contextlib.contextmanager
    def as_user(self, user):
        spec = {
            'login': str(user.uid),
            'default_email': user.email,
        }
        if user.is_yandexoid is not None:
            spec['is_yandexoid'] = user.is_yandexoid
        with self.settings(YAUTH_TEST_USER=spec):
            yield
