from pycommon import *
import json

class TestSubscribe:
    @classmethod
    def setup_class(cls):
        cls.xiva = XivaApiV2(host='localhost', port=18085, secure=True)
        cls.hub_server = fake_server(host='localhost', port=17081, raw_response='OK')

    @classmethod
    def teardown_class(cls):
        cls.hub_server.fini()

    def setup(self):
        resp = self.xiva.GET("/v2/vapid_key", {})
        self.public_key = resp.body
        self.uuid = "test_instance_id"
        self.webpushapi = XivaWebPushApi(host='localhost', port=18085, secure=True)
        self.webpushapi.connect()
        self.requests = []
        self.hub_server.set_request_hook(self.request_hook)

    def teardown(self):
        pass

    def request_hook(self, req):
        self.requests.append(req.path.split('?')[0])

    def test_subscribe_negative(self):
        ws = self.webpushapi.ws
        params = [
            {},
            {"public_key": self.public_key},
            {"uuid": self.uuid},
            {"public_key": "", "uuid": self.uuid},
            {"public_key": "", "uuid": self.uuid, "subscription_set":'a'*200},
            {"public_key": 'a'*513, "uuid": self.uuid, "subscription_set":'a'*200},
        ]
        responses = [
            {"error": 400, "id": None, "reason": "missing argument \"public_key\""},
            {"error": 400, "id": None, "reason": "missing argument \"uuid\""},
            {"error": 400, "id": None, "reason": "missing argument \"public_key\""},
            {"error": 400, "id": None, "reason": "missing argument \"public_key\""},
            {"error": 400, "id": None, "reason": "missing argument \"public_key\""},
            {"error": 400, "id": None, "reason": "public_key length limit exceeded"},
        ]
        self.hub_server.set_response(raw_response="[]")

        for i in range(0,len(params)):
            ws.send_message(json.dumps({"method": "/webpushapi/subscribe", "params": params[i]}))
            assert_ws_message_json(ws.recv_message(), 0, responses[i])

    def test_unsubscribe_negative(self):
        ws = self.webpushapi.ws
        params = [
            {},
            {"subscription": "5Va3R6XIDqpL8crYOswn1CSNfo"},
            {"subscription": "IWMkFjZ5IzNjBDNh1yM4EjYtUmZzQTLyAjMy0SN2ADNwATY3siZ2QjYjNDO4YDNzQWLiBTY50CNiJDNtQWY3QTL3IWNlJWZjdjLx82SaxEMxgHcmZVc"},
        ]
        responses = [
            {"error": 400, "id": None, "reason": "missing argument \"subscription\""},
            {"error": 400, "id": None, "reason": "invalid subscription"},
            {"error": None, "id": None, "result": ""},
        ]
        self.hub_server.set_response(raw_response="[]")

        for i in range(0,len(params)):
            ws.send_message(json.dumps({"method": "/webpushapi/unsubscribe", "params": params[i]}))
            assert_ws_message_json(ws.recv_message(), 0, responses[i])

    def test_subscribe(self):
        self.hub_server.set_response(raw_response="[]")

        resp = self.webpushapi.subscribe()
        assert_greater(resp["ttl"], 0)
        assert_not_equal(resp["subscription_set"], None)
        assert_not_equal(resp["push_resource"], None)
        assert_not_equal(resp["subscription"], None)

        subset = resp["subscription_set"]
        push_resource = resp["push_resource"]
        sub = resp["subscription"]

        resp = self.webpushapi.subscribe(subscription_set = subset)
        assert_greater(resp["ttl"], 0)
        assert_equal(resp["subscription_set"], subset)
        assert_not_equal(resp["push_resource"], push_resource)
        assert_not_equal(resp["subscription"], sub)

        resp = self.webpushapi.subscribe()
        assert_not_equal(resp["subscription_set"], subset)
        assert_not_equal(resp["push_resource"], push_resource)
        assert_not_equal(resp["subscription"], sub)

    def test_subscribe_uidset_oversize(self):
        self.hub_server.set_response(raw_response="[]")

        resp = self.webpushapi.subscribe()
        assert_greater(resp["ttl"], 0)
        assert_not_equal(resp["subscription_set"], None)
        assert_not_equal(resp["push_resource"], None)
        assert_not_equal(resp["subscription"], None)

        subset = resp["subscription_set"]
        push_resource = resp["push_resource"]
        sub = resp["subscription"]

        fake_list = ("[" + "{},"*79)[:-1] + "]"
        self.hub_server.set_response(raw_response=fake_list);
        resp = self.webpushapi.subscribe(subscription_set = subset)
        assert_equal(resp["subscription_set"], subset)

        fake_list = ("[" + "{},"*80)[:-1] + "]"
        self.hub_server.set_response(raw_response=fake_list);
        resp = self.webpushapi.subscribe(subscription_set = subset)
        assert_not_equal(resp["subscription_set"], subset)

    def test_unsubscribe(self):
        self.hub_server.set_response(raw_response="[]")
        resp = self.webpushapi.subscribe()
        subscription = resp["subscription"]
        self.requests = []
        resp = self.webpushapi.unsubscribe(subscription=subscription)
        eq_(self.requests[-1], "/uidset/unsubscribe")
