from pycommon import *
import BaseHTTPServer
import msgpack
import io
import sys
from time import sleep
from re import match

def setUp(self):
    global xiva, hub_server, fallback_hub_server, handler
    xiva = XivaApiV2(host='localhost', port=18083)
    hub_server = fake_server(host='localhost', port=17081, raw_response='OK')
    fallback_hub_server = fake_server(host='localhost', port=17082, raw_response='OK')

def tearDown(self):
    hub_server.fini()
    fallback_hub_server.fini()

class TestSend:
    def setup(self):
        hub_server.reset_state()
        self.payload = {"payload": "test"}
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

    def args(self, **kwargs):
        args = {'token': "S001", 'event': 'test', "uid": "1"}
        for key in kwargs: args[key] = kwargs[key]
        return args

    def args_wo_token(self, **kwargs):
        args = self.args(**kwargs)
        del args['token']
        return args

    def do_send(self, **kwargs):
        return xiva.send(body=json.dumps(self.payload), **self.args(**kwargs))

    def do_direct_send(self, **kwargs):
        return xiva.send_direct(body=json.dumps(self.payload), **self.args(**kwargs))

    def test_400_if_invalid_delivery_mode(self):
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args()),
            {'X-DeliveryMode':'abc'}, json.dumps(self.payload))
        assert_bad_request(response, 'invalid delivery mode "abc"')

    def test_400_if_request_timeout_too_small(self):
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args()),
            {'X-Request-Timeout':'75'}, json.dumps(self.payload))
        assert_bad_request(response, 'request timeout too small')

    def test_uid_with_separator(self):
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args(uid='uid:1')),
            {}, json.dumps(self.payload))
        assert_bad_request(response, 'uid can\'t contain ":"')
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args(token='S123456', uid='uid:1', service='autoru')),
            {}, json.dumps(self.payload))
        assert_ok(response)

    def test_200_if_default_delivery_mode(self):
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args()),
            {}, json.dumps(self.payload))
        sleep(0.05)
        assert_ok(response)
        assert_equals(len(self.requests), 1)
        assert_in("/binary_notify", self.requests[0].path)

    def test_200_if_default_delivery_mode_false_in_xconf(self):
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args(token='ab123456tstforcedirectcdef')),
            {}, json.dumps(self.payload))
        sleep(0.05)
        assert_ok(response)
        assert_equals(len(self.requests), 1)
        assert_in("/fast_binary_notify", self.requests[0].path)

    def test_xtoken_in_headers(self):
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args(token=None)),
            {'Authorization': 'Xiva S001', 'X-DeliveryMode':'direct'}, json.dumps(self.payload))
        assert_ok(response)
        sleep(0.1)
        assert_equals(len(self.requests), 1)

    def test_200_direct_send_with_report(self):
        resp = self.do_direct_send()
        sleep(0.05)
        assert_equals(len(self.requests), 1)
        assert_equals(resp.body, [{u'body': u'OK', u'code': 200, u'id': u'id-a'},
            {u'body': u'OK', u'code': 200, u'id': u'id-b'}]
        )
        # also check for correct content type
        assert_in('content-type', resp.headers)
        assert_equals(resp.headers['content-type'], 'application/json')

    def test_queued_send_with_ttl_0_causes_direct_send_with_no_report(self):
        resp = self.do_send(ttl=0)
        sleep(0.05)
        assert_equals(len(self.requests), 1)
        assert_in("/fast_binary_notify/", self.requests[0].path)
        assert_equals(resp.body, "OK")

    def test_queued_send_with_small_ttl_causes_direct_send_with_no_report(self):
        resp = self.do_send(ttl=4)
        sleep(0.05)
        assert_equals(len(self.requests), 1)
        assert_in("/fast_binary_notify/", self.requests[0].path)
        assert_equals(resp.body, "OK")

    def test_default_ttl_is_passed_if_not_specified(self):
        resp = self.do_send()
        sleep(0.05)
        assert_equals(len(self.requests), 1)
        path = self.requests[0].path
        path_ttl = path.split('&ttl=')[1].split('&')[0]
        assert_equals(path_ttl, "604800")
        msg = msgpack.unpackb(self.requests[0].body)
        assert_equals(msg[MessageFields.ttl], 604800)

    def test_ttl_is_passed(self):
        self.do_send(ttl=12345)
        sleep(0.05)
        assert_equals(len(self.requests), 1)
        path = self.requests[0].path
        path_ttl = path.split('&ttl=')[1].split('&')[0]
        assert_equals(path_ttl, "12345")
        msg = msgpack.unpackb(self.requests[0].body)
        assert_equals(msg[MessageFields.ttl], 12345)

    def test_small_ttl_is_passed_as_zero(self):
        self.do_send(ttl=4)
        sleep(0.05)
        assert_equals(len(self.requests), 1)
        path = self.requests[0].path
        path_ttl = path.split('&ttl=')[1].split('&')[0]
        assert_equals(path_ttl, "0")
        msg = msgpack.unpackb(self.requests[0].body)
        assert_equals(msg[MessageFields.ttl], 0)

    def test_uses_fallback_hub_on_request_timeout(self):
        hub_server.emulate_unresponsive_server = True
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args()),
            {}, json.dumps(self.payload))
        sleep(0.05)
        hub_server.emulate_unresponsive_server = False
        assert_ok(response)
        assert_equals(len(self.requests), 1)
        assert_equals(len(self.fallback_requests), 1)

    def test_passes_shortcut_matchers(self):
        self.do_send(subscription_id='qwerty')
        self.do_send(subscription_session='asdfg')
        self.do_send(subscription_uuid='zxcvb')
        sleep(0.05)
        messages = [msgpack.unpackb(req.body) for req in self.requests]
        assert_equals(len(messages), 3)
        assert_equals(messages[0][MessageFields.subscription_matchers][0], [2, '', ['qwerty']])
        assert_equals(messages[1][MessageFields.subscription_matchers][0], [3, '', ['asdfg']])
        assert_equals(messages[2][MessageFields.subscription_matchers][0], [4, '', ['zxcvb']])

    def test_matchers_priority_body(self):
        self.payload = {'payload': 'test', 'subscriptions': [{'platform': ['apns']}]}
        self.do_send(subscription_id='qwerty')
        sleep(0.05)
        messages = [msgpack.unpackb(req.body) for req in self.requests]
        assert_equals(len(messages), 1)
        assert_equals(messages[0][MessageFields.subscription_matchers][0], [0, '', ['apns']])

    def test_ext_id_passing(self):
        self.do_send(external_request_id='test1')
        self.do_send(request_id='test2')
        self.do_send(ext_id=None)
        sleep(0.05)

        messages = [msgpack.unpackb(req.body) for req in self.requests]
        assert_equals(len(messages), 3)
        assert_in('.test1', messages[0][MessageFields.transit_id])
        assert_in('.test2', messages[1][MessageFields.transit_id])
        assert(match('\\w{12,}', messages[2][MessageFields.transit_id])), 'transit id has wrong format: "{}"'.format(messages[2][MessageFields.transit_id])

    def test_messages_with_too_many_tags_are_rejected(self):
        self.payload = {'payload': 'test', 'tags': ['x' for i in range(1000)]}
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args()),
            {}, json.dumps(self.payload))
        assert_bad_request(response, "too many tags")

    def test_multipart_binary(self):
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args(token=None)),
            {
                'Authorization': 'Xiva S001',
                'X-DeliveryMode':'direct',
                'Content-type':'multipart/related; boundary=--------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3'
            },
        """


----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3
Content-Type: application/json

{
    "tags" : ["a","b"]
}
----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3
Content-Type: application/octet-stream

Test
----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3--
"""
        )
        assert_ok(response)
        sleep(0.1)
        assert_equals(len(self.requests), 1)

        msg = msgpack.unpackb(self.requests[0].body)
        assert_equals(msg[12], ["a", "b"]) # tags
        assert_equals(msg[7], 'Test') # payload
        assert_equals(msg[20], 2) # type binary

    def test_multipart_binary_invalid_content_type(self):
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args(token=None)),
            {
                'Authorization': 'Xiva S001',
                'X-DeliveryMode':'direct',
                'Content-type':'multipart/mixed; boundary=--------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3'
            },
        """


----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3
Content-Type: application/json

{}
----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3
Content-Type: application/octet-stream

Test
----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3--
"""
        )
        assert_bad_request(response, "invalid multipart send request")

    def test_multipart_binary_invalid_part1_content_type(self):
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args(token=None)),
            {
                'Authorization': 'Xiva S001',
                'X-DeliveryMode':'direct',
                'Content-type':'multipart/related; boundary=--------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3'
            },
        """


----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3
Content-Type: application/xml

<xml></xml>
----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3
Content-Type: application/octet-stream

Test
----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3--
"""
        )
        assert_bad_request(response, "invalid multipart send request")

    def test_multipart_binary_invalid_part2_content_type(self):
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args(token=None)),
            {
                'Authorization': 'Xiva S001',
                'X-DeliveryMode':'direct',
                'Content-type':'multipart/related; boundary=--------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3'
            },
        """


----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3
Content-Type: application/json

{}
----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3
Content-Type: application/json

{}
----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3--
"""
        )
        assert_bad_request(response, "invalid multipart send request")


    def test_multipart_binary_400_on_bad_token(self):
        response = xiva.POST(xiva.prepare_url('/v2/send?', **self.args(token=None)),
            {
                'Authorization': 'Xiva 01',
                'X-DeliveryMode':'direct',
                'Content-type':'multipart/related; boundary=--------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3'
            },
        """


----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3
Content-Type: application/json

{}
----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3
Content-Type: application/json

{}
----------gL6KM7GI3GI3Ef1Ij5cH2cH2gL6GI3--
"""
        )
        assert_unauthorized(response, "bad token")

    def test_message_with_normal_payload_is_accepted(self):
        self.payload = {'payload': 'a' * (1024 * 1024 - 1)}
        response = xiva.send(json.dumps(self.payload), **self.args())
        assert_ok(response)

    def test_message_with_large_payload_is_rejected(self):
        self.payload = {'payload': 'a' * (1024 * 1024)}
        response = xiva.send(json.dumps(self.payload), **self.args())
        assert_bad_request(response, "payload too large")

    def test_message_with_large_payload_for_unlimited_service_is_accepted(self):
        self.payload = {'payload': 'a' * (1024 * 1024)}
        response = xiva.send(json.dumps(self.payload), **self.args(token='unlimitedpayload'))
        assert_ok(response)

    def test_tvm_publisher_can_send(self):
        response = xiva.send(json.dumps(self.payload),
            tvm_ticket=xiva.publisher_tst_ticket, service='mail', **self.args_wo_token())
        assert_ok(response)

    def test_tvm_production_publisher_can_not_send_in_sandbox(self):
        response = xiva.send(json.dumps(self.payload),
            tvm_ticket=xiva.publisher_production_ticket, service='mail', **self.args_wo_token())
        assert_unauthorized(response, "forbidden service")

    def test_tvm_suspended_publisher_can_not_send(self):
        response = xiva.send(json.dumps(self.payload),
            tvm_ticket=xiva.publisher_suspended_ticket, service='mail', **self.args_wo_token())
        assert_unauthorized(response, "forbidden service")

    def test_tvm_subscriber_can_not_send(self):
        response = xiva.send(json.dumps(self.payload),
            tvm_ticket=xiva.subscriber_tst_ticket, service='mail', **self.args_wo_token())
        assert_unauthorized(response, "forbidden service")

    def test_broadcast_flag_is_not_set(self):
        self.do_send()
        sleep(0.05)
        assert_equals(len(self.requests), 1)
        msg = msgpack.unpackb(self.requests[0].body)
        assert_equals(msg[MessageFields.broadcast], False)

    def test_content_type_binary(self):
        response = self.do_direct_send()
        sleep(0.05)
        assert_ok(response)
        assert_equals(len(self.requests), 1)
        assert_content_type_equals(self.requests[0], 'application/octet-stream')
