from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
from json import dumps
from msgpack import packb, unpackb
from urllib import unquote
from re import match
from time import sleep
import certificates
from base64 import b64encode


def setUp(self):
    global xiva_webui_api, wmi_server, xconf_server, abc_server
    xiva_webui_api = XivaWebuiApi(host="localhost", port=18083)
    wmi_server = fake_server(host="localhost", port=17084, raw_response="OK")
    xconf_server = fake_server(host="localhost", port=17083, raw_response=packb([0, []]))
    abc_server = fake_server(host="localhost", port=17085, raw_response=dumps({"results": []}))


def tearDown(self):
    global wmi_server, xconf_server, abc_server
    wmi_server.fini()
    xconf_server.fini()
    abc_server.fini()


class TestWebUi:
    def setup(self):
        global wmi_server, xconf_server, abc_server
        self.wmi_server = wmi_server
        self.abc_server = abc_server
        self.xconf_server = xconf_server
        self.xconf_server.reset_state()
        self.xconf_server.set_response(raw_response=packb([0, []]))
        self.abc_server.set_response(raw_response=dumps({"results": []}))
        self.headers = {"cookie": "123", "Origin": "push.yandex.ru"}

    def teardown(self):
        error_in_hook = self.xconf_server.set_request_hook(None)
        if error_in_hook:
            raise error_in_hook
        sleep(0.05)  # request hook magic

    def auth_one_user(self, uid):
        self.wmi_server.set_response(raw_response=dumps({"check_cookies": {"uid": str(uid)}}))

    def xconf_hook(self, req):
        current_id = self.xconf_response_id
        self.xconf_response_id += 1
        path = unquote(req.path)
        body = unpackb(req.body) if path.startswith("/put") else ""
        print "xconf request %s: %s" % (path, body)
        if self.xconf_requests:
            assert_less(current_id, len(self.xconf_requests))
            expect = self.xconf_requests[current_id]
            print "expected[%d] %s: %s" % (current_id, expect["path"], expect["body"])
            assert_not_equal(
                match(expect["path"], path), None, "%s doesn't match %s" % (path, expect["path"])
            )
            assert_equal(expect["body"], body)

        if self.xconf_response_id < len(self.xconf_responses):
            self.xconf_server.set_response(
                raw_response=self.xconf_responses[self.xconf_response_id]
            )
        else:
            self.xconf_server.set_response(raw_response="")

    def set_xconf_responses(self, responses, requests=None):
        self.xconf_response_id = 0
        self.xconf_responses = responses
        self.xconf_requests = requests
        self.xconf_server.set_response(raw_response=self.xconf_responses[self.xconf_response_id])
        self.xconf_server.set_request_hook(self.xconf_hook)

    def test_revoke_service(self):
        "revoking service doesn't change other fields"
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=fake&type=service&owner=test_400&token=&environment=",
                    "body": [
                        "fake",
                        "test_",
                        "400",
                        "",
                        True,
                        [],
                        False,
                        0,
                        True,
                        False,
                        True,
                        {},
                        {},
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.service_revoke(
            self.headers,
            {"uid": "400"},
            {
                "name": "fake",
                "is_passport": False,
                "oauth_scopes": [],
                "is_stream": True,
                "stream_count": 123,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], "success")

    def test_revoke_and_restore_service_admin_only(self):
        self.auth_one_user(400)
        self.set_xconf_responses([packb([0, []])])
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {},
            {
                "name": "test-tst1",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(resp, '{"error":"must be admin to edit this field"}')
        resp, result = xiva_webui_api.service_revoke(
            self.headers,
            {},
            {
                "name": "fake",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(resp, '{"error":"must be admin to edit this field"}')
        assert_equal(self.xconf_server.total_requests, 0)
