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

    def check_bad_request(self, method, case):
        print case[2]
        data, error = case[:2]
        self.auth_one_user(200)
        self.set_xconf_responses([packb([0, []])], [])
        resp, result = getattr(self, method)({"uid": "200"}, data)
        assert_bad_request(resp, dumps({"error": error}, separators=(",", ":")))

    def test_update_send_token(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=tst1:st1&type=send_token&owner=tst1&token=S001&environment=sandbox",
                    "body": [["tst1", "st1", False]],
                },
            ],
        )
        resp, result = xiva_webui_api.stoken_update(
            self.headers, {"uid": "200", "env": "sandbox"}, {"name": "st1", "service": "tst1"}
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], {"token": "S001"})

    def test_update_send_token_abc(self):
        self.auth_one_user(200)
        self.auth_services("abc-service")
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=test-abc:st1&type=send_token&owner=test-abc&token=123abc&environment=sandbox",
                    "body": [["test-abc", "st1", False]],
                },
            ],
        )
        resp, result = xiva_webui_api.stoken_update(
            self.headers,
            {"owner": "abc-service", "owner_type": "abc", "env": "sandbox"},
            {"name": "st1", "service": "test-abc"},
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], {"token": "123abc"})

    def test_update_send_token_existing_name(self):
        "Don't check name for existing token"
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=check-test:st1_&type=send_token&owner=check-test&token=corp:qwerty&environment=corp",
                    "body": [["check-test", "st1_", False]],
                },
            ],
        )
        resp, result = xiva_webui_api.stoken_update(
            self.headers, {"uid": "800", "env": "corp"}, {"name": "st1_", "service": "check-test"}
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], {"token": "corp:qwerty"})

    def test_update_send_token_environment_aware(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=tst1:st1&type=send_token&owner=tst1&token=corp:S001&environment=corp",
                    "body": [["tst1", "st1", False]],
                },
            ],
        )
        resp, result = xiva_webui_api.stoken_update(
            self.headers, {"uid": "200", "env": "corp"}, {"name": "st1", "service": "tst1"}
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], {"token": "corp:S001"})

    def test_revoke_send_token(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=tst1:st1&type=send_token&owner=tst1&token=S001&environment=sandbox",
                    "body": [["tst1", "st1", True]],
                },
            ],
        )
        resp, result = xiva_webui_api.stoken_revoke(
            self.headers, {"uid": "200", "env": "sandbox"}, {"name": "st1", "service": "tst1"}
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], {"token": "S001"})

    def test_update_send_token_xconf_error_list(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [""], [{"path": "/list\?type=any&revision=\\d+&environment=", "body": ""}]
        )
        resp, result = xiva_webui_api.stoken_update(
            self.headers, {"uid": "200", "env": "sandbox"}, {"name": "st1", "service": "tst1"}
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "transport error")

    def test_update_send_token_xconf_error_put(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []]), ""],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=tst1:st1&type=send_token&owner=tst1&token=S001&environment=sandbox",
                    "body": [["tst1", "st1", False]],
                },
            ],
        )
        resp, result = xiva_webui_api.stoken_update(
            self.headers, {"uid": "200", "env": "sandbox"}, {"name": "st1", "service": "tst1"}
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "transport error")

    def test_update_send_token_service_doesnt_exist(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []])], [{"path": "/list\?type=any&revision=\\d+&environment=", "body": ""}]
        )
        resp, result = xiva_webui_api.stoken_update(
            self.headers, {"uid": "200", "env": "sandbox"}, {"name": "st1", "service": "_tst1"}
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "service not found")

    def test_update_send_token_owner_missmatch(self):
        self.auth_one_user(400)
        self.set_xconf_responses(
            [packb([0, []])], [{"path": "/list\?type=any&revision=\\d+&environment=", "body": ""}]
        )
        resp, result = xiva_webui_api.stoken_update(
            self.headers, {"uid": "400", "env": "sandbox"}, {"name": "st1", "service": "tst1"}
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "service owner mismatch")

    def test_create_send_token(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=tst1:st2&type=send_token&owner=tst1&token=.{40}&environment=corp",
                    "body": [["tst1", "st2", False]],
                },
            ],
        )
        resp, result = xiva_webui_api.stoken_update(
            self.headers, {"uid": "200", "env": "corp"}, {"name": "st2", "service": "tst1"}
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(len(result["result"]["token"]), 40)

    def test_create_send_token_bad_env(self):
        self.auth_one_user(200)
        self.set_xconf_responses([packb([0, []])], [])
        resp, result = xiva_webui_api.stoken_update(
            self.headers, {"uid": "200", "env": "fake"}, {"name": "st2", "service": "tst1"}
        )
        assert_ok(resp)
        assert_equal(self.xconf_server.total_requests, 0)
        assert_not_in("result", result)
        assert_equal(result["error"], "unknown environment")

    def test_update_listen_token(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=tst1:tst-l1&type=listen_token&owner=tst1&token=L001&environment=sandbox",
                    "body": [["tst1", "tst-l1", False], "tst-l1", []],
                },
            ],
        )
        resp, result = xiva_webui_api.ltoken_update(
            self.headers,
            {"uid": "200", "env": "sandbox"},
            {"name": "tst-l1", "service": "tst1", "client": "tst-l1"},
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], {"token": "L001"})

    def test_update_listen_token_existing_name(self):
        "Don't check name for existing ltoken"
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=check-test:lt1_&type=listen_token&owner=check-test&token=production:asdfg&environment=production",
                    "body": [["check-test", "lt1_", False], "check-test", []],
                },
            ],
        )
        resp, result = xiva_webui_api.ltoken_update(
            self.headers,
            {"uid": "800", "env": "production"},
            {"name": "lt1_", "service": "check-test", "client": "check-test"},
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], {"token": "production:asdfg"})

    def test_update_listen_token_environment_aware(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=tst1:tst-l1&type=listen_token&owner=tst1&token=corp:L001&environment=corp",
                    "body": [["tst1", "tst-l1", False], "tst-l1", []],
                },
            ],
        )
        resp, result = xiva_webui_api.ltoken_update(
            self.headers,
            {"uid": "200", "env": "corp"},
            {"name": "tst-l1", "service": "tst1", "client": "tst-l1"},
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], {"token": "corp:L001"})

    def test_revoke_listen_token(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=tst1:tst-l1&type=listen_token&owner=tst1&token=L001&environment=sandbox",
                    "body": [["tst1", "tst-l1", True], "tst-l1", []],
                },
            ],
        )
        resp, result = xiva_webui_api.ltoken_revoke(
            self.headers,
            {"uid": "200", "env": "sandbox"},
            {"name": "tst-l1", "service": "tst1", "client": "tst-l1"},
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(result["result"], {"token": "L001"})

    def test_update_listen_token_xconf_error_list(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [""], [{"path": "/list\?type=any&revision=\\d+&environment=", "body": ""}]
        )
        resp, result = xiva_webui_api.ltoken_update(
            self.headers,
            {"uid": "200", "env": "sandbox"},
            {"name": "tst-l1", "service": "tst1", "client": "tst-l1"},
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "transport error")

    def test_update_listen_token_xconf_error_put(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []]), ""],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=tst1:tst-l1&type=listen_token&owner=tst1&token=L001&environment=sandbox",
                    "body": [["tst1", "tst-l1", False], "tst-l1", []],
                },
            ],
        )
        resp, result = xiva_webui_api.ltoken_update(
            self.headers,
            {"uid": "200", "env": "sandbox"},
            {"name": "tst-l1", "service": "tst1", "client": "tst-l1"},
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "transport error")

    def test_update_listen_token_service_doesnt_exist(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []])], [{"path": "/list\?type=any&revision=\\d+&environment=", "body": ""}]
        )
        resp, result = xiva_webui_api.ltoken_update(
            self.headers,
            {"uid": "200", "env": "sandbox"},
            {"name": "tst-l1", "service": "_tst1", "client": "tst-l1"},
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "service not found")

    def test_update_listen_token_owner_missmatch(self):
        self.auth_one_user(400)
        self.set_xconf_responses(
            [packb([0, []])], [{"path": "/list\?type=any&revision=\\d+&environment=", "body": ""}]
        )
        resp, result = xiva_webui_api.ltoken_update(
            self.headers,
            {"uid": "400", "env": "sandbox"},
            {"name": "tst-l1", "service": "tst1", "client": "tst-l1"},
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "service owner mismatch")

    def test_create_listen_token_invalid_name(self):
        self.auth_one_user(200)
        self.set_xconf_responses([packb([0, []])] * 2, [] * 2)
        resp, result = xiva_webui_api.ltoken_update(
            self.headers,
            {"uid": "200", "env": "sandbox"},
            {"name": "tst_l2", "service": "tst1", "client": "tst1"},
        )
        assert_bad_request(resp, dumps({"error": "invalid token name"}, separators=(",", ":")))
        resp, result = xiva_webui_api.ltoken_update(
            self.headers,
            {"uid": "200", "env": "sandbox"},
            {"name": "qqqqqqqqqqqqqqqqqqqqqqqqqq", "service": "tst1", "client": "tst1"},
        )
        assert_bad_request(resp, dumps({"error": "invalid token name"}, separators=(",", ":")))

    def test_create_listen_token(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=tst1:tst-l2&type=listen_token&owner=tst1&token=.{40}&environment=production",
                    "body": [["tst1", "tst-l2", False], "tst1", []],
                },
            ],
        )
        resp, result = xiva_webui_api.ltoken_update(
            self.headers,
            {"uid": "200", "env": "production"},
            {"name": "tst-l2", "service": "tst1", "client": "tst1"},
        )
        assert_ok(resp)
        assert_not_in("error", result)
        assert_equal(len(result["result"]["token"]), 40)
