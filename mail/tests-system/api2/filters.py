from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
import time

def setUp(self):
    global xiva, hub
    xiva = XivaApiV2(host='localhost', port=18083)
    hub = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown(self):
    global hub
    hub.fini()

class TestSubscribeWithTags:
    def setup(self):
        hub.response.body = 'abcdef'
        hub.set_request_hook(self.request_hook)
        self.requests = []

    def request_hook(self, req):
        self.requests.append(req)

    def test_subscribe_url_ok(self):
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/url?',
            uid = '123', token = '12345678901234567890',
            service = 'mail:tarA', callback = 'qwerty', session = 'qwerty'), {})
        assert_ok(resp)

    def test_subscribe_delimiters(self):
        hub.response.body = msgpack.packb([-1, 200, '', ''])
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/url?',
            uid = '123', token = '12345678901234567890',
            service = 'mail:tarA_A', callback = 'qwerty', session = 'A'), {})
        assert_ok(resp)

        self.requests = []
        xiva.send_direct(body=json.dumps({"payload": "test"}),
            token = "S001", event = "test", uid = "1", tags = "tagA_A")
        time.sleep(0.05)
        assert_equals(len(self.requests), 1)

        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/url?',
            uid = '123', token = '12345678901234567890',
            service = 'mail:tarA-A', callback = 'qwerty', session = 'B'), {})
        assert_bad_request(resp, 'invalid argument "service"')

        filter = '{"rules":[{"do":"send_bright","if":{"$has_tags":["tagA-A"]}},{"do":"skip"}],"vars":{}}'
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/url?',
            uid = '123', token = '12345678901234567890',
            service = 'mail', callback = 'qwerty', session = 'C',
            filter = filter), {})
        assert_ok(resp)

        self.requests = []
        xiva.send_direct(body=json.dumps({"payload": "test"}),
            token = "S001", event = "test", uid = "1", tags = "tagA-A")
        time.sleep(0.05)
        assert_equals(len(self.requests), 1)

