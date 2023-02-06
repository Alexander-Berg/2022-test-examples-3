#include <crypta/idserv/data/graph_comparator.h>
#include <crypta/idserv/data/graph_test_utils.h>

#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TGraphComparator) {
    using namespace NCrypta::NIS;

    Y_UNIT_TEST(OrderedEqual) {
        const auto& graph1 = CreateCompleteGraph(10);
        const auto& graph2 = CreateCompleteGraph(10);

        UNIT_ASSERT(TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(TGraphComparator::OrderedEqual(graph1, graph2));
    }

    Y_UNIT_TEST(DifferentIds) {
        TGraph graph1;
        TGraph graph2;

        graph1.SetId(1);
        graph2.SetId(2);

        UNIT_ASSERT(!TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(!TGraphComparator::OrderedEqual(graph1, graph2));
    }

    Y_UNIT_TEST(DifferentGraphAttrs) {
        TGraph graph1;
        TGraph graph2;

        graph2.GetAttributes()["custom_attr"] = "value";

        UNIT_ASSERT(!TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(!TGraphComparator::OrderedEqual(graph1, graph2));
    }

    Y_UNIT_TEST(DifferentNodesCount) {
        TGraph graph1;
        TGraph graph2;

        graph1.CreateNode(TId("yandexuid", "1"));
        graph1.CreateNode(TId("yandexuid", "2"));

        graph2.CreateNode(TId("yandexuid", "1"));
        graph2.CreateNode(TId("yandexuid", "2"));
        graph2.CreateNode(TId("yandexuid", "3"));

        UNIT_ASSERT(!TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(!TGraphComparator::OrderedEqual(graph1, graph2));
    }

    Y_UNIT_TEST(DifferentNodes) {
        TGraph graph1;
        TGraph graph2;

        graph1.CreateNode(TId("yandexuid", "1"));
        graph1.CreateNode(TId("yandexuid", "2"));

        graph2.CreateNode(TId("xuniq", "1"));
        graph2.CreateNode(TId("yandexuid", "2"));

        UNIT_ASSERT(!TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(!TGraphComparator::OrderedEqual(graph1, graph2));
    }

    Y_UNIT_TEST(ReorderedNodes) {
        TGraph graph1;
        TGraph graph2;

        graph1.CreateNode(TId("yandexuid", "1"));
        graph1.CreateNode(TId("yandexuid", "2"));

        graph2.CreateNode(TId("yandexuid", "2"));
        graph2.CreateNode(TId("yandexuid", "1"));

        UNIT_ASSERT(TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(!TGraphComparator::OrderedEqual(graph1, graph2));
    }

    Y_UNIT_TEST(DifferentNodeAttrs) {
        TGraph graph1;
        TGraph graph2;

        graph1.CreateNode(TId("yandexuid", "1"))->Attributes["attr1"] = "value1";
        graph2.CreateNode(TId("yandexuid", "1"))->Attributes["attr2"] = "value2";

        UNIT_ASSERT(!TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(!TGraphComparator::OrderedEqual(graph1, graph2));
    }

    Y_UNIT_TEST(DifferentEdgesCount) {
        TGraph graph1;
        TGraph graph2;

        graph1.CreateNode(TId("yandexuid", "1"));
        graph1.CreateNode(TId("yandexuid", "2"));
        graph1.CreateNode(TId("yandexuid", "3"));

        graph2.CreateNode(TId("yandexuid", "1"));
        graph2.CreateNode(TId("yandexuid", "2"));
        graph2.CreateNode(TId("yandexuid", "3"));

        graph1.CreateEdge(0, 1);
        graph1.CreateEdge(1, 2);

        graph2.CreateEdge(0, 1);

        UNIT_ASSERT(!TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(!TGraphComparator::OrderedEqual(graph1, graph2));
    }

    Y_UNIT_TEST(DifferentEdges) {
        TGraph graph1;
        TGraph graph2;

        graph1.CreateNode(TId("yandexuid", "1"));
        graph1.CreateNode(TId("yandexuid", "2"));
        graph1.CreateNode(TId("yandexuid", "3"));

        graph2.CreateNode(TId("yandexuid", "1"));
        graph2.CreateNode(TId("yandexuid", "2"));
        graph2.CreateNode(TId("yandexuid", "3"));

        graph1.CreateEdge(0, 1);
        graph1.CreateEdge(1, 2);

        graph2.CreateEdge(0, 1);
        graph2.CreateEdge(0, 2);

        UNIT_ASSERT(!TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(!TGraphComparator::OrderedEqual(graph1, graph2));
    }

    Y_UNIT_TEST(ReorderedEdges) {
        TGraph graph1;
        TGraph graph2;

        graph1.CreateNode(TId("yandexuid", "1"));
        graph1.CreateNode(TId("yandexuid", "2"));
        graph1.CreateNode(TId("yandexuid", "3"));

        graph2.CreateNode(TId("yandexuid", "1"));
        graph2.CreateNode(TId("yandexuid", "2"));
        graph2.CreateNode(TId("yandexuid", "3"));

        graph1.CreateEdge(0, 1);
        graph1.CreateEdge(1, 2);

        graph2.CreateEdge(1, 2);
        graph2.CreateEdge(0, 1);

        UNIT_ASSERT(TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(!TGraphComparator::OrderedEqual(graph1, graph2));
    }

    Y_UNIT_TEST(ReorderedMultiEdges) {
        TGraph graph1;
        TGraph graph2;

        graph1.CreateNode(TId("yandexuid", "1"));
        graph1.CreateNode(TId("yandexuid", "2"));

        graph2.CreateNode(TId("yandexuid", "1"));
        graph2.CreateNode(TId("yandexuid", "2"));

        graph1.CreateEdge(0, 1)->Attributes["attr1"] = "value1";
        graph1.CreateEdge(1, 0)->Attributes["attr2"] = "value2";

        graph2.CreateEdge(0, 1)->Attributes["attr2"] = "value2";
        graph2.CreateEdge(1, 0)->Attributes["attr1"] = "value1";

        UNIT_ASSERT(TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(!TGraphComparator::OrderedEqual(graph1, graph2));
    }

    Y_UNIT_TEST(NotMatchingMultiEdges) {
        TGraph graph1;
        TGraph graph2;

        graph1.CreateNode(TId("yandexuid", "1"));
        graph1.CreateNode(TId("yandexuid", "2"));

        graph2.CreateNode(TId("yandexuid", "1"));
        graph2.CreateNode(TId("yandexuid", "2"));

        graph1.CreateEdge(0, 1)->Attributes["attr1"] = "value1";
        graph1.CreateEdge(1, 0)->Attributes["attr2"] = "value2";

        graph2.CreateEdge(0, 1)->Attributes["attr2"] = "value2";
        graph2.CreateEdge(1, 0)->Attributes["bad_attr"] = "bad_value";

        UNIT_ASSERT(!TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(!TGraphComparator::OrderedEqual(graph1, graph2));
    }

    Y_UNIT_TEST(DifferentEdgeAttrs) {
        TGraph graph1;
        TGraph graph2;

        graph1.CreateNode(TId("yandexuid", "1"));
        graph1.CreateNode(TId("yandexuid", "2"));

        graph2.CreateNode(TId("yandexuid", "1"));
        graph2.CreateNode(TId("yandexuid", "2"));

        graph1.CreateEdge(0, 1)->Attributes["attr1"] = "value1";
        graph2.CreateEdge(0, 1)->Attributes["attr2"] = "value2";

        UNIT_ASSERT(!TGraphComparator::Equal(graph1, graph2));
        UNIT_ASSERT(!TGraphComparator::OrderedEqual(graph1, graph2));
    }
}
