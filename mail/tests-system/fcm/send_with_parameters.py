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


class TestSendFCMWithParameters(APITestBase):
    def setup(self):
        super(self.__class__, self).set_args(
            app="xiva.mob.send.test", payload={"a": "b"}, token="1" * 100
        )
        self.xivamob = XivamobAPI()
        self.fake_fcm = fake_fcm
        self.set_fcm_response(fcm_response.success)

    def teardown(self):
        error_in_hook = self.fake_fcm.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook

    def send(self, **kwargs):
        return self.xivamob.send_gcm(**kwargs)  # gcm_compatibility

    def set_fcm_response(self, response):
        (code, raw_body) = response
        self.fake_fcm.set_response(raw_response=raw_body)
        self.fake_fcm.set_response_code(code)

    def args(self, params):
        for param in params:
            if not isinstance(params[param], str):
                params[param] = json.dumps(params[param])
        return super(self.__class__, self).args(**params)

    def check_fcm_requests(self, req, exp_body):
        req_body = json.loads(req.body)
        for key in exp_body:
            eq_(exp_body[key], req_body.get(key, None))

    def test_ok_valid_x_restricted_package_name(self):
        expect = {"restricted_package_name": "xiva.mob.send.test"}
        self.fake_fcm.set_request_hook(lambda (req): self.check_fcm_requests(req, expect))
        resp = self.send(
            **self.args(
                {"x-restricted_package_name": json.dumps(expect["restricted_package_name"])}
            )
        )
        assert_ok(resp, '{"message_id":"1"}')

    def test_error_x_restricted_package_name_not_string(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args({"x-restricted_package_name": 10}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-restricted_package_name": True}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-restricted_package_name": []}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-restricted_package_name": {}}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-restricted_package_name": '{"a":"notclosed}'}))
        assert_bad_request(resp, "invalid payload")

    def test_ok_valid_x_priority(self):
        expect = {"priority": "normal"}
        self.fake_fcm.set_request_hook(lambda (req): self.check_fcm_requests(req, expect))
        resp = self.send(**self.args({"x-priority": json.dumps(expect["priority"])}))
        assert_ok(resp, '{"message_id":"1"}')

    def test_error_x_priority_not_string(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args({"x-priority": 10}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-priority": True}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-priority": []}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-priority": {}}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-priority": '{"a":"notclosed}'}))
        assert_bad_request(resp, "invalid payload")

    def test_ok_valid_x_collapse_key(self):
        expect = {"collapse_key": "some-tag"}
        self.fake_fcm.set_request_hook(lambda (req): self.check_fcm_requests(req, expect))
        resp = self.send(**self.args({"x-collapse_key": json.dumps(expect["collapse_key"])}))
        assert_ok(resp, '{"message_id":"1"}')

    def test_error_x_collapse_key_not_string(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args({"x-collapse_key": 10}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-collapse_key": True}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-collapse_key": []}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-collapse_key": {}}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-collapse_key": '{"a":"notclosed}'}))
        assert_bad_request(resp, "invalid payload")

    def test_ok_valid_x_delay_while_idle(self):
        expect = {"delay_while_idle": True}
        self.fake_fcm.set_request_hook(lambda (req): self.check_fcm_requests(req, expect))
        resp = self.send(
            **self.args({"x-delay_while_idle": json.dumps(expect["delay_while_idle"])})
        )
        assert_ok(resp, '{"message_id":"1"}')

    def test_error_x_delay_while_idle_not_boolean(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args({"x-delay_while_idle": 10}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-delay_while_idle": "smth"}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-delay_while_idle": []}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-delay_while_idle": {}}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-delay_while_idle": '{"a":"notclosed}'}))
        assert_bad_request(resp, "invalid payload")

    def test_ok_valid_x_notification(self):
        expect = {"notification": {"title": "Hello, world!"}}
        self.fake_fcm.set_request_hook(lambda (req): self.check_fcm_requests(req, expect))
        resp = self.send(**self.args({"x-notification": expect["notification"]}))
        assert_ok(resp, '{"message_id":"1"}')

    def test_error_x_notification_not_json_object(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args({"x-notification": 10}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-notification": "smth"}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-notification": []}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-notification": True}))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args({"x-notification": '{"a":"notclosed}'}))
        assert_bad_request(resp, "invalid payload")

    # def test_ok_formatting_spaces_in_x_notification_ignored(self):
    #     expect = { "notification": { "title": "Hello" } }
    #     self.fake_fcm.set_request_hook(lambda (req): self.check_fcm_requests(req, expect))
    #     resp = self.send(**self.args({'x-notification': '{"title":' + ' ' *4000 + ' "Hello"}'}))
    #     assert_ok(resp)

    def test_error_too_large_parameter_causes_too_large_payload(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args({"x-notification": {"title": "Y" * 2048}}))
        assert_bad_request(resp, "invalid payload length")

    def test_ok_sum_of_payload_and_some_parameter_not_causes_too_large_paload_error(self):
        resp = self.send(
            **self.args({"x-notification": {"title": "Y" * 2000}, "payload": {"a": "b" * 4000}})
        )
        assert_ok(resp, '{"message_id":"1"}')

    def test_ok_unknown_x_prefixed_params_ignored(self):
        expect = {"unknown_parameter": None}
        self.fake_fcm.set_request_hook(lambda (req): self.check_fcm_requests(req, expect))
        resp = self.send(**self.args({"x-unknown_parameter": "some value"}))
        assert_ok(resp, '{"message_id":"1"}')

    def test_ok_empty_x_prefixed_params_ignored(self):
        expect = {"notification": None}
        self.fake_fcm.set_request_hook(lambda (req): self.check_fcm_requests(req, expect))
        resp = self.send(**self.args({"x-notification": ""}))
        assert_ok(resp, '{"message_id":"1"}')

    def test_error_empty_data_and_notification(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args({"payload": {}, "x-notification": {}}))
        assert_bad_request(resp, "invalid payload")

    def test_ok_empty_data_full_notification(self):
        resp = self.send(**self.args({"payload": {}, "x-notification": {"title": "smth"}}))
        assert_ok(resp, '{"message_id":"1"}')
