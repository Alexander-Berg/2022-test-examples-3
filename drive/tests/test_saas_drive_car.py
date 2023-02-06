import datetime
from cars.telematics.backend.packet_handlers import SignalCollectorPacketHandler

from twisted.trial import unittest

from ..backend.protocol import WialonCombineProtocol
from ..backend.server import WialonCombineTelematicsBackendServer
from ..backend.typed_packets import (
    WialonCombineDataPacket, WialonCombineDataPacketRecord, WialonCombineDataPacketPositionData,
    WialonCombineLoginPacket, WialonCombineDataPacketCustomParameters
)


class TelematicsDataSaasDriveSubmissionTestCase(unittest.TestCase):

    def setUp(self):
        self.imei = 1234567890
        self.protocol = WialonCombineProtocol()

        signal_collector_packet_handler = SignalCollectorPacketHandler.from_settings()

        self.server = WialonCombineTelematicsBackendServer(
            packet_handlers=[
                signal_collector_packet_handler,
            ],
            push_client=None,
        )

    def test_data_submission(self):
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

    def _login(self):
        login_packet = WialonCombineLoginPacket(id_=self.imei, pwd=None)
        self.server.handle_packet(protocol=self.protocol, packet=login_packet)
