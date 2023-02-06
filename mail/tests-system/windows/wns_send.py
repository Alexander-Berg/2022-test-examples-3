# flake8: noqa
import pytest
from pycommon.xivamob_api import *
from pycommon.fake_server import *
from pycommon.api_test_base import *
from pycommon.assert_response import *
from time import sleep
import json


def setup_module():
    global fake_push, fake_token
    fake_push = pytest.fake_provider
    fake_token = pytest.fake_provider2


message_unauthorized = "invalid subscription or endpoint"
message_bad_data = "data compose error"
message_invalid_cert = "invalid certificate"


class TestSendWns(APITestBase):
    def setup(self):
        super(self.__class__, self).set_args(
            app="ru.yandex.test", token="http://localhost:19998", payload="123"
        )
        self.xivamob = XivamobAPI()
        self.fake_push = fake_push
        self.fake_token = fake_token
        self.token_value = "12345"
        self.set_token_response(
            200,
            json.dumps(
                {"token_type": "bearer", "access_token": self.token_value, "expires_in": 86400}
            ),
        )
        self.set_wns_response(200)

    def teardown(self):
        error_in_hook = self.fake_push.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook
        error_in_hook = self.fake_token.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook

    def send(self, **kwargs):
        return self.xivamob.send_wns(**kwargs)

    def set_wns_response(self, code, raw_body=""):
        self.fake_push.set_response(raw_response=raw_body)
        self.fake_push.set_response_code(code)

    def set_token_response(self, code, raw_body=""):
        self.fake_token.set_response(raw_response=raw_body)
        self.fake_token.set_response_code(code)

    def test_send_ok(self):
        resp = self.send(**self.args())
        assert_ok(resp)

    def test_send_no_token(self):
        self.set_token_response(400)
        self.fake_push.set_request_hook(lambda (req): ok_(False, "unexpected request"))
        resp = self.send(**self.args(app="ru.yandex.test2"))
        assert_code_msg(resp, 403, message_invalid_cert)

    def test_send_bad_token(self):
        self.set_token_response(
            200,
            json.dumps({"token_type": "bearer", "access_token": self.token_value, "expires_in": 0}),
        )
        self.fake_push.set_request_hook(lambda (req): ok_(False, "unexpected request"))
        resp = self.send(**self.args(app="ru.yandex.test2"))
        assert_bad_gateway_error(resp, "cloud error")

    def test_send_bad_app(self):
        self.fake_token.set_request_hook(lambda (req): ok_(False, "unexpected token request"))
        self.fake_push.set_request_hook(lambda (req): ok_(False, "unexpected request"))
        resp = self.send(**self.args(app="ru.yandex.test3"))
        assert_code_msg(resp, 403, "no certificate")

    def test_send_invalid_endpoint(self):
        self.set_wns_response(400)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_code_msg(resp, 205, message_unauthorized)
        self.set_wns_response(404)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_code_msg(resp, 205, message_unauthorized)
        self.set_wns_response(410)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_code_msg(resp, 205, message_unauthorized)

    def test_send_invalid_certificate(self):
        self.set_wns_response(401)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_code_msg(resp, 403, message_invalid_cert)
        self.set_wns_response(403)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_code_msg(resp, 403, message_invalid_cert)

    def test_send_throttle_limit(self):
        self.set_wns_response(406)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_bad_gateway_error(resp, "cloud error")

    def test_send_invalid_payload(self):
        self.set_wns_response(413)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_bad_request(resp, "invalid payload length")

    def test_send_default_tile(self):
        resp = self.send(**self.args(**{"x-tile": "123", "payload": ""}))
        assert_bad_request(resp, message_bad_data)

    def test_send_nothing(self):
        self.fake_push.set_request_hook(lambda (req): ok_(False, "unexpected request"))
        resp = self.send(**self.args(payload=""))
        assert_bad_request(resp, message_bad_data)
