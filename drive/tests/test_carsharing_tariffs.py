from unittest.mock import patch

from django.urls import reverse

from cars.carsharing.core.tariff_manager import (
    CarsharingTariffHolder, CarsharingTariffManager, CarsharingTariffPickerV2,
)
from cars.carsharing.factories.car_model import CarModelFactory
from cars.carsharing.models.tariff_plan import CarsharingTariffPlan
from cars.users.factories.user import UserFactory
from ..views.carsharing_tariffs import CarsharingTariffPlanBaseView
from .base import AdminAPITestCase


class CarsharingTariffsTestCase(AdminAPITestCase):

    def setUp(self):
        super().setUp()

        self.tariff_holder = CarsharingTariffHolder()
        self.tariff_picker = CarsharingTariffPickerV2(tariff_holder=self.tariff_holder)
        self.tariff_manager = CarsharingTariffManager(tariff_picker=self.tariff_picker)

        patcher = patch.object(
            CarsharingTariffPlanBaseView,
            '_tariff_manager',
            self.tariff_manager,
        )
        patcher.start()
        self.addCleanup(patcher.stop)

    def get_list_url(self):
        return reverse('cars-admin:carsharing-tariff-plan-list')

    def get_details_url(self, tariff_plan_id):
        return reverse(
            'cars-admin:carsharing-tariff-plan-details',
            kwargs={
                'tariff_plan_id': tariff_plan_id,
            },
        )

    def get_create_or_update_plan_payload(self, name='test', entries=None,
                                          user_tag=None, car_model_code=None):
        if entries is None:
            entries = [
                self.get_create_plan_entry_payload(),
            ]

        payload = {
            'name': name,
            'entries': entries,
        }
        if user_tag is not None:
            payload['user_tag'] = user_tag
        if car_model_code is not None:
            payload['car_model_code'] = car_model_code

        return payload

    def get_create_plan_entry_payload(self, start_time=None, end_time=None, day_of_week=None,
                                      parking_cost_per_minute=None, ride_cost_per_minute=None):
        if start_time is None:
            start_time = '00:00'
        if end_time is None:
            end_time = '24:00'
        if parking_cost_per_minute is None:
            parking_cost_per_minute = 1
        if ride_cost_per_minute is None:
            ride_cost_per_minute = 2

        payload = {
            'start_time': start_time,
            'end_time': end_time,
            'day_of_week': day_of_week,
            'parking_cost_per_minute': parking_cost_per_minute,
            'ride_cost_per_minute': ride_cost_per_minute,
        }

        return payload

    def test_create_plan(self):
        data = self.get_create_or_update_plan_payload(
            name='test',
            entries=[
                self.get_create_plan_entry_payload(
                    start_time='00:00:00',
                    end_time='24:00:00',
                    parking_cost_per_minute=512,
                    ride_cost_per_minute=1024,
                ),
            ],
        )

        response = self.client.post(self.get_list_url(), data=data)
        self.assert_response_success(response)

        plan = CarsharingTariffPlan.objects.get(id=response.data['tariff_plan']['id'])
        self.assertEqual(plan.name, 'test')

        self.assertEqual(plan.entries.count(), 1)
        entry = plan.entries.get()
        self.assertIsNone(entry.start_time)
        self.assertIsNone(entry.end_time)
        self.assertEqual(entry.parking_cost_per_minute, 512)
        self.assertEqual(entry.ride_cost_per_minute, 1024)

        list_response = self.client.get(self.get_list_url())
        tariff_plans_data = list_response.data['results']
        self.assertEqual(len(tariff_plans_data), 1)

        tariff_plan_data = tariff_plans_data[0]
        self.assertEqual(len(tariff_plan_data['entries']), 1)

        tariff_plan_entry_data = tariff_plan_data['entries'][0]
        self.assertEqual(tariff_plan_entry_data['start_time'], '00:00:00')
        self.assertEqual(tariff_plan_entry_data['end_time'], '24:00:00')


    def test_create_duplicate_plan(self):
        data = self.get_create_or_update_plan_payload()

        response1 = self.client.post(self.get_list_url(), data=data)
        self.assert_response_success(response1)

        response2 = self.client.post(self.get_list_url(), data=data)
        self.assert_response_errors(response2, code='tariff_plan.exists')

        self.assertEqual(CarsharingTariffPlan.objects.count(), 1)

    def test_create_plan_with_overlapping_entries(self):
        data = self.get_create_or_update_plan_payload(
            entries=[
                self.get_create_plan_entry_payload(
                    day_of_week=3,
                    start_time='00:00:00',
                    end_time='03:00:00',
                ),
                self.get_create_plan_entry_payload(
                    day_of_week=3,
                    start_time='02:00:00',
                    end_time='04:00:00',
                ),
            ],
        )
        response = self.client.post(self.get_list_url(), data=data)
        self.assert_response_errors(response, code='tariff_plan.entries.inconsistent')

    def test_create_plan_with_overlapping_entries_different_dow(self):
        data = self.get_create_or_update_plan_payload(
            entries=[
                self.get_create_plan_entry_payload(
                    day_of_week=2,
                    start_time='00:00:00',
                    end_time='03:00:00',
                ),
                self.get_create_plan_entry_payload(
                    day_of_week=3,
                    start_time='02:00:00',
                    end_time='04:00:00',
                ),
            ],
        )
        response = self.client.post(self.get_list_url(), data=data)
        self.assert_response_success(response)

    def test_update_plan(self):
        car_model = CarModelFactory.create()

        create_data = self.get_create_or_update_plan_payload(
            name='test',
            car_model_code=car_model.code,
            entries=[
                self.get_create_plan_entry_payload(),
            ],
        )
        create_response = self.client.post(self.get_list_url(), data=create_data)
        tariff_plan_id = create_response.data['tariff_plan']['id']

        update_data = self.get_create_or_update_plan_payload(
            name='test_updated',
            user_tag='tag_updated',
            entries=[
                self.get_create_plan_entry_payload(
                    start_time='00:00:00',
                    end_time='01:00:00',
                ),
                self.get_create_plan_entry_payload(
                    start_time='01:00:00',
                    end_time='02:00:00',
                ),
            ],
        )
        update_response = self.client.put(
            self.get_details_url(tariff_plan_id),
            data=update_data,
        )
        self.assert_response_success(update_response)
        tariff_plan_data = update_response.data['tariff_plan']
        self.assertEqual(tariff_plan_data['name'], 'test_updated')
        self.assertEqual(tariff_plan_data['user_tag'], 'tag_updated')
        self.assertEqual(len(tariff_plan_data['entries']), 2)

        tariff_plan = CarsharingTariffPlan.objects.get(id=tariff_plan_id)
        self.assertEqual(tariff_plan.name, 'test_updated')
        self.assertEqual(tariff_plan.user_tag, 'tag_updated')
        self.assertEqual(tariff_plan.entries.count(), 2)
        self.assertIsNone(tariff_plan.car_model)

    def test_delete_plan(self):
        data = self.get_create_or_update_plan_payload()
        create_response = self.client.post(self.get_list_url(), data=data)
        self.assertTrue(CarsharingTariffPlan.objects.exists())

        delete_response = self.client.delete(
            self.get_details_url(create_response.data['tariff_plan']['id']),
        )
        self.assert_response_ok(delete_response)
        self.assertFalse(CarsharingTariffPlan.objects.exists())

    def test_read_restricted(self):
        user = UserFactory()
        with self.as_user(user):
            response = self.client.get(self.get_list_url())
        self.assert_response_permission_denied(response)

    def test_create_restricted(self):
        user = UserFactory()
        with self.as_user(user):
            response = self.client.post(
                self.get_list_url(),
                data=self.get_create_or_update_plan_payload(),
            )
        self.assert_response_permission_denied(response)

    def test_delete_restricted(self):
        create_response = self.client.post(
            self.get_list_url(),
            data=self.get_create_or_update_plan_payload(),
        )

        user = UserFactory()
        with self.as_user(user):
            response = self.client.delete(
                self.get_details_url(create_response.data['tariff_plan']['id']),
            )
        self.assert_response_permission_denied(response)
