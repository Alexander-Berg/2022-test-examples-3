from django.urls import reverse
from rest_framework.test import APITestCase

from cars.users.factories.user import UserFactory
from cars.users.models.user import User


class UserEmailTestCase(APITestCase):

    @property
    def url(self):
        return reverse('drive:user-email')

    def setUp(self):
        self.user = UserFactory.create(uid=1)

    def post(self, email, expect_ok=True):
        response = self.client.post(self.url, data={'email': email})
        self.assertEqual(response.status_code, 200)
        if expect_ok:
            self.assertEqual(response.data['status'], 'success')
        else:
            self.assertIn('errors', response.data)
        return response

    def assert_user_email_equal(self, email):
        user = User.objects.get(id=self.user.id)
        self.assertEqual(user.email, email)

    def test_ok(self):
        self.post('test@yandex.ru')
        self.assert_user_email_equal('test@yandex.ru')

    def test_same_email(self):
        self.user.email = 'test@yandex.ru'
        self.post('test@yandex.ru')

    def test_update_twice(self):
        self.post('once@yandex.ru')
        self.post('twice@yandex.ru')
        self.assert_user_email_equal('twice@yandex.ru')

    def test_invalid_email(self):
        response = self.post('lalala', expect_ok=False)
        self.assertEqual(response.data['errors'], ['email.invalid'])

    def test_duplicate_email(self):
        UserFactory.create(uid=2, email='test@yandex.ru')
        response = self.post('test@yandex.ru', expect_ok=False)
        self.assertEqual(response.data['errors'], ['email.exists'])
