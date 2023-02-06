from pycommon import *
from subscriber import *
import time

def setUp(self):
    global hub
    global subscriber1
    global subscriber2
    global subscriber500
    hub = Client(Testing.host(), Testing.port())
    hub.unsubscribe_all(Testing.uid1(), Testing.service1())
    hub.unsubscribe_all(Testing.uid2(), Testing.service1())
    hub.unsubscribe_all(Testing.uid3(), Testing.service1())

    subscriber1 = Subscriber(Testing.subscriber_port())
    subscriber1.start()
    subscriber2 = Subscriber(Testing.subscriber_port()+1)
    subscriber2.start()
    subscriber500 = Subscriber(Testing.subscriber_port()+2)
    subscriber500.start()

def tearDown(self):
    subscriber1.stop()
    subscriber2.stop()
    subscriber500.stop()

class TestBatchNotify:
    def setup(self):
        self.message = hub.make_message("", Testing.service1(), {}, 'test', 'transit-id', 0, 0)
        subscriber1.id = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber1.url)
        subscriber2.id = hub.subscribe(Testing.uid2(), Testing.service1(), subscriber2.url)

    def teardown(self):
        subscriber1.messages = []
        subscriber2.messages = []
        subscriber500.messages = []
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())
        hub.unsubscribe_all(Testing.uid2(), Testing.service1())
        subscriber1.set_response(200, raw_response='OK')
        subscriber2.set_response(200, raw_response='OK')
        subscriber500.set_response(500, raw_response='internal error')

    def test_400_without_body(self):
        response = hub.raw().post('/batch_binary_notify', {}, "")
        assert_equals(response.status, 400)

    def test_400_with_bad_message(self):
        response = hub.raw().post('/batch_binary_notify', {}, "<TRASH-DATA>")
        assert_equals(response.status, 400)

    def test_400_without_subscription(self):
        response = hub.raw().post('/batch_binary_notify', {}, msgpack.packb(self.message))
        assert_equals(response.status, 400)

    def test_400_with_bad_subscription(self):
        body = msgpack.packb(self.message)
        body = body + msgpack.packb("<TRASH_DATA>")
        response = hub.raw().post('/batch_binary_notify', {}, body)
        assert_equals(response.status, 400)

    def test_400_if_empty_uid_in_keys(self):
        body = msgpack.packb(self.message)
        body = body + msgpack.packb([('a','a'), ('','')])
        response = hub.raw().post('/batch_binary_notify', {}, body)
        assert_equals(response.status, 400)

    def test_200_for_single_key(self):
        status = hub.batch_binary_notify(self.message, [(Testing.uid1(), '')])
        assert_in([0, 200, 'OK', subscriber1.id], status)


    def test_204_for_single_key(self):
        status = hub.batch_binary_notify(self.message, [(Testing.uid3(), '')])
        assert_in([0, 204, 'no subscriptions', ''], status)

    def test_200_for_two_keys(self):
        status = hub.batch_binary_notify(self.message, [(Testing.uid1(), ''), (Testing.uid2(), '')])
        assert_in([0, 200, 'OK', subscriber1.id], status)
        assert_in([1, 200, 'OK', subscriber2.id], status)

    def test_200_for_two_keys_with_one_id(self):
        print hub.list(Testing.uid1(), Testing.service1())
        print hub.list(Testing.uid2(), Testing.service1())
        status = hub.batch_binary_notify(self.message, [(Testing.uid1(), subscriber1.id), (Testing.uid2(), '')])
        assert_in([0, 200, 'OK', subscriber1.id], status)
        assert_in([1, 200, 'OK', subscriber2.id], status)

    def test_200_for_two_keys_with_two_ids(self):
        status = hub.batch_binary_notify(self.message, [(Testing.uid1(), subscriber1.id), (Testing.uid2(), subscriber2.id)])
        assert_in([0, 200, 'OK', subscriber1.id], status)
        assert_in([1, 200, 'OK', subscriber2.id], status)

    def test_205_for_not_subscribed_id(self):
        status = hub.batch_binary_notify(self.message, [(Testing.uid1(), subscriber1.id), (Testing.uid2(), subscriber1.id)])
        assert_in([0, 200, 'OK', subscriber1.id], status)
        assert_in([1, 204, 'no subscriptions', subscriber1.id], status)

    def test_204_if_no_subscriptions_for_user(self):
        status = hub.batch_binary_notify(self.message, [(Testing.uid1(), ''), (Testing.uid3(), '')])
        assert_in([0, 200, 'OK', subscriber1.id], status)
        assert_in([1, 204, 'no subscriptions', ""], status)

        status = hub.batch_binary_notify(self.message, [(Testing.uid3(), ''), (Testing.uid1(), '')])
        assert_in([0, 204, 'no subscriptions', ""], status)
        assert_in([1, 200, 'OK', subscriber1.id], status)

    def test_200_many_subscriptions_per_uid(self):
        extra_subscriber_id = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber2.url)

        status = hub.batch_binary_notify(self.message, [(Testing.uid1(), '')])
        assert_in([0, 200, 'OK', subscriber1.id], status)
        assert_in([0, 200, 'OK', extra_subscriber_id], status)

    def test_200_many_subscriptions_per_uid_with_another_user_204(self):
        extra_subscriber_id = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber2.url)

        status = hub.batch_binary_notify(self.message, [(Testing.uid1(), ''), (Testing.uid3(), '')])
        assert_in([0, 200, 'OK', subscriber1.id], status)
        assert_in([0, 200, 'OK', extra_subscriber_id], status)
        assert_in([1, 204, 'no subscriptions', ""], status)

    def test_multiple_id_batch(self):
        subscriber1.id2 = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber1.url, session_key='abc')
        subscriber2.id2 = hub.subscribe(Testing.uid2(), Testing.service1(), subscriber2.url, session_key='abc')
        fake_sub = hub.subscribe(Testing.uid1(), Testing.service1(), 'fake_callback', session_key='def')
        sub_500 = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber500.url, session_key='abc')

        status = hub.batch_binary_notify(self.message, [(Testing.uid1(), subscriber1.id), (Testing.uid1(), subscriber1.id2),
            (Testing.uid1(), fake_sub), (Testing.uid1(), 'missing_sub'), (Testing.uid2(), subscriber2.id), (Testing.uid2(), subscriber2.id2),
            (Testing.uid1(), sub_500)])

        assert_in([0, 200, 'OK', subscriber1.id], status)
        assert_in([1, 200, 'OK', subscriber1.id2], status)
        assert_in([2, 502, 'connect error', fake_sub], status)
        assert_in([3, 204, 'no subscriptions', 'missing_sub'], status)
        assert_in([4, 200, 'OK', subscriber2.id], status)
        assert_in([5, 200, 'OK', subscriber2.id2], status)
        assert_in([6, 500, 'internal error', sub_500], status)

    def test_pass_codes(self):
        data = {200: 'OK', 400: 'bad request', 403: 'forbidden',
            429: 'rate limit', 502: 'bad gateway', 503: 'unavailable', 504: 'timeout'}
        for c in data:
            yield self.check_pass_code, c, data[c]

    def check_pass_code(self, code, body):
        subscriber1.set_response(code, raw_response=body)
        status = hub.batch_binary_notify(self.message, [(Testing.uid1(), '')])
        assert_in([0, code, body, subscriber1.id], status)
