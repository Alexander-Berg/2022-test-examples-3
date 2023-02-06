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


message_invalid_sub_ep = "invalid subscription or endpoint"
message_cloud_error = "cloud error"
message_data_compose_error = "data compose error"


class TestSendMpns(APITestBase):
    def setup(self):
        super(self.__class__, self).set_args(
            app="ru.yandex.test", token="http://localhost:19998", payload="123"
        )
        self.xivamob = XivamobAPI()
        self.fake_push = fake_push
        self.set_mpns_response(200)

    def teardown(self):
        error_in_hook = self.fake_push.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook

    def send(self, **kwargs):
        return self.xivamob.send_mpns(**kwargs)

    def set_mpns_response(self, code, raw_body=""):
        self.fake_push.set_response(raw_response=raw_body)
        self.fake_push.set_response_code(code)

    def test_send_ok(self):
        resp = self.send(**self.args())
        assert_ok(resp)

    def test_send_invalid_endpoint(self):
        self.set_mpns_response(400)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_code_msg(resp, 205, message_invalid_sub_ep)
        self.set_mpns_response(404)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_code_msg(resp, 205, message_invalid_sub_ep)
        self.set_mpns_response(410)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_code_msg(resp, 205, message_invalid_sub_ep)

    def test_send_invalid_certificate(self):
        self.set_mpns_response(401)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_code_msg(resp, 403, "invalid certificate")

    def test_send_throttle_limit(self):
        self.set_mpns_response(406)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_bad_gateway_error(resp, message_cloud_error)

    def test_send_disconnected(self):
        self.set_mpns_response(412)
        resp = self.send(**self.args(**{"x-toast": "123"}))
        assert_bad_gateway_error(resp, message_cloud_error)

    def test_send_default_tile(self):
        resp = self.send(**self.args(**{"x-tile": "123", "payload": ""}))
        assert_bad_request(resp, message_data_compose_error)

    def test_send_badge(self):
        resp = self.send(**self.args(**{"x-badge": "123", "payload": ""}))
        assert_bad_request(resp, message_data_compose_error)

    def test_send_nothing(self):
        self.fake_push.set_request_hook(lambda (req): ok_(False, "unexpected request"))
        resp = self.send(**self.args(payload=""))
        assert_bad_request(resp, message_data_compose_error)
