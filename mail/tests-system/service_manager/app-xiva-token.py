from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
from time import sleep

def setUp(self):
    global xiva, hub_server
    xiva = XivaApiV2(host='localhost', port=18083)
    hub_server = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown(self):
    hub_server.fini()

class TestSubscribeAppWithXToken:
    def setup(self):
        self._good_args = { 'uid': "200",
            'app_name': "xiva.test.mail", 'platform': "fcm",
            'uuid': "UUID1", 'push_token': "PUSHTOKEN1",
            'token': "L003",
            'service': 'tst1'
        }
        hub_server.set_request_hook(self.check_hub_subscribe_mobile())
        hub_server.reset_state()

    def teardown(self):
        sleep(0.1)
        error_in_hook = hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def check_hub_subscribe_mobile(self, override = {}):
        pars = {}
        defaults = { 'client': 'mobile' }
        for key in ['platform', 'service', 'uid', 'client']:
            pars[key] = override.get(key, None) or self._good_args.get(key, None) or \
                            defaults.get(key, None) or ''
        pars['session_key'] = self._good_args['uuid']
        pars['callback'] = 'xivamob:%s/%s' % (self._good_args['app_name'],self._good_args['push_token'])

        return lambda req: check_caught_request(req, '/subscribe_mobile', **pars)

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_auth_services_second_token_valid(self):
        "subscribe/app with service's second active xiva-token"
        resp = xiva.subscribe_app(**self.args())
        assert_ok(resp)
