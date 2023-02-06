from unittest.mock import MagicMock

from django.test import TestCase
from django.utils import timezone

from cars.core.datasync import StubDataSyncClient
from cars.registration.core.chat.manager import ChatManager
from cars.registration.core.registration_manager import RegistrationManager
from cars.registration.models.chat_action_result import RegistrationChatActionResult
from cars.registration_yang.tests.helpers.assignment import YangAssignmentTestHelper
from cars.users.core.datasync import DataSyncDocumentsClient
from cars.users.core.user_profile_updater import UserProfileUpdater
from cars.users.factories.user import UserFactory
from cars.users.models.registration_state import RegistrationState
from cars.users.models.user import User
from ..core.ban_manager import BanManager
from ..models.ban import Ban


class BaseBanManagerTestCase(TestCase):

    def setUp(self):
        self.user = UserFactory.create(status=User.Status.ACTIVE.value)
        RegistrationState.objects.create(
            user=self.user,
            chat_completed_at=timezone.now(),
        )

        self.operator = UserFactory.create()

        self.datasync = DataSyncDocumentsClient(
            datasync_client=StubDataSyncClient(),
        )

        self.chat_manager = ChatManager.from_settings()
        self.ban_manager = BanManager(
            chat_manager=self.chat_manager,
            datasync_client=self.datasync,
        )

    def assert_user_status_equal(self, status, user=None):
        if user is None:
            user = self.user
        user.refresh_from_db()
        self.assertIs(user.get_status(), status)

    def get_chat_state(self, user=None):
        if user is None:
            user = self.user
        chat_state = self.chat_manager.get_chat_state(user=user, allow_initialize=False)
        return chat_state

    def submit_chat_action(self, chat_action_id, data):
        return self.chat_manager.submit_chat_action(
            user=self.user,
            chat_action_id=chat_action_id,
            data=data,
        )

    def block_and_check_chat(self, reason, expect_action=False):
        self.ban_manager.block(
            user=self.user,
            operator=self.operator,
            reason=reason,
        )
        self.assert_user_status_equal(User.Status.BLOCKED)

        chat_state = self.get_chat_state()
        self.assertGreater(len(chat_state.new_messages), 0)

        if expect_action:
            self.assertIsNotNone(chat_state.action)
        else:
            self.assertIsNone(chat_state.action)

        return chat_state


class GenericBanManagerTestCase(BaseBanManagerTestCase):

    def test_block_driving_ban(self):
        chat_state = self.block_and_check_chat(reason=Ban.Reason.DRIVING_BAN)
        self.assertIn('ГИБДД', chat_state.new_messages[0].content['text'])

    def test_block_duplicate_license(self):
        chat_state = self.block_and_check_chat(reason=Ban.Reason.DUPLICATE_LICENSE)
        self.assertIn('удостоверен', chat_state.new_messages[0].content['text'])

    def test_block_duplicate_passport(self):
        chat_state = self.block_and_check_chat(reason=Ban.Reason.DUPLICATE_PASSPORT)
        self.assertIn('паспорт', chat_state.new_messages[0].content['text'])

    def test_block_speed_asshole(self):
        chat_state = self.block_and_check_chat(reason=Ban.Reason.SPEED_ASSHOLE)
        self.assertIn('скорость', chat_state.new_messages[0].content['text'])

    def test_block_too_old(self):
        chat_state = self.block_and_check_chat(reason=Ban.Reason.TOO_OLD)
        self.assertIn('безопасност', chat_state.new_messages[0].content['text'])

    def test_block_other_reason(self):
        chat_state = self.block_and_check_chat(reason=Ban.Reason.OTHER)
        self.assertIn('безопасност', chat_state.new_messages[0].content['text'])

        first_message = chat_state.new_messages[0]
        self.assertIn(self.user.first_name, first_message.content['text'])

        last_message = chat_state.new_messages[-1]
        self.assertIsNotNone(last_message.on_tap)
        self.assertTrue(last_message.on_tap['content'].get('debug_params'))

    def test_block_already_blocked_without_ban(self):
        UserProfileUpdater(self.user).update_status(User.Status.BLOCKED)

        self.ban_manager.block(
            user=self.user,
            operator=self.operator,
            reason=Ban.Reason.OTHER,
        )

        self.assert_user_status_equal(User.Status.BLOCKED)

        chat_state = self.get_chat_state()
        self.assertIsNone(chat_state.action)
        self.assertGreater(len(chat_state.new_messages), 0)

        first_message = chat_state.new_messages[0]
        self.assertIn(self.user.first_name, first_message.content['text'])

        last_message = chat_state.new_messages[-1]
        self.assertIsNotNone(last_message.on_tap)
        self.assertTrue(last_message.on_tap['content'].get('debug_params'))

    def test_block_already_blocked_without_ban_twice(self):
        UserProfileUpdater(self.user).update_status(User.Status.BLOCKED)

        self.ban_manager.block(
            user=self.user,
            operator=self.operator,
            reason=Ban.Reason.OTHER,
        )
        n_messages_before = len(self.get_chat_state().new_messages)

        self.ban_manager.block(
            user=self.user,
            operator=self.operator,
            reason=Ban.Reason.OTHER,
        )
        n_messages_after = len(self.get_chat_state().new_messages)

        self.assertEqual(n_messages_before, n_messages_after)

    def test_chat_cleared_before_block(self):
        chat_state_1 = self.block_and_check_chat(reason=Ban.Reason.DUPLICATE_LICENSE)
        self.assertIn('удостоверен', chat_state_1.new_messages[0].content['text'])

        self.ban_manager.unblock(user=self.user, operator=None)

        chat_state_2 = self.block_and_check_chat(reason=Ban.Reason.DUPLICATE_PASSPORT)
        self.assertIn('паспорт', chat_state_2.new_messages[0].content['text'])


class OldLicenseBanManagerTestCase(BaseBanManagerTestCase):

    def setUp(self):
        super().setUp()
        self.registration_manager = RegistrationManager(
            chat_manager=self.chat_manager,
            datasync_client=self.datasync,
            pusher=MagicMock(),
        )
        self.yah = YangAssignmentTestHelper(
            user=self.user,
            datasync_client=self.datasync,
        )

    def resubmit_license(self):
        chat_state = self.get_chat_state()
        self.assertIs(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.DRIVER_LICENSE_FRONT,
        )

        data = {
            'content': 'abcd',
            'scanners': [],
        }
        self.submit_chat_action(chat_action_id=chat_state.action.id, data=data)

        chat_state = self.get_chat_state()
        self.assertIsNotNone(chat_state.action)
        self.assertIs(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.DRIVER_LICENSE_BACK,
        )

        self.submit_chat_action(chat_action_id=chat_state.action.id, data=data)

        chat_state = self.get_chat_state()
        self.assertIsNone(chat_state.action)
        self.assertIn('несколько часов', chat_state.new_messages[-1].content['text'])

    def test_chat(self):
        chat_state = self.block_and_check_chat(
            reason=Ban.Reason.OLD_LICENSE,
            expect_action=True,
        )
        self.assertIn(self.user.first_name, chat_state.new_messages[0].content['text'])
        self.resubmit_license()

    def test_resubmit_new_license_and_unblock(self):
        self.block_and_check_chat(
            reason=Ban.Reason.OLD_LICENSE,
            expect_action=True,
        )
        self.resubmit_license()
        yang_assignment = self.yah.create_processed_yang_assignment(
            verified_at=timezone.now(),
            license_data_override={
                'categories_b_valid_from_date': None,
                'categories_b_valid_to_date': '2020-01-01T00:00:00.000Z',
            },
        )
        self.registration_manager.ingest_yang_assignment(yang_assignment)

        self.assert_user_status_equal(User.Status.BLOCKED)
        self.ban_manager.try_unblock_all()
        self.assert_user_status_equal(User.Status.ACTIVE)

        chat_state = self.get_chat_state()
        self.assertIn('обновили данные', chat_state.new_messages[-1].content['text'])

    def test_resubmit_old_license_and_unblock(self):
        self.block_and_check_chat(
            reason=Ban.Reason.OLD_LICENSE,
            expect_action=True,
        )
        self.resubmit_license()
        yang_assignment = self.yah.create_processed_yang_assignment(
            verified_at=timezone.now(),
            license_data_override={
                'categories_b_valid_from_date': None,
                'categories_b_valid_to_date': '2000-01-01T00:00:00.000Z',
            },
        )
        self.registration_manager.ingest_yang_assignment(yang_assignment)

        self.assert_user_status_equal(User.Status.BLOCKED)
        self.ban_manager.try_unblock_all()
        self.assert_user_status_equal(User.Status.BLOCKED)

        chat_state = self.get_chat_state()
        self.assertNotIn('обновили данные', chat_state.new_messages[-1].content['text'])
