from pycommon import *
import time

def setUp(self):
    global xiva, xiva_client, hub
    xiva = XivaApiV2(host='localhost', port=18083)
    xiva_client = XivaClient(host='localhost', port=18083, back_port=18080)
    hub = fake_server(host='localhost', port=17081, raw_response='OK')

def tearDown(self):
    global hub
    hub.fini()

class TestSubscribeWebsocketWithSign:
    def setup(self):
        self._good_args = { 'uid': "200", 'service': "tst1",
            'client': "test", 'session': "ABCD-EFGH", "format": "json"
        }
        self._sign_uid_service = xiva_client.secret_sign(**{ "token" : "L001", "uid" : "200", "service" : "tst1" })
        hub.requests = []
        hub.set_request_hook(lambda req: hub.requests.append(req))

    def args(self, **kwargs):
        args = self._good_args.copy()
        for key in kwargs:
            args[key] = kwargs[key]
        return args

    def conn_stat(self):
        stat = xiva_client.stat()
        return (int(stat['stat']['modules']['processor']['subscribers']),
            int(stat['stat']['modules']['processor']['channels']))

    def test_success_with_filter(self):
        (resp, ws) = xiva.subscribe_websocket(**self.args(filter='{ "do": "skip" }', **self._sign_uid_service))
        assert_ws_ok(resp, ws)
        wait(lambda: len(hub.requests) == 1, 2)
        assert_equals(len(hub.requests), 1)
        assert_in("filter=%7b+%22do%22%3a+%22skip%22+%7d", hub.requests[0].path)

    def test_success_prefer_filter_than_tags(self):
        (resp, ws) = xiva.subscribe_websocket(**self.args(service='tst1:tagA', filter='{ "do": "skip" }', **self._sign_uid_service))
        assert_ws_ok(resp, ws)
        wait(lambda: len(hub.requests) == 1, 2)
        assert_equals(len(hub.requests), 1)
        assert_in("filter=%7b+%22do%22%3a+%22skip%22+%7d", hub.requests[0].path)

    def test_success_use_tags_if_empty_filter(self):
        (resp, ws) = xiva.subscribe_websocket(**self.args(service='tst1:tagA', filter='', **self._sign_uid_service))
        assert_ws_ok(resp, ws)
        wait(lambda: len(hub.requests) == 1, 2)
        assert_equals(len(hub.requests), 1)
        assert_in("filter=%7b%22rules%22%3a%5b%7b%22do%22%3a%22send_bright%22%2c%22if%22%3a%7b%22%24has_tags%22%3a%5b%22tagA%22%5d%7d%7d%2c%7b%22do%22%3a%22skip%22%7d%5d%2c%22vars%22%3a%7b%7d%7d", hub.requests[0].path)
