from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
from json import dumps
from time import sleep

def setUp(self):
    global xiva, hub_server
    xiva = XivaApiV2(host='localhost', port=18083)
    hub_server = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown(self):
    hub_server.fini()

class TestApnsQueueWithXToken:
    def setup(self):
        self._subscribe_args = { 'uid': '200',
            'app_name': 'xiva.test.mail', 'platform': 'apnsqueue',
            'uuid': 'UUID1', 'push_token': 'PUSHTOKEN1',
            'token': '12345678901234567890',
            'device_id': 'DEV-ICE-ID',
            'service': 'mail'
        }
        callback_queue = 'apnsqueue:apns_queue/%s/%s' % (self._subscribe_args['app_name'],self._subscribe_args['device_id'])

        hub_args = {}
        for k1, k2 in [('uid',)*2, ('platform',)*2, ('uuid', 'session_key'), ('device_id',)*2, ('service',)*2]:
            hub_args[k2] = self._subscribe_args[k1]
        self.hub_args = [hub_args.copy(), hub_args.copy()]
        # Params for impl-subscription.
        self.hub_args[0]['uid'] = '%s_%s' % (self._subscribe_args['device_id'],self._subscribe_args['app_name'])
        self.hub_args[0]['callback'] = 'xivamob:%s/%s' % (self._subscribe_args['app_name'],self._subscribe_args['push_token'])
        self.hub_args[0]['service'] = 'apns_queue'
        self.hub_args[0]['client'] = 'queue'
        self.hub_args[0]['session_key'] = self._subscribe_args['device_id']
        self.hub_args[0]['id'] = 'mob:' + self._subscribe_args['device_id'].replace('-', '').lower()
        # Params for queue-subscription.
        self.hub_args[1]['callback'] = 'apnsqueue:apns_queue/%s/%s' % (self._subscribe_args['app_name'],self._subscribe_args['device_id'])
        self.hub_args[1]['client'] = 'mobile'
        self.hub_args[1]['platform'] = 'apns'
        self.hub_args[1]['id'] = 'mob:' + self._subscribe_args['uuid'].replace('-', '').lower()

        hub_server.set_fail_strategy([])
        hub_server.reset_state()
        hub_server.set_response(raw_response='OK')

    def teardown(self):
        sleep(0.1)
        error_in_hook = hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def check_hub_subscribe_queue(self, req):
        check_caught_request(req, '/subscribe_mobile', **self.hub_args[hub_server.total_requests - 1])

    def args(self, **kwargs):
        args = self._subscribe_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_ok(self):
        "subscribe/app with corrent args amkes two successive calls to /subscribe_mobile"
        hub_server.set_request_hook(self.check_hub_subscribe_queue)
        resp = xiva.subscribe_app(**self.args())
        assert_ok(resp)
        assert_equals(hub_server.total_requests, 2)

    def test_no_device(self):
        "subscribe/app with missing device fails when apns queue enabled"
        resp = xiva.subscribe_app(**self.args(device_id=None))
        assert_bad_request(resp, 'missing argument "device (device_id)"')
        resp = xiva.subscribe_app(**self.args(device_id=''))
        assert_bad_request(resp, 'missing argument "device (device_id)"')
        assert_equals(hub_server.total_requests, 0)

    def test_first_subscribe_failed(self):
        "fails on first /subscribe_mobile fail"
        hub_server.set_fail_strategy([0])
        resp = xiva.subscribe_app(**self.args())
        assert_equals(resp.status, 500)
        assert_equals(hub_server.total_requests, 1)

    def test_second_subscribe_failed(self):
        "fails on second /subscribe_mobile fail"
        hub_server.set_fail_strategy([1])
        resp = xiva.subscribe_app(**self.args())
        assert_equals(resp.status, 500)
        assert_equals(hub_server.total_requests, 2)

    def test_repeat(self):
        "/apns_queue_repeat calls hub with correct args"
        # Assume hub could only repeat 8,
        hub_server.set_response(raw_response='8')
        pars = { 'position': '123', 'count': '10', 'token': '12345678901234567890' }
        for key in ['app_name', 'device_id']:
            pars[key] = self._subscribe_args[key]

        hub_params = { 'count': pars['count'], 'uid': self.hub_args[0]['uid'], 'position': '113', 'service': 'apns_queue' }
        hub_server.set_request_hook(lambda req: check_caught_request(req, '/repeat_messages', **hub_params))
        resp = xiva.GET(xiva.prepare_url('/v2/apns_queue_repeat?', **pars), {})
        assert_ok(resp)
        assert_equals(resp.body, dumps({'result':{'repeated_count':8}}, separators=(',', ':')))

    def test_repeat_count(self):
        "/apns_queue_repeat returns bad request if count is too big"
        pars = { 'position': '123', 'count': '11' }
        pars['token'] = '12345678901234567890'
        for key in ['app_name', 'device_id', 'service']:
            pars[key] = self._subscribe_args[key]
        resp = xiva.GET(xiva.prepare_url('/v2/apns_queue_repeat?', **pars), {})
        assert_bad_request(resp, 'count is too big')
        sleep(0.1)
        assert_equals(hub_server.total_requests, 0)

    def test_repeat_fail(self):
        hub_server.set_fail_strategy([0])
        pars = { 'position': '123', 'count': '9' }
        pars['token'] = '12345678901234567890'
        for key in ['app_name', 'device_id', 'service']:
            pars[key] = self._subscribe_args[key]
        resp = xiva.GET(xiva.prepare_url('/v2/apns_queue_repeat?', **pars), {})
        assert_equals(resp.status, 200)

    def test_repeat_bad_response(self):
        hub_server.set_response(raw_response='fail')
        pars = { 'position': '123', 'count': '9' }
        pars['token'] = '12345678901234567890'
        for key in ['app_name', 'device_id', 'service']:
            pars[key] = self._subscribe_args[key]
        resp = xiva.GET(xiva.prepare_url('/v2/apns_queue_repeat?', **pars), {})
        assert_equals(resp.status, 200)
        assert_equals(resp.body, dumps({'result':{'repeated_count':0}}, separators=(',', ':')))
