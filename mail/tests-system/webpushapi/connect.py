from pycommon import *

class TestConnect:
    @classmethod
    def setup_class(cls):
        cls.xiva_unsecure = XivaApiV2(host='localhost', port=18083)
        cls.xiva = XivaApiV2(host='localhost', port=18085, secure=True)

    def test_unsecure_connect(self):
        (resp, ws) = self.xiva_unsecure.WS('/webpushapi/json_rpc', {})
        eq_(resp.code, 101)
        ws_message = ws.recv_message()
        assert_not_equal(ws_message, None)
        eq_(ws_message.opcode, 8)
        eq_(ws_message.status_code, 4403)
        eq_(ws_message.payload, 'insecure connection')

    def test_connect(self):
        (resp, ws) = self.xiva.WS('/webpushapi/json_rpc', {})
        eq_(resp.code, 101)
        ws_message = ws.recv_message()
        assert_equal(ws_message, None)
