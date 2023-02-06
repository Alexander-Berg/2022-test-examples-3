from pycommon import *
import msgpack
import time

class TestSubscribeWebsocketRProxy:
    @classmethod
    def setup_class(cls):
        cls.xiva = XivaApiV2(host='localhost', port=18083)
        cls.xiva_client = XivaClient(host='localhost', port=18083, back_port=18080)
        cls.hub_server = fake_server(host='localhost', port=17081, raw_response='OK')
        cls.rproxy_backend_server = fake_server(host='localhost', port=17086, raw_response='OK')
        cls.rproxy_backend_server2 = fake_server(host='localhost', port=17087, raw_response='OK')

    @classmethod
    def teardown_class(cls):
        cls.hub_server.fini()
        cls.rproxy_backend_server.fini()
        cls.rproxy_backend_server2.fini()

    def wait(self, ws):
        ws.poll(2)

    def setup(self):
        self._good_args = { 'uid': "200", 'service': "tst1",
            'client': "test", 'session': "ABCD-EFGH", "format": "json"
        }
        self._sign_uid_service = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "service" : "tst1" })
        self._sign_uid_two_services = self.xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "service" : "tst1,tst2" })
        self._sign_rate_limit = self.xiva_client.secret_sign(
            **{ "token" : "RProxyRateLimit", "uid" : "200", "service" : "tests-system-rproxy-rate-limit-no-recovery,tests-system-rproxy-rate-limit-fast-recovery" })
        self.rproxy_backend_server.set_response_code(200)
        self.rproxy_backend_server.set_request_hook(self.request_hook)
        self.rproxy_backend_server.requests = []
        self.rproxy_backend_server2.set_response_code(200)

    def request_hook(self, req):
        self.rproxy_backend_server.requests.append(req)

    def teardown(self):
        error_in_hook = self.hub_server.set_request_hook(None)
        if error_in_hook: raise error_in_hook

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_rproxy_returns_binary_responses(self):
        reqid = 22334455
        rawdata = 'AABBCCDDEEFF'
        binary_payload = '\x01' + msgpack.packb([0, reqid]) + rawdata
        self.rproxy_backend_server.set_response(raw_response = binary_payload)
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_equals(resp.code, 101)
        self.wait(ws)
        ws_message = ws.recv_message() # ping
        self.wait(ws)
        ws_message = ws.recv_message() # subscribed
        ws.send_binary_message(binary_payload)
        self.wait(ws)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.opcode, 2)
        assert_equals(ws_message.payload, binary_payload)

    def test_rproxy_calls_url_with_specified_path(self):
        reqid = 22334455
        rawdata = 'AABBCCDDEEFF'
        binary_payload = '\x01' + msgpack.packb([0, reqid, "path_to_append"]) + rawdata
        self.rproxy_backend_server.set_response(raw_response = binary_payload)
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_equals(resp.code, 101)
        self.wait(ws)
        ws_message = ws.recv_message() # ping
        self.wait(ws)
        ws_message = ws.recv_message() # subscribed
        ws.send_binary_message(binary_payload)
        self.wait(ws)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)

        assert_equals(len(self.rproxy_backend_server.requests), 1)
        assert_equals(self.rproxy_backend_server.requests[0].path, "/path_to_append")

    def test_rproxy_returns_error_for_invalid_service(self):
        reqid = 22334455
        rawdata = 'AABBCCDDEEFF'
        binary_payload = '\x01' + msgpack.packb([1, reqid]) + rawdata
        self.rproxy_backend_server.set_response(raw_response = binary_payload)
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_equals(resp.code, 101)
        self.wait(ws)
        ws_message = ws.recv_message() # ping
        self.wait(ws)
        ws_message = ws.recv_message() # subscribed
        ws.send_binary_message(binary_payload)
        self.wait(ws)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.opcode, 2)
        assert_equals(ws_message.payload[0], '\x02')
        assert_equals(ws_message.payload[1:], msgpack.packb([0, 5]))

    def test_rproxy_with_two_services_returns_binary_responses_from_first(self):
        reqid = 22334455
        rawdata = 'AABBCCDDEEFF'
        binary_payload = '\x01' + msgpack.packb([0, reqid]) + rawdata
        self.rproxy_backend_server.set_response(raw_response = 'BINARY1')
        self.rproxy_backend_server2.set_response(raw_response = 'BINARY2')
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(service="tst1,tst2", **self._sign_uid_two_services))
        assert_equals(resp.code, 101)
        self.wait(ws)
        ws_message = ws.recv_message() # ping
        self.wait(ws)
        ws_message = ws.recv_message() # subscribed 1st
        ws_message = ws.recv_message() # subscribed 2nd
        print('**', ws_message.payload)
        ws.send_binary_message(binary_payload)
        self.wait(ws)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.opcode, 2)
        assert_equals(ws_message.payload, 'BINARY1')

    def test_rproxy_with_two_services_returns_binary_responses_from_second(self):
        reqid = 22334455
        rawdata = 'AABBCCDDEEFF'
        binary_payload = '\x01' + msgpack.packb([1, reqid]) + rawdata
        self.rproxy_backend_server.set_response(raw_response = 'BINARY1')
        self.rproxy_backend_server2.set_response(raw_response = 'BINARY2')
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(service="tst1,tst2", **self._sign_uid_two_services))
        assert_equals(resp.code, 101)
        self.wait(ws)
        ws_message = ws.recv_message()
        self.wait(ws)
        ws_message = ws.recv_message() # subscribed 1st
        ws_message = ws.recv_message() # subscribed 2nd
        ws.send_binary_message(binary_payload)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.opcode, 2)
        assert_equals(ws_message.payload, 'BINARY2')

    def test_rproxy_with_two_unordered_services_returns_binary_responses_from_first(self):
        reqid = 22334455
        rawdata = 'AABBCCDDEEFF'
        binary_payload = '\x01' + msgpack.packb([1, reqid]) + rawdata
        self.rproxy_backend_server.set_response(raw_response = 'BINARY1')
        self.rproxy_backend_server2.set_response(raw_response = 'BINARY2')
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(service="tst2,tst1", **self._sign_uid_two_services))
        assert_equals(resp.code, 101)
        self.wait(ws)
        ws_message = ws.recv_message()
        self.wait(ws)
        ws_message = ws.recv_message() # subscribed 1st
        ws_message = ws.recv_message() # subscribed 2nd
        ws.send_binary_message(binary_payload)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.opcode, 2)
        assert_equals(ws_message.payload, 'BINARY1')

    def test_rproxy_to_broken_backend_returns_special_response(self):
        reqid = 22334455
        rawdata = 'AABBCCDDEEFF'
        binary_payload = '\x01' + msgpack.packb([0, reqid]) + rawdata
        self.rproxy_backend_server.set_response_code(500)
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_equals(resp.code, 101)
        self.wait(ws)
        ws_message = ws.recv_message() # ping
        self.wait(ws)
        ws_message = ws.recv_message() # subscribed
        ws.send_binary_message(binary_payload)
        self.wait(ws)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.opcode, 2)
        assert_equals(ws_message.payload[0], '\x02')
        assert_equals(ws_message.payload[1:], msgpack.packb([reqid, 2]))

    def test_rproxy_on_bad_message_returns_special_response(self):
        binary_payload = "000000"
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(**self._sign_uid_service))
        assert_equals(resp.code, 101)
        self.wait(ws)
        ws_message = ws.recv_message() # ping
        self.wait(ws)
        ws_message = ws.recv_message() # subscribed
        ws.send_binary_message(binary_payload)
        self.wait(ws)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.opcode, 2)
        assert_equals(ws_message.payload[1:], msgpack.packb([0, 1]))

    def test_rproxy_rate_limit(self):
        service_unavailable = 6
        reqid = 22334455
        rawdata = 'AABBCCDDEEFF'
        binary_payload = '\x01' + msgpack.packb([0, reqid]) + rawdata
        rate_limit_payload = '\x02' + msgpack.packb([reqid, service_unavailable])
        self.rproxy_backend_server.set_response(raw_response = binary_payload)
        (resp, ws) = self.xiva.subscribe_websocket(
            **self.args(service="tests-system-rproxy-rate-limit-no-recovery,tests-system-rproxy-rate-limit-fast-recovery", **self._sign_rate_limit))
        assert_equals(resp.code, 101)
        self.wait(ws)
        ws_message = ws.recv_message() # ping
        self.wait(ws)
        ws_message = ws.recv_message() # subscribed
        # first request should finish successfully
        ws.send_binary_message(binary_payload)
        self.wait(ws)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.opcode, 2)
        assert_equals(ws_message.payload, binary_payload)
        # second should be ratelimited
        ws.send_binary_message(binary_payload)
        self.wait(ws)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.opcode, 2)
        assert_equals(ws_message.payload, rate_limit_payload)

    def test_rproxy_rate_limit_recovery(self):
        rate_limit_recovery_interval = 0.001 # 1ms
        reqid = 22334455
        rawdata = 'AABBCCDDEEFF'
        binary_payload = '\x01' + msgpack.packb([1, reqid]) + rawdata
        self.rproxy_backend_server.set_response(raw_response = binary_payload)
        (resp, ws) = self.xiva.subscribe_websocket(
            **self.args(service="tests-system-rproxy-rate-limit-no-recovery,tests-system-rproxy-rate-limit-fast-recovery", **self._sign_rate_limit))
        assert_equals(resp.code, 101)
        self.wait(ws)
        ws_message = ws.recv_message() # ping
        self.wait(ws)
        ws_message = ws.recv_message() # subscribed
        # first request should finish successfully
        ws.send_binary_message(binary_payload)
        self.wait(ws)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.opcode, 2)
        assert_equals(ws_message.payload, binary_payload)
        # second request should finish successfully after delay
        sleep(rate_limit_recovery_interval)
        ws.send_binary_message(binary_payload)
        self.wait(ws)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.opcode, 2)
        assert_equals(ws_message.payload, binary_payload)


class TestSubscribeWebsocketRProxyUnavailable:
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

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def test_rproxy_to_unavailable_backend_returns_special_response(self):
        reqid = 22334455
        rawdata = 'AABBCCDDEEFF'
        binary_payload = '\x01' + msgpack.packb([0, reqid]) + rawdata
        (resp, ws) = self.xiva.subscribe_websocket(**self.args(service="tst1", **self._sign_uid_service))
        assert_equals(resp.code, 101)
        ws.poll(2)
        ws_message = ws.recv_message() # ping
        ws_message = ws.recv_message() # subscribed
        ws.send_binary_message(binary_payload)
        ws.poll(2)
        ws_message = ws.recv_message()
        assert_not_equals(ws_message, None)
        assert_equals(ws_message.opcode, 2)
        assert_equals(ws_message.payload[1:], msgpack.packb([reqid, 2]))