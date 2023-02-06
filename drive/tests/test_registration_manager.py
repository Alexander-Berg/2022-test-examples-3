# pylint: disable=too-many-lines
import unittest.mock

from django.test import TestCase
from django.utils import timezone

from cars.core.datasync import StubDataSyncClient
from cars.registration.models.chat_action_result import RegistrationChatActionResult
from cars.registration.models.chat_message import RegistrationChatMessage
from cars.registration_yang.models.assignment import YangAssignment
from cars.users.core.datasync import DataSyncDocumentsClient
from cars.users.factories.user import UserFactory
from cars.users.models.registration_state import RegistrationState
from cars.users.models.user import User
from cars.users.models.user_documents import UserDocument, UserDocumentPhoto
from cars.registration_yang.tests.helpers.assignment import YangAssignmentTestHelper
from ..core.chat.manager import ChatManager
from ..core.registration_manager import RegistrationManager


class BaseRegistrationManagerTestCase(TestCase):
    chat_script = '''
---
flow: []

action_groups:
  - id: resubmit_only_license_back
    pre_action:
      - type: text
        params:
          message: "Нужно еще раз сфотографировать оборотную сторону водительского удостоверения."
    action:
      - type: license_back
        params:
          text: "Включить камеру"
    post_action:
      - type: text
        params:
          message: "Спасибо. Я отправил фотографию на проверку."

  - id: resubmit_only_license_front
    pre_action:
      - type: text
        params:
          message: "Нужно еще раз сфотографировать лицевую сторону водительского удостоверения."
    action:
      - type: license_front
        params:
          text: "Включить камеру"
    post_action:
      - type: text
        params:
          message: "Спасибо. Я отправил фотографию на проверку."

  - id: resubmit_only_passport_bio
    pre_action:
      - type: text
        params:
          message: "Нужно еще раз снять разворот паспорта с вашим фото."
    action:
      - type: passport_bio
        params:
          text: "Включить камеру"
    post_action:
      - type: text
        params:
          message: "Спасибо. Я отправил фотографию на проверку."

  - id: resubmit_only_passport_reg
    pre_action:
      - type: text
        params:
          message: "Нужно еще раз сфотографировать разворот паспорта с регистрацией."
    action:
      - type: passport_reg
        params:
          text: "Включить камеру"
    post_action:
      - type: text
        params:
          message: "Спасибо. Я отправил фотографию на проверку."

  - id: resubmit_only_passport_selfie
    pre_action:
      - type: text
        params:
          message: "Нужно еще раз сделать селфи с паспортом."
    action:
      - type: passport_selfie
        params:
          text: "Включить камеру"
    post_action:
      - type: text
        params:
          message: "Спасибо. Я отправил фотографию на проверку."

  - id: resubmit_2_from_license_back
    pre_action:
      - type: text_block
        params:
          messages:
            - "{resubmit_two_photos_message}"
            - "Сначала оборотная сторона водительского удостоверения."
    action:
      - type: license_back
        params:
          text: "Включить камеру"
    post_action: []

  - id: resubmit_2_from_license_front
    pre_action:
      - type: text_block
        params:
          messages:
            - "{resubmit_two_photos_message}"
            - "Сначала лицевая сторона водительского удостоверения."
    action:
      - type: license_front
        params:
          text: "Включить камеру"
    post_action: []

  - id: resubmit_2_from_passport_bio
    pre_action:
      - type: text_block
        params:
          messages:
            - "{resubmit_two_photos_message}"
            - "Сначала разворот паспорта с вашим фото."
    action:
      - type: passport_bio
        params:
          text: "Включить камеру"
    post_action: []

  - id: resubmit_2_from_passport_reg
    pre_action:
      - type: text_block
        params:
          messages:
            - "{resubmit_two_photos_message}"
            - "Сначала разворот паспорта с регистрацией."
    action:
      - type: passport_reg
        params:
          text: "Включить камеру"
    post_action: []

  - id: resubmit_2_from_passport_selfie
    pre_action:
      - type: text_block
        params:
          messages:
            - "{resubmit_two_photos_message}"
            - "Сначала селфи с паспортом."
    action:
      - type: passport_selfie
        params:
          text: "Включить камеру"
    post_action: []

  - id: cont_resubmit_2p_license_back
    pre_action:
      - type: text
        params:
          message: "Теперь оборотная сторона водительского удостоверения."
    action:
      - type: license_back
        params:
          text: "Включить камеру"
    post_action:
      - type: text
        params:
          message: "Спасибо. Я отправил фотографии на проверку."
        when: cont_resubmit_2p_photos_final

  - id: cont_resubmit_2p_license_front
    pre_action:
      - type: text
        params:
          message: "Теперь лицевая сторона водительского удостоверения."
    action:
      - type: license_front
        params:
          text: "Включить камеру"
    post_action:
      - type: text
        params:
          message: "Спасибо. Я отправил фотографии на проверку."
        when: cont_resubmit_2p_photos_final

  - id: cont_resubmit_2p_passport_bio
    pre_action:
      - type: text
        params:
          message: "Теперь разворот паспорта с вашим фото."
    action:
      - type: passport_bio
        params:
          text: "Включить камеру"
    post_action:
      - type: text
        params:
          message: "Спасибо. Я отправил фотографии на проверку."
        when: cont_resubmit_2p_photos_final

  - id: cont_resubmit_2p_passport_reg
    pre_action:
      - type: text
        params:
          message: "Теперь разворот паспорта с регистрацией."
    action:
      - type: passport_reg
        params:
          text: "Включить камеру"
    post_action:
      - type: text
        params:
          message: "Спасибо. Я отправил фотографии на проверку."
        when: cont_resubmit_2p_photos_final

  - id: cont_resubmit_2p_passport_selfie
    pre_action:
      - type: text
        params:
          message: "Теперь селфи с паспортом."
    action:
      - type: passport_selfie
        params:
          text: "Включить камеру"
    post_action:
      - type: text
        params:
          message: "Спасибо. Я отправил фотографии на проверку."
        when: cont_resubmit_2p_photos_final

message_groups:

  - id: finish_ok
    chat_items:
      - type: text_block
        params:
          messages:
            - "{user.first_name} ok"
        when: user.first_name
      - type: text_block
        params:
          messages:
            - "ok"
        when:
          not: user.first_name

  - id: finish_ok_sorry
    chat_items:
      - type: text_block
        params:
          messages:
            - "{user.first_name} ok sorry"

  - id: finish_bad_age
    chat_items:
      - type: text_block
        params:
          messages:
            - "{user.first_name} bad age."

  - id: finish_bad_driving_experience
    chat_items:
      - type: text_block
        params:
          messages:
            - "{user.first_name} bad driving experience."
            - "Try again in {time_to_ok_driving_experience}."

  - id: finish_bad_no_reason
    chat_items:
      - type: text_block
        params:
          messages:
            - "no"
      - type: image
        params:
          url: "https://carsharing.s3.yandex.net/drive/registration-chat/email-preview.png"
          on_tap:
            type: mailto
            content:
              to: "drive@support.yandex.ru"
'''

    def setUp(self):
        self.user = UserFactory.create(status=User.Status.ONBOARDING.value)
        RegistrationState.objects.create(
            user=self.user,
            chat_completed_at=timezone.now(),
        )

        self.chat_manager = ChatManager.from_yaml(self.chat_script)
        self.chat_session = self.chat_manager.make_session(self.user)

        self.low_level_datasync = StubDataSyncClient()
        self.datasync = DataSyncDocumentsClient(
            datasync_client=self.low_level_datasync,
        )
        self.pusher = unittest.mock.MagicMock()
        self.manager = RegistrationManager(
            chat_manager=self.chat_manager,
            datasync_client=self.datasync,
            pusher=self.pusher,
        )

        self.yah = YangAssignmentTestHelper(
            user=self.user,
            datasync_client=self.datasync,
        )

    def assert_user_status_equal(self, user, status):
        self.assertEqual(User.objects.get(id=user.id).status, status.value)

    def assert_user_registration_date_present(self, user):
        self.assertIsNotNone(User.objects.get(id=user.id).registered_at)


class RegistrationManagerYangAssignmentsTestCase(BaseRegistrationManagerTestCase):

    def test_yang_assignment_user_data_updated(self):
        assignment = self.yah.create_processed_yang_assignment(
            passport_data_override={
                'first_name': 'ТЕСТ1',
                'last_name': 'ТЕСТ2',
                'middle_name': 'ТЕСТ3',
            },
            license_data_override={
                'first_name': 'ТЕСТ1',
                'last_name': 'ТЕСТ2',
                'middle_name': 'ТЕСТ3',
            },
        )
        self.manager.ingest_yang_assignment(assignment)
        user = User.objects.get(id=self.user.id)
        self.assertEqual(user.first_name, 'Тест1')
        self.assertEqual(user.last_name, 'Тест2')
        self.assertEqual(user.patronymic_name, 'Тест3')
        self.assertIsNotNone(
            UserDocument.objects
                .get(user=user, type=UserDocument.Type.PASSPORT.value)
                .natural_id_fingerprint
        )

    def test_yang_assignment_user_data_updated_for_active_user(self):
        assignment = self.yah.create_processed_yang_assignment(
            passport_data_override={
                'first_name': 'ТЕСТ11',
                'last_name': 'ТЕСТ21',
                'middle_name': 'ТЕСТ31',
            },
            license_data_override={
                'first_name': 'ТЕСТ11',
                'last_name': 'ТЕСТ21',
                'middle_name': 'ТЕСТ31',
            },
        )
        self.manager.ingest_yang_assignment(assignment)
        user = User.objects.get(id=self.user.id)
        self.assertEqual(user.first_name, 'Тест11')
        self.assertEqual(user.last_name, 'Тест21')
        self.assertEqual(user.patronymic_name, 'Тест31')

        self.assert_user_status_equal(user=self.user, status=User.Status.ACTIVE)

        assignment = self.yah.create_processed_yang_assignment(
            passport_data_override={
                'first_name': 'ТЕСТ12',
                'last_name': 'ТЕСТ22',
                'middle_name': 'ТЕСТ32',
            },
            license_data_override={
                'first_name': 'ТЕСТ12',
                'last_name': 'ТЕСТ22',
                'middle_name': 'ТЕСТ32',
            },
        )
        self.manager.ingest_yang_assignment(assignment)
        user = User.objects.get(id=self.user.id)
        self.assertEqual(user.first_name, 'Тест12')
        self.assertEqual(user.last_name, 'Тест22')
        self.assertEqual(user.patronymic_name, 'Тест32')

        self.assert_user_status_equal(user=self.user, status=User.Status.ACTIVE)

    def test_ok(self):
        assignment = self.yah.create_processed_yang_assignment()
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.ACTIVE)
        self.assert_user_registration_date_present(self.user)

        chat_messages = self.chat_session.get_chat_state().new_messages
        self.assertEqual(len(chat_messages), 1)
        message = chat_messages[0]
        self.assertIn('ok', message.content['text'])
        self.assertIn(
            User.objects.get(id=self.user.id).first_name,
            message.content['text'],
        )
        self.assertIsNotNone(YangAssignment.objects.get(id=assignment.id).ingested_at)
        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})

    def test_ok_no_name(self):
        assignment = self.yah.create_processed_yang_assignment(
            passport_data_override={
                'first_name': '',
            },
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.ACTIVE)
        self.assert_user_registration_date_present(self.user)

        chat_messages = self.chat_session.get_chat_state().new_messages
        self.assertEqual(len(chat_messages), 1)
        message = chat_messages[0]
        self.assertEqual('Имя ok', message.content['text'])  # will be fetched from docs now
        self.assertIn(
            User.objects.get(id=self.user.id).first_name,
            message.content['text'],
        )
        self.assertIsNotNone(YangAssignment.objects.get(id=assignment.id).ingested_at)
        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})

    def test_bad_age(self):
        assignment = self.yah.create_processed_yang_assignment(
            passport_data_override={
                'birth_date': '2000-01-01T00:00:00.000Z',
            },
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.SCREENING)

        chat_messages = self.chat_session.get_chat_state().new_messages
        self.assertEqual(len(chat_messages), 0)
        self.assertFalse(self.pusher.send.called)

    def test_ok_old_driver_license(self):
        assignment = self.yah.create_processed_yang_assignment(
            license_data={
                'last_name': 'ФАМИЛИЯ',
                'number': '1234567890',
                'issue_date': '2010-01-01T00:00:00.000Z',
                'first_name': 'ИМЯ',
                'birth_date': '1992-11-27T00:00:00.000Z',
                'middle_name': 'ОТЧЕСТВО',
                'categories': 'B',
                'id': 'carsharing'
            }
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.ACTIVE)
        self.assert_user_registration_date_present(self.user)

        chat_messages = self.chat_session.get_chat_state().new_messages
        self.assertEqual(len(chat_messages), 1)
        message = chat_messages[0]
        self.assertIn('ok', message.content['text'])
        self.assertIn(
            User.objects.get(id=self.user.id).first_name,
            message.content['text'],
        )
        self.assertIsNotNone(YangAssignment.objects.get(id=assignment.id).ingested_at)
        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})

    def test_ok_missing_category_b_start_date(self):
        assignment = self.yah.create_processed_yang_assignment(
            license_data_override={
                'categories_b_valid_from_date': None,
                'categories_b_valid_to_date': '2020-01-01T00:00:00.000Z',
            },
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.ACTIVE)
        self.assert_user_registration_date_present(self.user)

        chat_messages = self.chat_session.get_chat_state().new_messages
        self.assertEqual(len(chat_messages), 1)
        message = chat_messages[0]
        self.assertIn('ok', message.content['text'])
        self.assertIn(
            User.objects.get(id=self.user.id).first_name,
            message.content['text'],
        )
        self.assertIsNotNone(YangAssignment.objects.get(id=assignment.id).ingested_at)
        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})

    def test_ok_missing_category_b_span(self):
        assignment = self.yah.create_processed_yang_assignment(
            license_data_override={
                'issue_date': '2010-01-01T00:00:00.000Z',
                'categories_b_valid_from_date': None,
                'categories_b_valid_to_date': None,
            },
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.ACTIVE)
        self.assert_user_registration_date_present(self.user)

        chat_messages = self.chat_session.get_chat_state().new_messages
        self.assertEqual(len(chat_messages), 1)
        message = chat_messages[0]
        self.assertIn('ok', message.content['text'])
        self.assertIn(
            User.objects.get(id=self.user.id).first_name,
            message.content['text'],
        )
        self.assertIsNotNone(YangAssignment.objects.get(id=assignment.id).ingested_at)
        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})

    def test_expired_driver_license(self):
        assignment = self.yah.create_processed_yang_assignment(
            license_data_override={
                'categories_b_valid_from_date': '2000-01-01T00:00:00.000Z',
                'categories_b_valid_to_date': '2010-01-01T00:00:00.000Z',
            },
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.SCREENING)

        chat_messages = self.chat_session.get_chat_state().new_messages
        self.assertEqual(len(chat_messages), 0)
        self.assertFalse(self.pusher.send.called)

    def test_invalid_category_old_driver_license(self):
        assignment = self.yah.create_processed_yang_assignment(
            license_data={
                'last_name': 'ФАМИЛИЯ',
                'number': '1234567890',
                'issue_date': '2000-01-01T00:00:00.000Z',
                'first_name': 'ИМЯ',
                'birth_date': '1992-11-27T00:00:00.000Z',
                'middle_name': 'ОТЧЕСТВО',
                'categories': 'A',
                'id': 'carsharing'
            }
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.SCREENING)

        chat_messages = self.chat_session.get_chat_state().new_messages
        self.assertEqual(len(chat_messages), 0)
        self.assertFalse(self.pusher.send.called)

    def test_bad_driving_experience(self):
        assignment = self.yah.create_processed_yang_assignment(
            license_data_override={
                'categories_b_valid_from_date': '2018-01-01T00:00:00.000Z',
                'experience_from': '2018-01-01T00:00:00.000Z',
            },
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.SCREENING)

        chat_messages = self.chat_session.get_chat_state().new_messages
        self.assertEqual(len(chat_messages), 0)
        self.assertFalse(self.pusher.send.called)

    def test_fraud(self):
        assignment = self.yah.create_processed_yang_assignment(
            fraud_status=YangAssignment.Status.DEFINITELY_FRAUD,
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.SCREENING)

        chat_messages = self.chat_session.get_chat_state().new_messages
        self.assertEqual(len(chat_messages), 0)
        self.assertFalse(self.pusher.send.called)

    def test_maybe_fraud(self):
        assignment = self.yah.create_processed_yang_assignment(
            fraud_status=YangAssignment.Status.MAYBE_FRAUD,
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.SCREENING)

        chat_messages = self.chat_session.get_chat_state().new_messages
        self.assertEqual(len(chat_messages), 0)
        self.assertFalse(self.pusher.send.called)

    def test_foreign(self):
        foreign_status = UserDocumentPhoto.VerificationStatus.FOREIGN.value
        assignment = self.yah.create_processed_yang_assignment(
            passport_biographical__verification_status=foreign_status,
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.SCREENING)

        chat_messages = self.chat_session.get_chat_state().new_messages
        self.assertEqual(len(chat_messages), 0)
        self.assertFalse(self.pusher.send.called)

    def test_previous_driving_experience_is_accounted(self):
        assignment = self.yah.create_processed_yang_assignment(
            license_data_override={
                'categories_b_valid_from_date': '2018-01-01T00:00:00.000Z',
                'experience_from': '2000-01-01T00:00:00.000Z',
            },
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.ACTIVE)
        self.assert_user_registration_date_present(self.user)

    def test_only_passport_selfie_need_info(self):
        need_info_status = UserDocumentPhoto.VerificationStatus.NEED_INFO.value
        assignment = self.yah.create_processed_yang_assignment(
            passport_selfie__verification_status=need_info_status,
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.ONBOARDING)

        self.assertEqual(
            RegistrationState.objects.get(user=self.user).chat_action_id,
            'resubmit_only_passport_selfie',
        )

        chat_state = self.chat_session.get_chat_state()

        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.PASSPORT_SELFIE,
        )

        self.assertEqual(len(chat_state.new_messages), 1)
        message = chat_state.new_messages[0]
        self.assertIn('селфи с паспортом', message.content['text'])

        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})

        chat_state = self.chat_manager.submit_chat_action(
            user=self.user,
            chat_action_id='resubmit_only_passport_selfie',
            data={
                'content': 'abcd',
            },
        )
        self.assertIsNone(chat_state.action)
        self.assertEqual(len(chat_state.new_messages), 2)
        self.assertIn('отправил фотографию', chat_state.new_messages[-1].content['text'])

        chat_state = self.chat_session.get_chat_state()
        self.assertIsNone(chat_state.action)
        self.assertIn('отправил фотографию', chat_state.new_messages[-1].content['text'])

    def test_only_license_front_unrecognizable(self):
        unrecognizable_status = UserDocumentPhoto.VerificationStatus.UNRECOGNIZABLE.value
        assignment = self.yah.create_processed_yang_assignment(
            license_front__verification_status=unrecognizable_status,
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.ONBOARDING)

        chat_state = self.chat_session.get_chat_state()

        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.DRIVER_LICENSE_FRONT,
        )

        self.assertEqual(len(chat_state.new_messages), 1)
        message = chat_state.new_messages[0]
        self.assertIn(
            'лицевую сторону водительского удостоверения',
            message.content['text'],
        )

        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})

        chat_state = self.chat_manager.submit_chat_action(
            user=self.user,
            chat_action_id='resubmit_only_license_front',
            data={
                'content': 'abcd',
                'scanners': [],
            },
        )
        self.assertIsNone(chat_state.action)
        self.assertEqual(len(chat_state.new_messages), 2)
        self.assertIn('отправил фотографию', chat_state.new_messages[-1].content['text'])

        chat_state = self.chat_session.get_chat_state()
        self.assertIsNone(chat_state.action)
        self.assertIn('отправил фотографию', chat_state.new_messages[-1].content['text'])

    def test_passport_biographical_and_license_back_need_info(self):
        need_info_status = UserDocumentPhoto.VerificationStatus.NEED_INFO.value
        assignment = self.yah.create_processed_yang_assignment(
            passport_biographical__verification_status=need_info_status,
            license_back__verification_status=need_info_status,
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.ONBOARDING)

        chat_state = self.chat_session.get_chat_state()

        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.DRIVER_LICENSE_BACK,
        )
        self.assertEqual(len(chat_state.new_messages), 2)
        self.assertIn(
            'оборотной стороны водительского удостоверения',
            chat_state.new_messages[-2].content['text'],
        )
        self.assertIn(
            'разворота паспорта с вашим фото',
            chat_state.new_messages[-2].content['text'],
        )
        self.assertIn(
            'оборотная сторона водительского удостоверения',
            chat_state.new_messages[-1].content['text'],
        )

        chat_state = self.chat_session.get_chat_state()
        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.DRIVER_LICENSE_BACK,
        )
        self.assertIn(
            'оборотная сторона водительского удостоверения',
            chat_state.new_messages[-1].content['text'],
        )

        chat_state = self.chat_manager.submit_chat_action(
            user=self.user,
            chat_action_id='resubmit_2_from_license_back',
            data={
                'content': 'abcd',
                'scanners': [],
            },
        )

        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.PASSPORT_BIOGRAPHICAL,
        )

        self.assertEqual(len(chat_state.new_messages), 2)
        self.assertIn(
            'разворот паспорта с вашим фото',
            chat_state.new_messages[-1].content['text'],
        )

        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})

        chat_state = self.chat_manager.submit_chat_action(
            user=self.user,
            chat_action_id='cont_resubmit_2p_passport_bio',
            data={
                'content': 'abcd',
                'scanners': [],
            },
        )

        self.assertIsNone(chat_state.action)
        self.assertEqual(len(chat_state.new_messages), 2)
        self.assertEqual(
            chat_state.new_messages[-2].source,
            RegistrationChatMessage.Source.USER.value,
        )
        self.assertIn('отправил фотографии', chat_state.new_messages[-1].content['text'])

        chat_state = self.chat_session.get_chat_state()
        self.assertIn('отправил фотографии', chat_state.new_messages[-1].content['text'])

    def test_all_photos_need_info(self):
        need_info_status = UserDocumentPhoto.VerificationStatus.NEED_INFO.value
        assignment = self.yah.create_processed_yang_assignment(
            license_front__verification_status=need_info_status,
            license_back__verification_status=need_info_status,
            passport_biographical__verification_status=need_info_status,
            passport_registration__verification_status=need_info_status,
            passport_selfie__verification_status=need_info_status,
        )
        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.ONBOARDING)

        chat_state = self.chat_session.get_chat_state()
        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.DRIVER_LICENSE_BACK,
        )

        self.assertEqual(len(chat_state.new_messages), 2)
        message = chat_state.new_messages[0]
        self.assertIn('лицевой стороны водительского удостоверения', message.content['text'])
        self.assertIn('оборотной стороны водительского удостоверения', message.content['text'])
        self.assertIn('разворота паспорта с вашим фото', message.content['text'])
        self.assertIn('разворота паспорта с регистрацией', message.content['text'])
        self.assertIn('селфи с паспортом', message.content['text'])
        self.assertIn(
            'оборотная сторона водительского удостоверения',
            chat_state.new_messages[1].content['text'],
        )

        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})

        chat_state = self.chat_manager.submit_chat_action(
            user=self.user,
            chat_action_id='resubmit_2_from_license_back',
            data={
                'content': 'abcd',
                'scanners': [],
            },
        )
        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.DRIVER_LICENSE_FRONT,
        )
        self.assertEqual(len(chat_state.new_messages), 2)
        self.assertEqual(
            chat_state.new_messages[-2].source,
            RegistrationChatMessage.Source.USER.value,
        )
        self.assertIn(
            'лицевая сторона водительского удостоверения',
            chat_state.new_messages[-1].content['text'],
        )

        chat_state = self.chat_session.get_chat_state()
        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.DRIVER_LICENSE_FRONT,
        )
        self.assertEqual(
            chat_state.new_messages[-2].source,
            RegistrationChatMessage.Source.USER.value,
        )
        self.assertIn(
            'лицевая сторона водительского удостоверения',
            chat_state.new_messages[-1].content['text'],
        )

        chat_state = self.chat_manager.submit_chat_action(
            user=self.user,
            chat_action_id='cont_resubmit_2p_license_front',
            data={
                'content': 'abcd',
                'scanners': [],
            },
        )
        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.PASSPORT_BIOGRAPHICAL,
        )
        self.assertEqual(len(chat_state.new_messages), 2)
        self.assertEqual(
            chat_state.new_messages[-2].source,
            RegistrationChatMessage.Source.USER.value,
        )
        self.assertIn(
            'разворот паспорта с вашим фото',
            chat_state.new_messages[-1].content['text'],
        )

        chat_state = self.chat_session.get_chat_state()
        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.PASSPORT_BIOGRAPHICAL,
        )
        self.assertEqual(
            chat_state.new_messages[-2].source,
            RegistrationChatMessage.Source.USER.value,
        )
        self.assertIn(
            'разворот паспорта с вашим фото',
            chat_state.new_messages[-1].content['text'],
        )

        chat_state = self.chat_manager.submit_chat_action(
            user=self.user,
            chat_action_id='cont_resubmit_2p_passport_bio',
            data={
                'content': 'abcd',
                'scanners': [],
            },
        )
        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.PASSPORT_REGISTRATION,
        )
        self.assertEqual(len(chat_state.new_messages), 2)
        self.assertEqual(
            chat_state.new_messages[-2].source,
            RegistrationChatMessage.Source.USER.value,
        )
        self.assertIn(
            'разворот паспорта с регистрацией',
            chat_state.new_messages[-1].content['text'],
        )

        chat_state = self.chat_session.get_chat_state()
        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.PASSPORT_REGISTRATION,
        )
        self.assertEqual(
            chat_state.new_messages[-2].source,
            RegistrationChatMessage.Source.USER.value,
        )
        self.assertIn(
            'разворот паспорта с регистрацией',
            chat_state.new_messages[-1].content['text'],
        )

        chat_state = self.chat_manager.submit_chat_action(
            user=self.user,
            chat_action_id='cont_resubmit_2p_passport_reg',
            data={
                'content': 'abcd',
                'scanners': [],
            },
        )
        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.PASSPORT_SELFIE,
        )
        self.assertEqual(len(chat_state.new_messages), 2)
        self.assertEqual(
            chat_state.new_messages[-2].source,
            RegistrationChatMessage.Source.USER.value,
        )
        self.assertIn('селфи с паспортом', chat_state.new_messages[-1].content['text'])

        chat_state = self.chat_session.get_chat_state()
        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.PASSPORT_SELFIE,
        )
        self.assertEqual(
            chat_state.new_messages[-2].source,
            RegistrationChatMessage.Source.USER.value,
        )
        self.assertIn('селфи с паспортом', chat_state.new_messages[-1].content['text'])

        chat_state = self.chat_manager.submit_chat_action(
            user=self.user,
            chat_action_id='cont_resubmit_2p_passport_selfie',
            data={
                'content': 'abcd',
            },
        )
        self.assertIsNone(chat_state.action)
        self.assertEqual(len(chat_state.new_messages), 2)
        self.assertEqual(
            chat_state.new_messages[-2].source,
            RegistrationChatMessage.Source.USER.value,
        )
        self.assertIn('отправил фотографии', chat_state.new_messages[-1].content['text'])

        chat_state = self.chat_session.get_chat_state()
        self.assertIsNone(chat_state.action)
        self.assertIn('отправил фотографии', chat_state.new_messages[-1].content['text'])

    def test_need_info_and_missing_data(self):
        need_info_status = UserDocumentPhoto.VerificationStatus.NEED_INFO.value
        assignment = self.yah.create_processed_yang_assignment(
            license_front__verification_status=need_info_status,
            license_back__verification_status=need_info_status,
        )

        self.low_level_datasync.delete(
            collection=self.datasync.LICENSE_COLLECTION,
            uid=assignment.passport_biographical.document.user.uid,
            key=self.datasync.VERIFIED_NAME,
        )

        self.manager.ingest_yang_assignment(assignment)
        self.assert_user_status_equal(user=self.user, status=User.Status.ONBOARDING)

        chat_state = self.chat_session.get_chat_state()
        self.assertIsNotNone(chat_state.action)
        self.assertEqual(
            chat_state.action.get_type(),
            RegistrationChatActionResult.Type.DRIVER_LICENSE_BACK,
        )
        self.assertEqual(len(chat_state.new_messages), 2)
        message = chat_state.new_messages[0]
        self.assertIn('лицевой стороны водительского удостоверения', message.content['text'])
        self.assertIn('оборотной стороны водительского удостоверения', message.content['text'])
        self.assertIn(
            'оборотная сторона водительского удостоверения',
            chat_state.new_messages[1].content['text'],
        )

        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})


class RegistrationManagerScreeningTestCase(BaseRegistrationManagerTestCase):

    def setUp(self):
        super().setUp()
        self.user = UserFactory.create(status=User.Status.SCREENING.value)
        RegistrationState.objects.create(
            user=self.user,
            chat_completed_at=timezone.now(),
        )
        self.chat_session = self.chat_manager.make_session(self.user)

    def test_approve(self):
        self.manager.approve(user=self.user)
        self.assert_user_status_equal(user=self.user, status=User.Status.ACTIVE)
        self.assert_user_registration_date_present(self.user)

        chat_state = self.chat_session.get_chat_state()
        self.assertEqual(len(chat_state.new_messages), 1)
        message_text = chat_state.new_messages[0].content['text']
        self.assertIn(self.user.first_name, message_text)
        self.assertIn('ok', message_text)
        self.assertIn(
            User.objects.get(id=self.user.id).first_name,
            message_text,
        )
        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})

    def test_approve_rejected(self):
        self.user.status = User.Status.BAD_AGE.value
        self.user.save()

        self.manager.approve_rejected(user=self.user)
        self.assert_user_status_equal(user=self.user, status=User.Status.ACTIVE)
        self.assert_user_registration_date_present(self.user)

        chat_state = self.chat_session.get_chat_state()
        self.assertEqual(len(chat_state.new_messages), 1)
        message_text = chat_state.new_messages[0].content['text']
        self.assertIn(self.user.first_name, message_text)
        self.assertIn('ok sorry', message_text)
        self.assertIn(
            User.objects.get(id=self.user.id).first_name,
            message_text,
        )
        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})

    def test_approve_rejected_not_really_rejected(self):
        self.user.status = User.Status.ACTIVE.value
        self.user.save()
        with self.assertRaises(self.manager.NotRejectedError):
            self.manager.approve_rejected(user=self.user)

    def test_reject(self):
        self.manager.reject(user=self.user)
        self.assert_user_status_equal(user=self.user, status=User.Status.REJECTED)
        self.assert_user_registration_date_present(self.user)

        chat_state = self.chat_session.get_chat_state()
        self.assertEqual(len(chat_state.new_messages), 2)
        message_text = chat_state.new_messages[0].content['text']
        self.assertIn('no', message_text)
        self.assertEqual(self.pusher.send.call_count, 1)
        self.assertEqual(self.pusher.send.call_args[1],
                         {'uid': self.user.uid, 'message': unittest.mock.ANY})

    def test_reject_already_registered(self):
        self.user.status = User.Status.ACTIVE.value
        self.user.save()

        with self.assertRaises(self.manager.AlreadyRegisteredError):
            self.manager.reject(user=self.user)

        self.assert_user_status_equal(user=self.user, status=User.Status.ACTIVE)

        chat_state = self.chat_session.get_chat_state()
        self.assertEqual(len(chat_state.new_messages), 0)
        self.assertFalse(self.pusher.send.called)


class RegistrationManagerResubmitPhotosTestCase(BaseRegistrationManagerTestCase):

    def resubmit_and_check(self, user, photo_types, expected_chat_actions, expected_suggestions):
        self.manager.resubmit_photos(
            user=user,
            photo_types=photo_types,
        )

        for chat_action_id, suggestion in zip(expected_chat_actions, expected_suggestions):
            chat_state = self.chat_session.get_chat_state()
            self.assertEqual(chat_state.action.id, chat_action_id)
            self.assertIn(
                suggestion,
                chat_state.new_messages[-1].content['text'],
            )
            self.chat_manager.submit_chat_action(
                user=self.user,
                chat_action_id=chat_action_id,
                data={
                    'content': 'abcd',
                    'scanners': [],
                },
            )

        chat_state = self.chat_session.get_chat_state()
        self.assertIsNone(chat_state.action)

    def test_resubmit_one_photo(self):
        self.resubmit_and_check(
            user=self.user,
            photo_types=[UserDocumentPhoto.Type.DRIVER_LICENSE_BACK],
            expected_chat_actions=['resubmit_only_license_back'],
            expected_suggestions=['оборотную сторону водительского удостоверения'],
        )

    def test_resubmit_two_photos(self):
        self.resubmit_and_check(
            user=self.user,
            photo_types=[
                UserDocumentPhoto.Type.DRIVER_LICENSE_BACK,
                UserDocumentPhoto.Type.DRIVER_LICENSE_FRONT,
            ],
            expected_chat_actions=[
                'resubmit_2_from_license_back',
                'cont_resubmit_2p_license_front',
            ],
            expected_suggestions=[
                'оборотная сторона водительского удостоверения',
                'лицевая сторона водительского удостоверения',
            ],
        )

    def test_resubmit_everything(self):
        self.resubmit_and_check(
            user=self.user,
            photo_types=[
                UserDocumentPhoto.Type.DRIVER_LICENSE_BACK,
                UserDocumentPhoto.Type.DRIVER_LICENSE_FRONT,
                UserDocumentPhoto.Type.PASSPORT_BIOGRAPHICAL,
                UserDocumentPhoto.Type.PASSPORT_REGISTRATION,
                UserDocumentPhoto.Type.PASSPORT_SELFIE,
            ],
            expected_chat_actions=[
                'resubmit_2_from_license_back',
                'cont_resubmit_2p_license_front',
                'cont_resubmit_2p_passport_bio',
                'cont_resubmit_2p_passport_reg',
                'cont_resubmit_2p_passport_selfie',
            ],
            expected_suggestions=[
                'оборотная сторона водительского удостоверения',
                'лицевая сторона водительского удостоверения',
                'разворот паспорта с вашим фото',
                'разворот паспорта с регистрацией',
                'селфи с паспортом',
            ],
        )

    def test_resubmit_twice(self):
        self.manager.resubmit_photos(
            user=self.user,
            photo_types=[UserDocumentPhoto.Type.DRIVER_LICENSE_BACK],
        )
        with self.assertRaises(self.manager.PhotoResubmitInProgressError):
            self.manager.resubmit_photos(
                user=self.user,
                photo_types=[UserDocumentPhoto.Type.DRIVER_LICENSE_FRONT],
            )

    def test_resubmit_twice_with_dequeueing(self):
        self.manager.resubmit_photos(
            user=self.user,
            photo_types=[UserDocumentPhoto.Type.DRIVER_LICENSE_BACK],
        )
        self.chat_session.get_chat_state()
        with self.assertRaises(self.manager.PhotoResubmitInProgressError):
            self.manager.resubmit_photos(
                user=self.user,
                photo_types=[UserDocumentPhoto.Type.DRIVER_LICENSE_FRONT],
            )

    def test_resubmit_for_incomplete_chat(self):
        self.user.get_registration_state().delete()
        RegistrationState.objects.create(user=self.user)
        with self.assertRaises(self.manager.ChatNotCompletedError):
            self.manager.resubmit_photos(
                user=self.user,
                photo_types=[UserDocumentPhoto.Type.DRIVER_LICENSE_BACK],
            )
