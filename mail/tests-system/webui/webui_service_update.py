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

    def auth_two_users(self, uid, child_uid):
        self.wmi_server.set_response(
            raw_response=dumps({"check_cookies": {"uid": str(uid), "childUids": [str(child_uid)]}})
        )

    def auth_services(self, *services):
        self.abc_server.set_response(
            raw_response=dumps({"results": [{"service": {"slug": s}} for s in services]})
        )

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

    def test_update_service(self):
        self.auth_one_user(400)
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
            ],
        )
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {},
            {
                "name": "fake",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": True,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], "success")

    def test_update_abc_service(self):
        self.auth_one_user(400)
        self.auth_services("abc-service")
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=test-abc&type=service&owner=abc:abc-service&token=&environment=",
                    "body": [
                        "test-abc",
                        "abc:",
                        "abc-service",
                        "",
                        False,
                        [],
                        False,
                        0,
                        False,
                        False,
                        True,
                        {"sandbox": [[12345, "unknown", False]]},
                        {},
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"owner": "abc-service", "owner_type": "abc"},
            {
                "name": "test-abc",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], "success")

    def test_update_abc_service_unauthorized(self):
        self.auth_one_user(400)
        self.auth_services("abc-nope")
        self.set_xconf_responses([packb([0, []])])
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"owner": "abc-service", "owner_type": "abc"},
            {
                "test-abc": "fake",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_unauthorized(resp, '{"error":"not a member of provided service"}')
        assert_equal(self.xconf_server.total_requests, 0)

    def test_update_service_owner_mismatch(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []])], [{"path": "/list\?type=any&revision=\\d+&environment=", "body": ""}]
        )
        resp, result = xiva_webui_api.service_update(
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
        assert_bad_request(resp, dumps({"error": "service owner mismatch"}, separators=(",", ":")))
        assert_equal(self.xconf_server.total_requests, 0)

    def test_update_service_other_owner(self):
        self.auth_one_user(200)
        self.set_xconf_responses([packb([0, []])])
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"uid": "400"},
            {
                "name": "fake",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_equal(self.xconf_server.total_requests, 0)
        assert_unauthorized(resp, '{"error":"requested uid is not authorized"}')
        assert_equal(self.xconf_server.total_requests, 0)

    def test_update_service_by_admin(self):
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
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"uid": "400"},
            {
                "name": "fake",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], "success")

    def test_update_service_incompatible_properties(self):
        for param in [
            (True, True, False),
            (True, False, True),
            (False, True, True),
            (True, True, True),
        ]:
            yield self.check_update_service_incompatible_properties, param

    def check_update_service_incompatible_properties(self, param):
        self.auth_one_user(800)
        self.set_xconf_responses([packb([0, []])])
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"uid": "400"},
            {
                "name": "fake",
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

    def test_update_service_bad_stream_count(self):
        self.auth_one_user(400)
        self.set_xconf_responses([packb([0, []])])
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"uid": "400"},
            {
                "name": "fake",
                "is_passport": False,
                "is_stream": True,
                "stream_count": 0,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(
            resp, dumps({"error": "invalid field stream_count"}, separators=(",", ":"))
        )
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"uid": "400"},
            {
                "name": "fake",
                "is_passport": False,
                "is_stream": True,
                "stream_count": -10,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(
            resp, dumps({"error": "invalid field stream_count"}, separators=(",", ":"))
        )
        assert_equal(self.xconf_server.total_requests, 0)

    def test_update_service_bad_scopes(self):
        self.auth_one_user(400)
        self.set_xconf_responses([packb([0, []])])
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"uid": "400"},
            {
                "name": "fake",
                "is_passport": True,
                "is_stream": False,
                "oauth_scopes": ['"'],
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(resp, dumps({"error": "invalid OAuth scopes"}, separators=(",", ":")))
        assert_equal(self.xconf_server.total_requests, 0)

    def test_update_service_doesnt_exist_error(self):
        self.auth_one_user(400)
        self.set_xconf_responses(
            [packb([0, []])], [{"path": "/list\?type=any&revision=\\d+&environment=", "body": ""}]
        )
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"uid": "400"},
            {
                "name": "__fake",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(resp, dumps({"error": "service doesn't exist"}, separators=(",", ":")))

    def test_update_existing_token(self):
        self.auth_one_user(400)
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
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"uid": "400"},
            {
                "name": "fake",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], "success")

    def test_update_service_streamness_admin_only(self):
        self.auth_one_user(400)
        self.set_xconf_responses([packb([0, []])])
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {},
            {
                "name": "fake",
                "is_passport": False,
                "is_stream": True,
                "stream_count": 123,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(resp, '{"error":"must be admin to edit this field"}')
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {},
            {
                "name": "tst_stream",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(resp, '{"error":"must be admin to edit this field"}')
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {},
            {
                "name": "tst_stream",
                "is_passport": False,
                "is_stream": True,
                "stream_count": 11,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(resp, '{"error":"must be admin to edit this field"}')
        assert_equal(self.xconf_server.total_requests, 0)
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
                        False,
                        [],
                        True,
                        123,
                        False,
                        False,
                        True,
                        {},
                        {},
                    ],
                },
            ],
        )
        self.auth_one_user(800)
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"uid": "400"},
            {
                "name": "fake",
                "is_passport": False,
                "is_stream": True,
                "stream_count": 123,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], "success")

    def test_update_service_admin_chown(self):
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
            ],
        )
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"owner": "600"},
            {
                "name": "fake",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], "success")

    def test_update_service_admin_chown_any_abc_service(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=fake&type=service&owner=test_400&token=&environment=",
                    "body": [
                        "fake",
                        "abc:",
                        "someabcservice",
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
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"owner": "someabcservice", "owner_type": "abc"},
            {
                "name": "fake",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], "success")

    def test_update_service_chown_admin_only(self):
        self.auth_two_users(400, 600)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=fake&type=service&owner=test_400&token=&environment=",
                    "body": [
                        "fake",
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
            ],
        )
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"owner": "600"},
            {
                "name": "fake",
                "is_passport": False,
                "is_stream": False,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(resp, dumps({"error": "service owner mismatch"}, separators=(",", ":")))

    def test_update_service_delivery_mode_admin_only(self):
        self.auth_one_user(900)
        self.set_xconf_responses([packb([0, []])])
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {},
            {
                "name": "tst_force_direct",
                "is_passport": False,
                "is_stream": False,
                "stream_count": 0,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_bad_request(resp, '{"error":"must be admin to edit this field"}')
        assert_equal(self.xconf_server.total_requests, 0)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=tst_force_direct&type=service&owner=test_900&token=&environment=",
                    "body": [
                        "tst_force_direct",
                        "test_",
                        "900",
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
        self.auth_one_user(800)
        resp, result = xiva_webui_api.service_update(
            self.headers,
            {"uid": "900"},
            {
                "name": "tst_force_direct",
                "is_passport": False,
                "is_stream": False,
                "stream_count": 0,
                "auth_disabled": False,
                "queued_delivery_by_default": True,
            },
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], "success")
