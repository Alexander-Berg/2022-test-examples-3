# flake8: noqa
import pytest
from library.python import resource
from pycommon.xivamob_api import *
from pycommon.fake_server import *
from pycommon.api_test_base import *
from pycommon.assert_response import *
import json
import fcm_response


def setup_module():
    global fake_fcm, test_data
    fake_fcm = pytest.fake_provider
    data_str = resource.find("resfs/file/mail/xiva/mobile/tests-system/fcm/batch.json")
    test_data = json.loads(data_str)


class TestBatchSendFCM(APITestBase):
    def setup(self):
        super(self.__class__, self).set_args(
            app="xiva.mob.send.test", payload={"a": "b"}, tokens=json.dumps(["a", "b", "c"])
        )
        self.xivamob = XivamobAPI()
        self.fake_fcm = fake_fcm
        self.set_fcm_response(fcm_response.success)

    def teardown(self):
        error_in_hook = self.fake_fcm.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook

    def send(self, **kwargs):
        return self.xivamob.send_batch_fcm(**kwargs)

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
        assert_ok(resp, None)

    def test_check_request(self):
        expect = {"to": None, "registration_ids": ["a", "b", "c"]}
        self.fake_fcm.set_request_hook(lambda (req): self.check_fcm_requests(req, expect))
        resp = self.send(**self.args())
        assert_ok(resp, None)

    def test_error_unknown_app(self):
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args(app="unknown.app.test"))
        assert_code_msg(resp, 403, "no certificate")

    def test_error_invalid_or_expired_api_key(self):
        self.set_fcm_response(fcm_response.unauthorized)
        resp = self.send(**self.args())
        assert_code_msg(resp, 403, "invalid certificate")

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

    def test_error_invalid_tokens(self):
        inputs = [
            json.dumps(["ok", {"bad": "object"}]),
            json.dumps({"bad": "object"}),
            "{bad json",
            "[]",
            "",
            None,
        ]
        results = [
            'invalid "tokens" field: array of strings expected',
            'field "tokens" must be an array',
            'invalid "tokens" field: JSON parse error: Missing a name for object member. 1',
            'missing argument "tokens"',
            'missing argument "tokens"',
            'missing argument "tokens"',
        ]
        eq_(len(inputs), len(results))
        for i in range(len(inputs)):
            yield self.check_tokens_bad_request, inputs[i], results[i]

    def check_tokens_bad_request(self, tokens, result):
        self.setup()
        self.fake_fcm.set_request_hook(lambda (req): fail_("Unexpected request"))
        resp = self.send(**self.args(tokens=tokens))
        assert_bad_request(resp, result)
        self.teardown()

    def test_error_service_unavailable_fcm_responses(self):
        for key in set(["Unavailable5XX", "InternalServerError5XX"]):
            yield self.check_with_error, fcm_response.from_error(key)

    def check_with_error(self, fcm_resp):
        self.setup()
        self.set_fcm_response(fcm_resp)
        resp = self.send(**self.args())
        assert_bad_gateway_error(resp, "cloud error")
        self.teardown()

    def test_different_inputs(self):
        global test_data
        inputs = test_data["fcm_response"]
        outputs = test_data["expect"]
        eq_(len(inputs), len(outputs))
        for i in range(len(inputs)):
            yield self.check_input_output, inputs[i], outputs[i]

    def check_input_output(self, inp, out):
        self.setup()
        self.set_fcm_response((200, json.dumps(inp)))
        resp = self.send(**self.args())
        eq_(resp.status, 200)
        eq_(resp.body, json.dumps(out, separators=(",", ":"), sort_keys=True))
        self.teardown()
