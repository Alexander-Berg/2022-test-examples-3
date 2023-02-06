from unittest.mock import patch

from django.urls import reverse

from cars.django.tests import CarsharingAPITestCase
from cars.carsharing.core.tariff_manager import (
    CarsharingTariffHolder, CarsharingTariffManager, CarsharingTariffPickerV2,
)
from cars.carsharing.models.tariff_plan_entry import CarsharingTariffPlanEntry
from cars.users.factories.user import UserFactory
from ..views.carsharing_base import CarsharingBaseView


class CarsharingBaseTestCase(CarsharingAPITestCase):

    @property
    def url(self):
        return reverse('calculator:carsharing-base')

    def setUp(self):
        self.user = UserFactory.create()

        self.tariff_holder = CarsharingTariffHolder()
        self.tariff_picker = CarsharingTariffPickerV2(tariff_holder=self.tariff_holder)
        self.tariff_manager = CarsharingTariffManager(tariff_picker=self.tariff_picker)

        patcher = patch.object(CarsharingBaseView, '_tariff_manager', self.tariff_manager)
        patcher.start()
        self.addCleanup(patcher.stop)

    def test_ok(self):
        response = self.client.post(self.url, data={'requests': []})
        self.assert_response_ok(response)
        self.assertEqual(response.data['results'], [])

    def test_simple_tariff(self):
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

        data = {
            'requests': [
                {
                    'date': 0,
                    'user_tags': [],
                    'car_model_code': 'test',
                },
            ],
        }
        response = self.client.post(self.url, data=data)
        self.assert_response_ok(response)

        self.assertIn('results', response.data)

        results = response.data['results']
        self.assertEqual(len(results), 1)

        result = results[0]
        self.assertEqual(result['ride_cost_per_minute'], '1.00')
        self.assertEqual(result['parking_cost_per_minute'], '0.00')
