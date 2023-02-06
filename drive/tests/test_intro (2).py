from django.urls import reverse
from django.utils import timezone

from cars.users.factories.user import UserFactory
from cars.users.models.user import User
from cars.callcenter.models.call_priority import CallPriorityUser
from .base import DriveAPITestCase


class IntroTestCase(DriveAPITestCase):

    def setUp(self):
        super().setUp()
        self.user = UserFactory.create()

    @property
    def url(self):
        return reverse('drive:intro')

    def test_ok(self):
        response = self.client.get(self.url)
        self.assertEqual(response.status_code, 200, response.data)
        self.assertIn('user', response.data)
        self.assertIn('user_setup', response.data)

    def test_call_priority_on(self):
        cpu = CallPriorityUser(user_id=self.user.id, added_at=timezone.now())
        cpu.save()
        with self.as_user(self.user):
            response = self.client.get(self.url)
        self.assertEqual(response.data['user']['call_priority'], True)

    def test_call_priority_off(self):
        CallPriorityUser.objects.filter(user_id=self.user.id).delete()
        with self.as_user(self.user):
            response = self.client.get(self.url)
        self.assertEqual(response.data['user']['call_priority'], False)
