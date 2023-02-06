import contextlib

from rest_framework.test import APITestCase, APITransactionTestCase


class CarsharingTestCaseMixin:

    def assert_response_ok(self, response):
        self.assertEqual(response.status_code // 100, 2, response.data)

    def assert_response_bad_request(self, response):
        self.assertEqual(response.status_code, 400, response.data)

    def assert_response_permission_denied(self, response):
        self.assertEqual(response.status_code, 403, response.data)

    def assert_response_not_found(self, response):
        self.assertEqual(response.status_code, 404, response.data)

    def assert_response_success(self, response):
        self.assert_response_ok(response)
        self.assertIn('status', response.data, response.data)
        self.assertEqual(response.data['status'], 'success', response.data)

    def assert_response_errors(self, response, code=None):
        self.assert_response_ok(response)
        self.assertIn('status', response.data, response.data)
        self.assertEqual(response.data['status'], 'errors', response.data)
        self.assertIn('errors', response.data, response.data)
        if code:
            self.assertIn(code, response.data['errors'])

    @contextlib.contextmanager
    def as_user(self, user):
        spec = {
            'login': str(user.uid),
            'default_email': user.email,
        }
        if user.is_yandexoid is not None:
            spec['is_yandexoid'] = user.is_yandexoid
        with self.settings(YAUTH_TEST_USER=spec):
            yield


class CarsharingAPITestCase(CarsharingTestCaseMixin, APITestCase):
    pass


class CarsharingAPITransactionTestCase(CarsharingTestCaseMixin, APITransactionTestCase):
    pass
