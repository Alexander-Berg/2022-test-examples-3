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

    def test_create_service(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0", "0", "0", "0", "0", "0", "0", "0", "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=test-svc&type=service&owner=test_600&token=&environment=",
                    "body": [
                        "test-svc",
                        "test_",
                        "600",
                        "qqqqqqqqqqqqqqqqqqqqqqqqq",
                        True,
                        ["xxx"],
                        False,
                        0,
                        False,
                        False,
                        True,
                        {},
                        {},
                    ],
                },
                {
                    "path": "/put\?name=test-svc:default&type=send_token&owner=test-svc&token=.{40}&environment=sandbox",
                    "body": [["test-svc", "default", False]],
                },
                {
                    "path": "/put\?name=test-svc:default&type=listen_token&owner=test-svc&token=.{40}&environment=sandbox",
                    "body": [["test-svc", "default", False], "test-svc", []],
                },
                {
                    "path": "/put\?name=test-svc:default&type=send_token&owner=test-svc&token=.{40}&environment=corp",
                    "body": [["test-svc", "default", False]],
                },
                {
                    "path": "/put\?name=test-svc:default&type=listen_token&owner=test-svc&token=.{40}&environment=corp",
                    "body": [["test-svc", "default", False], "test-svc", []],
                },
                {
                    "path": "/put\?name=test-svc:default&type=send_token&owner=test-svc&token=.{40}&environment=production",
                    "body": [["test-svc", "default", False]],
                },
                {
                    "path": "/put\?name=test-svc:default&type=listen_token&owner=test-svc&token=.{40}&environment=production",
                    "body": [["test-svc", "default", False], "test-svc", []],
                },
            ],
        )
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {"uid": "600"},
            {
                "name": "svc",
                "oauth_scopes": ["xxx"],
                "description": "qqqqqqqqqqqqqqqqqqqqqqqqq",
                "is_passport": True,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"]["name"], "test-svc")
        assert_equal(len(result["result"]["send_tokens"]), 3)
        for env, tokens in result["result"]["send_tokens"].items():
            for token, data in tokens.items():
                assert_equal(len(token), 40)
                assert_equal(data["name"], "default")

        assert_equal(len(result["result"]["listen_tokens"]), 3)
        for env, tokens in result["result"]["listen_tokens"].items():
            for token, data in tokens.items():
                assert_equal(len(token), 40)
                assert_equal(data["name"], "default")
                assert_equal(data["client"], "test-svc")

    def test_create_service_abc(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0", "0", "0", "0", "0", "0", "0", "0", "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=test-svc&type=service&owner=abc:some-service&token=&environment=",
                    "body": [
                        "test-svc",
                        "abc:",
                        "some-service",
                        "qqqqqqqqqqqqqqqqqqqqqqqqq",
                        False,
                        [],
                        False,
                        0,
                        False,
                        True,
                        True,
                        {},
                        {},
                    ],
                },
                {
                    "path": "/put\?name=test-svc:default&type=send_token&owner=test-svc&token=.{40}&environment=sandbox",
                    "body": [["test-svc", "default", False]],
                },
                {
                    "path": "/put\?name=test-svc:default&type=listen_token&owner=test-svc&token=.{40}&environment=sandbox",
                    "body": [["test-svc", "default", False], "test-svc", []],
                },
                {
                    "path": "/put\?name=test-svc:default&type=send_token&owner=test-svc&token=.{40}&environment=corp",
                    "body": [["test-svc", "default", False]],
                },
                {
                    "path": "/put\?name=test-svc:default&type=listen_token&owner=test-svc&token=.{40}&environment=corp",
                    "body": [["test-svc", "default", False], "test-svc", []],
                },
                {
                    "path": "/put\?name=test-svc:default&type=send_token&owner=test-svc&token=.{40}&environment=production",
                    "body": [["test-svc", "default", False]],
                },
                {
                    "path": "/put\?name=test-svc:default&type=listen_token&owner=test-svc&token=.{40}&environment=production",
                    "body": [["test-svc", "default", False], "test-svc", []],
                },
            ],
        )
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {"owner": "some-service", "owner_type": "abc"},
            {
                "name": "svc",
                "oauth_scopes": ["xxx"],
                "description": "qqqqqqqqqqqqqqqqqqqqqqqqq",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": True,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"]["name"], "test-svc")
        assert_equal(len(result["result"]["send_tokens"]), 3)
        for env, tokens in result["result"]["send_tokens"].items():
            for token, data in tokens.items():
                assert_equal(len(token), 40)
                assert_equal(data["name"], "default")

        assert_equal(len(result["result"]["listen_tokens"]), 3)
        for env, tokens in result["result"]["listen_tokens"].items():
            for token, data in tokens.items():
                assert_equal(len(token), 40)
                assert_equal(data["name"], "default")
                assert_equal(data["client"], "test-svc")

    def test_create_service_must_be_admin(self):
        self.auth_one_user(600)
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {},
            {
                "name": "svc",
                "oauth_scopes": ["xxx"],
                "description": "qqqqqqqqqqqqqqqqqqqqqqqqq",
                "is_passport": True,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_equal(self.xconf_server.total_requests, 0)
        assert_unauthorized(resp, '{"error":"must be admin"}')

    def test_create_stream_service(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0", "0", "0", "0", "0", "0", "0", "0", "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=test-svc&type=service&owner=test_800&token=&environment=",
                    "body": [
                        "test-svc",
                        "test_",
                        "800",
                        "qqqqqqqqqqqqqqqqqqqqqqqqq",
                        False,
                        [],
                        True,
                        12,
                        False,
                        False,
                        True,
                        {},
                        {},
                    ],
                },
                {
                    "path": "/put\?name=test-svc:default&type=send_token&owner=test-svc&token=.{40}&environment=sandbox",
                    "body": [["test-svc", "default", False]],
                },
                {
                    "path": "/put\?name=test-svc:default&type=listen_token&owner=test-svc&token=.{40}&environment=sandbox",
                    "body": [["test-svc", "default", False], "test-svc", []],
                },
                {
                    "path": "/put\?name=test-svc:default&type=send_token&owner=test-svc&token=.{40}&environment=corp",
                    "body": [["test-svc", "default", False]],
                },
                {
                    "path": "/put\?name=test-svc:default&type=listen_token&owner=test-svc&token=.{40}&environment=corp",
                    "body": [["test-svc", "default", False], "test-svc", []],
                },
                {
                    "path": "/put\?name=test-svc:default&type=send_token&owner=test-svc&token=.{40}&environment=production",
                    "body": [["test-svc", "default", False]],
                },
                {
                    "path": "/put\?name=test-svc:default&type=listen_token&owner=test-svc&token=.{40}&environment=production",
                    "body": [["test-svc", "default", False], "test-svc", []],
                },
            ],
        )
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {},
            {
                "name": "svc",
                "description": "qqqqqqqqqqqqqqqqqqqqqqqqq",
                "is_passport": False,
                "is_stream": True,
                "queued_delivery_by_default": True,
                "stream_count": 12,
                "auth_disabled": False,
            },
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"]["name"], "test-svc")

    def test_create_service_bad_name(self):
        for name in ["1svc", "-svc", "svc_1", "svc.1", "qqqqqqqqqqqqqqqqqqqqqqqqqq"]:
            yield self.check_create_service_bad_name, name

    def check_create_service_bad_name(self, name):
        self.auth_one_user(800)
        self.set_xconf_responses([packb([0, []])])
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {},
            {
                "name": name,
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(resp, dumps({"error": "invalid service name"}, separators=(",", ":")))
        assert_equal(self.xconf_server.total_requests, 0)

    def test_create_service_incompatible_properties(self):
        for param in [
            (True, True, False),
            (True, False, True),
            (False, True, True),
            (True, True, True),
        ]:
            yield self.check_create_service_incompatible_properties, param

    def check_create_service_incompatible_properties(self, param):
        self.auth_one_user(800)
        self.set_xconf_responses([packb([0, []])])
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {},
            {
                "name": "xxx",
                "is_passport": param[0],
                "oauth_scopes": [],
                "is_stream": param[1],
                "stream_count": 2,
                "auth_disabled": param[2],
            },
        )
        assert_bad_request(
            resp,
            dumps(
                {
                    "error": "service can only have one property of passport, stream and auth_disabled"
                },
                separators=(",", ":"),
            ),
        )
        assert_equal(self.xconf_server.total_requests, 0)

    def check_create_service_bad_description(self):
        self.auth_one_user(800)
        self.set_xconf_responses([packb([0, []])])
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {},
            {
                "name": "qwe",
                "description": "qqqqqqqqqqqqqqqqqqqqqqqqqq",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(
            resp, dumps({"error": "invalid service description"}, separators=(",", ":"))
        )
        assert_equal(self.xconf_server.total_requests, 0)

    def check_create_service_bad_scopes(self):
        self.auth_one_user(800)
        self.set_xconf_responses([packb([0, []]), packb([0, []])])
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {},
            {
                "name": "qwe",
                "oauth_scopes": ["1", "2", "3"],
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(resp, dumps({"error": "invalid OAuth scopes"}, separators=(",", ":")))
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {},
            {
                "name": "qwe",
                "oauth_scopes": ['"#!/\\'],
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(resp, dumps({"error": "invalid OAuth scopes"}, separators=(",", ":")))
        assert_equal(self.xconf_server.total_requests, 0)

    def test_create_service_already_exists_error(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []])], [{"path": "/list\?type=any&revision=\\d+", "body": ""}]
        )
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {},
            {
                "name": "tst1",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "service already exists")

    def test_create_service_xconf_error_list(self):
        self.auth_one_user(800)
        self.set_xconf_responses([""], [{"path": "/list\?type=any&revision=\\d+", "body": ""}])
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {},
            {
                "name": "svctest",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "transport error")

    def test_create_service_xconf_error_put1(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), ""],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=test-svc&type=service&owner=test_800&token=&environment=",
                    "body": [
                        "test-svc",
                        "test_",
                        "800",
                        "",
                        False,
                        [],
                        False,
                        0,
                        False,
                        False,
                        True,
                        {},
                        {},
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {},
            {
                "name": "svc",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "transport error")

    def test_create_service_xconf_error_put2(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0", ""],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=test-svc&type=service&owner=test_600&token=&environment=",
                    "body": [
                        "test-svc",
                        "test_",
                        "600",
                        "",
                        False,
                        [],
                        False,
                        0,
                        False,
                        False,
                        True,
                        {},
                        {},
                    ],
                },
                {
                    "path": "/put\?name=test-svc:default&type=send_token&owner=test-svc&token=.{40}&environment=sandbox",
                    "body": [["test-svc", "default", False]],
                },
            ],
        )
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {"uid": "600"},
            {
                "name": "svc",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_equal(result["error"], "transport error")
        assert_equal(
            result["result"],
            {
                "apps": [],
                "description": "",
                "is_passport": False,
                "owner": "test_600",
                "tvm_publishers": {},
                "tvm_subscribers": {},
                "listen_tokens": {},
                "send_tokens": {},
                "is_stream": False,
                "queued_delivery_by_default": True,
                "name": "test-svc",
                "oauth_scopes": [],
                "revoked": False,
                "stream_count": 0,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )

    def test_create_service_xconf_error_put3(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0", "0", ""],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=test-svc&type=service&owner=test_600&token=&environment=",
                    "body": [
                        "test-svc",
                        "test_",
                        "600",
                        "",
                        False,
                        [],
                        False,
                        0,
                        False,
                        False,
                        True,
                        {},
                        {},
                    ],
                },
                {
                    "path": "/put\?name=test-svc:default&type=send_token&owner=test-svc&token=.{40}&environment=sandbox",
                    "body": [["test-svc", "default", False]],
                },
                {
                    "path": "/put\?name=test-svc:default&type=listen_token&owner=test-svc&token=.{40}&environment=sandbox",
                    "body": [["test-svc", "default", False], "test-svc", []],
                },
            ],
        )
        resp, result = xiva_webui_api.service_create(
            self.headers,
            {"uid": "600"},
            {
                "name": "svc",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_equal(result["error"], "transport error")
        assert_equal(result["result"]["name"], "test-svc")
        assert_in("sandbox", result["result"]["send_tokens"])
        assert_equal(len(result["result"]["send_tokens"]["sandbox"]), 1)
        assert_equal(len(result["result"]["listen_tokens"]), 0)
