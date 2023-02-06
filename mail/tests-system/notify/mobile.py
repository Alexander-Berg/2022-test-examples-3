from pycommon import *
from subscriber import *
import time
from urlparse import urlparse, parse_qsl
from json import dumps

def setUp(self):
    global hub, mobile
    hub = Client(Testing.host(), Testing.port())
    hub.unsubscribe_all(Testing.uid1(), Testing.service1())

    mobile = FakeXivaMobile(Testing.mobile_port())
    mobile.start()

def tearDown(self):
    global mobile
    mobile.stop()

class TestNotifyMobile:
    def setup(self):
        mobile.impl.set_response(raw_response='OK')
        self.app = 'ru.yandex.mail'
        self.token = 'pushtokenABCDE'
        self.token2 = 'pushtokenZYXWV'

    def teardown(self):
        mobile.requests = []
        mobile.bodies = []
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())
        hub.unsubscribe_all(Testing.uid1(), 'mail')
        hub.unsubscribe_all(Testing.uid1(), 'apns_queue')
        hub.unsubscribe_all('fake', 'mail')

    def subscribe_one_session(self, service=Testing.service1(), platform='apns'):
        hub.subscribe_mobile(Testing.uid1(), service, 'xivamob:%s/%s' % (self.app, self.token), session_key='a', platform=platform)

    def subscribe_two_sessions(self):
        self.subscribe_one_session()
        hub.subscribe_mobile(Testing.uid1(), Testing.service1(), 'xivamob:%s/%s' % (self.app, self.token2), session_key='b', platform='fcm')

    def subscribe_one_session_to_mail(self, platform='apns'):
        self.subscribe_one_session('mail', platform)

    def subscribe_two_sessions_to_mail(self):
        self.subscribe_one_session_to_mail()
        hub.subscribe_mobile(Testing.uid1(), 'mail', 'xivamob:%s/%s' % (self.app, self.token2), session_key='b', platform='fcm')

    def make_text_message(self, service=Testing.service1(), payload=None, transit_id=None):
        ret = make_message(uid=Testing.uid1(), service=service)
        if payload is not None:
            ret[MessageFields.raw_data] = payload
        if transit_id is not None:
            ret[MessageFields.transit_id] = transit_id
        return ret

    def make_binary_message(self):
        ret = make_message(uid=Testing.uid1(), service=Testing.service1(), raw_data=TestData.binary_payload)
        ret[MessageFields.type] = MessageContentType.binary
        return ret

    def test_mobile_notify(self):
        "check that correct request is made to mobile"

        payload = dumps({'data': 'something'}, separators=(',', ':'))
        aps = dumps({'title': 'test'}, separators=(',', ':'));

        self.subscribe_one_session()
        message = self.make_text_message()
        message[MessageFields.repacking_rules] = {'apns': '{"aps": %s}' % (aps)}
        message[MessageFields.raw_data] = payload
        hub.fast_binary_notify(message)

        wait(lambda: len(mobile.requests) == 1, 2)
        assert_equals(len(mobile.requests), 1)
        get_params = dict(parse_qsl(urlparse(urllib.unquote(mobile.requests[0].path)).query))
        post_params = dict(parse_qsl(urllib.unquote(mobile.requests[0].body)))

        assert_in('uid', get_params)
        assert_in('service', get_params)
        assert_in('app', get_params)
        assert_in('transit_id', get_params)
        assert_in('session', get_params)
        assert_in('ttl', get_params)
        assert_not_in('token', get_params)

        assert_in('x-aps', post_params)
        assert_in('token', post_params)
        assert_in('payload', post_params)

        assert_equals(get_params['uid'], Testing.uid1())
        assert_equals(get_params['service'], Testing.service1())
        assert_equals(get_params['app'], self.app)
        assert_equals(get_params['app'], self.app)

        assert_equals(post_params['token'], self.token)
        assert_equals(json.loads(post_params['payload'])['data'], 'something')
        assert_equals(post_params['x-aps'], aps)

    def test_transit_id_inject(self):
        "check transit id is injected to payload"

        payload = dumps({'data': 'something'}, separators=(',', ':'))
        aps = dumps({'title': 'test'}, separators=(',', ':'));

        self.subscribe_one_session()
        message = self.make_text_message()
        message[MessageFields.repacking_rules] = {'apns': '{"aps": %s}' % (aps)}
        message[MessageFields.raw_data] = payload
        hub.fast_binary_notify(message)

        wait(lambda: len(mobile.requests) == 1, 2)
        assert_equals(len(mobile.requests), 1)
        get_params = dict(parse_qsl(urlparse(urllib.unquote(mobile.requests[0].path)).query))
        post_params = dict(parse_qsl(urllib.unquote(mobile.requests[0].body)))

        assert_in('uid', get_params)
        assert_in('service', get_params)
        assert_in('app', get_params)
        assert_in('transit_id', get_params)
        assert_in('session', get_params)
        assert_in('ttl', get_params)
        assert_not_in('token', get_params)

        assert_in('x-aps', post_params)
        assert_in('token', post_params)
        assert_in('payload', post_params)

        assert_equals(get_params['uid'], Testing.uid1())
        assert_equals(get_params['service'], Testing.service1())
        assert_equals(get_params['app'], self.app)
        assert_equals(get_params['app'], self.app)

        assert_equals(post_params['token'], self.token)
        assert_equals(json.loads(post_params['payload'])['xiva']['transit_id'], message[MessageFields.transit_id])
        assert_equals(post_params['x-aps'], aps)

    def test_collapse_id_injected_for_mail_apns_and_apnsqueue(self):
        payload = dumps({'data': 'something'}, separators=(',', ':'))
        aps = dumps({'title': 'test'}, separators=(',', ':'));

        self.subscribe_one_session_to_mail('apns')

        # Apns_queue apnsqueue.
        self.subscribe_one_session(service='apns_queue', platform='apnsqueue')
        hub.fast_binary_notify(self.make_text_message('apns_queue', payload, 'mail_transit_id'))

        wait(lambda: len(mobile.requests) == 1, 2)
        assert_equals(len(mobile.requests), 1)

        post_params = dict(parse_qsl(urllib.unquote(mobile.requests[0].body)))
        assert_in('x-collapse-id', post_params)
        assert_equals(post_params['x-collapse-id'], 'mail_transit_id')

        # Custom repack.
        message = self.make_text_message('mail', payload, 'mail_transit_id')
        message[MessageFields.repacking_rules] = {'apns': '{"aps": %s}' % (aps)}
        hub.fast_binary_notify(message)

        wait(lambda: len(mobile.requests) == 2, 2)
        assert_equals(len(mobile.requests), 2)

        post_params = dict(parse_qsl(urllib.unquote(mobile.requests[1].body)))
        assert_in('x-collapse-id', post_params)
        assert_equals(post_params['x-collapse-id'], 'mail_transit_id')

    def test_custom_collapse_id_is_not_overwritten(self):
        payload = dumps({'data': 'something'}, separators=(',', ':'))

        self.subscribe_one_session_to_mail()
        message = self.make_text_message('mail', payload, 'mail_transit_id')
        message[MessageFields.repacking_rules] = {'apns': '{"collapse-id": "custom_collapse_id"}'}
        hub.fast_binary_notify(message)

        wait(lambda: len(mobile.requests) == 1, 2)
        assert_equals(len(mobile.requests), 1)
        post_params = dict(parse_qsl(urllib.unquote(mobile.requests[0].body)))

        assert_in('x-collapse-id', post_params)
        assert_equals(post_params['x-collapse-id'], '"custom_collapse_id"')

    def test_collapse_id_not_injected_for_mail_fcm_or_other_services(self):
        payload = dumps({'data': 'something'}, separators=(',', ':'))

        self.subscribe_one_session()
        self.subscribe_one_session_to_mail('fcm')
        hub.fast_binary_notify(self.make_text_message(Testing.service1(), payload))
        hub.fast_binary_notify(self.make_text_message('mail', payload, 'mail_transit_id'))

        wait(lambda: len(mobile.requests) == 2, 2)
        assert_equals(len(mobile.requests), 2)
        post_params = dict(parse_qsl(urllib.unquote(mobile.requests[0].body)))
        assert_not_in('x-collapse-id', post_params)
        post_params = dict(parse_qsl(urllib.unquote(mobile.requests[1].body)))
        assert_not_in('x-collapse-id', post_params)

    def test_transit_id_appmetrica_format(self):
        payload = dumps({'yamp': {}}, separators=(',', ':'))
        hub.subscribe_mobile(Testing.uid1(), Testing.service1(), 'xivamob:%s/%s' % ('ru.yandex.music', self.token), session_key='a', platform='fcm')
        hub.fast_binary_notify(self.make_text_message(Testing.service1(), payload))

        wait(lambda: len(mobile.requests) == 1, 2)
        assert_in('xt=transitid1', urllib.unquote(mobile.requests[0].body))
        assert_in('xe=test', urllib.unquote(mobile.requests[0].body))

    def test_long_transit_ids_truncated_before_injecting_in_collapse_id(self):
        MAX_COLLAPSE_ID_SIZE = 64
        payload = dumps({'data': 'something'}, separators=(',', ':'))
        aps = dumps({'title': 'test'}, separators=(',', ':'));

        self.subscribe_one_session_to_mail()
        message = self.make_text_message('mail', payload, transit_id='x' * (MAX_COLLAPSE_ID_SIZE + 1))
        message[MessageFields.repacking_rules] = {'apns': '{"aps": %s}' % (aps)}
        hub.fast_binary_notify(message)

        wait(lambda: len(mobile.requests) == 1, 2)
        assert_equals(len(mobile.requests), 1)
        post_params = dict(parse_qsl(urllib.unquote(mobile.requests[0].body)))

        assert_in('x-collapse-id', post_params)
        assert_equals(post_params['x-collapse-id'], 'x' * MAX_COLLAPSE_ID_SIZE)

    def token_updated(self, new_token):
        subs = hub.list(Testing.uid1(), Testing.service1())
        return subs[0]['url'] == 'xivamob:%s/%s' % (self.app, new_token)

    def test_token_update(self):
        "check that supscription is updated with new push token"

        new_token = 'newtoken1234'
        payload = dumps({'data': 'something'})

        mobile.impl.set_response(raw_response=dumps({'new_token': new_token}))
        self.subscribe_one_session()
        message = self.make_text_message()
        message[MessageFields.raw_data] = payload
        hub.fast_binary_notify(message)

        wait(lambda: len(mobile.requests) == 1, 2)
        assert_equals(len(mobile.requests), 1)

        wait(lambda: self.token_updated(new_token), 0.5)
        assert_true(self.token_updated(new_token))

    def test_token_update_blacklisted(self):
        "check that fcm supscription is not updated if new push token is blacklisted"

        payload = dumps({'data': 'something'})
        mobile.impl.set_response(raw_response=dumps({'new_token': 'BLACKLISTED'}))
        self.subscribe_two_sessions()
        message = self.make_text_message()
        hub.fast_binary_notify(message)
        wait(lambda: len(mobile.requests) == 2, 2)
        assert_equals(len(mobile.requests), 2)
        subs = hub.list(Testing.uid1(), Testing.service1())
        assert_equals(len(subs), 2)
        fcm_sub = [s for s in subs if s['platform'] == 'gcm'][0]
        apns_sub = [s for s in subs if s['platform'] == 'apns'][0]
        assert_equals(fcm_sub['url'], 'xivamob:%s/%s' % (self.app, self.token2))
        assert_equals(apns_sub['url'], 'xivamob:%s/%s' % (self.app, 'BLACKLISTED'))

    def test_unsupported_operation(self):
        "[MAIL] check that unsupported operation is ignored"

        payload = dumps({'operation': 'delete mails'})

        self.subscribe_two_sessions_to_mail()
        message = self.make_text_message()
        message[MessageFields.service] = 'mail'
        message[MessageFields.operation] = 'delete mails'
        message[MessageFields.raw_data] = payload
        hub.fast_binary_notify(message)
        time.sleep(1)

        wait(lambda: len(mobile.requests) == 1, 1)
        assert_equals(len(mobile.requests), 0)

    def test_ignore_binaries(self):
        "binary messages are ignored by mobile gate"

        self.subscribe_one_session()
        hub.fast_binary_notify(self.make_binary_message())

        wait(lambda: len(mobile.bodies) == 1, 2)
        assert_equals(len(mobile.bodies), 0)
