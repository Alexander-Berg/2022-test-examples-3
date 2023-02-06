from mail.notsolitesrv.tests.integration.lib.util.message import make_message
from mail.notsolitesrv.tests.integration.lib.expectation.mds.mock import Mds
from mail.notsolitesrv.tests.integration.lib.expectation.tupita.mock import Tupita
from mail.notsolitesrv.tests.integration.lib.util.responses import NslsResponses

from rules_util import make_drop_rule, make_forward_rule, check_rule_created
from util import create_user, check_message_stored

ORGID = 12345
SENDER = 'test@test.ru'
SENDER_CYR = 'test@тест.рф'
SENDER_PUNYCODE = 'test@xn--e1aybc.xn--p1ai'  # test@тест.рф


def get_queries(sender):
    return ["hdr_from_email:{}".format(sender)]


def send_email_nsls(nsls, addr_from, addr_to):
    msg_id, msg = make_message(addr_from, [addr_to])
    return msg_id, nsls.send_message(msg)


class TestDomainRules:
    @staticmethod
    def run_test_forward(context, sender, sender_in_rule, login1, login2):
        user1 = create_user(context, login1, ORGID)
        user2 = create_user(context, login2, ORGID)
        assert user1.uid is not None
        assert user2.uid is not None

        Tupita.expect_conditions_convert_call_success(context, ORGID, get_queries(sender_in_rule))
        Tupita.expect_check_call_success(context, ORGID, ["0"])
        response = context.furita_api.post('/v1/domain/rules/set', params={'orgid': ORGID},
                                           json=make_forward_rule(sender_in_rule, user2.email))
        assert response.status_code == 200, response.text
        check_rule_created(context.furitadb, make_forward_rule(sender_in_rule, user2.email), sender_in_rule, ORGID)

        Mds.expect_put_call_success(context, user=user1)
        msg_id, send_result = send_email_nsls(context.nsls, sender, user1.email)
        assert send_result == {"all": (NslsResponses.OK.code, NslsResponses.OK.error)}

        context.pyremock.assert_expectations()

        # check original message
        check_message_stored(context, user1.uid, mid=msg_id)
        # check forwarded message
        relay_messages = context.relay.storage.get_messages(msg_id)
        assert relay_messages is not None
        assert len(relay_messages) == 1
        assert relay_messages[0].envelope.rcpt_tos == [user2.email]
        assert relay_messages[0].envelope.mail_from == "domain_forward@ya.ru"

    def test_forward(self, context):
        self.run_test_forward(context, SENDER, SENDER, "user01", "user02")

    def test_forward_from_cyrillic_domain(self, context):
        self.run_test_forward(context, SENDER_PUNYCODE, SENDER_CYR, "user11", "user12")

    def test_forward_from_cyrillic_domain_punycode(self, context):
        self.run_test_forward(context, SENDER_PUNYCODE, SENDER_PUNYCODE, "user21", "user22")

    @staticmethod
    def test_drop(context):
        user3 = create_user(context, "user3", ORGID)
        Tupita.expect_conditions_convert_call_success(context, ORGID, get_queries(SENDER))
        Tupita.expect_check_call_success(context, ORGID, ["0"])
        response = context.furita_api.post('/v1/domain/rules/set', params={'orgid': ORGID},
                                            json=make_drop_rule(SENDER))
        assert response.status_code == 200, response.text
        check_rule_created(context.furitadb, make_drop_rule(SENDER), SENDER, ORGID)

        Mds.expect_put_call_success(context, user=user3)
        msg_id, send_result = send_email_nsls(context.nsls, SENDER, "user3@yandex.ru")
        assert send_result == {"all": (NslsResponses.OK.code, NslsResponses.OK.error)}

        context.pyremock.assert_expectations()
        check_message_stored(context, user3.uid, mid=None)
