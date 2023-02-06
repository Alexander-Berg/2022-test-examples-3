from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *

def setUp(self):
    global xiva, api, fake_hub
    xiva = HTTPRaw(host='localhost', port=18083)
    api = XivaApiV2(host='localhost', port=18083)
    fake_hub = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown(self):
    global fake_hub
    fake_hub.fini()

class TestGeneral:
    def test_reuse_connection_default(self):
        resp = xiva.GET('/ping', {})
        assert_ok(resp)
        assert_equals(resp.headers['connection'], 'keep-alive')

    def test_close_connection_on_demand(self):
        resp = xiva.GET('/ping', {'Connection': 'close'})
        assert_ok(resp)
        assert_in(resp.headers['connection'], 'close')

class TestAuthSchemeCase:
    def test_auth_scheme_case_insensitive(self):
        for scheme in ['xiva', 'Xiva', 'XIVA', 'xiVA']:
            yield self.check_auth_scheme_case_insensitive, scheme

    def check_auth_scheme_case_insensitive(self, scheme):
        resp = xiva.GET('/v2/secret_sign?service=tst1&uid=200', {'Authorization': scheme + ' L001'})
        assert_ok(resp)

    def test_auth_header_token_case_sensitive(self):
        resp = xiva.GET('/v2/secret_sign?service=tst1&uid=200', {'Authorization': 'xiva l001'})
        assert_unauthorized(resp, 'bad token')
