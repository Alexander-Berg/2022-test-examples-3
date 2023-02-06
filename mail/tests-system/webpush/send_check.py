# flake8: noqa
import pytest
from pycommon.xivamob_api import *
from pycommon.fake_server import *
from pycommon.api_test_base import *
from pycommon.assert_response import *
from nose.tools import *
from time import sleep
import json


def setup_module():
    global fake_push
    fake_push = pytest.fake_provider


message_expired_unauthorized = "subscription expired or unauthorized"
message_unauthorized = "invalid subscription or endpoint"
message_bad_data = "data compose error"


class TestSendWebpush(APITestBase):
    def setup(self):
        super(self.__class__, self).set_args(
            subscription=self.make_sub(
                "http://localhost:19998",
                "w9iDFelHU7suN8v7xo5oZA==",
                "BPj3wE8moDU6P-KZBWogZnQ7XDlZpJsGw0tcw8KlQl_VvVRGakpSu_JMk4zcxbG5wOapQauBFN8qVUEwN9RzaYc=",
            ),
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

    def get_header(self, req, header):
        return req.headers.getheader(header, None)

    def check_webpush_request_basic(self, req, with_payload=False):
        auth = self.get_header(req, "Authorization")
        crypto_key = self.get_header(req, "Crypto-Key")
        ttl = self.get_header(req, "TTL")
        assert_is_not_none(auth)
        assert_is_not_none(crypto_key)
        assert_is_not_none(ttl)
        assert_true(auth.startswith("WebPush "))
        vapid = auth[8:].split(".")
        eq_(len(vapid), 3)
        assert_not_equal(crypto_key.find("p256ecdsa="), -1)
        try:
            int(ttl)
        except Exception as ex:
            fail_("ttl is not a number")
        if not with_payload:
            assert_is_none(self.get_header(req, "Encryption"))
            assert_is_none(self.get_header(req, "Content-Encoding"))
            eq_(req.body, "")
            eq_(crypto_key.find("dh="), -1)
            eq_(crypto_key.find(";"), -1)

    def check_webpush_and_payload(self, req):
        self.check_webpush_request_basic(req, True)
        assert_equals(self.get_header(req, "X-Request-ID"), "transitidtest")
        crypto_key = self.get_header(req, "Crypto-Key")
        assert_not_equal(req.body, "")
        assert_not_equal(crypto_key.find("dh="), -1)
        assert_not_equal(crypto_key.find(";"), -1)
        encryption = self.get_header(req, "Encryption")
        encoding = self.get_header(req, "Content-Encoding")
        assert_is_not_none(encryption)
        assert_is_not_none(encoding)
        eq_(encoding, "aesgcm")
        assert_true(encryption.startswith("salt="))

    def test_check_headers_no_payload(self):
        self.fake_push.set_request_hook(lambda (req): self.check_webpush_request_basic(req))
        resp = self.send(**self.args())
        assert_ok(resp)

    def test_check_with_payload(self):
        self.fake_push.set_request_hook(lambda (req): self.check_webpush_and_payload(req))
        resp = self.send(**self.args(payload="payload test", transit_id="transitidtest"))
        assert_ok(resp)
