from pycommon import *
from subscriber import *
import time
import random
import string

MATCH_PLATFORM = 0
MATCH_TRANSPORT = 1
MATCH_SUBSCRIPTION_ID = 2
MATCH_SESSION = 3
MATCH_UUID = 4
MATCH_DEVICE = 5
MATCH_APP = 6

RULE_NAMES = ['platform', 'transport', 'id', 'session', 'uuid', 'device', 'app']

def generate_transit_id():
    return ''.join(random.choice(string.ascii_uppercase + string.digits) for _ in range(10))

def setUp(self):
    global hub, subscriber, mobile
    hub = Client(Testing.host(), Testing.port())
    hub.unsubscribe_all(Testing.uid1(), Testing.service1())

    subscriber = Subscriber(Testing.subscriber_port())
    subscriber.start()

    mobile = FakeXivaMobile(Testing.mobile_port())
    mobile.start()

def tearDown(self):
    global subscriber, mobile
    subscriber.stop()
    mobile.stop()

class TestMatchSubscriptions:
    def setup(self):
        self.subs = {}
        hub.subscribe(Testing.uid1(), Testing.service1(), subscriber.url)
        hub.subscribe(Testing.uid1(), Testing.service1(), 'webpush:callback', session_key='def', id='webpush:def')
        hub.subscribe_mobile(Testing.uid1(), Testing.service1(), 'xivamob:ru.yandex.mail/pushtokenABCDE', uuid='a-b-c', platform='apns', device='xyz')
        list = hub.list(Testing.uid1(), Testing.service1())
        for sub in list:
            if sub['id'].startswith('mob:'):
                self.subs['mobile'] = sub
            elif sub['id'].startswith('webpush:'):
                self.subs['webpush'] = sub
            else:
                self.subs['http'] = sub
        assert_equals(len(self.subs), 3)

    def teardown(self):
        subscriber.messages = []
        mobile.requests = []
        mobile.bodies = []
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())

    def message_with_matcher(self, rule_type, values):
        assert(rule_type >= 0 and rule_type < len(RULE_NAMES))
        msg = make_message(Testing.uid1(), Testing.service1(), {}, 'test', generate_transit_id(), 0, 0, 'test')
        if not isinstance(values, list):
            values = [values]
        # Values are expected to be sorted by xiva-server.
        msg[18] = [[rule_type, '', sorted(values)]]
        print 'subscription matcher: ', RULE_NAMES[rule_type], ' is one of the ', values
        return msg

    def test_match_platform_gcm(self): # gcm_compatibility
        hub.fast_binary_notify(self.message_with_matcher(MATCH_PLATFORM, ['gcm', 'wns', 'qwerty'])) # gcm_compatibility
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 0)
        hub.fast_binary_notify(self.message_with_matcher(MATCH_PLATFORM, ['apns']))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 1)

    def test_match_platform(self):
        hub.fast_binary_notify(self.message_with_matcher(MATCH_PLATFORM, ['fcm', 'wns', 'qwerty']))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 0)
        hub.fast_binary_notify(self.message_with_matcher(MATCH_PLATFORM, ['apns']))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 1)

    def test_match_transport(self):
        hub.fast_binary_notify(self.message_with_matcher(MATCH_TRANSPORT, 'fake'))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 0)
        hub.fast_binary_notify(self.message_with_matcher(MATCH_TRANSPORT, ['mobile', 'webpush']))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 2)
        hub.fast_binary_notify(self.message_with_matcher(MATCH_TRANSPORT, 'http'))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 1)
        assert_equals(len(mobile.requests), 2)

    def test_match_id(self):
        hub.fast_binary_notify(self.message_with_matcher(MATCH_SUBSCRIPTION_ID, ['fa', 'ke']))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 0)
        hub.fast_binary_notify(self.message_with_matcher(MATCH_SUBSCRIPTION_ID,
            [self.subs['webpush']['id'], self.subs['http']['id']]))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 1)
        assert_equals(len(mobile.requests), 1)

    def test_match_session(self):
        # UUID 'a-b-c' is transformed into session key,
        # therefore no subscription should be matched.
        hub.fast_binary_notify(self.message_with_matcher(MATCH_SESSION, 'a-b-c'))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 0)
        hub.fast_binary_notify(self.message_with_matcher(MATCH_SESSION, 'def'))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 1)

    def test_match_uuid(self):
        hub.fast_binary_notify(self.message_with_matcher(MATCH_UUID, 'f-a-k-e'))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 0)
        hub.fast_binary_notify(self.message_with_matcher(MATCH_UUID, 'a-b-c'))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 1)

    def test_match_device(self):
        hub.fast_binary_notify(self.message_with_matcher(MATCH_DEVICE, 'f-a-k-e'))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 0)
        hub.fast_binary_notify(self.message_with_matcher(MATCH_DEVICE, 'xyz'))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 1)

    def test_match_app(self):
        hub.fast_binary_notify(self.message_with_matcher(MATCH_APP, ['abcdef', 'ru.te.mail']))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 0)
        hub.fast_binary_notify(self.message_with_matcher(MATCH_APP, ['xyz', 'ru.yandex.mail']))
        time.sleep(0.15)
        assert_equals(len(subscriber.messages), 0)
        assert_equals(len(mobile.requests), 1)
