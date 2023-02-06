from twisted.test import proto_helpers
from twisted.trial import unittest

from ..backend.server import WialonCombineTelematicsBackendServer
from .wialon_combine_client import WialonCombineClient


class ClientScenariosTestCase(unittest.TestCase):

    def setUp(self):
        self.server = WialonCombineTelematicsBackendServer(
            packet_handlers=[],
            push_client=None,
        )
        self.proto = self.server.get_factory().buildProtocol(('127.0.0.1', 0))
        self.tr = proto_helpers.StringTransport()
        self.proto.makeConnection(self.tr)

    def test_server_doesnt_respond_to_unknown_clients(self):
        client = WialonCombineClient(proto=self.proto, imei=123)

        with self.assertRaises(client.BadServerResponseError):
            client.send_data()

        client.login()
        client.send_data()
