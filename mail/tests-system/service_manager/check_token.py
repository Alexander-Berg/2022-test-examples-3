from pycommon.asserts import *
from pycommon.xiva_api import *
from pycommon.fake_server import *
from json import loads
from msgpack import packb

def setUp(self):
    global xiva_back, xconf_server, xconf_server2
    xiva_back = HTTPRaw(host='localhost', port=18080)
    xconf_server = fake_server(host='localhost', port=17083, raw_response=packb([0,[]]))
    xconf_server2 = fake_server(host='localhost', port=17086, raw_response=packb([0,[]]))

def tearDown(self):
    global xconf_server, xconf_server2
    xconf_server.fini()
    xconf_server2.fini()

class TestCheckToken:
    def setup(self):
        global xiva_back
        self.xiva_back = xiva_back

    def teardown(self):
        pass

    def check_token(self, token=None):
        headers = {}
        if token is not None:
            headers['X-XivaToken'] = token
        resp = self.xiva_back.GET('/check_token', headers)
        assert_ok(resp)
        result = loads(resp.body)
        assert_in('token_found', result)
        return result

    def test_no_token(self):
        for headers in [{}, {'X-XivaToken': ''}]:
            resp = self.xiva_back.GET('/check_token', headers)
            assert_bad_request(resp, 'missing or empty X-XivaToken header')

    def test_unknown_token(self):
        resp = self.check_token('sometoken')
        assert_equal(resp['token_found'], False)
        assert_in('environments', resp)
        envs = resp['environments']
        assert_in('any', envs)
        assert_equal(len(envs), 1)

    def test_send_token_testing(self):
        resp = self.check_token('S001')
        assert_equal(resp['token_found'], True)
        assert_in('environments', resp)
        envs = resp['environments']
        assert_in('any', envs)
        assert_equal(envs['any'], {'type': 'send token', 'service': 'tst1', 'owner': 'test_200', 'description': ''})

    def test_listen_token_testing(self):
        resp = self.check_token('12345678901234567890')
        assert_equal(resp['token_found'], True)
        assert_in('environments', resp)
        envs = resp['environments']
        assert_in('any', envs)
        assert_equal(envs['any'], {'type': 'listen token', 'service': 'mail', 'owner': 'test_400', 'description': ''})

    def test_send_token_env_aware_positive(self):
        "Trims token's environment prefix for correct environment"
        resp = self.check_token('qwerty')
        assert_equal(resp['token_found'], True)
        assert_in('environments', resp)
        envs = resp['environments']
        assert_in('any', envs)
        assert_equal(envs['any'], {'type': 'send token', 'service': 'check-test', 'owner': 'test_800', 'description': ''})

    def test_listen_token_env_aware_positive(self):
        "Trims token's environment prefix for correct environment"
        resp = self.check_token('asdfg')
        assert_equal(resp['token_found'], True)
        assert_in('environments', resp)
        envs = resp['environments']
        assert_in('any', envs)
        assert_equal(envs['any'], {'type': 'listen token', 'service': 'check-test', 'owner': 'test_800', 'description': ''})

    def test_send_token_env_aware_negative(self):
        "Doesn't trim token's environment prefix for incorrect environment"
        resp = self.check_token('qazwsx')
        assert_equal(resp['token_found'], False)
        assert_in('environments', resp)
        envs = resp['environments']
        assert_in('any', envs)
        assert_equal(len(envs), 1)

    def test_listen_token_env_aware_negative(self):
        "Doesn't trim token's environment prefix for incorrect environment"
        resp = self.check_token('zxcvb')
        assert_equal(resp['token_found'], False)
        assert_in('environments', resp)
        envs = resp['environments']
        assert_in('any', envs)
        assert_equal(len(envs), 1)
