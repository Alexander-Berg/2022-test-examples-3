#include "graph_test_utils.h"

#include <util/string/cast.h>

using namespace NCrypta::NIS;

TGraph CreateGraph(size_t nodesCount, size_t edgesCount, size_t base) {
    TGraph graph;

    graph.SetId(base);

    auto& graphAttrs = graph.GetAttributes();
    graphAttrs["base"] = ToString(base);

    if (nodesCount < 1) {
        return graph;
    }

    for (size_t i = 0; i < nodesCount; i++) {
        auto* node = graph.CreateNode("yandexuid", ToString(base + i + 1));
        node->Attributes = {{"index", ToString(i)}};
    }

    size_t currEdgesCount = 0;

    auto addEdge = [&](size_t i, size_t step) {
        if (currEdgesCount >= edgesCount) {
            return false;
        }

        auto* edge = graph.CreateEdge(i, (i + step) % nodesCount);
        edge->Attributes = {{"source", "matching"}};

        currEdgesCount++;

        return true;
    };

    while (currEdgesCount < edgesCount) {
        for (size_t step = 1; (step * 2) < nodesCount; step++) {
            for (size_t i = 0; i < nodesCount; i++) {
                if (!addEdge(i, step)) {
                    return graph;
                }
            }
        }

        if ((nodesCount % 2) == 0) {
            size_t halfNodesCount = nodesCount / 2;

            for (size_t i = 0; i < halfNodesCount; i++) {
                if (!addEdge(i, halfNodesCount)) {
                    return graph;
                }
            }
        }
    }

    return graph;
}

TGraph CreateCompleteGraph(size_t nodesCount, size_t base) {
    size_t edgesCount = (nodesCount > 0) ? (nodesCount * (nodesCount - 1) / 2) : 0;

    return CreateGraph(nodesCount, edgesCount, base);
}
