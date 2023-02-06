from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
from pycommon.wait_condition import *
import time

class TestSubscribeWebsocketWithCookie:
    @classmethod
    def setup_class(cls):
        cls.xiva = XivaApiV2(host='localhost', port=18083)
        cls.xiva_client = XivaClient(host='localhost', port=18083, back_port=18080)
        cls.hub_server = fake_server(host='localhost', port=17081, raw_response='OK')
        cls.wmi_server = fake_server(host='localhost', port=17084, raw_response='OK')

    @classmethod
    def teardown_class(cls):
        cls.hub_server.fini()
        cls.wmi_server.fini()

    def setup(self):
        self._good_args = { 'uid': "200", 'service': "tst1",
            'client': "test", 'session': "ABCD-EFGH",
        }
        self.hub_server.requests = []
        self.hub_server.set_request_hook(lambda req: self.hub_server.requests.append(req))
        self.ws = None

    def teardown(self):
        # Close websocket and wait to unsubscribe
        if self.ws:
            self.hub_server.requests = []
            self.ws = None
            wait(lambda: len(self.hub_server.requests) == 1, 1)

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def conn_stat(self):
        stat = self.xiva_client.stat()
        return int(stat['stat']['modules']['processor']['subscribers'])

    def test_ws_with_no_arguments_fails_with_4400(self):
        (resp, self.ws) = self.xiva.subscribe_websocket()
        assert_ws_bad_request(resp, self.ws, 'missing argument "user (uid)"')

    def test_ws_without_cookies_fails_with_4401(self):
        (resp, self.ws) = self.xiva.subscribe_websocket(**self.args())
        assert_ws_unauthorized(resp, self.ws)

    def test_ws_fails_with_no_origin(self):
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"cookie" : "123"}, **self.args())
        assert_ws_unauthorized(resp, self.ws, 'this origin domain is not allowed')

    def test_ws_success_with_good_cookies(self):
        self.wmi_server.set_response(raw_response='{"check_cookies":{"uid":"200"}}')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"cookie" : "123", "Origin" : "push.yandex.ru"}, **self.args())
        assert_ws_ok(resp, self.ws)

    def test_ws_success_with_good_cookies_multiaccount_but_one_uid(self):
        self.wmi_server.set_response(raw_response='{"check_cookies":{"uid":"200", "childUids":["800"]}}')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"cookie" : "123", "Origin" : "push.yandex.ru"}, **self.args())
        assert_ws_ok(resp, self.ws)
        time.sleep(0.05)
        assert_equals(self.conn_stat(), 1)

    def test_ws_success_with_good_cookies_multiaccount_two_uids(self):
        self.wmi_server.set_response(raw_response='{"check_cookies":{"uid":"200", "childUids":["800"]}}')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"cookie" : "123", "Origin" : "push.yandex.ru"}, **self.args(uid="200,800"))
        assert_ws_ok(resp, self.ws)
        time.sleep(0.05)
        assert_equals(self.conn_stat(), 2)

    def test_ws_success_with_good_cookies_multiaccount_but_one_denied_uid(self):
        self.wmi_server.set_response(raw_response='{"check_cookies":{"uid":"200", "childUids":["800"]}}')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"cookie" : "123", "Origin" : "push.yandex.ru"}, **self.args(uid="200,500"))
        assert_ws_unauthorized(resp, self.ws)

    def test_ws_success_with_good_cookies_for_topic(self):
        self.wmi_server.set_response(raw_response='{"check_cookies":{"uid":"200"}}')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"cookie" : "123", "Origin" : "push.yandex.ru"}, **self.args(uid="200", topic="mytopic"))
        assert_ws_ok(resp, self.ws)

    def test_ws_fails_for_topic_with_multiple_uids(self):
        self.wmi_server.set_response(raw_response='{"check_cookies": {"uid":"200"}}')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"cookie" : "123", "Origin" : "push.yandex.ru"}, **self.args(uid="200,300", topic="mytopic"))
        assert_ws_bad_request(resp, self.ws, "request with topic should contain only one user id")

    def test_ws_success_with_good_multicookies_for_topic(self):
        self.wmi_server.set_response(raw_response='{"check_cookies":{"uid":"200", "childUids":["800"]}}')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"cookie" : "123", "Origin" : "push.yandex.ru"}, **self.args(uid="200", topic="mytopic"))
        assert_ws_ok(resp, self.ws)

    def test_ws_bb_connection_id_passing(self):
        self.wmi_server.set_response(raw_response='{"check_cookies": {"uid":"200", "bbConnectionId":"s:1533201902388:UaKasg:2"}}')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"cookie" : "123", "Origin" : "push.yandex.ru"}, **self.args(uid="200", service="fake"))
        wait(lambda: len(self.hub_server.requests) == 1, 2)
        assert_ws_ok(resp, self.ws)
        assert_in('/subscribe', self.hub_server.requests[0].path)
        assert_in('bb_connection_id=s%3a1533201902388%3aUaKasg%3a2', self.hub_server.requests[0].path)

    def test_ws_sub_id_passing(self):
        self.wmi_server.set_response(raw_response='{"check_cookies": {"uid":"200", "bbConnectionId":"s:1533201902388:UaKasg:2"}}')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"cookie" : "123", "Origin" : "push.yandex.ru"}, **self.args(uid="200", service="fake"))
        wait(lambda: len(self.hub_server.requests) == 1, 2)
        assert_ws_ok(resp, self.ws)
        assert_in('/subscribe', self.hub_server.requests[0].path)
        assert_regexp_in('id=[a-z0-9]{40}&', self.hub_server.requests[0].path)

    def test_ws_session_as_sub_id(self):
        self.wmi_server.set_response(raw_response='{"check_cookies": {"uid":"300", "bbConnectionId":"s:1533201902388:UaKasg:3"}}')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"cookie" : "123", "Origin" : "push.yandex.ru"}, **self.args(uid="300", service="tests-system-session-as-ws-subscription-id"))
        wait(lambda: len(self.hub_server.requests) == 1, 2)
        assert_ws_ok(resp, self.ws)
        assert_in('/subscribe', self.hub_server.requests[0].path)
        assert_in('id=ABCD-EFGH', self.hub_server.requests[0].path)
