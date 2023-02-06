package ru.yandex.crypta.graph2.model.matching.helper;

import ru.yandex.crypta.graph.soup.config.proto.TEdgeRecord;
import ru.yandex.crypta.graph2.model.soup.edge.Edge;
import ru.yandex.crypta.graph2.model.soup.edge.EdgeProtoHelper;
import ru.yandex.crypta.graph2.model.soup.edge.weight.EdgeInfoProvider;
import ru.yandex.crypta.graph2.model.soup.edge.weight.estimator.DatesActivityEdgeWeightEstimator;
import ru.yandex.crypta.graph2.model.soup.sources.DefaultEdgeTypeConfigProvider;
import ru.yandex.crypta.graph2.model.soup.sources.EdgeTypeConfigProvider;

public class DatesActivityEdgeInfoProvider implements EdgeInfoProvider {

    private final EdgeTypeConfigProvider edgeTypeConfigProvider = new DefaultEdgeTypeConfigProvider();
    private final DatesActivityEdgeWeightEstimator estimator = new DatesActivityEdgeWeightEstimator(
            new DefaultEdgeTypeConfigProvider()
    );

    @Override
    public double getEdgeWeight(Edge edge) {
        ru.yandex.crypta.graph2.model.soup.proto.Edge protoEdge = EdgeProtoTestHelper.createEdge(
                edge.getId1(), edge.getId1Type(),
                edge.getId2(), edge.getId2Type(),
                edge.getSourceType(),
                edge.getLogSource(),
                edge.getDates()
        );
        return getEdgeWeight(protoEdge);
    }

    @Override
    public double getEdgeWeight(ru.yandex.crypta.graph2.model.soup.proto.Edge edge) {
        return estimator.getEdgeWeight(edge);
    }

    @Override
    public TEdgeRecord getEdgeTypeConfig(Edge edge) {
        return edgeTypeConfigProvider.getEdgeTypeConfig(edge.calculateEdgeType());
    }

    @Override
    public TEdgeRecord getEdgeTypeConfig(ru.yandex.crypta.graph2.model.soup.proto.Edge edge) {
        return edgeTypeConfigProvider.getEdgeTypeConfig(EdgeProtoHelper.getEdgeType(edge));
    }

}
