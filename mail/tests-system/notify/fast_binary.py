from pycommon import *
from subscriber import *
from json import dumps
import time

def setUp(self):
    global hub
    global subscriber1
    global subscriber2
    hub = Client(Testing.host(), Testing.port())
    hub.unsubscribe_all(Testing.uid1(), Testing.service1())

    subscriber1 = Subscriber(Testing.subscriber_port())
    subscriber1.start()

    subscriber2 = Subscriber(Testing.subscriber_port()+1)
    subscriber2.start()

def tearDown(self):
    subscriber1.stop()
    subscriber2.stop()

class TestFastBinaryNotify:
    def setup(self):
        self.message = hub.make_message(Testing.uid1(), Testing.service1(), {}, 'test', 'transit-id-abc', 0, 0)

    def teardown(self):
        subscriber1.id = ''
        subscriber2.id = ''
        subscriber1.messages = []
        subscriber2.messages = []
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())

    def subscribe_one(self):
        subscriber1.id = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber1.url)

    def subscribe_two(self):
        subscriber1.id = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber1.url)
        subscriber2.id = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber2.url)

    def call_fast_binary_notify(self, report = False):
        response = hub.raw().post('/fast_binary_notify?report=1' if report else '/fast_binary_notify', {}, msgpack.packb(self.message))
        time.sleep(0.05)
        return response

    def test_400_without_body(self):
        response = hub.raw().post('/fast_binary_notify', {}, "")
        assert_bad_request(response, 'failed to unpack body')

    def test_400_with_trash(self):
        response = hub.raw().post('/fast_binary_notify', {}, "ASFWRTHGAWEFGSRYHTGFS~EFGR")
        assert_bad_request(response, 'failed to unpack body')

    def test_200_for_no_subscriptions(self):
        response = self.call_fast_binary_notify()
        assert_ok(response)
        eq_(len(subscriber1.messages), 0)
        eq_(len(subscriber2.messages), 0)

    def test_200_for_single(self):
        self.subscribe_one()
        response = self.call_fast_binary_notify()
        assert_ok(response)
        eq_(len(subscriber1.messages), 1)
        eq_(len(subscriber2.messages), 0)

    def test_200_for_two(self):
        self.subscribe_two()
        response = self.call_fast_binary_notify()
        assert_ok(response)
        eq_(len(subscriber1.messages), 1)
        eq_(len(subscriber2.messages), 1)

    def test_200_for_no_subscriptions_with_status(self):
        response = self.call_fast_binary_notify(report = True)
        assert_ok(response)
        report = msgpack.unpackb(response.body)
        eq_(len(report), 1)
        eq_(report[0][1], 204)
        eq_(len(subscriber1.messages), 0)
        eq_(len(subscriber2.messages), 0)

    def test_200_for_single_with_status(self):
        self.subscribe_one()
        response = self.call_fast_binary_notify(report = True)
        assert_ok(response)
        report = msgpack.unpackb(response.body)
        eq_(len(report), 1)
        eq_(report[0][1], 200)
        eq_(len(subscriber1.messages), 1)
        eq_(len(subscriber2.messages), 0)

    def test_200_for_two_with_status(self):
        self.subscribe_two()
        response = self.call_fast_binary_notify(report = True)
        assert_ok(response)
        report = msgpack.unpackb(response.body)
        eq_(len(report), 2)
        eq_(report[0][1], 200)
        eq_(report[1][1], 200)
        eq_(len(subscriber1.messages), 1)
        eq_(len(subscriber2.messages), 1)


    def test_205_for_one_of_subscriptions(self):
        self.subscribe_two()
        subscriber2.set_response(code = 205)
        response = self.call_fast_binary_notify(report = True)
        assert_ok(response)
        report = msgpack.unpackb(response.body)
        eq_(len(report), 2)
        codes = [el[1] for el in report]
        assert_in(200, codes)
        assert_in(205, codes)
        eq_(len(subscriber1.messages), 1)
        eq_(len(subscriber2.messages), 1)

    def test_422_on_repack_error(self):
        hub.subscribe_mobile(Testing.uid1(), Testing.service1(), 'xivamob:app.test/1', **{'platform':'apns', 'session_key': 'abc'})
        hub.subscribe_mobile(Testing.uid1(), Testing.service1(), 'xivamob:app.test/2', **{'platform':'fcm', 'session_key': 'def'})
        bad_repack = dumps({ 'repack_payload': ["some_field"] })
        self.message[7] = "Not a json"
        self.message[13] = { 'apns': bad_repack, 'fcm': bad_repack }
        response = self.call_fast_binary_notify(report = True)
        assert_ok(response)
        report = msgpack.unpackb(response.body)
        eq_(len(report), 2)
        assert_in([0, 422, 'raw_data is not JSON', 'mob:abc'], report)
        assert_in([0, 422, 'raw_data is not JSON', 'mob:def'], report)
