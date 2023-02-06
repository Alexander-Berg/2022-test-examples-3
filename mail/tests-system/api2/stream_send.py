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

class TestStreamSend:
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
        args = {'token': "abcdef", 'event': 'test'}
        for key in kwargs: args[key] = kwargs[key]
        return args

    def do_send(self, **kwargs):
        return xiva.stream_send(body=json.dumps(self.payload), **self.args(**kwargs))

    def test_stream_send_ok(self):
        response = self.do_send();
        sleep(0.05)
        assert_ok(response)
        assert_equals(len(self.requests), 1)
        assert_equals(msgpack.unpackb(self.requests[0].body)[2], 'tst_stream')

    def test_stream_send_uids_ok(self):
        good_uids = []
        for n in xrange(0, 10):
            good_uids.append('stream_' + str(n))
        for nreq in xrange(200):
            response = self.do_send(uid='123'); # must be ignored
            assert_ok(response)
        sleep(0.05)
        assert_equals(len(self.requests), 200)
        uids = {}
        for req in self.requests:
            uid = msgpack.unpackb(req.body)[0]
            uids[uid] = ''
            assert_in(uid, good_uids);
        assert_equals(len(uids), 10)

    def test_not_stream_token(self):
        response = self.do_send(token='S001');
        assert_unauthorized(response, 'bad token')

    def test_direct_mode_not_allowed(self):
        response = xiva.POST(xiva.prepare_url('/v2/stream_send?', **self.args()),
            {'X-DeliveryMode':'direct'}, json.dumps(self.payload))
        assert_bad_request(response, 'direct delivery mode is not allowed for stream services')
        response = self.do_send(ttl=0);
        assert_bad_request(response, 'direct delivery mode is not allowed for stream services')

    def test_stream_token_for_send_unauthorized(self):
        response = xiva.send(body=json.dumps(self.payload), **self.args())
        assert_unauthorized(response, 'bad token')

    def test_tvm_publisher_can_send(self):
        args = self.args()
        del args['token']
        response = xiva.stream_send(json.dumps(self.payload),
            tvm_ticket=xiva.publisher_tst_ticket, service='tst_stream', **args)
        assert_ok(response)

    def test_broadcast_flag_is_not_set(self):
        self.do_send()
        sleep(0.05)
        assert_equals(len(self.requests), 1)
        msg = msgpack.unpackb(self.requests[0].body)
        assert_equals(msg[MessageFields.broadcast], False)

    def test_content_type_binary(self):
        response = self.do_send()
        sleep(0.05)
        assert_ok(response)
        assert_equals(len(self.requests), 1)
        assert_content_type_equals(self.requests[0], 'application/octet-stream')
