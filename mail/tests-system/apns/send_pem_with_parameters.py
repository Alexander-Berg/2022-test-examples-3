# flake8: noqa
import pytest
from pycommon.xivamob_api import *
from pycommon.api_test_base import *
from pycommon.assert_response import *
import json

# TODO: check actually receiving push messages and it's content
class TestSendAPNSWithParameters(APITestBase):
    def setup(self):
        super(self.__class__, self).set_args(
            app="xiva.mob.send.test", payload={"a": "b"}, token="1" * 64
        )
        self.xivamob = XivamobAPI()
        self.good_answer_with_id = '{"apns_message_id":"012MSGID45"}'

    def teardown(self):
        pass

    def send(self, **kwargs):
        return self.xivamob.send_apns(**kwargs)

    def test_ok_valid_x_aps_param(self):
        resp = self.send(**self.args(**{"x-aps": json.dumps({"alert": "Hello, xiva!"})}))
        assert_ok(resp, self.good_answer_with_id)

    def test_error_x_aps_param_not_json_object(self):
        resp = self.send(**self.args(**{"x-aps": "raw string value"}))
        assert_bad_request(resp, "invalid payload")

    def test_ok_unknown_x_prefixed_params_ignored(self):
        resp = self.send(**self.args(**{"x-some-setting": "some value"}))
        assert_ok(resp, self.good_answer_with_id)

    def test_ok_too_large_unknown_x_prefixed_params_ignored(self):
        resp = self.send(**self.args(**{"x-some-setting": "X" * 3000}))
        assert_ok(resp, self.good_answer_with_id)

    def test_error_too_large_payload_after_adding_x_aps(self):
        params = {"x-aps": json.dumps({"alert": "y" * 2000})}
        resp = self.send(**self.args(payload={"a": "b" * 2500}, **params))
        assert_bad_request(resp, "invalid payload length")

    def test_ok_formatting_spaces_in_x_aps_ignored(self):
        params = {"x-aps": '{ "alert": ' + " " * 2000 + '"val" }'}
        resp = self.send(**self.args(**params))
        assert_ok(resp, self.good_answer_with_id)
