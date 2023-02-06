from unittest.mock import MagicMock

from twisted.test import proto_helpers
from twisted.trial import unittest

from ..backend.server import WialonCombineTelematicsBackendServer
from ..commands.base import CommandResult
from ..commands.end_of_lease import EndOfLeaseCommand
from ..commands.simple import SimpleCommand
from ..commands.registry import CommandRegistry
from .wialon_combine_client import WialonCombineClient


class CommandsTestCase(unittest.TestCase):

    def setUp(self):
        self.server = WialonCombineTelematicsBackendServer(
            packet_handlers=[],
            push_client=MagicMock(),
        )
        self.proto = self.server.get_factory().buildProtocol(('127.0.0.1', 0))
        self.tr = proto_helpers.StringTransport()
        self.proto.makeConnection(self.tr)

        self.imei = 123
        self.client = WialonCombineClient(proto=self.proto, imei=self.imei)
        self.client.login()

    def test_simple_ok(self):
        command = SimpleCommand(imei=self.imei, text='server?')
        self.server.send_command(command=command)
        self.assertEqual(self.proto.transport.value(), b'#server?#\r\n')

        command.deferred.addCallback(self.assertTrue)

        return command.deferred

    def test_end_of_lease_ok(self):
        command = EndOfLeaseCommand(imei=self.imei, id_='D5961458')
        self.server.send_command(command=command)

        self.assertEqual(self.proto.transport.value(), b'/D5961458#yadrive_end_of_lease#\r\n')
        self.proto.transport.clear()

        self.assertFalse(command.is_ready())

        self.proto.dataReceived(
            b'#M#/D5961458 yadrive_end_of_lease command received\n;854B\r\n'
        )
        self.proto.dataReceived(
            b'$$\x01\x00\x04\x00)Zl\xec\xd1\x01\x0c/D5961458 engine allready stopped\n\x00\x90\xce'
        )
        self.proto.dataReceived(
            b'$$\x01\x00\x05\x00,Zl\xec\xd1\x01\x0c/D5961458 check parking and light ok\n\x00=I'
        )

        self.assertFalse(command.is_ready())

        self.proto.dataReceived(
            b'$$\x01\x00\t\x00#Zl\xec\xdb\x01\x0c/D5961458 end of lease done\n\x00\xa3\xf3'
        )

        command.deferred.addCallback(
            lambda result: self.assertEqual(result.status, CommandResult.Status.OK),
        )

        return command.deferred

    def test_end_of_lease_ok_real_sample(self):
        command = EndOfLeaseCommand(imei=self.imei, id_='1BA9433E')
        self.server.send_command(command=command)

        self.proto.dataReceived(
            b'$$\x01\x00\x14\x00!Z\xc0\xfc\xab\x02\x01\x03R~\xa0\x02A\xa5\xac\x00\x00\x00H\x00\x92\n\x00\x00\x00\x80\x01\x887\x08E\x8bx\xcd\x15\x17#M#/1BA9433E yadrive_end_of_lease command received\n;C9C7\r\n',  # pylint: disable=line-too-long
        )
        self.proto.dataReceived(
            b'$$\x01\x00\x15\x00)Z\xc0\xfc\xb7\x01\x0c/1BA9433E engine allready stopped\n\x00\x89\xec$$\x01\x00\x16\x01kZ\xc0\xfc\xb3\x02\x01\x03R~\xa0\x02A\xa5\xac\x00\x00\x00H\x00\x92\n\x00\x00\x00\x80\x01\x84\xd9\x00\x01Z\xc0\xfc\xb3\x02\x01\x03R~\xa0\x02A\xa5\xac\x00\x00\x00H\x00\x92\n\x00\x00\x00\x80\x01\x88\x9a\x00\x01Z\xc0\xfc\xb3\x02\x01\x03R~\xa0\x02A\xa5\xac\x00\x00\x00H\x00\x92\n\x00\x00\x00\x80\x01\x88\xa0\x00\x01Z\xc0\xfc\xb4\x02\x01\x03R~\xa0\x02A\xa5\xac\x00\x00\x00H\x00\x92\x0b\x00e\x00\x80\x1f\t\x02\x00\x10\xcen\r\x00\x04\x11\x00\x01\x14\x00\x00j\x08?\x81G\xaek\x08?\xe6ffl\x08?\xc0\x00\x00m\x00\x0en\x00\x0bq\x08E\x87E\x08r\x00\x01\x84\x13\x00\x00\x84\xdb\x08?s\x00\x00\x84\xdc\x08>8\x00\x00\x84\xdd\x08= \x00\x00\x84\xe2\x08B\x02\xd3\xeb\x84\xe4\x08AO?t\x84\xe5\x08@\x85\xae\xc1\x87\xd1\x00\xfa\x87\xd2\x00c\x87\xd3\x01i\x01\x87\xd4\x01!g\x87\xd5\x00\x1f\x87\xd8\x02\x00\x01\x14\x97\x87\xdb\x01\x06s\x87\xdc\x01\t\xc8\x87\xdf\x01\x04Y\x88;\x00<\x88=\x00\x00\x88>\x08B\xb6\x00\x00\x88?\x00\x00Z\xc0\xfc\xb5\x02\x01\x03R~\xa0\x02A\xa5\xac\x00\x00\x00H\x00\x92\r\x00\x00\x00\x80\x01\x84\xd9\x00\x00Z\xc0\xfc\xb5\x02\x01\x03R~\xa0\x02A\xa5\xac\x00\x00\x00H\x00\x92\r\x00\x00\x00\x80\x01\x88\x9a\x00\x00Z\xc0\xfc\xb5\x02\x01\x03R~\xa0\x02A\xa5\xac\x00\x00\x00H\x00\x92\r\x00\x00\x00\x80\x01\x88\xa0\x00\x00.\xe4$$\x01\x00\x17\x00,Z\xc0\xfc\xb8\x01\x0c/1BA9433E check parking and light ok\n\x00\x08r',  # pylint: disable=line-too-long
        )
        self.proto.transport.clear()
        self.proto.dataReceived(
            b'$$\x01\x00\x18\x00#Z\xc0\xfc\xba\x01\x0c/1BA9433E end of lease done\n\x00\x95B'
        )

        command.deferred.addCallback(
            lambda result: self.assertEqual(result.status, CommandResult.Status.OK),
        )

        return command.deferred

    def test_end_of_lease_timeout(self):
        command = EndOfLeaseCommand(imei=self.imei, id_='D5961458', timeout=0)
        self.server.send_command(command=command)
        command.deferred.addCallback(
            lambda result: self.assertEqual(result.status, CommandResult.Status.TIMEOUT),
        )
        return command.deferred

    def test_replies_accumulated(self):
        command = EndOfLeaseCommand(imei=self.imei, id_='D5961458')
        self.server.send_command(command=command)

        self.assertEqual(len(command.replies), 0)

        self.proto.dataReceived(
            b'#M#/D5961458 yadrive_end_of_lease command received\n;854B\r\n'
        )

        self.proto.dataReceived(
            b'$$\x01\x00\x04\x00)Zl\xec\xd1\x01\x0c/D5961458 engine allready stopped\n\x00\x90\xce'
        )
        self.assertEqual(len(command.replies), 1)

        self.proto.dataReceived(
            b'$$\x01\x00\x05\x00,Zl\xec\xd1\x01\x0c/D5961458 check parking and light ok\n\x00=I'
        )
        self.assertEqual(len(command.replies), 2)

        self.proto.dataReceived(
            b'$$\x01\x00\t\x00#Zl\xec\xdb\x01\x0c/D5961458 end of lease done\n\x00\xa3\xf3'
        )
        self.assertEqual(len(command.replies), 3)

        self.assertLess(command.replies[0].received_at, command.replies[1].received_at)
        self.assertLess(command.replies[1].received_at, command.replies[2].received_at)


class CommandRegistryTestCase(unittest.TestCase):

    def test_eviction(self):
        registry = CommandRegistry(eviction_timeout=0)

        self.assertEqual(len(registry), 0)

        command1 = EndOfLeaseCommand(id_='a', imei=777, timeout=0)
        registry.register(command1)
        self.assertEqual(len(registry), 1)
        self.assertIsNotNone(registry.get(command1.id))

        command2 = EndOfLeaseCommand(id_='b', imei=777, timeout=0)
        registry.register(command2)
        self.assertEqual(len(registry), 1)
        self.assertIsNone(registry.get(command1.id))
        self.assertIsNotNone(registry.get(command2.id))

        return command2.deferred
