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


def assert_meh(resp):
    assert_code_msg(resp, 245, "Meh")


class TestSendMpns(APITestBase):
    def setup(self):
        super(self.__class__, self).set_args(
            **{
                "app": "ru.yandex.test",
                "token": "http://localhost:19998",
                "payload": "123",
                "x-toast": "123",
                "x-tile": json.dumps({"Field1": "Val1"}),
            }
        )
        self.xivamob = XivamobAPI()
        self.fake_push = fake_push
        self.set_mpns_response(200)
        self.expected_request_count = None

    def teardown(self):
        if self.expected_request_count:
            if self.expected_request_count != self.request_count:
                sleep(0.1)  # wait for late response
            eq_(self.expected_request_count, self.request_count)
        error_in_hook = self.fake_push.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook

    def send(self, **kwargs):
        return self.xivamob.send_mpns(**kwargs)

    def set_mpns_response(self, code, raw_body=""):
        self.fake_push.set_response(raw_response=raw_body)
        self.fake_push.set_response_code(code)

    def mpns_request_chain_status(self, status_list):
        self.request_count = 0
        self.expected_request_count = len(status_list)
        self.set_mpns_response(status_list[0])
        self.fake_push.set_request_hook(
            lambda (req): self.mpns_request_chain_callback(status_list[1:])
        )

    def mpns_request_chain_callback(self, status_list):
        if self.request_count > len(status_list):
            ok_(False, "unexpected request number")
        if self.request_count < len(status_list):  # can be equal for last request
            self.set_mpns_response(status_list[self.request_count])
        self.request_count += 1

    def test_send_default_tile_multi(self):
        self.set_mpns_response(200)
        resp = self.send(**self.args(**{"x-tile": "123"}))
        assert_meh(resp)

    def test_send_200_400(self):
        self.mpns_request_chain_status([200, 400])
        resp = self.send(**self.args())
        assert_code_msg(resp, 205, "invalid subscription or endpoint")

    def test_send_200_401(self):
        self.mpns_request_chain_status([200, 401])
        resp = self.send(**self.args())
        assert_code_msg(resp, 403, "invalid certificate")

    def test_send_200_201_202(self):
        self.mpns_request_chain_status([200, 201, 202])
        resp = self.send(**self.args())
        assert_ok(resp)

    def test_send_200_200_and_bad_req(self):
        self.mpns_request_chain_status([200, 200])
        resp = self.send(**self.args(**{"x-tile": "123"}))
        assert_meh(resp)

    def test_send_200_500_500(self):
        self.mpns_request_chain_status([200, 500])
        resp = self.send(**self.args())
        assert_bad_gateway_error(resp)

    def test_send_412_500_and_bad_req(self):
        self.mpns_request_chain_status([412])
        resp = self.send(**self.args(**{"x-tile": "123"}))
        assert_bad_gateway_error(resp, "cloud error")
