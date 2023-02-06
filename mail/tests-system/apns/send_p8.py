# flake8: noqa
import pytest
from pycommon.xivamob_api import *
from pycommon.api_test_base import *
from pycommon.assert_response import *


# TODO: check actually receiving push messages
class TestSendAPNS(APITestBase):
    def setup(self):
        super(self.__class__, self).set_args(
            app="ru.yandex.mail", payload={"a": "b"}, token="1" * 64
        )
        self.xivamob = XivamobAPI()

    def teardown(self):
        pass

    def send(self, **kwargs):
        return self.xivamob.send_apns(**kwargs)

    def test_ok_send_valid(self):
        resp = self.send(**self.args())
        assert_ok(resp, '{"apns_message_id":"012MSGID45"}')

    def test_error_unknown_app(self):
        resp = self.send(**self.args(app="unknown.app.test"))
        assert_code_msg(resp, 403, "no certificate")

    def test_error_too_large_ttl(self):
        resp = self.send(**self.args(ttl=10000000000000))
        assert_bad_request(resp, 'invalid argument "ttl"')

    def test_error_negative_ttl(self):
        resp = self.send(**self.args(ttl=-10))
        assert_bad_request(resp, 'invalid characters in argument "ttl"')

    def test_error_non_numeric_ttl(self):
        resp = self.send(**self.args(ttl="abcde"))
        assert_bad_request(resp, 'invalid characters in argument "ttl"')

    def test_error_push_token_contains_invalid_symbols(self):
        resp = self.send(**self.args(token="Y" * 64))
        assert_code_msg(resp, 205, "invalid token")

    def test_error_expired_ceritificate(self):
        resp = self.send(**self.args(app="invalid.app.test"))
        assert_code_msg(resp, 403, "no certificate")

    def test_error_non_json_payload(self):
        resp = self.send(**self.args(payload="raw string"))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args(payload=10))
        assert_bad_request(resp, "invalid payload")

    def test_error_invalid_json_payload(self):
        resp = self.send(**self.args(payload='{"key": "unclosed value}'))
        assert_bad_request(resp, "invalid payload")

    def test_error_too_large_payload(self):
        resp = self.send(**self.args(payload={"key": "1" * 5000}))
        assert_bad_request(resp, "invalid payload length")

    def test_error_empty_payload(self):
        resp = self.send(**self.args(payload={}))
        assert_bad_request(resp, "invalid payload")
