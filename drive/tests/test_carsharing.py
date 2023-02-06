from django.urls import reverse
from factory.fuzzy import FuzzyText

from cars.carsharing.factories import CarFactory
from cars.carsharing.models import Car
from cars.eka.models import FuelCardActivation
from .base import AdminAPITestCase


class CarListTestCase(AdminAPITestCase):

    @property
    def url(self):
        return reverse('cars-admin:car-list')

    def test_no_cars(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['cars']), 0)

    def test_one_car(self):
        CarFactory.create()
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['cars']), 1)

    def test_many_cars(self):
        for status in Car.Status:
            CarFactory.create(status=status.value)

        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data['cars']), len(Car.Status))


class CarInsuranceTestCase(AdminAPITestCase):

    def setUp(self):
        super().setUp()
        self.car = CarFactory.create()

    def get_car_details_url(self, car_id):
        return reverse('cars-admin:car-details', kwargs={'car_id': car_id})

    def test_has_insurance_policy(self):
        response = self.client.get(str(self.get_car_details_url(str(self.car.id))))
        self.assertEqual(response.status_code, 200)

        car = response.json()

        self.assertEqual(
            car['insurance']['agreement_number'],
            self.car.insurance.agreement_number
        )
        self.assertEqual(
            car['insurance']['agreement_partner_number'],
            self.car.insurance.agreement_partner_number
        )
        self.assertAlmostEqual(
            car['insurance']['base_cost'],
            str(self.car.insurance.base_cost)
        )
        self.assertAlmostEqual(
            car['insurance']['per_minute_cost'],
            str(self.car.insurance.per_minute_cost)
        )


class CarStatusUpdateTestCase(AdminAPITestCase):

    def get_move_to_available_url(self, car):
        return reverse('cars-admin:car-status-available', kwargs={'car_id': car.id})

    def get_move_to_service_url(self, car):
        return reverse('cars-admin:car-status-service', kwargs={'car_id': car.id})

    def assert_car_status_equal(self, car, status):
        car = Car.objects.get(id=car.id)
        self.assertEqual(car.get_status(), status)

    def test_move_to_available_ok(self):
        car = CarFactory.create(status=Car.Status.SERVICE.value)
        response = self.client.post(self.get_move_to_available_url(car))
        self.assert_response_ok(response)
        self.assert_car_status_equal(car, Car.Status.AVAILABLE)

    def test_already_available(self):
        car = CarFactory.create(status=Car.Status.AVAILABLE.value)
        response = self.client.post(self.get_move_to_available_url(car))
        self.assert_response_ok(response)
        self.assert_car_status_equal(car, Car.Status.AVAILABLE)

    def test_move_to_available_from_other_status(self):
        allowed_statuses = {
            Car.Status.AVAILABLE,
            Car.Status.CLEANING,
            Car.Status.FUELING,
            Car.Status.NEW,
            Car.Status.SERVICE,
        }
        for status in Car.Status:
            if status in allowed_statuses:
                continue
            car = CarFactory.create(status=status.value)
            response = self.client.post(self.get_move_to_available_url(car))
            self.assert_response_bad_request(response)
            self.assert_car_status_equal(car, status)

    def test_move_to_service_ok(self):
        car = CarFactory.create(status=Car.Status.AVAILABLE.value)
        response = self.client.post(self.get_move_to_service_url(car))
        self.assert_response_ok(response)
        self.assert_car_status_equal(car, Car.Status.SERVICE)

    def test_already_in_service(self):
        car = CarFactory.create(status=Car.Status.SERVICE.value)
        response = self.client.post(self.get_move_to_service_url(car))
        self.assert_response_ok(response)
        self.assert_car_status_equal(car, Car.Status.SERVICE)

    def test_move_to_service_from_other_status(self):
        allowed_statuses = {
            Car.Status.AVAILABLE,
            Car.Status.CLEANING,
            Car.Status.FUELING,
            Car.Status.NEW,
            Car.Status.SERVICE,
        }
        for status in Car.Status:
            if status in allowed_statuses:
                continue
            car = CarFactory.create(status=status.value)
            response = self.client.post(self.get_move_to_service_url(car))
            self.assert_response_bad_request(response)
            self.assert_car_status_equal(car, status)


class CarFuelCardNumberUpdateTestCase(AdminAPITestCase):

    def setUp(self):
        super().setUp()
        self.car = CarFactory.create()

    def _do_update_request(self, car, new_fuel_card_number):
        url = reverse('cars-admin:car-fuel-card-update', kwargs={'car_id': str(car.id)})
        response = self.client.post(url, {'number': new_fuel_card_number})
        return response

    def test_update_missing_fuel_card_number(self):
        self.car.fuel_card_number = None
        self.car.save()

        new_fuel_card_number = FuzzyText(length=18).fuzz()
        response = self._do_update_request(self.car, new_fuel_card_number)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['fuel_card_number'], new_fuel_card_number)

        car = Car.objects.get(id=self.car.id)
        self.assertEqual(car.fuel_card_number, new_fuel_card_number)

    def test_update_existing_fuel_card_number(self):
        new_fuel_card_number = FuzzyText(length=18).fuzz()
        response = self._do_update_request(self.car, new_fuel_card_number)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()['fuel_card_number'], new_fuel_card_number)

        car = Car.objects.get(id=self.car.id)
        self.assertEqual(car.fuel_card_number, new_fuel_card_number)


class CarFuelCardTestCase(AdminAPITestCase):

    def setUp(self):
        super().setUp()
        self.car = CarFactory.create()

    def _do_activate_card(self):
        url = reverse('cars-admin:car-fuel-card-activate', kwargs={'car_id': str(self.car.id)})
        response = self.client.post(url)
        return response

    def _do_block_card(self):
        url = reverse('cars-admin:car-fuel-card-block', kwargs={'car_id': str(self.car.id)})
        response = self.client.post(url)
        return response

    def test_unlock_then_lock(self):
        self.assertEqual(FuelCardActivation.objects.count(), 0)
        self._do_activate_card()
        self.assertEqual(FuelCardActivation.objects.count(), 1)
        self._do_block_card()
        self.assertEqual(FuelCardActivation.objects.count(), 1)
        self._do_activate_card()
        self.assertEqual(FuelCardActivation.objects.count(), 2)
        self._do_block_card()
        self.assertEqual(FuelCardActivation.objects.count(), 2)
