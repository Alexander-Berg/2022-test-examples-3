from pycommon import *
import BaseHTTPServer
import msgpack
import io
import sys
from time import sleep

def setUp(self):
    global xiva, hub_server
    xiva = XivaApiV2(host='localhost', port=18083)
    hub_server = fake_server(host='localhost', port=17081, raw_response='OK')
    hub_server.shared_data = {}

def tearDown(self):
    hub_server.fini()

def assert_msg_field(raw_msg, field, value):
    msg = msgpack.unpackb(raw_msg)
    assert_equals(msg[field], value)

class TestWildSend:
    def setup(self):
        hub_server.reset_state()
        self.payload = {"payload": "test"}
        hub_server.set_response(raw_response="ok")
        hub_server.set_request_hook(self.request_hook)
        self.requests = []

    def teardown(self):
        pass

    def request_hook(self, req):
        self.requests.append(req)

    def args(self, **kwargs):
        args = {'token': "S001", 'event': 'test'}
        for key in kwargs: args[key] = kwargs[key]
        return args

    def do_send(self, **kwargs):
        return xiva.wild_send(body=json.dumps(self.payload), **self.args(**kwargs))

    def test_stream_send_ok(self):
        response = self.do_send()
        wait(lambda: len(self.requests) == 1, 0.05, 0.01)
        assert_ok(response)
        assert_equals(len(self.requests), 1)
        assert_msg_field(self.requests[0].body, MessageFields.service, 'tst1')

    def test_broadcast_flag_is_set(self):
        self.do_send()
        wait(lambda: len(self.requests) == 1, 0.05, 0.01)
        assert_equals(len(self.requests), 1)
        assert_msg_field(self.requests[0].body, MessageFields.broadcast, True)

    def test_content_type_binary(self):
        self.do_send()
        wait(lambda: len(self.requests) == 1, 0.05, 0.01)
        assert_equals(len(self.requests), 1)
        assert_content_type_equals(self.requests[0], 'application/octet-stream')
