import datetime

from twisted.trial import unittest

from ..backend.packet_handlers import BasePacketHandler
from ..backend.protocol import WialonCombineProtocol
from ..backend.server import WialonCombineTelematicsBackendServer
from ..backend.typed_packets import (
    WialonCombineDataPacket, WialonCombineDataPacketRecord, WialonCombineDataPacketPositionData,
    WialonCombineLoginPacket,
)


class AccumulatorPacketHandler(BasePacketHandler):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.packets = []

    def handle_packet(self, imei, packet):
        self.packets.append((imei, packet))


class TelematicsBackendServerTestCase(unittest.TestCase):

    def setUp(self):
        self.imei = 0
        self.protocol = WialonCombineProtocol()
        self.accumulator = AccumulatorPacketHandler()
        self.server = WialonCombineTelematicsBackendServer(
            packet_handlers=[
                self.accumulator,
            ],
            push_client=None,
        )

    def test_login(self):
        self._login()
        self.assertEqual(len(self.accumulator.packets), 1)
        imei, packet = self.accumulator.packets[0]
        self.assertEqual(imei, self.imei)
        self.assertTrue(isinstance(packet, WialonCombineLoginPacket))

    def test_data(self):
        self._login()
        data_packet = WialonCombineDataPacket(
            records=[
                WialonCombineDataPacketRecord(
                    timestamp=datetime.datetime.utcnow().timestamp(),
                    subrecords=[
                        WialonCombineDataPacketPositionData(
                            lat=12.3,
                            lon=32.1,
                            speed=0,
                            course=0,
                            height=0,
                            sats=0,
                            hdop=0,
                        ),
                    ],
                ),
            ],
        )
        self.server.handle_packet(protocol=self.protocol, packet=data_packet)
        self.assertEqual(len(self.accumulator.packets), 2)
        imei, packet = self.accumulator.packets[-1]
        self.assertEqual(imei, self.imei)
        position_data = packet.records[0].subrecords[0]
        self.assertEqual(position_data.lat, 12.3)
        self.assertEqual(position_data.lon, 32.1)

    def _login(self):
        login_packet = WialonCombineLoginPacket(id_=self.imei, pwd=None)
        self.server.handle_packet(protocol=self.protocol, packet=login_packet)
