from rest_framework.test import APITestCase, APIClient

from commerce.adv_backend.backend.utils import reverse


class TestViewPing(APITestCase):
    client = APIClient()
    maxDiff = None

    def test_ping(self):
        response = self.client.get(reverse('ping'))

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.content, b'pong')
