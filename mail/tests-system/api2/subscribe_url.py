from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
from time import sleep

def setUp(self):
    global xiva, hub
    xiva = XivaApiV2(host='localhost', port=18083)
    hub = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown(self):
    global hub
    hub.fini()

class TestSubscribeUrl:
    def setup(self):
        hub.response.body = 'abcdef'

    def test_subscribe_url_ok(self):
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/url?',
            uid = '123', token = '12345678901234567890',
            service = 'mail', callback = 'qwerty', session = 'qwerty'), {})
        assert_ok(resp)

    def test_subscribe_url_with_too_big_filter(self):
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/url?',
            uid = '123', token = '12345678901234567890',
            service = 'mail', callback = 'qwerty', session = 'qwerty',
            filter = 'a'*3601), {})
        assert_bad_request(resp, 'filter size limit exceeded')

    def test_subscribe_url_content_type(self):
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/url?',
            uid = '123', token = '12345678901234567890', service = 'mail',
            callback = 'qwerty', session = 'qwerty'), {})
        assert_ok(resp)
        assert_in('content-type', resp.headers)
        assert_equals(resp.headers['content-type'], 'application/json')

    def test_subscribe_url_sub_id_passing(self):
        hub.set_request_hook(lambda req: assert_in('id=7c09f63891b363ce2abf8e0bcd7d3e93737a7c63', req.path) if '/subscribe' in req.path else None)
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/url?',
            uid = '123', token = '12345678901234567890', service = 'mail',
            callback = 'qwerty', session = 'qwerty'), {})
        assert_ok(resp)

    def test_subscribe_url_request_content_type_urlencoded(self):
        requests = []
        hub.set_request_hook(lambda req: requests.append(req))
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/url?',
            uid = '123', token = '12345678901234567890',
            service = 'mail', callback = 'qwerty', session = 'qwerty'), {})
        sleep(0.05)
        assert_ok(resp)
        eq_(len(requests), 1)
        assert_content_type_equals(requests[0], 'application/x-www-form-urlencoded; charset=UTF-8')
        requests = []

class TestUnsubscribeUrl:
    def test_subscribe_url_sub_id_passing(self):
        hub.set_request_hook(lambda req: assert_in('id=7c09f63891b363ce2abf8e0bcd7d3e93737a7c63', req.path) if '/unsubscribe' in req.path else None)
        resp = xiva.POST(xiva.prepare_url('/v2/unsubscribe/url?',
            uid = '123', token = '12345678901234567890', service = 'mail',
            subscription_id = '7c09f63891b363ce2abf8e0bcd7d3e93737a7c63'), {})
        assert_ok(resp)
