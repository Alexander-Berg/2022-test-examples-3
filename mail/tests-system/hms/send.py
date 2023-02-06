# flake8: noqa
import pytest
from pycommon.xivamob_api import *
from pycommon.fake_server import *
from pycommon.api_test_base import *
from pycommon.assert_response import *
from pycommon.asserts import *

import json


def setup_module():
    global fake_push, fake_token
    fake_push = pytest.fake_provider
    fake_token = pytest.fake_provider2


message_cloud_error = "cloud error"
message_no_certificate = "no certificate"
message_invalid_payload = "invalid payload"


class TestSendHMS(APITestBase):
    def setup(self):
        super(self.__class__, self).set_args(
            app="ru.yandex.test", token="1" * 64, payload={"a": "b"}
        )
        self.xivamob = XivamobAPI()
        self.fake_push = fake_push
        self.fake_token = fake_token
        self.token_value = "12345"
        self.set_token_response(
            200,
            json.dumps({"access_token": self.token_value, "expires_in": 86400}),
        )
        self.set_hms_response(200)

    def teardown(self):
        error_in_hook = self.fake_push.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook
        error_in_hook = self.fake_token.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook

    def send(self, **kwargs):
        return self.xivamob.send_hms(**kwargs)

    def set_hms_response(self, code, raw_body=""):
        self.fake_push.set_response(raw_response=raw_body)
        self.fake_push.set_response_code(code)

    def set_token_response(self, code, raw_body=""):
        self.fake_token.set_response(raw_response=raw_body)
        self.fake_token.set_response_code(code)

    def test_send_ok(self):
        resp = self.send(**self.args())
        assert_ok(resp)

    def test_send_no_token(self):
        self.set_token_response(401)
        self.fake_push.set_request_hook(lambda (req): ok_(False, "unexpected request"))
        resp = self.send(**self.args(app="ru.yandex.test2"))
        assert_bad_gateway_error(resp, message_cloud_error)

    def test_send_bad_token(self):
        self.set_token_response(
            200,
            json.dumps({"access_token": self.token_value, "expires_in": 0}),
        )
        self.fake_push.set_request_hook(lambda (req): ok_(False, "unexpected request"))
        resp = self.send(**self.args(app="ru.yandex.test2"))
        assert_bad_gateway_error(resp, message_cloud_error)

    def test_send_bad_app(self):
        self.fake_token.set_request_hook(lambda (req): ok_(False, "unexpected token request"))
        self.fake_push.set_request_hook(lambda (req): ok_(False, "unexpected request"))
        resp = self.send(**self.args(app="ru.yandex.test3"))
        assert_code_msg(resp, 403, message_no_certificate)

    def test_send_long_payload(self):
        self.fake_push.set_request_hook(lambda (req): ok_(False, "unexpected request"))
        resp = self.send(**self.args(**{"payload": {"a": "b" * 4096}}))
        assert_bad_request(resp, message_invalid_payload)

    def test_transfer_original_response_body(self):
        inputs = [
            (200, {"code": "80000000", "msg": "Success", "requestId": "1234567890"}),
            (400, {"code": "80100001", "msg": "UnSupported param", "requestId": ""}),
            (401, {"code": "80200001", "msg": "AuthenticationFailed", "requestId": ""}),
            (404, {"code": "80300007", "msg": "ServiceNotFound", "requestId": ""}),
            (500, {"code": "81000001", "msg": "InternalServiceError", "requestId": ""}),
            (503, {"code": "80100017", "msg": "TrafficControl", "requestId": ""}),
        ]
        results = [
            {"requestId": "1234567890"},
            {"code": "80100001", "msg": "UnSupported param"},
            {"code": "80200001", "msg": "AuthenticationFailed"},
            {"code": "80300007", "msg": "ServiceNotFound"},
            {"code": "81000001", "msg": "InternalServiceError"},
            {"code": "80100017", "msg": "TrafficControl"},
        ]
        eq_(len(inputs), len(results))
        for response, result in zip(inputs, results):
            code, body = response
            yield self.check_response_body, code, body, result

    def check_response_body(self, code, body, expected_body):
        self.setup()
        self.set_hms_response(code, json.dumps(body))
        resp = self.send(**self.args(**{"payload": {}}))
        eq_(json.loads(resp.body), expected_body)
        self.teardown()
