from pycommon import *
from subscriber import *
import time
import random
import string

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

class TestNotifySingleSubscriber:
    def setup(self):
        pass
    def teardown(self):
        subscriber.messages = []
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())
        hub.unsubscribe_all(Testing.uid1(), 'mail')

    def test_http(self):
        hub.subscribe(Testing.uid1(), Testing.service1(), subscriber.url,
          filter='{"rules": [{ "if": { "$has_tags": ["a"] }, "do": "send_silent" },{ "if": { "$has_tags": ["b"] }, "do": "send_bright" },{ "do": "skip" }], "vars": {}}')
        hub.fast_notify(Testing.uid1(), Testing.service1(), "test", tags='a')
        hub.fast_notify(Testing.uid1(), Testing.service1(), "test", tags='b')
        hub.fast_notify(Testing.uid1(), Testing.service1(), "test", tags='c')
        time.sleep(0.15)
        assert_equals(len(subscriber.messages),2)

    def test_mobile_mail(self):
        hub.subscribe_mobile(Testing.uid1(), 'mail', 'xivamob:ru.yandex.mail/pushtokenABCDE', session_key='a', platform='apns',
          filter='{"rules": [{ "if": { "$has_tags": ["a"] }, "do": "send_silent" },{ "if": { "$has_tags": ["b"] }, "do": "send_bright" },{ "do": "skip" }], "vars": {}}')

        transit_id1 = generate_transit_id()
        transit_id2 = generate_transit_id()
        transit_id3 = generate_transit_id()

        # Check both report=0 (default) and report=1
        hub.fast_binary_notify(make_message(Testing.uid1(), 'mail',
            {"fid_type" : "1"}, '{"operation":"insert"}',
            transit_id1, 0,0, 'insert', tags=['a']), report=1)
        hub.fast_binary_notify(make_message(Testing.uid1(), 'mail',
            {"fid_type" : "1"}, '{"operation":"insert"}',
            transit_id2, 0,0, 'insert', tags=['b']))
        hub.fast_binary_notify(make_message(Testing.uid1(), 'mail',
            {"fid_type" : "1"}, '{"operation":"insert"}',
            transit_id3, 0,0, 'insert', tags=['c']))

        wait(lambda: len(mobile.bodies) == 3, 5)
        assert_equals(len(mobile.bodies),3)

        req0 = urllib.unquote(mobile.bodies[0]).decode('utf8')
        assert_in(transit_id1, req0)
        assert_not_in('loc-key', req0)
        assert_in('content-available', req0)
        req1 = urllib.unquote(mobile.bodies[1]).decode('utf8')
        assert_in(transit_id2, req1)
        assert_in('loc-key', req1)
        assert_not_in('content-available', req1)
        req2 = urllib.unquote(mobile.bodies[2]).decode('utf8')
        assert_in(transit_id2, req2)
        assert_in('content-available', req2)
        assert_not_in('loc-key', req2)

