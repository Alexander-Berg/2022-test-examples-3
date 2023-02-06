import json

from library.python.protobuf.json import proto2json
import yatest.common

from crypta.audience.lib.storage import segment_priorities
from crypta.audience.proto import storage_pb2


def test_vinyl():
    filename = yatest.common.test_output_path("base.vinyl")
    proto = storage_pb2.TPrioritiesByType()
    by_segment_type = proto.PrioritiesByType["bySegmentType"]
    by_segment_type.Priorities[100] = 200
    by_segment_type.DefaultPriority = 20

    segment_priorities.convert_proto_to_vinyl(proto, filename)

    with open(filename) as f:
        proto = segment_priorities.convert_vinyl_to_proto(f.read())
        return json.loads(proto2json.proto2json(proto, proto2json.Proto2JsonConfig(map_as_object=True)))
