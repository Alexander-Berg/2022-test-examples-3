from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
from pycommon.wait_condition import *
import time
import json

def assert_attached_message(ws_message):
    assert_not_equal(ws_message, None)
    eq_(ws_message.status_code, 0)
    payload = json.loads(ws_message.payload)
    assert_json(payload, {'error': None}, ['id', 'result'])
    assert_json(payload['result'], {'operation': 'attached'}, ['subscription_token'])

def assert_detached_message(ws_message):
    msg = {
        "error": None,
        "id": None,
        "result": { "operation": "detached" }
    }
    assert_ws_message_json(ws_message, 0, msg)

def assert_json(json_value, fields=None, contains_fields=None):
    if fields:
        for k, v in fields.items(): eq_(json_value[k], v)
    if contains_fields:
        for field in contains_fields: assert_in(field, json_value)

class TestConnectWebsocketApi:
    def test_insecure_connect_fail(self):
        client = HTTPRaw(host='localhost', port=18083, secure=False)
        (resp, ws) = client.WS('/websocketapi')
        assert_equals(resp.status, 101)
        msg = ws.recv_message()
        assert_not_equals(msg, None)
        assert_equals(msg.opcode, 8)
        assert_equals(msg.status_code, 4403)
        assert_equals(msg.payload, 'insecure connection')

    def test_secure_connect_ok(self):
        client = HTTPRaw(host='localhost', port=18085, secure=True)
        (resp, ws) = client.WS('/websocketapi')
        assert_equals(resp.status, 101)
        msg = ws.recv_message()
        assert_equals(msg, None)

class TestWebsocketApi:
    @classmethod
    def setup_class(cls):
        cls.raw = HTTPRaw(host='localhost', port=18085, secure=True)
        cls.xiva_client = XivaClient(host='localhost', port=18083, back_port=18080)
        cls.hub_server = fake_server(host='localhost', port=17081, raw_response='OK')
        cls.wmi_server = fake_server(host='localhost', port=17084, raw_response='OK')

    @classmethod
    def teardown_class(cls):
        cls.hub_server.fini()
        cls.wmi_server.fini()

    def setup(self):
        self._good_args = { 'uid': "200", 'service': "tst1",
            'client': "test", 'session': "ABCD-EFGH", "format": "json"
        }
        self._sign_uid_service = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "service" : "tst1" })
        self._sign_uid_service_topic = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "service" : "tst1", "topic": "topic001" })
        self.hub_server.requests = []
        self.hub_server.set_request_hook(lambda req: self.hub_server.requests.append(req))
        self.ws = None
        self.uid_service_topic = ("200", "tst1", "topic001")

    def teardown(self):
        self.hub_server.set_response_code(code=200, reason="OK")
        # Close websocket and wait to unsubscribe
        if self.ws:
            self.hub_server.requests = []
            self.ws = None
            wait(lambda: len(self.hub_server.requests) == 1, 1)

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def connect(self, headers={}):
        (resp, ws) = self.raw.WS('/websocketapi', headers)
        eq_(resp.code, 101)
        ws_message = ws.recv_message()
        assert_equal(ws_message, None)
        return ws

    def auth_params(self, to_topic=False, cookie_auth=False):
        if not cookie_auth:
            if to_topic:
                return self._sign_uid_service_topic
            else:
                return self._sign_uid_service
        return {}

    def subscribe(self, to_topic=False, cookie_auth=False, optional_params={}):
        params = self.args(**self.auth_params(to_topic, cookie_auth))
        if to_topic:
            _, _, topic = self.uid_service_topic
            params["topic"] = topic
        params.update(optional_params)
        args = { "method": "/subscribe", "params": params }
        self.ws.send_message(json.dumps(args))
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_attached_message(msg)
        payload = json.loads(msg.payload)
        sub_token = payload['result']['subscription_token']
        assert_not_equals(sub_token, None)
        return sub_token

    def unsubscribe(self, sub_token, to_topic=False, cookie_auth=False):
        uid, service, topic = self.uid_service_topic
        params = { "subscription_token": sub_token, "uid": uid, "service": service}
        params.update(self.auth_params(to_topic, cookie_auth))
        if to_topic:
            params["topic"] = topic
        args = { "method": "/unsubscribe", "params": params }
        self.ws.send_message(json.dumps(args))
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_detached_message(msg)

    def notify(self, service, uid):
        notification = [
            bytes(uid), b'', bytes(service), b'', b'', b'', {b'\rmethod_id': b'', b'\rsz': b'1825'},
            b'1234567890', True, b'fLJU600KTa61', 3, 1470054101, []
        ]
        self.xiva_client.notify(service, notification)

    def notify_topic(self, service, topic):
        self.notify(service, 'topic:' + topic)

    def test_404(self):
        self.ws = self.connect()
        self.ws.send_message('{"method": "unknown", "params": {"a":"b"}}')
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_ws_message_json(msg, 0, '{"error":404,"id":null,"reason":"The requested URL was not found"}')

    def test_400(self):
        self.ws = self.connect()
        self.ws.send_message('{"method": "/subscribe", "params": {}}')
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_ws_message_json(msg, 0, '{"error":400,"id":null,"reason":"missing argument \\"user (uid)\\""}')

    def test_401(self):
        self.ws = self.connect()
        args = { "method": "/subscribe", "params": self.args() }
        self.ws.send_message(json.dumps(args))
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_ws_message_json(msg, 0, '{"error":401,"id":null,"reason":""}')

    def test_subscribe_by_sign(self):
        self.ws = self.connect()
        args = { "method": "/subscribe", "params": self.args(**self._sign_uid_service) }
        self.ws.send_message(json.dumps(args))
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_attached_message(msg)

    def test_subscribe_by_cookies(self):
        self.wmi_server.set_response(raw_response='{"check_cookies":{"uid":"200"}}')
        self.ws = self.connect(headers={"cookie" : "123", "Origin" : "push.yandex.ru"})
        args = { "method": "/subscribe", "params": self.args() }
        self.ws.send_message(json.dumps(args))
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_attached_message(msg)

    def test_subscribe_by_cookies_with_session_as_id(self):
        self.wmi_server.set_response(raw_response='{"check_cookies":{"uid":"200"}}')
        self.ws = self.connect(headers={"cookie" : "123", "Origin" : "push.yandex.ru"})
        args = {
            "method": "/subscribe",
            "params": {
                "uid": "200",
                "service": "tests-system-session-as-ws-subscription-id",
                "client": "test",
                "session": "SHOULD-BECOME-ID",
                "format": "json"
            }
        }
        self.ws.send_message(json.dumps(args))
        wait(lambda: len(self.hub_server.requests) == 1, 2)
        assert_in('/subscribe', self.hub_server.requests[0].path)
        assert_in('id=SHOULD-BECOME-ID', self.hub_server.requests[0].path)

    def test_subscribe_bad_sign_with_good_cookies_gives_401(self):
        self.wmi_server.set_response(raw_response='{"check_cookies":{"uid":"200"}}')
        self.ws = self.connect(headers={"cookie" : "123", "Origin" : "push.yandex.ru"})
        args = { "method": "/subscribe", "params": self.args(sign="12345", ts="12345") }
        self.ws.send_message(json.dumps(args))
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_ws_message_json(msg, 0, '{"error":401,"id":null,"reason":"bad sign"}')

    def test_subscribe_and_notify(self):
        uid, service, _ = self.uid_service_topic
        self.ws = self.connect()
        self.subscribe()
        self.ws.recv_message() # skip subscribed
        self.notify(service, uid)
        msg = self.ws.recv_message()
        assert_not_equals(msg, None)
        payload = json.loads(msg.payload)
        assert_equals(payload['method'], '/push')
        assert_equals(payload['params']['data']['uid'], '200')
        assert_equals(payload['params']['data']['service'], 'tst1')
        assert_equals(payload['params']['data']['operation'], '')
        assert_not_in('id', payload)

    def test_unsubscribe(self):
        self.ws = self.connect()
        sub_token = self.subscribe()
        self.ws.recv_message() # skip subscribed
        self.unsubscribe(sub_token)
        time.sleep(0.1)
        assert(len(self.hub_server.requests) > 0)
        assert_in('unsubscribe', self.hub_server.requests[-1].path)

    def test_unsubscribe_400(self):
        self.ws = self.connect()
        params = self._sign_uid_service
        params.update({ 'uid': '200', 'service': 'tst1'})
        args = { "method": "/unsubscribe", "params": params }
        self.ws.send_message(json.dumps(args))
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_ws_message_json(msg, 0, '{"error":400,"id":null,"reason":"missing argument \\"subscription_token\\""}')

    def test_unsubscribe_subscription_not_found(self):
        self.ws = self.connect()
        params = self._sign_uid_service
        params.update({ 'subscription_token': 'INVALID_TOKEN', 'uid': '200', 'service': 'tst1'})
        args = { "method": "/unsubscribe", "params": params, "id": 1 }
        self.ws.send_message(json.dumps(args))
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_ws_message_json(msg, 0, '{"error":205,"id":1,"reason":"no subscriptions"}')

    def test_unsubscribe_and_notify(self):
        uid, service, _ = self.uid_service_topic
        self.ws = self.connect()
        sub_token = self.subscribe()
        self.ws.recv_message() # skip subscribed
        self.unsubscribe(sub_token)
        self.notify(service, uid)
        msg = self.ws.recv_message()
        assert_equals(msg, None)

    def test_unsubscribe_some_and_notify(self):
        uid, service, token = self.uid_service_topic
        self.ws = self.connect()
        sub_token1 = self.subscribe()
        self.ws.recv_message() # skip subscribed
        self.subscribe()
        self.ws.recv_message() # skip subscribed
        self.unsubscribe(sub_token1)
        self.notify(service, uid)
        msg = self.ws.recv_message()
        assert_not_equals(msg, None)
        payload = json.loads(msg.payload)
        assert_equals(payload['method'], '/push')
        assert_equals(payload['params']['data']['uid'], uid)
        assert_equals(payload['params']['data']['service'], service)
        assert_equals(payload['params']['data']['operation'], '')
        msg = self.ws.recv_message()
        assert_equals(msg, None)

    def test_unsubscribe_401(self):
        self.ws = self.connect()
        params = { 'subscription_token': 'xxx', 'uid': '200', 'service': 'tst1'}
        args = { "method": "/unsubscribe", "params": params }
        self.ws.send_message(json.dumps(args))
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_ws_message_json(msg, 0, '{"error":401,"id":null,"reason":"this origin domain is not allowed"}')

    def test_unsubscribe_bad_sign(self):
        self.ws = self.connect()
        params = { 'subscription_token': 'xxx', 'uid': '200', 'service': 'tst1', "ts":str(int(time.time())), "sign": "INVALID_SIGN",}
        args = { "method": "/unsubscribe", "params": params }
        self.ws.send_message(json.dumps(args))
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_ws_message_json(msg, 0, '{"error":401,"id":null,"reason":"bad sign"}')

    def test_subscribe_topic(self):
        _, service, topic = self.uid_service_topic
        self.ws = self.connect()
        self.subscribe(to_topic=True)
        self.ws.recv_message() # skip subscribed
        self.notify_topic(service, topic)
        msg = self.ws.recv_message()
        assert_not_equals(msg, None)
        payload = json.loads(msg.payload)
        assert_equals(payload['method'], '/push')
        assert_equals(payload['params']['data']['topic'], topic)
        assert_equals(payload['params']['data']['service'], service)

    def test_unsubscribe_topic(self):
        self.ws = self.connect()
        sub_token = self.subscribe(to_topic=True)
        self.ws.recv_message() # skip subscribed
        self.unsubscribe(sub_token, to_topic=True)
        assert(len(self.hub_server.requests) > 0)
        assert_in('unsubscribe', self.hub_server.requests[-1].path)

    def test_unsubscribe_topic_by_cookies(self):
        self.wmi_server.set_response(raw_response='{"check_cookies":{"uid":"200"}}')
        self.ws = self.connect(headers={"cookie" : "123", "Origin" : "push.yandex.ru"})
        sub_token = self.subscribe(to_topic=True, cookie_auth=True)
        self.ws.recv_message() # skip subscribed
        self.unsubscribe(sub_token, to_topic=True, cookie_auth=True)
        assert(len(self.hub_server.requests) > 0)
        assert_in('unsubscribe', self.hub_server.requests[-1].path)

    def test_fetch_history(self):
        self.hub_server.set_response(raw_response="test-sub-id", headers=
            {"X-Xiva-Position": "8", "X-Xiva-Count": "1"})
        self.ws = self.connect()
        self.subscribe(optional_params={"fetch_history": "200:tst1:8:1"})
        self.ws.recv_message() # skip subscribed
        ws_message = self.ws.recv_message()
        assert_not_equals(ws_message, None)
        payload = json.loads(ws_message.payload)
        assert_equals(payload["params"]["data"]["position"], 8)
        assert_equals(payload["params"]["data"]["count"], 1)
        assert_not_in('id', payload)

    def test_notify_connected(self):
        uid, service, _ = self.uid_service_topic
        self.ws = self.connect()
        self.subscribe()
        ws_message = self.ws.recv_message()
        assert_not_equals(ws_message, None)
        payload = json.loads(ws_message.payload)
        assert_equals(payload["params"]["data"]["uid"], uid)
        assert_equals(payload["params"]["data"]["service"], service)
        assert_equals(payload["params"]["data"]["operation"], "subscribed")
        assert_equals(payload["params"]["data"]["event"], "subscribed")
        assert_not_in('id', payload)

    def test_send_disconnected_on_subscribe_unretriable_error(self):
        self.hub_server.set_response_code(code=403, reason="forbidden")
        self.ws = self.connect()
        sub_token = self.subscribe()
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_not_equals(msg, None)
        payload = json.loads(msg.payload)
        assert_equals(payload['method'], '/push')
        assert_equals(payload['params']['data']['operation'], 'disconnected')
        assert_equals(payload['params']['subscription_token'], sub_token)
        time.sleep(0.1)
        msg = self.ws.recv_message()
        assert_equals(msg, None)
