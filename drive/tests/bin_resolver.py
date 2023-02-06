from django.urls import reverse
from rest_framework.test import APITestCase


class BinResolverTestCase(APITestCase):

    def get_url(self, bin_):
        return '{}?bin={}'.format(reverse('util:bin-resolver'), bin_)

    def get_bin_response(self, bin_, expected_status_code=200):
        response = self.client.get(self.get_url(bin_))
        self.assertEqual(response.status_code, expected_status_code)
        return response

    def test_resolve_empty(self):
        self.get_bin_response('', expected_status_code=400)

    def test_resolve_nonexistent(self):
        response = self.get_bin_response('123456')
        self.assertIsNone(response.data['data'])

    def test_resolve_sberbank(self):
        response = self.get_bin_response('557000')
        self.assertEqual(response.data['data']['name'], 'Сбербанк')
