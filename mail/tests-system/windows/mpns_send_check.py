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

    def check_request(self, req, expected_types, expected_values):
        headers = req.headers
        assert_in("Content-Type", headers)
        eq_(headers["Content-Type"], "text/xml")
        if len(expected_types) != len(expected_values):
            ok_(False, "invalid expected")
        if self.request_count >= len(expected_types):
            ok_(False, "too many requests")
        expected_type = expected_types[self.request_count]
        if expected_type is not None:
            assert_in("X-WindowsPhone-Target", headers)
            eq_(headers["X-WindowsPhone-Target"], expected_type)
        eq_(req.body, expected_values[self.request_count])
        self.increment_request_counter()

    def increment_request_counter(self):
        self.request_count += 1

    def test_send_check(self):
        self.fake_push.set_request_hook(
            lambda (req): self.check_request(
                req,
                ["toast", None, "token"],
                [
                    '<wp:Notification xmlns:wp="WPNotification"><wp:Toast><wp:Text1>test</wp:Text1></wp:Toast></wp:Notification>',
                    "123",
                    '<wp:Notification xmlns:wp="WPNotification"><wp:Tile><wp:Field2 Action="Clear"></wp:Field2><wp:Field1>val1</wp:Field1></wp:Tile></wp:Notification>',
                ],
            )
        )
        self.request_count = 0
        resp = self.send(
            **self.args(
                **{"x-toast": "test", "x-tile": json.dumps({"Field1": "val1", "clear": "Field2"})}
            )
        )
        sleep(0.1)
        eq_(self.request_count, 3)
        assert_ok(resp)
