from pycommon import *
import time

def setUp():
    global xiva, hub, wmi
    xiva = XivaApiV2(host='localhost', port=18083)
    hub = fake_server(host='localhost', port=17081, raw_response='OK')
    wmi = fake_server(host='localhost', port=17084, raw_response='OK')

def tearDown():
    hub.fini()
    wmi.fini()

settings = Empty()
settings.list_interval = 0.2 # sec
settings.max_age = 8 # sec

user = Empty()
user.id = "200"

another_user = Empty()
another_user.id = "300"

service_with_feature = "tst1"
service_with_feature_token = "L001"
service_with_no_feature = "autoru"
service_with_no_feature_token = "L123456"

user_attrs = {
    "uid" : user.id,
    "service" : service_with_feature,
    "client" : "test",
    "session" : "B"
}

another_user_attrs = {
    "uid" : another_user.id,
    "service" : service_with_feature,
    "client" : "test",
    "session" : "B"
}

topic = "topic1"
another_topic = "topic2"

topic_uid = 'topic:' + topic
topic_attrs = dict(user_attrs)
topic_attrs['topic'] = topic

def list_with_one_user():
    ret = []
    ret.append(user_attrs)
    ret[0]['init_time'] = int(time.time())
    ret[0]['session_key'] = ret[0]['session']
    return ret

def list_with_one_topic():
    ret = []
    ret.append(dict(topic_attrs))
    ret[0]['uid'] = 'topic:' + ret[0]['topic']
    del ret[0]['topic']
    ret[0]['init_time'] = int(time.time())
    ret[0]['session_key'] = ret[0]['session']
    return ret

def list_with_two_users():
    ret = list_with_one_user()
    ret.append(another_user_attrs)
    ret[1]['init_time'] = int(time.time())
    ret[1]['session_key'] = ret[1]['session']
    return ret

def list_in_topic(user_list):
    ret = user_list
    for i in ret:
        i['uid'] = 'topic:' + topic
    return ret

def list_in_another_topic(user_list):
    ret = user_list
    for i in ret:
        i['uid'] = 'topic:' + another_topic
    return ret

def subscribe_user_impl(watch, uid, attrs):
    wmi.set_response(raw_response='{"check_cookies":{"uid":'+uid+'}}')
    (resp, ws) = xiva.subscribe_websocket(
        headers={"cookie" : "123", "Origin" : "push.yandex.ru"},
        watch_subscribers=1 if watch else 0,
        **attrs
    )
    time.sleep(0.1)
    assert_ws_ok(resp, ws)
    return ws

def subscribe_user(watch):
    return subscribe_user_impl(watch, user.id, user_attrs)

def subscribe_user_to_unwatchable_service(watch):
    user_attrs_extended = user_attrs.copy()
    user_attrs_extended["service"] = service_with_no_feature
    return subscribe_user_impl(watch, user.id, user_attrs_extended)

def subscribe_user_topic(watch):
    return subscribe_user_impl(watch, user.id, topic_attrs)

def watch_subscribers(topics):
    wmi.set_response(raw_response='{"check_cookies":{"uid":'+user.id+'}}')
    (resp, ws) = xiva.WS('/v2/watch/subscribers?topic=' + ','.join(topics) + '&service=' + service_with_feature,
        headers={"cookie" : "123", "Origin" : "push.yandex.ru"}
    )
    time.sleep(0.1)
    assert_ws_ok(resp, ws, expect_greeting=False)
    return ws

def watch_subscribers_forbidden_service(topics):
    wmi.set_response(raw_response='{"check_cookies":{"uid":'+user.id+'}}')
    (resp, ws) = xiva.WS('/v2/watch/subscribers?topic=' + ','.join(topics) + '&service=' + service_with_no_feature,
        headers={"cookie" : "123", "Origin" : "push.yandex.ru"}
    )
    time.sleep(0.1)
    assert_ws_ok(resp, ws, expect_greeting=False)
    return ws

def get_secret_sign(service, token, uid, topic):
    resp = xiva.secret_sign(token=token, service=service, uid=uid, topic=topic)
    assert_ok(resp)
    json_result = json.loads(resp.body)
    assert_in('sign', json_result)
    assert_in('ts', json_result)
    return json_result['sign'], json_result['ts']

def watch_subscribers_sign(topics):
    topics_str = ','.join(topics)
    sign, ts = get_secret_sign(service=service_with_feature,
        token=service_with_feature_token, uid='uid1', topic=topics_str)
    resp, ws = xiva.watch_subscribers(service=service_with_feature,
        uid='uid1', topic=topics_str, sign=sign, ts=ts)
    time.sleep(0.1)
    assert_ws_ok(resp, ws, expect_greeting=False)
    return ws

def watch_subscribers_sign_forbidden_service(topics):
    topics_str = ','.join(topics)
    sign, ts = get_secret_sign(service=service_with_no_feature,
        token=service_with_no_feature_token, uid='uid1', topic=topics_str)
    resp, ws = xiva.watch_subscribers(service=service_with_no_feature,
        uid='uid1', topic=topics_str, sign=sign, ts=ts)
    time.sleep(0.1)
    assert_ws_ok(resp, ws, expect_greeting=False)
    return ws

def wait_for_hub_requests(count):
    wait(lambda: len(hub.requests) >= count, 4)
    time.sleep(0.05)

def assert_ws_list_len(ws_message, expected_len):
    neq_(ws_message, None)
    event = json.loads(ws_message.payload)
    assert_in("list", event)
    list = event["list"]
    if len(list) != expected_len:
        print event
        print str(time.time())
    eq_(len(list), expected_len)

class TestWatchSubscribers:
    def setup(self):
        hub.requests = []
        hub.response_chain_clear()
        hub.set_request_hook(lambda req: hub.requests.append(req))

    def teardown(self):
        time.sleep(0.1)

    def test_subscribe_receive_list_if_requested(self):
        list = json.dumps(list_with_one_user())
        hub.response_chain_append(Response(code=200)) # subscribe
        hub.response_chain_append(Response(code=200, body=list))
        hub.set_response(raw_response=list)
        ws = subscribe_user(watch=True)
        wait_for_hub_requests(2)
        eq_(len(hub.requests), 2)
        check_caught_request(hub.requests[1], '/list_json',
            uid=user.id,
            service=service_with_feature)

        ws_message = ws.recv_message() # ping
        ws_message = ws.recv_message() # subscribed
        neq_(ws_message, None)

        subscribers = json.loads(ws_message.payload)
        assert_equals(subscribers, {
            'list': [
                {
                    'session': 'B',
                    'client': 'test',
                    'uid': user.id
                }
            ],
            'event': 'xiva.subscribers',
            'service': service_with_feature
            })

    def test_subscribe_topic_receive_list_if_requested(self):
        list = json.dumps(list_with_one_topic())
        hub.response_chain_append(Response(code=200)) # subscribe
        hub.response_chain_append(Response(code=200, body=list))
        hub.set_response(raw_response=list)
        ws = subscribe_user_topic(watch=True)
        wait_for_hub_requests(2)
        eq_(len(hub.requests), 2)
        check_caught_request(hub.requests[1], '/list_json',
            uid=topic_uid,
            service=service_with_feature)

        ws_message = ws.recv_message() # ping
        ws_message = ws.recv_message() # subscribed
        neq_(ws_message, None)

        subscribers = json.loads(ws_message.payload)
        assert_equals(subscribers, {
            'list': [
                {
                    'session': 'B',
                    'client': 'test',
                    'topic': topic
                }
            ],
            'event': 'xiva.subscribers',
            'service': service_with_feature
            })

    def test_subscribe_no_list_if_forbidden(self):
        ws = subscribe_user_to_unwatchable_service(watch=True)
        wait_for_hub_requests(1)
        eq_(len(hub.requests), 1) # subscribed
        ws_message = ws.recv_message() # ping
        ws_message = ws.recv_message() # subscribed
        eq_(ws_message, None)

    def test_subscribe_no_list_if_not_requested(self):
        list = json.dumps(list_with_one_user())
        hub.response_chain_append(Response(code=200)) # subscribe
        hub.response_chain_append(Response(code=200, body=list))
        hub.set_response(raw_response=list)
        ws = subscribe_user(watch=False)
        wait_for_hub_requests(2)
        eq_(len(hub.requests), 1)

        ws_message = ws.recv_message() # ping
        ws_message = ws.recv_message() # subscribed
        eq_(ws_message, None)

    def test_subscribe_receive_list_once_if_no_changes(self):
        list = json.dumps(list_with_one_user())
        hub.response_chain_append(Response(code=200)) # subscribe
        hub.response_chain_append(Response(code=200, body=list))
        hub.set_response(raw_response=list)
        ws = subscribe_user(watch=True)
        wait_for_hub_requests(2)
        eq_(len(hub.requests), 2)

        ws.recv_message() # skip 'subscribed'

        assert_ws_list_len(ws.recv_message(), 1)

        assert_no_ws_message(ws)
        time.sleep(1.5*settings.list_interval)
        assert_no_ws_message(ws)

    def test_subscribe_receive_empty_list_if_subscription_is_too_old(self):
        list = json.dumps(list_with_one_user())
        hub.response_chain_append(Response(code=200)) # subscribe
        hub.response_chain_append(Response(code=200, body=list))
        hub.set_response(raw_response=list)
        ws = subscribe_user(watch=True)
        wait_for_hub_requests(2)
        print hub.requests[0].path
        eq_(len(hub.requests), 2)

        ws.recv_message() # skip 'subscribed'

        assert_ws_list_len(ws.recv_message(), 1)
        assert_no_ws_message(ws)

        list = list_with_one_user()
        list[0]['init_time'] -= settings.max_age
        hub.set_response(raw_response=json.dumps(list))

        wait_for_hub_requests(len(hub.requests) + 1)
        assert_ws_list_len(ws.recv_message(), 0)

    def test_subscribe_receive_new_list_on_changes(self):
        list = json.dumps(list_with_one_user())
        hub.response_chain_append(Response(code=200)) # subscribe
        hub.response_chain_append(Response(code=200, body=list))
        hub.set_response(raw_response=list)
        ws = subscribe_user(watch=True)
        wait_for_hub_requests(2)
        eq_(len(hub.requests), 2)

        ws.recv_message() # skip 'subscribed'

        assert_ws_list_len(ws.recv_message(), 1)
        assert_no_ws_message(ws)

        hub.set_response(raw_response=json.dumps(list_with_two_users()))
        wait_for_hub_requests(len(hub.requests) + 1)
        assert_ws_list_len(ws.recv_message(), 2)

    def test_watch_subscribers_unauth(self):
        wmi.set_response(raw_response='{}')
        (resp, ws) = xiva.WS('/v2/watch/subscribers?topic=a,b&service=' + service_with_feature)
        assert_ws_unauthorized(resp, ws)

    def test_watch_subscribers_unauth(self):
        wmi.set_response(raw_response='{}')
        (resp, ws) = xiva.WS('/v2/watch/subscribers?topic=a,b&service=' + service_with_feature)
        assert_ws_unauthorized(resp, ws)

    def test_watch_subscribers_sign_unauth(self):
        resp, ws = xiva.watch_subscribers(service=service_with_feature,
            uid='uid1', topic='t1,t2,t3,t4', sign='abc', ts='123')

    def test_watch_subscribers_only_for_topics(self):
        wmi.set_response(raw_response='{"check_cookies":{"uid":'+user.id+'}}')
        (resp, ws) = xiva.WS('/v2/watch/subscribers?uid'+another_user.id+'=a&service=' + service_with_feature)
        assert_ws_bad_request(resp, ws)

    def test_watch_subscribers_success(self):
        watch_subscribers([topic])
        watch_subscribers([topic, another_topic])

    def test_watch_subscribers_sign_success(self):
        watch_subscribers_sign(['t1', 't2', 't3', 't4'])

    def test_watch_subscribers_list_once(self):
        hub.set_response(raw_response=json.dumps(list_in_topic(list_with_one_user())))
        ws = watch_subscribers([topic])
        wait_for_hub_requests(1)
        assert_ws_list_len(ws.recv_message(), 1)
        wait_for_hub_requests(2)
        assert_no_ws_message(ws)

    def test_watch_subscribers_sign_list_once(self):
        hub.set_response(raw_response=json.dumps(list_in_topic(list_with_one_user())))
        ws = watch_subscribers_sign([topic])
        wait_for_hub_requests(1)
        assert_ws_list_len(ws.recv_message(), 1)
        wait_for_hub_requests(2)
        assert_no_ws_message(ws)

    def test_watch_subscribers_list_if_changed(self):
        hub.set_response(raw_response=json.dumps(list_in_topic(list_with_one_user())))
        ws = watch_subscribers([topic])
        wait_for_hub_requests(1)
        assert_ws_list_len(ws.recv_message(), 1)

        hub.set_response(raw_response=json.dumps(list_in_topic(list_with_two_users())))
        wait_for_hub_requests(len(hub.requests) + 1)
        assert_ws_list_len(ws.recv_message(), 2)

        hub.set_response(raw_response=json.dumps(list_in_topic(list_with_one_user())))
        time.sleep(1.5*settings.list_interval)
        assert_ws_list_len(ws.recv_message(), 1)

    def test_watch_subscribers_sign_list_if_changed(self):
        hub.set_response(raw_response=json.dumps(list_in_topic(list_with_one_user())))
        ws = watch_subscribers_sign([topic])
        wait_for_hub_requests(1)
        assert_ws_list_len(ws.recv_message(), 1)

        hub.set_response(raw_response=json.dumps(list_in_topic(list_with_two_users())))
        wait_for_hub_requests(len(hub.requests) + 1)
        assert_ws_list_len(ws.recv_message(), 2)

        hub.set_response(raw_response=json.dumps(list_in_topic(list_with_one_user())))
        time.sleep(1.5*settings.list_interval)
        assert_ws_list_len(ws.recv_message(), 1)

    def test_watch_subscribers_list_ignore_too_old(self):
        hub.set_response(raw_response=json.dumps(list_in_topic(list_with_one_user())))
        ws = watch_subscribers([topic])
        wait_for_hub_requests(1)
        assert_ws_list_len(ws.recv_message(), 1)

        list = list_in_topic(list_with_one_user())
        list[0]['init_time'] -= settings.max_age
        hub.set_response(raw_response=json.dumps(list))
        wait_for_hub_requests(len(hub.requests) + 1)
        assert_ws_list_len(ws.recv_message(), 0)

    def test_watch_subscribers_sign_list_ignore_too_old(self):
        hub.set_response(raw_response=json.dumps(list_in_topic(list_with_one_user())))
        ws = watch_subscribers_sign([topic])
        wait_for_hub_requests(1)
        assert_ws_list_len(ws.recv_message(), 1)

        list = list_in_topic(list_with_one_user())
        list[0]['init_time'] -= settings.max_age
        hub.set_response(raw_response=json.dumps(list))
        wait_for_hub_requests(len(hub.requests) + 1)
        assert_ws_list_len(ws.recv_message(), 0)

    def test_watch_subscribers_list_many_topics(self):
        list_two_topics = list_in_topic(list_with_one_user())
        for i in list_in_another_topic(list_with_two_users()):
            list_two_topics.append(i)
        hub.set_response(raw_response=json.dumps(list_two_topics))
        ws = watch_subscribers([topic, another_topic])
        wait_for_hub_requests(1)
        ws_message = ws.recv_message()
        neq_(ws_message, None)

    def test_watch_subscribers_sign_list_many_topics(self):
        list_two_topics = list_in_topic(list_with_one_user())
        for i in list_in_another_topic(list_with_two_users()):
            list_two_topics.append(i)
        hub.set_response(raw_response=json.dumps(list_two_topics))
        ws = watch_subscribers_sign([topic, another_topic])
        wait_for_hub_requests(1)
        ws_message = ws.recv_message()
        neq_(ws_message, None)

    def test_watch_subscribers_forbidden(self):
        ws = watch_subscribers_forbidden_service([topic, another_topic])
        time.sleep(0.1)
        eq_(len(hub.requests), 0)
        assert_ws_forbidden(ws)

    def test_watch_subscribers_sign_forbidden(self):
        ws = watch_subscribers_sign_forbidden_service([topic, another_topic])
        time.sleep(0.1)
        eq_(len(hub.requests), 0)
        assert_ws_forbidden(ws)
