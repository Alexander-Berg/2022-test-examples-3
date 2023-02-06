from crypta.idserv.data.python.common import create_complete_graph, assert_all_nodes_are_adjacent
from crypta.graph.export.serializers.graph.proto.python.graph_proto_serializer import TGraphProtoSerializer
import pytest


@pytest.mark.parametrize("size", [1, 3, 10])
def test_serialize_deserialize_serialize(size):
    graph = create_complete_graph(size)

    serialized_graph1 = TGraphProtoSerializer.Serialize(graph)
    deserialized_graph = TGraphProtoSerializer.Deserialize(serialized_graph1)
    serialized_graph2 = TGraphProtoSerializer.Serialize(deserialized_graph)

    assert serialized_graph1 == serialized_graph2
    assert_all_nodes_are_adjacent(deserialized_graph)


@pytest.mark.xfail(raises=RuntimeError)
def test_deserialize_invalid():
    TGraphProtoSerializer.Deserialize("INVALID_PROTOBUF")
