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

def tearDown(self):
    hub_server.fini()

class TestSend:
    def setup(self):
        hub_server.reset_state()
        self.payload = {"payload": "test"}
        hub_server.set_request_hook(self.request_hook)
        self.requests = []

    def teardown(self):
        pass

    def request_hook(self, req):
        self.requests.append(req)

    def args(self, **kwargs):
        args = {'token': "S003", 'event': 'test', "uid": "1"}
        for key in kwargs: args[key] = kwargs[key]
        return args

    def do_send(self, **kwargs):
        return xiva.send(body=json.dumps(self.payload), **self.args(**kwargs))

    def do_direct_send(self, **kwargs):
        return xiva.send_direct(body=json.dumps(self.payload), **self.args(**kwargs))

    def test_200_for_services_second_active_token(self):
        response = self.do_send()
        sleep(0.05)
        assert_ok(response)
        assert_equals(len(self.requests), 1)
