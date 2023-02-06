import copy
import uuid

from django.conf import settings
from django.urls import reverse
from rest_framework.test import APITransactionTestCase

from cars.core.constants import AppPlatform
from cars.users.models.app_install import AppInstall
from cars.users.models.user import User


class UserSessionTestCase(APITransactionTestCase):

    @property
    def session_url(self):
        return reverse('drive:user-session')

    def setUp(self):
        self.uuid = uuid.uuid4()
        self.device_id = uuid.uuid4()
        self.app_version = '7.7.7'
        self.app_build = '100'
        self.client.credentials(  # pylint: disable=no-member
            HTTP_UUID=str(self.uuid),
            HTTP_DEVICEID=str(self.device_id),
            HTTP_APPVERSION=self.app_version,
            HTTP_APPBUILD=self.app_build,
        )


class AppInstallTestCase(UserSessionTestCase):

    def test_no_arguments(self):
        response = self.client.post(self.session_url)
        self.assertEqual(response.status_code, 400)

    def test_ok(self):
        data = {
            'platform': AppPlatform.IOS.value,
        }
        response = self.client.post(self.session_url, data=data)
        self.assertEqual(response.status_code, 200, response.data)

        app_install = AppInstall.objects.get()
        self.assertEqual(app_install.uuid, self.uuid)
        self.assertEqual(app_install.device_id, self.device_id)
        self.assertEqual(app_install.app_version, self.app_version)
        self.assertEqual(app_install.app_build, self.app_build)
        self.assertTrue(app_install.is_latest)

    def test_ok_android(self):
        data = {
            'platform': AppPlatform.ANDROID.value,
        }
        self.client.post(self.session_url, data=data)
        app_install = AppInstall.objects.get()
        self.assertEqual(app_install.app_name, 'com.yandex.mobile.drive')

    def test_ok_ios(self):
        data = {
            'platform': AppPlatform.IOS.value,
        }
        self.client.post(self.session_url, data=data)
        app_install = AppInstall.objects.get()
        self.assertEqual(app_install.app_name, 'ru.yandex.mobile.drive.inhouse')

    def test_push_token_updated(self):
        data = {
            'platform': AppPlatform.IOS.value,
        }
        response = self.client.post(self.session_url, data=data)

        app_install = AppInstall.objects.get()
        self.assertEqual(app_install.push_token, None)

        data['push_token'] = 'test'
        response = self.client.post(self.session_url, data=data)
        self.assertEqual(response.status_code, 200)

        app_install = AppInstall.objects.get()
        self.assertEqual(app_install.push_token, 'test')

    def test_new_app_install_created(self):
        data = {
            'platform': AppPlatform.IOS.value,
        }
        self.client.post(self.session_url, data=data)

        self.assertEqual(AppInstall.objects.count(), 1)

        self.client.credentials(  # pylint: disable=no-member
            HTTP_UUID=str(uuid.uuid4()),
            HTTP_DEVICEID=str(self.device_id),
        )
        self.client.post(self.session_url, data=data)

        self.assertEqual(AppInstall.objects.count(), 2)
        self.assertEqual(AppInstall.objects.filter(is_latest=True).count(), 1)

    def test_old_app_install_marked_latest(self):
        data = {
            'platform': AppPlatform.IOS.value,
        }
        self.client.post(self.session_url, data=data)
        old_app_install = AppInstall.objects.get()

        self.client.credentials(  # pylint: disable=no-member
            HTTP_UUID=str(uuid.uuid4()),
            HTTP_DEVICEID=str(self.device_id),
        )
        self.client.post(self.session_url, data=data)

        self.assertEqual(AppInstall.objects.count(), 2)
        self.assertFalse(AppInstall.objects.get(id=old_app_install.id).is_latest)

        self.client.credentials(  # pylint: disable=no-member
            HTTP_UUID=str(self.uuid),
            HTTP_DEVICEID=str(self.device_id),
        )
        self.client.post(self.session_url, data=data)
        self.assertTrue(AppInstall.objects.get(id=old_app_install.id).is_latest)


class UserProfileTestCase(UserSessionTestCase):

    def test_yandexoid(self):
        test_yandexoid_spec = copy.deepcopy(settings.YAUTH_TEST_USER)
        test_yandexoid_spec['is_yandexoid'] = True
        login = test_yandexoid_spec['login']
        with self.settings(YAUTH_TEST_USER=test_yandexoid_spec):
            self.client.post(
                self.session_url,
                data={
                    'platform': AppPlatform.IOS.value,
                },
            )
        self.assertTrue(User.objects.get(username=login).is_yandexoid)

    def test_not_yandexoid(self):
        test_yandexoid_spec = copy.deepcopy(settings.YAUTH_TEST_USER)
        test_yandexoid_spec['is_yandexoid'] = False
        login = test_yandexoid_spec['login']
        with self.settings(YAUTH_TEST_USER=test_yandexoid_spec):
            self.client.post(
                self.session_url,
                data={
                    'platform': AppPlatform.IOS.value,
                },
            )
        self.assertFalse(User.objects.get(username=login).is_yandexoid)
