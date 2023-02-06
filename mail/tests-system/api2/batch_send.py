from pycommon import *
import BaseHTTPServer
import msgpack
import io
import sys
import time
from time import sleep

def setUp(self):
    global xiva, hub_server, fallback_hub_server, handler
    xiva = XivaApiV2(host='localhost', port=18083)
    handler = BatchRequestHandler
    hub_server = make_server(handler, host='localhost', port=17081, raw_response='OK')
    hub_server.shared_data = {}

    fallback_hub_server = make_server(handler, host='localhost', port=17082, raw_response='OK')
    fallback_hub_server.shared_data = {}

def tearDown(self):
    hub_server.fini()
    fallback_hub_server.fini()

class BatchRequestHandler(BaseHTTPServer.BaseHTTPRequestHandler):
    def do_POST(self):
        self.body = self.rfile.read(int(self.headers.getheader('content-length', 0)))
        unpacker = msgpack.Unpacker(io.BytesIO(self.body))
        msg = unpacker.unpack()
        recipients = unpacker.unpack()

        if isinstance(self.server.shared_data, dict):
            shared_data = []
            for key,value in self.server.shared_data.items():
                shared_data.append({key:value})
            self.server.shared_data = shared_data

        response_data = []
        for i in range(0, len(recipients)):
            uid = recipients[i][0]
            subid = recipients[i][1]
            for item in self.server.shared_data:
                mkey,mvalue = item.items()[0]
                if mkey == uid and (len(subid) == 0 or subid == mvalue[2]):
                    response_data.append([i] + mvalue)

        print 'rcpts', recipients
        print 'response', response_data
        response = msgpack.packb(response_data)
        self.protocol_version = 'HTTP/1.1'
        self.send_response(200, 'OK')
        self.send_header('Content-Length', len(response))
        self.send_header('Connection', 'close')
        self.end_headers()
        self.wfile.write(response)
        return ''
    def log_message(self, format, *args):
        return

class TestBatchSend:
    def setup(self):
        hub_server.reset_state()
        fallback_hub_server.reset_state()

    def teardown(self):
        time.sleep(0.05)

    def args(self, **kwargs):
        args = {'token': "S001", 'event': 'test'}
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def do_batch_send(self, recipients, subscriptions=[], **kwargs):
        body = {"payload": "test", "recipients": recipients, "subscriptions": subscriptions}
        return xiva.batch_send(body=json.dumps(body), **self.args(**kwargs))

    def extract_message(self, body):
        unpacker = msgpack.Unpacker()
        unpacker.feed(body)
        return unpacker.next()

    def test_200_in_one_request(self):
        hub_server.shared_data = {
            '1' : [200, 'OK'], '2' : [200, 'OK'], '3' : [200, 'OK']
        }

        resp = self.do_batch_send(["1","2","3"])

        eq_(hub_server.total_requests, 1)
        eq_(resp.body["results"], [
            {u'body':u'OK',u'code':200},
            {u'body':u'OK',u'code':200},
            {u'body':u'OK',u'code':200}]
        )

    def test_xtoken_in_header(self):
        hub_server.shared_data = {
            '1' : [200, 'OK'], '2' : [200, 'OK'], '3' : [200, 'OK']
        }
        resp = xiva.POST(xiva.prepare_url('/v2/batch_send?', **self.args(token=None)),
            {'Authorization': 'Xiva S001'}, json.dumps({"payload": "test", "recipients": ["1","2","3"]}))
        assert_ok(resp)

    def test_200_in_two_request(self):
        hub_server.shared_data = {
            '1' : [200, 'OK'], '2' : [200, 'OK'], '3' : [200, 'OK'],
            '4' : [200, 'OK'], '5' : [200, 'OK'], '6' : [200, 'OK']
        }

        resp = self.do_batch_send(["1","2","3","4","5","6"])

        eq_(hub_server.total_requests, 2)
        eq_(resp.body["results"], [
            {u'body':u'OK',u'code':200},
            {u'body':u'OK',u'code':200},
            {u'body':u'OK',u'code':200},
            {u'body':u'OK',u'code':200},
            {u'body':u'OK',u'code':200},
            {u'body':u'OK',u'code':200}]
        )
        # also check for correct content type
        assert_in('content-type', resp.headers)
        assert_equals(resp.headers['content-type'], 'application/json')

    def test_200_empty_one_of_two_requests(self):
        hub_server.shared_data = {
            '1' : [200, 'OK'], '2' : [200, 'OK'], '3' : [200, 'OK'],
            '4' : [200, 'OK']
        }

        resp = self.do_batch_send(["1","2","3","4","5","6"])

        eq_(hub_server.total_requests, 2)
        eq_(resp.body["results"], [
            {u'body':u'OK',u'code':200},
            {u'body':u'OK',u'code':200},
            {u'body':u'OK',u'code':200},
            {u'body':u'OK',u'code':200},
            {u'body':u'internal timeout',u'code':500},
            {u'body':u'internal timeout',u'code':500}]
        )

    def test_200_empty_all(self):
        hub_server.shared_data = {
        }

        resp = self.do_batch_send(["1","2","3","4","5","6"])

        eq_(hub_server.total_requests, 2)
        eq_(resp.body["results"], [
            {u'body':u'internal timeout',u'code':500},
            {u'body':u'internal timeout',u'code':500},
            {u'body':u'internal timeout',u'code':500},
            {u'body':u'internal timeout',u'code':500},
            {u'body':u'internal timeout',u'code':500},
            {u'body':u'internal timeout',u'code':500}]
        )

    def test_200_mixed_answer(self):
        hub_server.shared_data = {
            '4' : [409, 'text'],
            '2' : [500, 'text'],
            '6' : [204, 'text'],
            '3' : [200, '{"text":"abc"}'],
            '1' : [200, 'OK']
        }

        resp = self.do_batch_send(["1","2","3","4","5","6"])

        eq_(hub_server.total_requests, 2)
        eq_(resp.body["results"], [
            {u'body':u'OK',u'code':200},
            {u'body':u'text',u'code':500},
            {u'body':{u'text':u'abc'},u'code':200},
            {u'body':u'text',u'code':409},
            {u'body':u'internal timeout',u'code':500},
            {u'body':u'text',u'code':204}]
        )

    def test_200_send_by_id(self):
        hub_server.shared_data = {
            '1' : [200, 'OK', 'A'],
            '2' : [200, 'OK', 'B'],
        }

        resp = self.do_batch_send([{"1":"A"},{"2":"B"},{"3":"C"}])
        eq_(hub_server.total_requests, 1)
        eq_(resp.body["results"], [
            {u'body':u'OK',u'code':200, u'id': u'A'},
            {u'body':u'OK',u'code':200, u'id': u'B'},
            {u'body':u'internal timeout',u'code':500}
        ])

    def test_200_send_mixed_by_id_and_uid(self):
        hub_server.shared_data = {
            '1' : [200, 'OK', 'A'],
            '2' : [200, 'OK'],
        }

        resp = self.do_batch_send([{"1":"A"},"2",{"3":"C"}])
        eq_(hub_server.total_requests, 1)
        eq_(resp.body["results"], [
            {u'body':u'OK',u'code':200, u'id': u'A'},
            {u'body':u'OK',u'code':200},
            {u'body':u'internal timeout',u'code':500}
        ])


    def test_200_many_subscriptions(self):
        hub_server.shared_data = [
            {'1' : [200, 'OK', 'A']},
            {'1' : [200, 'OK', 'B']},
            {'1' : [205, 'text', 'C']},
        ]

        resp = self.do_batch_send(["1"])
        eq_(hub_server.total_requests, 1)
        eq_(resp.body["results"], [
            [{u'body':u'OK',u'code':200, u'id': u'A'},
            {u'body':u'OK',u'code':200, u'id': u'B'},
            {u'body':u'text',u'code':205, u'id': u'C'}]
        ])


    def test_duplicates(self):
        hub_server.shared_data = [
            {'1' : [200, 'OK', 'A']},
            {'1' : [200, 'OK', 'B']},
        ]

        resp = self.do_batch_send(["1", {"1":"A"}])
        eq_(resp.body["results"], [
            [{u'body': u'OK', u'code': 200, u'id': u'A'}, {u'body': u'OK', u'code': 200, u'id': u'B'}],
            {u'body': {u'duplicate': 0}, u'code': 409},
        ])

        resp = self.do_batch_send([{"1":"A"}, "1"])
        eq_(resp.body["results"], [
            {u'body': {u'duplicate': 1}, u'code': 409},
            [{"body":"OK","code":200,"id":"A"},{"body":"OK","code":200,"id":"B"}]
        ])

        resp = self.do_batch_send([{"1":"A"}, {"1":"A"}])
        eq_(resp.body["results"], [
            {u'body': u'OK', u'code': 200, u'id': u'A'},
            {u'body': {u'duplicate': 0}, u'code': 409},
        ])

    def test_uses_fallback_hub_on_request_timeout(self):
        hub_server.shared_data = [
            {'1' : [200, 'OK', 'A']},
            {'1' : [200, 'OK', 'B']},
            {'1' : [205, 'text', 'C']},
        ]
        fallback_hub_server.shared_data = hub_server.shared_data

        hub_server.emulate_unresponsive_server = True
        resp = self.do_batch_send(["1"])
        hub_server.emulate_unresponsive_server = False

        eq_(hub_server.total_requests, 1)
        eq_(fallback_hub_server.total_requests, 1)
        eq_(resp.body["results"], [
            [{u'body':u'OK',u'code':200, u'id': u'A'},
            {u'body':u'OK',u'code':200, u'id': u'B'},
            {u'body':u'text',u'code':205, u'id': u'C'}]
        ])

    def test_ext_id_passing(self):
        messages = []
        hub_server.set_request_hook(lambda req: messages.append(self.extract_message(req.body)))
        hub_server.shared_data = { '1' : [200, 'OK'] }

        resp = self.do_batch_send(['1'], external_request_id='test1')
        resp = self.do_batch_send(['1'], request_id='test2')
        resp = self.do_batch_send(['1'], ext_id=None)
        sleep(0.05)

        eq_(len(messages), 3)
        assert_in('.test1', messages[0][9])
        assert_in('.test2', messages[1][9])
        assert_not_in('.', messages[2][9])

    def test_pass_sharding_key_for_single_uid(self):
        requests = []
        hub_server.set_request_hook(lambda req: requests.append(req))
        hub_server.shared_data = { '1' : [200, 'OK'] }

        resp = self.do_batch_send(['1'])
        resp = self.do_batch_send(['1', '2'])
        sleep(0.05)

        eq_(len(requests), 2)
        assert_equals('sharding_key=1', requests[0].path.split('&')[-1])
        assert_equals('sharding_key=', requests[1].path.split('&')[-1])

    def test_ok_subscripion_matchers(self):
        hub_server.shared_data = { '1' : [200, 'OK'] }

        good_subscriptions = [
            { 'transport':  ['abc'] },
            { 'platform':  ['apns'] },
            { 'app': ['ya', 'test'] }
        ]
        for subscription in good_subscriptions:
            resp = self.do_batch_send(['1'], subscriptions=[subscription])
            assert_ok(resp)

    def test_unsupported_subscripion_matchers(self):
        hub_server.shared_data = { '1' : [400, 'bad request'] }

        unsupported_subscriptions = [
            { 'session':  ['456'] },
            { 'uuid':  ['789'] },
            { 'device':  ['smth'] },
            { 'subscription_id': ['123'] }
        ]
        for subscription in unsupported_subscriptions:
            resp = self.do_batch_send(['1'], subscriptions=[subscription])
            assert_bad_request(resp, "unsupported subscription matchers for batch")

    def test_messages_with_too_many_tags_are_rejected(self):
        body = {"payload": "test", 'tags': ['x' for i in range(1000)], "recipients": ['1']}
        resp = xiva.batch_send(body=json.dumps(body), **self.args())
        assert_bad_request(resp, "too many tags")

    def test_batch_multipart_binary(self):
        response = xiva.POST(xiva.prepare_url('/v2/batch_send?', **self.args(token=None)),
            {
                'Authorization': 'Xiva S001',
                'X-DeliveryMode':'direct',
                'Content-type':'multipart/related; boundary=--------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3'
            },
        """


----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3
Content-Type: application/json

{
    "recipients" : ["1", "2", "3"]
}
----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3
Content-Type: application/octet-stream

Test
----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3--
"""
        )
        assert_ok(response)
        sleep(0.1)
        eq_(hub_server.total_requests, 1)
        eq_(len(json.loads(response.body)["results"]), 3)

    def test_content_type_binary(self):
        requests = []
        hub_server.set_request_hook(lambda req: requests.append(req))
        hub_server.shared_data = { '1' : [200, 'OK'] }

        resp = self.do_batch_send(['1'])
        sleep(0.05)

        eq_(len(requests), 1)
        assert_content_type_equals(requests[0], 'application/octet-stream')

class TestBatchSendTTL:
    def setup(self):
        xiva = XivaApiV2(host='localhost', port=18083)
        hub_server.set_response(raw_response=msgpack.packb([[0,200,'OK','id-a'], [0,200,'OK','id-b']]))
        hub_server.set_request_hook(self.request_hook)
        self.requests = []

    def teardown(self):
        pass

    def request_hook(self, req):
        self.requests.append(req)

    def args(self, **kwargs):
        args = {'token': "S001", 'event': 'test'}
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def do_batch_send(self, **getargs):
        body = {"payload": "test", "recipients": ["1"]}
        return xiva.batch_send(body=json.dumps(body), **self.args(**getargs))

    def test_default_ttl_is_passed_if_not_specified(self):
        resp = self.do_batch_send()
        sleep(0.1)
        assert_equals(len(self.requests), 1)
        unpacker = msgpack.Unpacker(io.BytesIO(self.requests[0].body))
        msg = unpacker.unpack()
        assert_equals(msg[MessageFields.ttl], 604800)

    def test_ttl_is_passed(self):
        self.do_batch_send(ttl=12345)
        sleep(0.1)
        assert_equals(len(self.requests), 1)
        unpacker = msgpack.Unpacker(io.BytesIO(self.requests[0].body))
        msg = unpacker.unpack()
        assert_equals(msg[MessageFields.ttl], 12345)

    def test_small_ttl_is_passed_as_zero(self):
        self.do_batch_send(ttl=4)
        sleep(0.1)
        assert_equals(len(self.requests), 1)
        unpacker = msgpack.Unpacker(io.BytesIO(self.requests[0].body))
        msg = unpacker.unpack()
        assert_equals(msg[MessageFields.ttl], 0)
