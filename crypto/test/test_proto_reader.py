from library.python.protobuf.json import proto2json

from crypta.lib.python.worker_utils.proto_reader import ProtoReader
from crypta.lib.python.worker_utils.test.proto.message_pb2 import Message


def test_schedule():
    result = []

    def process(protos, cookie):
        result.append([protos, cookie])

    proto_reader = ProtoReader(process, Message)

    converter = proto2json.Proto2JsonConverter(Message)
    protos_batch = [Message(Value=str(i)) for i in range(5)]
    batch = [converter.convert(proto) for proto in protos_batch]
    cookie = 100

    proto_reader.schedule(batch, cookie)

    assert [[protos_batch, cookie]] == result
