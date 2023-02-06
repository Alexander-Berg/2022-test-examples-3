import crypta.lib.python.yql.proto_field as yql_proto
from crypta.lib.python.yql.proto_field.test.proto.bus_pb2 import (
    TBus,
    TGarage,
)


def test_get_attrs():
    attrs = yql_proto.get_attrs(TBus)

    assert 2 == len(attrs)
    assert "_yql_proto_field_Driver" in attrs
    assert "_yql_proto_field_Motor" in attrs


def test_get_attrs_default_yt_serialization():
    attrs = yql_proto.get_attrs(TGarage)

    assert 0 == len(attrs)
