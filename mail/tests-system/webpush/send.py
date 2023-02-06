# flake8: noqa
import pytest
from pycommon.xivamob_api import *
from pycommon.fake_server import *
from pycommon.api_test_base import *
from pycommon.assert_response import *
from time import sleep
import json


def setup_module():
    global fake_push
    fake_push = pytest.fake_provider


message_expired_unauthorized = "subscription expired or unauthorized"
message_unauthorized = "invalid subscription or endpoint"
message_bad_data = "data compose error"


class TestSendWebpush(APITestBase):
    def setup(self):
        super(self.__class__, self).set_args(
            subscription=self.make_sub(
                "http://localhost:19998",
                "w9iDFelHU7suN8v7xo5oZA==",
                "BPj3wE8moDU6P-KZBWogZnQ7XDlZpJsGw0tcw8KlQl_VvVRGakpSu_JMk4zcxbG5wOapQauBFN8qVUEwN9RzaYc=",
            ),
            payload="hello world",
            ttl=3600,
        )
        self.xivamob = XivamobAPI()
        self.fake_push = fake_push
        self.set_webpush_response(201)

    def teardown(self):
        error_in_hook = self.fake_push.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook

    def send(self, **kwargs):
        return self.xivamob.send_webpush(**kwargs)

    def make_sub(self, endpoint, auth, p256dh):
        return json.dumps({"endpoint": endpoint, "keys": {"auth": auth, "p256dh": p256dh}})

    def set_webpush_response(self, code, raw_body="", reason=None):
        self.fake_push.set_response(raw_response=raw_body)
        self.fake_push.set_response_code(code, reason)

    def test_send_ok(self):
        resp = self.send(**self.args())
        assert_ok(resp)

    def test_send_gone(self):
        self.set_webpush_response(410)
        resp = self.send(**self.args())
        assert_code_msg(resp, 205, message_expired_unauthorized)

    def test_send_not_found(self):
        self.set_webpush_response(404)
        resp = self.send(**self.args())
        assert_code_msg(resp, 205, message_unauthorized)

    def test_send_unauthorized(self):
        self.set_webpush_response(401)
        resp = self.send(**self.args())
        assert_code_msg(resp, 205, message_unauthorized)

    def test_send_bad_request_unauthorized(self):
        self.set_webpush_response(400, reason="UnauthorizedRegistration")
        resp = self.send(**self.args())
        assert_code_msg(resp, 205, message_expired_unauthorized)

    def test_send_bad_request(self):
        self.set_webpush_response(400)
        resp = self.send(**self.args())
        assert_bad_request(resp, message_bad_data)

    def test_send_request_entity_too_large(self):
        self.set_webpush_response(413)
        resp = self.send(**self.args())
        assert_bad_request(resp, message_bad_data)

    def test_rate_limit(self):
        self.set_webpush_response(429)
        resp = self.send(**self.args())
        assert_bad_gateway_error(resp, "cloud error")

    def test_send_cloud_error(self):
        self.set_webpush_response(500)
        resp = self.send(**self.args())
        assert_bad_gateway_error(resp, "cloud error")
