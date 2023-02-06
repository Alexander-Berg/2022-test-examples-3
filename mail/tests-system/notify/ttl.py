from pycommon import *
from subscriber import *
import time

def setUp(self):
    global hub
    global subscriber1
    global mobile
    hub = Client(Testing.host(), Testing.port())
    hub.unsubscribe_all(Testing.uid1(), Testing.service1())

    subscriber1 = Subscriber(Testing.subscriber_port())
    subscriber1.start()

    mobile = FakeXivaMobile(Testing.mobile_port())
    mobile.start()

def tearDown(self):
    subscriber1.stop()
    mobile.stop()

class TestTtlGCM: # gcm_compatibility
    def setup(self):
        self.message = hub.make_message(Testing.uid1(), Testing.service1(), {}, 'test', 'transit-id-abc', 0, int(time.time()))

    def teardown(self):
        mobile.id = ''
        subscriber1.id = ''
        subscriber1.messages = []
        mobile.messages = []
        mobile.requests = []
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())

    def subscribe(self):
        subscriber1.id = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber1.url)
        mobile.id = hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'gcm', 'session_key': 'abc'}) # gcm_compatibility

    def call_fast_binary_notify(self):
        response = hub.raw().post('/fast_binary_notify?report=1', {}, msgpack.packb(self.message))
        time.sleep(0.05)
        return response

    def call_binary_notify(self):
        response = hub.raw().post('/binary_notify', {}, msgpack.packb(self.message))
        time.sleep(0.5)
        return response

    def test_ttl_default(self):
        self.subscribe()
        response = self.call_fast_binary_notify()
        assert_ok(response)

        assert_equals(len(subscriber1.messages), 1)
        assert_equals(len(mobile.requests), 1)
        # one second can pass
        assert_in(subscriber1.messages[0][14], range(86396, 86401))
        assert_in('ttl=86', mobile.requests[0].path)

    def test_ttl(self):
        self.message.append(123)
        self.subscribe()
        response = self.call_fast_binary_notify()
        assert_ok(response)

        assert_equals(len(subscriber1.messages), 1)
        assert_equals(len(mobile.requests), 1)
        assert_in(subscriber1.messages[0][14], range(120, 124))
        assert_in('ttl=12', mobile.requests[0].path)

class TestTtl: # gcm_compatibility
    def setup(self):
        self.message = hub.make_message(Testing.uid1(), Testing.service1(), {}, 'test', 'transit-id-abc', 0, int(time.time()))

    def teardown(self):
        mobile.id = ''
        subscriber1.id = ''
        subscriber1.messages = []
        mobile.messages = []
        mobile.requests = []
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())

    def subscribe(self):
        subscriber1.id = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber1.url)
        mobile.id = hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc'})

    def call_fast_binary_notify(self):
        response = hub.raw().post('/fast_binary_notify?report=1', {}, msgpack.packb(self.message))
        time.sleep(0.05)
        return response

    def call_binary_notify(self):
        response = hub.raw().post('/binary_notify', {}, msgpack.packb(self.message))
        time.sleep(0.5)
        return response

    def test_ttl_default(self):
        self.subscribe()
        response = self.call_fast_binary_notify()
        assert_ok(response)

        assert_equals(len(subscriber1.messages), 1)
        assert_equals(len(mobile.requests), 1)
        # one second can pass
        assert_in(subscriber1.messages[0][14], range(86396, 86401))
        assert_in('ttl=86', mobile.requests[0].path)

    def test_ttl(self):
        self.message.append(123)
        self.subscribe()
        response = self.call_fast_binary_notify()
        assert_ok(response)

        assert_equals(len(subscriber1.messages), 1)
        assert_equals(len(mobile.requests), 1)
        assert_in(subscriber1.messages[0][14], range(120, 124))
        assert_in('ttl=12', mobile.requests[0].path)

# ENABLE WORKERS BEFORE RUNNING EXPIRE TEST
'''    def test_ttl_expired(self):
        self.message.append(1)
        self.message[11] = int(time.time()) - 100
        self.subscribe()
        response = self.call_binary_notify()
        assert_ok(response)

        assert_equals(len(subscriber1.messages), 0)
        assert_equals(len(mobile.requests), 0)
'''

