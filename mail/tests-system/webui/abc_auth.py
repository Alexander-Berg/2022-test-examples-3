from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
from json import loads, dumps
from msgpack import packb, unpackb
from re import match
from time import sleep


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


class TestAbcAuth:
    def setup(self):
        global wmi_server, xconf_server, abc_server
        self.wmi_server = wmi_server
        self.abc_server = abc_server
        self.xconf_server = xconf_server
        self.xconf_server.reset_state()
        self.xconf_server.set_response(raw_response=packb([0, []]))
        self.abc_server.set_response(raw_response=dumps({"results": []}))
        self.headers = {"cookie": "123", "Origin": "push.yandex.ru"}
        self._abc_args = {
            "role__code__in": ",".join(["product_owner", "xiva_manager"]),
            "fields": "service.slug",
            "page_size": "1000",
        }

    def auth_one_user(self, uid):
        self.wmi_server.set_response(raw_response=dumps({"check_cookies": {"uid": str(uid)}}))

    def auth_two_users(self, uid, child_uid):
        self.wmi_server.set_response(
            raw_response=dumps({"check_cookies": {"uid": str(uid), "childUids": [str(child_uid)]}})
        )

    def abc_request_hook(self, req, uid):
        args = self._abc_args.copy()
        args["person__uid"] = uid
        # TODO After ABC-5730 fixed:
        # args['person__uid__in'] = ','.join(uids)
        assert_in("Authorization", req.headers)
        assert_equals(req.headers["Authorization"], "OAuth test-oauth-token")
        check_caught_request(req, "/api/v4/services/members/", **args)

    def check_abc_request(self, uid):
        abc_server.set_request_hook(lambda req: self.abc_request_hook(req, uid))

    def test_normal_response(self):
        self.auth_one_user("123")
        self.check_abc_request("123")
        resp, _ = xiva_webui_api.list(self.headers)
        assert_ok(resp)
        assert_not_in("error", loads(resp.body))

    def test_normal_response_ma(self):
        self.auth_two_users("456", "123")
        self.check_abc_request("456")
        resp, _ = xiva_webui_api.list(self.headers)
        assert_ok(resp)
        assert_not_in("error", loads(resp.body))

    def test_bad_abc_response(self):
        """Check webui reaction on invalid ABC responses"""
        cases = [  # (abc_response, description)
            ("123", "Not JSON"),
            (dumps([]), "Bad JSON type"),
            (dumps([123, "456"]), "Bad JSON type 2"),
            (dumps({"results": ["test"]}), 'Bad "results" element type'),
            (dumps({"results": [123]}), 'Bad "results" element type 2'),
            (dumps({"results": [[]]}), 'Bad "results" element type 3'),
            (dumps({"results": [{"service": 123}]}), 'Bad "service" type'),
            (dumps({"results": [{"service": []}]}), 'Bad "service" type 2'),
            (dumps({"results": [{"service": {"slug": []}}]}), 'Bad "slug" type 2'),
            (dumps({"results": [{"service": {"slug": {}}}]}), 'Bad "slug" type 3'),
        ]
        for abc_resp, desc in cases:
            yield self.check_bad_abc_response, abc_resp, desc

    def check_bad_abc_response(self, abc_resp, desc):
        print desc
        self.auth_one_user("123")
        self.check_abc_request("123")
        self.abc_server.set_response(raw_response=abc_resp)
        resp, _ = xiva_webui_api.list(self.headers)
        assert_unauthorized(resp, "authorization failed")
