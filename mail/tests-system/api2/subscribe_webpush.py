from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
import json

def setUp(self):
    global xiva, hub_server, fallback_hub_server
    xiva = XivaApiV2(host='localhost', port=18083)
    hub_server = fake_server(host='localhost', port=17081, raw_response='OK')
    fallback_hub_server = fake_server(host='localhost', port=17082, raw_response='OK')

def tearDown(self):
    hub_server.fini()
    fallback_hub_server.fini()

class TestSubscribeWebpushWithXToken:
    def setup(self):
        self._good_args = { 'uid': "200",
            'client': "auto-test", 'session': "UUID1",
            'subscription': json.dumps(
                {'endpoint': "https://push.service.com/abajsdfm;",
                'something': "la:asdfi234ojaf-asdf234adf"}),
            'token': "12345678901234567890",
            'service': 'mail',
            'ttl': '12345',
        }
        hub_server.set_request_hook(self.check_hub_subscribe())
        fallback_hub_server.set_request_hook(self.check_hub_subscribe())
        hub_server.reset_state()
        fallback_hub_server.reset_state()

    def teardown(self):
        error_in_hook = hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook
        error_in_hook = fallback_hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def check_hub_subscribe(self, override = {}):
        pars = {}
        for key in ['service', 'uid', 'client', 'filter', 'extra', 'ttl']:
            pars[key] = override.get(key, None) or self._good_args.get(key, None) or ''
        pars['session_key'] = self._good_args['session']
        pars['id'] = 'webpush:' + self._good_args['session']
        pars['callback'] = 'webpush:' + urllib.quote_plus(self._good_args['subscription']).lower()

        return lambda req: check_caught_request(req, '/subscribe', **pars)

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_auth_valid(self):
        "subscribe/webpush with correct xiva-token"
        resp = xiva.subscribe_webpush(**self.args())
        assert_ok(resp)
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/webpush?', **self.args(token=None)),
            {'Authorization': 'Xiva 12345678901234567890'})
        assert_ok(resp)

    def test_auth_invalid_or_missing_xtoken(self):
        "subscribe/webpush with unknown or missing xiva-token"
        resp = xiva.subscribe_webpush(**self.args(token='123'))
        assert_unauthorized(resp, 'bad token')
        resp = xiva.subscribe_webpush(**self.args(token=''))
        assert_unauthorized(resp)
        resp = xiva.subscribe_webpush(**self.args(token=None))
        assert_unauthorized(resp)


    def test_error_invalid_subscription(self):
        "subscribe/webpush with subscription that is not valid json"
        resp = xiva.subscribe_webpush(**self.args(subscription=
            "https://push.service.com/abajsdfm;la:asdfi234ojaf-asdf234adf"))
        assert_bad_request(resp, 'invalid argument "subscription"')

    def test_error_no_uid(self):
        "subscribe/webpush without uid"
        resp = xiva.subscribe_webpush(**self.args(uid=None))
        assert_bad_request(resp, 'missing argument "user (uid)"')
        resp = xiva.subscribe_webpush(**self.args(uid=''))
        assert_bad_request(resp, 'missing argument "user (uid)"')

    def test_error_ttl_is_too_large(self):
        "subscribe/webpush with large ttl"
        resp = xiva.subscribe_webpush(**self.args(ttl=999999))
        assert_bad_request(resp, 'ttl is too large')

    def test_uses_default_ttl_if_omitted(self):
        "subscribe/webpush without ttl"
        hub_server.set_request_hook(self.check_hub_subscribe(override={'ttl': '17520'}))
        fallback_hub_server.set_request_hook(self.check_hub_subscribe(override={'ttl': '17520'}))
        resp = xiva.subscribe_webpush(**self.args(ttl=None))
        assert_ok(resp)

    def test_vapid_key(self):
        "subscribe/vapid_key returns urlsafe base64 public key"
        resp = xiva.GET("/v2/vapid_key", {})
        assert_ok(resp)
        assert_greater(len(resp.body), 60)
        assert_in('access-control-allow-origin', resp.headers)
        assert_equals(resp.headers['access-control-allow-origin'], '*')

    def test_uses_fallback_hub_on_request_timeout(self):
        "subscribe/webpush uses fallback hub server when main is not available"
        hub_server.emulate_unresponsive_server = True
        resp = xiva.subscribe_webpush(**self.args())
        hub_server.emulate_unresponsive_server = False
        assert_ok(resp)
        assert_equals(hub_server.total_requests, 1)
        assert_equals(fallback_hub_server.total_requests, 1)

    def test_returns_subscription_id(self):
        hub_server.set_request_hook(None)
        hub_server.set_response(raw_response = "fake-subscription-id")
        resp = xiva.subscribe_webpush(**self.args(service="tests-system-whitelisted", token="tests-system-whitelisted-ltoken"))
        assert_ok(resp, '{"subscription-id":"fake-subscription-id"}')

    def test_doesnt_return_subscription_id_for_blacklisted_services(self):
        hub_server.set_request_hook(None)
        hub_server.set_response(raw_response = "fake-subscription-id")
        resp = xiva.subscribe_webpush(**self.args(service="tests-system-blacklisted", token="tests-system-blacklisted-ltoken"))
        assert_ok(resp, "OK")

class TestUnSubscribeWebpushWithXToken:
    def setup(self):
        hub_server.set_request_hook(self.check_hub_unsubscribe_webpush)
        fallback_hub_server.set_request_hook(self.check_hub_unsubscribe_webpush)
        self._good_args = {
            'session': "UUID1",
            'token': "12345678901234567890",
            'service': 'mail',
            'uid': '400'
        }
        hub_server.reset_state()
        fallback_hub_server.reset_state()

    def teardown(self):
        error_in_hook = hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook
        error_in_hook = fallback_hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def check_hub_unsubscribe_webpush(self, req):
        pars = {}
        pars['service'] = self._good_args['service']
        pars['uid'] = self._good_args['uid']
        pars['subscription-id'] = 'webpush:' + self._good_args['session']
        check_caught_request(req, '/unsubscribe', **pars)

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_auth_valid(self):
        "unsubscribe/webpush with corrent xiva-token"
        resp = xiva.unsubscribe_webpush(**self.args())
        assert_ok(resp)
        resp = xiva.POST(xiva.prepare_url('/v2/unsubscribe/webpush?', **self.args(token=None)),
            {'Authorization': 'Xiva 12345678901234567890'})
        assert_ok(resp)

    def test_auth_invalid_or_missing_xtoken(self):
        "unsubscribe/webpush with unknown or missing xiva-token"
        resp = xiva.unsubscribe_webpush(**self.args(token='123'))
        assert_unauthorized(resp, 'bad token')
        resp = xiva.unsubscribe_webpush(**self.args(token=''))
        assert_unauthorized(resp)
        resp = xiva.unsubscribe_webpush(**self.args(token=None))
        assert_unauthorized(resp)

    def test_error_no_uid(self):
        "unsubscribe/webpush without uid"
        resp = xiva.unsubscribe_webpush(**self.args(uid=None))
        assert_bad_request(resp, 'request must contain either "uid" or "topic" arguments')
        resp = xiva.unsubscribe_webpush(**self.args(uid=''))
        assert_bad_request(resp, 'request must contain either "uid" or "topic" arguments')

    def test_error_topic_and_uid(self):
        "unsubscribe/webpush with topic and uid"
        resp = xiva.unsubscribe_webpush(**self.args(topic="mytopic"))
        assert_bad_request(resp, 'request can\'t contain both "uid" and "topic" arguments')

    def test_uses_fallback_hub_on_request_timeout(self):
        "unsubscribe/webpush uses fallback hub server when main is not available"
        hub_server.emulate_unresponsive_server = True
        resp = xiva.unsubscribe_webpush(**self.args())
        hub_server.emulate_unresponsive_server = False
        assert_ok(resp)
        assert_equals(hub_server.total_requests, 1)
        assert_equals(fallback_hub_server.total_requests, 1)
