import struct

from PyCRC.CRC16 import CRC16


class WialonCombineClient(object):

    class BadServerResponseError(Exception):
        pass

    def __init__(self, proto, imei):
        self._proto = proto
        self._imei = imei

    def login(self):
        data = b''.join([
            b'$$\x00\x00\x00\x00\n\x010',
            struct.pack('>Q', self._imei),
        ])
        data = self._add_checksum(data)
        self._proto.dataReceived(data)

        response = self._proto.transport.value()
        self._proto.transport.clear()
        if response != b'@@\x00\x00\x00':
            raise self.BadServerResponseError(response)

    def send_data(self):
        data = b'$$\x01\x00\x01\x00\x9aY\xcf\xd8\xcc\x02\x01\x03S(\xf0\x02;\xc0\x04\x00\x00\x00\xc8\x00\x9f\x10\x00G\x00\x80\x1a\t\x02\x00\x02\xf8\x98\x0b\x00\x01\r\x00\x00\x0e\x00\x00j\x08?5\xc2\x8fm\x00\x10n\x00\nq\x08B|\xba\xccs\x00\x06\x84\xd9\x00\x00\x84\xe2\x08B\x18\x19\x8b\x84\xe4\x08AL\x08\x8d\x84\xe5\x08@\x86g\x9a\x87\xd5\x00\x19\x885\x00\x00\x886\x00\x00\x887\x08C\x0c\xb33\x888\x08\x00\x00\x00\x00\x889\x08\x00\x00\x00\x00\x88:\x08\x00\x00\x00\x00\x88;\x00\x00\x88<\x00&\x88=\x00\x00\x88>\x08B\xb4\x00\x00\x88\x9a\x00\x00\x88\xa2\x00\x00\xdeA'  # pylint: disable=line-too-long
        self._proto.dataReceived(data)

        response = self._proto.transport.value()
        self._proto.transport.clear()
        if response != b'@@\x00\x00\x01':
            raise self.BadServerResponseError(response)

    def _add_checksum(self, data):
        checksum = struct.pack('>H', CRC16().calculate(data))
        data = b''.join([data, checksum])
        return data
