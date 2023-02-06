from django.urls import reverse
from .base import AdminAPITestCase


class ConstantsTestCase(AdminAPITestCase):

    @property
    def url(self):
        return reverse('cars-admin:constants')

    def test_ok(self):
        response = self.client.get(self.url)
        self.assert_response_ok(response)
        self.assertIn('photo_types', response.data)
        self.assertIn('user_statuses', response.data)
        self.assertIn('carsharing', response.data)
        self.assertIn('car_models', response.data['carsharing'])
