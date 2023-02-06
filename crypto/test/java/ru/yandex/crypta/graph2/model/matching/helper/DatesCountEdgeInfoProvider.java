package ru.yandex.crypta.graph2.model.matching.helper;

import ru.yandex.crypta.graph.soup.config.proto.TEdgeRecord;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.edge.weight.EdgeInfoProvider;

public class DatesCountEdgeInfoProvider implements EdgeInfoProvider {

    private static final TEdgeRecord USUAL_PROPS = TEdgeRecord.newBuilder().build();

    @Override
    public double getEdgeWeight(Edge edge) {
        return edge.getDates().size();
    }

    @Override
    public double getEdgeWeight(ru.yandex.crypta.graph2.model.soup.proto.Edge edge) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TEdgeRecord getEdgeTypeConfig(Edge edge) {
        return USUAL_PROPS;
    }

    @Override
    public TEdgeRecord getEdgeTypeConfig(ru.yandex.crypta.graph2.model.soup.proto.Edge edge) {
        return USUAL_PROPS;
    }
}
