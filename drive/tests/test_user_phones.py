from django.urls import reverse
from rest_framework.test import APITestCase


class UserPhonesTestCase(APITestCase):

    @property
    def submit_url(self):
        return reverse('drive:user-phones-submit')

    @property
    def commit_url(self):
        return reverse('drive:user-phones-commit')

    def test_simple(self):
        response = self.client.post(self.submit_url)
        self.assertEqual(response.status_code, 400)
