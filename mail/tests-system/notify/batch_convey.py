from pycommon import *
from subscriber import *
import json
from urlparse import parse_qs

def setUp(self):
    global hub
    global subscriber1
    global subscriber2
    global subscriber500
    global dead_subscriber_url
    global mobile
    hub = Client(Testing.host(), Testing.port())
    hub.unsubscribe_all(Testing.uid1(), Testing.service1())
    hub.unsubscribe_all(Testing.uid2(), Testing.service1())
    hub.unsubscribe_all(Testing.uid3(), Testing.service1())

    subscriber1 = Subscriber(Testing.subscriber_port())
    subscriber2 = Subscriber(Testing.subscriber_port()-1)
    subscriber500 = Subscriber(Testing.subscriber_port()-2)
    dead_subscriber_url = 'http://127.0.0.1:' + str(Testing.dead_subscriber_port()) + '/fake'
    mobile = FakeXivaMobile(Testing.mobile_port())
    subscriber1.start()
    subscriber2.start()
    subscriber500.start()
    mobile.start()

def tearDown(self):
    subscriber1.stop()
    subscriber2.stop()
    subscriber500.stop()
    mobile.stop()

class TestBatchConvey:
    def setup(self):
        self.message = hub.make_message("", Testing.service1(), {}, 'test', 'transit-id', 0, 0)

    def teardown(self):
        subscriber1.messages = []
        subscriber2.messages = []
        mobile.messages = []
        mobile.requests = []
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())
        hub.unsubscribe_all(Testing.uid2(), Testing.service1())
        hub.unsubscribe_all(Testing.uid3(), Testing.service1())

    def test_batch_convey_gcm_compatibility(self):
        subscriber1.impl.set_response(raw_response='Hello')
        subscriber2.impl.set_response_code(400)
        subscriber2.impl.set_response(raw_response='Fail')
        subscriber500.impl.set_response_code(500)
        subscriber500.impl.set_response(raw_response='internal error')
        mobile.impl.set_response_code(200)
        mobile.impl.set_response(raw_response=json.dumps({'results':[
            {'code':200, 'result':'gcm_ok'},
            {'code':205, 'result':''},
            {'code':200, 'result':{'new_token':'1234'}}]}))
        subs = []
        subs.append(hub.subscribe(Testing.uid1(), Testing.service1(), subscriber1.url))
        subs.append(hub.subscribe(Testing.uid1(), Testing.service1(), subscriber2.url))
        subs.append(hub.subscribe(Testing.uid1(), Testing.service1(), dead_subscriber_url))
        subs.append(hub.subscribe(Testing.uid1(), Testing.service1(), subscriber500.url))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'gcm', 'session_key': 'abc'})) # gcm_compatibility
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/1', **{'platform':'gcm', 'session_key': 'def'})) # gcm_compatibility
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/2', **{'platform':'gcm', 'session_key': 'ghi'})) # gcm_compatibility
        response = hub.batch_binary_notify(self.message, [(Testing.uid1(), '')])

        time.sleep(0.5)

        mob_ids = ['mob:abc', 'mob:def', 'mob:ghi']
        mob_order = [int(x) for x in json.loads(parse_qs(mobile.requests[0].body)['tokens'][0])]

        assert_in([0, 200, 'Hello', subs[0]], response)
        assert_in([0, 400, 'Fail', subs[1]], response)
        assert_in([0, 502, 'connect error', subs[2]], response)
        assert_in([0, 500, 'internal error', subs[3]], response)
        assert_in([0, 200, 'gcm_ok', mob_ids[mob_order[0]]], response)
        assert_in([0, 205, 'subscription dropped', mob_ids[mob_order[1]]], response)
        assert_in([0, 200, '{"new_token":"...4"}', mob_ids[mob_order[2]]], response)
        assert_equals(len(response), len(subs))

        assert_equals(len(subscriber1.messages), 1)
        assert_equals(len(mobile.requests), 1)
        assert(mobile.requests[0].path.startswith('/batch_push'))

    def test_batch_convey(self):
        subscriber1.impl.set_response(raw_response='Hello')
        subscriber2.impl.set_response_code(400)
        subscriber2.impl.set_response(raw_response='Fail')
        subscriber500.impl.set_response_code(500)
        subscriber500.impl.set_response(raw_response='internal error')
        mobile.impl.set_response_code(200)
        mobile.impl.set_response(raw_response=json.dumps({'results':[
            {'code':200, 'result':'gcm_ok'},
            {'code':205, 'result':''},
            {'code':200, 'result':{'new_token':'new_fcm_test_token'}}]}))
        subs = []
        subs.append(hub.subscribe(Testing.uid1(), Testing.service1(), subscriber1.url))
        subs.append(hub.subscribe(Testing.uid1(), Testing.service1(), subscriber2.url))
        subs.append(hub.subscribe(Testing.uid1(), Testing.service1(), dead_subscriber_url))
        subs.append(hub.subscribe(Testing.uid1(), Testing.service1(), subscriber500.url))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/1', **{'platform':'fcm', 'session_key': 'def'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/2', **{'platform':'fcm', 'session_key': 'ghi'}))
        response = hub.batch_binary_notify(self.message, [(Testing.uid1(), '')])

        mob_ids = ['mob:abc', 'mob:def', 'mob:ghi']
        mob_order = [int(x) for x in json.loads(parse_qs(mobile.requests[0].body)['tokens'][0])]

        assert_in([0, 200, 'Hello', subs[0]], response)
        assert_in([0, 400, 'Fail', subs[1]], response)
        assert_in([0, 502, 'connect error', subs[2]], response)
        assert_in([0, 500, 'internal error', subs[3]], response)
        assert_in([0, 200, 'gcm_ok', mob_ids[mob_order[0]]], response)
        assert_in([0, 205, 'subscription dropped', mob_ids[mob_order[1]]], response)
        assert_in([0, 200, '{"new_token":"...test_token"}', mob_ids[mob_order[2]]], response)
        assert_equals(len(response), len(subs))

        assert_equals(len(subscriber1.messages), 1)
        assert_equals(len(mobile.requests), 1)
        assert(mobile.requests[0].path.startswith('/batch_push'))

    def test_gcm_batch_fail(self): # gcm_compatibility
        mobile.impl.set_response(raw_response='Fail')
        mobile.impl.set_response_code(400)
        subs = []
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'gcm', 'session_key': 'abc'})) # gcm_compatibility
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/1', **{'platform':'gcm', 'session_key': 'def'})) # gcm_compatibility
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/2', **{'platform':'gcm', 'session_key': 'ghi'})) # gcm_compatibility
        response = hub.batch_binary_notify(self.message, [(Testing.uid1(), '')])
        assert_in([0, 400, 'Fail', 'mob:abc'], response)
        assert_in([0, 400, 'Fail', 'mob:def'], response)
        assert_in([0, 400, 'Fail', 'mob:ghi'], response)
        assert_equals(len(response), len(subs))

    def test_fcm_batch_fail(self):
        mobile.impl.set_response(raw_response='Fail')
        mobile.impl.set_response_code(400)
        subs = []
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/1', **{'platform':'fcm', 'session_key': 'def'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/2', **{'platform':'fcm', 'session_key': 'ghi'}))
        response = hub.batch_binary_notify(self.message, [(Testing.uid1(), '')])
        assert_in([0, 400, 'Fail', 'mob:abc'], response)
        assert_in([0, 400, 'Fail', 'mob:def'], response)
        assert_in([0, 400, 'Fail', 'mob:ghi'], response)
        assert_equals(len(response), len(subs))

    def test_gcm_batch_response_parse_order(self): # gcm_compatibility
        mobile.impl.set_response_code(200)
        mobile.impl.set_response(raw_response=json.dumps({'results':[
            {'code':200, 'result':'gcm_ok'},
            {'code':429, 'result':''},
            {'code':200, 'result':{'new_token':'test_gcm_new_token'}}]}))
        subs = []
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'gcm', 'session_key': 'abc'})) # gcm_compatibility
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/1', **{'platform':'gcm', 'session_key': 'def'})) # gcm_compatibility
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/2', **{'platform':'gcm', 'session_key': 'ghi'})) # gcm_compatibility
        response = hub.batch_binary_notify(self.message, [(Testing.uid1(), '')])

        assert_equals(len(mobile.requests), 1)
        query = parse_qs(mobile.requests[0].body)
        expected = [[0, 200, 'gcm_ok', 'mob:abc'],
                    [0, 429, 'rate limit', 'mob:def'],
                    [0, 200, '{\"new_token\":\"..._new_token\"}', 'mob:ghi']]
        sub_ids = [x[3] for x in expected]
        # Reorder expected answers' ids according to subscription order
        sub_order = json.loads(query['tokens'][0])
        # Rewrite sub ids
        for i in xrange(0, len(expected)):
            expected[i][3] = sub_ids[int(sub_order[i])]

        assert_equals(expected, response)

    def test_fcm_batch_response_parse_order(self):
        mobile.impl.set_response_code(200)
        mobile.impl.set_response(raw_response=json.dumps({'results':[
            {'code':200, 'result':'gcm_ok'},
            {'code':429, 'result':''},
            {'code':200, 'result':{'new_token':'test_fcm_new_token'}}]}))
        subs = []
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/1', **{'platform':'fcm', 'session_key': 'def'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/2', **{'platform':'fcm', 'session_key': 'ghi'}))
        response = hub.batch_binary_notify(self.message, [(Testing.uid1(), '')])

        assert_equals(len(mobile.requests), 1)
        query = parse_qs(mobile.requests[0].body)
        expected = [[0, 200, 'gcm_ok', 'mob:abc'],
                    [0, 429, 'rate limit', 'mob:def'],
                    [0, 200, '{\"new_token\":\"..._new_token\"}', 'mob:ghi']]
        sub_ids = [x[3] for x in expected]
        # Reorder expected answers' ids according to subscription order
        sub_order = json.loads(query['tokens'][0])
        # Rewrite sub ids
        for i in xrange(0, len(expected)):
            expected[i][3] = sub_ids[int(sub_order[i])]

        assert_equals(expected, response)

    def test_mobile_platforms_gcm(self): # gcm_compatibility
        mobile.impl.set_response_code(200)
        mobile.impl.set_response(raw_response='')
        subs = []
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'gcm', 'session_key': 'abc'})) # gcm_compatibility
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/1', **{'platform':'gcm', 'session_key': 'def'})) # gcm_compatibility
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/2', **{'platform':'apns', 'session_key': 'ghi'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/3', **{'platform':'apns', 'session_key': 'qwe'}))
        response = hub.batch_binary_notify(self.message, [(Testing.uid1(), '')])

        assert_equals(len(mobile.requests), 3)
        assert_equals(len(response), 4)


    def test_mobile_platforms(self):
        mobile.impl.set_response_code(200)
        mobile.impl.set_response(raw_response='')
        subs = []
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/1', **{'platform':'fcm', 'session_key': 'def'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/2', **{'platform':'apns', 'session_key': 'ghi'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/3', **{'platform':'apns', 'session_key': 'qwe'}))
        response = hub.batch_binary_notify(self.message, [(Testing.uid1(), '')])

        assert_equals(len(mobile.requests), 3)
        assert_equals(len(response), 4)

    def test_fcm_with_topics(self):
        mobile.impl.set_response_code(200)
        mobile.impl.set_response(raw_response='')
        subs = []
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/1', **{'platform':'fcm', 'session_key': 'def'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/2', **{'platform':'fcm', 'session_key': 'ghi'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test//topics/topic1', **{'platform':'fcm', 'session_key': 'topic1'}))
        subs.append(hub.subscribe_mobile(Testing.uid2(), Testing.service1(),
            'xivamob:app.test//topics/topic2', **{'platform':'fcm', 'session_key': 'topic2'}))
        response = hub.batch_binary_notify(self.message, [(Testing.uid1(), ''), (Testing.uid2(), '')])

        assert_equals(len(mobile.requests), 3)
        assert_in([0, 200, 'OK', 'mob:topic1'], response)
        assert_in([1, 200, 'OK', 'mob:topic2'], response)
        urls = [req.path for req in mobile.requests]
        assert_equals(len([u for u in urls if '/push/' in u]), 2)
        assert_equals(len([u for u in urls if '/batch_push/' in u]), 1)

    def test_disk_pushes_do_not_batch(self):
        disk_payload = '''{
            "values": [],
            "root": {
                "tag": "album_deltas_updated",
                "parameters": {
                    "revision": 4824
                }
            }
        }'''
        mobile.impl.set_response_code(200)
        mobile.impl.set_response(raw_response='')
        disk_service = 'disk-json'
        subs = []
        subs.append(hub.subscribe_mobile(Testing.uid1(), disk_service,
            'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc'}))
        subs.append(hub.subscribe_mobile(Testing.uid1(), disk_service,
            'xivamob:app.test/1', **{'platform':'fcm', 'session_key': 'def'}))
        self.message[MessageFields.service] = disk_service
        self.message[MessageFields.raw_data] = disk_payload
        response = hub.batch_binary_notify(self.message, [(Testing.uid1(), '')])

        assert_equals(len(mobile.requests), 2)
        urls = [req.path for req in mobile.requests]
        assert_equals(len([u for u in urls if '/push/' in u]), 2)
        assert_equals(len([u for u in urls if '/batch_push/' in u]), 0)
