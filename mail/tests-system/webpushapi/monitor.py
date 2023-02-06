from pycommon import *
import json
import time
import base64

def setUp(self):
    global xiva, xiva_back, hub_server
    xiva = XivaApiV2(host='localhost', port=18085, secure=True)
    xiva_back = XivaApiBack(host='localhost', port=18080)
    hub_server = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown(self):
    global hub_server
    hub_server.fini()

class TestNotify:
    def setup(self):
        hub_server.set_request_hook(self.request_hook)
        resp = xiva.GET("/v2/vapid_key", {})
        self.public_key = resp.body
        self.uuid = "test_instance_id"
        self.webpushapi = XivaWebPushApi(host='localhost', port=18085, secure=True)
        self.webpushapi.connect()
        self.requests = []

    def decode_uid(self, sub):
        ret = sub[::-1]
        missing_padding = len(ret) % 4
        if missing_padding > 0:
            ret += b'='* (4 - missing_padding)
        return base64.b64decode(ret)

    def request_hook(self, req):
        self.requests.append(req.path.split('?')[0])

    def test_list_catalogue(self):
        ws = self.webpushapi.ws
        hub_server.set_response(raw_response="[]")

        resp = self.webpushapi.subscribe()
        subset = resp["subscription_set"]
        push_resource = resp["push_resource"]
        sub = resp["subscription"]

        ws.send_message(json.dumps({"method": "/webpushapi/monitor", "params": {'subscription_set':subset}}))

        time.sleep(0.1)

        resp = xiva_back.GET('/webpushapi/list_catalogue', {})
        assert_ok(resp)
        list = json.loads(resp.body)
        assert_equals(len(list), 1)
        assert_equals(list[0], self.decode_uid(sub).split('+')[0])


    def test_notify(self):
        ws = self.webpushapi.ws
        hub_server.set_response(raw_response="[]")

        resp = self.webpushapi.subscribe()
        subset = resp["subscription_set"]
        sub = resp["subscription"]

        ws.send_message(json.dumps({"method": "/webpushapi/monitor", "params": {'subscription_set':subset}}))

        notification = [self.decode_uid(sub), '', 'webpushapi', '', '', '', {}, b'datadatadata', True, 'transit', 3, 1470054101, []]
        xiva_back.POST("/webpushapi/notify", {}, msgpack.packb(notification))

        ws_message = ws.recv_message()
        assert_not_equal(ws_message, None)
        msg = json.loads(ws_message.payload)
        assert_in('params', msg)
        assert_in('data', msg['params'])
        assert_in('payload', msg['params']['data'])
        payload = base64.b64decode(msg['params']['data']['payload'])
        assert_equals('datadatadata', payload)
        assert_in('id', msg)
        assert_not_equals(msg['id'], None)
