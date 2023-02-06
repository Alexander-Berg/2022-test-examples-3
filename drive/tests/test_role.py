from django.urls import reverse
from .base import AdminAPITestCase


class CurrentUserRoleTestCase(AdminAPITestCase):

    @property
    def url(self):
        return reverse('cars-admin:current-user-role')

    def test_ok(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {'role': 'admin'})
