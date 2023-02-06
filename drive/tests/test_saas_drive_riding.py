import datetime
import queue

from twisted.trial import unittest

from ..backend.packet_handlers import SaasDriveRidingSubmitterPacketHandler
from ..backend.packet_handlers.saas.base import (
    CarStateCacheStub,
    SaasClientStub,
)
from ..backend.protocol import WialonCombineProtocol
from ..backend.server import WialonCombineTelematicsBackendServer
from ..backend.typed_packets import (
    WialonCombineDataPacket, WialonCombineDataPacketRecord, WialonCombineDataPacketPositionData,
    WialonCombineLoginPacket, WialonCombineDataPacketCustomParameters
)


class TelematicsDataSaasDriveNavigatorSubmissionTestCase(unittest.TestCase):

    def setUp(self):
        self.imei = 1234567890
        self.protocol = WialonCombineProtocol()

        self.state_cache = CarStateCacheStub()
        self.saas_client = SaasClientStub()
        SaasDriveRidingSubmitterPacketHandler.status_cache = self.state_cache
        SaasDriveRidingSubmitterPacketHandler.current_ride_cache = (
            self.state_cache
        )

        saas_submitter_packet_handler = SaasDriveRidingSubmitterPacketHandler(
            saas_client=self.saas_client,
        )

        self.server = WialonCombineTelematicsBackendServer(
            packet_handlers=[
                saas_submitter_packet_handler,
            ],
            push_client=None,
        )

    def test_data_submission(self):
        self._login()
        data_packet = WialonCombineDataPacket(
            records=[
                WialonCombineDataPacketRecord(
                    timestamp=int(datetime.datetime.utcnow().timestamp()),
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

        try:
            doc = self.saas_client.documents.get(timeout=1)
        except queue.Empty:
            self.fail('No documents were added')

        self.assertEqual(
            set(doc.to_json().keys()),
            {
                's_device_id',
                'DID',
                'ACCID',
                'ModificationType',
                'TS',
                'RHASH',
                'options',
                'geo',
                'url',
                's_speed',
                'destination',
                's_course',
                'SESSIONID',
            }
        )

        self.assertIn('modification_timestamp', doc.to_json()['options'])
        self.assertEqual(isinstance(doc.to_json()['TS']['value'], int), True)

    def test_data_submission_with_custom_subrecord_no_position(self):
        self._login()
        data_packet = WialonCombineDataPacket(
            records=[
                WialonCombineDataPacketRecord(
                    timestamp=int(datetime.datetime.utcnow().timestamp()),
                    subrecords=[
                        WialonCombineDataPacketCustomParameters(
                            params={
                                1243: -0.25,
                                1244: -0.12890625,
                                1245: -0.93359375,
                                2109: 1156,
                            }
                        ),
                    ],
                ),
            ],
        )
        self.server.handle_packet(protocol=self.protocol, packet=data_packet)

        try:
            doc = self.saas_client.documents.get(timeout=1)
            self.fail('No docs were supposed to be received')
        except queue.Empty:
            pass

    def test_data_submission_with_custom_subrecord(self):
        self._login()
        data_packet = WialonCombineDataPacket(
            records=[
                WialonCombineDataPacketRecord(
                    timestamp=int(datetime.datetime.utcnow().timestamp()),
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
                        WialonCombineDataPacketCustomParameters(
                            params={
                                1243: -0.25,
                                1244: -0.12890625,
                                1245: -0.93359375,
                                2109: 1156,
                            }
                        ),
                    ],
                ),
            ],
        )
        self.server.handle_packet(protocol=self.protocol, packet=data_packet)

        try:
            doc = self.saas_client.documents.get(timeout=1)
        except queue.Empty:
            self.fail('No documents were added')

        self.assertEqual(
            set(doc.to_json().keys()),
            {
                's_device_id',
                'DID',
                'ACCID',
                'ModificationType',
                'TS',
                'RHASH',
                'options',
                'geo',
                'url',
                's_speed',
                'destination',
                's_course',
                'SESSIONID',
                'rpm',
                'g_x',
                'g_y',
                'g_z',
            }
        )

        self.assertIn('modification_timestamp', doc.to_json()['options'])
        self.assertEqual(isinstance(doc.to_json()['TS']['value'], int), True)
        self.assertAlmostEqual(doc.to_json()['g_x']['value'], -0.25)
        self.assertAlmostEqual(doc.to_json()['g_y']['value'], -0.12890625)
        self.assertAlmostEqual(doc.to_json()['g_z']['value'], -0.93359375)
        self.assertEqual(doc.to_json()['rpm']['value'], 1156)

    def _login(self):
        login_packet = WialonCombineLoginPacket(id_=self.imei, pwd=None)
        self.server.handle_packet(protocol=self.protocol, packet=login_packet)
