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


message_invalid_subscription = "invalid subscription or endpoint"


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

    def test_send_bad_ttl(self):
        resp = self.send(**self.args(ttl="lol"))
        assert_bad_request(resp, 'invalid characters in argument "ttl"')

    def test_send_invalid_subscription_bad_auth(self):
        resp = self.send(
            **self.args(
                subscription=self.make_sub(
                    "http://localhost:19998",
                    "bad auth",
                    "BPj3wE8moDU6P-KZBWogZnQ7XDlZpJsGw0tcw8KlQl_VvVRGakpSu_JMk4zcxbG5wOapQauBFN8qVUEwN9RzaYc=",
                )
            )
        )
        assert_internal_server_error(resp, "internal error")

    def test_send_invalid_subscription_bad_key(self):
        resp = self.send(
            **self.args(
                subscription=self.make_sub(
                    "http://localhost:19998", "w9iDFelHU7suN8v7xo5oZA", "bad_key"
                )
            )
        )
        assert_internal_server_error(resp, "internal error")

    def test_send_invalid_subscription_empty_auth(self):
        resp = self.send(
            **self.args(
                subscription=self.make_sub(
                    "http://localhost:19998",
                    "",
                    "BPj3wE8moDU6P-KZBWogZnQ7XDlZpJsGw0tcw8KlQl_VvVRGakpSu_JMk4zcxbG5wOapQauBFN8qVUEwN9RzaYc=",
                )
            )
        )
        assert_code_msg(resp, 205, message_invalid_subscription)

    def test_send_invalid_subscription_empty_key(self):
        resp = self.send(
            **self.args(
                subscription=self.make_sub("http://localhost:19998", "w9iDFelHU7suN8v7xo5oZA", "")
            )
        )
        assert_code_msg(resp, 205, message_invalid_subscription)

    def test_send_invalid_subscription_missing_auth(self):
        resp = self.send(
            **self.args(
                subscription=json.dumps(
                    {
                        "endpoint": "http://localhost:19998",
                        "keys": {
                            "p256dh": "BPj3wE8moDU6P-KZBWogZnQ7XDlZpJsGw0tcw8KlQl_VvVRGakpSu_JMk4zcxbG5wOapQauBFN8qVUEwN9RzaYc="
                        },
                    }
                )
            )
        )
        assert_code_msg(resp, 205, message_invalid_subscription)

    def test_send_invalid_subscription_missing_key(self):
        resp = self.send(
            **self.args(
                subscription=json.dumps(
                    {
                        "endpoint": "http://localhost:19998",
                        "keys": {"auth": "w9iDFelHU7suN8v7xo5oZA"},
                    }
                )
            )
        )
        assert_code_msg(resp, 205, message_invalid_subscription)

    def test_send_invalid_subscription_empty_keys(self):
        resp = self.send(
            **self.args(subscription=json.dumps({"endpoint": "http://localhost:19998", "keys": ""}))
        )
        assert_code_msg(resp, 205, message_invalid_subscription)

    def test_send_invalid_subscription_missing_keys(self):
        resp = self.send(
            **self.args(subscription=json.dumps({"endpoint": "http://localhost:19998"}))
        )
        assert_code_msg(resp, 205, message_invalid_subscription)

    def test_send_invalid_endpoint(self):
        resp = self.send(
            **self.args(
                subscription=self.make_sub(
                    "@#$%^",
                    "w9iDFelHU7suN8v7xo5oZA==",
                    "BPj3wE8moDU6P-KZBWogZnQ7XDlZpJsGw0tcw8KlQl_VvVRGakpSu_JMk4zcxbG5wOapQauBFN8qVUEwN9RzaYc=",
                )
            )
        )
        assert_internal_server_error(resp)

    def test_send_empty_endpoint(self):
        resp = self.send(
            **self.args(
                subscription=self.make_sub(
                    "",
                    "w9iDFelHU7suN8v7xo5oZA==",
                    "BPj3wE8moDU6P-KZBWogZnQ7XDlZpJsGw0tcw8KlQl_VvVRGakpSu_JMk4zcxbG5wOapQauBFN8qVUEwN9RzaYc=",
                )
            )
        )
        assert_code_msg(resp, 205, message_invalid_subscription)

    def test_send_missing_endpoint(self):
        resp = self.send(
            **self.args(
                subscription=json.dumps(
                    {
                        "keys": {
                            "auth": "w9iDFelHU7suN8v7xo5oZA",
                            "p256dh": "BPj3wE8moDU6P-KZBWogZnQ7XDlZpJsGw0tcw8KlQl_VvVRGakpSu_JMk4zcxbG5wOapQauBFN8qVUEwN9RzaYc=",
                        }
                    }
                )
            )
        )
        assert_code_msg(resp, 205, message_invalid_subscription)

    def test_send_bad_endpoint(self):
        resp = self.send(
            **self.args(
                subscription=self.make_sub(
                    "http://localhost:1234",
                    "w9iDFelHU7suN8v7xo5oZA==",
                    "BPj3wE8moDU6P-KZBWogZnQ7XDlZpJsGw0tcw8KlQl_VvVRGakpSu_JMk4zcxbG5wOapQauBFN8qVUEwN9RzaYc=",
                )
            )
        )
        assert_internal_server_error(resp)

    def test_send_invalid_subscription(self):
        resp = self.send(
            **self.args(
                subscription=json.dumps(
                    {
                        "endpoint": ["random json"],
                        "keys": {"auth": ["very bad"], "p256dh": "subscription"},
                    }
                )
            )
        )
        assert_code_msg(resp, 205, message_invalid_subscription)

    def test_send_subscription_not_json(self):
        resp = self.send(**self.args(subscription="not json at all"))
        assert_code_msg(resp, 205, message_invalid_subscription)

    def test_send_missing_subscription(self):
        resp = self.send(ttl=3600, payload="hello!")
        assert_bad_request(resp, 'missing argument "subscription"')
