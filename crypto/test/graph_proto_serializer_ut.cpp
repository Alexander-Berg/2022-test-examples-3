#include <crypta/graph/export/serializers/graph/proto/graph_proto_serializer.h>

#include <crypta/idserv/data/graph_comparator.h>
#include <crypta/idserv/data/graph_test_utils.h>
#include <crypta/lib/native/proto_serializer/proto_serializer.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TGraphProtoSerializer) {
    using namespace NCrypta;
    using namespace NCrypta::NIS;

    Y_UNIT_TEST(Serialize) {
        const auto& graph = CreateCompleteGraph(10);
        const auto& protoStr = TGraphProtoSerializer::Serialize(graph);
        const auto& graphProto = NProtoSerializer::CreateFromString<crypta::idserv::proto::TGraph>(protoStr);

        UNIT_ASSERT_EQUAL(10, graphProto.GetNodes().size());
        UNIT_ASSERT_EQUAL(45, graphProto.GetEdges().size());
    }

    Y_UNIT_TEST(SerializeDeserialize) {
        const auto& graph = CreateCompleteGraph(10);

        const auto& serializedGraph = TGraphProtoSerializer::Serialize(graph);
        const auto& deserializedGraph = TGraphProtoSerializer::Deserialize(serializedGraph);

        UNIT_ASSERT(TGraphComparator::OrderedEqual(graph, deserializedGraph));
    }

    Y_UNIT_TEST(DeserializeEmpty) {
        UNIT_ASSERT_EXCEPTION(TGraphProtoSerializer::Deserialize(""), yexception);
    }
}
