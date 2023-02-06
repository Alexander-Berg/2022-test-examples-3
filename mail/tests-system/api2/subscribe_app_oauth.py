from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
import time

def setUp(self):
    global xiva, oauth_server, hub_server
    xiva = XivaApiV2(host='localhost', port=18083)
    oauth_server = fake_chunked_server(host='localhost', port=17080)
    hub_server = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown(self):
    oauth_server.fini()
    hub_server.fini()

class SubscribeAppWithOauth(object):
    def setup(self, args):
        hub_server.set_request_hook(self.check_hub_subscribe_mobile)
        self._good_args = args

    def teardown(self):
        time.sleep(0.2)
        error_in_hook = hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def check_hub_subscribe_mobile(self, req):
        check_caught_request(req, '/subscribe_mobile',
            platform=self._good_args['platform'],
            service=self._good_args['service'],
            client='mobile',
            bb_connection_id='t:254050310',
            uid='1',
            session_key=self._good_args['uuid'],
            callback='xivamob:%s/%s' % (self._good_args['app_name'],self._good_args['push_token']),
            id='mob:' + self._good_args['uuid'].replace('-', '').lower()
        )

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_oauth_valid_token(self):
        "subscribe/app with oauth; good oauth response"
        oauth_server.set_response('oauth/valid-yandex-resp.xml')
        resp = xiva.subscribe_app(**self.args())
        assert_ok(resp)

    def test_oauth_valid_token_ignore_uid_from_url(self):
        "subscribe/app with oauth and explicit uid that must be ignored"
        oauth_server.set_response('oauth/valid-yandex-resp.xml')
        resp = xiva.subscribe_app(**self.args(uid='200'))
        assert_ok(resp)

    def test_oauth_invalid_token_ignore_any_other_info(self):
        "subscribe/app with oauth; error oauth response with some extra data about token that should be ignored"
        oauth_server.set_response('oauth/invalid-extra-resp.xml')
        resp = xiva.subscribe_app(**self.args())
        assert_unauthorized(resp)

    def test_oauth_invalid_bb_response_format(self):
        "subscribe/app with oauth; oauth response in invalid format"
        oauth_server.set_response('oauth/invalid-trash-resp.xml')
        resp = xiva.subscribe_app(**self.args())
        assert_unauthorized(resp)

    def test_oauth_valid_token_but_depersonalized(self):
        "subscribe/app with oauth; oauth response doesnt contain uid"
        oauth_server.set_response('oauth/valid-yandex-resp-without-uid.xml')
        resp = xiva.subscribe_app(**self.args())
        assert_unauthorized(resp)

    def test_error_no_oauth_token_in_request(self):
        "subscribe/app without uid nor oauth token"
        oauth_server.set_response('oauth/valid-yandex-resp.xml')
        resp = xiva.subscribe_app(**self.args(oauth_token=None))
        assert_unauthorized(resp)
        resp = xiva.subscribe_app(**self.args(oauth_token=''))
        assert_unauthorized(resp)

    def test_topics_not_supported_with_oauth(self):
        "subscribe/app with topic; should fail with Bad Request"
        oauth_server.set_response('oauth/valid-yandex-resp.xml')
        resp = xiva.subscribe_app(**self.args(topic="mytopic"))
        assert_bad_request(resp, "topics are not supported for OAuth authentication method")

    def test_oauth_connection_id_precedence(self):
        "subscribe/app with oauth; bb_connection_id must be taken from oauth, not header"
        oauth_server.set_response('oauth/valid-yandex-resp.xml')
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/app?', **self.args()),
            {'Authorization': 'OAuth 123', 'X-BB-ConnectionID': 't:12345'})
        assert_ok(resp)

class TestSubscribeAppWithOauth(SubscribeAppWithOauth):
    def setup(self):
        super(self.__class__, self).setup({ 'oauth_token': "123", 'service': "fake",
            'app_name': "xiva.test.mail", 'platform': "fcm",
            'uuid': "UUID1", 'push_token': "PUSHTOKEN1"
        })


class UnSubscribeAppWithOauth(object):
    def setup(self, args):
        hub_server.set_request_hook(self.check_hub_unsubscribe)
        self._good_args = args

    def teardown(self):
        time.sleep(0.1)
        error_in_hook = hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def check_hub_unsubscribe(self, req):
        params = {
            'uid': '1',
            'service': self._good_args['service'],
            'subscription-id': 'mob:' + self._good_args['uuid'].replace('-', '').lower()
        }
        check_caught_request(req, '/unsubscribe', **params)

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_oauth_valid_token(self):
        "unsubscribe/app with oauth; good oauth response"
        oauth_server.set_response('oauth/valid-yandex-resp.xml')
        resp = xiva.unsubscribe_app(**self.args())
        assert_ok(resp)

    def test_oauth_valid_token_ignore_uid_from_url(self):
        "unsubscribe/app with oauth and explicit uid that must be ignored"
        oauth_server.set_response('oauth/valid-yandex-resp.xml')
        resp = xiva.unsubscribe_app(**self.args(uid='200'))
        assert_ok(resp)

    def test_oauth_invalid_token_ignore_any_other_info(self):
        "unsubscribe/app with oauth; error oauth response with some extra data about token that should be ignored"
        oauth_server.set_response('oauth/invalid-extra-resp.xml')
        resp = xiva.unsubscribe_app(**self.args())
        assert_unauthorized(resp)

    def test_oauth_invalid_bb_response_format(self):
        "unsubscribe/app with oauth; oauth response in invalid format"
        oauth_server.set_response('oauth/invalid-trash-resp.xml')
        resp = xiva.unsubscribe_app(**self.args())
        assert_unauthorized(resp)

    def test_oauth_valid_token_but_depersonalized(self):
        "unsubscribe/app with oauth; oauth response doesnt contain uid"
        oauth_server.set_response('oauth/valid-yandex-resp-without-uid.xml')
        resp = xiva.unsubscribe_app(**self.args())
        assert_unauthorized(resp)

    def test_error_no_oauth_token_in_request(self):
        "call unsubscribe/app without uid nor oauth token"
        oauth_server.set_response('oauth/valid-yandex-resp.xml')
        resp = xiva.unsubscribe_app(oauth_token="", service="mail",
                        uuid="UUID1", push_token="PUSHTOKEN1")
        assert_unauthorized(resp)
        resp = xiva.unsubscribe_app(oauth_token=None, service="mail",
                        uuid="UUID1", push_token="PUSHTOKEN1")
        assert_unauthorized(resp)

    def test_topics_not_supported_with_oauth(self):
        "unsubscribe/app with topic; should fail with Bad Request"
        oauth_server.set_response('oauth/valid-yandex-resp.xml')
        resp = xiva.unsubscribe_app(**self.args(topic="mytopic"))
        assert_bad_request(resp, "topics are not supported for OAuth authentication method")

class TestUnSubscribeAppWithOauth(UnSubscribeAppWithOauth):
    def setup(self):
        super(self.__class__, self).setup({ 'oauth_token': "123", 'service': "fake",
            'uuid': "UUID1", 'push_token': "PUSHTOKEN1"
        })
