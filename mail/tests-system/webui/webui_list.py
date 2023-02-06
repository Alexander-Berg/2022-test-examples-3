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


class TestWebUiList:
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

    def list_request(self):
        resp, webui_list = xiva_webui_api.list(self.headers)
        if resp.status != 200:
            webui_list = {}
        return resp, self.filter_list(webui_list)

    def filter_list(self, webui_list):
        if "services" in webui_list:
            to_remove = []
            for s in webui_list["services"]:
                if match(r"tests-system-", s):
                    to_remove.append(s)
            for s in to_remove:
                webui_list["services"].pop(s)
        return webui_list

    def test_bad_origin_fail(self):
        self.headers["Origin"] = "fake.origin.com"
        resp = xiva_webui_api.list(self.headers)[0]
        assert_unauthorized(resp, "this origin domain is not allowed")
        del self.headers["Origin"]
        resp = xiva_webui_api.list(self.headers)[0]
        assert_unauthorized(resp, "this origin domain is not allowed")

    def test_list_unauthorized(self):
        self.wmi_server.set_response(raw_response="")
        resp, result = self.list_request()
        assert_unauthorized_json(
            resp,
            '{"error":"unauthorized","redirect":"https://passport-test.yandex.ru/auth?retpath="}',
        )

    def test_single_user_list_ok_uid_200(self):
        self.auth_one_user(200)
        resp, result = self.list_request()
        assert_ok(resp)
        assert_equal(
            result,
            {
                "admin": False,
                "services": {
                    "regtst": {
                        "revoked": False,
                        "auth_disabled": False,
                        "description": "",
                        "stream_count": 0,
                        "is_passport": False,
                        "apps": [],
                        "is_stream": False,
                        "queued_delivery_by_default": True,
                        "tvm_publishers": {},
                        "tvm_subscribers": {},
                        "listen_tokens": {},
                        "send_tokens": {
                            "sandbox": {
                                "01234567890": {
                                    "token": "01234567890",
                                    "name": "st1",
                                    "revoked": False,
                                }
                            }
                        },
                        "oauth_scopes": [],
                        "owner": "test_200",
                        "name": "regtst",
                    },
                    "tst2": {
                        "revoked": False,
                        "auth_disabled": False,
                        "description": "",
                        "stream_count": 0,
                        "is_passport": False,
                        "apps": [],
                        "is_stream": False,
                        "queued_delivery_by_default": True,
                        "tvm_publishers": {},
                        "tvm_subscribers": {},
                        "listen_tokens": {},
                        "send_tokens": {
                            "sandbox": {"S002": {"token": "S002", "name": "st1", "revoked": False}}
                        },
                        "oauth_scopes": [],
                        "owner": "test_200",
                        "name": "tst2",
                    },
                    "tst1": {
                        "revoked": False,
                        "auth_disabled": False,
                        "name": "tst1",
                        "stream_count": 0,
                        "is_passport": True,
                        "apps": [],
                        "is_stream": False,
                        "queued_delivery_by_default": True,
                        "tvm_publishers": {},
                        "tvm_subscribers": {},
                        "send_tokens": {
                            "sandbox": {
                                "S001": {"token": "S001", "name": "st1", "revoked": False},
                                "S003": {"token": "S003", "name": "st3", "revoked": False},
                            },
                            "corp": {
                                "S001": {"token": "S001", "name": "st1", "revoked": False},
                                "S003": {"token": "S003", "name": "st3", "revoked": False},
                            },
                        },
                        "oauth_scopes": [],
                        "listen_tokens": {
                            "sandbox": {
                                "L001": {
                                    "token": "L001",
                                    "client": "tst-l1",
                                    "name": "tst-l1",
                                    "revoked": False,
                                },
                                "L003": {
                                    "token": "L003",
                                    "client": "tst-l3",
                                    "name": "tst-l3",
                                    "revoked": False,
                                },
                            },
                            "corp": {
                                "L001": {
                                    "token": "L001",
                                    "client": "tst-l1",
                                    "name": "tst-l1",
                                    "revoked": False,
                                },
                                "L003": {
                                    "token": "L003",
                                    "client": "tst-l3",
                                    "name": "tst-l3",
                                    "revoked": False,
                                },
                            },
                        },
                        "owner": "test_200",
                        "description": "",
                    },
                },
                "prefix": "test-",
                "environments": ["sandbox", "corp", "production"],
            },
        )

    def test_single_user_list_ok_uid_400(self):
        self.auth_one_user(400)
        resp, result = self.list_request()
        assert_ok(resp)
        assert_equal(
            result,
            {
                "admin": False,
                "services": {
                    "mail": {
                        "revoked": False,
                        "auth_disabled": False,
                        "name": "mail",
                        "stream_count": 0,
                        "is_passport": False,
                        "apps": [],
                        "is_stream": False,
                        "queued_delivery_by_default": True,
                        "oauth_scopes": [],
                        "tvm_publishers": {
                            "sandbox": [
                                {"id": 1000502, "name": "publisher-tst", "suspended": False},
                                {"id": 1000503, "name": "publisher-suspended", "suspended": True},
                            ],
                            "production": [
                                {"id": 1000504, "name": "publisher-production", "suspended": False}
                            ],
                        },
                        "tvm_subscribers": {
                            "sandbox": [
                                {"id": 1000505, "name": "subscriber-tst", "suspended": False}
                            ]
                        },
                        "send_tokens": {},
                        "listen_tokens": {
                            "sandbox": {
                                "12345678901234567890": {
                                    "token": "12345678901234567890",
                                    "client": "tst",
                                    "name": "tst",
                                    "revoked": False,
                                }
                            }
                        },
                        "owner": "test_400",
                        "description": "",
                    },
                    "bass": {
                        "revoked": False,
                        "auth_disabled": False,
                        "name": "bass",
                        "stream_count": 0,
                        "is_passport": False,
                        "apps": [],
                        "is_stream": False,
                        "queued_delivery_by_default": True,
                        "oauth_scopes": [],
                        "tvm_publishers": {},
                        "tvm_subscribers": {
                            "sandbox": [
                                {"id": 1000505, "name": "subscriber-tst", "suspended": False}
                            ]
                        },
                        "send_tokens": {},
                        "listen_tokens": {
                            "sandbox": {
                                "bass123456": {
                                    "token": "bass123456",
                                    "client": "tst",
                                    "name": "tst",
                                    "revoked": False,
                                }
                            }
                        },
                        "owner": "test_400",
                        "description": "",
                    },
                    "tst_stream": {
                        "revoked": False,
                        "auth_disabled": False,
                        "description": "",
                        "stream_count": 10,
                        "is_passport": False,
                        "apps": [],
                        "is_stream": True,
                        "queued_delivery_by_default": True,
                        "tvm_publishers": {
                            "sandbox": [
                                {"id": 1000502, "name": "publisher-tst", "suspended": False}
                            ]
                        },
                        "tvm_subscribers": {},
                        "listen_tokens": {},
                        "send_tokens": {
                            "sandbox": {
                                "abcdef": {"token": "abcdef", "name": "sst1", "revoked": False}
                            }
                        },
                        "oauth_scopes": [],
                        "owner": "test_400",
                        "name": "tst_stream",
                    },
                    "fake": {
                        "revoked": False,
                        "auth_disabled": False,
                        "description": "",
                        "stream_count": 0,
                        "is_passport": True,
                        "apps": [],
                        "is_stream": False,
                        "queued_delivery_by_default": True,
                        "tvm_publishers": {},
                        "tvm_subscribers": {},
                        "send_tokens": {},
                        "listen_tokens": {},
                        "oauth_scopes": [],
                        "owner": "test_400",
                        "name": "fake",
                    },
                },
                "prefix": "test-",
                "environments": ["sandbox", "corp", "production"],
            },
        )

    def test_single_user_list_ok_uid_600(self):
        self.auth_one_user(600)
        resp, result = self.list_request()
        assert_ok(resp)
        assert_equal(
            result,
            {"admin": False, "prefix": "test-", "environments": ["sandbox", "corp", "production"]},
        )

    def test_single_user_single_service_list_ok(self):
        self.auth_one_user(600)
        self.auth_services("xxx", "yyy", "zzz")
        resp, result = self.list_request()
        assert_ok(resp)
        assert_equal(
            result,
            {"admin": False, "prefix": "test-", "environments": ["sandbox", "corp", "production"]},
        )

    def test_single_user_single_service_list_ok2(self):
        self.auth_one_user(600)
        self.auth_services("xxx", "yyy", "zzz", "abc-service")
        resp, result = self.list_request()
        assert_ok(resp)
        assert_equal(
            result,
            {
                "admin": False,
                "prefix": "test-",
                "environments": ["sandbox", "corp", "production"],
                "services": {
                    "test-abc": {
                        "revoked": False,
                        "auth_disabled": False,
                        "description": "",
                        "stream_count": 0,
                        "is_passport": False,
                        "tvm_publishers": {
                            "sandbox": [{"id": 12345, "name": "unknown", "suspended": False}]
                        },
                        "tvm_subscribers": {},
                        "send_tokens": {},
                        "listen_tokens": {},
                        "oauth_scopes": [],
                        "apps": [],
                        "is_stream": False,
                        "queued_delivery_by_default": True,
                        "owner": "abc:abc-service",
                        "name": "test-abc",
                        "send_tokens": {
                            "sandbox": {
                                "123abc": {"token": "123abc", "name": "st1", "revoked": False}
                            }
                        },
                    }
                },
            },
        )

    def test_list_user_service_collision(self):
        self.auth_one_user("abc-service")
        self.auth_services("xxx", "yyy", "zzz")
        resp, result = self.list_request()
        assert_ok(resp)
        assert_equal(
            result,
            {"admin": False, "prefix": "test-", "environments": ["sandbox", "corp", "production"]},
        )

    def test_list_user_service_collision2(self):
        self.auth_one_user("abc-service")
        self.auth_services("abc-service")
        resp, result = self.list_request()
        assert_ok(resp)
        assert_equal(
            result,
            {
                "admin": False,
                "prefix": "test-",
                "environments": ["sandbox", "corp", "production"],
                "services": {
                    "test-abc": {
                        "revoked": False,
                        "auth_disabled": False,
                        "description": "",
                        "stream_count": 0,
                        "is_passport": False,
                        "tvm_publishers": {
                            "sandbox": [{"id": 12345, "name": "unknown", "suspended": False}]
                        },
                        "tvm_subscribers": {},
                        "send_tokens": {},
                        "listen_tokens": {},
                        "oauth_scopes": [],
                        "apps": [],
                        "is_stream": False,
                        "queued_delivery_by_default": True,
                        "owner": "abc:abc-service",
                        "name": "test-abc",
                        "send_tokens": {
                            "sandbox": {
                                "123abc": {"token": "123abc", "name": "st1", "revoked": False}
                            }
                        },
                    }
                },
            },
        )

    def test_list_admin(self):
        self.auth_one_user(800)
        resp, result = self.list_request()
        assert_ok(resp)
        expected = {
            "admin": True,
            "prefix": "test-",
            "environments": ["sandbox", "corp", "production"],
            "services": {
                "disk-json": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": False,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "send_tokens": {},
                    "listen_tokens": {},
                    "oauth_scopes": [],
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "owner": "corp_123",
                    "name": "disk-json",
                },
                "test-abc": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": False,
                    "tvm_publishers": {
                        "sandbox": [{"id": 12345, "name": "unknown", "suspended": False}]
                    },
                    "tvm_subscribers": {},
                    "send_tokens": {},
                    "listen_tokens": {},
                    "oauth_scopes": [],
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "owner": "abc:abc-service",
                    "name": "test-abc",
                    "send_tokens": {
                        "sandbox": {"123abc": {"token": "123abc", "name": "st1", "revoked": False}}
                    },
                },
                "test-tst1": {
                    "revoked": True,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": False,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "send_tokens": {},
                    "listen_tokens": {},
                    "oauth_scopes": [],
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "owner": "test_800",
                    "name": "test-tst1",
                },
                "noauth": {
                    "revoked": False,
                    "auth_disabled": True,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": False,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "send_tokens": {},
                    "listen_tokens": {},
                    "oauth_scopes": [],
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "owner": "test_800",
                    "name": "noauth",
                },
                "check-test": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": False,
                    "oauth_scopes": [],
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "owner": "test_800",
                    "name": "check-test",
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "send_tokens": {
                        "sandbox": {
                            "corp:qazwsx": {
                                "token": "corp:qazwsx",
                                "name": "st1_",
                                "revoked": False,
                            }
                        },
                        "corp": {"qwerty": {"token": "qwerty", "name": "st1_", "revoked": False}},
                    },
                    "listen_tokens": {
                        "sandbox": {
                            "production:zxcvb": {
                                "token": "production:zxcvb",
                                "client": "check-test",
                                "name": "lt1_",
                                "revoked": False,
                            }
                        },
                        "production": {
                            "asdfg": {
                                "token": "asdfg",
                                "client": "check-test",
                                "name": "lt1_",
                                "revoked": False,
                            }
                        },
                    },
                    "apps": [
                        {
                            "app_name": "ru.yandex.mail",
                            "platform": "apns",
                            "expiration": 123,
                            "can_revert": True,
                            "environment": "auto",
                            "revoked": False,
                            "sha1": "57b82f57917acf99a3d08abcafa306900203b24d",
                            "updated_at": 0,
                        },
                        {
                            "app_name": "ru.yandex.mail.v2",
                            "platform": "apns",
                            "expiration": 123,
                            "can_revert": True,
                            "environment": "auto",
                            "revoked": False,
                            "sha1": "6baef686ef28cc4097395abd62b7e08930ddeff2",
                            "updated_at": 0,
                        },
                        {
                            "app_name": "xiva.test.mail",
                            "platform": "apns",
                            "expiration": 0,
                            "can_revert": False,
                            "environment": "development",
                            "revoked": False,
                            "sha1": "57b82f57917acf99a3d08abcafa306900203b24d",
                            "updated_at": 0,
                        },
                        {
                            "app_name": "ru.yandex.mail",
                            "platform": "fcm",
                            "expiration": 0,
                            "can_revert": False,
                            "environment": "auto",
                            "revoked": False,
                            "sha1": "640d87e741e6aa4c669a82a4cd304787960513ab",
                            "updated_at": 0,
                        },
                        {
                            "app_name": "ru.yandex.mail.v2",
                            "platform": "fcm",
                            "expiration": 0,
                            "can_revert": True,
                            "environment": "auto",
                            "revoked": True,
                            "updated_at": 0,
                        },
                        {
                            "app_name": "xiva.test.mail",
                            "platform": "fcm",
                            "expiration": 0,
                            "can_revert": False,
                            "environment": "auto",
                            "revoked": False,
                            "sha1": "a62f2225bf70bfaccbc7f1ef2a397836717377de",
                            "updated_at": 0,
                        },
                        {
                            "app_name": "ru.test.hms",
                            "platform": "hms",
                            "expiration": 0,
                            "can_revert": False,
                            "environment": "auto",
                            "revoked": False,
                            "sha1": "5db984a549fc9ad85532ff5148e57a713a39316a",
                            "updated_at": 0,
                        },
                        {
                            "app_name": "ru.test.wns",
                            "platform": "wns",
                            "expiration": 0,
                            "can_revert": False,
                            "environment": "auto",
                            "revoked": False,
                            "sha1": "0bb71894439eab89e826c33b98b94d693dca1c5d",
                            "updated_at": 0,
                        },
                    ],
                },
                "check-p8test": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": False,
                    "oauth_scopes": [],
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "owner": "test_777",
                    "name": "check-p8test",
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "send_tokens": {
                        "sandbox": {
                            "corp:klmk": {"name": "st2_", "revoked": False, "token": "corp:klmk"}
                        },
                        "corp": {"knjknkn": {"name": "st2_", "revoked": False, "token": "knjknkn"}},
                    },
                    "listen_tokens": {
                        "sandbox": {
                            "production:sdfvsdfv": {
                                "client": "check-p8test",
                                "name": "lt2_",
                                "revoked": False,
                                "token": "production:sdfvsdfv",
                            }
                        },
                        "production": {
                            "dfvsdfv": {
                                "client": "check-p8test",
                                "name": "lt2_",
                                "revoked": False,
                                "token": "dfvsdfv",
                            }
                        },
                    },
                    "apps": [
                        {
                            "app_name": "ru.yandex.mail.p8test",
                            "can_revert": True,
                            "environment": "auto",
                            "expiration": 0,
                            "platform": "apns",
                            "revoked": False,
                            "sha1": "69f4ab7692c2d25b762e3f157c3ff484fcc4863b",
                            "updated_at": 0,
                        }
                    ],
                },
                "regtst": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": False,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "listen_tokens": {},
                    "send_tokens": {
                        "sandbox": {
                            "01234567890": {"token": "01234567890", "name": "st1", "revoked": False}
                        }
                    },
                    "oauth_scopes": [],
                    "owner": "test_200",
                    "name": "regtst",
                },
                "tst2": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": False,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "listen_tokens": {},
                    "send_tokens": {
                        "sandbox": {"S002": {"token": "S002", "name": "st1", "revoked": False}}
                    },
                    "oauth_scopes": [],
                    "owner": "test_200",
                    "name": "tst2",
                },
                "tst1": {
                    "revoked": False,
                    "auth_disabled": False,
                    "name": "tst1",
                    "stream_count": 0,
                    "is_passport": True,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "send_tokens": {
                        "sandbox": {
                            "S001": {"token": "S001", "name": "st1", "revoked": False},
                            "S003": {"token": "S003", "name": "st3", "revoked": False},
                        },
                        "corp": {
                            "S001": {"token": "S001", "name": "st1", "revoked": False},
                            "S003": {"token": "S003", "name": "st3", "revoked": False},
                        },
                    },
                    "oauth_scopes": [],
                    "listen_tokens": {
                        "sandbox": {
                            "L001": {
                                "token": "L001",
                                "client": "tst-l1",
                                "name": "tst-l1",
                                "revoked": False,
                            },
                            "L003": {
                                "token": "L003",
                                "client": "tst-l3",
                                "name": "tst-l3",
                                "revoked": False,
                            },
                        },
                        "corp": {
                            "L001": {
                                "token": "L001",
                                "client": "tst-l1",
                                "name": "tst-l1",
                                "revoked": False,
                            },
                            "L003": {
                                "token": "L003",
                                "client": "tst-l3",
                                "name": "tst-l3",
                                "revoked": False,
                            },
                        },
                    },
                    "owner": "test_200",
                    "description": "",
                },
                "mail": {
                    "revoked": False,
                    "auth_disabled": False,
                    "name": "mail",
                    "stream_count": 0,
                    "is_passport": False,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "oauth_scopes": [],
                    "tvm_publishers": {
                        "sandbox": [
                            {"id": 1000502, "name": "publisher-tst", "suspended": False},
                            {"id": 1000503, "name": "publisher-suspended", "suspended": True},
                        ],
                        "production": [
                            {"id": 1000504, "name": "publisher-production", "suspended": False}
                        ],
                    },
                    "tvm_subscribers": {
                        "sandbox": [{"id": 1000505, "name": "subscriber-tst", "suspended": False}]
                    },
                    "send_tokens": {},
                    "listen_tokens": {
                        "sandbox": {
                            "12345678901234567890": {
                                "token": "12345678901234567890",
                                "client": "tst",
                                "name": "tst",
                                "revoked": False,
                            }
                        }
                    },
                    "owner": "test_400",
                    "description": "",
                },
                "bass": {
                    "revoked": False,
                    "auth_disabled": False,
                    "name": "bass",
                    "stream_count": 0,
                    "is_passport": False,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "oauth_scopes": [],
                    "tvm_publishers": {},
                    "tvm_subscribers": {
                        "sandbox": [{"id": 1000505, "name": "subscriber-tst", "suspended": False}]
                    },
                    "send_tokens": {},
                    "listen_tokens": {
                        "sandbox": {
                            "bass123456": {
                                "token": "bass123456",
                                "client": "tst",
                                "name": "tst",
                                "revoked": False,
                            }
                        }
                    },
                    "owner": "test_400",
                    "description": "",
                },
                "tst_stream": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 10,
                    "is_passport": False,
                    "apps": [],
                    "is_stream": True,
                    "queued_delivery_by_default": True,
                    "tvm_publishers": {
                        "sandbox": [{"id": 1000502, "name": "publisher-tst", "suspended": False}]
                    },
                    "tvm_subscribers": {},
                    "listen_tokens": {},
                    "send_tokens": {
                        "sandbox": {"abcdef": {"token": "abcdef", "name": "sst1", "revoked": False}}
                    },
                    "oauth_scopes": [],
                    "owner": "test_400",
                    "name": "tst_stream",
                },
                "tst_force_direct": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": False,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": False,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "listen_tokens": {},
                    "send_tokens": {
                        "sandbox": {
                            "ab123456tstforcedirectcdef": {
                                "token": "ab123456tstforcedirectcdef",
                                "name": "sst1",
                                "revoked": False,
                            }
                        }
                    },
                    "oauth_scopes": [],
                    "owner": "test_900",
                    "name": "tst_force_direct",
                },
                "fake": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": True,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "send_tokens": {},
                    "listen_tokens": {},
                    "oauth_scopes": [],
                    "owner": "test_400",
                    "name": "fake",
                },
                "autoru": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": True,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "send_tokens": {
                        "sandbox": {
                            "S123456": {"token": "S123456", "name": "tst1", "revoked": False}
                        }
                    },
                    "listen_tokens": {
                        "sandbox": {
                            "L123456": {
                                "token": "L123456",
                                "client": "tst",
                                "name": "tst",
                                "revoked": False,
                            }
                        }
                    },
                    "oauth_scopes": [],
                    "owner": "test_800",
                    "name": "autoru",
                },
            },
        }
        print json.dumps(result, sort_keys=True)
        print json.dumps(expected, sort_keys=True)
        assert_equal(result, expected)

    def test_dual_uid_list_ok(self):
        self.auth_two_users(200, 400)
        resp, result = self.list_request()
        assert_ok(resp)
        expected = {
            "admin": False,
            "prefix": "test-",
            "environments": ["sandbox", "corp", "production"],
            "services": {
                "regtst": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": False,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "listen_tokens": {},
                    "send_tokens": {
                        "sandbox": {
                            "01234567890": {"token": "01234567890", "name": "st1", "revoked": False}
                        }
                    },
                    "oauth_scopes": [],
                    "owner": "test_200",
                    "name": "regtst",
                },
                "tst2": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": False,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "listen_tokens": {},
                    "send_tokens": {
                        "sandbox": {"S002": {"token": "S002", "name": "st1", "revoked": False}}
                    },
                    "oauth_scopes": [],
                    "owner": "test_200",
                    "name": "tst2",
                },
                "tst1": {
                    "revoked": False,
                    "auth_disabled": False,
                    "name": "tst1",
                    "stream_count": 0,
                    "is_passport": True,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "send_tokens": {
                        "sandbox": {
                            "S001": {"token": "S001", "name": "st1", "revoked": False},
                            "S003": {"token": "S003", "name": "st3", "revoked": False},
                        },
                        "corp": {
                            "S001": {"token": "S001", "name": "st1", "revoked": False},
                            "S003": {"token": "S003", "name": "st3", "revoked": False},
                        },
                    },
                    "oauth_scopes": [],
                    "listen_tokens": {
                        "sandbox": {
                            "L001": {
                                "token": "L001",
                                "client": "tst-l1",
                                "name": "tst-l1",
                                "revoked": False,
                            },
                            "L003": {
                                "token": "L003",
                                "client": "tst-l3",
                                "name": "tst-l3",
                                "revoked": False,
                            },
                        },
                        "corp": {
                            "L001": {
                                "token": "L001",
                                "client": "tst-l1",
                                "name": "tst-l1",
                                "revoked": False,
                            },
                            "L003": {
                                "token": "L003",
                                "client": "tst-l3",
                                "name": "tst-l3",
                                "revoked": False,
                            },
                        },
                    },
                    "owner": "test_200",
                    "description": "",
                },
                "mail": {
                    "revoked": False,
                    "auth_disabled": False,
                    "name": "mail",
                    "stream_count": 0,
                    "is_passport": False,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "oauth_scopes": [],
                    "tvm_publishers": {
                        "sandbox": [
                            {"id": 1000502, "name": "publisher-tst", "suspended": False},
                            {"id": 1000503, "name": "publisher-suspended", "suspended": True},
                        ],
                        "production": [
                            {"id": 1000504, "name": "publisher-production", "suspended": False}
                        ],
                    },
                    "tvm_subscribers": {
                        "sandbox": [{"id": 1000505, "name": "subscriber-tst", "suspended": False}]
                    },
                    "send_tokens": {},
                    "listen_tokens": {
                        "sandbox": {
                            "12345678901234567890": {
                                "token": "12345678901234567890",
                                "client": "tst",
                                "name": "tst",
                                "revoked": False,
                            }
                        }
                    },
                    "owner": "test_400",
                    "description": "",
                },
                "bass": {
                    "revoked": False,
                    "auth_disabled": False,
                    "name": "bass",
                    "stream_count": 0,
                    "is_passport": False,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "oauth_scopes": [],
                    "tvm_publishers": {},
                    "tvm_subscribers": {
                        "sandbox": [{"id": 1000505, "name": "subscriber-tst", "suspended": False}]
                    },
                    "send_tokens": {},
                    "listen_tokens": {
                        "sandbox": {
                            "bass123456": {
                                "token": "bass123456",
                                "client": "tst",
                                "name": "tst",
                                "revoked": False,
                            }
                        }
                    },
                    "owner": "test_400",
                    "description": "",
                },
                "tst_stream": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 10,
                    "is_passport": False,
                    "apps": [],
                    "is_stream": True,
                    "queued_delivery_by_default": True,
                    "tvm_publishers": {
                        "sandbox": [{"id": 1000502, "name": "publisher-tst", "suspended": False}]
                    },
                    "tvm_subscribers": {},
                    "listen_tokens": {},
                    "send_tokens": {
                        "sandbox": {"abcdef": {"token": "abcdef", "name": "sst1", "revoked": False}}
                    },
                    "oauth_scopes": [],
                    "owner": "test_400",
                    "name": "tst_stream",
                },
                "fake": {
                    "revoked": False,
                    "auth_disabled": False,
                    "description": "",
                    "stream_count": 0,
                    "is_passport": True,
                    "apps": [],
                    "is_stream": False,
                    "queued_delivery_by_default": True,
                    "tvm_publishers": {},
                    "tvm_subscribers": {},
                    "send_tokens": {},
                    "listen_tokens": {},
                    "oauth_scopes": [],
                    "owner": "test_400",
                    "name": "fake",
                },
            },
        }
        print json.dumps(result, sort_keys=True)
        print json.dumps(expected, sort_keys=True)
        assert_equal(result, expected)
