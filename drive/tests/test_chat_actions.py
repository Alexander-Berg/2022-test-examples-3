import base64
import logging

from django.test import TestCase

from cars.core.mds.wrapper import MDSDocumentsWrapper
from cars.core.datasync import StubDataSyncClient
from cars.users.core.datasync import DataSyncDocumentsClient
from cars.users.core.recognized_data_submitter import RecognizedDataSubmitter
from cars.users.factories import UserFactory
from cars.users.models.registration_state import RegistrationState
from cars.users.models.user import User
from cars.users.models.user_documents import UserDocumentPhoto
from ..core import BaseChatAction, ChatManager
from ..core.chat.actions import DriverLicenseChatAction, PassportChatAction
from ..models import RegistrationChatActionResult, RegistrationChatMessage


LOGGER = logging.getLogger(__name__)


class BaseChatActionTestCase(TestCase):

    script = None

    @classmethod
    def get_script_content(cls):
        return cls.script

    def setUp(self):
        self.mds_client = MDSDocumentsWrapper.from_settings()
        self.manager = ChatManager.from_yaml(content=self.get_script_content())
        self.user = UserFactory.create(
            status=User.Status.ONBOARDING.value,
            credit_card=None,
        )
        RegistrationState.objects.create(user=self.user)
        session = self.manager.make_session(user=self.user)
        session.run_until_next_action(current_action_id=None)

    def submit_chat_action(self, chat_action_id, data):
        return self.manager.submit_chat_action(
            user=self.user,
            chat_action_id=chat_action_id,
            data=data,
        )

    def b64encode(self, value):
        return base64.b64encode(value).decode('utf-8')


class CreditCardChatActionTestCase(BaseChatActionTestCase):

    script = '''
---
flow:
  - action_group_id: "test_credit_card"
action_groups:
  - id: "test_credit_card"
    pre_action: []
    action:
      - type: credit_card
        params:
          text: "test_credit_card_text"
    post_action: []
message_groups: []
'''

    def test_success(self):
        data = {
            'pan': {
                'prefix': '123456',
                'suffix': '1234',
            },
        }
        self.submit_chat_action(chat_action_id='test_credit_card', data=data)
        action = RegistrationChatActionResult.objects.get(
            user=self.user,
            chat_action_id='test_credit_card',
        )
        self.assertIsNotNone(action.submitted_at)

    def test_empty_data(self):
        with self.assertRaises(BaseChatAction.ValidationError):
            self.submit_chat_action(chat_action_id='test_credit_card', data={})

    def test_credit_card_is_bound(self):
        data = {
            'pan': {
                'prefix': '123456',
                'suffix': '1234',
            },
        }
        self.assertIsNone(self.user.get_credit_card())
        self.submit_chat_action(chat_action_id='test_credit_card', data=data)
        credit_card = self.user.get_credit_card()
        self.assertIsNotNone(credit_card)
        self.assertEqual(credit_card.pan_prefix, '123456')
        self.assertEqual(credit_card.pan_suffix, '1234')

    def test_user_message_contains_card_details(self):
        data = {
            'pan': {
                'prefix': '123456',
                'suffix': '1234',
            },
        }
        self.submit_chat_action(chat_action_id='test_credit_card', data=data)
        message = RegistrationChatMessage.objects.get(
            source=RegistrationChatMessage.Source.USER.value,
        )
        self.assertIn('pan', message.content)
        self.assertEqual(message.content['pan']['prefix'], '123456')
        self.assertEqual(message.content['pan']['suffix'], '1234')


class DriverLicenseBackChatActionTestCase(BaseChatActionTestCase):

    script = '''
---
flow:
  - action_group_id: "test_license_back"
action_groups:
  - id: "test_license_back"
    pre_action: []
    action:
      - type: license_back
        params:
          text: "test_license_back_text"
    post_action: []
message_groups: []
'''

    def test_empty_data(self):
        with self.assertRaises(BaseChatAction.ValidationError):
            self.submit_chat_action(chat_action_id='test_license_back', data={})

    def test_invalid_base64(self):
        data = {
            'content': 'invalid base64',
            'scanners': [],
        }
        with self.assertRaises(BaseChatAction.ValidationError):
            self.submit_chat_action(chat_action_id='test_license_back', data=data)

    def test_success(self):
        data = {
            'content': 'abcd',
            'scanners': [],
        }
        self.submit_chat_action(chat_action_id='test_license_back', data=data)
        chat_action_result = RegistrationChatActionResult.objects.get(
            user=self.user,
            chat_action_id='test_license_back',
        )
        self.assertIsNotNone(chat_action_result.submitted_at)

    def test_content(self):
        original_content = b'0xdeadbeef'
        data = {
            'content': self.b64encode(original_content),
            'scanners': [],
        }
        self.submit_chat_action(chat_action_id='test_license_back', data=data)
        photo = UserDocumentPhoto.objects.get(
            user=self.user,
            type=UserDocumentPhoto.Type.DRIVER_LICENSE_BACK.value,
        )
        self.assertIsNone(photo.verified_at)
        content = self.mds_client.get_user_document_photo(photo)
        self.assertEqual(content, original_content)

    def test_barcode(self):
        self.user.first_name = self.user.last_name = self.user.patronymic_name = ''
        self.user.save()

        value = '1234567890|20170101|20270101|ФАМ|ИМЯ|ОТЧ|19900101|B,B1,M|1234|12345678'
        encoded_value = self.b64encode(base64.b64encode(value.encode('utf-8')))
        data = {
            'content': 'abcd',
            'scanners': [
                {
                    'scanner': 'PDF417',
                    'type': 'binary',
                    'fields': {
                        'data': {
                            'value': encoded_value,
                            'confidence': 1.0,
                        },
                    },
                },
            ],
        }
        self.submit_chat_action(chat_action_id='test_license_back', data=data)

        user = User.objects.get(id=self.user.id)
        self.assertEqual(user.first_name, 'Имя')
        self.assertEqual(user.last_name, 'Фам')
        self.assertEqual(user.patronymic_name, 'Отч')

    def test_unencoded_barcode(self):
        self.user.first_name = self.user.last_name = self.user.patronymic_name = ''
        self.user.save()

        value = '1234567890|20170101|20270101|ФАМ|ИМЯ|ОТЧ|19900101|B,B1,M|1234|12345678'
        encoded_value = self.b64encode(value.encode('utf-8'))
        data = {
            'content': 'abcd',
            'scanners': [
                {
                    'scanner': 'PDF417',
                    'type': 'binary',
                    'fields': {
                        'data': {
                            'value': encoded_value,
                            'confidence': 1.0,
                        },
                    },
                },
            ],
        }
        self.submit_chat_action(chat_action_id='test_license_back', data=data)

        user = User.objects.get(id=self.user.id)
        self.assertEqual(user.first_name, 'Имя')
        self.assertEqual(user.last_name, 'Фам')
        self.assertEqual(user.patronymic_name, 'Отч')


class DriverLicenseChatActionTestCase(BaseChatActionTestCase):

    script = '''
---
flow:
  - action_group_id: "test_license"
action_groups:
  - id: "test_license"
    pre_action: []
    action:
      - type: license
        params:
          text: "test_license_text"
    post_action: []
message_groups: []
'''

    def test_empty_data(self):
        with self.assertRaises(BaseChatAction.ValidationError):
            self.submit_chat_action(chat_action_id='test_license', data={})


class PassportBiographicalRegistrationChatActionTestCase(BaseChatActionTestCase):

    script = '''
---
flow:
  - action_group_id: "test_passport"
action_groups:
  - id: "test_passport"
    pre_action: []
    action:
      - type: passport
        params:
          text: "test_passport"
    post_action: []
message_groups: []
'''

    def test_recognized_submit(self):
        data = {
            'registration': {
                'content': self.b64encode(b'test'),
                'scanners': []
            },
            'biographical': {
                'content': self.b64encode(b'test'),
                'scanners': [
                    {
                        'fields': {
                            'name': {
                                'value': 'ИМЯ',
                                'confidence': 1
                            },
                            'birthplace': {
                                'value': 'ГОР. МОСКВА',
                                'confidence': 1
                            },
                            'series': {
                                'value': '1234',
                                'confidence': 0.8520265854245387
                            },
                            'surname': {
                                'value': 'ФАМИЛИЯ',
                                'confidence': 1
                            },
                            'issue_date': {
                                'value': '01.01.2000',
                                'confidence': 0.8502945764790574
                            },
                            'patronymic': {
                                'value': 'ОТЧЕСТВО',
                                'confidence': 0.8492476989858658
                            },
                            'authority_code': {
                                'value': '123-456',
                                'confidence': 0.866187527240523
                            },
                            'authority': {
                                'value': ('ОТДЕЛЕНИЕМ УФМС РОССИИ ПО МОСКОВСКОЙ ОБЛАСТИ'
                                          ' В ЛЕНИНСКОМ РАЙОНЕ ГОР. МОСКВА'),
                                'confidence': 0.866187527240523
                            },
                            'gender': {
                                'value': 'МУЖ.',
                                'confidence': 0.8487821530649421
                            },
                            'birthdate': {
                                'value': '01.01.1992',
                                'confidence': 0.8739516171246299
                            },
                            'number': {
                                'value': '123456',
                                'confidence': 0.8632398109028969
                            }
                        },
                        'type': 'page3',
                        'scanner': 'Smart ID 1.15.0.25617'
                    }
                ]
            }
        }

        client = DataSyncDocumentsClient(StubDataSyncClient())
        PassportChatAction.recognized_data_submitter = RecognizedDataSubmitter(
            client
        )

        self.submit_chat_action(
            chat_action_id='test_passport',
            data=data
        )

        reflected_in_datasync = client.get_passport_unverified(
            self.user.uid,
            'initial'
        )
        expected = {
            'id': 'initial',
            'first_name': 'ИМЯ',
            'last_name': 'ФАМИЛИЯ',
            'middle_name': 'ОТЧЕСТВО',
            'doc_type': 'id',
            'doc_value': '1234123456',
            'citizenship': 'РОССИЙСКАЯ ФЕДЕРАЦИЯ',
            'birth_place': 'ГОР. МОСКВА',
            'subdivision_code': '123-456',
            'issue_date': '2000-01-01T00:00:00+00:00',
            'gender': 'МУЖ',
            'birth_date': '1992-01-01T00:00:00+00:00',
        }
        self.assertEqual(reflected_in_datasync, expected)

    def test_recognized_submit_not_all_fields(self):
        data = {
            'registration': {
                'content': self.b64encode(b'test'),
                'scanners': []
            },
            'biographical': {
                'content': self.b64encode(b'test'),
                'scanners': [
                    {
                        'fields': {
                            'name': {
                                'value': 'ИМЯ',
                                'confidence': 1
                            },
                            'birthplace': {
                                'value': 'ГОР. МОСКВА',
                                'confidence': 1
                            }
                        },
                        'type': 'page3',
                        'scanner': 'Smart ID 1.15.0.25617'
                    },
                ],
            },
        }

        client = DataSyncDocumentsClient(StubDataSyncClient())
        PassportChatAction.recognized_data_submitter = RecognizedDataSubmitter(
            client
        )

        self.submit_chat_action(
            chat_action_id='test_passport',
            data=data
        )

        reflected_in_datasync = client.get_passport_unverified(
            self.user.uid,
            'initial'
        )
        expected = {
            'id': 'initial',
            'first_name': 'ИМЯ',
            'doc_type': 'id',
            'citizenship': 'РОССИЙСКАЯ ФЕДЕРАЦИЯ',
            'birth_place': 'ГОР. МОСКВА',
        }
        self.assertEqual(reflected_in_datasync, expected)


class PassportSelfieChatActionTestCase(BaseChatActionTestCase):

    script = '''
---
flow:
  - action_group_id: "test_passport_selfie"
action_groups:
  - id: "test_passport_selfie"
    pre_action: []
    action:
      - type: passport_selfie
        params:
          text: "test_passport_selfie"
    post_action: []
message_groups: []
'''

    def test_only_selfie(self):
        data = {
            'content': self.b64encode(b'test'),
        }
        self.submit_chat_action(chat_action_id='test_passport_selfie', data=data)
        photo = UserDocumentPhoto.objects.get(
            user=self.user,
            type=UserDocumentPhoto.Type.PASSPORT_SELFIE.value,
        )
        self.assertIsNone(photo.verified_at)
        content = self.mds_client.get_user_document_photo(photo)
        self.assertEqual(content, b'test')

    def test_selfie_with_face(self):
        data = {
            'face': {
                'content': self.b64encode(b'face'),
            },
            'selfie': {
                'content': self.b64encode(b'selfie'),
            },
        }
        self.submit_chat_action(chat_action_id='test_passport_selfie', data=data)

        expected_data = [
            [UserDocumentPhoto.Type.FACE.value, b'face'],
            [UserDocumentPhoto.Type.PASSPORT_SELFIE.value, b'selfie'],
        ]
        for type_, expected_content in expected_data:
            photo = UserDocumentPhoto.objects.get(
                user=self.user,
                type=type_,
            )
            self.assertIsNone(photo.verified_at)
            content = self.mds_client.get_user_document_photo(photo)
            self.assertEqual(content, expected_content)


class ImageChatMessageTestCase(BaseChatActionTestCase):

    script = '''
---
flow:
  - action_group_id: "test_image_chat_item"
action_groups:
  - id: "test_image_chat_item"
    pre_action:
      - type: image
        params:
          url: "https://ya.ru/favicon.ico"
          on_tap:
            type: open_url
            content:
              url: "https://ya.ru"
      - type: image
        params:
          url: "https://ya.ru/favicon.ico"
    action:
      - type: ok
        params:
          text: ""
    post_action: []
message_groups: []
'''

    def test_ok(self):
        state = self.manager.get_chat_state(user=self.user)

        message1 = state.new_messages[0]
        self.assertEqual(message1.content['url'], 'https://ya.ru/favicon.ico')
        self.assertEqual(message1.on_tap['type'], 'open_url')
        self.assertEqual(message1.on_tap['content']['url'], 'https://ya.ru')

        message2 = state.new_messages[1]
        self.assertIsNone(message2.on_tap)


class TextChatMessageTestCase(BaseChatActionTestCase):

    script = '''
---
flow:
  - action_group_id: "test_text_chat_item"
action_groups:
  - id: "test_text_chat_item"
    pre_action:
      - type: text
        params:
          message: "test1"
          on_tap:
            type: mailto
            content:
              to: "noreply@yandex.ru"
      - type: text
        params:
          message: "test2"
    action:
      - type: ok
        params:
          text: ""
    post_action: []
message_groups: []
'''

    def test_ok(self):
        state = self.manager.get_chat_state(user=self.user)

        message1 = state.new_messages[0]
        self.assertEqual(message1.content['text'], 'test1')
        self.assertEqual(message1.on_tap['type'], 'mailto')
        self.assertEqual(message1.on_tap['content']['to'], 'noreply@yandex.ru')

        message2 = state.new_messages[1]
        self.assertEqual(message2.content['text'], 'test2')
        self.assertIsNone(message2.on_tap)
