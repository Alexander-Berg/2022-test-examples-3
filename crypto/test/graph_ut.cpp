#include <crypta/idserv/data/graph_test_utils.h>
#include <library/cpp/testing/unittest/registar.h>

Y_UNIT_TEST_SUITE(TGraph) {
    using namespace NCrypta::NIS;

    void AssertAllNodesAreAdjacent(const TGraph& graph, size_t nodeFrom, size_t nodeTo) {
        if (nodeTo < 1) {
            return;
        }

        for (size_t n1 = nodeFrom; n1 < nodeTo - 1; n1++) {
            for (size_t n2 = n1 + 1; n2 < nodeTo; n2++) {
                UNIT_ASSERT(graph.AreAdjacent(n1, n2));
                UNIT_ASSERT(graph.AreAdjacent(n2, n1));
            }
        }
    }

    Y_UNIT_TEST(CompleteGraph) {
        const auto size = 10;
        auto graph = CreateCompleteGraph(size, 1000);

        const auto& nodes = graph.GetNodes();
        const auto& edges = graph.GetEdges();

        UNIT_ASSERT_EQUAL(size, nodes.size());
        UNIT_ASSERT_EQUAL(size * (size - 1) / 2, edges.size());

        UNIT_ASSERT_STRINGS_EQUAL(ToString(1000), graph.GetAttributes().at("base"));

        for (size_t i = 0; i < nodes.size(); i++) {
            const auto& node = graph.GetNode(i);
            UNIT_ASSERT_EQUAL(i, graph.IndexOf(node));

            UNIT_ASSERT_EQUAL(size - 1, node->Edges.size());

            for (const auto& edge : node->Edges) {
                UNIT_ASSERT_UNEQUAL(edges.end(), std::find(edges.begin(), edges.end(), edge));
            }
        }

        AssertAllNodesAreAdjacent(graph, 0, graph.GetNodes().size());
    }

    void TestMerge(size_t nodesCount1, size_t nodesCount2) {
        auto graph1 = CreateCompleteGraph(nodesCount1, 1000000);
        auto graph2 = CreateCompleteGraph(nodesCount2, 2000000);

        const auto edgesCount1 = graph1.GetEdges().size();
        const auto edgesCount2 = graph2.GetEdges().size();

        TVector<TNode*> nodePtrs;
        nodePtrs.insert(nodePtrs.end(), graph1.GetNodes().begin(), graph1.GetNodes().end());
        nodePtrs.insert(nodePtrs.end(), graph2.GetNodes().begin(), graph2.GetNodes().end());

        graph1.Merge(std::move(graph2));

        UNIT_ASSERT_EQUAL(nodesCount1 + nodesCount2, graph1.GetNodes().size());
        UNIT_ASSERT_EQUAL(edgesCount1 + edgesCount2, graph1.GetEdges().size());

        UNIT_ASSERT_EQUAL(0, graph2.GetNodes().size());
        UNIT_ASSERT_EQUAL(0, graph2.GetEdges().size());

        const auto& nodes = graph1.GetNodes();
        const auto& edges = graph1.GetEdges();

        for (size_t i = 0; i < nodes.size(); i++) {
            const auto node = graph1.GetNode(i);
            UNIT_ASSERT_EQUAL(i, graph1.IndexOf(node));
            UNIT_ASSERT_EQUAL(nodePtrs[i], node);

            for (const auto& edge : node->Edges) {
                UNIT_ASSERT_UNEQUAL(edges.end(), std::find(edges.begin(), edges.end(), edge));
            }
        }

        AssertAllNodesAreAdjacent(graph1, 0, nodesCount1);
        AssertAllNodesAreAdjacent(graph1, nodesCount1, nodesCount2);
    }

    Y_UNIT_TEST(Merge_30_20) {
        TestMerge(30, 20);
    }

    Y_UNIT_TEST(Merge_0_0) {
        TestMerge(0, 0);
    }

    Y_UNIT_TEST(Merge_1_1) {
        TestMerge(1, 1);
    }

    Y_UNIT_TEST(Merge_0_20) {
        TestMerge(0, 20);
    }

    Y_UNIT_TEST(Merge_20_0) {
        TestMerge(20, 0);
    }

    Y_UNIT_TEST(Merge_20_1) {
        TestMerge(20, 1);
    }

    Y_UNIT_TEST(Merge_1_20) {
        TestMerge(1, 20);
    }

    Y_UNIT_TEST(MergeIntersecting) {
        size_t base = 1000000;
        size_t size = 10;

        auto graph1 = CreateCompleteGraph(size, base);
        auto graph2 = CreateCompleteGraph(size, base + 1);

        UNIT_ASSERT_EXCEPTION(graph1.Merge(std::move(graph2)), yexception);
    }

    Y_UNIT_TEST(UnknownIds) {
        TGraph graph;

        auto* node1 = graph.CreateNode(TId("unknown", ""));
        auto* node2 = graph.CreateNode(TId("unknown", ""));
        graph.CreateEdge(node1, node2);

        UNIT_ASSERT_EQUAL(2, graph.GetNodes().size());
        UNIT_ASSERT(graph.AreAdjacent(node1, node2));
    }

    Y_UNIT_TEST(FindNode) {
        auto graph = CreateCompleteGraph(3, 100);

        UNIT_ASSERT_EQUAL(TId("yandexuid", "101"), graph.FindNode(TId("yandexuid", "101"))->Id);
        UNIT_ASSERT_EQUAL(TId("yandexuid", "102"), graph.FindNode(TId("yandexuid", "102"))->Id);
        UNIT_ASSERT_EQUAL(TId("yandexuid", "103"), graph.FindNode(TId("yandexuid", "103"))->Id);
        UNIT_ASSERT_EQUAL(nullptr, graph.FindNode(TId("yandexuid", "104")));
        UNIT_ASSERT_EXCEPTION(graph.FindNode(TId("unknown", "")), yexception);
    }

    Y_UNIT_TEST(HasNode) {
        auto graph = CreateCompleteGraph(3, 100);

        UNIT_ASSERT(graph.HasNode(TId("yandexuid", "101")));
        UNIT_ASSERT(graph.HasNode(TId("yandexuid", "102")));
        UNIT_ASSERT(graph.HasNode(TId("yandexuid", "103")));
        UNIT_ASSERT(!graph.HasNode(TId("yandexuid", "104")));
        UNIT_ASSERT_EXCEPTION(graph.HasNode(TId("unknown", "")), yexception);
    }
}
