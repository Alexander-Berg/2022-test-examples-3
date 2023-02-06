from pycommon import *
from subscriber import *
from urlparse import parse_qs

def setUp(self):
    global hub
    global mobile
    hub = Client(Testing.host(), Testing.port())
    hub.unsubscribe_all(Testing.uid1(), Testing.service1())

    mobile = FakeXivaMobile(Testing.mobile_port())
    mobile.start()

def tearDown(self):
    global mobile
    mobile.stop()

class TestMobileWebpush:
    def setup(self):
        pass

    def teardown(self):
        mobile.bodies = []
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())

    def subscribe(self):
        return hub.subscribe(Testing.uid1(), Testing.service1(),
            'webpush:subscription%25encoded', session_key='ABC-DEF',
            client="mobile", id="webpush:ABC-DEF")

    def unsubscribe(self):
        return hub.unsubscribe(Testing.uid1(), Testing.service1(), 'webpush:ABC-DEF')

    def make_json_message(self):
        return make_message(uid=Testing.uid1(), service=Testing.service1(), raw_data='{"key":"value"}')

    def make_binary_message(self):
        ret = make_message(uid=Testing.uid1(), service=Testing.service1(), raw_data=TestData.binary_payload)
        ret[MessageFields.type] = MessageContentType.binary
        return ret

    def test_subscribe_ok_with_correct_id(self):
        self.subscribe()
        list = hub.list(Testing.uid1(), Testing.service1())
        list_ids = map(lambda x: x['id'], list)
        assert_in('webpush:ABC-DEF', list_ids)

    def test_unsubscribe_by_id_ok(self):
        self.subscribe()
        self.unsubscribe()
        list = hub.list(Testing.uid1(), Testing.service1())
        assert_equals(list, [])

    def test_notify_ok(self):
        "notify to webpush subscription calls xivamob with correct args"
        self.subscribe()
        hub.fast_binary_notify(self.make_json_message())
        wait(lambda: len(mobile.bodies) == 1, 2)
        assert_equals(len(mobile.bodies), 1)
        body = mobile.bodies[0]
        subscription_in_body = parse_qs(body)['subscription'][0]
        assert_equals(subscription_in_body, "subscription%encoded")

    def test_notify_bad_uri(self):
        "no messages will be sent to webpush subscription with bad uri"
        hub.subscribe(Testing.uid1(), Testing.service1(),
            'webpush:', session_key='ABC-DEF',
            client="mobile", id="webpush:ABC-DEF")
        hub.fast_binary_notify(self.make_json_message())
        wait(lambda: len(mobile.bodies) == 1, 2)
        assert_equals(len(mobile.bodies), 0)

    def test_header_for_binaries(self):
        "binary messages are sent to webpush subscribers with a special header"
        self.subscribe()
        message = self.make_binary_message()
        hub.fast_binary_notify(message)
        wait(lambda: len(mobile.bodies) == 1, 5)
        assert_equals(len(mobile.bodies), 1)
        body = mobile.bodies[0]
        (content_type, hdr, payload) = unpack_binary_frame(parse_qs(body)['payload'][0])
        print content_type, hdr, payload
        assert_equals(content_type, 3)
        assert_equals(hdr[0], Testing.uid1())
        assert_equals(hdr[1], Testing.service1())
        assert_equals(payload, TestData.binary_payload)
