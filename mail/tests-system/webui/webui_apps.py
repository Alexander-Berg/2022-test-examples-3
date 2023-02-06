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


class FuzzyTimeNow:
    def __eq__(self, other):
        return other >= time.time() - 1 and other <= time.time() + 1


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

    def test_update_or_revert_app_service_owner_mismatch(self):
        self.auth_one_user(200)
        self.set_xconf_responses(
            [packb([0, []])] * 2,
            [{"path": "/list\?type=any&revision=\\d+&environment=", "body": ""}] * 2,
        )
        resp, result = xiva_webui_api.app_update(
            self.headers,
            {"uid": "200"},
            {
                "app_name": "test",
                "platform": "fcm",
                "service": "mail",
                "apikey": "123",
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "service owner mismatch")
        resp, result = xiva_webui_api.app_revert(
            self.headers,
            {"uid": "200"},
            {
                "app_name": "ru.yandex.mail.v2",
                "platform": "fcm",
                "service": "mail",
                "apikey": "123",
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_not_in("result", result)
        assert_equal(result["error"], "service owner mismatch")

    def test_create_app_fcm(self):
        self.auth_one_user(400)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=gcm:test&type=mobile&owner=xivaservice:mail&token=&environment=",
                    "body": ["", "mail", "gcm", "test", 0, "123", 0, "", 0, FuzzyTimeNow()],
                },
            ],
        )
        resp, result = xiva_webui_api.app_update(
            self.headers,
            {"uid": "400"},
            {
                "app_name": "test",
                "platform": "fcm",
                "service": "mail",
                "apikey": "123",
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "test",
                "platform": "fcm",
                "expiration": 0,
                "can_revert": False,
                "environment": "auto",
                "revoked": False,
                "sha1": "40bd001563085fc35165329ea1ff5c5ecbdbbeef",
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_create_app_wns(self):
        self.auth_one_user(400)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=wns:test&type=mobile&owner=xivaservice:mail&token=&environment=",
                    "body": ["", "mail", "wns", "test", 0, "456\n123", 0, "", 0, FuzzyTimeNow()],
                },
            ],
        )
        resp, result = xiva_webui_api.app_update(
            self.headers,
            {"uid": "400"},
            {
                "app_name": "test",
                "platform": "wns",
                "service": "mail",
                "secret": "123",
                "sid": "456",
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "test",
                "platform": "wns",
                "expiration": 0,
                "can_revert": False,
                "environment": "auto",
                "revoked": False,
                "sha1": "9897d921f063edd27745d427f0cb7876ea919f56",
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_create_app_hms(self):
        self.auth_one_user(400)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=hms:test&type=mobile&owner=xivaservice:mail&token=&environment=",
                    "body": ["", "mail", "hms", "test", 0, "456\n123", 0, "", 0, FuzzyTimeNow()],
                },
            ],
        )
        resp, result = xiva_webui_api.app_update(
            self.headers,
            {"uid": "400"},
            {
                "app_name": "test",
                "platform": "hms",
                "service": "mail",
                "secret": "123",
                "client_id": "456",
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "test",
                "platform": "hms",
                "expiration": 0,
                "can_revert": False,
                "environment": "auto",
                "revoked": False,
                "sha1": "9897d921f063edd27745d427f0cb7876ea919f56",
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_create_app_apns_pem(self):
        self.auth_one_user(400)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=apns:test&type=mobile&owner=xivaservice:mail&token=&environment=",
                    "body": [
                        "",
                        "mail",
                        "apns",
                        "test",
                        0,
                        certificates.valid_apns,
                        1921246654,
                        "",
                        0,
                        FuzzyTimeNow(),
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.app_update(
            self.headers,
            {"uid": "400"},
            {
                "app_name": "test",
                "platform": "apns",
                "service": "mail",
                "cert": b64encode(certificates.valid_apns),
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "test",
                "platform": "apns",
                "expiration": 1921246654,
                "can_revert": False,
                "environment": "auto",
                "revoked": False,
                "sha1": "57b82f57917acf99a3d08abcafa306900203b24d",
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_create_app_apns_p12(self):
        self.auth_one_user(400)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=apns:test&type=mobile&owner=xivaservice:mail&token=&environment=",
                    "body": [
                        "",
                        "mail",
                        "apns",
                        "test",
                        0,
                        certificates.valid_apns,
                        1921246654,
                        "",
                        0,
                        FuzzyTimeNow(),
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.app_update(
            self.headers,
            {"uid": "400"},
            {
                "app_name": "test",
                "platform": "apns",
                "service": "mail",
                "cert": b64encode(certificates.valid_apns_p12),
                "cert-pass": certificates.p12_password,
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "test",
                "platform": "apns",
                "expiration": 1921246654,
                "can_revert": False,
                "environment": "auto",
                "revoked": False,
                "sha1": "57b82f57917acf99a3d08abcafa306900203b24d",
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_create_app_apns_p8(self):
        self.auth_one_user(400)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=apns:p8.test&type=mobile&owner=xivaservice:mail&token=&environment=",
                    "body": [
                        "",
                        "mail",
                        "apns",
                        "p8.test",
                        0,
                        certificates.apns_p8_token0_binary,
                        0,
                        "",
                        1,
                        FuzzyTimeNow(),
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.app_update(
            self.headers,
            {"uid": "400"},
            {
                "app_name": "p8.test",
                "platform": "apns",
                "service": "mail",
                "environment": "production",
                "key": b64encode(certificates.apns_p8_token0),
                "key_id": "ABC123DEFG",
                "issuer_key": "DEF123GHIJ",
                "topic": "test",
                "type": "production",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "p8.test",
                "platform": "apns",
                "expiration": 0,
                "can_revert": False,
                "environment": "production",
                "revoked": False,
                "sha1": "69f4ab7692c2d25b762e3f157c3ff484fcc4863b",
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_update_app_fcm(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=gcm:ru.yandex.mail&type=mobile&owner=xivaservice:check-test&token=&environment=",
                    "body": [
                        "",
                        "check-test",
                        "gcm",
                        "ru.yandex.mail",
                        0,
                        "123",
                        0,
                        "s1",
                        0,
                        FuzzyTimeNow(),
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.app_update(
            self.headers,
            {"uid": "800"},
            {
                "app_name": "ru.yandex.mail",
                "platform": "fcm",
                "service": "check-test",
                "apikey": "123",
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "ru.yandex.mail",
                "platform": "fcm",
                "expiration": 0,
                "can_revert": True,
                "environment": "auto",
                "revoked": False,
                "sha1": "40bd001563085fc35165329ea1ff5c5ecbdbbeef",
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_update_app_apns_pem(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=apns:ru.yandex.mail.v2&type=mobile&owner=xivaservice:check-test&token=&environment=",
                    "body": [
                        "",
                        "check-test",
                        "apns",
                        "ru.yandex.mail.v2",
                        0,
                        certificates.valid_apns,
                        1921246654,
                        certificates.outdated,
                        1,
                        FuzzyTimeNow(),
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.app_update(
            self.headers,
            {"uid": "800"},
            {
                "app_name": "ru.yandex.mail.v2",
                "platform": "apns",
                "service": "check-test",
                "cert": b64encode(certificates.valid_apns),
                "environment": "production",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "ru.yandex.mail.v2",
                "platform": "apns",
                "expiration": 1921246654,
                "can_revert": True,
                "environment": "production",
                "revoked": False,
                "sha1": "57b82f57917acf99a3d08abcafa306900203b24d",
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_update_app_apns_p8(self):
        self.auth_one_user(777)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=apns:ru.yandex.mail.p8test&type=mobile&owner=xivaservice:check-p8test&token=&environment=",
                    "body": [
                        "",
                        "check-p8test",
                        "apns",
                        "ru.yandex.mail.p8test",
                        0,
                        certificates.apns_p8_token1_binary,
                        0,
                        certificates.apns_p8_token0_binary,
                        2,
                        FuzzyTimeNow(),
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.app_update(
            self.headers,
            {"uid": "777"},
            {
                "app_name": "ru.yandex.mail.p8test",
                "platform": "apns",
                "service": "check-p8test",
                "environment": "development",
                "key": b64encode(certificates.apns_p8_token1),
                "key_id": "SDFGASFDS",
                "issuer_key": "345gdfs345",
                "topic": "ru.yandex.mail.v2.Development",
                "type": "development",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "ru.yandex.mail.p8test",
                "platform": "apns",
                "expiration": 0,
                "can_revert": True,
                "environment": "development",
                "revoked": False,
                "sha1": "208f46b0fb4e08feab86990545b0700a206dd91b",
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_revert_app_fcm(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=gcm:ru.yandex.mail.v2&type=mobile&owner=xivaservice:check-test&token=&environment=",
                    "body": [
                        "",
                        "check-test",
                        "gcm",
                        "ru.yandex.mail.v2",
                        0,
                        "old",
                        0,
                        "",
                        0,
                        FuzzyTimeNow(),
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.app_revert(
            self.headers,
            {"uid": "800"},
            {
                "app_name": "ru.yandex.mail.v2",
                "platform": "fcm",
                "service": "check-test",
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "ru.yandex.mail.v2",
                "platform": "fcm",
                "expiration": 0,
                "can_revert": False,
                "environment": "auto",
                "revoked": False,
                "sha1": "c00dbbc9dadfbe1e232e93a729dd4752fade0abf",
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_revert_app_apns_pem(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=apns:ru.yandex.mail.v2&type=mobile&owner=xivaservice:check-test&token=&environment=",
                    "body": [
                        "",
                        "check-test",
                        "apns",
                        "ru.yandex.mail.v2",
                        0,
                        certificates.valid_apns,
                        1921246654,
                        certificates.outdated,
                        0,
                        FuzzyTimeNow(),
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.app_revert(
            self.headers,
            {"uid": "800"},
            {
                "app_name": "ru.yandex.mail.v2",
                "platform": "apns",
                "service": "check-test",
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "ru.yandex.mail.v2",
                "platform": "apns",
                "expiration": 1921246654,
                "can_revert": True,
                "environment": "auto",
                "revoked": False,
                "sha1": "57b82f57917acf99a3d08abcafa306900203b24d",
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_revert_app_apns_p8(self):
        self.auth_one_user(777)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=apns:ru.yandex.mail.p8test&type=mobile&owner=xivaservice:check-p8test&token=&environment=",
                    "body": [
                        "",
                        "check-p8test",
                        "apns",
                        "ru.yandex.mail.p8test",
                        0,
                        certificates.apns_p8_token1_binary,
                        0,
                        certificates.apns_p8_token0_binary,
                        0,
                        FuzzyTimeNow(),
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.app_revert(
            self.headers,
            {"uid": "777"},
            {
                "app_name": "ru.yandex.mail.p8test",
                "platform": "apns",
                "service": "check-p8test",
                "environment": "development",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "ru.yandex.mail.p8test",
                "platform": "apns",
                "expiration": 0,
                "can_revert": True,
                "environment": "auto",
                "revoked": False,
                "sha1": "208f46b0fb4e08feab86990545b0700a206dd91b",
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_revoke_app(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=gcm:ru.yandex.mail&type=mobile&owner=xivaservice:check-test&token=&environment=",
                    "body": [
                        "",
                        "check-test",
                        "gcm",
                        "ru.yandex.mail",
                        0,
                        "",
                        0,
                        "s1",
                        0,
                        FuzzyTimeNow(),
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.app_revoke(
            self.headers,
            {"uid": "800"},
            {
                "app_name": "ru.yandex.mail",
                "platform": "fcm",
                "service": "check-test",
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "ru.yandex.mail",
                "platform": "fcm",
                "expiration": 0,
                "can_revert": True,
                "environment": "auto",
                "revoked": True,
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_revoke_app_apns_p8(self):
        self.auth_one_user(777)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=apns:ru.yandex.mail.p8test&type=mobile&owner=xivaservice:check-p8test&token=&environment=",
                    "body": [
                        "",
                        "check-p8test",
                        "apns",
                        "ru.yandex.mail.p8test",
                        0,
                        "",
                        0,
                        certificates.apns_p8_token0_binary,
                        0,
                        FuzzyTimeNow(),
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.app_revoke(
            self.headers,
            {"uid": "777"},
            {
                "app_name": "ru.yandex.mail.p8test",
                "platform": "apns",
                "service": "check-p8test",
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "ru.yandex.mail.p8test",
                "platform": "apns",
                "expiration": 0,
                "can_revert": True,
                "environment": "auto",
                "revoked": True,
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_revoke_revoked_app(self):
        self.auth_one_user(800)
        self.set_xconf_responses(
            [packb([0, []]), "0"],
            [
                {"path": "/list\?type=any&revision=\\d+&environment=", "body": ""},
                {
                    "path": "/put\?name=gcm:ru.yandex.mail.v2&type=mobile&owner=xivaservice:check-test&token=&environment=",
                    "body": [
                        "",
                        "check-test",
                        "gcm",
                        "ru.yandex.mail.v2",
                        0,
                        "",
                        0,
                        "old",
                        0,
                        FuzzyTimeNow(),
                    ],
                },
            ],
        )
        resp, result = xiva_webui_api.app_revoke(
            self.headers,
            {"uid": "800"},
            {
                "app_name": "ru.yandex.mail.v2",
                "platform": "fcm",
                "service": "check-test",
                "environment": "auto",
            },
        )
        assert_ok(resp)
        assert_in("result", result)
        assert_equal(
            result["result"],
            {
                "app_name": "ru.yandex.mail.v2",
                "platform": "fcm",
                "expiration": 0,
                "can_revert": True,
                "environment": "auto",
                "revoked": True,
                "updated_at": FuzzyTimeNow(),
            },
        )

    def test_app_info_not_admin_fails(self):
        self.auth_one_user(777)
        resp, result = xiva_webui_api.app_info(
            self.headers, {"app": "ru.yandex.mail.p8test", "platform": "apns"}
        )
        assert_unauthorized(resp, '{"error":"must be admin"}')

    def test_app_info_doesnt_exist(self):
        self.auth_one_user(800)
        resp, result = xiva_webui_api.app_info(
            self.headers, {"app": "ru.yandex.nonexistent", "platform": "apns"}
        )
        assert_bad_request(resp, '{"error":"app doesn\'t exist"}')

    def test_app_info_apns(self):
        self.auth_one_user(800)
        resp, result = xiva_webui_api.app_info(
            self.headers, {"app": "ru.yandex.mail", "platform": "apns"}
        )
        assert_ok(resp)
        self.check_app_info_fields(result)
        assert_equal(result["current"]["UID (topic)"], "ru.yandex.mail")
        assert_equal(result["backup"]["Type"], "production")

    def test_app_info_fcm(self):
        self.auth_one_user(800)
        resp, result = xiva_webui_api.app_info(
            self.headers, {"app": "xiva.test.mail", "platform": "fcm"}
        )
        assert_ok(resp)
        self.check_app_info_fields(result)
        assert_equal(result["environment"], "auto")
        assert_equal(result["current"]["API key"], "key")

    def test_app_info_hms(self):
        self.auth_one_user(800)
        resp, result = xiva_webui_api.app_info(
            self.headers, {"app": "ru.test.hms", "platform": "hms"}
        )
        assert_ok(resp)
        self.check_app_info_fields(result)
        assert_equal(result["environment"], "auto")
        assert_equal(result["current"]["ID"], "id")
        assert_equal(result["current"]["Secret"], "secret")

    def test_app_info_wns(self):
        self.auth_one_user(800)
        resp, result = xiva_webui_api.app_info(
            self.headers, {"app": "ru.test.wns", "platform": "wns"}
        )
        assert_ok(resp)
        self.check_app_info_fields(result)
        assert_equal(result["environment"], "auto")
        assert_equal(result["current"]["ID"], "sid")
        assert_equal(result["current"]["Secret"], "secret")

    def check_app_info_fields(self, app_info):
        assert_in("updated_at", app_info)
        assert_in("environment", app_info)
        assert_in("current", app_info)
        assert_in("backup", app_info)
