from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *

def setUp(self):
    global xiva, fake_hub
    xiva = HTTPRaw(host='localhost', port=18083)
    fake_hub = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown(self):
    global fake_hub
    fake_hub.fini()

class TestList:
    def setup(self):
        global xiva, fake_hub
        self.raw = xiva
        self.hub = fake_hub
        self.hub.response.body = '[]'

    def test_aliases(self):
        for alias in ['subscriptions/user', 'subscriptions', 'list']:
            resp = self.raw.GET('/v2/' + alias + '?uid=123&service=mail&token=12345678901234567890', {})
            assert_ok(resp)

    def test_content_type(self):
        resp = self.raw.GET('/v2/list?uid=123&service=mail&token=12345678901234567890', {})
        assert_ok(resp)
        assert_in('content-type', resp.headers)
        assert_equals(resp.headers['content-type'], 'application/json')

    def test_device(self):
        self.hub.response.body = json.dumps([
            {"platform": "apns", "device": "123", "url": "xivamob:123"},
            {"platform": "", "device": "", "url": "xivatest:456"}]
        )
        resp = self.raw.GET('/v2/list?uid=123&service=mail&token=12345678901234567890', {})
        assert_ok(resp)
        response_body = json.loads(resp.body)
        assert_in('device', response_body[0])
        assert_equals(response_body[0]['device'], '123')
        assert_not_in('device', response_body[1])

    def test_empty_list(self):
        self.hub.response.body = json.dumps([])
        resp = self.raw.GET('/v2/list?uid=123&service=mail&token=12345678901234567890', {})
        assert_ok(resp)
        assert_equals(resp.body, '[]')