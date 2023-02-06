from django.urls import reverse

from cars.users.factories.user import UserFactory
from cars.users.models import User
from cars.admin.tests.base import AdminAPITestCase


class UserCreationTestCase(AdminAPITestCase):

    def get_user_deletion_url(self, user_id):
        return reverse('cars-admin:user-details', kwargs={'user_id': user_id})

    def get_user_creation_url(self):
        return reverse('cars-admin:user-create')

    def test_create_user(self):
        url = self.get_user_creation_url()
        with self.as_user(self.user):
            response = self.client.post(url, {'uid': 1234567})
            self.assert_response_ok(response)
            self.assertEqual(User.objects.filter(uid=1234567).count(), 1)

    def test_create_user_twice(self):
        url = self.get_user_creation_url()
        with self.as_user(self.user):
            response = self.client.post(url, {'uid': 1234567})
            self.assert_response_ok(response)
            self.assertEqual(User.objects.filter(uid=1234567).count(), 1)
            response = self.client.post(url, {'uid': 1234567})
            self.assertEqual(response.status_code, 400)

    def test_delete_user(self):
        u = User(status='active', uid=666)
        u.save()

        url = self.get_user_deletion_url(str(u.id))
        with self.as_user(self.user):
            response = self.client.delete(url)
            self.assert_response_ok(response)

            u.refresh_from_db()
            self.assertEqual(u.status, 'deleted')
            self.assertEqual(u.uid, -666)
