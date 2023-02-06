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

class TestUpdateCallback:
    def setup(self):
        self.callback1 = 'http://' + Testing.host()+ ':' + str(Testing.port()) + '/ping?one'
        self.callback2 = 'http://' + Testing.host()+ ':' + str(Testing.port()) + '/ping?two'
        self.callback3 = 'http://' + Testing.host()+ ':' + str(Testing.port()) + '/ping?three'
    def teardown(self):
        subscriber.messages = []
        hub.unsubscribe_all(Testing.uidset1_uid1(), Testing.service1())
        hub.unsubscribe_all(Testing.uidset1_uid2(), Testing.service1())
        hub.unsubscribe_all(Testing.uidset2_uid1(), Testing.service1())

    def test_update_callback(self):
        ids1 = set()
        ids1.add(hub.subscribe_uidset(Testing.uidset1(), Testing.uidset1_uid1(), Testing.service1(), self.callback1))
        ids1.add(hub.subscribe_uidset(Testing.uidset1(), Testing.uidset1_uid2(), Testing.service1(), self.callback2))
        callbacks1 = {self.callback1, self.callback2}

        ids2 = set()
        ids2.add(hub.subscribe_uidset(Testing.uidset2(), Testing.uidset2_uid1(), Testing.service1(), self.callback1))
        callbacks2 = {self.callback1}

        params = ({'uidset': Testing.uidset1(), 'service' : Testing.service1(),
            'callback' : self.callback3})
        response = hub.raw().get("/uidset/update_callback", params)
        assert_equals(response.status, 200)
        sleep(0.2)

        list_uidset1 = hub.list_uidset(Testing.uidset1(), Testing.service1())
        list_uidset2 = hub.list_uidset(Testing.uidset2(), Testing.service1())

        ids1_after = {x['id'] for x in list_uidset1}
        ids2_after = {x['id'] for x in list_uidset2}
        callbacks1_after = {x['url'] for x in list_uidset1}
        callbacks2_after = {x['url'] for x in list_uidset2}

        assert_equals(ids1, ids1_after)
        assert_equals(ids2, ids2_after)
        assert_equals(callbacks1_after, {self.callback3, self.callback3})
        assert_equals(callbacks2_after, callbacks2)
