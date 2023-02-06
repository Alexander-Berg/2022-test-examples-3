from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
import time

class TestSubscribeWebsocketDisabledAuth:
    @classmethod
    def setup_class(cls):
        cls.xiva = XivaApiV2(host='localhost', port=18083)
        cls.xiva_client = XivaClient(host='localhost', port=18083, back_port=18080)
        cls.hub_server = fake_server(host='localhost', port=17081, raw_response='OK')
        cls.wmi_server = fake_server(host='localhost', port=17084, raw_response='OK')
        cls.oauth_server = fake_chunked_server(host='localhost', port=17080)

    @classmethod
    def teardown_class(cls):
        cls.hub_server.fini()
        cls.wmi_server.fini()
        cls.oauth_server.fini()

    def setup(self):
        self._good_args = { 'uid': '200', 'service': 'noauth',
            'client': 'test', 'session': 'ABCD-EFGH',
        }

    def teardown(self):
        error_in_hook = self.hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_ws_success_without_unnecessary_args(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args())
        assert_ws_ok(resp, ws)

    def test_ws_success_uid_list(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="2,3"))
        assert_ws_ok(resp, ws)

    def test_ws_ignores_bad_cookies(self):
        self.wmi_server.set_response(raw_response='{"check_cookies":{"userId":"400"}}')
        (resp, ws) = self.xiva.subscribe_websocket(headers={'cookie' : '123', 'Origin' : 'push.yandex.ru'}, **self.args())
        assert_ws_ok(resp, ws)

    def test_ws_ignore_bad_oauth(self):
        self.oauth_server.set_response('oauth/invalid-resp.xml')
        (resp, ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123"}, **self.args())
        assert_ws_ok(resp, ws)

    def test_ws_fail_service_list(self):
        "fails if not all services have auth disabled"
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(service="noauth,fake"))
        assert_ws_unauthorized(resp, ws)
