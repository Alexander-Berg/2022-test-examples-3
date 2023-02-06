from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *

def setUp(self):
    global xiva, hub
    xiva = XivaApiV2(host='localhost', port=18083)
    hub = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown(self):
    global hub
    hub.fini()

class TestUnsubscribe:
    def setup(self):
        hub.response.body = 'OK'

    def test_unsubscribe_for_url_subscription(self):
        resp = xiva.POST(xiva.prepare_url('/v2/subscribe/url?',
            uid = '123', token = '12345678901234567890',
            service = 'mail', callback = 'qwerty', session = 'qwerty'))
        assert_ok(resp)
        sub_id = json.loads(resp.body)['subscription-id']

        resp = xiva.POST(xiva.prepare_url('/v2/unsubscribe?',
            uid = '123', token = '12345678901234567890',
            service = 'mail', subscription_id = sub_id))
        assert_ok(resp)


