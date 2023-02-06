from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
import time

class TestServiceManagerOAuth:
    @classmethod
    def setup_class(cls):
        cls.xiva = XivaApiV2(host='localhost', port=18083)
        cls.xiva_client = XivaClient(host='localhost', port=18083, back_port=18080)
        cls.oauth_server = fake_chunked_server(host='localhost', port=17080)
        cls.hub_server = fake_server(host='localhost', port=17081, raw_response='OK')

    @classmethod
    def teardown_class(cls):
        cls.oauth_server.fini()
        cls.hub_server.fini()

    def setup(self):
        self._good_args = { 'uid': "200", 'service': "tst1",
            'client': "test", 'session': "ABCD-EFGH" }

    def teardown(self):
        error_in_hook = self.hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook
        error_in_hook = self.oauth_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_ws_fail_with_not_passport_service(self):
        self.oauth_server.set_response('oauth/valid-not-yandex-resp.xml')
        (resp, ws) = self.xiva.subscribe_websocket(headers={"Authorization":"OAuth 123"}, **self.args(uid="1", service="tst2"))
        assert_ws_unauthorized(resp, ws)
