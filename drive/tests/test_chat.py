from django.test import TestCase
from django.urls import reverse

from cars.registration.models.chat_message import RegistrationChatMessage
from cars.users.factories import UserFactory
from cars.users.models.registration_state import RegistrationState
from cars.users.models.user import User


class ChatTestCase(TestCase):

    @property
    def chat_url(self):
        return reverse('registration:chat')

    @property
    def chat_action_url(self):
        return reverse('registration:chat-action')

    def setUp(self):
        self.user = UserFactory.create(
            uid=1,
            username='1',
            status=User.Status.ONBOARDING.value,
        )

    def get_registration_state(self, user=None):
        if user is None:
            user = self.user
        return RegistrationState.objects.filter(user=user).first()

    def test_first_chat_access(self):
        self.assertIsNone(self.get_registration_state())
        self.assertFalse(RegistrationChatMessage.objects.filter(user=self.user).exists())

        response = self.client.get(self.chat_url)
        self.assertEqual(response.status_code, 200)

        self.assertIsNotNone(self.get_registration_state())
        self.assertTrue(RegistrationChatMessage.objects.filter(user=self.user).exists())

    def test_submit_intro(self):
        response = self.client.get(self.chat_url)

        action = response.data['action']
        self.assertEqual(action['id'], 'intro')
        self.assertEqual(action['type'], 'ok')

        self.assertIsNotNone(self.get_registration_state().chat_started_at)
        self.assertEqual(self.get_registration_state().chat_action_id, 'intro')

        data = {
            'id': 'intro',
            'data': {},
        }
        response = self.client.post(self.chat_action_url, data=data)
        self.assertEqual(response.status_code, 200)
