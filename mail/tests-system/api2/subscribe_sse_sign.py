from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
import time

class TestSubscribeSSEWithSign:
    @classmethod
    def setup_class(cls):
        cls.xiva = XivaApiV2(host='localhost', port=18083)
        cls.xiva_client = XivaClient(host='localhost', port=18083, back_port=18080)
        cls.hub_server = fake_server(host='localhost', port=17081, raw_response='OK')

    @classmethod
    def teardown_class(cls):
        cls.hub_server.fini()

    def setup(self):
        self._good_args = { 'uid': "200", 'service': "tst1",
            'client': "test", 'session': "ABCD-EFGH",
        }
        self._sign_uid_service = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "service" : "tst1" })
        self._sign_uid_two_services = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "service" : "tst1,tst2" })
        self._sign_two_uids_two_services = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200,300", "service" : "tst1,tst2" })

    def teardown(self):
        error_in_hook = self.hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_if_no_arguments_fails_with_400(self):
        resp = self.xiva.subscribe_sse()
        assert_bad_request(resp, 'missing argument "user (uid)"')

    def test_without_sign_fails_with_401(self):
        resp = self.xiva.subscribe_sse(**self.args())
        assert_unauthorized(resp, 'Unauthorized')

    # DISABLED because SSE connections have late close detection
    # and affects websocket tests
    #
    # def test_success_with_correct_args(self):
    #     resp = self.xiva.subscribe_sse(**self.args(**self._sign_uid_service))
    #     assert_equals(resp.code, 200)
    # def test_ping_is_sent_twice_on_success(self):
    #     resp = self.xiva.subscribe_sse(**self.args(**self._sign_uid_service))
    #     assert_equals(resp.code, 200)
    #     parsed_msg = json.loads(resp.recv_message())
    #     assert_in("operation", parsed_msg)
    #     assert_equals(parsed_msg["operation"], "ping")
    #     assert_in("server-interval-sec", parsed_msg)
    #     assert_equals(resp.recv_message(), '{ "operation": "opera-fix" }')
