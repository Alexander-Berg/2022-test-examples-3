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


class TestSendWns(APITestBase):
    def setup(self):
        super(self.__class__, self).set_args(
            app="ru.yandex.test", token="http://localhost:19998", payload="123", ttl=3600
        )
        self.xivamob = XivamobAPI()
        self.fake_push = fake_push
        self.fake_token = fake_token
        self.token_value = "12345"
        self.fake_token.set_response_code(200)
        self.fake_token.set_response(
            raw_response=json.dumps(
                {"token_type": "bearer", "access_token": self.token_value, "expires_in": 86400}
            )
        )
        self.set_wns_response(200)

    def teardown(self):
        error_in_hook = self.fake_push.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook

    def send(self, **kwargs):
        return self.xivamob.send_wns(**kwargs)

    def set_wns_response(self, code, raw_body=""):
        self.fake_push.set_response(raw_response=raw_body)
        self.fake_push.set_response_code(code)

    def check_request(self, req, expected_types, expected_values):
        headers = req.headers
        assert_in("Authorization", headers)
        assert_in("Content-Type", headers)
        assert_in("Content-Length", headers)
        assert_in("X-WNS-Type", headers)
        assert_in("X-WNS-TTL", headers)
        eq_(headers["Authorization"], "Bearer " + self.token_value)
        eq_(headers["X-WNS-TTL"], "3600")
        if len(expected_types) != len(expected_values):
            ok_(False, "invalid expected")
        if self.request_count >= len(expected_types):
            ok_(False, "too many requests")
        eq_(headers["X-WNS-Type"], expected_types[self.request_count])
        eq_(req.body, expected_values[self.request_count])
        self.increment_request_counter()

    def increment_request_counter(self):
        self.request_count += 1

    def test_send_check(self):
        self.fake_push.set_request_hook(
            lambda (req): self.check_request(
                req,
                ["wns/toast", "wns/raw", "wns/tile", "wns/badge"],
                [
                    '<toast><visual><binding template="ToastText01"><text id="1">test</text></binding></visual></toast>',
                    "123",
                    '<tile><visual><binding><text id="1">test</text></binding></visual></tile>',
                    '<badge value="test"/>',
                ],
            )
        )
        self.request_count = 0
        resp = self.send(
            **self.args(
                **{
                    "x-toast": "test",
                    "x-badge": "test",
                    "x-tile": json.dumps({"visual": {"binding": {"text": "test"}}}),
                }
            )
        )
        sleep(0.1)
        eq_(self.request_count, 4)
        assert_ok(resp)
