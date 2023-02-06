import datetime
import decimal

import pytz
from django.test import TestCase
from django.utils import timezone

import cars.settings
from cars.users.core.user_profile_updater import UserProfileUpdater
from cars.users.factories.user import UserFactory
from ..core.tariff_manager import (
    CarsharingTariffHolder, CarsharingTariffManager,
    CarsharingTariffPickerRequest, CarsharingTariffPickerV2,
)
from ..factories.car import CarFactory
from ..factories.car_model import CarModelFactory
from ..models.tariff_plan import CarsharingTariffPlan
from ..models.tariff_plan_entry import CarsharingTariffPlanEntry


class CarsharingTariffPickerTestCase(TestCase):

    def setUp(self):
        self.user = UserFactory.create()
        self.car = CarFactory.create()
        self.tariff_holder = CarsharingTariffHolder()
        self.tariff_picker = CarsharingTariffPickerV2(tariff_holder=self.tariff_holder)
        self.tariff_manager = CarsharingTariffManager(tariff_picker=self.tariff_picker)

    def create_wildcard_tariff(self,
                               ride_cost_per_minute, parking_cost_per_minute,
                               name=None, user_tag=None, car_model=None):
        name = name if name else 'test'
        car_model_code = car_model.code if car_model else None
        self.tariff_manager.create_tariff_plan(
            created_by=self.user,
            name=name,
            user_tag=user_tag,
            car_model_code=car_model_code,
            entries=[
                CarsharingTariffPlanEntry(
                    start_time=None,
                    end_time=None,
                    day_of_week=None,
                    ride_cost_per_minute=ride_cost_per_minute,
                    parking_cost_per_minute=parking_cost_per_minute,
                ),
            ],
        )
        self.tariff_manager.sync_tariff_plans()

    def pick_tariff(self, dt=None, tz=None, car=None):
        dt = dt or timezone.now()
        car = car or self.car
        request = CarsharingTariffPickerRequest(
            date=dt,
            timezone=tz,
            user_tags=self.user.tags,
            car_model_code=car.model_id,
            is_plus_user=self.user.get_plus_status(),
        )
        results = self.tariff_manager.pick_tariff(requests=[request])
        self.assertEqual(len(results), 1)
        return results[0]

    def test_no_candidates(self):
        self.tariff_manager.sync_tariff_plans()
        result = self.pick_tariff()
        self.assertIsNotNone(result.ride_cost_per_minute)
        self.assertIsNotNone(result.parking_cost_per_minute)

    def test_generic_tariff(self):
        self.create_wildcard_tariff(
            ride_cost_per_minute=1,
            parking_cost_per_minute=0,
        )
        result = self.pick_tariff()
        self.assertEqual(result.ride_cost_per_minute, 1)
        self.assertEqual(result.parking_cost_per_minute, 0)

    def test_latest_tariff_preferred(self):
        self.create_wildcard_tariff(
            name='t1',
            ride_cost_per_minute=1,
            parking_cost_per_minute=0,
        )
        self.create_wildcard_tariff(
            name='t2',
            ride_cost_per_minute=2,
            parking_cost_per_minute=1,
        )
        result = self.pick_tariff()
        self.assertEqual(result.ride_cost_per_minute, 2)
        self.assertEqual(result.parking_cost_per_minute, 1)

    def test_user_tag_match_preferred(self):
        UserProfileUpdater(self.user).add_tags(['test'])
        self.create_wildcard_tariff(
            name='t1',
            user_tag='test',
            ride_cost_per_minute=1,
            parking_cost_per_minute=0,
        )
        self.create_wildcard_tariff(
            name='t2',
            ride_cost_per_minute=2,
            parking_cost_per_minute=1,
        )
        result = self.pick_tariff()
        self.assertEqual(result.ride_cost_per_minute, 1)
        self.assertEqual(result.parking_cost_per_minute, 0)

    def test_car_model_match_preferred(self):
        other_model = CarModelFactory.create(code='test')
        other_car = CarFactory.create(model=other_model)

        self.create_wildcard_tariff(
            name='t1',
            car_model=other_model,
            ride_cost_per_minute=1,
            parking_cost_per_minute=0,
        )
        self.create_wildcard_tariff(
            name='t2',
            ride_cost_per_minute=2,
            parking_cost_per_minute=1,
        )
        result = self.pick_tariff(car=other_car)
        self.assertEqual(result.ride_cost_per_minute, 1)

    def test_user_tag_match_preferred_over_car_model_match(self):
        UserProfileUpdater(self.user).add_tags(['test'])

        other_model = CarModelFactory.create(code='test')
        other_car = CarFactory.create(model=other_model)

        self.create_wildcard_tariff(
            name='t1',
            user_tag='test',
            ride_cost_per_minute=1,
            parking_cost_per_minute=0,
        )
        self.create_wildcard_tariff(
            name='t2',
            car_model=other_model,
            ride_cost_per_minute=2,
            parking_cost_per_minute=1,
        )
        result = self.pick_tariff(car=other_car)
        self.assertEqual(result.ride_cost_per_minute, 1)
        self.assertEqual(result.parking_cost_per_minute, 0)
        self.assertEqual(result.parking_cost_per_minute, 0)

    def test_yandex_plus_accounted(self):
        UserProfileUpdater(self.user).add_tags(
            [
                'test',
            ]
        )

        other_model = CarModelFactory.create(code='test')
        other_car = CarFactory.create(model=other_model)

        self.create_wildcard_tariff(
            name='t1',
            user_tag='test',
            ride_cost_per_minute=2,
            parking_cost_per_minute=0,
        )
        self.create_wildcard_tariff(
            name='t2',
            car_model=other_model,
            ride_cost_per_minute=3,
            parking_cost_per_minute=1,
        )

        self.user.is_plus_user = True
        self.user.is_yandexoid = True
        self.user.save()

        result = self.pick_tariff(car=other_car)
        self.assertEqual(result.ride_cost_per_minute, decimal.Decimal('1.9'))
        self.assertEqual(result.parking_cost_per_minute, 0)
        self.assertEqual(result.parking_cost_per_minute, 0)

    def test_time_is_accounted(self):
        self.tariff_manager.create_tariff_plan(
            created_by=self.user,
            name='test',
            entries=[
                CarsharingTariffPlanEntry(
                    start_time=datetime.time(14, 0),
                    end_time=datetime.time(15, 0),
                    day_of_week=None,
                    ride_cost_per_minute=1,
                    parking_cost_per_minute=0,
                ),
                CarsharingTariffPlanEntry(
                    start_time=datetime.time(15, 0),
                    end_time=datetime.time(16, 0),
                    day_of_week=None,
                    ride_cost_per_minute=2,
                    parking_cost_per_minute=1,
                ),
            ],
        )
        self.tariff_manager.sync_tariff_plans()

        plan = CarsharingTariffPlan.objects.get()

        dt1 = plan.get_timezone().localize(datetime.datetime(2018, 1, 1, 14, 30))
        result1 = self.pick_tariff(dt=dt1)
        self.assertEqual(result1.ride_cost_per_minute, 1)
        self.assertEqual(result1.parking_cost_per_minute, 0)

        dt2 = plan.get_timezone().localize(datetime.datetime(2018, 1, 1, 15, 30))
        result2 = self.pick_tariff(dt=dt2)
        self.assertEqual(result2.ride_cost_per_minute, 2)
        self.assertEqual(result2.parking_cost_per_minute, 1)

    def test_day_of_week_is_accounted(self):
        self.tariff_manager.create_tariff_plan(
            created_by=self.user,
            name='test',
            entries=[
                CarsharingTariffPlanEntry(
                    start_time=None,
                    end_time=None,
                    day_of_week=1,
                    ride_cost_per_minute=1,
                    parking_cost_per_minute=0,
                ),
                CarsharingTariffPlanEntry(
                    start_time=None,
                    end_time=None,
                    day_of_week=2,
                    ride_cost_per_minute=2,
                    parking_cost_per_minute=1,
                ),
            ],
        )
        self.tariff_manager.sync_tariff_plans()

        plan = CarsharingTariffPlan.objects.get()

        dt1 = plan.get_timezone().localize(datetime.datetime(2018, 1, 1))
        result1 = self.pick_tariff(dt=dt1)
        self.assertEqual(result1.ride_cost_per_minute, 1)
        self.assertEqual(result1.parking_cost_per_minute, 0)

        dt2 = plan.get_timezone().localize(datetime.datetime(2018, 1, 2))
        result2 = self.pick_tariff(dt=dt2)
        self.assertEqual(result2.ride_cost_per_minute, 2)
        self.assertEqual(result2.parking_cost_per_minute, 1)

    def test_timezone_conversion(self):
        self.tariff_manager.create_tariff_plan(
            created_by=self.user,
            name='test',
            entries=[
                CarsharingTariffPlanEntry(
                    start_time=datetime.time(14, 0),
                    end_time=datetime.time(15, 0),
                    day_of_week=None,
                    ride_cost_per_minute=1,
                    parking_cost_per_minute=0,
                ),
            ],
        )
        self.tariff_manager.sync_tariff_plans()

        plan = CarsharingTariffPlan.objects.get()

        moscow_tz = pytz.timezone('Europe/Moscow')
        assert plan.get_timezone() == moscow_tz

        dt1 = moscow_tz.localize(datetime.datetime(2018, 1, 1, 14, 30))
        result1 = self.pick_tariff(dt=dt1)
        self.assertEqual(result1.ride_cost_per_minute, 1)
        self.assertEqual(result1.parking_cost_per_minute, 0)

        dt2 = pytz.utc.localize(datetime.datetime(2018, 1, 1, 11, 30))
        result2 = self.pick_tariff(dt=dt2)
        self.assertEqual(result2.ride_cost_per_minute, 1)
        self.assertEqual(result2.parking_cost_per_minute, 0)

    def test_request_timezone(self):
        self.tariff_manager.create_tariff_plan(
            created_by=self.user,
            name='test',
            entries=[
                CarsharingTariffPlanEntry(
                    start_time=datetime.time(14, 0),
                    end_time=datetime.time(15, 0),
                    day_of_week=None,
                    ride_cost_per_minute=1,
                    parking_cost_per_minute=0,
                ),
            ],
        )
        self.tariff_manager.sync_tariff_plans()

        plan = CarsharingTariffPlan.objects.get()

        moscow_tz = pytz.timezone('Europe/Moscow')
        assert plan.get_timezone() == moscow_tz

        dt = moscow_tz.localize(datetime.datetime(2018, 1, 1, 14, 30))

        result1 = self.pick_tariff(dt=dt, tz=moscow_tz)
        self.assertEqual(result1.start_time, datetime.time(14, 0))

        result2 = self.pick_tariff(dt=dt, tz=pytz.UTC)
        self.assertEqual(result2.start_time, datetime.time(11, 0))

    def test_free_parking_no_candidates(self):
        self.tariff_manager.create_tariff_plan(
            created_by=self.user,
            name='test',
            entries=[
                CarsharingTariffPlanEntry(
                    start_time=None,
                    end_time=None,
                    day_of_week=None,
                    ride_cost_per_minute=1,
                    parking_cost_per_minute=0,
                ),
            ],
        )
        self.tariff_manager.sync_tariff_plans()

        result = self.pick_tariff()

        self.assertEqual(result.ride_cost_per_minute, 1)
        self.assertEqual(result.parking_cost_per_minute, 0)
        self.assertIsNone(result.free_parking)

    def test_free_parking_ok(self):
        tariff_plan = self.tariff_manager.create_tariff_plan(
            created_by=self.user,
            name='test',
            entries=[
                CarsharingTariffPlanEntry(
                    start_time=datetime.time(0, 0),
                    end_time=datetime.time(6, 0),
                    day_of_week=None,
                    ride_cost_per_minute=1,
                    parking_cost_per_minute=0,
                ),
                CarsharingTariffPlanEntry(
                    start_time=datetime.time(6, 0),
                    end_time=datetime.time(12, 0),
                    day_of_week=None,
                    ride_cost_per_minute=4,
                    parking_cost_per_minute=2,
                ),
            ],
        )
        self.tariff_manager.sync_tariff_plans()

        result = self.pick_tariff(
            dt=tariff_plan.get_timezone().localize(datetime.datetime(2018, 1, 1, 3, 0, 0)),
        )

        self.assertEqual(result.ride_cost_per_minute, 1)
        self.assertEqual(result.parking_cost_per_minute, 0)

        self.assertIsNotNone(result.free_parking)
        self.assertEqual(
            result.free_parking.end_date,
            tariff_plan.get_timezone().localize(datetime.datetime(2018, 1, 1, 6, 0, 0)),
        )
        self.assertEqual(result.free_parking.next_tariff.parking_cost_per_minute, 2)

    def test_free_parking_specific_date(self):
        tariff_plan = self.tariff_manager.create_tariff_plan(
            created_by=self.user,
            name='test',
            entries=[
                CarsharingTariffPlanEntry(
                    start_time=None,
                    end_time=None,
                    day_of_week=None,
                    ride_cost_per_minute=1,
                    parking_cost_per_minute=0,
                ),
                CarsharingTariffPlanEntry(
                    start_time=datetime.time(6, 0),
                    end_time=datetime.time(12, 0),
                    day_of_week=1,
                    ride_cost_per_minute=4,
                    parking_cost_per_minute=2,
                ),
            ],
        )
        self.tariff_manager.sync_tariff_plans()

        result = self.pick_tariff(
            dt=tariff_plan.get_timezone().localize(datetime.datetime(2018, 1, 1, 3, 0, 0)),
        )

        self.assertIsNotNone(result.free_parking)
        self.assertEqual(
            result.free_parking.end_date,
            tariff_plan.get_timezone().localize(datetime.datetime(2018, 1, 1, 6, 0, 0)),
        )
        self.assertEqual(result.free_parking.next_tariff.parking_cost_per_minute, 2)

    def test_free_parking_next_day_no_start_time(self):
        tariff_plan = self.tariff_manager.create_tariff_plan(
            created_by=self.user,
            name='test',
            entries=[
                CarsharingTariffPlanEntry(
                    start_time=None,
                    end_time=None,
                    day_of_week=1,
                    ride_cost_per_minute=1,
                    parking_cost_per_minute=0,
                ),
                CarsharingTariffPlanEntry(
                    start_time=None,
                    end_time=None,
                    day_of_week=2,
                    ride_cost_per_minute=4,
                    parking_cost_per_minute=2,
                ),
            ],
        )
        self.tariff_manager.sync_tariff_plans()

        result = self.pick_tariff(
            dt=tariff_plan.get_timezone().localize(datetime.datetime(2018, 1, 1, 3, 0, 0)),
        )

        self.assertIsNotNone(result.free_parking)
        self.assertEqual(
            result.free_parking.end_date,
            tariff_plan.get_timezone().localize(datetime.datetime(2018, 1, 2, 0, 0, 0)),
        )
        self.assertEqual(result.free_parking.next_tariff.parking_cost_per_minute, 2)

    def test_free_parking_next_day_definite_start_time(self):
        tariff_plan = self.tariff_manager.create_tariff_plan(
            created_by=self.user,
            name='test',
            entries=[
                CarsharingTariffPlanEntry(
                    start_time=None,
                    end_time=None,
                    day_of_week=1,
                    ride_cost_per_minute=1,
                    parking_cost_per_minute=0,
                ),
                CarsharingTariffPlanEntry(
                    start_time=datetime.time(6, 0),
                    end_time=None,
                    day_of_week=2,
                    ride_cost_per_minute=4,
                    parking_cost_per_minute=2,
                ),
            ],
        )
        self.tariff_manager.sync_tariff_plans()

        result = self.pick_tariff(
            dt=tariff_plan.get_timezone().localize(datetime.datetime(2018, 1, 1, 3, 0, 0)),
        )

        self.assertIsNotNone(result.free_parking)
        self.assertEqual(
            result.free_parking.end_date,
            tariff_plan.get_timezone().localize(datetime.datetime(2018, 1, 2, 6, 0, 0)),
        )
        self.assertEqual(result.free_parking.next_tariff.parking_cost_per_minute, 2)

    def test_free_parking_with_wildcard_alternative(self):
        UserProfileUpdater(self.user).add_tags(['test'])

        tariff_plan = self.tariff_manager.create_tariff_plan(
            created_by=self.user,
            name='test-1',
            user_tag='test',
            entries=[
                CarsharingTariffPlanEntry(
                    start_time=None,
                    end_time=datetime.time(23, 0),
                    day_of_week=1,
                    ride_cost_per_minute=1,
                    parking_cost_per_minute=0,
                ),
            ],
        )
        self.tariff_manager.create_tariff_plan(
            created_by=self.user,
            name='test-2',
            entries=[
                CarsharingTariffPlanEntry(
                    start_time=None,
                    end_time=None,
                    day_of_week=None,
                    ride_cost_per_minute=4,
                    parking_cost_per_minute=2,
                ),
            ],
        )
        self.tariff_manager.sync_tariff_plans()

        result = self.pick_tariff(
            dt=tariff_plan.get_timezone().localize(datetime.datetime(2018, 1, 1, 21, 0, 0)),
        )

        self.assertIsNotNone(result.free_parking)
        self.assertEqual(
            result.free_parking.end_date,
            tariff_plan.get_timezone().localize(datetime.datetime(2018, 1, 1, 23, 0, 0)),
        )
        self.assertEqual(result.free_parking.next_tariff.parking_cost_per_minute, 2)
