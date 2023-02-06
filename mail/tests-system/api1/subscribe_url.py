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

class TestSubscribeUrl:
    def setup(self):
        hub.response.body = 'abcdef'

    def test_subscribe_url_ctoken_compatibility(self):
        resp = xiva.POST(xiva.prepare_url('/v1/subscribe/url?',
            uid = '123', ctoken = '12345678901234567890',
            service = 'mail', callback = 'qwerty', session = 'qwerty'), {})
        assert_ok(resp)
