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


class TestSendFCM(APITestBase):
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

    def send_gcm(self, **kwargs):  # gcm_compatibility
        return self.xivamob.send_gcm(**kwargs)  # gcm_compatibility

    def send(self, **kwargs):
        return self.xivamob.send_fcm(**kwargs)

    def set_fcm_response(self, response):
        (code, raw_body) = response
        self.fake_fcm.set_response(raw_response=raw_body)
        self.fake_fcm.set_response_code(code)

    def check_fcm_requests(self, req, exp_body):
        req_body = json.loads(req.body)
        for key in exp_body:
            eq_(exp_body[key], req_body.get(key, None))

    def test_ok_send_valid(self):
        resp = self.send(**self.args())
        assert_ok(resp, '{"message_id":"1"}')

    def test_ok_send_valid_but_token_changed(self):
        self.set_fcm_response(fcm_response.success_but_token_changed)
        resp = self.send(**self.args())
        assert_code_msg(resp, 200, '{"message_id":"1","new_token":"other"}')

    def test_error_unknown_app(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args(app="unknown.app.test"))
        assert_code_msg(resp, 403, "no certificate")

    def test_error_invalid_or_expired_api_key(self):
        self.set_fcm_response(fcm_response.unauthorized)
        resp = self.send(**self.args())
        assert_code_msg(resp, 403, "invalid certificate")

    def test_ok_push_token_too_small(self):
        resp = self.send(**self.args(token="123"))
        assert_ok(resp, '{"message_id":"1"}')

    def test_ok_push_token_too_large(self):
        resp = self.send(**self.args(token="1" * 1024))
        assert_ok(resp, '{"message_id":"1"}')

    def test_ok_push_token_with_any_symbols(self):
        resp = self.send(**self.args(token="124567890-=qwertyuoasdj`@#%^&()!"))
        assert_ok(resp, '{"message_id":"1"}')

    def test_ok_push_token_larger_then_4kb(self):
        resp = self.send(**self.args(token="Y" * 5000))
        assert_ok(resp, '{"message_id":"1"}')

    def test_error_non_json_payload(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args(payload="raw string"))
        assert_bad_request(resp, "invalid payload")
        resp = self.send(**self.args(payload=10))
        assert_bad_request(resp, "invalid payload")

    def test_error_invalid_json_payload(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args(payload='{"key": "unclosed value}'))
        assert_bad_request(resp, "invalid payload")

    def test_error_too_large_payload(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args(payload={"key": "1" * 5000}))
        assert_bad_request(resp, "invalid payload length")

    def test_error_too_large_ttl(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args(ttl=10000000000000))
        assert_bad_request(resp, 'invalid argument "ttl"')

    def test_error_negative_ttl(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args(ttl=-10))
        assert_bad_request(resp, 'invalid characters in argument "ttl"')

    def test_error_non_numeric_ttl(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args(ttl="abcde"))
        assert_bad_request(resp, 'invalid characters in argument "ttl"')

    def test_error_invalid_ttl_error_fcm_response(self):
        self.set_fcm_response(fcm_response.from_error("InvalidTtl"))
        resp = self.send(**self.args())
        assert_bad_gateway_error(resp, json.dumps({"error": "InvalidTtl"}, separators=(",", ":")))

    def test_error_invalid_json_body_fcm_response(self):
        self.set_fcm_response(fcm_response.invalid_json_in_body)
        resp = self.send(**self.args())
        assert_bad_gateway_error(resp)

    def test_error_invalid_data_type_fcm_response(self):
        self.set_fcm_response(fcm_response.invalid_data_field_in_body)
        resp = self.send(**self.args(payload={"from": "smth"}))
        assert_bad_gateway_error(resp)

    def test_error_empty_data(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args(payload={}))
        assert_bad_request(resp, "invalid payload")

    def test_error_invalid_keys_in_data_fcm_response(self):
        self.set_fcm_response(fcm_response.from_error("InvalidDataKey"))
        resp = self.send(**self.args(payload={"from": "smth"}))
        assert_bad_gateway_error(
            resp, json.dumps({"error": "InvalidDataKey"}, separators=(",", ":"))
        )

    def test_error_message_is_too_big_fcm_response(self):
        self.set_fcm_response(fcm_response.from_error("MessageTooBig"))
        resp = self.send(**self.args())
        assert_bad_gateway_error(
            resp, json.dumps({"error": "MessageTooBig"}, separators=(",", ":"))
        )

    def test_error_service_unavailable_fcm_responses(self):
        for key in set(
            [
                "DeviceMessageRateExceeded",
                "TopicsMessageRateExceeded",
                "Unavailable",
                "InternalServerError",
            ]
        ):
            yield self.check_with_error, json.dumps(
                {"error": key}, separators=(",", ":")
            ), fcm_response.from_error(key)

    def test_error_service_5xx_fcm_responses(self):
        for key in set(["Unavailable5XX", "InternalServerError5XX"]):
            yield self.check_with_error, "cloud error", fcm_response.from_error(key)

    def check_with_error(self, key, fcm_resp):
        self.setup()
        self.set_fcm_response(fcm_resp)
        resp = self.send(**self.args())
        assert_bad_gateway_error(
            resp,
        )
        self.teardown()

    def test_send_to_topic_ok(self):
        self.set_fcm_response(fcm_response.topic_success)
        resp = self.send(**self.args(token="/topics/test"))
        assert_ok(resp, '{"message_id":"1"}')

    def test_error_service_unavailable_topic_fcm_responses(self):
        for key in set(
            [
                "DeviceMessageRateExceeded",
                "TopicsMessageRateExceeded",
                "Unavailable",
                "InternalServerError",
            ]
        ):
            yield self.check_with_error, json.dumps(
                {"error": key}, separators=(",", ":")
            ), fcm_response.topic_from_error(key)

    def test_error_service_5xx_topic_fcm_responses(self):
        for key in set(["Unavailable5XX", "InternalServerError5XX"]):
            yield self.check_with_error, "cloud error", fcm_response.topic_from_error(key)

    def test_blacklisted_token(self):
        resp = self.send(**self.args(token="BLACKLISTED"))
        eq_(resp.status, 205)
