from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
from time import sleep

def setUp(self):
    global xiva, xiva_ext, hub_server, fallback_hub_server
    xiva = XivaApiV2(host='localhost', port=18083)
    xiva_ext = XivaApiExt(host='localhost', port=18083)
    hub_server = fake_server(host='localhost', port=17081, raw_response='OK')
    fallback_hub_server = fake_server(host='localhost', port=17082, raw_response='OK')

def tearDown(self):
    hub_server.fini()
    fallback_hub_server.fini()

class TestServiceManagerSubscribeAppWithXTokenGCM: # gcm_compatibility
    def setup(self):
        self._good_args = { 'uid': '200',
            'app_name': 'xiva.test.mail', 'platform': 'gcm', # gcm_compatibility
            'uuid': 'UUID1', 'push_token': 'PUSHTOKEN1',
            'token': '12345678901234567890',
            'service': 'mail'
        }

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_app_not_registered(self):
        "subscribe/app with app_name not registered in xconf"
        resp = xiva.subscribe_app(**self.args(app_name='xiva.fake'))
        assert_bad_request(resp, 'application xiva.fake for platform fcm is not registered')

    def test_app_no_secret(self):
        "subscribe/app with app_name registered in xconf with empty secret"
        resp = xiva.subscribe_app(**self.args(app_name='xiva.test.noapp', platform='a'))
        assert_bad_request(resp, 'application xiva.test.noapp for platform fcm is not registered')
        resp = xiva.subscribe_app(**self.args(app_name='xiva.test.noapp', platform='i'))
        assert_bad_request(resp, 'application xiva.test.noapp for platform apns is not registered')

    def test_mpns_app(self):
        "subscribe/app with mpns platform which doesn't require registration"
        resp = xiva.subscribe_app(**self.args(app_name='xiva.fake', platform='mpns'))
        assert_ok(resp)

class TestServiceManagerSubscribeAppWithXToken:
    def setup(self):
        self._good_args = { 'uid': '200',
            'app_name': 'xiva.test.mail', 'platform': 'fcm',
            'uuid': 'UUID1', 'push_token': 'PUSHTOKEN1',
            'token': '12345678901234567890',
            'service': 'mail'
        }

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_app_not_registered(self):
        "subscribe/app with app_name not registered in xconf"
        resp = xiva.subscribe_app(**self.args(app_name='xiva.fake'))
        assert_bad_request(resp, 'application xiva.fake for platform fcm is not registered')

    def test_app_no_secret(self):
        "subscribe/app with app_name registered in xconf with empty secret"
        resp = xiva.subscribe_app(**self.args(app_name='xiva.test.noapp', platform='a'))
        assert_bad_request(resp, 'application xiva.test.noapp for platform fcm is not registered')
        resp = xiva.subscribe_app(**self.args(app_name='xiva.test.noapp', platform='i'))
        assert_bad_request(resp, 'application xiva.test.noapp for platform apns is not registered')

    def test_mpns_app(self):
        "subscribe/app with mpns platform which doesn't require registration"
        resp = xiva.subscribe_app(**self.args(app_name='xiva.fake', platform='mpns'))
        assert_ok(resp)
