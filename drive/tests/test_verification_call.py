import datetime
import json
import os
import unittest.mock
from urllib3.exceptions import HTTPError

from django.test import TestCase

import cars.settings
from cars.users.factories.user import UserFactory
from cars.users.models.registration_state import RegistrationState
from cars.users.models.user import User
from ..core.chat.manager import ChatManager
from ..core.verification_call_resolver import RegistrationVerificationCallResolver
from ..models.chat_action_result import RegistrationChatActionResult
from ..models.verification_call import RegistrationVerificationCall


class VerificationCallTestCase(TestCase):

    script = '''
---
flow:
  - action_group_id: "call"

action_groups:
  - id: call
    pre_action:
      - type: text
        params:
          message: "pre_action"
    action:
      - type: call_verify
        params:
          text: "call_me"
    post_action:
      - type: text
        params:
          message: "ok_message"
        when:
          eq:
            - call_status
            - OK
      - type: text
        params:
          message: "no_pick_up_message"
        when:
          eq:
            - call_status
            - NO_PICK_UP
      - type: retry_action
        params: {}
        when:
          eq:
            - call_status
            - NO_PICK_UP
      - type: text
        params:
          message: "error_message"
        when:
          eq:
            - call_status
            - ERROR
      - type: retry_action
        params: {}
        when:
          eq:
            - call_status
            - ERROR
message_groups: []
'''

    def setUp(self):
        self.user = UserFactory.create(status=User.Status.ONBOARDING.value, phone='+71234567890')
        RegistrationState.objects.create(user=self.user)
        self.chat_manager = ChatManager.from_yaml(self.script)
        self.chat_session = self.chat_manager.make_session(user=self.user)
        self.chat_session.get_chat_state()

    def create_resolver(self, octopus_client):
        return RegistrationVerificationCallResolver(
            chat_manager=self.chat_manager,
            octopus_client=octopus_client,
            call_timeout=cars.settings.REGISTRATION['octopus']['call_timeout'],
            solomon_client=unittest.mock.MagicMock(),
            call_check_lag=1,
        )

    """
    def submit_call(self):
        self.chat_session.submit_chat_action(
            chat_action_id='call',
            data={},
        )

    def assert_call_status(self, call, status):
        self.assertEqual(call.status, status.value)

    def test_chat_populated(self):
        state = self.chat_session.get_chat_state()
        self.assertIsNotNone(state.action)

        self.submit_call()

        state = self.chat_session.get_chat_state()
        self.assertIsNone(state.action)
        self.assertEqual(state.new_messages[-1].content['text'], 'call_me')

    def test_new(self):
        self.submit_call()

        resolver = self.create_resolver(octopus_client=InprogressOctopusClient())
        resolver.resolve_all()

        call = RegistrationVerificationCall.objects.get()
        self.assert_call_status(call, RegistrationVerificationCall.Status.INPROGRESS)

        state = self.chat_session.get_chat_state()
        self.assertIsNone(state.action)
        self.assertEqual(state.new_messages[-1].content['text'], 'call_me')

    def test_timeout(self):
        self.submit_call()

        call = RegistrationVerificationCall.objects.get()
        call.submitted_at -= datetime.timedelta(seconds=120)
        call.save()

        resolver = self.create_resolver(octopus_client=InprogressOctopusClient())
        resolver.resolve_all()

        call = RegistrationVerificationCall.objects.get()
        self.assert_call_status(call, RegistrationVerificationCall.Status.ERROR)

        state = self.chat_session.get_chat_state()
        self.assertIsNotNone(state.action)
        self.assertEqual(state.action.id, 'call')
        self.assertEqual(state.new_messages[-1].content['text'], 'error_message')

    def test_hangup(self):
        self.submit_call()

        resolver = self.create_resolver(octopus_client=HangupOctopusClient())
        resolver.resolve_all()

        call = RegistrationVerificationCall.objects.get()
        self.assert_call_status(call, RegistrationVerificationCall.Status.ERROR)

        state = self.chat_session.get_chat_state()
        self.assertIsNotNone(state.action)
        self.assertEqual(state.action.id, 'call')
        self.assertEqual(state.new_messages[-1].content['text'], 'error_message')

    def test_ok(self):
        self.submit_call()

        resolver = self.create_resolver(octopus_client=OkOctopusClient())
        resolver.resolve_all()

        call = RegistrationVerificationCall.objects.get()
        self.assert_call_status(call, RegistrationVerificationCall.Status.OK)

        self.assertEqual(
            RegistrationChatActionResult.objects.get(id=call.chat_action_result_id).status,
            RegistrationChatActionResult.Status.COMPLETE.value,
        )

        state = self.chat_session.get_chat_state()
        self.assertEqual(state.new_messages[-1].content['text'], 'ok_message')

    def test_unanswer(self):
        self.submit_call()

        resolver = self.create_resolver(octopus_client=UnanswerOctopusClient())
        resolver.resolve_all()

        call = RegistrationVerificationCall.objects.get()
        self.assert_call_status(call, RegistrationVerificationCall.Status.NO_PICK_UP)

        self.assertEqual(
            RegistrationChatActionResult.objects.get(id=call.chat_action_result_id).status,
            RegistrationChatActionResult.Status.COMPLETE.value,
        )

        state = self.chat_session.get_chat_state()
        self.assertIsNotNone(state.action)
        self.assertEqual(state.action.id, 'call')
        self.assertEqual(state.new_messages[-1].content['text'], 'no_pick_up_message')

    def test_repeat_unanswered_call(self):
        self.submit_call()

        resolver = self.create_resolver(octopus_client=UnanswerOctopusClient())
        resolver.resolve_all()

        state = self.chat_session.get_chat_state()
        self.assertIsNotNone(state.action)
        self.assertEqual(state.new_messages[-1].content['text'], 'no_pick_up_message')

        self.submit_call()

        state = self.chat_session.get_chat_state()
        self.assertIsNone(state.action)
        self.assertEqual(state.new_messages[-1].content['text'], 'call_me')

        resolver = self.create_resolver(octopus_client=OkOctopusClient())
        resolver.resolve_all()

        state = self.chat_session.get_chat_state()
        self.assertIsNone(state.action)
        self.assertEqual(state.new_messages[-1].content['text'], 'ok_message')

    def test_octopus_fails_to_get_session(self):
        self.submit_call()

        resolver = self.create_resolver(
            octopus_client=ExceptionOctopusClientGetSession()
        )
        resolver.resolve_all()

        call = RegistrationVerificationCall.objects.get()
        self.assert_call_status(call, RegistrationVerificationCall.Status.OK)

        self.assertEqual(
            RegistrationChatActionResult.objects.get(id=call.chat_action_result_id).status,
            RegistrationChatActionResult.Status.COMPLETE.value,
        )

        state = self.chat_session.get_chat_state()
        self.assertEqual(state.new_messages[-1].content['text'], 'ok_message')


class FixedOctopusClient(object):

    def __init__(self, type_):
        response_path = os.path.join(
            os.path.dirname(__file__),
            'resources',
            'octopus_session_logs',
            '{}.json'.format(type_),
        )
        with open(response_path) as f:
            self.response = json.load(f)

    def get_session_log(self, session_id):  # pylint: disable=unused-argument
        return self.response


class HangupOctopusClient(FixedOctopusClient):
    def __init__(self):
        super().__init__(type_='hangup')


class InprogressOctopusClient(FixedOctopusClient):
    def __init__(self):
        super().__init__(type_='inprogress')


class OkOctopusClient(FixedOctopusClient):
    def __init__(self):
        super().__init__(type_='ok')


class UnanswerOctopusClient(FixedOctopusClient):
    def __init__(self):
        super().__init__(type_='unanswer')


class ExceptionOctopusClientGetSession:

    def get_session_log(self, session_id):
        raise HTTPError('octopus get session failed')
"""
