from pycommon import *
from subscriber import *
import time

def setUp(self):
    global hub
    global subscriber
    hub = Client(Testing.host(), Testing.port())
    hub.unsubscribe_all(Testing.uid1(), "mail")
    hub.unsubscribe_all(Testing.uid2(), "mail")
    hub.unsubscribe_all(Testing.uid1(), Testing.service1())
    hub.unsubscribe_all(Testing.uid2(), Testing.service1())
    hub.unsubscribe_all(Testing.uid1(), Testing.service2())
    hub.unsubscribe_all(Testing.uid2(), Testing.service2())

    subscriber = Subscriber(Testing.subscriber_port())
    subscriber.start()

def tearDown(self):
    subscriber.stop()

class TestNotifySingleSubscriber:
    def setup(self):
        pass
    def teardown(self):
        subscriber.messages = []
        subscriber.requests = []
        subscriber.set_response(code=200, raw_response='OK')
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())
        hub.unsubscribe_all(Testing.uid1(), Testing.service2())
        hub.unsubscribe_all(Testing.uid2(), Testing.service1())
        hub.unsubscribe_all(Testing.uid2(), Testing.service2())

    def test_notify_sends_to_single_subscription(self):
        subid = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber.url)
        response = hub.raw().post("/notify/" + Testing.service1(),
           {'uid' : Testing.uid1()}, 'test_data')
        assert_equals(response.status, 200)
        wait(lambda: len(subscriber.messages) == 1, 5)
        assert_equals(len(subscriber.messages), 1)

    def test_notify_without_deduplication_sends_two_messages(self):
        subid = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber.url)
        response = hub.raw().post("/notify/" + Testing.service1(),
           {'uid' : Testing.uid1()}, 'test_data')
        assert_equals(response.status, 200)
        response = hub.raw().post("/notify/" + Testing.service1(),
           {'uid' : Testing.uid1()}, 'test_data')
        assert_equals(response.status, 200)
        wait(lambda: len(subscriber.messages) == 2, 5)
        assert_equals(len(subscriber.messages), 2)

    def test_notify_with_deduplication_sends_one_message(self):
        duplicate_data = 'test_data_' + str(int(time.time()))
        subid = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber.url)
        response = hub.raw().post("/notify/" + Testing.service1(),
           {'uid' : Testing.uid1(), 'enable_deduplication' : 1}, duplicate_data)
        assert_equals(response.status, 200)
        response = hub.raw().post("/notify/" + Testing.service1(),
           {'uid' : Testing.uid1(), 'enable_deduplication' : 1}, duplicate_data)
        assert_equals(response.status, 200)
        wait(lambda: len(subscriber.messages) == 1, 5)
        assert_equals(len(subscriber.messages), 1)

    def test_notify_with_subsription_id_placeholder(self):
        subid1 = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber.url + '?subscription_id1=${subscription_id}')
        subid2 = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber.url + '?subscription_id2=${subscription-id}')
        message = make_message(Testing.uid1(), Testing.service1(), {}, 'test', 'transit_id', 0, 0, 'subscribe')

        hub.fast_binary_notify(message)
        time.sleep(0.15)
        assert_equals(len(subscriber.requests),2)
        path1, path2 = subscriber.requests[0].path, subscriber.requests[1].path
        if 'subscription_id2' in path1:
            path1, path2 = path2, path1
        assert_in(subid1, path1)
        assert_in(subid2, path2)

    def test_send_method_without_slash_gives_404(self):
        response = hub.raw().post('/send', {}, "")
        assert_equals(response.status, 404)

    def test_send_method_accepts_any_service(self):
        response = hub.raw().post('/send/afasfaergeg', {}, "")
        assert_equals(response.status, 400)

    def test_send_method_sends_message_to_custom_subscription(self):
        message = hub.make_message(Testing.uid1(), Testing.service1(), {}, 'test', 'transit-id', 0, 0)
        body = msgpack.packb(message)
        body = body + msgpack.packb(hub.make_subscription(Testing.uid1(), Testing.service1(), 'test-sub-id', subscriber.url))
        response = hub.raw().post('/send/asdf', {}, body)
        assert_equals(response.status, 200)
        wait(lambda: len(subscriber.messages) == 1, 5)
        assert_equals(len(subscriber.messages), 1)
        assert_messages_equal(subscriber.messages[0], message)

    def test_send_method_replies_gone_when_subscription_is_gone(self):
        subscriber.set_response(code=205, raw_response='gone')
        message = hub.make_message(Testing.uid1(), Testing.service1(), {}, 'test', 'transit-id', 0, 0)
        body = msgpack.packb(message)
        body = body + msgpack.packb(hub.make_subscription(Testing.uid1(), Testing.service1(), 'test-sub-id', subscriber.url))
        response = hub.raw().post('/send/asdf', {}, body)
        assert_equals(response.status, 205)
        wait(lambda: len(subscriber.messages) == 1, 5)
        assert_equals(len(subscriber.messages), 1)
        assert_messages_equal(subscriber.messages[0], message)

    def test_websocket_served_by_separate_gate(self):
        hub.subscribe(Testing.uid1(), Testing.service1(), subscriber.url)
        hub.subscribe(Testing.uid2(), Testing.service1(), 'xivaws:' + subscriber.url)
        response = hub.raw().post("/notify/" + Testing.service1(),
           {'uid' : Testing.uid1()}, 'test_data')
        assert_equals(response.status, 200)
        wait(lambda: len(subscriber.messages) == 1, 5)
        assert_equals(len(subscriber.messages), 1)
        assert_equals(subscriber.requests[-1].headers['User-Agent'], 'xiva')
        response = hub.raw().post("/notify/" + Testing.service1(),
           {'uid' : Testing.uid2()}, 'test_data')
        assert_equals(response.status, 200)
        wait(lambda: len(subscriber.messages) == 2, 5)
        assert_equals(len(subscriber.messages), 2)
        assert_equals(subscriber.requests[-1].headers['User-Agent'], 'xivaws')
