package ru.yandex.crypta.graph2.model.matching.helper;

import java.util.List;

import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.model.soup.edge.EdgeProtoHelper;
import ru.yandex.crypta.graph2.model.soup.proto.Edge;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

public class EdgeProtoTestHelper {
    public static Edge createEdge(String id1, EIdType id1Type, String id2, EIdType id2Type, ESourceType sourceType,
                                  ELogSourceType logSource, List<String> dates) {
        String tId1Type = Soup.CONFIG.name(id1Type);
        String tId2Type = Soup.CONFIG.name(id2Type);
        String tSourceType = Soup.CONFIG.name(sourceType);
        String tLogSourceType = Soup.CONFIG.name(logSource);

        Edge.Builder builder = Edge.newBuilder()
                .setId1(id1)
                .setId1Type(tId1Type)
                .setId2(id2)
                .setId2Type(tId2Type)
                .setSourceType(tSourceType)
                .setLogSource(tLogSourceType);

        EdgeProtoHelper.setDates(builder, dates);
        return builder.build();
    }
}
