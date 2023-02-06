from django.urls import reverse
from django.utils import timezone

from cars.users.core.user_profile_updater import UserProfileUpdater
from cars.users.factories.user import UserFactory
from cars.users.models.registration_state import RegistrationState
from cars.users.models.user import User
from cars.users.models.user_documents import UserDocumentPhoto
from .base import AdminAPITestCase


class UserRegistrationTestCase(AdminAPITestCase):

    def get_approve_url(self, user):
        return reverse('cars-admin:user-registration-approve', kwargs={'user_id': user.id})

    def get_approve_rejected_url(self, user):
        return reverse('cars-admin:user-registration-approve-rejected', kwargs={'user_id': user.id})

    def get_reject_url(self, user):
        return reverse('cars-admin:user-registration-reject', kwargs={'user_id': user.id})

    def assert_user_status_equal(self, user, status):
        self.assertEqual(User.objects.get(id=user.id).status, status.value)


class RegistrationApproveTestCase(UserRegistrationTestCase):

    def setUp(self):
        super().setUp()
        UserProfileUpdater(self.user).update_status(User.Status.ONBOARDING)
        RegistrationState.objects.create(
            user=self.user,
            chat_completed_at=timezone.now(),
        )

    def test_approve_ok(self):
        url = self.get_approve_url(self.user)
        response = self.client.post(url)
        self.assertEqual(response.status_code, 200, response.data)
        self.assertEqual(response.data['status'], User.Status.ACTIVE.value)
        self.assert_user_status_equal(self.user, User.Status.ACTIVE)

    def test_approve_twice_not_ok(self):
        url = self.get_approve_url(self.user)
        self.client.post(url)
        response = self.client.post(url)
        self.assertEqual(response.status_code, 400, response.data)
        self.assert_user_status_equal(self.user, User.Status.ACTIVE)

class RegistrationApproveRejectedTestCase(UserRegistrationTestCase):

    def setUp(self):
        super().setUp()
        UserProfileUpdater(self.user).update_status(User.Status.BAD_AGE)
        RegistrationState.objects.create(
            user=self.user,
            chat_completed_at=timezone.now(),
        )

    def test_approve_rejected_ok(self):
        url = self.get_approve_rejected_url(self.user)
        response = self.client.post(url)
        self.assertEqual(response.status_code, 200, response.data)
        self.assertEqual(response.data['status'], User.Status.ACTIVE.value)
        self.assert_user_status_equal(self.user, User.Status.ACTIVE)

    def test_approve_rejected_not_really_rejected(self):
        self.user.status = User.Status.ONBOARDING.value
        self.user.save()
        url = self.get_approve_rejected_url(self.user)
        response = self.client.post(url)
        self.assertEqual(response.status_code, 400, response.data)
        self.assert_user_status_equal(self.user, User.Status.ONBOARDING)


class RegistrationRejectTestCase(UserRegistrationTestCase):

    def setUp(self):
        super().setUp()
        UserProfileUpdater(self.user).update_status(User.Status.SCREENING)
        RegistrationState.objects.create(
            user=self.user,
            chat_completed_at=timezone.now(),
        )

    def test_reject_ok(self):
        url = self.get_reject_url(self.user)
        response = self.client.post(url)
        self.assertEqual(response.status_code, 200, response.data)
        self.assertEqual(response.data['status'], User.Status.REJECTED.value)
        self.assert_user_status_equal(self.user, User.Status.REJECTED)

    def test_reject_already_registered(self):
        self.user.status = User.Status.BAD_AGE.value
        self.user.save()
        url = self.get_reject_url(self.user)
        response = self.client.post(url)
        self.assertEqual(response.status_code, 400, response.data)
        self.assert_user_status_equal(self.user, User.Status.BAD_AGE)


class RegistrationResubmitPhotosTestCase(AdminAPITestCase):

    def setUp(self):
        super().setUp()
        UserProfileUpdater(self.user).update_status(User.Status.ONBOARDING)
        RegistrationState.objects.create(
            user=self.user,
            chat_completed_at=timezone.now(),
        )

    def get_url(self, user):
        return reverse('cars-admin:user-registration-resubmit-photos', kwargs={'user_id': user.id})

    def test_resubmit_ok(self):
        url = self.get_url(self.user)
        data = {
            'photo_types': [UserDocumentPhoto.Type.DRIVER_LICENSE_BACK.value],
        }
        response = self.client.post(url, data=data)
        self.assertEqual(response.status_code, 200)

    def test_resubmit_twice(self):
        url = self.get_url(self.user)
        data = {
            'photo_types': [UserDocumentPhoto.Type.DRIVER_LICENSE_BACK.value],
        }

        response = self.client.post(url, data=data)
        self.assertEqual(response.status_code, 200)

        response = self.client.post(url, data=data)
        self.assertEqual(response.status_code, 400)

    def test_resubmit_for_incomplete_chat(self):
        self.user.get_registration_state().delete()
        url = self.get_url(self.user)
        data = {
            'photo_types': [UserDocumentPhoto.Type.DRIVER_LICENSE_BACK.value],
        }
        response = self.client.post(url, data=data)
        self.assertEqual(response.status_code, 400)
