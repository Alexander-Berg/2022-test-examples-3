from pycommon import *
from subscriber import *

def setUp(self):
    global hub
    global subscriber
    hub = Client(Testing.host(), Testing.port())
    hub.unsubscribe_all(Testing.uidset1_uid1(), Testing.service1())

    subscriber = Subscriber(Testing.subscriber_port())
    subscriber.start()

def tearDown(self):
    subscriber.stop()

class TestSubscribe:
    def setup(self):
        self.simple_callback = 'http://' + Testing.host()+ ':' + str(Testing.port()) + '/ping'
    def teardown(self):
        subscriber.messages = []
        hub.unsubscribe_all(Testing.uidset1_uid1(), Testing.service1())
        hub.unsubscribe_all(Testing.uidset1_uid2(), Testing.service1())

    def test_subscribes_uidset(self):
        response = hub.subscribe_uidset(Testing.uidset1(), Testing.uidset1_uid1(),
            Testing.service1(), self.simple_callback, id='sub1')
        list = hub.list_uidset(Testing.uidset1(), Testing.service1())
        list_ids = map(lambda x: x['id'], list)
        assert_list_equal(list_ids, ['sub1'])

    def test_generates_id_if_not_given(self):
        subid = hub.subscribe_uidset(Testing.uidset1(), Testing.uidset1_uid1(),
            Testing.service1(), self.simple_callback)
        assert_not_equal(subid, 'OK')
        assert_regexp_matches(subid, "[a-zA-Z0-9]+")

    def test_rejects_uid_from_wrong_shard(self):
        params = ({ 'uidset': Testing.uidset1(), 'uid' : '1',
            'service' : Testing.service1(), 'callback' : self.simple_callback,
            'id' : 'sub1' })
        response = hub.raw().get("/uidset/subscribe", params)
        assert_equals(response.status, 400)

    def test_unsubscribes_uidset(self):
        hub.subscribe_uidset(Testing.uidset1(), Testing.uidset1_uid1(),
            Testing.service1(), self.simple_callback, id='sub1')
        list = hub.list_uidset(Testing.uidset1(), Testing.service1())
        assert_greater(len(list), 0)

        hub.unsubscribe_uidset(Testing.uidset1(), Testing.uidset1_uid1(),
            Testing.service1(), 'sub1')
        list = hub.list_uidset(Testing.uidset1(), Testing.service1())
        assert_equals(len(list), 0)

    def test_unsubscribe_uidset_no_subscription(self):
        params = ({ 'uidset': Testing.uidset1(), 'uid' : Testing.uidset1_uid1(),
            'service' : Testing.service1(), 'subscription-id' : 'no_such_sub' })
        response = hub.raw().get("/uidset/unsubscribe", params)
        assert_equals(response.status, 200)
