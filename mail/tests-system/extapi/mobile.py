from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
import json
import time

def setUp(self):
    global xiva, mobile
    xiva = XivaApiExt(host='localhost', port=18083)
    mobile = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown(self):
    mobile.fini()

class TestSendMobileWithXToken:
    def setup(self):
        self._good_args = { 'token': '01234567890', 'payload': '123',
        'x-param': '456', 'param': '789', 'push_token': 'qwerty' }
        self._mobile_args = { 'payload': '123', 'x-param': '456', 'token': 'qwerty', 'param': None }
        self._batch_args = { 'payload': '123', 'x-param': '456', 'tokens': '["qwe","rty"]', 'param': None }

    def teardown(self):
        time.sleep(0.1)
        error_in_hook = mobile.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_push_xtoken_invalid_or_missing(self):
        resp = xiva.mobile_push_apns(**self.args(token='123'))
        assert_unauthorized(resp, 'bad token')
        resp = xiva.mobile_push_apns(**self.args(token=''))
        assert_unauthorized(resp)
        resp = xiva.mobile_push_apns(**self.args(token=None))
        assert_unauthorized(resp)

    def test_push_xtoken_in_header(self):
        resp = xiva.POST(xiva.prepare_url('/ext/v1/mobile_push/apns?', **self.args(token=None)),
            {'Authorization': 'Xiva 01234567890'})
        assert_ok(resp)

    def test_push_apns(self):
        mobile.set_request_hook(lambda req: check_caught_request(req, '/push/apns', **self._mobile_args))
        resp = xiva.mobile_push_apns(**self.args())
        assert_ok(resp)

    def test_push_gcm(self): # gcm_compatibility
        mobile.set_request_hook(lambda req: check_caught_request(req, '/push/gcm', **self._mobile_args)) # gcm_compatibility
        resp = xiva.mobile_push_gcm(**self.args()) # gcm_compatibility
        assert_ok(resp)

    def test_push_fcm(self):
        mobile.set_request_hook(lambda req: check_caught_request(req, '/push/gcm', **self._mobile_args)) # gcm_compatibility
        resp = xiva.mobile_push_fcm(**self.args())
        assert_ok(resp)

    def test_push_gcm_batch(self): # gcm_compatibility
        mobile.set_request_hook(lambda req: check_caught_request(req, '/batch_push/gcm', **self._batch_args)) # gcm_compatibility
        resp = xiva.mobile_batch_push_gcm(**self.args(push_token=None, push_tokens='["qwe","rty"]')) # gcm_compatibility
        assert_ok(resp)

    def test_push_fcm_batch(self):
        mobile.set_request_hook(lambda req: check_caught_request(req, '/batch_push/gcm', **self._batch_args)) # gcm_compatibility
        resp = xiva.mobile_batch_push_fcm(**self.args(push_token=None, push_tokens='["qwe","rty"]'))
        assert_ok(resp)

    def test_push_mpns(self):
        mobile.set_request_hook(lambda req: check_caught_request(req, '/push/mpns', **self._mobile_args))
        resp = xiva.mobile_push_mpns(**self.args())
        assert_ok(resp)

    def test_push_wns(self):
        mobile.set_request_hook(lambda req: check_caught_request(req, '/push/wns', **self._mobile_args))
        resp = xiva.mobile_push_wns(**self.args())
        assert_ok(resp)
