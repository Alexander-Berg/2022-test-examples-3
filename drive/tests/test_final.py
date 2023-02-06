from django.test import TestCase

from cars.core.constants import AppPlatform
from cars.users.core.user_profile_updater import UserProfileUpdater
from cars.users.factories.user import UserFactory
from cars.users.models.registration_state import RegistrationState
from cars.users.models.app_install import AppInstall
from cars.users.models.user import User
from ..core.chat.actions import BaseChatAction
from ..core.chat.manager import ChatManager


class FinalTestCase(TestCase):

    script = '''
---
flow: []
action_groups: []
message_groups:
  - id: app_update_android
    chat_items:
      - type: text_block
        params:
          messages:
            - "Android text"
      - type: image
        params:
          url: "https://carsharing.s3.yandex.net/drive/registration-chat/app-update-gplay.png"
          on_tap:
            type: open_url
            content:
              url: "https://ya.ru"
  - id: app_update_ios
    chat_items:
      - type: text_block
        params:
          messages:
            - "iOS text"
      - type: image
        params:
          url: "https://carsharing.s3.yandex.net/drive/registration-chat/app-update-appstore.png"
          on_tap:
            type: open_url
            content:
              url: "https://ya.ru"
'''

    def setUp(self):
        self.user = UserFactory.create(status=User.Status.ONBOARDING.value)
        RegistrationState.objects.create(user=self.user)
        self.chat_manager = ChatManager.from_yaml(self.script)
        self.chat_session = self.chat_manager.make_session(user=self.user)
        self.chat_session.get_chat_state()

    def test_nothing_for_onboarding_user(self):
        state = self.chat_session.get_chat_state()
        self.assertIsNone(state.action)

    def test_enter_app_for_active_user_with_old_app_android(self):
        UserProfileUpdater(self.user).update_status(User.Status.ACTIVE)

        ai = AppInstall.objects.get(user=self.user, is_latest=True)
        ai.app_version = None
        ai.app_build = None
        ai.platform = AppPlatform.ANDROID.value
        ai.save()

        state = self.chat_session.get_chat_state()
        self.assertIsNone(state.action)
        self.assertEqual(len(state.new_messages), 2)
        self.assertIn('Android', state.new_messages[0].content['text'])

    def test_enter_app_for_active_user_with_old_app_ios(self):
        UserProfileUpdater(self.user).update_status(User.Status.ACTIVE)

        ai = AppInstall.objects.get(user=self.user, is_latest=True)
        ai.app_version = None
        ai.app_build = None
        ai.platform = AppPlatform.IOS.value
        ai.save()

        state = self.chat_session.get_chat_state()
        self.assertIsNone(state.action)
        self.assertEqual(len(state.new_messages), 2)
        self.assertIn('iOS', state.new_messages[0].content['text'])

    def test_enter_app_for_active_user_with_old_app_twice(self):
        UserProfileUpdater(self.user).update_status(User.Status.ACTIVE)

        ai = AppInstall.objects.get(user=self.user, is_latest=True)
        ai.app_version = None
        ai.app_build = None
        ai.platform = AppPlatform.IOS.value
        ai.save()

        state = self.chat_session.get_chat_state()
        self.assertEqual(len(state.new_messages), 2)

        state = self.chat_session.get_chat_state()
        self.assertEqual(len(state.new_messages), 2)

    def test_enter_app_for_active_user_with_new_app(self):
        UserProfileUpdater(self.user).update_status(User.Status.ACTIVE)
        state = self.chat_session.get_chat_state()
        self.assertIsNotNone(state.action)
        self.assertIs(state.action.get_type(), BaseChatAction.Type.ENTER_APP)
