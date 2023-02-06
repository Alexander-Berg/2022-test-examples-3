from pycommon import *
from subscriber import *

APP_TEST_CALLBACK_TOKEN_1 = 'xivamob:app.test/007'
APP_TEST_CALLBACK_TOKEN_2 = 'xivamob:app.test/008'
APP_OTHER_CALLBACK_TOKEN = 'xivamob:app.other/007'
UUID_1 = 'abc'
UUID_2 = 'def'
PLATFORM = 'gcm'
DEVICE_1 = 'device1'
DEVICE_2 = 'device2'

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

class TestSubscribe:
    def setup(self):
        self.simple_callback = 'http://' + Testing.host()+ ':' + str(Testing.port()) + '/ping'
    def teardown(self):
        subscriber.messages = []
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())
        hub.unsubscribe_all(Testing.uid1(), Testing.service2())
        hub.unsubscribe_all(Testing.uid2(), Testing.service1())
        hub.unsubscribe_all(Testing.uid2(), Testing.service2())

    def test_subscribe_without_params_returns_400(self):
        params = dict()
        params['uid'] = Testing.uid1()
        check(hub.raw().get("/subscribe", params), 400, 'missing argument "service"')
        params['service'] = Testing.service2()
        check(hub.raw().get("/subscribe", params), 400, 'missing argument "callback"')

    def test_subscribe_returns_correct_id(self):
        params = ({ 'uid' : Testing.uid1(), 'service' : Testing.service1(),
            'callback' : self.simple_callback })
        response = hub.raw().get("/subscribe", params)
        assert_equals(response.status, 200)
        subid = response.body
        assert_equals("x-xiva-position" in response.headers, False)
        assert_equals("x-xiva-count" in response.headers, False)
        assert_not_equal(subid, 'OK')
        # ":" is also valid for mobile subscriptions
        assert_regexp_matches(subid, "[a-zA-Z0-9]+")

    def test_subscribe_with_id_returns_correct_id(self):
        params = ({ 'uid' : Testing.uid1(), 'service' : Testing.service1(),
            'callback' : self.simple_callback, 'id' : 'any_subscription_id' })
        response = hub.raw().get("/subscribe", params)
        assert_equals(response.status, 200)
        assert_equals(response.body, 'any_subscription_id')

    def test_subscribe_mobile_returns_correct_id_gcm(self): # gcm_compatibility
        subid = hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'gcm', 'session_key': 'abc'}) # gcm_compatibility
        # ":" is also valid for mobile subscriptions
        assert_not_equal(subid, 'OK')
        assert_regexp_matches(subid, "[a-zA-Z0-9:]+")

    def test_subscribe_mobile_returns_correct_id_fcm(self):
        subid = hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc'})
        # ":" is also valid for mobile subscriptions
        assert_not_equal(subid, 'OK')
        assert_regexp_matches(subid, "[a-zA-Z0-9:]+")

    def test_subscribe_mobile_with_id_returns_correct_id(self):
        subid = hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc',
            'id' : 'mob_subscription_id'})
        assert_equals(subid, 'mob_subscription_id')

    def test_success_subscribe_text_uid(self):
        params = ({ 'uid' : Testing.text_uid(), 'service' : Testing.service1(),
            'callback' : self.simple_callback })
        response = hub.raw().get("/subscribe", params)
        assert_equals(response.status, 200)

    def test_subscribe_adds_subscription(self):
        params = ({ 'uid' : Testing.uid1(), 'service' : Testing.service1(),
            'callback' : self.simple_callback })
        response = hub.raw().get("/subscribe", params)
        assert_equals(response.status, 200)
        subid = response.body
        list = hub.list(Testing.uid1(), Testing.service1())
        list_ids = map(lambda x: x['id'], list)
        assert_list_equal(list_ids, [subid])

    def test_subscribe_with_id_adds_subscription(self):
        params = ({ 'uid' : Testing.uid1(), 'service' : Testing.service1(),
            'callback' : self.simple_callback, 'id' : 'any_subscription_id' })
        response = hub.raw().get("/subscribe", params)
        assert_equals(response.status, 200)
        list = hub.list(Testing.uid1(), Testing.service1())
        list_ids = map(lambda x: x['id'], list)
        assert_list_equal(list_ids, ['any_subscription_id'])

    def test_subscribe_mobile_with_id_adds_subscription(self):
        hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc',
            'id' : 'mob_subscription_id'})
        list = hub.list(Testing.uid1(), Testing.service1())
        list_ids = map(lambda x: x['id'], list)
        assert_list_equal(list_ids, ['mob_subscription_id'])

    def test_double_subscribe_adds_one_subscription(self):
        params = ({ 'uid' : Testing.uid1(), 'service' : Testing.service1(),
            'callback' : self.simple_callback })
        response = hub.raw().get("/subscribe", params)
        assert_equals(response.status, 200)
        response = hub.raw().get("/subscribe", params)
        assert_equals(response.status, 200)
        subid = response.body
        list = hub.list(Testing.uid1(), Testing.service1())
        list_ids = map(lambda x: x['id'], list)
        assert_list_equal(list_ids, [subid])

    def test_double_subscribe_with_id_adds_one_subscription(self):
        params = ({ 'uid' : Testing.uid1(), 'service' : Testing.service1(),
            'callback' : self.simple_callback, 'id' : 'any_subscription_id' })
        response = hub.raw().get("/subscribe", params)
        assert_equals(response.status, 200)
        response = hub.raw().get("/subscribe", params)
        assert_equals(response.status, 200)
        subid = response.body
        list = hub.list(Testing.uid1(), Testing.service1())
        list_ids = map(lambda x: x['id'], list)
        assert_list_equal(list_ids, ['any_subscription_id'])

    def test_subscribe_connection_id_gcm(self): # gcm_compatibility
        subid1 = hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'gcm', 'session_key': 'abc'}) # gcm_compatibility
        subid2 = hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/1', **{'platform':'gcm', 'session_key': 'def', 'bb_connection_id': 't:12345'}) # gcm_compatibility
        subid3 = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback, **{'session_key': '123'})
        subid4 = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback, **{'session_key': '456', 'bb_connection_id': 't:54321'})
        sub_list = hub.list(Testing.uid1(), Testing.service1())
        sub_by_id = dict(zip([sub['id'] for sub in sub_list], sub_list))
        assert_equals(sub_by_id[subid1]['connection_id'], '')
        assert_equals(sub_by_id[subid2]['connection_id'], 't:12345')
        assert_equals(sub_by_id[subid3]['connection_id'], '')
        assert_equals(sub_by_id[subid4]['connection_id'], 't:54321')

    def test_subscribe_connection_id(self):
        subid1 = hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc'})
        subid2 = hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/1', **{'platform':'fcm', 'session_key': 'def', 'bb_connection_id': 't:12345'})
        subid3 = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback, **{'session_key': '123'})
        subid4 = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback, **{'session_key': '456', 'bb_connection_id': 't:54321'})
        sub_list = hub.list(Testing.uid1(), Testing.service1())
        sub_by_id = dict(zip([sub['id'] for sub in sub_list], sub_list))
        assert_equals(sub_by_id[subid1]['connection_id'], '')
        assert_equals(sub_by_id[subid2]['connection_id'], 't:12345')
        assert_equals(sub_by_id[subid3]['connection_id'], '')
        assert_equals(sub_by_id[subid4]['connection_id'], 't:54321')

    def test_subscribe_batch_list_gcm_compatibility(self):
        subid1 = hub.subscribe_mobile(Testing.uid1(), Testing.service1(), 'xivamob:app.test/0', **{'platform':'gcm', 'session_key': 'abc'}) # gcm_compatibility
        subid2 = hub.subscribe_mobile(Testing.uid1(), Testing.service2(), 'xivamob:app.test/1', **{'platform':'gcm', 'session_key': 'def'}) # gcm_compatibility
        subid3 = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback, **{'session_key': '123'})
        subid4 = hub.subscribe(Testing.uid1(), Testing.service2(), self.simple_callback, **{'session_key': '456'})
        response = hub.get_200("/batch_list_json", { 'uid': Testing.uid1(), 'services': Testing.service1() + ',' + Testing.service2() })
        sub_list = json.loads(response.body)
        assert_equals(len(sub_list), 4)
        sub_by_id = dict(zip([sub['id'] for sub in sub_list], sub_list))
        assert_equals(sub_by_id[subid1]['service'], Testing.service1())
        assert_equals(sub_by_id[subid2]['service'], Testing.service2())
        assert_equals(sub_by_id[subid3]['service'], Testing.service1())
        assert_equals(sub_by_id[subid4]['service'], Testing.service2())

    def test_subscribe_batch_list(self):
        subid1 = hub.subscribe_mobile(Testing.uid1(), Testing.service1(), 'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc'})
        subid2 = hub.subscribe_mobile(Testing.uid1(), Testing.service2(), 'xivamob:app.test/1', **{'platform':'fcm', 'session_key': 'def'})
        subid3 = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback, **{'session_key': '123'})
        subid4 = hub.subscribe(Testing.uid1(), Testing.service2(), self.simple_callback, **{'session_key': '456'})
        response = hub.get_200("/batch_list_json", { 'uid': Testing.uid1(), 'services': Testing.service1() + ',' + Testing.service2() })
        sub_list = json.loads(response.body)
        assert_equals(len(sub_list), 4)
        sub_by_id = dict(zip([sub['id'] for sub in sub_list], sub_list))
        assert_equals(sub_by_id[subid1]['service'], Testing.service1())
        assert_equals(sub_by_id[subid2]['service'], Testing.service2())
        assert_equals(sub_by_id[subid3]['service'], Testing.service1())
        assert_equals(sub_by_id[subid4]['service'], Testing.service2())

    def test_subscribe_batch_uids_list(self):
        subid1 = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback, **{'session_key': '123'})
        subid2 = hub.subscribe(Testing.uid2(), Testing.service1(), self.simple_callback, **{'session_key': '456'})
        response = hub.get_200("/batch_uids_list_json",
            { 'uid': Testing.uid1() + "," + Testing.uid2(), 'service': Testing.service1() })
        sub_list = json.loads(response.body)
        assert_equals(len(sub_list), 2)
        sub_by_id = dict(zip([sub['id'] for sub in sub_list], sub_list))
        assert_equals(sub_by_id[subid1]['service'], Testing.service1())
        assert_equals(sub_by_id[subid2]['service'], Testing.service1())
        assert_equals(sub_by_id[subid1]['uid'], Testing.uid1())
        assert_equals(sub_by_id[subid2]['uid'], Testing.uid2())

    def test_subscribe_with_history(self):
        hub.subscribe(Testing.uid1(), Testing.service1(), subscriber.url)
        hub.notify(Testing.uid1(), Testing.service1(), 'test_data')
        wait(lambda: len(subscriber.messages) == 1, 5)
        assert_equals(len(subscriber.messages), 1)
        local_id = subscriber.messages[0][10]

        subscriber.messages = []
        response = hub.subscribe_ex(Testing.uid1(), Testing.service1(), subscriber.url,
            **{'position':local_id-1, 'history_count': 1})
        assert_equals(response.headers["x-xiva-position"], str(local_id-1))
        assert_equals(response.headers["x-xiva-count"], "1")
        wait(lambda: len(subscriber.messages) == 1, 5)
        assert_equals(len(subscriber.messages), 1)

        subscriber.messages = []
        response = hub.subscribe_ex(Testing.uid1(), Testing.service1(), subscriber.url,
            **{'position':local_id-1, 'history_count': 1, "strict_position": 1})
        assert_equals(response.headers["x-xiva-position"], str(local_id-1))
        assert_equals(response.headers["x-xiva-count"], "1")
        wait(lambda: len(subscriber.messages) == 1, 5)
        assert_equals(len(subscriber.messages), 1)

        subscriber.messages = []
        response = hub.subscribe_ex(Testing.uid1(), Testing.service1(), subscriber.url,
            **{'position':local_id-3, 'history_count': 1})
        assert_equals(response.headers["x-xiva-position"], str(local_id-1))
        assert_equals(response.headers["x-xiva-count"], "1")
        wait(lambda: len(subscriber.messages) == 1, 5)
        assert_equals(len(subscriber.messages), 1)

        subscriber.messages = []
        response = hub.subscribe_ex(Testing.uid1(), Testing.service1(), subscriber.url,
            **{'position':local_id-3, 'history_count': 1, "strict_position": 1})
        assert_equals(response.headers["x-xiva-position"], str(local_id))
        assert_equals(response.headers["x-xiva-count"], "0")
        time.sleep(1)
        assert_equals(len(subscriber.messages), 0)

    def test_add_broken_subscription(self):
        # It is not feasible to sleep here for 30 seconds,
        # therefore this test does not check that the broken
        # subscription actually gets removed, only that the
        # method returns 200.
        params = ({ 'platform' : 'apns', 'id' : 'broken'})
        response = hub.raw().get("/add_broken_subscription", params)
        assert_equals(response.status, 200)

    def test_subscribe_mobile_schedules_deduplication(self):
        hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            APP_TEST_CALLBACK_TOKEN_1, platform=PLATFORM, session_key=UUID_1,
            device=DEVICE_1)
        time.sleep(1.1)
        hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            APP_TEST_CALLBACK_TOKEN_1, platform=PLATFORM, session_key=UUID_2,
            device=DEVICE_2)
        # Give deduplication some time to work. TODO: move to unit tests, test
        # without sleeping.
        time.sleep(1.0)
        assert_equals(hub.list_ids(Testing.uid1(), Testing.service1()), ['mob:' + UUID_2])

    # For the case when server updated and passes id, but hub is not yet updated and don't know about id
    def test_subscribe_mobile_with_unknown_arg_works(self):
        subid = hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            'xivamob:app.test/0', **{'platform':'fcm', 'session_key': 'abc',
            'unknown' : 'argument'})
        assert_equals(subid, 'mob:abc')
        list = hub.list(Testing.uid1(), Testing.service1())
        list_ids = map(lambda x: x['id'], list)
        assert_list_equal(list_ids, ['mob:abc'])

class TestUnsubscribe:
    def setup(self):
        self.simple_callback = 'http://' + Testing.host()+ ':' + str(Testing.port()) + '/ping'
        subscriber.set_response(200)

    def teardown(self):
        subscriber.messages = []
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())
        hub.unsubscribe_all(Testing.uid1(), Testing.service2())
        hub.unsubscribe_all(Testing.uid2(), Testing.service1())
        hub.unsubscribe_all(Testing.uid2(), Testing.service2())

    # positive

    def test_unsubscribe_dels_subscription(self):
        subid = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback)
        response = hub.raw().get("/unsubscribe", { 'uid' : Testing.uid1(),
            'service' : Testing.service1(), 'subscription-id' : subid})
        list = hub.list(Testing.uid1(), Testing.service1())
        assert_equals(len(list), 0)

    # negative

    def test_unsubscribe_without_parameters_gives_400(self):
        response = hub.raw().get("/unsubscribe", {})
        assert_equals(response.status, 400)

    def test_unsubscribe_without_service_gives_400(self):
         check(hub.raw().get("/unsubscribe",
            {'uid' : Testing.uid1(), 'subscription-id' : "fake"}),
            400, 'missing argument "service"')

    def test_unsubscribe_without_id_gives_400(self):
        check(hub.raw().get("/unsubscribe", {'uid' : Testing.uid1(),
            'service' : Testing.service1()}),
            400, 'missing argument "subscription-id"')

    def test_unsubscribe_with_wrong_id_does_nothing(self):
        subid = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback)
        response = hub.raw().get("/unsubscribe", { 'uid' : Testing.uid1(),
            'service' : Testing.service1(), 'subscription-id' : subid[2:]+subid[0:2]})
        list = hub.list(Testing.uid1(), Testing.service1())
        assert_equals(len(list), 1)

    def test_unsubscribe_with_wrong_service_does_nothing(self):
        subid = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback)
        response = hub.raw().get("/unsubscribe", { 'uid' : Testing.uid1(),
            'service' : Testing.service2(), 'subscription-id' : subid})
        list = hub.list(Testing.uid1(), Testing.service1())
        assert_equals(len(list), 1)

    def test_unsubscribe_with_wrong_uid_does_nothing(self):
        subid = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback)
        response = hub.raw().get("/unsubscribe", { 'uid' : Testing.uid2(),
            'service' : Testing.service1(), 'subscription-id' : subid})
        list = hub.list(Testing.uid1(), Testing.service1())
        assert_equals(len(list), 1)

    def test_subscribe_with_explicit_id_returns_same_id(self):
        params = ({ 'uid' : Testing.uid1(), 'service' : Testing.service1(),
            'callback' : self.simple_callback, 'id' : 'test-uniq-id' })
        response = hub.raw().get("/subscribe", params)
        assert_equals(response.status, 200)
        subid = response.body
        assert_equals(subid, 'test-uniq-id')

    def test_worker_deactivates_subscription_on_204(self):
        subid = hub.subscribe(Testing.uid1(), Testing.service1(), subscriber.url)
        subscriber.set_response(204)
        response = hub.raw().post("/notify/" + Testing.service1(),
           {'uid' : Testing.uid1()}, 'test_data')
        assert_equals(response.status, 200)
        wait(lambda: any(sub['url'].startswith('inactive://') for sub in hub.list(Testing.uid1(), Testing.service1())), 5, 1)
        list = hub.list(Testing.uid1(), Testing.service1())
        assert_equals(len(list), 1)
        assert_true(list[0]['url'].startswith('inactive://'))

class TestBatchUnsubscribe:
    def setup(self):
        self.simple_callback = 'http://' + Testing.host()+ ':' + str(Testing.port()) + '/ping'
        self.sub1 = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback, **{'session_key': '123'})
        self.sub2 = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback, **{'session_key': '456'})
        self.list = hub.list(Testing.uid1(), Testing.service1())
        assert_equals(len(self.list), 2)

    def teardown(self):
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())

    def batch_unsubscribe(self, service, uid, ids, ts = ''):
        return hub.raw().get("/batch_unsubscribe", {'service': service, 'uid': uid, 'ids': ','.join(ids), 'ts': ts})

    def test_unsubscribes_given_subs_with_default_ts(self):
        time.sleep(1) # Hack because of different time rounding in db and hub.
        resp = self.batch_unsubscribe(Testing.service1(), Testing.uid1(), [self.sub1, self.sub2])
        assert_ok(resp)
        assert_equals(len(hub.list(Testing.uid1(), Testing.service1())), 0)

    def test_unsubscribes_given_subs(self):
        ts = max(s['init_time'] for s in self.list)
        resp = self.batch_unsubscribe(Testing.service1(), Testing.uid1(), [self.sub1, 'fake'], ts)
        assert_ok(resp)
        list = hub.list(Testing.uid1(), Testing.service1())
        assert_equals(len(list), 1)
        assert_equals(list[0]['id'], self.sub2)

    def test_unsubscribes_before_ts_only(self):
        time.sleep(1)
        sub3 = hub.subscribe(Testing.uid1(), Testing.service1(), self.simple_callback, **{'session_key': '789'})
        list = hub.list(Testing.uid1(), Testing.service1())
        assert_equals(len(list), 3)
        ts = max(s['init_time'] for s in list) - 1
        resp = self.batch_unsubscribe(Testing.service1(), Testing.uid1(), [self.sub1, self.sub2, sub3], ts)
        assert_ok(resp)
        list = hub.list(Testing.uid1(), Testing.service1())
        assert_equals(len(list), 1)
        assert_equals(list[0]['id'], sub3)

    def test_400_with_missing_param(self):
        resp = self.batch_unsubscribe('', Testing.uid1(), [self.sub1, self.sub2])
        assert_bad_request(resp, 'missing argument "service"')
        resp = self.batch_unsubscribe(Testing.service1(), Testing.uid1(), [])
        assert_bad_request(resp, 'missing argument "ids"')
        resp = self.batch_unsubscribe(Testing.service1(), Testing.uid1(), ['',''])
        assert_bad_request(resp, 'empty ids list')

    def test_doesnt_unsubscribe_other_subs(self):
        ts = min(s['init_time'] for s in self.list) - 1
        resp = self.batch_unsubscribe(Testing.service2(), Testing.uid1(), [self.sub1, self.sub2], ts)
        assert_ok(resp)
        resp = self.batch_unsubscribe(Testing.service1(), Testing.uid2(), [self.sub1, self.sub2], ts)
        assert_ok(resp)
        resp = self.batch_unsubscribe(Testing.service1(), Testing.uid1(), ['fake1', 'fake2'], ts)
        assert_ok(resp)
        assert_equals(len(hub.list(Testing.uid1(), Testing.service1())), 2)

class TestDeduplicate:
    def setup(self):
        # Disable implicit deduplication in subscribe_mobile, since
        # we want to test the explicit one.
        hub.disable('deduplication')

    def teardown(self):
        hub.unsubscribe_all(Testing.uid1(), Testing.service1())
        hub.enable('deduplication')

    def subscribe_mobile(self, callback1, callback2, device1, device2):
        hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            callback1, platform=PLATFORM, session_key=UUID_1, device=device1)
        # Duplicates must have different init_time.
        # Or must they? TODO: drop this restriction.
        time.sleep(1.1)
        hub.subscribe_mobile(Testing.uid1(), Testing.service1(),
            callback2, platform=PLATFORM, session_key=UUID_2, device=device2)

    def subscribe_push_token_duplicates(self, callback):
        self.subscribe_mobile(callback, callback, DEVICE_1, DEVICE_2)

    def subscribe_device_duplicates(self, device):
        self.subscribe_mobile(APP_TEST_CALLBACK_TOKEN_1, APP_TEST_CALLBACK_TOKEN_2,
            device, device)

    def test_deduplicate_unsubscribes_push_token_duplicates(self):
        self.subscribe_push_token_duplicates(APP_TEST_CALLBACK_TOKEN_1)
        response = hub.deduplicate(Testing.uid1(), Testing.service1())
        assert_equals(hub.list_ids(Testing.uid1(), Testing.service1()), ['mob:' + UUID_2])
        assert_equals(json.loads(response.body), {'unsubscribed': [{'session_key': UUID_1}]})

    def test_deduplicate_unsubscribes_device_duplicates(self):
        self.subscribe_device_duplicates(DEVICE_1)
        response = hub.deduplicate(Testing.uid1(), Testing.service1())
        assert_equals(hub.list_ids(Testing.uid1(), Testing.service1()), ['mob:' + UUID_2])
        assert_equals(json.loads(response.body), {'unsubscribed': [{'session_key': UUID_1}]})

    def test_deduplicate_allows_different_push_tokens(self):
        self.subscribe_mobile(APP_TEST_CALLBACK_TOKEN_1, APP_TEST_CALLBACK_TOKEN_2,
            DEVICE_1, DEVICE_2)
        response = hub.deduplicate(Testing.uid1(), Testing.service1())
        assert_equals(set(hub.list_ids(Testing.uid1(), Testing.service1())),
            {'mob:' + UUID_1, 'mob:' + UUID_2})
        assert_equals(json.loads(response.body), {'unsubscribed': []})

    def test_deduplicate_allows_different_apps_on_device(self):
        self.subscribe_mobile(APP_TEST_CALLBACK_TOKEN_1, APP_OTHER_CALLBACK_TOKEN,
            DEVICE_1, DEVICE_1)
        response = hub.deduplicate(Testing.uid1(), Testing.service1())
        assert_equals(set(hub.list_ids(Testing.uid1(), Testing.service1())),
            {'mob:' + UUID_1, 'mob:' + UUID_2})
        assert_equals(json.loads(response.body), {'unsubscribed': []})

    # TODO: move to units, this may give false positives.
    def test_subscribe_does_not_deduplicate(self):
        self.subscribe_push_token_duplicates(APP_TEST_CALLBACK_TOKEN_1)
        hub.enable('deduplication')
        sub_id = hub.subscribe(Testing.uid1(), Testing.service1(), 'http://localhost:980/ping')
        time.sleep(1.0)
        assert_equals(set(hub.list_ids(Testing.uid1(), Testing.service1())),
            {'mob:' + UUID_1, 'mob:' + UUID_2, sub_id})
