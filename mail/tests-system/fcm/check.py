# flake8: noqa
import pytest
from pycommon.xivamob_api import *
from pycommon.fake_server import *
from pycommon.api_test_base import *
from pycommon.assert_response import *
import json
import fcm_response


def setup_module():
    global fake_fcm
    fake_fcm = pytest.fake_provider


class TestCheckFCM:
    def setup(self):
        self.xivamob = XivamobAPI()
        self.fake_fcm = fake_fcm

    def teardown(self):
        error_in_hook = self.fake_fcm.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook

    def check_fcm(self, **kwargs):
        return self.xivamob.check_fcm(**kwargs)

    def set_fcm_response(self, response):
        (code, raw_body) = response
        self.fake_fcm.set_response(raw_response=raw_body)
        self.fake_fcm.set_response_code(code)

    def test_check_ok(self):
        self.set_fcm_response(fcm_response.check_ok)
        resp = self.check_fcm(app="xiva.mob.send.test", service="mail", token="12345")
        assert_code_msg(resp, 200, '{"token":"12345","code":"200","error":""}')

    def test_check_invalid_token(self):
        self.set_fcm_response(fcm_response.check_bad)
        resp = self.check_fcm(app="xiva.mob.send.test", service="mail", token="abcdef")
        assert_code_msg(resp, 200, '{"token":"abcdef","code":"400","error":"InvalidToken"}')

    def test_check_not_found(self):
        self.set_fcm_response(fcm_response.check_unexpected)
        resp = self.check_fcm(app="xiva.mob.send.test", service="mail", token="secret")
        assert_code_msg(resp, 200, '{"token":"secret","code":"404","error":"NotFound"}')

    def test_check_empty_token(self):
        resp = self.check_fcm(app="xiva.mob.send.test", service="mail", token="")
        assert_code_msg(resp, 400, 'missing argument "token"')

    def test_check_missed_service(self):
        resp = self.check_fcm(app="xiva.mob.send.test", token="abcdef")
        assert_code_msg(resp, 400, 'missing argument "service"')

    def test_check_unknown_app(self):
        resp = self.check_fcm(app="neverapp", service="mail", token="secret")
        assert_code_msg(resp, 403, "no certificate")
