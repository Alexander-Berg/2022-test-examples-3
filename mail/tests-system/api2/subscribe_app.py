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

class TestSubscribeAppWithXToken(object):
    def setup(self):
        self._good_args = { 'uid': "200",
            'app_name': "xiva.test.mail", 'platform': "fcm",
            'uuid': "UUID1", 'push_token': "PUSHTOKEN1",
            'token': "12345678901234567890",
            'service': 'mail'
        }
        hub_server.set_request_hook(self.check_hub_subscribe_mobile())
        fallback_hub_server.set_request_hook(self.check_hub_subscribe_mobile())
        hub_server.reset_state()
        fallback_hub_server.reset_state()

    def teardown(self):
        sleep(0.2)
        error_in_hook = hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook
        error_in_hook = fallback_hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def check_hub_subscribe_mobile(self, override = {}):
        pars = override # include all override keys
        defaults = { 'client': 'mobile' }
        for key in ['platform', 'service', 'uid', 'client']:
            pars[key] = pars.get(key, None) or self._good_args.get(key, None) or \
                            defaults.get(key, None) or ''
        pars['session_key'] = self._good_args['uuid']
        pars['callback'] = 'xivamob:%s/%s' % (self._good_args['app_name'],self._good_args['push_token'])
        pars['id'] = 'mob:' + self._good_args['uuid'].replace('-', '').lower()
        return lambda req: check_caught_request(req, '/subscribe_mobile', **pars)

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_auth_valid(self):
        "subscribe/app with corrent xiva-token"
        resp = xiva.subscribe_app(**self.args())
        assert_ok(resp)
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/app?', **self.args(token=None)),
            {'Authorization': 'Xiva 12345678901234567890'})
        assert_ok(resp)

    def test_auth_valid_with_pusher_service(self):
        "subscribe/app with corrent xiva-token, xiva replaces client with 'mobile'"
        hub_server.set_request_hook(self.check_hub_subscribe_mobile({'service':'disk-json'}))
        resp = xiva.subscribe_app(**self.args(service='disk-json'))
        assert_ok(resp)

    def test_auth_invalid_unknown_xtoken(self):
        "subscribe/app with unknown xiva-token"
        resp = xiva.subscribe_app(**self.args(token='123'))
        assert_unauthorized(resp, 'bad token')

    def test_error_no_xtoken(self):
        "subscribe/app without xiva-token"
        resp = xiva.subscribe_app(**self.args(token=''))
        assert_unauthorized(resp)
        resp = xiva.subscribe_app(**self.args(token=None))
        assert_unauthorized(resp)

    def test_error_no_uid(self):
        "subscribe/app without uid"
        resp = xiva.subscribe_app(**self.args(uid=None))
        assert_bad_request(resp, 'missing argument "user (uid)"')
        resp = xiva.subscribe_app(**self.args(uid=''))
        assert_bad_request(resp, 'missing argument "user (uid)"')

    def test_error_uid_with_separator(self):
        "subscribe/app with separator in uid"
        resp = xiva.subscribe_app(**self.args(uid="uid:1"))
        assert_bad_request(resp, 'uid can\'t contain ":"')
        hub_server.set_request_hook(None)
        resp = xiva.subscribe_app(**self.args(uid='uid:1', token='L123456', service='autoru'))
        assert_ok(resp)

    def test_error_fcm_blacklisted(self):
        "subscribe/app with blacklisted fcm token"
        resp = xiva.subscribe_app(**self.args(push_token='BLACKLISTED'))
        assert_bad_request(resp, 'invalid pushtoken')
        resp = xiva.subscribe_app(**self.args(platform='gcm', push_token='BLACKLISTED')) # gcm_compatibility
        assert_bad_request(resp, 'invalid pushtoken')

    def test_platform_resolve(self):
        "subscribe/app correctly translates deprecated platforms"
        hub_server.set_request_hook(self.check_hub_subscribe_mobile({'platform': 'fcm'}))
        resp = xiva.subscribe_app(**self.args(platform = 'a'))
        sleep(0.1)
        hub_server.set_request_hook(self.check_hub_subscribe_mobile({'platform': 'fcm'}))
        resp = xiva.subscribe_app(**self.args(platform = 'gcm')) # gcm_compatibility
        sleep(0.1)
        hub_server.set_request_hook(self.check_hub_subscribe_mobile({'platform': 'apns'}))
        resp = xiva.subscribe_app(**self.args(platform = 'ios'))
        assert_ok(resp)

    def test_uses_fallback_hub_on_request_timeout(self):
        "subscribe/app uses fallback hub server when main is not available"
        hub_server.emulate_unresponsive_server = True
        resp = xiva.subscribe_app(**self.args())
        hub_server.emulate_unresponsive_server = False
        assert_ok(resp)
        assert_equals(hub_server.total_requests, 1)
        assert_equals(fallback_hub_server.total_requests, 1)

    def test_connection_id_passing(self):
        "subscribe/app with X-BB-ConnectionID header"
        hub_server.set_request_hook(self.check_hub_subscribe_mobile({'bb_connection_id':'t:12345'}))
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/app?', **self.args()), {'X-BB-ConnectionID': 't:12345'})
        assert_ok(resp)

    def test_returns_subscription_id(self):
        hub_server.set_request_hook(None)
        hub_server.set_response(raw_response = "fake-subscription-id")
        resp = xiva.subscribe_app(**self.args(service="tests-system-whitelisted", token="tests-system-whitelisted-ltoken"))
        assert_ok(resp, '{"subscription-id":"fake-subscription-id"}')

    def test_doesnt_return_subscription_id_for_blacklisted_services(self):
        hub_server.set_request_hook(None)
        hub_server.set_response(raw_response = "fake-subscription-id")
        resp = xiva.subscribe_app(**self.args(service="tests-system-blacklisted", token="tests-system-blacklisted-ltoken"))
        assert_ok(resp, "OK")

    def test_content_type_urlencoded(self):
        requests = []
        hub_server.set_request_hook(lambda req: requests.append(req))
        resp = xiva.subscribe_app(**self.args())
        sleep(0.05)
        assert_ok(resp, "OK")
        eq_(len(requests), 1)
        assert_content_type_equals(requests[0], 'application/x-www-form-urlencoded; charset=UTF-8')

class TestSubscribeAppWithTVM(TestSubscribeAppWithXToken):
    def test_tvm_subscriber_can_subscribe(self):
        del self._good_args['token']
        response = xiva.subscribe_app(tvm_ticket=xiva.subscriber_tst_ticket, **self.args())
        assert_ok(response)

    def test_tvm_publisher_cannot_subscribe(self):
        del self._good_args['token']
        response = xiva.subscribe_app(tvm_ticket=xiva.publisher_tst_ticket, **self.args())
        assert_unauthorized(response, 'forbidden service')

class TestUnSubscribeAppWithXToken(object):
    def setup(self):
        hub_server.set_request_hook(self.check_hub_unsubscribe)
        fallback_hub_server.set_request_hook(self.check_hub_unsubscribe)
        self._good_args = {
            'uuid': "UUID1", 'push_token': "PUSHTOKEN1",
            'token': "12345678901234567890",
            'service': 'mail',
            'uid': '400'
        }
        hub_server.reset_state()
        fallback_hub_server.reset_state()

    def teardown(self):
        sleep(0.1)
        error_in_hook = hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def check_hub_unsubscribe(self, req):
        params = {
            'uid': '400',
            'service': self._good_args['service'],
            'subscription-id': 'mob:' + self._good_args['uuid'].replace('-', '').lower()
        }
        check_caught_request(req, '/unsubscribe', **params)

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_auth_valid(self):
        "unsubscribe/app with correct xiva-token"
        resp = xiva.unsubscribe_app(**self.args())
        assert_ok(resp)
        resp = xiva.POST(xiva.prepare_url('/v2/unsubscribe/app?', **self.args(token=None)),
            {'Authorization': 'Xiva 12345678901234567890'})
        assert_ok(resp)

    def test_auth_invalid_unknown_xtoken(self):
        "unsubscribe/app with unknown xiva-token"
        resp = xiva.unsubscribe_app(**self.args(token='123'))
        assert_unauthorized(resp, 'bad token')

    def test_error_no_xtoken(self):
        "unsubscribe/app without xiva-token"
        resp = xiva.unsubscribe_app(**self.args(token=''))
        assert_unauthorized(resp)
        resp = xiva.unsubscribe_app(**self.args(token=None))
        assert_unauthorized(resp)

    def test_error_no_uid(self):
        "unsubscribe/app without uid"
        resp = xiva.unsubscribe_app(**self.args(uid=None))
        assert_bad_request(resp, 'request authorized with xiva token must contain either "uid" or "topic" arguments')
        resp = xiva.unsubscribe_app(**self.args(uid=''))
        assert_bad_request(resp, 'request authorized with xiva token must contain either "uid" or "topic" arguments')

    def test_error_topic_and_uid(self):
        "unsubscribe/app with topic and uid"
        resp = xiva.unsubscribe_app(**self.args(topic="mytopic"))
        assert_bad_request(resp, 'request can\'t contain both "uid" and "topic" arguments')

    def test_uses_fallback_hub_on_request_timeout(self):
        "unsubscribe/app uses fallback hub server when main is not available"
        hub_server.emulate_unresponsive_server = True
        resp = xiva.unsubscribe_app(**self.args())
        hub_server.emulate_unresponsive_server = False
        assert_ok(resp)
        assert_equals(hub_server.total_requests, 1)
        assert_equals(fallback_hub_server.total_requests, 1)

class TestUnSubscribeAppWithTVM(TestUnSubscribeAppWithXToken):
    def test_tvm_subscriber_can_unsubscribe(self):
        del self._good_args['token']
        response = xiva.unsubscribe_app(tvm_ticket=xiva.subscriber_tst_ticket, **self.args())
        assert_ok(response)

    def test_tvm_publisher_cannot_unsubscribe(self):
        del self._good_args['token']
        response = xiva.unsubscribe_app(tvm_ticket=xiva.publisher_tst_ticket, **self.args())
        assert_unauthorized(response, 'forbidden service')

class TestScheduleUnsubscribeAppWithXToken:
    def setup(self):
        hub_server.set_request_hook(self.check_hub_unsubscribe_mobile)
        fallback_hub_server.set_request_hook(self.check_hub_unsubscribe_mobile)
        self._good_args = {
            'uuid': "UUID1",
            'token': "12345678901234567890",
            'service': 'mail',
            'platform': 'apns'
        }
        hub_server.reset_state()
        fallback_hub_server.reset_state()

    def teardown(self):
        sleep(0.1)
        error_in_hook = hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def check_hub_unsubscribe_mobile(self, req):
        check_caught_request(req, '/add_broken_subscription',
            service=self._good_args['service'],
            platform=self._good_args['platform'],
            id='mob:'+self._good_args['uuid'].lower(),
        )

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_auth_valid(self):
        "schedule_unsubscribe/app with correct xiva-token"
        resp = xiva_ext.schedule_unsubscribe_app(**self.args())
        assert_ok(resp)
        resp = xiva.POST(xiva.prepare_url('/ext/v2/schedule_unsubscribe/app?', **self.args(token=None)),
            {'Authorization': 'Xiva 12345678901234567890'})
        assert_ok(resp)

    def test_auth_invalid_or_missing_xtoken(self):
        "schedule_unsubscribe/app with missing or unknown xiva-token"
        resp = xiva_ext.schedule_unsubscribe_app(**self.args(token='123'))
        # Should return ok - authorization disabled, see RTEC-4605
        assert_ok(resp)

    def test_error_no_uuid(self):
        "schedule_unsubscribe/app without uuid"
        resp = xiva_ext.schedule_unsubscribe_app(**self.args(uuid=None))
        assert_bad_request(resp, 'missing argument "uuid"')

    def test_uses_fallback_hub_on_request_timeout(self):
        "schedule_unsubscribe/app uses fallback hub server when main is not available"
        hub_server.emulate_unresponsive_server = True
        resp = xiva_ext.schedule_unsubscribe_app(**self.args())
        hub_server.emulate_unresponsive_server = False
        assert_ok(resp)
        assert_equals(hub_server.total_requests, 1)
        assert_equals(fallback_hub_server.total_requests, 1)
