from email.header import decode_header
from mail.notsolitesrv.tests.integration.lib.expectation.mds.mock import Mds
from mail.notsolitesrv.tests.integration.lib.expectation.msettings.mock import MSettings
from mail.notsolitesrv.tests.integration.lib.expectation.tupita.mock import Tupita
from mail.notsolitesrv.tests.integration.lib.util.message import make_message
from mail.notsolitesrv.tests.integration.lib.util.responses import NslsResponses

from .util import create_user


def create_autoreply_rule(context, uid, sender_email):
    Tupita.expect_check_call_success(context, uid=uid, matched_queries=["1", "2", "3"])
    response = context.furita_api.get('/api/edit.json', params={
        'db': 'pg',
        'uid': uid,
        'name': 'Rule',
        'letter': 'all',
        'attachment': '',
        'logic': '0',
        'field1': 'from',
        'field2': '1',
        'field3': sender_email,
        'clicker': 'reply',
        'autoanswer': 'Autoreply text!',
        'order': '0',
        'stop': '0',
    })
    assert response.status_code == 200, response.text


def check_from_name(message, expected_from_name):
    assert decode_header(message.mime.get("From"))[0][0] == bytes(expected_from_name, 'utf-8')


class TestAutoreply:
    @staticmethod
    def run_test(context, sender_login, rcpt_login, settings_name, expected_from_name):
        sender = create_user(context, sender_login)
        rcpt = create_user(context, rcpt_login)

        # set custom from name for rcpt
        MSettings.expect_profile_call_success(
            context,
            uid=rcpt.uid,
            expected_settings={"from_name": settings_name}
        )

        create_autoreply_rule(context, rcpt.uid, sender.email)
        # send message
        Mds.expect_put_call_success(context, user=rcpt)
        _, msg = make_message(sender.email, [rcpt.email])
        assert context.nsls.send_message(msg) == {"all": (NslsResponses.OK.code, NslsResponses.OK.error)}
        # check autoreply sent
        relay_messages = context.relay.storage.get_messages_by_rcpt(sender.email)
        assert len(relay_messages) == 1
        check_from_name(relay_messages[0], expected_from_name)
        context.pyremock.assert_expectations()

    def test_autoreply_from_name(self, context):
        self.run_test(context, "ar_user1", "ar_user2", "Peter Resnikoff", "Peter Resnikoff")

    def test_autoreply_from_name_cyrillic(self, context):
        self.run_test(context, "ar_user3", "ar_user4", "ООО Союз меча и орала", "ООО Союз меча и орала")

    def test_autoreply_from_name_empty(self, context):
        self.run_test(context, "ar_user5", "ar_user6", "", "Резников Пётр")  # default fakebb value
