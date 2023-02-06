from django.urls import reverse
from django.utils import timezone

from cars.bans.models.ban import Ban
from cars.users.models.registration_state import RegistrationState
from cars.users.models.user import User
from .base import AdminAPITestCase


class BanTestCase(AdminAPITestCase):

    def setUp(self):
        super().setUp()

        registration_state = self.user.get_registration_state()
        if registration_state is None:
            registration_state = RegistrationState.objects.create(user=self.user)
        registration_state.chat_completed_at = timezone.now()
        registration_state.save()

    def get_block_url(self, user=None):
        if user is None:
            user = self.user
        return reverse('cars-admin:user-block', kwargs={'user_id': user.id})

    def get_ban_list_url(self, user=None):
        if user is None:
            user = self.user
        return reverse('cars-admin:user-ban-list', kwargs={'user_id': user.id})

    def get_unblock_url(self, user=None):
        if user is None:
            user = self.user
        return reverse('cars-admin:user-unblock', kwargs={'user_id': user.id})

    def assert_user_status_equal(self, status, user=None):
        if user is None:
            user = self.user
        user.refresh_from_db()
        self.assertIs(user.get_status(), status)

    def test_block_and_unblock(self):
        self.assert_user_status_equal(User.Status.ACTIVE)

        block_response = self.client.post(self.get_block_url(), data={'reason': 'blocked_other'})
        self.assert_response_success(block_response)
        self.assert_user_status_equal(User.Status.BLOCKED)

        unblock_response = self.client.post(self.get_unblock_url())
        self.assert_response_success(unblock_response)
        self.assert_user_status_equal(User.Status.ACTIVE)

    def test_block_no_permissions(self):
        with self.as_user(self.user_no_permissions):
            response = self.client.post(self.get_block_url())
        self.assert_response_permission_denied(response)

    def test_unblock_no_permissions(self):
        with self.as_user(self.user_no_permissions):
            response = self.client.post(self.get_unblock_url())
        self.assert_response_permission_denied(response)

    def test_block_comment(self):
        block_response = self.client.post(
            self.get_block_url(),
            data={
                'reason': 'blocked_other',
                'comment': 'qwerty',
            },
        )
        self.assert_response_success(block_response)

        ban = Ban.objects.get(user=self.user)
        self.assertEqual(ban.comment, 'qwerty')

    def test_ban_list(self):
        self.client.post(self.get_block_url(), data={'reason': 'blocked_other'})
        response = self.client.get(self.get_ban_list_url())
        self.assert_response_ok(response)

        bans_data = response.data['results']
        self.assertEqual(len(bans_data), 1)

        ban_data = bans_data[0]
        self.assertEqual(ban_data['reason'], 'blocked_other')
        self.assertIsNotNone(ban_data['started_at'])
        self.assertIsNone(ban_data['finished_at'])
