# pytest volta  --reuse-db --nomigrations

from django.test import Client
from django.test import TestCase


class CommonTestCase(TestCase):

    def setUp(self):
        self.client = Client()

    @staticmethod
    def get_data_from_resp(response):
        return response.content.decode('utf-8').rstrip().split('\n')

