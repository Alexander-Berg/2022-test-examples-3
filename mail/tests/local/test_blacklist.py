from mail.notsolitesrv.tests.integration.lib.util.message import make_message
from mail.notsolitesrv.tests.integration.lib.expectation.mds.mock import Mds
from mail.notsolitesrv.tests.integration.lib.util.responses import NslsResponses

from .util import create_user, check_message_stored


PUNYCODE_ADDR = 'test@xn--e1aybc.xn--p1ai'  # test@тест.рф


class TestBlacklist:
    @staticmethod
    def run_test(context, user, user_email, blacklist_email):
        # send letter, check received
        Mds.expect_put_call_success(context, user=user)
        msg_id, msg = make_message(PUNYCODE_ADDR, [user_email])
        assert context.nsls.send_message(msg) == {"all": (NslsResponses.OK.code, NslsResponses.OK.error)}
        revision = check_message_stored(context, user.uid, mid=msg_id)
        # add to blacklist
        response = context.furita_api.get('/api/blacklist_add.json', params={
            'db': 'pg',
            'uid': user.uid,
            'email': blacklist_email
        })
        assert response.status_code == 200, response.text
        # send same letter, check not received
        Mds.expect_put_call_success(context, user=user)
        _, msg = make_message(PUNYCODE_ADDR, [user_email])
        assert context.nsls.send_message(msg) == {"all": (NslsResponses.OK.code, NslsResponses.OK.error)}
        check_message_stored(context, user.uid, mid=None, revision=revision)

        context.pyremock.assert_expectations()

    def test_punycode(self, context):
        user1 = create_user(context, "bl_user1")
        self.run_test(context, user1, 'bl_user1@yandex.ru', 'test@тест.рф')

    def test_double_punycode(self, context):
        user2 = create_user(context, "bl_user2")
        self.run_test(context, user2, 'bl_user2@yandex.ru', PUNYCODE_ADDR)

    def test_invalid_blacklist_entry(self, context):
        user3 = create_user(context, "bl_user3")
        # add invalid address to blacklist
        response = context.furita_api.get('/api/blacklist_add.json', params={
            'db': 'pg',
            'uid': user3.uid,
            'email': 'test@news1.test.com 6 янв в 19:25 : vab@ieie.nsc.ru'
        })
        assert response.status_code == 200, response.text
        # send any letter to this user
        Mds.expect_put_call_success(context, user=user3)
        _, msg = make_message('test@test.com', [user3.email])
        assert context.nsls.send_message(msg) == {"all": (NslsResponses.OK.code, NslsResponses.OK.error)}
        context.pyremock.assert_expectations()
