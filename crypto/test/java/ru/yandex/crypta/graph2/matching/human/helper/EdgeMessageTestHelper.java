package ru.yandex.crypta.graph2.matching.human.helper;

import ru.yandex.crypta.graph.soup.config.Soup;
import ru.yandex.crypta.graph.soup.config.proto.ELogSourceType;
import ru.yandex.crypta.graph.soup.config.proto.ESourceType;
import ru.yandex.crypta.graph2.model.matching.component.ComponentCenter;
import ru.yandex.crypta.graph2.model.matching.proto.CryptaIdEdgeMessage;
import ru.yandex.crypta.lib.proto.identifiers.EIdType;

public class EdgeMessageTestHelper {

    public static CryptaIdEdgeMessage build(String id1, EIdType id1Type, String id2, EIdType id2Type,
                                            ESourceType sourceType, ELogSourceType logSource) {
        return CryptaIdEdgeMessage.newBuilder()
                .setId(id2)
                .setIdType(Soup.CONFIG.name(id2Type))
                .setId1(id1)
                .setId1Type(Soup.CONFIG.name(id1Type))
                .setId2(id2)
                .setId2Type(Soup.CONFIG.name(id2Type))
                .setSourceType(Soup.CONFIG.name(sourceType))
                .setLogSource(Soup.CONFIG.name(logSource))
                .build();
    }

    public static CryptaIdEdgeMessage edgeMessageToSecondId(String id1, EIdType id1Type, String id2, EIdType id2Type,
                                                            ESourceType sourceType, ELogSourceType logSource) {
        return build(id1, id1Type, id2, id2Type, sourceType, logSource);
    }


    public static CryptaIdEdgeMessage edgeMessageToSecondId(String id1, EIdType id1Type, String id2, EIdType id2Type,
                                                            double survivalWeight, double datesWeight) {
        return build(id1, id1Type, id2, id2Type, ESourceType.ACCOUNT_MANAGER, ELogSourceType.ACCESS_LOG)
                .toBuilder()
                .setDatesWeight(datesWeight)
                .setSurvivalWeight(survivalWeight)
                .build();

    }


    public static CryptaIdEdgeMessage edgeMessageToSecondId(String id1, EIdType id1Type,
                                                            String id2, EIdType id2Type) {
        return build(id1, id1Type, id2, id2Type, ESourceType.ACCOUNT_MANAGER, ELogSourceType.ACCESS_LOG);

    }

    public static CryptaIdEdgeMessage edgeMessageToSecondId(String id1, EIdType id1Type,
                                                            String id2, EIdType id2Type,
                                                            ComponentCenter cryptaId) {
        return build(id1, id1Type, id2, id2Type, ESourceType.ACCOUNT_MANAGER, ELogSourceType.ACCESS_LOG)
                .toBuilder()
                .setCryptaId(cryptaId.getCryptaId())
                .build();

    }
}
