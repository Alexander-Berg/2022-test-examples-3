from pycommon import *
import json
import time

def setUp():
    global xiva, hub
    xiva = XivaApiExt(host='localhost', port=18083)
    hub = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown():
    hub.fini()

class TestBbLogin:
    def setUp(self):
        hub.reset_state()
        hub.set_request_hook(self.check_hub_req)

    def teardown(self):
        time.sleep(0.2)
        error_in_hook = hub.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def test_missing_params(self):
        resp = xiva.bb_login()
        assert_bad_request(resp, 'missing argument "user"')
        resp = xiva.bb_login(user='one')
        assert_bad_request(resp, 'missing argument "yandexuid"')

    def test_missing_auth(self):
        resp = xiva.bb_login(user='xyz', yandexuid='yauid')
        assert_unauthorized(resp)

    def check_list_json(self, req):
        check_caught_request(req, '/list_json',
            uid='yauid',
            service='bass')

    def check_subscribe(self, req):
        check_caught_request(req, '/subscribe',
            uid='xyz',
            service='bass',
            uidservice='xyzbass',
            client='YandexBrowser',
            session_key='yauid',
            ttl='720',
            id='webpush:yauid',
            account='xyz',
            bb_connection_id='',
            priority='high',
            # these go in body
            filter='',
            callback='webpush:%7bsomething%7d',
            extra_data='')

    def check_hub_req(self, req):
        if req.path.startswith('/list_json?'):
            self.check_list_json(req)
        elif req.path.startswith('/subscribe?'):
            self.check_subscribe(req)
        else:
            raise Exception('unexpected req: ' + req.path)

    def test_unauthorized(self):
        resp = xiva.bb_login(token='12345678901234567890', user='xyz', yandexuid='yauid')
        assert_unauthorized(resp, "bad token")

    def test_ok(self):
        hub.response_chain = []
        hub.response_chain.append(Response(code=200, body='''
            [
                {
                    "ack_event_ts" : 0,
                    "ack_local_id" : 0,
                    "ack_time" : 1557450118,
                    "client" : "YandexBrowser",
                    "connection_id" : "",
                    "device" : "",
                    "extra_data" : "",
                    "filter" : "",
                    "id" : "webpush:yauid",
                    "init_local_id" : 0,
                    "init_time" : 1557450118,
                    "next_retry_time" : 0,
                    "platform" : "",
                    "retry_interval" : 0,
                    "service" : "bass",
                    "session_key" : "yauid",
                    "ttl" : 17520,
                    "uid" : "yauid",
                    "url" : "webpush:%7bsomething%7d"
                }
            ]
        '''))
        hub.response_chain.append(Response(code=200, body='new_sub_id'))
        resp = xiva.bb_login(token='bass123456', user='xyz', yandexuid='yauid')
        assert_equal(hub.total_requests, 2)
        assert_ok(resp)

    def test_list_fail(self):
        hub.response_chain = []
        hub.response_chain.append(Response(code=500, body='subscriptions storage failed'))
        resp = xiva.bb_login(token='bass123456', user='xyz', yandexuid='yauid')
        assert_equal(hub.total_requests, 1)
        assert_equal(resp.status, 500)

    def test_empty_list(self):
        hub.response_chain = []
        hub.response_chain.append(Response(code=200, body='[]'))
        resp = xiva.bb_login(token='bass123456', user='xyz', yandexuid='yauid')
        assert_equal(hub.total_requests, 1)
        assert_equal(resp.status, 200)

    def test_multiple_subs(self):
        hub.response_chain = []
        hub.response_chain.append(Response(code=200, body='''
            [
                {
                    "ack_event_ts" : 0,
                    "ack_local_id" : 0,
                    "ack_time" : 1557450118,
                    "client" : "OtherBrowser",
                    "connection_id" : "",
                    "device" : "",
                    "extra_data" : "",
                    "filter" : "",
                    "id" : "webpush:otherid",
                    "init_local_id" : 0,
                    "init_time" : 1557450118,
                    "next_retry_time" : 0,
                    "platform" : "",
                    "retry_interval" : 0,
                    "service" : "bass",
                    "session_key" : "yauid",
                    "ttl" : 17520,
                    "uid" : "yauid",
                    "url" : "webpush:%7bsomething%7d"
                },
                {
                    "ack_event_ts" : 0,
                    "ack_local_id" : 0,
                    "ack_time" : 1557450118,
                    "client" : "YandexBrowser",
                    "connection_id" : "",
                    "device" : "",
                    "extra_data" : "",
                    "filter" : "",
                    "id" : "other",
                    "init_local_id" : 0,
                    "init_time" : 1557450118,
                    "next_retry_time" : 0,
                    "platform" : "",
                    "retry_interval" : 0,
                    "service" : "bass",
                    "session_key" : "yauid",
                    "ttl" : 17520,
                    "uid" : "yauid",
                    "url" : "mob:%7bsomething%7d"
                },
                {
                    "ack_event_ts" : 0,
                    "ack_local_id" : 0,
                    "ack_time" : 1557450118,
                    "client" : "YandexBrowser",
                    "connection_id" : "",
                    "device" : "",
                    "extra_data" : "",
                    "filter" : "",
                    "id" : "webpush:yauid",
                    "init_local_id" : 0,
                    "init_time" : 1557450118,
                    "next_retry_time" : 0,
                    "platform" : "",
                    "retry_interval" : 0,
                    "service" : "bass",
                    "session_key" : "yauid",
                    "ttl" : 17520,
                    "uid" : "yauid",
                    "url" : "webpush:%7bsomething%7d"
                }
            ]
        '''))
        hub.response_chain.append(Response(code=200, body='new_sub_id'))
        resp = xiva.bb_login(token='bass123456', user='xyz', yandexuid='yauid')
        assert_equal(hub.total_requests, 2)
        assert_ok(resp)

    def test_subscribe_fail(self):
        hub.response_chain = []
        hub.response_chain.append(Response(code=200, body='''
            [
                {
                    "ack_event_ts" : 0,
                    "ack_local_id" : 0,
                    "ack_time" : 1557450118,
                    "client" : "YandexBrowser",
                    "connection_id" : "",
                    "device" : "",
                    "extra_data" : "",
                    "filter" : "",
                    "id" : "webpush:yauid",
                    "init_local_id" : 0,
                    "init_time" : 1557450118,
                    "next_retry_time" : 0,
                    "platform" : "",
                    "retry_interval" : 0,
                    "service" : "bass",
                    "session_key" : "yauid",
                    "ttl" : 17520,
                    "uid" : "yauid",
                    "url" : "webpush:%7bsomething%7d"
                }
            ]
        '''))
        hub.response_chain.append(Response(code=500, body='subscriptions storage failed'))
        resp = xiva.bb_login(token='bass123456', user='xyz', yandexuid='yauid')
        assert_equal(hub.total_requests, 2)
        assert_equal(resp.status, 500)

class TestBbLogout:
    def test_missing_params(self):
        resp = xiva.bb_logout()
        assert_bad_request(resp, 'missing argument "user"')
        resp = xiva.bb_logout(user='one')
        assert_bad_request(resp, 'missing argument "yandexuid"')

    def test_missing_auth(self):
        resp = xiva.bb_logout(user='xyz', yandexuid='yauid')
        assert_unauthorized(resp)

    def test_ok(self):
        resp = xiva.bb_logout(token='12345678901234567890', user='xyz', yandexuid='yauid')
        assert_ok(resp)
