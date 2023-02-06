from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
from pycommon.wait_condition import *
import time

# this suite covers only auth check, for more tests cases see TestSubscribeWebsocketWithSign
class TestSubscribeWebsocketWithOauth:
    @classmethod
    def setup_class(cls):
        cls.xiva = XivaApiV2(host='localhost', port=18083)
        cls.xiva_client = XivaClient(host='localhost', port=18083, back_port=18080)
        cls.oauth_server = fake_chunked_server(host='localhost', port=17080)
        cls.hub_server = fake_server(host='::', port=17081, raw_response='OK')

    @classmethod
    def teardown_class(cls):
        cls.oauth_server.fini()
        cls.hub_server.fini()

    def setup(self):
        self._good_args = { 'uid': "200", 'service': "tst1",
            'client': "test", 'session': "ABCD-EFGH" }
        self.hub_server.requests = []
        self.hub_server.set_request_hook(lambda req: self.hub_server.requests.append(req))
        self.ws = None

    def teardown(self):
        time.sleep(0.05)
        error_in_hook = self.oauth_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook
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

    def test_ws_with_no_arguments_fails_with_4400(self):
        (resp, self.ws) = self.xiva.subscribe_websocket()
        assert_ws_bad_request(resp, self.ws, 'missing argument "user (uid)"')

    def test_ws_without_oauth_token_fails_with_4401(self):
        (resp, self.ws) = self.xiva.subscribe_websocket(**self.args())
        assert_ws_unauthorized(resp, self.ws)

    def test_ws_fails_on_invalid_authorization_header(self):
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"123"}, **self.args())
        assert_ws_unauthorized(resp, self.ws, 'unsupported authorization header')

    def test_ws_fails_on_invalid_oauth_token(self):
        self.oauth_server.set_response('oauth/invalid-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123"}, **self.args())
        assert_ws_unauthorized(resp, self.ws, 'auth failed')

    def test_ws_fails_on_uid_mismatch(self):
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123"}, **self.args())
        assert_ws_unauthorized(resp, self.ws, 'auth failed')

    def test_ws_fail_with_non_existent_service(self):
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123"}, **self.args(uid="1", service="fake_fake"))
        assert_ws_bad_request(resp, self.ws)

    def test_ws_success(self):
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123"}, **self.args(uid="1", service="fake"))
        assert_ws_ok(resp, self.ws)

    def test_ws_fail_not_yandex(self):
        self.oauth_server.set_response('oauth/valid-not-yandex-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123"}, **self.args(uid="1", service="fake"))
        assert_ws_unauthorized(resp, self.ws, 'auth failed')

    def test_oauth_token_get_parameter(self):
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(**self.args(uid="1", service="fake", oauth_token="123"))
        assert_ws_ok(resp, self.ws)

    def test_oauth_token_precedence(self):
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        # Use assert_equals because assert is a statement and can't be used inside lambda.
        self.oauth_server.set_request_hook(lambda req: assert_in('oauth_token=123', req.body))
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123"}, **self.args(uid="1", service="fake", oauth_token="321"))
        assert_ws_ok(resp, self.ws)

    def test_ws_fails_on_uid_list(self):
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123"}, **self.args(uid="1,2"))
        assert_ws_bad_request(resp, self.ws, 'only one user id is allowed in OAuth authentication method')

    def test_unknown_service_fails_with_4400(self):
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123"}, **self.args(uid="1", service="unknown"))
        assert_ws_bad_request(resp, self.ws, 'no service unknown')

    def test_ws_topic_are_not_supported(self):
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123"}, **self.args(uid="1",topic="mytopic"))
        assert_ws_bad_request(resp, self.ws, "topics are not supported for OAuth authentication method")

    def test_ws_service_list(self):
        args = self.args(uid="1", service="fake,tst1", oauth_token="123")
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(**args)
        assert_ws_ok(resp, self.ws)

    def test_ws_service_list_v1(self):
        args = self.args(uid="1", service="fake,tst1", oauth_token="123")
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        (resp, self.ws) = self.xiva.WS(self.xiva.prepare_url("/v1/subscribe/websocket?", **args), {})
        assert_ws_ok(resp, self.ws)

    def test_ws_connection_id_passing(self):
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123",
            "X-BB-ConnectionID":"t:12345"}, **self.args(uid="1", service="fake"))
        wait(lambda: len(self.hub_server.requests) == 1, 2)
        assert_ws_ok(resp, self.ws)
        assert_in('/subscribe', self.hub_server.requests[0].path)
        assert_in('bb_connection_id=t%3a254050310', self.hub_server.requests[0].path)

    def test_ws_sub_id_passing(self):
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123",
            "X-BB-ConnectionID":"t:12345"}, **self.args(uid="1", service="fake"))
        wait(lambda: len(self.hub_server.requests) == 1, 2)
        assert_ws_ok(resp, self.ws)
        assert_in('/subscribe', self.hub_server.requests[0].path)
        assert_regexp_in('id=[a-z0-9]{40}&', self.hub_server.requests[0].path)

    def test_ws_session_as_sub_id(self):
        self.oauth_server.set_response('oauth/valid-yandex-resp.xml')
        (resp, self.ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123",
            "X-BB-ConnectionID":"t:12345"}, **self.args(uid="1", service="tests-system-session-as-ws-subscription-id"))
        wait(lambda: len(self.hub_server.requests) == 1, 2)
        assert_ws_ok(resp, self.ws)
        assert_in('/subscribe', self.hub_server.requests[0].path)
        assert_in('id=ABCD-EFGH', self.hub_server.requests[0].path)
