from pycommon import *
import msgpack

def setUp(self):
    global xiva, hub_server, fallback_hub_server
    xiva = XivaApiV2(host='localhost', port=18083)
    hub_server = fake_server(host='localhost', port=17081, raw_response='OK')
    fallback_hub_server = fake_server(host='localhost', port=17082, raw_response='OK')

def tearDown(self):
    hub_server.fini()
    fallback_hub_server.fini()

class TestSend:
    def setup(self):
        hub_server.reset_state()
        hub_server.set_response(raw_response=msgpack.packb([[0,200,'OK','id-a'], [0,200,'OK','id-b']]))
        hub_server.set_request_hook(self.request_hook)
        self.requests = []

        fallback_hub_server.set_response(raw_response=msgpack.packb([[0,200,'OK','id-a'], [0,200,'OK','id-b']]))
        fallback_hub_server.set_request_hook(self.fallback_request_hook)
        self.fallback_requests = []

    def teardown(self):
        pass

    def request_hook(self, req):
        self.requests.append(req)

    def fallback_request_hook(self, req):
        self.fallback_requests.append(req)

    def test_200_stoken_compatibility(self):
        response = xiva.POST(xiva.prepare_url('/v1/send?', stoken="S001", event="test", uid="1"),
            {}, json.dumps({"payload": "test"}))
        assert_ok(response)
