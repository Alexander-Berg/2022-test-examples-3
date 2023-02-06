from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
import time

class TestWebsocketFetchHistory:
    @classmethod
    def setup_class(cls):
        cls.xiva = XivaApiV2(host='localhost', port=18083)
        cls.xiva_client = XivaClient(host='localhost', port=18083, back_port=18080)
        cls.hub_server = fake_server(host='localhost', port=17081, raw_response='OK')

    @classmethod
    def teardown_class(cls):
        cls.hub_server.fini()

    def setup(self):
        self._good_args = { 'uid': "200", 'service': "tst1",
            'client': "test", 'session': "ABCD-EFGH", "format": "json"
        }
        self._sign_uid_service = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "service" : "tst1" })

    def teardown(self):
        error_in_hook = self.hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_fetch_history(self):
        self.hub_server.set_response(raw_response="test-sub-id", headers=
            {"X-Xiva-Position": "8", "X-Xiva-Count": "1"})

        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200",
            service="tst1", fetch_history="200:tst1:8:1",
            **self._sign_uid_service))

        time.sleep(0.2)

        ws_message = ws.recv_message() # skip ping
        ws_message = ws.recv_message() # skip subscribed
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        data = json.loads(ws_message.payload)
        assert_equals(data["position"], 8)
        assert_equals(data["count"], 1)

    def test_try_fetch_history(self):
        self.hub_server.set_response(raw_response="test-sub-id", headers=
            {"X-Xiva-Position": "9", "X-Xiva-Count": "0"})

        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200",
            service="tst1", try_fetch_history="200:tst1:5:3",
            **self._sign_uid_service))

        time.sleep(0.2)

        ws_message = ws.recv_message() # skip ping
        ws_message = ws.recv_message() # skip subscribed
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        data = json.loads(ws_message.payload)
        assert_equals(data["position"], 9)
        assert_equals(data["count"], 0)

    def test_400_if_both_history_parameters(self):
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200",
            service="tst1", fetch_history="200:tst1:5:3",
            try_fetch_history="200:tst1:5:3",
            **self._sign_uid_service))

        time.sleep(0.1)
        assert_ws_bad_request(resp, ws)

    def test_fetch_history_for_topic(self):
        self.hub_server.set_response(raw_response="test-sub-id", headers=
            {"X-Xiva-Position": "8", "X-Xiva-Count": "1"})

        sign_uid_topic_service = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "topic": "tst-topic", "service" : "tst1" })
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200",
            topic="tst-topic", service="tst1", fetch_history="tst-topic:tst1:8:1",
            **sign_uid_topic_service))

        time.sleep(0.2)

        ws_message = ws.recv_message() # skip ping
        ws_message = ws.recv_message() # skip subscribed
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        data = json.loads(ws_message.payload)
        assert_equals(data["position"], 8)
        assert_equals(data["count"], 1)

    def test_try_fetch_history_for_topic(self):
        self.hub_server.set_response(raw_response="test-sub-id", headers=
            {"X-Xiva-Position": "9", "X-Xiva-Count": "0"})

        sign_uid_topic_service = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "topic": "tst-topic", "service" : "tst1" })
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(uid="200",
            topic="tst-topic", service="tst1", try_fetch_history="tst-topic:tst1:5:3",
            **sign_uid_topic_service))

        time.sleep(0.2)

        ws_message = ws.recv_message() # skip ping
        ws_message = ws.recv_message() # skip subscribed
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        data = json.loads(ws_message.payload)
        assert_equals(data["position"], 9)
        assert_equals(data["count"], 0)
